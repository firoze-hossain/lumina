package dev.lumina.project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Creates new projects on disk.
 * JAVA        -> generated locally (Maven or Gradle layout + Main class).
 * SPRING_BOOT -> downloaded from start.spring.io (same service IntelliJ uses).
 */
public final class ProjectGenerator {

    private ProjectGenerator() {
    }

    /** Runs on a background thread; log lines go to the console. */
    public static Path generate(ProjectSpec spec, Consumer<String> log) throws IOException {
        Path dir = spec.projectDir();
        if (Files.isDirectory(dir)) {
            try (var entries = Files.list(dir)) {
                if (entries.findAny().isPresent()) {
                    throw new IOException("Directory is not empty: " + dir);
                }
            }
        }
        Files.createDirectories(dir);

        switch (spec.generator()) {
            case JAVA -> generateJava(spec, dir, log);
            case SPRING_BOOT -> generateSpringBoot(spec, dir, log);
        }

        if (spec.initGit()) {
            initGit(dir, log);
        }
        log.accept("\u2713 Project created at " + dir);
        return dir;
    }

    // ------------------------------------------------------------ plain java

    private static void generateJava(ProjectSpec spec, Path dir, Consumer<String> log)
            throws IOException {
        log.accept("Generating Java project (" + spec.buildSystem() + ") \u2026");

        String pkg = spec.packageName();
        Path srcMain = dir.resolve("src/main/java");
        Path pkgDir = pkg.isBlank() ? srcMain : srcMain.resolve(pkg.replace('.', '/'));
        Files.createDirectories(pkgDir);
        Files.createDirectories(dir.resolve("src/main/resources"));
        Files.createDirectories(dir.resolve("src/test/java"));

        String pkgLine = pkg.isBlank() ? "" : "package " + pkg + ";\n\n";
        Files.writeString(pkgDir.resolve("Main.java"), pkgLine + """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello from %s!");
                    }
                }
                """.formatted(spec.name()));

        if (spec.buildSystem() == ProjectSpec.BuildSystem.MAVEN) {
            Files.writeString(dir.resolve("pom.xml"), """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>%s</groupId>
                        <artifactId>%s</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <properties>
                            <maven.compiler.release>%s</maven.compiler.release>
                            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                        </properties>
                    </project>
                    """.formatted(spec.group(), spec.artifact(), spec.javaVersion()));
        } else {
            Files.writeString(dir.resolve("settings.gradle"),
                    "rootProject.name = '" + spec.artifact() + "'\n");
            Files.writeString(dir.resolve("build.gradle"), """
                    plugins {
                        id 'java'
                        id 'application'
                    }

                    group = '%s'
                    version = '1.0-SNAPSHOT'

                    java {
                        toolchain {
                            languageVersion = JavaLanguageVersion.of(%s)
                        }
                    }

                    repositories {
                        mavenCentral()
                    }

                    application {
                        mainClass = '%s'
                    }
                    """.formatted(spec.group(), spec.javaVersion(),
                    pkg.isBlank() ? "Main" : pkg + ".Main"));
        }

        Files.writeString(dir.resolve(".gitignore"), """
                target/
                build/
                .gradle/
                .idea/
                *.class
                *.log
                """);
        Files.writeString(dir.resolve("README.md"),
                "# " + spec.name() + "\n\nCreated with Lumina IDE.\n");
    }

    // ----------------------------------------------------------- spring boot

    private static void generateSpringBoot(ProjectSpec spec, Path dir, Consumer<String> log)
            throws IOException {
        String type = spec.buildSystem() == ProjectSpec.BuildSystem.MAVEN
                ? "maven-project" : "gradle-project";

        StringBuilder url = new StringBuilder("https://start.spring.io/starter.zip")
                .append("?type=").append(type)
                .append("&language=java")
                .append("&javaVersion=").append(enc(spec.javaVersion()))
                .append("&groupId=").append(enc(spec.group()))
                .append("&artifactId=").append(enc(spec.artifact()))
                .append("&name=").append(enc(spec.name()))
                .append("&packageName=").append(enc(spec.packageName()));
        String deps = spec.springDependencies() == null ? "" : spec.springDependencies()
                .replace(" ", "");
        if (!deps.isBlank()) {
            url.append("&dependencies=").append(enc(deps));
        }

        log.accept("Requesting project from start.spring.io \u2026");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new IOException("start.spring.io returned HTTP " + response.statusCode()
                    + (body.isBlank() ? "" : "\n" + shorten(body)));
        }

        log.accept("Unpacking " + (response.body().length / 1024) + " KB \u2026");
        unzip(response.body(), dir);

        // ZipInputStream drops Unix permissions: restore the wrappers' exec bit.
        for (String wrapper : new String[]{"mvnw", "gradlew"}) {
            Path w = dir.resolve(wrapper);
            if (Files.isRegularFile(w) && !w.toFile().setExecutable(true)) {
                log.accept("Note: could not mark " + wrapper + " executable");
            }
        }
    }

    private static void unzip(byte[] zipBytes, Path target) throws IOException {
        Path root = target.toAbsolutePath().normalize();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = root.resolve(entry.getName()).normalize();
                if (!out.startsWith(root)) {
                    throw new IOException("Blocked zip entry outside target: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out);
                }
                zis.closeEntry();
            }
        }
    }

    // ------------------------------------------------------------------ git

    private static void initGit(Path dir, Consumer<String> log) {
        try {
            Process p = new ProcessBuilder("git", "init")
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();
            p.getInputStream().readAllBytes();
            int code = p.waitFor();
            log.accept(code == 0 ? "Initialized git repository"
                    : "git init exited with code " + code);
        } catch (IOException e) {
            log.accept("git not found on PATH \u2014 skipped repository init");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------- helpers

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String shorten(String s) {
        return s.length() > 400 ? s.substring(0, 400) + "\u2026" : s;
    }
}
