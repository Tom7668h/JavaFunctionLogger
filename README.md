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
        // Business logic here
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

        // Business logic here
        return new OrderResult("SUCCESS");
    }
}
```

---

### 🚀 Schnellstart & CLI-Nutzung

#### 1. Kompilieren & Tests ausführen
```bash
git clone https://github.com/Tom7668h/JavaFunctionLogger.git
cd JavaFunctionLogger
mvn clean test
```

#### 2. Standalone Fat JAR erstellen
```bash
mvn package -DskipTests
```
Erzeugt `target/JavaFunctionLogger-1.0.0.jar`.

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

<br><br>

---

## 🇬🇧 English Documentation

An automated **Abstract Syntax Tree (AST)** code instrumentation and observability tool built with **Java 17 LTS**, **JavaParser**, and **Picocli**. It recursively parses Java source trees, injects `Log4j2` loggers, and adds runtime method trace statements (with parameter values, PII redaction, array formatting, and execution timing) without breaking compilation or code formatting.

---

### 🎯 Key Highlights & Engineering Features

- **True AST Manipulation (Not Fragile Regex):**
  Uses **`JavaParser`** to build a full syntactic model of Java source files. Handles generics, annotations, overloaded methods, interfaces, and nested classes accurately.
- **Safe Logger Injection:**
  Detects whether a class already has a `Logger` field or imports. If missing, cleanly adds `LogManager` / `Logger` imports and injects logger declarations into the class body.
- **Dynamic Parameter Tracing & Array Formatting:**
  Inspects method parameters and generates structured trace statements with proper array formatting.
- **PII & Secret Redaction (Security Compliant):**
  Automatically masks sensitive arguments (`password`, `secret`, `token`, `key`, `pin`, etc.) with `"[REDACTED]"` to prevent credential leakage into application logs.
- **Infinite Recursion Guard:**
  Automatically skips `toString()`, `hashCode()`, `equals()`, `finalize()`, and `clone()`.
- **Execution-Time Profiling (`--timing`):**
  Injects high-precision timestamps (`System.currentTimeMillis()`) to profile slow methods.
- **Non-Destructive Backups (`--backup`):**
  Generates a `.bak` backup copy before modifying files.

---

### 🚀 Quickstart

```bash
git clone https://github.com/Tom7668h/JavaFunctionLogger.git
cd JavaFunctionLogger
mvn clean test
mvn package -DskipTests
java -jar target/JavaFunctionLogger-1.0.0.jar --dry-run /path/to/project
```

---

### 📄 License
MIT License. Created by [Tom7668h](https://github.com/Tom7668h).
