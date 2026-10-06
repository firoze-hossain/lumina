package dev.lumina.copyright;

import java.util.Objects;

/**
 * Model representing a Scope-to-Copyright mapping in IntelliJ IDEA's
 * Editor > Copyright page.
 */
public class ScopeCopyrightMapping {

    private String scope;
    private String copyright;

    public ScopeCopyrightMapping() {
    }

    public ScopeCopyrightMapping(String scope, String copyright) {
        this.scope = scope;
        this.copyright = copyright;
    }

    public String getScope() {
        return scope != null ? scope : "";
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getCopyright() {
        return copyright != null ? copyright : "";
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public ScopeCopyrightMapping copy() {
        return new ScopeCopyrightMapping(this.scope, this.copyright);
    }

    public boolean isEquivalentTo(ScopeCopyrightMapping other) {
        if (other == null) return false;
        return Objects.equals(this.scope, other.scope)
                && Objects.equals(this.copyright, other.copyright);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopeCopyrightMapping that = (ScopeCopyrightMapping) o;
        return Objects.equals(scope, that.scope) && Objects.equals(copyright, that.copyright);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, copyright);
    }

    @Override
    public String toString() {
        return scope + " -> " + copyright;
    }
}
