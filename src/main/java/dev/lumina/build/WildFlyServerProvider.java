package dev.lumina.build;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in provider for JBoss and WildFly Application Servers.
 */
public class WildFlyServerProvider implements ApplicationServerProvider {

    public static final String TYPE_ID = "wildfly";

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getDisplayName() {
        return "JBoss/WildFly Server";
    }

    @Override
    public Node createIcon() {
        Canvas canvas = new Canvas(16, 16);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Distinctive red and blue WildFly icon
        gc.setFill(Color.web("#E25950"));
        gc.fillOval(1, 1, 14, 14);

        gc.setFill(Color.web("#3F8EE9"));
        gc.fillArc(2, 2, 12, 12, 45, 180, javafx.scene.shape.ArcType.ROUND);

        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(5, 5, 6, 6);

        return canvas;
    }

    @Override
    public String getDefaultName() {
        return "WildFly Server";
    }

    @Override
    public boolean validateHomePath(String path) {
        if (path == null || path.isBlank()) return false;
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) return false;
        File modulesJar = new File(dir, "jboss-modules.jar");
        File standaloneDir = new File(dir, "standalone");
        File binDir = new File(dir, "bin");
        return modulesJar.exists() || (standaloneDir.exists() && binDir.exists());
    }

    @Override
    public String detectVersion(String path) {
        if (path == null) return "WildFly 31.0.0";
        File dir = new File(path);
        String name = dir.getName().toLowerCase();
        if (name.contains("wildfly-")) {
            return "WildFly " + name.replace("wildfly-", "").replace(".final", "");
        }
        if (name.contains("jboss-")) {
            return "JBoss " + name.replace("jboss-", "");
        }
        return "WildFly 31.0.0";
    }

    @Override
    public List<String> detectLibraries(String path) {
        List<String> libs = new ArrayList<>();
        if (path != null) {
            File dir = new File(path);
            File modulesJar = new File(dir, "jboss-modules.jar");
            if (modulesJar.exists()) {
                libs.add(modulesJar.getName());
            }
            File modulesDir = new File(dir, "modules");
            if (modulesDir.isDirectory()) {
                findJars(modulesDir, libs, 0, 3);
            }
        }
        if (libs.isEmpty()) {
            libs.add("jboss-modules.jar");
            libs.add("wildfly-client-all.jar");
            libs.add("wildfly-ee.jar");
            libs.add("undertow-core.jar");
        }
        return libs;
    }

    private void findJars(File dir, List<String> result, int depth, int maxDepth) {
        if (depth > maxDepth || result.size() >= 50) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                findJars(f, result, depth + 1, maxDepth);
            } else if (f.getName().endsWith(".jar")) {
                result.add(f.getName());
            }
        }
    }

    @Override
    public ApplicationServer createServer(String homePath) {
        ApplicationServer server = new ApplicationServer();
        server.setTypeId(getTypeId());
        server.setHomePath(homePath != null ? homePath : "");
        server.setName(getDefaultName());
        server.setVersion(detectVersion(homePath));
        server.setLibraries(detectLibraries(homePath));
        server.setBaseDirectory(homePath != null ? homePath + "/standalone" : "");
        return server;
    }

    @Override
    public boolean supportsBaseDirectory() {
        return true;
    }
}
