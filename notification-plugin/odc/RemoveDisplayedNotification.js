// JS-node Input: NotificationJSON Text (ONE complete row from the read result).
// Outputs: Removed Boolean, Success Boolean, ErrorMessage Text.
(async function () {
    $parameters.Success = false;
    $parameters.Removed = false;
    $parameters.ErrorMessage = "";
    try {
        const plugin = window.CapacitorPlugins && window.CapacitorPlugins.SSVNotificationManagement;
        if (!plugin) throw new Error("SSVNotificationManagement is not installed in this native package.");
        const item = JSON.parse($parameters.NotificationJSON);
        if (!Number.isInteger(item.id) || !Object.prototype.hasOwnProperty.call(item, "tag") ||
            !(item.tag === null || typeof item.tag === "string") ||
            typeof item.postTime !== "string" || !/^\d+$/.test(item.postTime)) {
            throw new Error("Select one original row with id, tag and postTime.");
        }
        const result = await plugin.removeDisplayedNotification({
            id: item.id, tag: item.tag, postTime: item.postTime
        });
        $parameters.Removed = result.removed === true;
        $parameters.Success = true;
    } catch (error) {
        $parameters.ErrorMessage = error && error.message ? error.message : String(error);
    } finally {
        $resolve();
    }
})();
