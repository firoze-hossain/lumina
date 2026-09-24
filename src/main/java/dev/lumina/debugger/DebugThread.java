package dev.lumina.debugger;

/**
 * Represents a thread in the target debuggee.
 * Example: "http-nio-8092-exec-1"@18,165 in group "main": RUNNING
 */
public record DebugThread(
        long id,
        String name,
        String groupName,
        boolean isSuspended,
        String statusDescription
) {
    public String formattedLabel() {
        String group = groupName != null && !groupName.isBlank() ? " in group \"" + groupName + "\"" : "";
        String status = statusDescription != null && !statusDescription.isBlank() ? ": " + statusDescription : "";
        return "\"" + name + "\"@" + id + group + status;
    }
}
