# NotificationManagement Android 0.2.0 – Diagnose

Gemeint ist die mitgesendete TaskId, z. B. "4711".
Diese Version erweitert das Auslesen. Sie setzt oder erfindet keine TaskId.

## Neue Ausgabe

- pluginVersion: "0.2.0"
- Jede Benachrichtigung behält id, tag, postTime, title, body, group, isGroupSummary, extras.
- diagnostics enthält Aktionen samt deren eigenen Extras, Kanal, Kategorie, Sortierschlüssel,
  Shortcut-ID, Extra-Datentypen und die auf oberster Ebene nicht serialisierbaren Extra-Felder.
- Die Schlüssel contentIntent, deleteIntent und actionIntent enthalten nur Metadaten:
  deren ursprüngliche Intent-Payload ist mit diesen öffentlichen Android-APIs nicht auslesbar.
- Es gibt keine neue Empfangslogik, keine zusätzlichen Berechtigungen und keine Firebase-Abhängigkeit.
- Die Löschfunktion ist unverändert. iOS ist weiterhin nicht implementiert.

## Grenzen

Die Methode liest nur Benachrichtigungen dieser App. Sie löst keinen Klick aus.
Sie kann keine TaskId zurückholen, die nur im PendingIntent gespeichert ist.
Parcelable-Werte wie Bilder werden nicht als Nutzdaten serialisiert. Deren Klassen
werden auf der obersten Bundle-Ebene unter omittedExtras aufgeführt. Verschachtelte
Bundles/Arrays werden weiterhin nur bis Tiefe 5 bzw. 100 Array-Elementen ausgegeben.
Ein leeres actions-Array bedeutet: Die Benachrichtigung enthält keine Aktionsbuttons.
Es wird keine künstliche ActionList zum Testen vorausgesetzt.

## Update auf GitHub und in ODC

1. ZIP entpacken und den Ordner notification-plugin öffnen.
2. Den Inhalt dieses Ordners ins bestehende Repository hochladen/ersetzen.
   package.json muss direkt im Repository liegen. Der neue .tgz-Dateiname ist
   ssv-notification-management-0.2.0.tgz.
3. Extensibility Configuration: source.npm auf die Raw-URL der neuen .tgz ändern.
   Möglichst die SHA des neuen Commits im URL-Pfad verwenden.
4. Library veröffentlichen, App-Abhängigkeit aktualisieren und App veröffentlichen.
5. Neues natives Android-Paket bauen und installieren.
6. Bestehendes GetDisplayedNotifications unverändert aufrufen. pluginVersion muss 0.2.0 sein.
7. Eine Nachricht mit ExtraDataList Key="TaskId", Value="4711" senden, nicht antippen,
   App manuell öffnen und das JSON auslesen.

## Validierung

ODC-JavaScript-Wrapper getestet. Paketinhalt geprüft. Android-Code und tatsächliche
Payload-Ausgabe müssen im ODC-Build und auf dem Gerät geprüft werden; hier fehlen Android SDK/Gradle.
