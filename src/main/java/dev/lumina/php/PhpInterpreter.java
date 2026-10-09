package dev.lumina.php;

import java.util.Objects;

/**
 * Model representing a configured PHP CLI Interpreter in Lumina IDE.
 */
public class PhpInterpreter {

    private String id;
    private String name;
    private String path;
    private String phpVersion;
    private String debugger;
    private String phpIniPath;

    public PhpInterpreter() {
        this.id = "default-cli-php";
        this.name = "/bin/php (8.3.6)";
        this.path = "/bin/php";
        this.phpVersion = "8.3.6";
        this.debugger = "Xdebug 3.3.1";
        this.phpIniPath = "/etc/php/8.3/cli/php.ini";
    }

    public PhpInterpreter(String id, String name, String path, String phpVersion) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.phpVersion = phpVersion;
        this.debugger = "None";
        this.phpIniPath = "";
    }

    public PhpInterpreter(String id, String name, String path, String phpVersion, String debugger, String phpIniPath) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.phpVersion = phpVersion;
        this.debugger = debugger;
        this.phpIniPath = phpIniPath;
    }

    public PhpInterpreter copy() {
        return new PhpInterpreter(id, name, path, phpVersion, debugger, phpIniPath);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPhpVersion() {
        return phpVersion;
    }

    public void setPhpVersion(String phpVersion) {
        this.phpVersion = phpVersion;
    }

    public String getDebugger() {
        return debugger;
    }

    public void setDebugger(String debugger) {
        this.debugger = debugger;
    }

    public String getPhpIniPath() {
        return phpIniPath;
    }

    public void setPhpIniPath(String phpIniPath) {
        this.phpIniPath = phpIniPath;
    }

    public String getDisplayLabel() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (path != null && !path.isBlank()) {
            return path + (phpVersion != null && !phpVersion.isBlank() ? " (" + phpVersion + ")" : "");
        }
        return "Unknown PHP Interpreter";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhpInterpreter that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(path, that.path) &&
                Objects.equals(phpVersion, that.phpVersion) &&
                Objects.equals(debugger, that.debugger) &&
                Objects.equals(phpIniPath, that.phpIniPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, path, phpVersion, debugger, phpIniPath);
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
