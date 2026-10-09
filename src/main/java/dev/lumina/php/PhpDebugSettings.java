package dev.lumina.php;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * Model representing PHP Debug configuration settings in Lumina IDE.
 */
public class PhpDebugSettings {

    // External connections
    private boolean ignoreExternalConnections = true;
    private boolean breakAtFirstLine = false;
    private int maxSimultaneousConnections = 3;

    // Xdebug
    private String xdebugPort = "9003,9000";
    private boolean xdebugCanAcceptExternalConnections = true;
    private boolean xdebugResolveBreakpoint = true;
    private boolean xdebugMoveBreakpoint = true;
    private boolean xdebugForceBreakNoPathMapping = true;
    private boolean xdebugForceBreakOutsideProject = true;
    private boolean xdebugEnableReturnFunctionValueDebugging = true;
    private boolean xdebugPredictFutureConditionValues = true;
    private boolean xdebugGrayOutUnreachableBlocks = true;

    // Zend Debugger
    private String zendDebugPort = "10137";
    private boolean zendCanAcceptExternalConnections = true;
    private String zendBroadcastingPort = "20080";
    private boolean zendAutoDetectIdeIp = true;
    private String zendDetectedIdeIp = "";
    private boolean zendIgnoreZRayRequests = true;

    // Evaluation
    private boolean showArrayAndObjectChildren = true;
    private boolean safeEvaluationMode = true;
    private boolean importNamespaceAndUseStatements = true;
    private boolean enableToStringObjectView = true;
    private boolean enableNavigateLinks = true;

    // Advanced / Settings
    private boolean detectPathMappings = true;
    private boolean notifyIfSessionFinishedWithoutPause = true;
    private boolean passRequiredOptionsThroughCommandLine = true;
    private boolean notifyIfBreakpointResolvedToDifferentLine = true;

    // DBGp Proxy
    private String dbgpIdeKey = "";
    private String dbgpHost = "";
    private String dbgpPort = "9001";

    // Skipped Paths
    private boolean notifySkippedFiles = true;
    private List<String> skippedPaths = new ArrayList<>();

    // Step Filters
    private boolean skipMagicMethods = false;
    private boolean skipConstructors = false;
    private List<String> skippedMethods = new ArrayList<>();
    private List<String> skippedFiles = new ArrayList<>();

    // Xdebug Cloud
    private boolean connectToXdebugCloud = false;
    private String xdebugCloudId = "";

    public PhpDebugSettings() {
        this.zendDetectedIdeIp = detectHostIps();
    }

    public PhpDebugSettings(PhpDebugSettings other) {
        if (other == null) return;
        this.ignoreExternalConnections = other.ignoreExternalConnections;
        this.breakAtFirstLine = other.breakAtFirstLine;
        this.maxSimultaneousConnections = other.maxSimultaneousConnections;
        this.xdebugPort = other.xdebugPort;
        this.xdebugCanAcceptExternalConnections = other.xdebugCanAcceptExternalConnections;
        this.xdebugResolveBreakpoint = other.xdebugResolveBreakpoint;
        this.xdebugMoveBreakpoint = other.xdebugMoveBreakpoint;
        this.xdebugForceBreakNoPathMapping = other.xdebugForceBreakNoPathMapping;
        this.xdebugForceBreakOutsideProject = other.xdebugForceBreakOutsideProject;
        this.xdebugEnableReturnFunctionValueDebugging = other.xdebugEnableReturnFunctionValueDebugging;
        this.xdebugPredictFutureConditionValues = other.xdebugPredictFutureConditionValues;
        this.xdebugGrayOutUnreachableBlocks = other.xdebugGrayOutUnreachableBlocks;
        this.zendDebugPort = other.zendDebugPort;
        this.zendCanAcceptExternalConnections = other.zendCanAcceptExternalConnections;
        this.zendBroadcastingPort = other.zendBroadcastingPort;
        this.zendAutoDetectIdeIp = other.zendAutoDetectIdeIp;
        this.zendDetectedIdeIp = other.zendDetectedIdeIp;
        this.zendIgnoreZRayRequests = other.zendIgnoreZRayRequests;
        this.showArrayAndObjectChildren = other.showArrayAndObjectChildren;
        this.safeEvaluationMode = other.safeEvaluationMode;
        this.importNamespaceAndUseStatements = other.importNamespaceAndUseStatements;
        this.enableToStringObjectView = other.enableToStringObjectView;
        this.enableNavigateLinks = other.enableNavigateLinks;
        this.detectPathMappings = other.detectPathMappings;
        this.notifyIfSessionFinishedWithoutPause = other.notifyIfSessionFinishedWithoutPause;
        this.passRequiredOptionsThroughCommandLine = other.passRequiredOptionsThroughCommandLine;
        this.notifyIfBreakpointResolvedToDifferentLine = other.notifyIfBreakpointResolvedToDifferentLine;
        this.dbgpIdeKey = other.dbgpIdeKey;
        this.dbgpHost = other.dbgpHost;
        this.dbgpPort = other.dbgpPort;
        this.notifySkippedFiles = other.notifySkippedFiles;
        this.skippedPaths = new ArrayList<>(other.skippedPaths);
        this.skipMagicMethods = other.skipMagicMethods;
        this.skipConstructors = other.skipConstructors;
        this.skippedMethods = new ArrayList<>(other.skippedMethods);
        this.skippedFiles = new ArrayList<>(other.skippedFiles);
        this.connectToXdebugCloud = other.connectToXdebugCloud;
        this.xdebugCloudId = other.xdebugCloudId;
    }

