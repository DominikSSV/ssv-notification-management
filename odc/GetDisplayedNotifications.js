// JS-node Outputs: NotificationsJSON Text, Success Boolean, ErrorMessage Text.
(async function () {
    $parameters.Success = false;
    $parameters.NotificationsJSON = "";
    $parameters.ErrorMessage = "";
    try {
        const plugin = window.CapacitorPlugins && window.CapacitorPlugins.SSVNotificationManagement;
        if (!plugin) throw new Error("SSVNotificationManagement is not installed in this native package.");
        const result = await plugin.getDisplayedNotifications();
        $parameters.NotificationsJSON = JSON.stringify(result);
        $parameters.Success = true;
    } catch (error) {
        $parameters.ErrorMessage = error && error.message ? error.message : String(error);
    } finally {
        $resolve();
    }
})();
