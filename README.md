# SSV Notification Management – Android-Prototyp 0.1.0

## Was vorbereitet ist

Ein eigenständiges Capacitor-Plugin mit zwei Methoden:

- `getDisplayedNotifications()` liest noch angezeigte Benachrichtigungen der eigenen App.
- `removeDisplayedNotification({id, tag, postTime})` entfernt genau einen zuvor ausgewählten Eintrag.

Android-Code, JavaScript-Brücke, TypeScript-Typen, Gradle-Konfiguration, ODC-JavaScript-Nodes und Tests sind enthalten. Keine Firebase-Abhängigkeit, kein MessagingService, kein Boot Receiver, keine Push-Registrierung und keine Initialisierung beim Laden. Der Pluginname in JavaScript lautet `SSVNotificationManagement`.

**Status:** Entwicklungsversion, nicht auf einem Handy oder mit MABS kompiliert/getestet. Die sechs automatisierten Tests prüfen die ODC-JavaScript-Nodes und deren Fehlerbehandlung, nicht die Android-Implementierung. Hier sind kein Android SDK/Gradle/JDK-Compiler und kein angemeldetes ODC verfügbar. iOS ist in dieser ersten Version ausdrücklich NICHT implementiert. Noch keine automatische Entfernung nach TaskId.

## Vor dem ersten MABS-Test

MABS 12.1 / Capacitor wurde anhand des Screenshots bestätigt. Die genaue Capacitor-Core-Version lässt sich daraus nicht ablesen und wurde in der öffentlichen MABS-Stack-Tabelle nicht ausgewiesen. Die Peer-Range `>=6 <9` ist eine API-basierte Entwicklungsvorgabe, keine geprüfte Kompatibilitätsgarantie. Vor einem produktiven Einsatz gegen die tatsächliche Core-Version kompilieren. Diese findet ein Entwickler im heruntergeladenen Capacitor-Quellprojekt bzw. in dessen package-lock.json/Build-Protokoll.

Zuerst in einer separaten Testapp mit dem gleichen Firebase-Plugin und dessen Konfiguration testen. Die Benachrichtigungen der Zielapp kann eine andere Testapp nicht auslesen: Android isoliert Apps. Daher Benachrichtigungen an die Testapp selbst schicken.

## 1. Native Quelle bereitstellen – das ist der nächste notwendige Schritt

Dieses ZIP wird NICHT als Ressource in ODC hochgeladen. MABS muss das native Paket aus einer erreichbaren Paketquelle herunterladen können.

Praktischer Weg: Inhalt des Ordners `notification-plugin` in die Wurzel eines Git-Repositories legen. `package.json`, `dist` und `android` müssen dort direkt liegen. Es ist kein lokaler TypeScript-Build nötig: die kleine JS-Brücke ist bereits als ESM und CommonJS enthalten. Keine Unternehmensdaten oder Tokens mit hochladen.

ODC akzeptiert im Feld `source.npm` einen npm Package Spec. npm erlaubt darunter Git-URLs. Die Erreichbarkeit und Authentifizierung eines privaten Repositories aus MABS müssen separat eingerichtet/verifiziert werden. Es wurde hier kein Repository erstellt und nichts öffentlich veröffentlicht.

Beispiel (Platzhalter ersetzen; vollständigen Commit-Hash verwenden):

```json
{
  "buildConfigurations": {
    "capacitor": {
      "source": {
        "npm": "git+https://github.com/OWNER/REPOSITORY.git#COMMIT_SHA"
      }
    }
  }
}
```

Die alternative `.tgz`-Datei ist ein vorbereitetes npm-Paket, kein APK und kein ODC-Library-Upload. Sie kann über eine für MABS erreichbare, beständige HTTPS-Paketquelle referenziert werden; ein ChatGPT-Dateilink ist dafür keine geeignete Paketquelle. Die Git-Variante benötigt diese Datei nicht.

`private: true` verhindert ein versehentliches npm-Publish; dies ist unabhängig von der Sichtbarkeit des Git-Repositories. Für npm-Veröffentlichung müssten Paketname/Scope, Zugriff und private-Flag bewusst angepasst werden.

## 2. ODC-Library einrichten

1. Deine Mobile Library `NotificationManagement` öffnen.
2. Library Properties → Mobile → Extensibility.
3. Nur Capacitor auswählen.
4. Den alten Eintrag `@capacitor/push-notifications` vollständig durch die neue feste Paketquelle ersetzen.
5. Keine zusätzlichen Android-Berechtigungen oder Firebase-Einstellungen sind für diese zwei Methoden nötig.

## 3. Client Action GetDisplayedNotifications

Public = Yes. Outputs sowohl an der Client Action als auch am JavaScript-Node:

| Name | Typ |
|---|---|
| NotificationsJSON | Text |
| Success | Boolean |
| ErrorMessage | Text |

JavaScript-Node: Inhalt von `odc/GetDisplayedNotifications.js` kopieren. Danach Assign: die drei JS-node Outputs an die drei Action Outputs zuweisen. Beispiel: `NotificationsJSON = JavaScript1.NotificationsJSON`.

Auf einem Testscreen einen Button „Benachrichtigungen auslesen“ anlegen. OnClick → Action aufrufen → NotificationsJSON und ErrorMessage in Screen-Variablen übernehmen und per Expression anzeigen. In dieser Phase keinen Aufruf beim Login/Appstart und keine Entfernung einbauen.

