package dev.lumina.web;

import java.util.Objects;

/**
 * Represents a path-to-web-context mapping in Lumina IDE.
 */
public class WebContextMapping implements Cloneable {

    private String path = "";
    private String webContext = "/";

    public WebContextMapping() {
    }

    public WebContextMapping(String path, String webContext) {
        this.path = path != null ? path : "";
        this.webContext = webContext != null ? webContext : "/";
    }

    public WebContextMapping(WebContextMapping other) {
        if (other != null) {
            this.path = other.path;
            this.webContext = other.webContext;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    public String getWebContext() {
        return webContext;
    }

    public void setWebContext(String webContext) {
        this.webContext = webContext != null ? webContext : "/";
    }

    @Override
    public WebContextMapping clone() {
        return new WebContextMapping(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebContextMapping that = (WebContextMapping) o;
        return Objects.equals(path, that.path) && Objects.equals(webContext, that.webContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, webContext);
    }
}
