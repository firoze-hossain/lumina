package dev.lumina.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes Lumina's own project metadata folder, {@code .lumina/} \u2014
 * Lumina's equivalent of IntelliJ's {@code .idea/}, generated the same way
 * and for the same reasons, just under Lumina's own name instead of
 * borrowing IntelliJ's. Written the first time this project is opened, not
 * copied from a template: the Java version, build system, and VCS mapping
 * are all read from the actual project.
 *
 * <p>What each file is for (so this stays maintainable):
 * <ul>
 *   <li>{@code project.xml} \u2014 the project SDK / language level, and for
 *       Maven projects, which pom.xml Lumina treats as the project model
 *       (IntelliJ's misc.xml).</li>
 *   <li>{@code compiler.xml} \u2014 where annotation-processor output goes
 *       (Lombok, MapStruct, Spring's configuration-processor all need
 *       this) so generated sources are recognized as source roots.</li>
 *   <li>{@code encodings.xml} \u2014 pins UTF-8 for source/resource
 *       directories regardless of the OS default encoding.</li>
 *   <li>{@code jarRepositories.xml} \u2014 the remote Maven repositories
 *       Lumina offers when manually adding a dependency.</li>
 *   <li>{@code vcs.xml} \u2014 marks this directory as a Git repository,
 *       only written when a .git folder actually exists.</li>
 *   <li>{@code gradle.xml} \u2014 Gradle's equivalent of project.xml's Maven
 *       import marker, only written for Gradle projects.</li>
 *   <li>{@code .lumina/.gitignore} \u2014 keeps user-local, constantly
 *       churning files (workspace state, shelved changes, local
 *       datasource credentials) out of version control while the rest of
 *       .lumina/ stays committed and shared with the team \u2014 exactly the
 *       split IntelliJ uses for .idea/.</li>
 * </ul>
 * A workspace-state file (open editors, expanded tree) is deliberately not
 * generated here \u2014 that is genuinely user-local, and Lumina already keeps
 * its own per-project session separately under {@code ~/.lumina/sessions/}
 * (see {@link dev.lumina.util.ProjectSession}), the same split IntelliJ
 * draws between committed .idea/ files and its own local workspace.xml.
 */
public final class LuminaFolderGenerator {

    private LuminaFolderGenerator() {
    }

    /** Writes .lumina/ into dir if it doesn't already exist. Safe to call
     *  on every project open \u2014 a no-op once the folder is there. */
    public static void ensure(Path dir, String moduleName) {
        Path lumina = dir.resolve(".lumina");
        if (Files.isDirectory(lumina)) return;
        try {
            Files.createDirectories(lumina);
            boolean maven = Files.isRegularFile(dir.resolve("pom.xml"));
            boolean gradle = !maven && (Files.isRegularFile(dir.resolve("build.gradle"))
                    || Files.isRegularFile(dir.resolve("build.gradle.kts")));
            int javaVersion = detectJavaVersion(dir, maven, gradle);
            boolean git = Files.isDirectory(dir.resolve(".git"));

            write(lumina.resolve(".gitignore"), LUMINA_GITIGNORE);
            write(lumina.resolve("encodings.xml"), encodingsXml());
            write(lumina.resolve("jarRepositories.xml"), JAR_REPOSITORIES_XML);
            write(lumina.resolve("project.xml"), projectXml(javaVersion, maven));
            write(lumina.resolve("compiler.xml"), compilerXml(moduleName, gradle));
            if (git) {
                write(lumina.resolve("vcs.xml"), VCS_XML);
            }
            if (gradle) {
                write(lumina.resolve("gradle.xml"), gradleXml());
            }
        } catch (IOException ignored) {
            // .lumina/ is a convenience; failing to write it should never
            // block opening or working with the project
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------- detection

    private static int detectJavaVersion(Path dir, boolean maven, boolean gradle) {
        try {
            if (maven) {
                String pom = Files.readString(dir.resolve("pom.xml"));
                Matcher m = Pattern.compile(
                        "<(?:java\\.version|maven\\.compiler\\.release"
                                + "|maven\\.compiler\\.source)>\\s*(?:1\\.)?(\\d+)\\s*<")
                        .matcher(pom);
                if (m.find()) return Integer.parseInt(m.group(1));
            }
            if (gradle) {
                for (String name : new String[]{"build.gradle", "build.gradle.kts"}) {
                    Path f = dir.resolve(name);
                    if (!Files.isRegularFile(f)) continue;
                    String text = Files.readString(f);
                    Matcher m = Pattern.compile("JavaLanguageVersion\\.of\\((\\d+)\\)")
                            .matcher(text);
                    if (m.find()) return Integer.parseInt(m.group(1));
                    m = Pattern.compile("(?:sourceCompatibility|targetCompatibility)\\s*=\\s*"
                            + "['\"]?(?:JavaVersion\\.VERSION_)?(?:1\\.)?(\\d+)").matcher(text);
                    if (m.find()) return Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception ignored) {
        }
        return 25;   // Lumina's own default toolchain when nothing is configured
    }

    // ------------------------------------------------------------- templates

    private static String projectXml(int javaVersion, boolean maven) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project version=\"4\">\n")
                .append("  <component name=\"ExternalStorageConfigurationManager\" enabled=\"true\" />\n");
        if (maven) {
            sb.append("  <component name=\"MavenProjectsManager\">\n")
                    .append("    <option name=\"originalFiles\">\n      <list>\n")
                    .append("        <option value=\"$PROJECT_DIR$/pom.xml\" />\n")
                    .append("      </list>\n    </option>\n  </component>\n");
        }
        sb.append("  <component name=\"ProjectRootManager\" version=\"2\" languageLevel=\"JDK_")
                .append(javaVersion).append("\" default=\"true\" project-jdk-name=\"")
                .append(javaVersion).append("\" project-jdk-type=\"JavaSDK\" />\n</project>\n");
        return sb.toString();
    }

    private static String compilerXml(String moduleName, boolean gradle) {
        String sourceOut = gradle
                ? "build/generated/sources/annotationProcessor/java/main"
                : "target/generated-sources/annotations";
        String testOut = gradle
                ? "build/generated/sources/annotationProcessor/java/test"
                : "target/generated-test-sources/test-annotations";
        String profileName = gradle ? "Gradle Imported" : "Maven default annotation processors profile";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project version=\"4\">\n"
                + "  <component name=\"CompilerConfiguration\">\n"
                + "    <annotationProcessing>\n"
                + "      <profile name=\"" + profileName + "\" enabled=\"true\">\n"
                + "        <sourceOutputDir name=\"" + sourceOut + "\" />\n"
                + "        <sourceTestOutputDir name=\"" + testOut + "\" />\n"
                + "        <outputRelativeToContentRoot value=\"true\" />\n"
                + "        <module name=\"" + moduleName + "\" />\n"
                + "      </profile>\n"
                + "    </annotationProcessing>\n"
                + "  </component>\n</project>\n";
    }

    private static String encodingsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project version=\"4\">\n"
                + "  <component name=\"Encoding\">\n"
                + "    <file url=\"file://$PROJECT_DIR$/src/main/java\" charset=\"UTF-8\" />\n"
                + "    <file url=\"file://$PROJECT_DIR$/src/main/resources\" charset=\"UTF-8\" />\n"
                + "  </component>\n</project>\n";
    }

    private static String gradleXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project version=\"4\">\n"
                + "  <component name=\"GradleSettings\">\n"
                + "    <option name=\"linkedExternalProjectsSettings\">\n"
                + "      <GradleProjectSettings>\n"
                + "        <option name=\"testRunner\" value=\"PLATFORM\" />\n"
                + "        <option name=\"distributionType\" value=\"DEFAULT_WRAPPED\" />\n"
                + "        <option name=\"externalProjectPath\" value=\"$PROJECT_DIR$\" />\n"
                + "        <option name=\"modules\">\n          <set>\n"
                + "            <option value=\"$PROJECT_DIR$\" />\n"
                + "          </set>\n        </option>\n"
                + "        <option name=\"resolveModulePerSourceSet\" value=\"false\" />\n"
                + "      </GradleProjectSettings>\n"
                + "    </option>\n  </component>\n</project>\n";
    }

    private static final String VCS_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project version=\"4\">\n"
                    + "  <component name=\"VcsDirectoryMappings\">\n"
                    + "    <mapping directory=\"\" vcs=\"Git\" />\n"
                    + "  </component>\n</project>\n";

    private static final String JAR_REPOSITORIES_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project version=\"4\">\n"
                    + "  <component name=\"RemoteRepositoriesConfiguration\">\n"
                    + "    <remote-repository>\n"
                    + "      <option name=\"id\" value=\"central\" />\n"
                    + "      <option name=\"name\" value=\"Central Repository\" />\n"
                    + "      <option name=\"url\" value=\"https://repo.maven.apache.org/maven2\" />\n"
                    + "    </remote-repository>\n"
                    + "    <remote-repository>\n"
                    + "      <option name=\"id\" value=\"central\" />\n"
                    + "      <option name=\"name\" value=\"Maven Central repository\" />\n"
                    + "      <option name=\"url\" value=\"https://repo1.maven.org/maven2\" />\n"
                    + "    </remote-repository>\n"
                    + "  </component>\n</project>\n";

    private static final String LUMINA_GITIGNORE =
            "# Default ignored files\n/shelf/\n/workspace.xml\n"
                    + "# Ignored default folder with query files\n/queries/\n"
                    + "# Datasource local storage ignored files\n/dataSources/\n/dataSources.local.xml\n"
                    + "# Editor-based HTTP Client requests\n/httpRequests/\n";
}