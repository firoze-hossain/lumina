package dev.lumina.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Filter group representing a named set of package or class filters in the Java Profiler.
 */
public class ProfilerFilterGroup implements Cloneable {

    private String groupName;
    private String filtersPattern;

    public ProfilerFilterGroup() {
        this("", "");
    }

    public ProfilerFilterGroup(String groupName, String filtersPattern) {
        this.groupName = groupName;
        this.filtersPattern = filtersPattern;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getFiltersPattern() {
        return filtersPattern;
    }

    public void setFiltersPattern(String filtersPattern) {
        this.filtersPattern = filtersPattern;
    }

    /**
     * Checks if a given class or method name matches any pattern in this filter group.
     */
    public boolean matches(String target) {
        if (target == null || filtersPattern == null || filtersPattern.isBlank()) {
            return false;
        }

        String[] tokens = filtersPattern.split(",");
        for (String token : tokens) {
            String p = token.trim();
            if (p.isEmpty()) continue;
            if (matchesWildcard(p, target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesWildcard(String pattern, String text) {
        if (pattern.equals("*")) return true;
        // Convert wildcard pattern like com.google.* or *::* to regex
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append(".");
            } else if ("\\.[]{}()+-^$|/".indexOf(c) != -1) {
                regex.append("\\").append(c);
            } else {
                regex.append(c);
            }
        }
        regex.append("$");
        try {
            return Pattern.compile(regex.toString()).matcher(text).matches();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ProfilerFilterGroup clone() {
        try {
            return (ProfilerFilterGroup) super.clone();
        } catch (CloneNotSupportedException e) {
            return new ProfilerFilterGroup(this.groupName, this.filtersPattern);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfilerFilterGroup that = (ProfilerFilterGroup) o;
        return Objects.equals(groupName, that.groupName) &&
                Objects.equals(filtersPattern, that.filtersPattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName, filtersPattern);
    }

    @Override
    public String toString() {
        return groupName + ": " + filtersPattern;
    }
}
