package dev.lumina.run;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A runnable configuration for the toolbar dropdown, plus detection logic.
 * commands == null means "run the current editor file".
 */
public record RunConfiguration(String label, List<List<String>> commands, Path workDir) {

    public static final String CURRENT_FILE = "Current File";

    public static RunConfiguration currentFile() {
        return new RunConfiguration(CURRENT_FILE, null, null);
    }

    @Override
    public String toString() {
        return label;
    }

    // -------------------------------------------------------------- detection

    /** Inspect a project folder and propose run configurations. */
    public static List<RunConfiguration> detect(Path root) {
        List<RunConfiguration> configs = new ArrayList<>();
        configs.add(currentFile());
        if (root == null) return configs;

        Path pom = root.resolve("pom.xml");
        if (Files.isRegularFile(pom)) {
            String content = readQuiet(pom);
            if (content.contains("spring-boot")) {
                configs.add(new RunConfiguration(
                        "Spring Boot \u2014 " + root.getFileName(),
                        List.of(maven(root, "spring-boot:run")), root));
            }
        }

        Path gradle = firstExisting(root, "build.gradle", "build.gradle.kts");
        if (gradle != null) {
            String content = readQuiet(gradle);
            if (content.contains("org.springframework.boot")) {
                configs.add(new RunConfiguration(
                        "Spring Boot \u2014 " + root.getFileName(),
                        List.of(gradleCmd(root, "bootRun")), root));
            } else if (content.contains("application")) {
                configs.add(new RunConfiguration(
                        "Gradle run \u2014 " + root.getFileName(),
                        List.of(gradleCmd(root, "run")), root));
            }
        }
        return configs;
    }

    public static boolean isMavenProject(Path root) {
        return root != null && Files.isRegularFile(root.resolve("pom.xml"));
    }

    public static boolean isGradleProject(Path root) {
        return root != null && firstExisting(root, "build.gradle", "build.gradle.kts") != null;
    }

    /** Commands to compile the project, then run one main class on its classpath. */
    public static List<List<String>> compileAndRunClass(Path root, String fqcn) {
        if (isMavenProject(root)) {
            return List.of(
                    maven(root, "-q", "compile"),
                    List.of(javaBin(), "-cp", root.resolve("target/classes").toString(), fqcn));
        }
        if (isGradleProject(root)) {
            return List.of(
                    gradleCmd(root, "classes"),
                    List.of(javaBin(), "-cp",
                            root.resolve("build/classes/java/main").toString(), fqcn));
        }
        return null;
    }

    public static List<List<String>> buildProject(Path root) {
        if (isMavenProject(root)) return List.of(maven(root, "compile"));
        if (isGradleProject(root)) return List.of(gradleCmd(root, "build", "-x", "test"));
        return null;
    }

    public static List<List<String>> cleanProject(Path root) {
        if (isMavenProject(root)) return List.of(maven(root, "clean"));
        if (isGradleProject(root)) return List.of(gradleCmd(root, "clean"));
        return null;
    }

    // ------------------------------------------------------ command builders

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static String javaBin() {
        return Path.of(System.getProperty("java.home"), "bin",
                windows() ? "java.exe" : "java").toString();
    }

    /** Prefer the project's Maven wrapper, fall back to mvn on PATH. */
    public static List<String> maven(Path root, String... args) {
        List<String> cmd = new ArrayList<>();
        if (windows()) {
            cmd.add("cmd");
            cmd.add("/c");
            cmd.add(Files.isRegularFile(root.resolve("mvnw.cmd")) ? "mvnw.cmd" : "mvn");
        } else {
            cmd.add(Files.isRegularFile(root.resolve("mvnw"))
                    ? root.resolve("mvnw").toString() : "mvn");
        }
        cmd.addAll(List.of(args));
        return cmd;
    }

    public static List<String> gradleCmd(Path root, String... args) {
        List<String> cmd = new ArrayList<>();
        if (windows()) {
            cmd.add("cmd");
            cmd.add("/c");
            cmd.add(Files.isRegularFile(root.resolve("gradlew.bat")) ? "gradlew.bat" : "gradle");
        } else {
            cmd.add(Files.isRegularFile(root.resolve("gradlew"))
                    ? root.resolve("gradlew").toString() : "gradle");
        }
        cmd.addAll(List.of(args));
        return cmd;
    }

    // -------------------------------------------------------------- helpers

    private static Path firstExisting(Path root, String... names) {
        for (String n : names) {
            Path p = root.resolve(n);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    private static String readQuiet(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            return "";
        }
    }
}
