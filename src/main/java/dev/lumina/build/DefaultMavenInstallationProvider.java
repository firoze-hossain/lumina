package dev.lumina.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Default provider for discovering Maven installations on the host system.
 */
public class DefaultMavenInstallationProvider implements MavenInstallationProvider {

    @Override
    public List<MavenInstallation> discoverInstallations() {
        List<MavenInstallation> list = new ArrayList<>();

        // 1. Bundled Maven 3 matching reference specification
        list.add(new MavenInstallation("Bundled (Maven 3)", "BUNDLED", "3.9.11", true));

        // 2. Maven Wrapper
        list.add(new MavenInstallation("Use Maven Wrapper", "WRAPPER", "3.9.11", false));

        // 3. System MAVEN_HOME or M2_HOME
        String envHome = System.getenv("MAVEN_HOME");
        if (envHome == null || envHome.isBlank()) {
            envHome = System.getenv("M2_HOME");
        }
        if (envHome != null && !envHome.isBlank()) {
            File f = new File(envHome);
            if (f.exists() && f.isDirectory()) {
                String v = detectVersion(envHome);
                list.add(new MavenInstallation(envHome, envHome, v, false));
            }
        }

        // 4. Common system installation locations
        String[] commonPaths = {
                "/usr/share/maven",
                "/usr/local/Cellar/maven",
                "/opt/homebrew/opt/maven",
                "/opt/maven",
                System.getProperty("user.home") + "/.sdkman/candidates/maven/current"
        };
        for (String p : commonPaths) {
            File f = new File(p);
            if (f.exists() && f.isDirectory() && !p.equals(envHome)) {
                String v = detectVersion(p);
                list.add(new MavenInstallation(p, p, v, false));
            }
        }

        return list;
    }

    @Override
    public String detectVersion(String homePath) {
        if (homePath == null || homePath.isBlank() || "Bundled (Maven 3)".equals(homePath) || "BUNDLED".equals(homePath) || "WRAPPER".equals(homePath)) {
            return "3.9.11";
        }
        File dir = new File(homePath);
        if (!dir.exists()) return null;

        // Try reading lib directory jar names
        File libDir = new File(dir, "lib");
        if (libDir.isDirectory()) {
            File[] files = libDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.startsWith("maven-core-") && name.endsWith(".jar")) {
                        String ver = name.substring("maven-core-".length(), name.length() - ".jar".length());
                        return ver;
                    }
                }
            }
        }

        String name = dir.getName().toLowerCase();
        if (name.contains("maven-") || name.contains("apache-maven-")) {
            return name.replace("apache-maven-", "").replace("maven-", "");
        }

        return null;
    }
}
