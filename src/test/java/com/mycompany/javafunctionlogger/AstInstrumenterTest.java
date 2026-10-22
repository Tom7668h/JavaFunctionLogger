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
}
