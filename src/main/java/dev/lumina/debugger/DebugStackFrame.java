package dev.lumina.debugger;

/**
 * Represents a stack frame in the active call stack matching IntelliJ IDEA presentation.
 * Example: login:24, AuthenticationController (com.roze.nexacommerce.user.controller)
 */
public record DebugStackFrame(
        int frameIndex,
        String methodName,
        int lineNumber,
        String className,
        String packageName,
        String fileName,
        boolean isHiddenFrame,
        Object rawFrame
) {
    public String simpleClassName() {
        if (className == null) return "Unknown";
        int idx = className.lastIndexOf('.');
        return idx >= 0 ? className.substring(idx + 1) : className;
    }

    public String formattedLabel() {
        if (isHiddenFrame) {
            return frameIndex + " hidden frames";
        }
        String pkgStr = packageName != null && !packageName.isBlank() ? " (" + packageName + ")" : "";
        return methodName + ":" + lineNumber + ", " + simpleClassName() + pkgStr;
    }

    public String fullFqcn() {
        return className;
    }
}
