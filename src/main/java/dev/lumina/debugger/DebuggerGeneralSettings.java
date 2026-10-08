package dev.lumina.debugger;

import java.util.Objects;

/**
 * General Debugger settings matching IntelliJ IDEA's
 * "Build, Execution, Deployment > Debugger" settings page (Image 1).
 */
public class DebuggerGeneralSettings implements Cloneable {

    public enum BreakpointRemoveMode {
        CLICK_LEFT,
        DRAG_OR_MIDDLE
    }

    public enum TransportMode {
        SOCKET,
        SHARED_MEMORY
    }

    // Top checkboxes
    private boolean showDebugWindowOnBreakpoint = true;
    private boolean focusApplicationOnBreakpoint = true;
    private boolean hideDebugWindowOnProcessTermination = false;
    private boolean scrollExecutionPointToCenter = false;
    private boolean clickLineNumberToRunToCursor = true;

    // Remove breakpoint section
    private BreakpointRemoveMode removeBreakpointMode = BreakpointRemoveMode.CLICK_LEFT;
    private boolean confirmRemovalConditionalOrLogging = false;

    // Java section
    private TransportMode transport = TransportMode.SOCKET;
    private boolean showAlternativeSourceSwitcher = true;
    private boolean killDebugProcessImmediately = false;
    private boolean attachMemoryAgent = false;

    // Kotlin section
    private boolean attachCoroutineAgent = true;

    public DebuggerGeneralSettings() {
    }

    public DebuggerGeneralSettings(DebuggerGeneralSettings other) {
        if (other != null) {
            this.showDebugWindowOnBreakpoint = other.showDebugWindowOnBreakpoint;
            this.focusApplicationOnBreakpoint = other.focusApplicationOnBreakpoint;
            this.hideDebugWindowOnProcessTermination = other.hideDebugWindowOnProcessTermination;
            this.scrollExecutionPointToCenter = other.scrollExecutionPointToCenter;
            this.clickLineNumberToRunToCursor = other.clickLineNumberToRunToCursor;
            this.removeBreakpointMode = other.removeBreakpointMode;
            this.confirmRemovalConditionalOrLogging = other.confirmRemovalConditionalOrLogging;
            this.transport = other.transport;
            this.showAlternativeSourceSwitcher = other.showAlternativeSourceSwitcher;
            this.killDebugProcessImmediately = other.killDebugProcessImmediately;
            this.attachMemoryAgent = other.attachMemoryAgent;
            this.attachCoroutineAgent = other.attachCoroutineAgent;
        }
    }

    public boolean isShowDebugWindowOnBreakpoint() {
        return showDebugWindowOnBreakpoint;
    }

    public void setShowDebugWindowOnBreakpoint(boolean showDebugWindowOnBreakpoint) {
        this.showDebugWindowOnBreakpoint = showDebugWindowOnBreakpoint;
    }

    public boolean isFocusApplicationOnBreakpoint() {
        return focusApplicationOnBreakpoint;
    }

    public void setFocusApplicationOnBreakpoint(boolean focusApplicationOnBreakpoint) {
        this.focusApplicationOnBreakpoint = focusApplicationOnBreakpoint;
    }

    public boolean isHideDebugWindowOnProcessTermination() {
        return hideDebugWindowOnProcessTermination;
    }

    public void setHideDebugWindowOnProcessTermination(boolean hideDebugWindowOnProcessTermination) {
        this.hideDebugWindowOnProcessTermination = hideDebugWindowOnProcessTermination;
    }

    public boolean isScrollExecutionPointToCenter() {
        return scrollExecutionPointToCenter;
    }

    public void setScrollExecutionPointToCenter(boolean scrollExecutionPointToCenter) {
        this.scrollExecutionPointToCenter = scrollExecutionPointToCenter;
    }

    public boolean isClickLineNumberToRunToCursor() {
        return clickLineNumberToRunToCursor;
    }

    public void setClickLineNumberToRunToCursor(boolean clickLineNumberToRunToCursor) {
        this.clickLineNumberToRunToCursor = clickLineNumberToRunToCursor;
    }

    public BreakpointRemoveMode getRemoveBreakpointMode() {
        return removeBreakpointMode;
    }

    public void setRemoveBreakpointMode(BreakpointRemoveMode removeBreakpointMode) {
        this.removeBreakpointMode = removeBreakpointMode != null ? removeBreakpointMode : BreakpointRemoveMode.CLICK_LEFT;
    }

    public boolean isConfirmRemovalConditionalOrLogging() {
        return confirmRemovalConditionalOrLogging;
    }

    public void setConfirmRemovalConditionalOrLogging(boolean confirmRemovalConditionalOrLogging) {
        this.confirmRemovalConditionalOrLogging = confirmRemovalConditionalOrLogging;
    }

    public TransportMode getTransport() {
        return transport;
    }

    public void setTransport(TransportMode transport) {
        this.transport = transport != null ? transport : TransportMode.SOCKET;
    }

    public boolean isShowAlternativeSourceSwitcher() {
        return showAlternativeSourceSwitcher;
    }

    public void setShowAlternativeSourceSwitcher(boolean showAlternativeSourceSwitcher) {
        this.showAlternativeSourceSwitcher = showAlternativeSourceSwitcher;
    }

    public boolean isKillDebugProcessImmediately() {
        return killDebugProcessImmediately;
    }

    public void setKillDebugProcessImmediately(boolean killDebugProcessImmediately) {
        this.killDebugProcessImmediately = killDebugProcessImmediately;
    }

    public boolean isAttachMemoryAgent() {
        return attachMemoryAgent;
    }

    public void setAttachMemoryAgent(boolean attachMemoryAgent) {
        this.attachMemoryAgent = attachMemoryAgent;
    }

    public boolean isAttachCoroutineAgent() {
        return attachCoroutineAgent;
    }

    public void setAttachCoroutineAgent(boolean attachCoroutineAgent) {
        this.attachCoroutineAgent = attachCoroutineAgent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DebuggerGeneralSettings that)) return false;
        return showDebugWindowOnBreakpoint == that.showDebugWindowOnBreakpoint &&
                focusApplicationOnBreakpoint == that.focusApplicationOnBreakpoint &&
                hideDebugWindowOnProcessTermination == that.hideDebugWindowOnProcessTermination &&
                scrollExecutionPointToCenter == that.scrollExecutionPointToCenter &&
                clickLineNumberToRunToCursor == that.clickLineNumberToRunToCursor &&
                confirmRemovalConditionalOrLogging == that.confirmRemovalConditionalOrLogging &&
                showAlternativeSourceSwitcher == that.showAlternativeSourceSwitcher &&
                killDebugProcessImmediately == that.killDebugProcessImmediately &&
                attachMemoryAgent == that.attachMemoryAgent &&
                attachCoroutineAgent == that.attachCoroutineAgent &&
                removeBreakpointMode == that.removeBreakpointMode &&
                transport == that.transport;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showDebugWindowOnBreakpoint, focusApplicationOnBreakpoint,
                hideDebugWindowOnProcessTermination, scrollExecutionPointToCenter,
                clickLineNumberToRunToCursor, removeBreakpointMode,
                confirmRemovalConditionalOrLogging, transport,
                showAlternativeSourceSwitcher, killDebugProcessImmediately,
                attachMemoryAgent, attachCoroutineAgent);
    }

    @Override
    public DebuggerGeneralSettings clone() {
        return new DebuggerGeneralSettings(this);
    }
}
