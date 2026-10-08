package dev.lumina.build;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in provider for Apache Tomcat Application Server.
 */
public class TomcatServerProvider implements ApplicationServerProvider {

    public static final String TYPE_ID = "tomcat";

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getDisplayName() {
        return "Tomcat Server";
    }

    @Override
    public Node createIcon() {
        Canvas canvas = new Canvas(16, 16);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Tomcat cat / feather emblem in yellow & red
        gc.setFill(Color.web("#E5A158"));
        gc.fillRoundRect(1, 2, 14, 12, 4, 4);

        gc.setFill(Color.web("#C75450"));
        gc.fillPolygon(new double[]{4, 8, 12}, new double[]{13, 5, 13}, 3);

        gc.setFill(Color.web("#2B2D30"));
        gc.fillOval(5, 5, 2, 2);
        gc.fillOval(9, 5, 2, 2);

        return canvas;
    }

    @Override
    public String getDefaultName() {
        return "Tomcat 10.1";
    }

    @Override
    public boolean validateHomePath(String path) {
        if (path == null || path.isBlank()) return false;
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) return false;
        File libDir = new File(dir, "lib");
        File catalinaJar = new File(libDir, "catalina.jar");
        File binDir = new File(dir, "bin");
        File catalinaSh = new File(binDir, "catalina.sh");
        File catalinaBat = new File(binDir, "catalina.bat");
        return catalinaJar.exists() || catalinaSh.exists() || catalinaBat.exists();
    }

    @Override
    public String detectVersion(String path) {
        if (path == null) return "Tomcat 10.1.20";
        File dir = new File(path);
        String name = dir.getName().toLowerCase();
        if (name.contains("tomcat-")) {
            return "Tomcat " + name.replace("apache-tomcat-", "").replace("tomcat-", "");
        }
        return "Tomcat 10.1.20";
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
            libs.add("catalina.jar");
            libs.add("servlet-api.jar");
            libs.add("jsp-api.jar");
            libs.add("el-api.jar");
            libs.add("tomcat-coyote.jar");
            libs.add("tomcat-util.jar");
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
