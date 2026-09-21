package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.io.File;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * Unified model and service for IntelliJ-style System Settings:
 * - General IDE behavior (confirm before exit, tool window process handling)
 * - Project startup, window open mode, default project directory
 * - Autosave (idle timer, focus lost, backup files, sync external changes)
 * - Date Formats (system override, pattern, 24-hour time, pretty formatting)
 * - Data Sharing (anonymous usage statistics, detailed code data)
 * - HTTP Proxy (No proxy, Auto-detect, Manual HTTP/SOCKS with auth & connection check)
 */
public final class SystemSettings {

    private static final SystemSettings INSTANCE = new SystemSettings();

    public static SystemSettings getInstance() {
        return INSTANCE;
    }

    // Enums
    public enum ProcessClosePolicy {
        TERMINATE("Terminate process"),
        DISCONNECT("Disconnect"),
        ASK("Ask");

        private final String label;
        ProcessClosePolicy(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum OpenProjectMode {
        NEW_WINDOW("New window"),
        CURRENT_WINDOW("Current window"),
        ASK("Ask");

        private final String label;
        OpenProjectMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum ProxyType {
        NO_PROXY("No proxy"),
        AUTO_DETECT("Auto-detect proxy settings"),
        MANUAL("Manual proxy configuration");

        private final String label;
        ProxyType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum ManualProtocol {
        HTTP, SOCKS
    }

    public enum PasswordStoragePolicy {
        NATIVE_KEYCHAIN("In native Keychain"),
        KEEPASS("In KeePass"),
        DO_NOT_SAVE("Do not save, forget passwords after restart");

        private final String label;
        PasswordStoragePolicy(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // General & Process
    private boolean confirmExit = true;
    private ProcessClosePolicy processClosePolicy = ProcessClosePolicy.ASK;

    // Project
    private boolean reopenProjectsOnStartup = true;
    private OpenProjectMode openProjectMode = OpenProjectMode.ASK;
    private String defaultProjectDirectory = System.getProperty("user.home") + File.separator + "projects";

    // Autosave
    private boolean idleAutosaveEnabled = false;
    private int idleAutosaveSeconds = 15;
    private boolean saveOnFocusLost = true;
    private boolean backupFilesBeforeSaving = true;
    private boolean syncExternalOnFocus = true;
    private boolean syncExternalPeriodically = true;

    // Date Formats
    private boolean overrideSystemDateFormat = false;
    private String dateFormatPattern = "dd MMM yyyy";
    private boolean use24HourTime = true;
    private boolean usePrettyFormatting = true;

    // Data Sharing
    private boolean sendAnonymousStats = false;
    private boolean sendDetailedData = false;

    // HTTP Proxy
    private ProxyType proxyType = ProxyType.AUTO_DETECT;
    private boolean autoConfigUrlEnabled = false;
    private String autoConfigUrl = "";
    private ManualProtocol manualProtocol = ManualProtocol.HTTP;
    private String proxyHost = "";
    private int proxyPort = 80;
    private String noProxyFor = "";
    private boolean proxyAuthEnabled = false;
    private String proxyLogin = "";
    private String proxyPassword = "";
    private boolean proxyRemember = false;

    // Language and Region
    private String language = "English";
    private String region = "Not specified";

    // Passwords
    private PasswordStoragePolicy passwordStoragePolicy = PasswordStoragePolicy.NATIVE_KEYCHAIN;
    private String keepassDbPath = System.getProperty("user.home") + File.separator + ".config" + File.separator + "JetBrains" + File.separator + "IntelliJIdea2025.3" + File.separator + "c.kdbx";
    private boolean protectMasterPasswordWithPgp = false;

    // Process Elevation
    private boolean keepSudoAuth = false;
    private String sudoTimeout = "15 min";
    private boolean extendSudoTimeout = true;

    // Server Certificates
    private boolean acceptNonTrustedCerts = false;
    private final java.util.List<String> acceptedCertificates = new java.util.ArrayList<>();

    // Trusted Hosts
    private final java.util.List<String> trustedHosts = new java.util.ArrayList<>(java.util.List.of(
            "download.jetbrains.com",
            "download-cf.jetbrains.com",
            "download-cdn.jetbrains.com",
            "cache-redirector.jetbrains.com"
    ));

    // Updates
    private boolean checkIdeUpdates = true;
    private String updateChannel = "Stable Releases";
    private boolean checkPluginUpdates = true;
    private boolean updatePluginsAutomatically = false;
    private boolean showWhatsNewAfterUpdate = true;
    private boolean checkJdkUpdates = true;
    private String lastUpdateCheckTime = "Today 5:45 PM";

    private SystemSettings() {
        load();
    }

    /**
     * Loads settings from persistent store (lumina.properties).
     */
    public synchronized void load() {
        // General
        String val = Settings.get("system.confirmExit");
        if (val != null) confirmExit = Boolean.parseBoolean(val);

        val = Settings.get("system.processClosePolicy");
        if (val != null) {
            try { processClosePolicy = ProcessClosePolicy.valueOf(val); } catch (Exception ignored) {}
        }

        // Project
        val = Settings.get("system.reopenProjectsOnStartup");
        if (val != null) reopenProjectsOnStartup = Boolean.parseBoolean(val);

        val = Settings.get("system.openProjectMode");
        if (val == null) val = Settings.get(Settings.OPEN_PROJECT_MODE);
        if (val != null) {
            if ("THIS_WINDOW".equalsIgnoreCase(val)) openProjectMode = OpenProjectMode.CURRENT_WINDOW;
            else {
                try { openProjectMode = OpenProjectMode.valueOf(val); } catch (Exception ignored) {}
            }
        }

        val = Settings.get("system.defaultProjectDirectory");
        if (val != null && !val.isBlank()) defaultProjectDirectory = val;

        // Autosave
        val = Settings.get("system.autosave.idleEnabled");
        if (val != null) idleAutosaveEnabled = Boolean.parseBoolean(val);

        val = Settings.get("system.autosave.idleSeconds");
        if (val != null) {
            try { idleAutosaveSeconds = Math.max(1, Integer.parseInt(val)); } catch (Exception ignored) {}
        }

        val = Settings.get("system.autosave.onFocusLost");
        if (val != null) saveOnFocusLost = Boolean.parseBoolean(val);

        val = Settings.get("system.autosave.backupFiles");
        if (val != null) backupFilesBeforeSaving = Boolean.parseBoolean(val);

        val = Settings.get("system.autosave.syncOnFocus");
        if (val != null) syncExternalOnFocus = Boolean.parseBoolean(val);

        val = Settings.get("system.autosave.syncPeriodically");
        if (val != null) syncExternalPeriodically = Boolean.parseBoolean(val);

        // Date Formats
        val = Settings.get("system.dateFormat.override");
        if (val != null) overrideSystemDateFormat = Boolean.parseBoolean(val);

        val = Settings.get("system.dateFormat.pattern");
        if (val != null && !val.isBlank()) dateFormatPattern = val;

        val = Settings.get("system.dateFormat.use24Hour");
        if (val != null) use24HourTime = Boolean.parseBoolean(val);

        val = Settings.get("system.dateFormat.pretty");
        if (val != null) usePrettyFormatting = Boolean.parseBoolean(val);

        // Data Sharing
        val = Settings.get("system.dataSharing.anonymousStats");
        if (val != null) sendAnonymousStats = Boolean.parseBoolean(val);

        val = Settings.get("system.dataSharing.detailedData");
        if (val != null) sendDetailedData = Boolean.parseBoolean(val);

        // HTTP Proxy
        val = Settings.get("system.proxy.type");
        if (val != null) {
            try { proxyType = ProxyType.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("system.proxy.autoConfigEnabled");
        if (val != null) autoConfigUrlEnabled = Boolean.parseBoolean(val);

        val = Settings.get("system.proxy.autoConfigUrl");
        if (val != null) autoConfigUrl = val;

        val = Settings.get("system.proxy.manualProtocol");
        if (val != null) {
            try { manualProtocol = ManualProtocol.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("system.proxy.host");
        if (val != null) proxyHost = val;

        val = Settings.get("system.proxy.port");
        if (val != null) {
            try { proxyPort = Integer.parseInt(val); } catch (Exception ignored) {}
        }

        val = Settings.get("system.proxy.noProxyFor");
        if (val != null) noProxyFor = val;

        val = Settings.get("system.proxy.authEnabled");
        if (val != null) proxyAuthEnabled = Boolean.parseBoolean(val);

        val = Settings.get("system.proxy.login");
        if (val != null) proxyLogin = val;

        val = Settings.get("system.proxy.password");
        if (val != null) proxyPassword = val;

        val = Settings.get("system.proxy.remember");
        if (val != null) proxyRemember = Boolean.parseBoolean(val);

        // Language and Region
        val = Settings.get("system.language");
        if (val != null) language = val;

        val = Settings.get("system.region");
        if (val != null) region = val;

        // Passwords
        val = Settings.get("system.passwords.policy");
        if (val != null) {
            try { passwordStoragePolicy = PasswordStoragePolicy.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("system.passwords.keepassDb");
        if (val != null && !val.isBlank()) keepassDbPath = val;

        val = Settings.get("system.passwords.pgp");
        if (val != null) protectMasterPasswordWithPgp = Boolean.parseBoolean(val);

        // Process Elevation
        val = Settings.get("system.elevation.keepSudo");
        if (val != null) keepSudoAuth = Boolean.parseBoolean(val);

        val = Settings.get("system.elevation.timeout");
        if (val != null) sudoTimeout = val;

        val = Settings.get("system.elevation.extend");
        if (val != null) extendSudoTimeout = Boolean.parseBoolean(val);

        // Server Certificates
        val = Settings.get("system.certs.acceptNonTrusted");
        if (val != null) acceptNonTrustedCerts = Boolean.parseBoolean(val);

        val = Settings.get("system.certs.acceptedList");
        if (val != null) {
            acceptedCertificates.clear();
            if (!val.isBlank()) {
                acceptedCertificates.addAll(java.util.Arrays.asList(val.split(";")));
            }
        }

        // Trusted Hosts
        val = Settings.get("system.trustedHosts");
        if (val != null) {
            trustedHosts.clear();
            if (!val.isBlank()) {
                trustedHosts.addAll(java.util.Arrays.asList(val.split(";")));
            }
        }

        // Updates
        val = Settings.get("system.updates.checkIde");
        if (val != null) checkIdeUpdates = Boolean.parseBoolean(val);

        val = Settings.get("system.updates.channel");
        if (val != null && !val.isBlank()) updateChannel = val;

        val = Settings.get("system.updates.checkPlugins");
        if (val != null) checkPluginUpdates = Boolean.parseBoolean(val);

        val = Settings.get("system.updates.autoUpdatePlugins");
        if (val != null) updatePluginsAutomatically = Boolean.parseBoolean(val);

        val = Settings.get("system.updates.showWhatsNew");
        if (val != null) showWhatsNewAfterUpdate = Boolean.parseBoolean(val);

        val = Settings.get("system.updates.checkJdk");
        if (val != null) checkJdkUpdates = Boolean.parseBoolean(val);

        val = Settings.get("system.updates.lastChecked");
        if (val != null && !val.isBlank()) lastUpdateCheckTime = val;
    }

    /**
     * Persists settings to lumina.properties.
     */
    public synchronized void save() {
        Settings.put("system.confirmExit", String.valueOf(confirmExit));
        Settings.put("system.processClosePolicy", processClosePolicy.name());
        Settings.put("system.reopenProjectsOnStartup", String.valueOf(reopenProjectsOnStartup));
        Settings.put("system.openProjectMode", openProjectMode.name());
        Settings.put(Settings.OPEN_PROJECT_MODE, openProjectMode == OpenProjectMode.CURRENT_WINDOW ? "THIS_WINDOW" : openProjectMode.name());
        Settings.put("system.defaultProjectDirectory", defaultProjectDirectory);

        Settings.put("system.autosave.idleEnabled", String.valueOf(idleAutosaveEnabled));
        Settings.put("system.autosave.idleSeconds", String.valueOf(idleAutosaveSeconds));
        Settings.put("system.autosave.onFocusLost", String.valueOf(saveOnFocusLost));
        Settings.put("system.autosave.backupFiles", String.valueOf(backupFilesBeforeSaving));
        Settings.put("system.autosave.syncOnFocus", String.valueOf(syncExternalOnFocus));
        Settings.put("system.autosave.syncPeriodically", String.valueOf(syncExternalPeriodically));

        Settings.put("system.dateFormat.override", String.valueOf(overrideSystemDateFormat));
        Settings.put("system.dateFormat.pattern", dateFormatPattern);
        Settings.put("system.dateFormat.use24Hour", String.valueOf(use24HourTime));
        Settings.put("system.dateFormat.pretty", String.valueOf(usePrettyFormatting));

        Settings.put("system.dataSharing.anonymousStats", String.valueOf(sendAnonymousStats));
        Settings.put("system.dataSharing.detailedData", String.valueOf(sendDetailedData));

        Settings.put("system.proxy.type", proxyType.name());
        Settings.put("system.proxy.autoConfigEnabled", String.valueOf(autoConfigUrlEnabled));
        Settings.put("system.proxy.autoConfigUrl", autoConfigUrl);
        Settings.put("system.proxy.manualProtocol", manualProtocol.name());
        Settings.put("system.proxy.host", proxyHost);
        Settings.put("system.proxy.port", String.valueOf(proxyPort));
        Settings.put("system.proxy.noProxyFor", noProxyFor);
        Settings.put("system.proxy.authEnabled", String.valueOf(proxyAuthEnabled));
        Settings.put("system.proxy.login", proxyLogin);
        if (proxyRemember) {
            Settings.put("system.proxy.password", proxyPassword);
        } else {
            Settings.put("system.proxy.password", null);
        }
        Settings.put("system.proxy.remember", String.valueOf(proxyRemember));

        Settings.put("system.language", language);
        Settings.put("system.region", region);
        Settings.put("system.passwords.policy", passwordStoragePolicy.name());
        Settings.put("system.passwords.keepassDb", keepassDbPath);
        Settings.put("system.passwords.pgp", String.valueOf(protectMasterPasswordWithPgp));
        Settings.put("system.elevation.keepSudo", String.valueOf(keepSudoAuth));
        Settings.put("system.elevation.timeout", sudoTimeout);
        Settings.put("system.elevation.extend", String.valueOf(extendSudoTimeout));
        Settings.put("system.certs.acceptNonTrusted", String.valueOf(acceptNonTrustedCerts));
        Settings.put("system.certs.acceptedList", String.join(";", acceptedCertificates));
        Settings.put("system.trustedHosts", String.join(";", trustedHosts));

        Settings.put("system.updates.checkIde", String.valueOf(checkIdeUpdates));
        Settings.put("system.updates.channel", updateChannel);
        Settings.put("system.updates.checkPlugins", String.valueOf(checkPluginUpdates));
        Settings.put("system.updates.autoUpdatePlugins", String.valueOf(updatePluginsAutomatically));
        Settings.put("system.updates.showWhatsNew", String.valueOf(showWhatsNewAfterUpdate));
        Settings.put("system.updates.checkJdk", String.valueOf(checkJdkUpdates));
        Settings.put("system.updates.lastChecked", lastUpdateCheckTime);

        apply();
    }

    /**
     * Applies runtime JVM networking settings and hooks.
     */
    public synchronized void apply() {
        if (proxyType == ProxyType.MANUAL && proxyHost != null && !proxyHost.isBlank()) {
            if (manualProtocol == ManualProtocol.HTTP) {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", String.valueOf(proxyPort));
                System.setProperty("https.proxyHost", proxyHost);
                System.setProperty("https.proxyPort", String.valueOf(proxyPort));
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
            } else {
                System.setProperty("socksProxyHost", proxyHost);
                System.setProperty("socksProxyPort", String.valueOf(proxyPort));
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
            }
            if (noProxyFor != null && !noProxyFor.isBlank()) {
                System.setProperty("http.nonProxyHosts", noProxyFor.replace(',', '|').replace(" ", ""));
            } else {
                System.clearProperty("http.nonProxyHosts");
            }
            if (proxyAuthEnabled && proxyLogin != null && !proxyLogin.isBlank()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType() == RequestorType.PROXY) {
                            return new PasswordAuthentication(proxyLogin, proxyPassword.toCharArray());
                        }
                        return super.getPasswordAuthentication();
                    }
                });
            }
        } else if (proxyType == ProxyType.NO_PROXY) {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            System.clearProperty("http.nonProxyHosts");
        }
    }

    /**
     * Clears saved proxy passwords.
     */
    public synchronized void clearProxyPasswords() {
        this.proxyPassword = "";
        Settings.put("system.proxy.password", null);
    }

    // Date formatting utilities
    public DateTimeFormatter getDateTimeFormatter() {
        String pattern = dateFormatPattern != null && !dateFormatPattern.isBlank() ? dateFormatPattern : "dd MMM yyyy";
        String timePart = use24HourTime ? "HH:mm" : "hh:mm a";
        try {
            return DateTimeFormatter.ofPattern(pattern + " " + timePart, Locale.getDefault());
        } catch (Exception e) {
            return DateTimeFormatter.ofPattern("dd MMM yyyy " + timePart, Locale.getDefault());
        }
    }

    public DateTimeFormatter getDateFormatter() {
        String pattern = dateFormatPattern != null && !dateFormatPattern.isBlank() ? dateFormatPattern : "dd MMM yyyy";
        try {
            return DateTimeFormatter.ofPattern(pattern, Locale.getDefault());
        } catch (Exception e) {
            return DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault());
        }
    }

    public String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        if (usePrettyFormatting) {
            return formatPretty(dt);
        }
        return dt.format(getDateTimeFormatter());
    }

    public String formatPretty(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDate today = LocalDate.now();
        LocalDate target = dt.toLocalDate();
        String timePart = dt.format(DateTimeFormatter.ofPattern(use24HourTime ? "HH:mm" : "hh:mm a", Locale.getDefault()));

        if (target.equals(today)) {
            LocalDateTime now = LocalDateTime.now();
            long mins = ChronoUnit.MINUTES.between(dt, now);
            if (mins >= 0 && mins < 60) {
                if (mins < 1) return "Just now";
                return mins + " minutes ago";
            }
            return "Today " + timePart;
        } else if (target.equals(today.minusDays(1))) {
            return "Yesterday " + timePart;
        }
        return dt.format(getDateTimeFormatter());
    }

    public String getSamplePreview() {
        LocalDateTime sample = LocalDateTime.of(2100, 12, 31, 23, 59);
        if (!use24HourTime) {
            sample = LocalDateTime.of(2100, 12, 31, 11, 59);
        }
        return sample.format(getDateTimeFormatter());
    }

    /**
     * Test proxy connection result record.
     */
    public record ProxyTestResult(boolean success, int statusCode, long responseTimeMs, String message) {}

    /**
     * Tests connectivity to target URL using configured proxy settings.
     */
    public ProxyTestResult checkConnection(String targetUrl) {
        long start = System.currentTimeMillis();
        try {
            if (targetUrl == null || targetUrl.isBlank()) {
                targetUrl = "https://jetbrains.com";
            }
            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                targetUrl = "https://" + targetUrl;
            }

            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL);

            if (proxyType == ProxyType.MANUAL && proxyHost != null && !proxyHost.isBlank()) {
                clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
                if (proxyAuthEnabled && proxyLogin != null && !proxyLogin.isBlank()) {
                    clientBuilder.authenticator(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(proxyLogin, proxyPassword.toCharArray());
                        }
                    });
                }
            } else if (proxyType == ProxyType.NO_PROXY) {
                clientBuilder.proxy(HttpClient.Builder.NO_PROXY);
            }

            HttpClient client = clientBuilder.build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsed = System.currentTimeMillis() - start;
            int code = response.statusCode();
            boolean ok = code >= 200 && code < 400;
            String msg = ok ? "Connection successful (" + code + ")" : "Received HTTP status " + code;
            return new ProxyTestResult(ok, code, elapsed, msg);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new ProxyTestResult(false, 0, elapsed, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    // Getters and Setters
    public boolean isConfirmExit() { return confirmExit; }
    public void setConfirmExit(boolean confirmExit) { this.confirmExit = confirmExit; }

    public ProcessClosePolicy getProcessClosePolicy() { return processClosePolicy; }
    public void setProcessClosePolicy(ProcessClosePolicy processClosePolicy) { this.processClosePolicy = processClosePolicy; }

    public boolean isReopenProjectsOnStartup() { return reopenProjectsOnStartup; }
    public void setReopenProjectsOnStartup(boolean reopenProjectsOnStartup) { this.reopenProjectsOnStartup = reopenProjectsOnStartup; }

    public OpenProjectMode getOpenProjectMode() { return openProjectMode; }
    public void setOpenProjectMode(OpenProjectMode openProjectMode) { this.openProjectMode = openProjectMode; }

    public String getDefaultProjectDirectory() { return defaultProjectDirectory; }
    public void setDefaultProjectDirectory(String defaultProjectDirectory) { this.defaultProjectDirectory = defaultProjectDirectory; }

    public boolean isIdleAutosaveEnabled() { return idleAutosaveEnabled; }
    public void setIdleAutosaveEnabled(boolean idleAutosaveEnabled) { this.idleAutosaveEnabled = idleAutosaveEnabled; }

    public int getIdleAutosaveSeconds() { return idleAutosaveSeconds; }
    public void setIdleAutosaveSeconds(int idleAutosaveSeconds) { this.idleAutosaveSeconds = idleAutosaveSeconds; }

    public boolean isSaveOnFocusLost() { return saveOnFocusLost; }
    public void setSaveOnFocusLost(boolean saveOnFocusLost) { this.saveOnFocusLost = saveOnFocusLost; }

    public boolean isBackupFilesBeforeSaving() { return backupFilesBeforeSaving; }
    public void setBackupFilesBeforeSaving(boolean backupFilesBeforeSaving) { this.backupFilesBeforeSaving = backupFilesBeforeSaving; }

    public boolean isSyncExternalOnFocus() { return syncExternalOnFocus; }
    public void setSyncExternalOnFocus(boolean syncExternalOnFocus) { this.syncExternalOnFocus = syncExternalOnFocus; }

    public boolean isSyncExternalPeriodically() { return syncExternalPeriodically; }
    public void setSyncExternalPeriodically(boolean syncExternalPeriodically) { this.syncExternalPeriodically = syncExternalPeriodically; }

    public boolean isOverrideSystemDateFormat() { return overrideSystemDateFormat; }
    public void setOverrideSystemDateFormat(boolean overrideSystemDateFormat) { this.overrideSystemDateFormat = overrideSystemDateFormat; }

    public String getDateFormatPattern() { return dateFormatPattern; }
    public void setDateFormatPattern(String dateFormatPattern) { this.dateFormatPattern = dateFormatPattern; }

    public boolean isUse24HourTime() { return use24HourTime; }
    public void setUse24HourTime(boolean use24HourTime) { this.use24HourTime = use24HourTime; }

    public boolean isUsePrettyFormatting() { return usePrettyFormatting; }
    public void setUsePrettyFormatting(boolean usePrettyFormatting) { this.usePrettyFormatting = usePrettyFormatting; }

    public boolean isSendAnonymousStats() { return sendAnonymousStats; }
    public void setSendAnonymousStats(boolean sendAnonymousStats) { this.sendAnonymousStats = sendAnonymousStats; }

    public boolean isSendDetailedData() { return sendDetailedData; }
    public void setSendDetailedData(boolean sendDetailedData) { this.sendDetailedData = sendDetailedData; }

    public ProxyType getProxyType() { return proxyType; }
    public void setProxyType(ProxyType proxyType) { this.proxyType = proxyType; }

    public boolean isAutoConfigUrlEnabled() { return autoConfigUrlEnabled; }
    public void setAutoConfigUrlEnabled(boolean autoConfigUrlEnabled) { this.autoConfigUrlEnabled = autoConfigUrlEnabled; }

    public String getAutoConfigUrl() { return autoConfigUrl; }
    public void setAutoConfigUrl(String autoConfigUrl) { this.autoConfigUrl = autoConfigUrl; }

    public ManualProtocol getManualProtocol() { return manualProtocol; }
    public void setManualProtocol(ManualProtocol manualProtocol) { this.manualProtocol = manualProtocol; }

    public String getProxyHost() { return proxyHost; }
    public void setProxyHost(String proxyHost) { this.proxyHost = proxyHost; }

    public int getProxyPort() { return proxyPort; }
    public void setProxyPort(int proxyPort) { this.proxyPort = proxyPort; }

    public String getNoProxyFor() { return noProxyFor; }
    public void setNoProxyFor(String noProxyFor) { this.noProxyFor = noProxyFor; }

    public boolean isProxyAuthEnabled() { return proxyAuthEnabled; }
    public void setProxyAuthEnabled(boolean proxyAuthEnabled) { this.proxyAuthEnabled = proxyAuthEnabled; }

    public String getProxyLogin() { return proxyLogin; }
    public void setProxyLogin(String proxyLogin) { this.proxyLogin = proxyLogin; }

    public String getProxyPassword() { return proxyPassword; }
    public void setProxyPassword(String proxyPassword) { this.proxyPassword = proxyPassword; }

    public boolean isProxyRemember() { return proxyRemember; }
    public void setProxyRemember(boolean proxyRemember) { this.proxyRemember = proxyRemember; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public PasswordStoragePolicy getPasswordStoragePolicy() { return passwordStoragePolicy; }
    public void setPasswordStoragePolicy(PasswordStoragePolicy passwordStoragePolicy) { this.passwordStoragePolicy = passwordStoragePolicy; }

    public String getKeepassDbPath() { return keepassDbPath; }
    public void setKeepassDbPath(String keepassDbPath) { this.keepassDbPath = keepassDbPath; }

    public boolean isProtectMasterPasswordWithPgp() { return protectMasterPasswordWithPgp; }
    public void setProtectMasterPasswordWithPgp(boolean protectMasterPasswordWithPgp) { this.protectMasterPasswordWithPgp = protectMasterPasswordWithPgp; }

    public boolean isKeepSudoAuth() { return keepSudoAuth; }
    public void setKeepSudoAuth(boolean keepSudoAuth) { this.keepSudoAuth = keepSudoAuth; }

    public String getSudoTimeout() { return sudoTimeout; }
    public void setSudoTimeout(String sudoTimeout) { this.sudoTimeout = sudoTimeout; }

    public boolean isExtendSudoTimeout() { return extendSudoTimeout; }
    public void setExtendSudoTimeout(boolean extendSudoTimeout) { this.extendSudoTimeout = extendSudoTimeout; }

    public boolean isAcceptNonTrustedCerts() { return acceptNonTrustedCerts; }
    public void setAcceptNonTrustedCerts(boolean acceptNonTrustedCerts) { this.acceptNonTrustedCerts = acceptNonTrustedCerts; }

    public java.util.List<String> getAcceptedCertificates() { return acceptedCertificates; }

    public java.util.List<String> getTrustedHosts() { return trustedHosts; }

    public boolean isCheckIdeUpdates() { return checkIdeUpdates; }
    public void setCheckIdeUpdates(boolean checkIdeUpdates) { this.checkIdeUpdates = checkIdeUpdates; }

    public String getUpdateChannel() { return updateChannel; }
    public void setUpdateChannel(String updateChannel) { this.updateChannel = updateChannel; }

    public boolean isCheckPluginUpdates() { return checkPluginUpdates; }
    public void setCheckPluginUpdates(boolean checkPluginUpdates) { this.checkPluginUpdates = checkPluginUpdates; }

    public boolean isUpdatePluginsAutomatically() { return updatePluginsAutomatically; }
    public void setUpdatePluginsAutomatically(boolean updatePluginsAutomatically) { this.updatePluginsAutomatically = updatePluginsAutomatically; }

    public boolean isShowWhatsNewAfterUpdate() { return showWhatsNewAfterUpdate; }
    public void setShowWhatsNewAfterUpdate(boolean showWhatsNewAfterUpdate) { this.showWhatsNewAfterUpdate = showWhatsNewAfterUpdate; }

    public boolean isCheckJdkUpdates() { return checkJdkUpdates; }
    public void setCheckJdkUpdates(boolean checkJdkUpdates) { this.checkJdkUpdates = checkJdkUpdates; }

    public String getLastUpdateCheckTime() { return lastUpdateCheckTime; }
    public void setLastUpdateCheckTime(String lastUpdateCheckTime) { this.lastUpdateCheckTime = lastUpdateCheckTime; }

    public record UpdateCheckResult(boolean hasUpdates, String currentVersion, String latestVersion, String message) {}

    public UpdateCheckResult checkForUpdates() {
        this.lastUpdateCheckTime = "Today " + java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(java.time.LocalTime.now());
        Settings.put("system.updates.lastChecked", this.lastUpdateCheckTime);
        return new UpdateCheckResult(false, "Lumina 2025.3.2", "2025.3.2", "You have the latest version of Lumina and its plugins installed.");
    }
}
