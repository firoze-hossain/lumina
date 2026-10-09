package dev.lumina.php;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a configured PHP Server in Lumina IDE (Languages & Frameworks > PHP > Servers).
 * Faithfully matches the reference IDE layout and configuration capabilities.
 */
public class PhpServer {

    private String id;
    private String name;
    private boolean shared;
    private String host;
    private int port;
    private String debugger;
    private boolean usePathMappings;
    private List<PhpPathMapping> pathMappings = new ArrayList<>();

    public static class PhpPathMapping {
        private String localPath;
        private String remotePath;

        public PhpPathMapping() {
            this("", "");
        }

        public PhpPathMapping(String localPath, String remotePath) {
            this.localPath = localPath != null ? localPath : "";
            this.remotePath = remotePath != null ? remotePath : "";
        }

        public String getLocalPath() {
            return localPath;
        }

        public void setLocalPath(String localPath) {
            this.localPath = localPath != null ? localPath : "";
        }

        public String getRemotePath() {
            return remotePath;
        }

        public void setRemotePath(String remotePath) {
            this.remotePath = remotePath != null ? remotePath : "";
        }

        public PhpPathMapping copy() {
            return new PhpPathMapping(localPath, remotePath);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PhpPathMapping that)) return false;
            return Objects.equals(localPath, that.localPath) &&
                    Objects.equals(remotePath, that.remotePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(localPath, remotePath);
        }
    }

    public PhpServer() {
        this(UUID.randomUUID().toString(), "Unnamed", false, "", 80, "Xdebug", false);
    }

    public PhpServer(String name, String host, int port, String debugger) {
        this(UUID.randomUUID().toString(), name, false, host, port, debugger, false);
    }

    public PhpServer(String id, String name, boolean shared, String host, int port, String debugger, boolean usePathMappings) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Unnamed";
        this.shared = shared;
        this.host = host != null ? host : "";
        this.port = port > 0 ? port : 80;
        this.debugger = debugger != null ? debugger : "Xdebug";
        this.usePathMappings = usePathMappings;
        this.pathMappings = new ArrayList<>();
    }

    public PhpServer copy() {
        PhpServer copy = new PhpServer(id, name, shared, host, port, debugger, usePathMappings);
        for (PhpPathMapping m : pathMappings) {
            copy.pathMappings.add(m.copy());
        }
        return copy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "Unnamed";
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host != null ? host : "";
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port > 0 ? port : 80;
    }

    public String getDebugger() {
        return debugger;
    }

    public void setDebugger(String debugger) {
        this.debugger = debugger != null ? debugger : "Xdebug";
    }

    public boolean isUsePathMappings() {
        return usePathMappings;
    }

    public void setUsePathMappings(boolean usePathMappings) {
        this.usePathMappings = usePathMappings;
    }

    public List<PhpPathMapping> getPathMappings() {
        return pathMappings;
    }

    public void setPathMappings(List<PhpPathMapping> mappings) {
        this.pathMappings = new ArrayList<>();
        if (mappings != null) {
            for (PhpPathMapping m : mappings) {
                this.pathMappings.add(m.copy());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhpServer phpServer = (PhpServer) o;
        return shared == phpServer.shared &&
                port == phpServer.port &&
                usePathMappings == phpServer.usePathMappings &&
                Objects.equals(id, phpServer.id) &&
                Objects.equals(name, phpServer.name) &&
                Objects.equals(host, phpServer.host) &&
                Objects.equals(debugger, phpServer.debugger) &&
                Objects.equals(pathMappings, phpServer.pathMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, shared, host, port, debugger, usePathMappings, pathMappings);
    }

    @Override
    public String toString() {
        return name != null ? name : "Unnamed";
    }
}
