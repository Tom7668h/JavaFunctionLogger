# JavaFunctionLogger

JavaFunctionLogger ist ein Java-Programm, das alle `.java`-Dateien in einem angegebenen Verzeichnis durchsucht und mithilfe von Log4j2 alle Funktionen im Trace-Modus detailliert loggt. Das Programm fügt den Logger automatisch hinzu, falls er noch nicht in der Klasse vorhanden ist, und protokolliert folgende Informationen für jede Funktion:
- Funktionsname
- Modifizierer (z.B. public, private, static)
- Übergabeparameter (mit Datentypen und Werten zur Laufzeit)
- Rückgabewert-Typ

## Voraussetzungen

- Java 8 oder höher
- Maven
- Log4j2-Bibliotheken (werden über Maven hinzugefügt)

## Installation

1. **Repository klonen**:
   ```bash
   git clone https://github.com/username/JavaFunctionLogger.git
   cd JavaFunctionLogger
