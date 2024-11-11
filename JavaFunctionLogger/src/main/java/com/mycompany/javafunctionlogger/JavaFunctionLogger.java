/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.javafunctionlogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaFunctionLogger {

    private static final Logger logger = LogManager.getLogger(JavaFunctionLogger.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            logger.error("Bitte geben Sie das zu durchsuchende Verzeichnis als Argument an.");
            return;
        }
        String directoryPath = args[0];
        try {
            searchJavaFiles(directoryPath);
        } catch (IOException e) {
            logger.error("Fehler beim Durchsuchen des Verzeichnisses: ", e);
        }
    }

    private static void searchJavaFiles(String directoryPath) throws IOException {
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(JavaFunctionLogger::processJavaFile);
    }

    private static void processJavaFile(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            String className = extractClassName(filePath);
            ensureLogger(lines, className);
            analyzeFunctions(lines, className);
        } catch (IOException e) {
            logger.error("Fehler beim Verarbeiten der Datei: " + filePath, e);
        }
    }

    private static void ensureLogger(List<String> lines, String className) throws IOException {
        boolean hasLogger = lines.stream().anyMatch(line -> line.contains("Logger logger = LogManager.getLogger"));
        if (!hasLogger) {
            lines.add(1, "import org.apache.logging.log4j.LogManager;");
            lines.add(2, "import org.apache.logging.log4j.Logger;");
            lines.add(3, "private static final Logger logger = LogManager.getLogger(" + className + ".class);");
            Files.write(Paths.get(className + ".java"), lines);
            logger.info("Logger zu " + className + " hinzugefügt.");
        }
    }

    private static String extractClassName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private static void analyzeFunctions(List<String> lines, String className) {
        Pattern methodPattern = Pattern.compile("(public|protected|private|static|\\s)+\\s+[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\(([^)]*)\\)");
        for (String line : lines) {
            Matcher matcher = methodPattern.matcher(line);
            if (matcher.find()) {
                String modifiers = matcher.group(1);
                String returnType = matcher.group(2);
                String methodName = matcher.group(3);
                String parameters = matcher.group(4);
                
                logFunctionDetails(className, modifiers, returnType, methodName, parameters);
            }
        }
    }

    private static void logFunctionDetails(String className, String modifiers, String returnType, String methodName, String parameters) {
        logger.trace("Class: " + className);
        logger.trace("Function: " + methodName);
        logger.trace("Modifiers: " + modifiers);
        logger.trace("Return Type: " + returnType);
        logger.trace("Parameters: " + parameters);
    }
}