    /**
     * Dynamically detects host IPv4 addresses across active network interfaces.
     */
    public static String detectHostIps() {
        List<String> nonLoopback = new ArrayList<>();
        List<String> loopback = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (!ni.isUp() || ni.isVirtual()) continue;
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {
                            String host = addr.getHostAddress();
                            if (addr.isLoopbackAddress()) {
                                if (!loopback.contains(host)) loopback.add(host);
                            } else {
                                if (!nonLoopback.contains(host)) nonLoopback.add(host);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        List<String> all = new ArrayList<>(nonLoopback);
        all.addAll(loopback);
        if (all.isEmpty()) {
            all.add("127.0.0.1");
        }
        return String.join(",", all);
    }

    // Getters and Setters

    public boolean isIgnoreExternalConnections() {
        return ignoreExternalConnections;
    }

    public void setIgnoreExternalConnections(boolean ignoreExternalConnections) {
        this.ignoreExternalConnections = ignoreExternalConnections;
    }

    public boolean isBreakAtFirstLine() {
        return breakAtFirstLine;
    }

    public void setBreakAtFirstLine(boolean breakAtFirstLine) {
        this.breakAtFirstLine = breakAtFirstLine;
    }

    public int getMaxSimultaneousConnections() {
        return maxSimultaneousConnections;
    }

    public void setMaxSimultaneousConnections(int maxSimultaneousConnections) {
        this.maxSimultaneousConnections = maxSimultaneousConnections;
    }

    public String getXdebugPort() {
        return xdebugPort;
    }

    public void setXdebugPort(String xdebugPort) {
        this.xdebugPort = xdebugPort != null ? xdebugPort : "9003,9000";
    }

    public boolean isXdebugCanAcceptExternalConnections() {
        return xdebugCanAcceptExternalConnections;
    }

    public void setXdebugCanAcceptExternalConnections(boolean xdebugCanAcceptExternalConnections) {
        this.xdebugCanAcceptExternalConnections = xdebugCanAcceptExternalConnections;
    }

    public boolean isXdebugResolveBreakpoint() {
        return xdebugResolveBreakpoint;
    }

    public void setXdebugResolveBreakpoint(boolean xdebugResolveBreakpoint) {
        this.xdebugResolveBreakpoint = xdebugResolveBreakpoint;
    }

    public boolean isXdebugMoveBreakpoint() {
        return xdebugMoveBreakpoint;
    }

    public void setXdebugMoveBreakpoint(boolean xdebugMoveBreakpoint) {
        this.xdebugMoveBreakpoint = xdebugMoveBreakpoint;
    }

    public boolean isXdebugForceBreakNoPathMapping() {
        return xdebugForceBreakNoPathMapping;
    }

    public void setXdebugForceBreakNoPathMapping(boolean xdebugForceBreakNoPathMapping) {
        this.xdebugForceBreakNoPathMapping = xdebugForceBreakNoPathMapping;
    }

    public boolean isXdebugForceBreakOutsideProject() {
        return xdebugForceBreakOutsideProject;
    }

    public void setXdebugForceBreakOutsideProject(boolean xdebugForceBreakOutsideProject) {
        this.xdebugForceBreakOutsideProject = xdebugForceBreakOutsideProject;
    }

    public boolean isXdebugEnableReturnFunctionValueDebugging() {
        return xdebugEnableReturnFunctionValueDebugging;
    }

    public void setXdebugEnableReturnFunctionValueDebugging(boolean xdebugEnableReturnFunctionValueDebugging) {
        this.xdebugEnableReturnFunctionValueDebugging = xdebugEnableReturnFunctionValueDebugging;
    }

    public boolean isXdebugPredictFutureConditionValues() {
        return xdebugPredictFutureConditionValues;
    }

    public void setXdebugPredictFutureConditionValues(boolean xdebugPredictFutureConditionValues) {
        this.xdebugPredictFutureConditionValues = xdebugPredictFutureConditionValues;
    }

    public boolean isXdebugGrayOutUnreachableBlocks() {
        return xdebugGrayOutUnreachableBlocks;
    }

    public void setXdebugGrayOutUnreachableBlocks(boolean xdebugGrayOutUnreachableBlocks) {
        this.xdebugGrayOutUnreachableBlocks = xdebugGrayOutUnreachableBlocks;
    }

    public String getZendDebugPort() {
        return zendDebugPort;
    }

    public void setZendDebugPort(String zendDebugPort) {
        this.zendDebugPort = zendDebugPort != null ? zendDebugPort : "10137";
    }

    public boolean isZendCanAcceptExternalConnections() {
        return zendCanAcceptExternalConnections;
    }

    public void setZendCanAcceptExternalConnections(boolean zendCanAcceptExternalConnections) {
        this.zendCanAcceptExternalConnections = zendCanAcceptExternalConnections;
    }

    public String getZendBroadcastingPort() {
        return zendBroadcastingPort;
    }

    public void setZendBroadcastingPort(String zendBroadcastingPort) {
        this.zendBroadcastingPort = zendBroadcastingPort != null ? zendBroadcastingPort : "20080";
    }

    public boolean isZendAutoDetectIdeIp() {
        return zendAutoDetectIdeIp;
    }

    public void setZendAutoDetectIdeIp(boolean zendAutoDetectIdeIp) {
        this.zendAutoDetectIdeIp = zendAutoDetectIdeIp;
    }

    public String getZendDetectedIdeIp() {
        return zendDetectedIdeIp;
    }

    public void setZendDetectedIdeIp(String zendDetectedIdeIp) {
        this.zendDetectedIdeIp = zendDetectedIdeIp != null ? zendDetectedIdeIp : "";
    }

    public boolean isZendIgnoreZRayRequests() {
        return zendIgnoreZRayRequests;
    }

    public void setZendIgnoreZRayRequests(boolean zendIgnoreZRayRequests) {
        this.zendIgnoreZRayRequests = zendIgnoreZRayRequests;
    }

    public boolean isShowArrayAndObjectChildren() {
        return showArrayAndObjectChildren;
    }

    public void setShowArrayAndObjectChildren(boolean showArrayAndObjectChildren) {
        this.showArrayAndObjectChildren = showArrayAndObjectChildren;
    }

    public boolean isSafeEvaluationMode() {
        return safeEvaluationMode;
    }

    public void setSafeEvaluationMode(boolean safeEvaluationMode) {
        this.safeEvaluationMode = safeEvaluationMode;
    }

    public boolean isImportNamespaceAndUseStatements() {
        return importNamespaceAndUseStatements;
    }

    public void setImportNamespaceAndUseStatements(boolean importNamespaceAndUseStatements) {
        this.importNamespaceAndUseStatements = importNamespaceAndUseStatements;
    }

    public boolean isEnableToStringObjectView() {
        return enableToStringObjectView;
    }

    public void setEnableToStringObjectView(boolean enableToStringObjectView) {
        this.enableToStringObjectView = enableToStringObjectView;
    }

    public boolean isEnableNavigateLinks() {
        return enableNavigateLinks;
    }

    public void setEnableNavigateLinks(boolean enableNavigateLinks) {
        this.enableNavigateLinks = enableNavigateLinks;
    }

    public boolean isDetectPathMappings() {
        return detectPathMappings;
    }

    public void setDetectPathMappings(boolean detectPathMappings) {
        this.detectPathMappings = detectPathMappings;
    }

    public boolean isNotifyIfSessionFinishedWithoutPause() {
        return notifyIfSessionFinishedWithoutPause;
    }

    public void setNotifyIfSessionFinishedWithoutPause(boolean notifyIfSessionFinishedWithoutPause) {
        this.notifyIfSessionFinishedWithoutPause = notifyIfSessionFinishedWithoutPause;
    }

    public boolean isPassRequiredOptionsThroughCommandLine() {
        return passRequiredOptionsThroughCommandLine;
    }

    public void setPassRequiredOptionsThroughCommandLine(boolean passRequiredOptionsThroughCommandLine) {
        this.passRequiredOptionsThroughCommandLine = passRequiredOptionsThroughCommandLine;
    }

    public boolean isNotifyIfBreakpointResolvedToDifferentLine() {
        return notifyIfBreakpointResolvedToDifferentLine;
    }

    public void setNotifyIfBreakpointResolvedToDifferentLine(boolean notifyIfBreakpointResolvedToDifferentLine) {
        this.notifyIfBreakpointResolvedToDifferentLine = notifyIfBreakpointResolvedToDifferentLine;
    }

    public String getDbgpIdeKey() {
        return dbgpIdeKey;
    }

    public void setDbgpIdeKey(String dbgpIdeKey) {
        this.dbgpIdeKey = dbgpIdeKey != null ? dbgpIdeKey : "";
    }

    public String getDbgpHost() {
        return dbgpHost;
    }

    public void setDbgpHost(String dbgpHost) {
        this.dbgpHost = dbgpHost != null ? dbgpHost : "";
    }

    public String getDbgpPort() {
        return dbgpPort;
    }

    public void setDbgpPort(String dbgpPort) {
        this.dbgpPort = dbgpPort != null ? dbgpPort : "9001";
    }

    public boolean isNotifySkippedFiles() {
        return notifySkippedFiles;
    }

    public void setNotifySkippedFiles(boolean notifySkippedFiles) {
        this.notifySkippedFiles = notifySkippedFiles;
    }

    public List<String> getSkippedPaths() {
        return skippedPaths;
    }

    public void setSkippedPaths(List<String> skippedPaths) {
        this.skippedPaths = skippedPaths != null ? new ArrayList<>(skippedPaths) : new ArrayList<>();
    }

    public boolean isSkipMagicMethods() {
        return skipMagicMethods;
    }

    public void setSkipMagicMethods(boolean skipMagicMethods) {
        this.skipMagicMethods = skipMagicMethods;
    }

    public boolean isSkipConstructors() {
        return skipConstructors;
    }

    public void setSkipConstructors(boolean skipConstructors) {
        this.skipConstructors = skipConstructors;
    }

    public List<String> getSkippedMethods() {
        return skippedMethods;
    }

    public void setSkippedMethods(List<String> skippedMethods) {
        this.skippedMethods = skippedMethods != null ? new ArrayList<>(skippedMethods) : new ArrayList<>();
    }

    public List<String> getSkippedFiles() {
        return skippedFiles;
    }

    public void setSkippedFiles(List<String> skippedFiles) {
        this.skippedFiles = skippedFiles != null ? new ArrayList<>(skippedFiles) : new ArrayList<>();
    }

    public boolean isConnectToXdebugCloud() {
        return connectToXdebugCloud;
    }

    public void setConnectToXdebugCloud(boolean connectToXdebugCloud) {
        this.connectToXdebugCloud = connectToXdebugCloud;
    }

    public String getXdebugCloudId() {
        return xdebugCloudId;
    }

    public void setXdebugCloudId(String xdebugCloudId) {
        this.xdebugCloudId = xdebugCloudId != null ? xdebugCloudId : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhpDebugSettings that = (PhpDebugSettings) o;
        return ignoreExternalConnections == that.ignoreExternalConnections &&
                breakAtFirstLine == that.breakAtFirstLine &&
                maxSimultaneousConnections == that.maxSimultaneousConnections &&
                xdebugCanAcceptExternalConnections == that.xdebugCanAcceptExternalConnections &&
                xdebugResolveBreakpoint == that.xdebugResolveBreakpoint &&
                xdebugMoveBreakpoint == that.xdebugMoveBreakpoint &&
                xdebugForceBreakNoPathMapping == that.xdebugForceBreakNoPathMapping &&
                xdebugForceBreakOutsideProject == that.xdebugForceBreakOutsideProject &&
                xdebugEnableReturnFunctionValueDebugging == that.xdebugEnableReturnFunctionValueDebugging &&
                xdebugPredictFutureConditionValues == that.xdebugPredictFutureConditionValues &&
                xdebugGrayOutUnreachableBlocks == that.xdebugGrayOutUnreachableBlocks &&
                zendCanAcceptExternalConnections == that.zendCanAcceptExternalConnections &&
                zendAutoDetectIdeIp == that.zendAutoDetectIdeIp &&
                zendIgnoreZRayRequests == that.zendIgnoreZRayRequests &&
                showArrayAndObjectChildren == that.showArrayAndObjectChildren &&
                safeEvaluationMode == that.safeEvaluationMode &&
                importNamespaceAndUseStatements == that.importNamespaceAndUseStatements &&
                enableToStringObjectView == that.enableToStringObjectView &&
                enableNavigateLinks == daylightEquals(that) &&
                detectPathMappings == that.detectPathMappings &&
                notifyIfSessionFinishedWithoutPause == that.notifyIfSessionFinishedWithoutPause &&
                passRequiredOptionsThroughCommandLine == that.passRequiredOptionsThroughCommandLine &&
                notifyIfBreakpointResolvedToDifferentLine == that.notifyIfBreakpointResolvedToDifferentLine &&
                notifySkippedFiles == that.notifySkippedFiles &&
                skipMagicMethods == that.skipMagicMethods &&
                skipConstructors == that.skipConstructors &&
                connectToXdebugCloud == that.connectToXdebugCloud &&
                Objects.equals(xdebugPort, that.xdebugPort) &&
                Objects.equals(zendDebugPort, that.zendDebugPort) &&
                Objects.equals(zendBroadcastingPort, that.zendBroadcastingPort) &&
                Objects.equals(zendDetectedIdeIp, that.zendDetectedIdeIp) &&
                Objects.equals(dbgpIdeKey, that.dbgpIdeKey) &&
                Objects.equals(dbgpHost, that.dbgpHost) &&
                Objects.equals(dbgpPort, that.dbgpPort) &&
                Objects.equals(skippedPaths, that.skippedPaths) &&
                Objects.equals(skippedMethods, that.skippedMethods) &&
                Objects.equals(skippedFiles, that.skippedFiles) &&
                Objects.equals(xdebugCloudId, that.xdebugCloudId);
    }

    private boolean daylightEquals(PhpDebugSettings that) {
        return enableNavigateLinks == that.enableNavigateLinks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                ignoreExternalConnections, breakAtFirstLine, maxSimultaneousConnections,
                xdebugPort, xdebugCanAcceptExternalConnections, xdebugResolveBreakpoint,
                xdebugMoveBreakpoint, xdebugForceBreakNoPathMapping, xdebugForceBreakOutsideProject,
                xdebugEnableReturnFunctionValueDebugging, xdebugPredictFutureConditionValues,
                xdebugGrayOutUnreachableBlocks, zendDebugPort, zendCanAcceptExternalConnections,
                zendBroadcastingPort, zendAutoDetectIdeIp, zendDetectedIdeIp, zendIgnoreZRayRequests,
                showArrayAndObjectChildren, safeEvaluationMode, importNamespaceAndUseStatements,
                enableToStringObjectView, enableNavigateLinks, detectPathMappings,
                notifyIfSessionFinishedWithoutPause, passRequiredOptionsThroughCommandLine,
                notifyIfBreakpointResolvedToDifferentLine, dbgpIdeKey, dbgpHost, dbgpPort,
                notifySkippedFiles, skippedPaths, skipMagicMethods, skipConstructors,
                skippedMethods, skippedFiles, connectToXdebugCloud, xdebugCloudId
        );
    }
}
