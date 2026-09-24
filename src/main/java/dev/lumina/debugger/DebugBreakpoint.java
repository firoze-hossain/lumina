package dev.lumina.debugger;

import java.util.Objects;

/**
 * Represents a line breakpoint in the IDE.
 */
public record DebugBreakpoint(String fqcn, int line, boolean enabled, String condition) {

    public DebugBreakpoint(String fqcn, int line) {
        this(fqcn, line, true, null);
    }

    public String displayLabel() {
        String simple = fqcn != null && fqcn.contains(".")
                ? fqcn.substring(fqcn.lastIndexOf('.') + 1) : fqcn;
        return simple + ":" + line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DebugBreakpoint that = (DebugBreakpoint) o;
        return line == that.line && Objects.equals(fqcn, that.fqcn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqcn, line);
    }
}