## 4. Erste Prüfung auf Android

1. Testapp mit Library veröffentlichen und natives Paket MABS 12.1 / Capacitor bauen.
2. Testapp starten. Bereits ohne Plugin-Aufruf muss der Start funktionieren.
3. Drei normale Firebase-Push-Nachrichten an diese Testapp senden: A/B mit TaskId 4711, C mit TaskId 4712. ExtraDataList: Key `TaskId`, Value als Text.
4. App über ihr Icon öffnen, nicht durch Tippen auf eine Benachrichtigung.
5. Auslesen-Button drücken.
6. Ergebnisse prüfen: `id`, `tag`, `postTime`, `title`, `body`, `extras`.

Beispiel eines Ergebnisses, KEINE Zusage über das echte Firebase-Payload:

```json
{"notifications":[{"id":0,"tag":"example-A","postTime":"1791374400000","title":"Aufgabe","body":"Bitte prüfen","group":null,"isGroupSummary":false,"extras":{"TaskId":"4711"}}]}
```

`id` allein ist auf Android nicht eindeutig genug. Den kompletten Eintrag behalten. `tag` kann null sein, id kann 0 sein. `postTime` ist absichtlich Text (64-Bit-Zeitstempel). Gruppenübersichten sind ebenfalls möglich; die erste manuelle Entfernung auf einen echten Einzeleintrag begrenzen.

## 5. Client Action RemoveDisplayedNotification

Public = Yes. Input `NotificationJSON` Text: EIN vollständiger Originaleintrag aus `notifications`, NICHT die gesamte Liste.

Outputs: `Removed` Boolean, `Success` Boolean, `ErrorMessage` Text. Gleichnamige Input/Outputs am JS-node definieren. Code aus `odc/RemoveDisplayedNotification.js` kopieren; Input durchreichen und danach Outputs per Assign übernehmen.

Nur hinter einem separaten Testbutton ausführen. Wenn Success=True und Removed=False, war der Eintrag beim Entfernen bereits weg oder hatte sich verändert. Removed=True bedeutet, dass die native cancel-Funktion für einen aktuellen passenden Eintrag aufgerufen wurde, keine bestätigte UI-Rückmeldung des Systems. Danach neu auslesen und Benachrichtigungsleiste prüfen.

Der Zeitstempel schützt gegen viele veraltete Auswahlen, garantiert aber keine atomare Entfernung, wenn zeitgleich eine neue Nachricht unter derselben tag/id erscheint. Für spätere produktive Zuordnung pro Nachricht stabile technische Identitäten verwenden.

## 6. TaskId-Zuordnung: bewusst noch offen

Firebase ExtraDataList ist nicht automatisch identisch mit Android Notification.extras. Häufig liegen Zusatzdaten im Tap-Intent und sind über getActiveNotifications nicht direkt zugänglich. Das Plugin liest den PendingIntent nicht mit Tricks aus. Auch eine selbst vergebene NotificationId ist nicht automatisch die Android-ID.

Wenn TaskId in extras fehlt, bitte das Ergebnis (ohne personenbezogene Inhalte) bereitstellen. Dann prüfen wir die echte Firebase-Plugin-Implementierung und deren SendRequest-Struktur. Eine mögliche Android-Strategie ist ein expliziter notification.tag, sofern im Versand verfügbar; auf FCM-Ebene existiert er, aber die ODC-Feldverfügbarkeit ist noch nicht geprüft. Gleicher tag kann bestehende Nachrichten ersetzen: für mehrere Nachrichten je Aufgabe wären eindeutige Ereignis-Tags und eine Task-Zuordnung nötig.

Bis die Zuordnung nachgewiesen ist, NICHT nach Titel/Body raten und NICHT alle Nachrichten löschen. Keine Funktion RemoveNotificationsForTask wird vorgetäuscht. Für iOS ist eine eigene Implementierung mit getDeliveredNotifications und Entfernung über request.identifier erforderlich.

## Tests und Dateien

`npm test` führt die sechs JS-Wrapper-Tests aus. Keine Netzwerkverbindung erforderlich. `npm pack --ignore-scripts` erstellt das native Quellenpaket. Das Paket enthält keine install/prepare/postinstall-Hooks. Die Tests prüfen fehlendes Plugin, native Fehler, erhaltene Identitäten, null-tag/zero-id, ungültige Auswahl und bereits entfernte Benachrichtigungen.

## Quellen

- ODC Integration: https://success.outsystems.com/documentation/outsystems_developer_cloud/building_apps/mobile_apps/use_mobile_plugins/integrate_capacitor_plugin_into_a_mobile_app/
- ODC Library-Schema: https://success.outsystems.com/documentation/outsystems_developer_cloud/building_apps/mobile_apps/configure_mobile_apps/universal_extensibility_configurations_json_schema/library_plugin_extensibility_configuration_json_schema/
- Capacitor Android Plugins: https://capacitorjs.com/docs/v6/plugins/android
- Android NotificationManager: https://developer.android.com/reference/android/app/NotificationManager
- npm Package Spec: https://docs.npmjs.com/cli/v11/using-npm/package-spec/
- MABS Stack: https://success.outsystems.com/Support/Release_Notes/Mobile_Apps_Build_Service_Versions/
