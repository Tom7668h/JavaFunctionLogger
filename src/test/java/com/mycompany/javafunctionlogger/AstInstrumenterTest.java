package com.mycompany.javafunctionlogger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AST Code Instrumenter Tests")
public class AstInstrumenterTest {

    @Test
    @DisplayName("Should inject Log4j2 logger and method trace into plain Java class")
    public void testBasicClassInstrumentation() {
        String source = """
            package test;
            
            public class UserService {
                public String findUser(int id, String tenant) {
                    return "User-" + id;
                }
            }
            """;

        AstInstrumenter.InstrumentationResult result = AstInstrumenter.instrumentSource(source, "UserService", false);

        assertNotNull(result.modifiedSourceCode);
        assertEquals(1, result.methodsInstrumented);
        assertTrue(result.loggerAdded);
        assertTrue(result.modifiedSourceCode.contains("import org.apache.logging.log4j.Logger;"));
        assertTrue(result.modifiedSourceCode.contains("private static final Logger logger = LogManager.getLogger(UserService.class);"));
        assertTrue(result.modifiedSourceCode.contains("logger.trace(\"[UserService#findUser] Called with: id=\" + id + \", tenant=\" + tenant);"));
    }

    @Test
    @DisplayName("Should skip interface declarations without crashing")
    public void testInterfaceSkipping() {
        String source = """
            package test;
            
            public interface UserRepository {
                String getUser(int id);
            }
            """;

        AstInstrumenter.InstrumentationResult result = AstInstrumenter.instrumentSource(source, "UserRepository", false);

        assertEquals(0, result.methodsInstrumented);
        assertFalse(result.loggerAdded);
    }

    @Test
    @DisplayName("Should inject execution timing when requested")
    public void testTimingInjection() {
        String source = """
            package test;
            
            public class PaymentProcessor {
                public void process(double amount) {
                    System.out.println("Processing: " + amount);
                }
            }
            """;

        AstInstrumenter.InstrumentationResult result = AstInstrumenter.instrumentSource(source, "PaymentProcessor", true);

        assertEquals(1, result.methodsInstrumented);
        assertTrue(result.modifiedSourceCode.contains("final long _methodStartTime = System.currentTimeMillis();"));
    }

    @Test
    @DisplayName("Idempotency: Re-instrumenting existing code must not duplicate logging statements")
    public void testIdempotency() {
        String source = """
            package test;
            
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }
            }
            """;

        AstInstrumenter.InstrumentationResult firstPass = AstInstrumenter.instrumentSource(source, "Calculator", false);
        assertEquals(1, firstPass.methodsInstrumented);

        // Second pass
        AstInstrumenter.InstrumentationResult secondPass = AstInstrumenter.instrumentSource(firstPass.modifiedSourceCode, "Calculator", false);
        assertEquals(0, secondPass.methodsInstrumented, "Already instrumented method should not be modified again");
    }

    @Test
    @DisplayName("Recursion Guard: toString, hashCode, equals must be skipped to avoid infinite loops")
    public void testRecursionGuardSkipping() {
        String source = """
            package test;
            
            public class UserEntity {
                private String name;
                
                @Override
                public String toString() {
                    return "UserEntity{name='" + name + "'}";
                }
                
                @Override
                public int hashCode() {
                    return name != null ? name.hashCode() : 0;
                }
                
                @Override
                public boolean equals(Object obj) {
                    return this == obj;
                }
                
                public void updateName(String newName) {
                    this.name = newName;
                }
            }
            """;

        AstInstrumenter.InstrumentationResult result = AstInstrumenter.instrumentSource(source, "UserEntity", false);
        assertEquals(1, result.methodsInstrumented, "Only updateName should be instrumented, avoiding toString/hashCode/equals");
        assertEquals("updateName", result.instrumentedMethodNames.get(0));
        assertFalse(result.modifiedSourceCode.contains("[UserEntity#toString]"));
        assertFalse(result.modifiedSourceCode.contains("[UserEntity#hashCode]"));
        assertFalse(result.modifiedSourceCode.contains("[UserEntity#equals]"));
    }

    @Test
    @DisplayName("Security & PII: Sensitive parameters must be automatically redacted in log statements")
    public void testSensitiveParameterRedaction() {
        String source = """
            package test;
            
            public class AuthService {
                public boolean login(String username, String password, String secretToken) {
                    return true;
                }
            }
            """;

        AstInstrumenter.InstrumentationResult result = AstInstrumenter.instrumentSource(source, "AuthService", false);
        assertEquals(1, result.methodsInstrumented);
        assertTrue(result.modifiedSourceCode.contains("username=\" + username"));
        assertTrue(result.modifiedSourceCode.contains("password=\" + \"[REDACTED]\""));
        assertTrue(result.modifiedSourceCode.contains("secretToken=\" + \"[REDACTED]\""));
        assertFalse(result.modifiedSourceCode.contains("password=\" + password"));
    }

    @Test
    @DisplayName("Array Formatting: Array parameters must be wrapped in java.util.Arrays.toString")
    public void testArrayParameterFormatting() {
        String source = """
            package test;
            
            public class BatchProcessor {
                public void process(int[] ids, String[] tags) {
                }
            }
            """;

        AstInstrumenter.InstrumentationResult result = AstInstrumenter.instrumentSource(source, "BatchProcessor", false);
        assertEquals(1, result.methodsInstrumented);
        assertTrue(result.modifiedSourceCode.contains("ids=\" + java.util.Arrays.toString(ids)"));
        assertTrue(result.modifiedSourceCode.contains("tags=\" + java.util.Arrays.toString(tags)"));
    }

    @Test
    @DisplayName("Backup: instrumentFile with backup=true creates .bak before modification")
    public void testFileBackupCreation() throws java.io.IOException {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("TestClass", ".java");
        java.nio.file.Path backupFile = tempFile.resolveSibling(tempFile.getFileName().toString() + ".bak");
        
        try {
            String source = """
                public class TempService {
                    public void execute() {
                    }
                }
                """;
            java.nio.file.Files.writeString(tempFile, source);

            AstInstrumenter.InstrumentationResult result = AstInstrumenter.instrumentFile(tempFile, false, false, true);
            assertEquals(1, result.methodsInstrumented);
            assertTrue(java.nio.file.Files.exists(backupFile), "Backup file should be created");
            assertEquals(source, java.nio.file.Files.readString(backupFile), "Backup should contain original uninstrumented code");
            assertTrue(java.nio.file.Files.readString(tempFile).contains("logger.trace"), "Original file should contain trace");
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
            java.nio.file.Files.deleteIfExists(backupFile);
        }
    }
}
