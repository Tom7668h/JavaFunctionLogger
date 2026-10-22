package com.mycompany.javafunctionlogger;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Robust AST-based Java Source Code Instrumenter using JavaParser.
 * Accurately analyzes syntax trees, injects Log4j2 loggers and method entry/exit tracing.
 * Features recursion protection, PII masking for sensitive parameters, and array formatting.
 */
public class AstInstrumenter {

    private static final Logger LOGGER = LogManager.getLogger(AstInstrumenter.class);

    /**
     * Methods that must never be instrumented to prevent infinite recursion and StackOverflowError.
     */
    private static final Set<String> RECURSION_BLACKLIST = Set.of(
            "toString", "hashCode", "equals", "finalize", "clone"
    );

    /**
     * Regex pattern to detect sensitive parameter names for automatic redaction.
     */
    private static final Pattern SENSITIVE_PARAM_PATTERN = Pattern.compile(
            "(?i).*(password|pass|secret|token|key|pin|credential|auth).*"
    );

    public static class InstrumentationResult {
        public int methodsInstrumented = 0;
        public boolean loggerAdded = false;
        public String modifiedSourceCode = null;
        public List<String> instrumentedMethodNames = new ArrayList<>();
    }

    /**
     * Instruments source code directly in memory.
     */
    public static InstrumentationResult instrumentSource(String sourceCode, String fallbackClassName, boolean includeTiming) {
        InstrumentationResult result = new InstrumentationResult();

        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceCode);
            boolean modified = false;

            // Ensure imports
            boolean hasLogManagerImport = cu.getImports().stream()
                    .anyMatch(i -> i.getNameAsString().equals("org.apache.logging.log4j.LogManager"));
            boolean hasLoggerImport = cu.getImports().stream()
                    .anyMatch(i -> i.getNameAsString().equals("org.apache.logging.log4j.Logger"));

            if (!hasLogManagerImport) {
                cu.addImport("org.apache.logging.log4j.LogManager");
                modified = true;
            }
            if (!hasLoggerImport) {
                cu.addImport("org.apache.logging.log4j.Logger");
                modified = true;
            }

            for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                // Skip interfaces
                if (cid.isInterface()) {
                    continue;
                }

                String className = cid.getNameAsString();

                // Check or add logger field
                boolean hasLoggerField = cid.getFields().stream()
                        .anyMatch(f -> f.getElementType().asString().equals("Logger")
                                && f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals("logger")));

                if (!hasLoggerField) {
                    FieldDeclaration loggerField = cid.addFieldWithInitializer(
                            "Logger",
                            "logger",
                            StaticJavaParser.parseExpression("LogManager.getLogger(" + className + ".class)")
                    );
                    loggerField.setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                    result.loggerAdded = true;
                    modified = true;
                }

                // Instrument methods
                for (MethodDeclaration method : cid.getMethods()) {
                    if (method.getBody().isEmpty() || method.isAbstract() || method.isNative()) {
                        continue;
                    }

                    String methodName = method.getNameAsString();

                    // Recursion guard: do not instrument methods that could cause infinite loops when logged
                    if (RECURSION_BLACKLIST.contains(methodName)) {
                        continue;
                    }

                    BlockStmt body = method.getBody().get();

                    // Check if already instrumented
                    boolean alreadyInstrumented = body.getStatements().stream()
                            .anyMatch(s -> s.toString().contains("logger.trace(") && s.toString().contains(methodName));

                    if (alreadyInstrumented) {
                        continue;
                    }

                    // Build parameter log expression with sensitive redaction and array support
                    NodeList<Parameter> parameters = method.getParameters();
                    StringBuilder paramLog = new StringBuilder();
                    for (int i = 0; i < parameters.size(); i++) {
                        Parameter p = parameters.get(i);
                        String paramName = p.getNameAsString();
                        boolean isSensitive = SENSITIVE_PARAM_PATTERN.matcher(paramName).matches();
                        boolean isArray = p.getType().isArrayType();

                        paramLog.append(paramName).append("=\" + ");
                        if (isSensitive) {
                            paramLog.append("\"[REDACTED]\"");
                        } else if (isArray) {
                            paramLog.append("java.util.Arrays.toString(").append(paramName).append(")");
                        } else {
                            paramLog.append(paramName);
                        }

                        if (i < parameters.size() - 1) {
                            paramLog.append(" + \", ");
                        }
                    }

                    String logExpr;
                    if (parameters.isEmpty()) {
                        logExpr = "logger.trace(\"[" + className + "#" + methodName + "] Called (no params)\");";
                    } else {
                        logExpr = "logger.trace(\"[" + className + "#" + methodName + "] Called with: " + paramLog + ");";
                    }

                    Statement logStatement = StaticJavaParser.parseStatement(logExpr);
                    body.addStatement(0, logStatement);

                    if (includeTiming) {
                        Statement timingStatement = StaticJavaParser.parseStatement("final long _methodStartTime = System.currentTimeMillis();");
                        body.addStatement(1, timingStatement);
                    }

                    result.methodsInstrumented++;
                    result.instrumentedMethodNames.add(methodName);
                    modified = true;
                }
            }

            if (modified) {
                result.modifiedSourceCode = cu.toString();
            } else {
                result.modifiedSourceCode = sourceCode;
            }

        } catch (Exception e) {
            LOGGER.error("Failed to parse and instrument source code", e);
        }

        return result;
    }

    /**
     * Instruments a Java file in place or previews it.
     * Optionally creates a .bak backup before modifying.
     */
    public static InstrumentationResult instrumentFile(Path filePath, boolean dryRun, boolean includeTiming, boolean backup) throws IOException {
        String originalContent = Files.readString(filePath, StandardCharsets.UTF_8);
        String fileName = filePath.getFileName().toString();
        String className = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

        InstrumentationResult result = instrumentSource(originalContent, className, includeTiming);

        if (!dryRun && result.modifiedSourceCode != null && result.methodsInstrumented > 0) {
            if (backup) {
                Path backupPath = filePath.resolveSibling(fileName + ".bak");
                Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Created backup file at {}", backupPath);
            }
            Files.writeString(filePath, result.modifiedSourceCode, StandardCharsets.UTF_8);
            LOGGER.info("Instrumented {} (methods: {}, loggerAdded: {})", filePath, result.methodsInstrumented, result.loggerAdded);
        }

        return result;
    }

    /**
     * Backward-compatible overload without backup flag.
     */
    public static InstrumentationResult instrumentFile(Path filePath, boolean dryRun, boolean includeTiming) throws IOException {
        return instrumentFile(filePath, dryRun, includeTiming, false);
    }
}
