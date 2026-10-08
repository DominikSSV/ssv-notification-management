package com.ssv.notificationmanagement;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Array;
import java.util.Objects;

/** No load hooks, Firebase dependencies, services, listeners or registration. */
@CapacitorPlugin(name = "SSVNotificationManagement")
public class SSVNotificationManagementPlugin extends Plugin {
    private NotificationManager manager() {
        return (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @PluginMethod
    public void getDisplayedNotifications(PluginCall call) {
        if (Build.VERSION.SDK_INT < 23) {
            call.reject("Android API 23 or newer is required.");
            return;
        }
        try {
            NotificationManager manager = manager();
            if (manager == null) { call.reject("NotificationManager unavailable."); return; }
            JSArray list = new JSArray();
            for (StatusBarNotification item : manager.getActiveNotifications()) {
                // Only the calling app; no NotificationListenerService or global access.
                if (!getContext().getPackageName().equals(item.getPackageName())) continue;
                Notification n = item.getNotification();
                JSObject row = new JSObject();
                row.put("id", item.getId());
                row.put("tag", item.getTag() == null ? JSONObject.NULL : item.getTag());
                row.put("postTime", Long.toString(item.getPostTime()));
                row.put("title", text(n.extras, Notification.EXTRA_TITLE));
                row.put("body", text(n.extras, Notification.EXTRA_TEXT));
                row.put("group", n.getGroup() == null ? JSONObject.NULL : n.getGroup());
                row.put("isGroupSummary", (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0);
                row.put("extras", jsonBundle(n.extras, 0));
                try {
                    row.put("diagnostics", notificationDiagnostics(item, n));
                } catch (Exception diagnosticError) {
                    JSObject diagnostics = new JSObject();
                    diagnostics.put("error", diagnosticError.getClass().getSimpleName() + ": " + diagnosticError.getMessage());
                    row.put("diagnostics", diagnostics);
                }
                list.put(row);
            }
            JSObject result = new JSObject();
            result.put("pluginVersion", "0.2.0");
            result.put("notifications", list);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("Unable to read displayed notifications: " + error.getMessage());
        }
    }

    @PluginMethod
    public void removeDisplayedNotification(PluginCall call) {
        if (Build.VERSION.SDK_INT < 23) { call.reject("Android API 23 or newer is required."); return; }
        Integer id = call.getInt("id");
        String tag = call.getString("tag");
        String postTime = call.getString("postTime");
        if (id == null || !call.getData().has("tag") || postTime == null || postTime.isEmpty()) {
            call.reject("Provide id, explicit tag (string or null), and postTime from the read result.");
            return;
        }
        try {
            NotificationManager manager = manager();
            if (manager == null) { call.reject("NotificationManager unavailable."); return; }
            boolean removed = false;
            for (StatusBarNotification item : manager.getActiveNotifications()) {
                if (getContext().getPackageName().equals(item.getPackageName())
                    && item.getId() == id && Objects.equals(item.getTag(), tag)
                    && Long.toString(item.getPostTime()).equals(postTime)) {
                    // A stale list must not remove a replacement posted before this check.
                    manager.cancel(tag, id);
                    removed = true;
                    break;
                }
            }
            JSObject result = new JSObject();
            result.put("removed", removed);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("Unable to remove notification: " + error.getMessage());
        }
    }

    // Diagnostic read only: never send a PendingIntent or alter a notification.
    private static JSObject notificationDiagnostics(StatusBarNotification item, Notification n) throws Exception {
        JSObject result = new JSObject();
        result.put("key", item.getKey());
        result.put("category", nullable(n.category));
        result.put("sortKey", nullable(n.getSortKey()));
        result.put("channelId", Build.VERSION.SDK_INT >= 26 ? nullable(n.getChannelId()) : JSONObject.NULL);
        result.put("shortcutId", Build.VERSION.SDK_INT >= 26 ? nullable(n.getShortcutId()) : JSONObject.NULL);
        result.put("extrasKeyTypes", bundleKeyTypes(n.extras));
        result.put("omittedExtras", omittedBundleValues(n.extras));
        result.put("contentIntent", pendingIntentMetadata(n.contentIntent));
        result.put("deleteIntent", pendingIntentMetadata(n.deleteIntent));
        result.put("fullScreenIntent", pendingIntentMetadata(n.fullScreenIntent));
        result.put("pendingIntentPayloadReadable", false);
        JSArray actions = new JSArray();
        if (n.actions != null) {
            for (int i = 0; i < n.actions.length; i++) {
                Notification.Action action = n.actions[i];
                if (action == null) continue;
                JSObject row = new JSObject();
                row.put("index", i);
                row.put("title", action.title == null ? "" : action.title.toString());
                row.put("extras", jsonBundle(action.getExtras(), 0));
                row.put("extrasKeyTypes", bundleKeyTypes(action.getExtras()));
                row.put("omittedExtras", omittedBundleValues(action.getExtras()));
                row.put("actionIntent", pendingIntentMetadata(action.actionIntent));
                actions.put(row);
            }
        }
        result.put("actions", actions);
        return result;
    }

    private static Object nullable(String value) {
        return value == null ? JSONObject.NULL : value;
    }

    private static Object pendingIntentMetadata(PendingIntent intent) throws Exception {
        if (intent == null) return JSONObject.NULL;
        JSObject result = new JSObject();
        result.put("present", true);
        result.put("creatorPackage", nullable(intent.getCreatorPackage()));
        result.put("payloadReadable", false);
        return result;
    }

    private static JSONObject bundleKeyTypes(Bundle bundle) throws Exception {
        JSONObject result = new JSONObject();
        if (bundle == null) return result;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            result.put(key, value == null ? "null" : value.getClass().getName());
        }
        return result;
    }

    private static JSONObject omittedBundleValues(Bundle bundle) throws Exception {
        JSONObject result = new JSONObject();
        if (bundle == null) return result;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (jsonValue(value, 1) == null) {
                result.put(key, value == null ? "null" : value.getClass().getName());
            }
        }
        return result;
    }

    private static String text(Bundle bundle, String key) {
        if (bundle == null) return "";
        Object value = bundle.get(key);
        return value instanceof CharSequence ? value.toString() : "";
    }
    private static JSONObject jsonBundle(Bundle bundle, int depth) throws Exception {
        JSONObject result = new JSONObject();
        if (bundle == null || depth > 5) return result;
        for (String key : bundle.keySet()) {
            Object safe = jsonValue(bundle.get(key), depth + 1);
            if (safe != null) result.put(key, safe);
        }
        return result;
    }
    private static Object jsonValue(Object value, int depth) throws Exception {
        if (value == null) return JSONObject.NULL;
        if (depth > 5) return null;
        if (value instanceof CharSequence) return value.toString();
        if (value instanceof Boolean) return value;
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return Double.isNaN(d) || Double.isInfinite(d) ? null : value;
        }
        if (value instanceof Bundle) return jsonBundle((Bundle) value, depth);
        if (value.getClass().isArray()) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < Math.min(Array.getLength(value), 100); i++) {
                Object safe = jsonValue(Array.get(value, i), depth + 1);
                if (safe != null) array.put(safe);
            }
            return array;
        }
        // Parcelable objects such as PendingIntent, Bitmap and RemoteViews are omitted.
        return null;
    }
}
