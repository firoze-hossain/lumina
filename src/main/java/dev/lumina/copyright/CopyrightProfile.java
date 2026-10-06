package dev.lumina.copyright;

import java.util.Objects;

/**
 * Model representing a Copyright Profile in Lumina, matching IntelliJ IDEA's
 * copyright profile configuration.
 */
public class CopyrightProfile {

    private String name;
    private String notice;
    private String keyword = "Copyright";
    private boolean allowReplacing = true;
    private boolean shared = false; // false = Local, true = Shared

    public CopyrightProfile() {
        this.notice = "/*\n * Copyright (c) $today.year YourCompany.\n * All rights reserved.\n */";
    }

    public CopyrightProfile(String name, boolean shared) {
        this.name = name;
        this.shared = shared;
        this.notice = "/*\n * Copyright (c) $today.year YourCompany.\n * All rights reserved.\n */";
    }

    public CopyrightProfile(String name, String notice, String keyword, boolean allowReplacing, boolean shared) {
        this.name = name;
        this.notice = notice;
        this.keyword = keyword != null ? keyword : "Copyright";
        this.allowReplacing = allowReplacing;
        this.shared = shared;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotice() {
        return notice != null ? notice : "";
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getKeyword() {
        return keyword != null ? keyword : "Copyright";
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public boolean isAllowReplacing() {
        return allowReplacing;
    }

    public void setAllowReplacing(boolean allowReplacing) {
        this.allowReplacing = allowReplacing;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public CopyrightProfile copy() {
        CopyrightProfile clone = new CopyrightProfile();
        clone.name = this.name;
        clone.notice = this.notice;
        clone.keyword = this.keyword;
        clone.allowReplacing = this.allowReplacing;
        clone.shared = this.shared;
        return clone;
    }

    public boolean isEquivalentTo(CopyrightProfile other) {
        if (other == null) return false;
        return Objects.equals(name, other.name)
                && Objects.equals(notice, other.notice)
                && Objects.equals(keyword, other.keyword)
                && allowReplacing == other.allowReplacing
                && shared == other.shared;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CopyrightProfile that = (CopyrightProfile) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return getName() + (shared ? " [Shared]" : " [Local]");
    }
}
