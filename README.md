# Image Investigator — Android-Prototyp 0.2

Ein Android-Studio-Projekt für die manuelle Rückwärtssuche. Fotoauswahl über Android Photo Picker (einschließlich ausgewählter Google-Fotos-Inhalte, soweit im Picker verfügbar), Android-Teilen-Menü, Bildvorschau und Übergabe an andere Apps. Die Schaltflächen für Yandex, Bing, TinEye und SauceNAO öffnen deren Upload-Seiten; das Foto muss dort erneut ausgewählt werden. Social-Schaltflächen starten öffentliche Textsuchen und enthalten keine Bilderkennung.

Der lokale Bildvergleich sortiert bis zu 50 ausgewählte Bilder nach einem 64-Bit-Wahrnehmungshash. Er kann Varianten desselben Motivs finden. Andere Fotos derselben Person werden damit nicht zuverlässig erkannt. Das Referenzbild und Vergleichsbilder bleiben bei dieser Funktion auf dem Gerät; die App speichert keinen dauerhaften Index.

## Bauen

Ordner in Android Studio öffnen, Gradle synchronisieren und `app` starten oder `Build > Build APK(s)` wählen. Voraussetzung: Android SDK 35 und JDK 17. Es ist kein Gradle Wrapper enthalten; Android Studio kann eine installierte Gradle-Version verwenden. Die App benötigt keine allgemeinen Foto- oder Internetberechtigungen.

Im GitHub-Repository wird bei jedem Push auf `main` automatisch eine Debug-APK gebaut. Alternativ unter **Actions → Android APK bauen → Run workflow** starten. Nach erfolgreichem Lauf steht unter **Artifacts** die Debug-APK als ZIP bereit. Die Debug-APK ist für eigene Tests gedacht; die erste Installation kann auf Android eine Freigabe für diese Quelle erfordern. Für Updates muss der Signierschlüssel erhalten bleiben, deshalb ist ein Release-Build mit festem Schlüssel ein späterer Schritt.

## Tatsächlicher Funktionsstand

- Im Quellcode vorhanden, jedoch noch nicht auf einem Android-Gerät getestet: Bild auswählen/empfangen, Vorschau, Abmessungen, Vergleich von bis zu 50 explizit ausgewählten Bildern, Android Share Intent, externe Suchseiten und öffentliche Textsuchen.
- Noch nicht implementiert: vollständiger lokaler Fotoindex, Gesichtserkennung/-abgleich, KI-Erkennung, automatische Crops, Ergebniszusammenführung, Google-Fotos-Komplettimport, SafeSearch-Steuerung, automatisch ausgeführte Uploads.
- Der GitHub-Actions-Build wurde erfolgreich ausgeführt; eine Installation auf einem Android-Gerät steht noch aus.

Die App erhält über den Photo Picker nur vom Nutzer ausgewählte Fotos. Sie kann nicht den internen Gesichtsindex von Google Fotos abfragen. Suchseiten können hochgeladene Bilder gemäß ihren eigenen Bedingungen verarbeiten.
