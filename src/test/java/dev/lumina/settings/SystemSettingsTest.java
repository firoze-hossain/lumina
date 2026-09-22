package dev.lumina.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class SystemSettingsTest {

    private SystemSettings settings;

    @BeforeEach
    void setUp() {
        dev.lumina.util.Settings.put("system.dateFormat.pattern", null);
        dev.lumina.util.Settings.put("system.dateFormat.use24Hour", null);
        settings = SystemSettings.getInstance();
        settings.resetToDefaults();
    }

    @Test
    void testDefaults() {
        assertTrue(settings.isConfirmExit(), "Confirm exit should be enabled by default");
        assertEquals(SystemSettings.ProcessClosePolicy.ASK, settings.getProcessClosePolicy());
        assertTrue(settings.isReopenProjectsOnStartup());
        assertEquals(SystemSettings.OpenProjectMode.ASK, settings.getOpenProjectMode());
        assertNotNull(settings.getDefaultProjectDirectory());
        assertFalse(settings.isIdleAutosaveEnabled());
        assertEquals(15, settings.getIdleAutosaveSeconds());
        assertTrue(settings.isSaveOnFocusLost());
        assertTrue(settings.isBackupFilesBeforeSaving());
        assertFalse(settings.isOverrideSystemDateFormat());
        assertEquals("dd MMM yyyy", settings.getDateFormatPattern());
        assertTrue(settings.isUse24HourTime());
        assertTrue(settings.isUsePrettyFormatting());
        assertFalse(settings.isSendAnonymousStats());
        assertFalse(settings.isSendDetailedData());
        assertEquals(SystemSettings.ProxyType.AUTO_DETECT, settings.getProxyType());
    }

    @Test
    void testPersistenceCycle() {
        settings.setConfirmExit(false);
        settings.setProcessClosePolicy(SystemSettings.ProcessClosePolicy.TERMINATE);
        settings.setReopenProjectsOnStartup(false);
        settings.setOpenProjectMode(SystemSettings.OpenProjectMode.NEW_WINDOW);
        settings.setDefaultProjectDirectory("/tmp/test-projects");
        settings.setIdleAutosaveEnabled(true);
        settings.setIdleAutosaveSeconds(30);
        settings.setDateFormatPattern("yyyy-MM-dd");
        settings.setUse24HourTime(false);
        settings.setUsePrettyFormatting(false);
        settings.setProxyType(SystemSettings.ProxyType.MANUAL);
        settings.setProxyHost("127.0.0.1");
        settings.setProxyPort(8080);
        settings.save();

        // Reload
        settings.load();
        assertFalse(settings.isConfirmExit());
        assertEquals(SystemSettings.ProcessClosePolicy.TERMINATE, settings.getProcessClosePolicy());
        assertFalse(settings.isReopenProjectsOnStartup());
        assertEquals(SystemSettings.OpenProjectMode.NEW_WINDOW, settings.getOpenProjectMode());
        assertEquals("/tmp/test-projects", settings.getDefaultProjectDirectory());
        assertTrue(settings.isIdleAutosaveEnabled());
        assertEquals(30, settings.getIdleAutosaveSeconds());
        assertEquals("yyyy-MM-dd", settings.getDateFormatPattern());
        assertFalse(settings.isUse24HourTime());
        assertFalse(settings.isUsePrettyFormatting());
        assertEquals(SystemSettings.ProxyType.MANUAL, settings.getProxyType());
        assertEquals("127.0.0.1", settings.getProxyHost());
        assertEquals(8080, settings.getProxyPort());

        // Restore clean defaults
        settings.setConfirmExit(true);
        settings.setProcessClosePolicy(SystemSettings.ProcessClosePolicy.ASK);
        settings.setReopenProjectsOnStartup(true);
        settings.setOpenProjectMode(SystemSettings.OpenProjectMode.ASK);
        settings.setIdleAutosaveEnabled(false);
        settings.setIdleAutosaveSeconds(15);
        settings.setDateFormatPattern("dd MMM yyyy");
        settings.setUse24HourTime(true);
        settings.setUsePrettyFormatting(true);
        settings.setProxyType(SystemSettings.ProxyType.AUTO_DETECT);
        settings.save();
    }

    @Test
    void testDateFormatting() {
        settings.setDateFormatPattern("yyyy-MM-dd");
        settings.setUse24HourTime(true);
        settings.setUsePrettyFormatting(false);

        LocalDateTime sample = LocalDateTime.of(2026, 9, 21, 14, 30);
        String formatted = settings.formatDateTime(sample);
        assertEquals("2026-09-21 14:30", formatted);

        settings.setUse24HourTime(false);
        String formatted12 = settings.formatDateTime(sample);
        assertTrue(formatted12.contains("02:30") || formatted12.contains("2:30"));
        assertTrue(formatted12.toUpperCase().contains("PM"));
    }

    @Test
    void testPrettyFormatting() {
        settings.setUsePrettyFormatting(true);
        settings.setUse24HourTime(true);

        LocalDateTime now = LocalDateTime.now();
        String justNow = settings.formatDateTime(now);
        assertEquals("Just now", justNow);

        LocalDateTime minutesAgo = now.minusMinutes(12);
        String minsAgoStr = settings.formatDateTime(minutesAgo);
        assertEquals("12 minutes ago", minsAgoStr);

        LocalDateTime yesterday = now.minusDays(1).withHour(10).withMinute(15);
        String yesterdayStr = settings.formatDateTime(yesterday);
        assertTrue(yesterdayStr.startsWith("Yesterday"));
    }

    @Test
    void testSamplePreview() {
        settings.setDateFormatPattern("dd MMM yyyy");
        settings.setUse24HourTime(true);
        String preview24 = settings.getSamplePreview();
        assertTrue(preview24.contains("31 Dec 2100 23:59"));

        settings.setUse24HourTime(false);
        String preview12 = settings.getSamplePreview();
        assertTrue(preview12.contains("11:59"));
    }

    @Test
    void testProxyApplyAndClearPasswords() {
        settings.setProxyType(SystemSettings.ProxyType.NO_PROXY);
        settings.apply();
        assertNull(System.getProperty("http.proxyHost"));

        settings.setProxyType(SystemSettings.ProxyType.MANUAL);
        settings.setManualProtocol(SystemSettings.ManualProtocol.HTTP);
        settings.setProxyHost("proxy.internal.corp");
        settings.setProxyPort(3128);
        settings.setNoProxyFor("localhost,*.local");
        settings.apply();

        assertEquals("proxy.internal.corp", System.getProperty("http.proxyHost"));
        assertEquals("3128", System.getProperty("http.proxyPort"));
        assertEquals("localhost|*.local", System.getProperty("http.nonProxyHosts"));

        // Clear password
        settings.setProxyPassword("secret123");
        settings.clearProxyPasswords();
        assertEquals("", settings.getProxyPassword());

        // Restore clean proxy state
        settings.setProxyType(SystemSettings.ProxyType.NO_PROXY);
        settings.apply();
    }

    @Test
    void testLanguageAndRegionSettings() {
        settings.setLanguage("Japanese");
        settings.setRegion("Japan");
        settings.save();

        settings.load();
        assertEquals("Japanese", settings.getLanguage());
        assertEquals("Japan", settings.getRegion());

        // Restore
        settings.setLanguage("English");
        settings.setRegion("Not specified");
        settings.save();
    }

    @Test
    void testPasswordsAndElevationSettings() {
        settings.setPasswordStoragePolicy(SystemSettings.PasswordStoragePolicy.KEEPASS);
        settings.setKeepassDbPath("/custom/path/c.kdbx");
        settings.setProtectMasterPasswordWithPgp(true);

        settings.setKeepSudoAuth(true);
        settings.setSudoTimeout("30 min");
        settings.setExtendSudoTimeout(false);
        settings.save();

        settings.load();
        assertEquals(SystemSettings.PasswordStoragePolicy.KEEPASS, settings.getPasswordStoragePolicy());
        assertEquals("/custom/path/c.kdbx", settings.getKeepassDbPath());
        assertTrue(settings.isProtectMasterPasswordWithPgp());

        assertTrue(settings.isKeepSudoAuth());
        assertEquals("30 min", settings.getSudoTimeout());
        assertFalse(settings.isExtendSudoTimeout());

        // Restore
        settings.setPasswordStoragePolicy(SystemSettings.PasswordStoragePolicy.NATIVE_KEYCHAIN);
        settings.setKeepSudoAuth(false);
        settings.setSudoTimeout("15 min");
        settings.setExtendSudoTimeout(true);
        settings.save();
    }

    @Test
    void testCertificatesAndTrustedHostsSettings() {
        settings.setAcceptNonTrustedCerts(true);
        settings.getAcceptedCertificates().clear();
        settings.getAcceptedCertificates().add("custom-root.crt");

        settings.getTrustedHosts().add("custom.mirror.org");
        settings.save();

        settings.load();
        assertTrue(settings.isAcceptNonTrustedCerts());
        assertTrue(settings.getAcceptedCertificates().contains("custom-root.crt"));
        assertTrue(settings.getTrustedHosts().contains("custom.mirror.org"));

        // Restore
        settings.setAcceptNonTrustedCerts(false);
        settings.getAcceptedCertificates().clear();
        settings.getTrustedHosts().remove("custom.mirror.org");
        settings.save();
    }

    @Test
    void testUpdatesSettingsAndCheck() {
        settings.setCheckIdeUpdates(true);
        settings.setUpdateChannel("Early Access Program");
        settings.setCheckPluginUpdates(true);
        settings.setUpdatePluginsAutomatically(true);
        settings.setShowWhatsNewAfterUpdate(false);
        settings.setCheckJdkUpdates(false);
        settings.save();

        settings.load();
        assertTrue(settings.isCheckIdeUpdates());
        assertEquals("Early Access Program", settings.getUpdateChannel());
        assertTrue(settings.isCheckPluginUpdates());
        assertTrue(settings.isUpdatePluginsAutomatically());
        assertFalse(settings.isShowWhatsNewAfterUpdate());
        assertFalse(settings.isCheckJdkUpdates());

        // Test checkForUpdates
        SystemSettings.UpdateCheckResult result = settings.checkForUpdates();
        assertNotNull(result);
        assertNotNull(result.message());
        assertTrue(settings.getLastUpdateCheckTime().startsWith("Today"));

        // Restore
        settings.setUpdateChannel("Stable Releases");
        settings.setUpdatePluginsAutomatically(false);
        settings.setShowWhatsNewAfterUpdate(true);
        settings.setCheckJdkUpdates(true);
        settings.save();
    }
}
