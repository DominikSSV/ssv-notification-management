export interface IntentMetadata {
  present: boolean; creatorPackage: string | null; payloadReadable: false;
}
export interface NotificationActionDiagnostics {
  index: number; title: string; extras: Record<string, unknown>;
  extrasKeyTypes: Record<string, string>; omittedExtras: Record<string, string>;
  actionIntent: IntentMetadata | null;
}
export interface NotificationDiagnostics {
  key: string; category: string | null; sortKey: string | null;
  channelId: string | null; shortcutId: string | null;
  extrasKeyTypes: Record<string, string>; omittedExtras: Record<string, string>;
  contentIntent: IntentMetadata | null; deleteIntent: IntentMetadata | null;
  fullScreenIntent: IntentMetadata | null; pendingIntentPayloadReadable: false;
  actions: NotificationActionDiagnostics[];
}
export interface DisplayedNotification {
  id: number; tag: string | null; postTime: string;
  title: string; body: string; group: string | null;
  isGroupSummary: boolean; extras: Record<string, unknown>;
  diagnostics: NotificationDiagnostics | {error: string};
}
export interface NotificationManagementPlugin {
  getDisplayedNotifications(): Promise<{pluginVersion: string; notifications: DisplayedNotification[]}>;
  removeDisplayedNotification(options: {id: number; tag: string | null; postTime: string}): Promise<{removed: boolean}>;
}
export declare const SSVNotificationManagement: NotificationManagementPlugin;
