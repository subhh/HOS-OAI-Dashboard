# Technische Designentscheidungen
Hier finden sich die in den Treffen vereinbarten 

## Datenmodell und Datenbank
* Harvest-Runs werden von den States abgeleitet - jederState hat genau einen Harvest-Run
* Bei fehlerhaften Harvest-Runs kann der zugehörige State leere Felder besitzen
* Grundsätzlich soll das Harvesting einmal am Tag laufen
* States haben einen Zeitstempel, der nur den jeweiligen Tag enthält (keine Uhrzeit etc.)
* Wird (zum Beispiel durch manuellen Eingriff) mehrfach am Tag geharvestet, wird in der Datenbank der letzte Harvest-Run und der dazugehörige State gespeichert
* Die anderen Harvest-Run und States werden ebenfalls gespeichert, aber durch eine geeignete Technik als inaktiv markiert
* Zeitstempel sind grundsätzlich in UTC (Z am Ende) abzulegen
* Records können kein, ein oder mehrere Sets besitzen

## Datenspeicherung
* Die Speicherung der Records erfolgt nach dem Harvestingprozess in einem Git
* Es werden nicht nur die Records, sondern auch die metha-ID-Informationen (verb=Identify) abgespeichert
* Die Datenspeicherung soll grundsätzlich ermöglichen, alte Systemzustände wiederherzustellen.
* Das Wiederherstellen alter Systemzustände ist aber nicht Teil des aktuellen Auftrags. Es werden außer der Speicherung im Git keine Tools hierfür entwickelt.

## Robustheit und Validierung
* Sets ohne SetName sind Fehler und werden ignoriert
* Es wird ein Validierungsmechanismus benötigt

## IDs
* Repository-IDs werden manuell in der Konfigurationsdatei vergeben, da sich sowohl die URL als auch der Name theoretisch ändern können
* Die ID eines Sets ist die spec
* Bei Metadaten ist das Prefix des Schemas die ID
