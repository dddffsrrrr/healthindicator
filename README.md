# Health Indicator (Fabric 1.21.11)

Zeigt die echten Lebenspunkte (+ Absorption vom Gapple) anderer Spieler neben ihrem Namen an, z.B. `❤ 7.5 +2`.

* Einstellungen: Mod Menu → Health Indicator → Konfigurieren
* Tasten: Optionen → Steuerung → Tastenbelegung → Health Indicator (`H` = an/aus)
* Erkennt, wenn der Server die Lebenspunkte fälscht, und zeigt dann `?` (oder den Scoreboard-Wert unter dem Namen).

Bauen: `gradlew build` (Java 21) oder über GitHub Actions. Die Jar liegt in `build/libs/`.
Benötigt Fabric API und (empfohlen) Mod Menu 17.x.
