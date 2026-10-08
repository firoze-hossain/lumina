package dev.lumina.build;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in provider for Apache TomEE Enterprise Application Server.
 */
public class TomEEServerProvider implements ApplicationServerProvider {

    public static final String TYPE_ID = "tomee";

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getDisplayName() {
        return "TomEE Server";
    }

    @Override
    public Node createIcon() {
        Canvas canvas = new Canvas(16, 16);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // TomEE distinctive emblem
        gc.setFill(Color.web("#E5A158"));
        gc.fillRoundRect(1, 2, 14, 12, 4, 4);

        gc.setFill(Color.web("#3592C4"));
        gc.fillPolygon(new double[]{3, 8, 13}, new double[]{13, 6, 13}, 3);

        gc.setFill(Color.web("#C75450"));
        gc.fillOval(6, 4, 4, 4);

        return canvas;
    }

    @Override
    public String getDefaultName() {
        return "TomEE 9.1";
    }

    @Override
    public boolean validateHomePath(String path) {
        if (path == null || path.isBlank()) return false;
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) return false;
        File libDir = new File(dir, "lib");
        File binDir = new File(dir, "bin");
        return (libDir.exists() && new File(libDir, "tomee-common.jar").exists())
                || (binDir.exists() && new File(binDir, "catalina.sh").exists());
    }

    @Override
    public String detectVersion(String path) {
        if (path == null) return "TomEE 9.1.2";
        File dir = new File(path);
        String name = dir.getName().toLowerCase();
        if (name.contains("tomee-")) {
            return "TomEE " + name.replace("apache-tomee-", "").replace("tomee-", "");
        }
        return "TomEE 9.1.2";
    }

    @Override
    public List<String> detectLibraries(String path) {
        List<String> libs = new ArrayList<>();
        if (path != null) {
            File libDir = new File(path, "lib");
            if (libDir.isDirectory()) {
                File[] files = libDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".jar")) {
                            libs.add(f.getName());
                        }
                    }
                }
            }
        }
        if (libs.isEmpty()) {
            libs.add("tomee-common.jar");
            libs.add("openejb-core.jar");
            libs.add("catalina.jar");
            libs.add("javaee-api.jar");
            libs.add("servlet-api.jar");
        }
        return libs;
    }

    @Override
    public ApplicationServer createServer(String homePath) {
        ApplicationServer server = new ApplicationServer();
        server.setTypeId(getTypeId());
        server.setHomePath(homePath != null ? homePath : "");
        server.setName(getDefaultName());
        server.setVersion(detectVersion(homePath));
        server.setLibraries(detectLibraries(homePath));
        server.setBaseDirectory(homePath != null ? homePath : "");
        return server;
    }

    @Override
    public boolean supportsBaseDirectory() {
        return true;
    }
}
