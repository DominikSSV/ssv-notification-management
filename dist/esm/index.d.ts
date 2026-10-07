export interface DisplayedNotification {
  id: number; tag: string | null; postTime: string;
  title: string; body: string; group: string | null;
  isGroupSummary: boolean; extras: Record<string, unknown>;
}
export interface NotificationManagementPlugin {
  getDisplayedNotifications(): Promise<{notifications: DisplayedNotification[]}>;
  removeDisplayedNotification(options: {id: number; tag: string | null; postTime: string}): Promise<{removed: boolean}>;
}
export declare const SSVNotificationManagement: NotificationManagementPlugin;
