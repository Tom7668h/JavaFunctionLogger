# 🔍 JavaFunctionLogger – AST Code Instrumentation & Observability CLI

[![Java](https://img.shields.io/badge/Java-17%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaParser](https://img.shields.io/badge/AST-JavaParser%203.26-blue)](https://javaparser.org/)
[![Picocli](https://img.shields.io/badge/CLI-Picocli%204.7-4E9A06)](https://picocli.info/)
[![Log4j2](https://img.shields.io/badge/Logging-Log4j2-C71A36?logo=apache)](https://logging.apache.org/log4j/2.x/)
[![JUnit 5](https://img.shields.io/badge/Tests-8%20Passed-6E9F18?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 🌐 **Sprache / Language:** [🇩🇪 Zur deutschen Version](#-deutsche-dokumentation) | [🇬🇧 Switch to English Version](#-english-documentation)

---

## 🇩🇪 Deutsche Dokumentation

Ein automatisches **Abstract Syntax Tree (AST)** Code-Instrumentierungs- und Observability-CLI-Tool, entwickelt mit **Java 17 LTS**, **JavaParser 3.26** und **Picocli 4.7**. Es analysiert Java-Quellcode-Verzeichnisbäume rekursiv, injiziert `Log4j2`-Logger und fügt strukturierte Methodenaufruf-Trace-Logs (inklusive Parameterwerten, PII-Maskierung, Array-Formatierung und Laufzeitmessung) ein – ohne Formatierung oder Kompilierbarkeit zu zerstören.

---

### 🎯 Kern-Features & Technische Highlights

- **Echte AST-Manipulation (Kein fehleranfälliges Regex):**  
  Nutzt **`JavaParser`** zur vollständigen syntaktischen Modellierung des Java-Codes. Verarbeitet Generics, Annotationen, überladene Methoden, Interfaces und verschachtelte Klassen absolut zuverlässig.
- **Sichere Logger-Injektion:**  
  Erkennt, ob eine Klasse bereits einen `Logger` besitzt. Falls nicht, werden fehlende Imports für `org.apache.logging.log4j.LogManager` / `Logger` hinzugefügt und `private static final Logger logger = LogManager.getLogger(ClassName.class);` in den Klassenrumpf injiziert.
- **Dynamisches Parameter-Tracing & Array-Formatierung:**  
  Analysiert Methodenparameter und erzeugt strukturierte Trace-Statements (z.B. `logger.trace("[OrderService#process] Called with: id=" + id + ", tags=" + java.util.Arrays.toString(tags));`).
- **DSGVO- & Compliance-konforme PII-Maskierung:**  
  Maskiert sensible Argumente (`password`, `secret`, `token`, `key`, `pin`, `credential`, `auth`) automatisch mit `"[REDACTED]"`, um Secret-Leaks in Log-Dateien auszuschließen.
- **Endlosschleifen-Schutz (Infinite Recursion Guard):**  
  Überspringt automatisch `toString()`, `hashCode()`, `equals()`, `finalize()` und `clone()`, um fatale `StackOverflowError` durch rekursives Logging zu verhindern.
- **Laufzeit-Profiling (`--timing`):**  
  Injiziert auf Wunsch präzise Zeitmessungen (`System.currentTimeMillis()`), um Engpässe und langsame Methoden im Produktivbetrieb zu identifizieren.
- **Zerstörungsfreie Backups (`--backup`):**  
  Erstellt vor jeder Datei-Modifikation eine `.bak`-Sicherheitskopie.

---

### 🏗️ Vorher-Nachher-Vergleich (AST Transformation)

#### Vor der Instrumentierung:
```java
public class OrderService {
    public OrderResult placeOrder(String customerId, String authToken, int[] itemIds) {
        // Business-Logik
        return new OrderResult("SUCCESS");
    }
}
```

#### Nach der Instrumentierung:
```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OrderService {
    private static final Logger logger = LogManager.getLogger(OrderService.class);

    public OrderResult placeOrder(String customerId, String authToken, int[] itemIds) {
        logger.trace("[OrderService#placeOrder] Called with: customerId=" + customerId + ", authToken=" + "[REDACTED]" + ", itemIds=" + java.util.Arrays.toString(itemIds));
        final long _methodStartTime = System.currentTimeMillis();

        // Business-Logik
        return new OrderResult("SUCCESS");
    }
}
```

---

### 🚀 Schnellstart & CLI-Nutzung

#### Voraussetzungen
- **Java Development Kit (JDK):** 17 oder höher
- **Apache Maven:** 3.8 oder höher

#### 1. Kompilieren & Tests ausführen
```bash
git clone https://github.com/Tom7668h/JavaFunctionLogger.git
cd JavaFunctionLogger
mvn clean test
```
*Ergebnis: 8 von 8 Unit-Tests erfolgreich (100% grün).*

#### 2. Standalone Fat JAR erstellen
```bash
mvn package -DskipTests
```
Erzeugt `target/JavaFunctionLogger-1.0.0.jar` inklusive aller Abhängigkeiten.

#### 3. CLI ausführen
```bash
# Vorschau im Terminal anzeigen ohne Dateien zu verändern
java -jar target/JavaFunctionLogger-1.0.0.jar --dry-run /pfad/zum/java-projekt

# Mit Zeitmessung und automatischen .bak Backups instrumentieren
java -jar target/JavaFunctionLogger-1.0.0.jar --timing --backup /pfad/zum/java-projekt

# Test-Klassen von der Instrumentierung ausschließen
java -jar target/JavaFunctionLogger-1.0.0.jar --exclude ".*Test.*" /pfad/zum/java-projekt
```

---

### 💻 CLI-Optionen & Flags

| Flag | Beschreibung |
| :--- | :--- |
| `<directoryPath>` | Zielverzeichnis zum rekursiven Durchsuchen und Instrumentieren (Standard: `.`) |
| `-b, --backup` | Erstellt eine `.bak`-Sicherungsdatei vor jeder Modifikation |
| `-d, --dry-run` | Zeigt Modifikationen nur in der Konsole an, ohne Dateien auf der Festplatte zu ändern |
| `-e, --exclude=<regex>` | Regulärer Ausdruck für Dateipfade, die von der Instrumentierung ausgeschlossen werden |
| `-t, --timing` | Injiziert Ausführungszeitmessung (`_methodStartTime`) |
| `-v, --verbose` | Detaillierte Debug-Ausgaben in der Konsole |
| `-V, --version` | Versionsinformationen ausgeben und beenden |
| `-h, --help` | Hilfemeldung anzeigen und beenden |

---

### 📄 Lizenz
MIT-Lizenz. Erstellt von [Tom7668h](https://github.com/Tom7668h).

<br><br>

---

## 🇬🇧 English Documentation

An automated **Abstract Syntax Tree (AST)** code instrumentation and observability tool built with **Java 17 LTS**, **JavaParser 3.26**, and **Picocli 4.7**. It recursively parses Java source trees, injects `Log4j2` loggers, and adds runtime method trace statements (with parameter values, PII redaction, array formatting, and execution timing) without breaking compilation or code formatting.

---

### 🎯 Key Highlights & Engineering Features

- **True AST Manipulation (Not Fragile Regex):**  
  Uses **`JavaParser`** to build a full syntactic model of Java source files. Handles generics, annotations, overloaded methods, interfaces, and nested classes accurately.
- **Safe Logger Injection:**  
  Detects whether a class already has a `Logger` field or imports. If missing, cleanly adds `org.apache.logging.log4j.LogManager` / `Logger` imports and injects `private static final Logger logger = LogManager.getLogger(ClassName.class);` directly into the class body.
- **Dynamic Parameter Tracing & Array Formatting:**  
  Inspects method parameters and generates structured trace statements (e.g. `logger.trace("[OrderService#process] Called with: id=" + id + ", tags=" + java.util.Arrays.toString(tags));`).
- **PII & Secret Redaction (Security Compliant):**  
  Automatically masks sensitive arguments (`password`, `secret`, `token`, `key`, `pin`, `credential`, `auth`) with `"[REDACTED]"` to prevent sensitive data leakage into application logs (GDPR & PCI-DSS ready).
- **Infinite Recursion Guard:**  
  Automatically skips `toString()`, `hashCode()`, `equals()`, `finalize()`, and `clone()` to prevent accidental `StackOverflowError` caused by recursive logging calls.
- **Execution-Time Profiling (`--timing`):**  
  Optionally injects high-precision execution timestamps (`System.currentTimeMillis()`) to profile slow methods and bottlenecks.
- **Non-Destructive Backups (`--backup`):**  
  Optionally generates a `.bak` backup copy of every source file before applying mutations in-place.

---

### 🏗️ Before-and-After Comparison (AST Transformation)

#### Before Instrumentation:
```java
public class OrderService {
    public OrderResult placeOrder(String customerId, String authToken, int[] itemIds) {
        // Business logic here
        return new OrderResult("SUCCESS");
    }
}
```

#### After Instrumentation:
```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OrderService {
    private static final Logger logger = LogManager.getLogger(OrderService.class);

    public OrderResult placeOrder(String customerId, String authToken, int[] itemIds) {
        logger.trace("[OrderService#placeOrder] Called with: customerId=" + customerId + ", authToken=" + "[REDACTED]" + ", itemIds=" + java.util.Arrays.toString(itemIds));
        final long _methodStartTime = System.currentTimeMillis();

        // Business logic here
        return new OrderResult("SUCCESS");
    }
}
```

---

### 🚀 Quickstart & CLI Usage

#### Requirements
- **Java Development Kit (JDK):** 17 or higher
- **Apache Maven:** 3.8 or higher

#### 1. Build and Run Tests
```bash
git clone https://github.com/Tom7668h/JavaFunctionLogger.git
cd JavaFunctionLogger
mvn clean test
```
*Result: 8 out of 8 Unit Tests passed (100% green).*

#### 2. Package Standalone Fat JAR
```bash
mvn package -DskipTests
```
This produces `target/JavaFunctionLogger-1.0.0.jar` with all dependencies bundled inside.

#### 3. Run CLI
```bash
# Preview modifications in console without modifying disk
java -jar target/JavaFunctionLogger-1.0.0.jar --dry-run /path/to/my-java-project

# Instrument with execution timing and automatic .bak backups
java -jar target/JavaFunctionLogger-1.0.0.jar --timing --backup /path/to/my-java-project

# Instrument excluding test classes
java -jar target/JavaFunctionLogger-1.0.0.jar --exclude ".*Test.*" /path/to/my-java-project
```

---

### 💻 CLI Options & Flags

| Flag | Description |
| :--- | :--- |
| `<directoryPath>` | Target directory to recursively scan and instrument (default: `.`) |
| `-b, --backup` | Create a `.bak` backup file before modifying files |
| `-d, --dry-run` | Preview modifications in console without writing changes to disk |
| `-e, --exclude=<regex>` | Regular expression for file paths to exclude from instrumentation |
| `-t, --timing` | Inject execution time measurement (`_methodStartTime`) |
| `-v, --verbose` | Verbose debug console logging |
| `-V, --version` | Print version information and exit |
| `-h, --help` | Show command line options and exit |

---

### 📄 License
MIT License. Created by [Tom7668h](https://github.com/Tom7668h).
