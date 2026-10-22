package com.mycompany.javafunctionlogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Enterprise JavaFunctionLogger CLI
 */
@Command(
    name = "java-function-logger",
    mixinStandardHelpOptions = true,
    version = "JavaFunctionLogger 1.0.0",
    description = "Automatically inspects Java source trees, injects Log4j2 loggers and method trace instrumentation using JavaParser AST."
)
public class JavaFunctionLogger implements Callable<Integer> {

    private static final Logger LOGGER = LogManager.getLogger(JavaFunctionLogger.class);

    @Parameters(index = "0", description = "Target directory to recursively scan and instrument.", defaultValue = ".")
    private String directoryPath = ".";

    @Option(names = {"-d", "--dry-run"}, description = "Preview modifications without saving changes to disk.")
    private boolean dryRun = false;

    @Option(names = {"-t", "--timing"}, description = "Inject execution time measurement at method entry.")
    private boolean includeTiming = false;

    @Option(names = {"-b", "--backup"}, description = "Create a .bak backup file before modifying files.")
    private boolean backup = false;

    @Option(names = {"-e", "--exclude"}, description = "Regular expression for file paths to exclude from instrumentation.")
    private String excludeRegex = null;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose console logging.")
    private boolean verbose = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JavaFunctionLogger()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        printBanner();

        Path rootPath = Paths.get(directoryPath).toAbsolutePath().normalize();
        System.out.println("🔍 Scanning directory: " + rootPath);
        System.out.println("⚙️  Mode: " + (dryRun ? "DRY-RUN (No files will be modified)" : "LIVE MUTATION"));
        System.out.println("⏱️  Execution Timing: " + (includeTiming ? "ENABLED" : "DISABLED"));
        System.out.println("💾 File Backup (.bak): " + (backup ? "ENABLED" : "DISABLED"));
        if (excludeRegex != null) {
            System.out.println("🚫 Exclusion Filter: " + excludeRegex);
        }
        System.out.println("------------------------------------------------------------");

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            System.err.println("❌ Error: Directory does not exist or is not a directory: " + rootPath);
            return 1;
        }

        int[] totalFiles = {0};
        int[] modifiedFiles = {0};
        int[] totalMethods = {0};

        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .filter(p -> !p.toString().contains("target") && !p.toString().contains(".git"))
                  .filter(p -> excludeRegex == null || !p.toString().matches(excludeRegex))
                  .forEach(filePath -> {
                      totalFiles[0]++;
                      try {
                          AstInstrumenter.InstrumentationResult res = AstInstrumenter.instrumentFile(filePath, dryRun, includeTiming, backup);
                          if (res.methodsInstrumented > 0) {
                              modifiedFiles[0]++;
                              totalMethods[0] += res.methodsInstrumented;
                              System.out.println("  ✓ [" + (dryRun ? "PREVIEW" : "MODIFIED") + "] " + rootPath.relativize(filePath) 
                                      + " (" + res.methodsInstrumented + " methods: " + String.join(", ", res.instrumentedMethodNames) + ")");
                          } else if (verbose) {
                              System.out.println("  - [SKIPPED] " + rootPath.relativize(filePath) + " (no uninstrumented methods)");
                          }
                      } catch (Exception e) {
                          System.err.println("  ✗ [ERROR] Failed to process " + filePath + ": " + e.getMessage());
                      }
                  });

            System.out.println("------------------------------------------------------------");
            System.out.println("📊 Instrumentation Summary:");
            System.out.println("   • Total .java Files Scanned: " + totalFiles[0]);
            System.out.println("   • Files Modified / Instrumented: " + modifiedFiles[0]);
            System.out.println("   • Total Methods Injected: " + totalMethods[0]);
            System.out.println(dryRun ? "✨ Dry-run finished. No disk changes made." : "✅ All files instrumented successfully.");

            return 0;
        } catch (IOException e) {
            System.err.println("Fatal I/O Error: " + e.getMessage());
            return 1;
        }
    }

    private void printBanner() {
        System.out.println("""
           ___                  ______                 _   _               _                               
          |_  |                 |  ___|               | | (_)             | |                              
            | | __ ___   ____ _ | |_ _   _ _ __   ___ | |_ _  ___  _ __   | |     ___   __ _  __ _  ___ _ __ 
            | |/ _` \\ \\ / / _` ||  _| | | | '_ \\ / __|| __| |/ _ \\| '_ \\  | |    / _ \\ / _` |/ _` |/ _ \\ '__|
        /\\__/ / (_| |\\ V / (_| || | | |_| | | | | (__ | |_| | (_) | | | | | |___| (_) | (_| | (_| |  __/ |   
        \\____/ \\__,_| \\_/ \\__,_|\\_|  \\__,_|_| |_|\\___| \\__|_|\\___/|_| |_| \\_____/\\___/ \\__, |\\__, |\\___|_|   
                                                                                        __/ | __/ |          
                                                                                       |___/ |___/           
        """);
    }
}
