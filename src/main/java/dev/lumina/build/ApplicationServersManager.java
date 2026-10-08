package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager service for configured Application Servers and SPI providers in Lumina IDE.
 */
public class ApplicationServersManager {

    public static final String KEY_SERVER_COUNT = "appservers.count";
    public static final String KEY_PREFIX = "appservers.";

    private static ApplicationServersManager instance;

    private final Map<String, ApplicationServerProvider> providers = new LinkedHashMap<>();
    private final List<ApplicationServer> servers = new CopyOnWriteArrayList<>();

    private ApplicationServersManager() {
        // Register built-in server providers
        registerProvider(new WildFlyServerProvider());
        registerProvider(new TomcatServerProvider());
        registerProvider(new TomEEServerProvider());

        loadServers();
    }

    public static synchronized ApplicationServersManager getInstance() {
        if (instance == null) {
            instance = new ApplicationServersManager();
        }
        return instance;
    }

    public void registerProvider(ApplicationServerProvider provider) {
        if (provider != null) {
            providers.put(provider.getTypeId(), provider);
        }
    }

    public List<ApplicationServerProvider> getProviders() {
        return Collections.unmodifiableList(new ArrayList<>(providers.values()));
    }

    public ApplicationServerProvider getProvider(String typeId) {
        return providers.get(typeId);
    }

    public List<ApplicationServer> getServers() {
        List<ApplicationServer> copies = new ArrayList<>();
        for (ApplicationServer server : servers) {
            copies.add(server.clone());
        }
        return copies;
    }

    public void setServers(List<ApplicationServer> newServers) {
        servers.clear();
        if (newServers != null) {
            for (ApplicationServer s : newServers) {
                servers.add(s.clone());
            }
        }
        saveServers();
    }

    public void addServer(ApplicationServer server) {
        if (server != null) {
            servers.add(server.clone());
            saveServers();
        }
    }

    public void removeServer(ApplicationServer server) {
        if (server != null) {
            servers.removeIf(s -> s.getId().equals(server.getId()));
            saveServers();
        }
    }

    public void loadServers() {
        servers.clear();
        String countStr = Settings.get(KEY_SERVER_COUNT);
        if (countStr == null) return;
        try {
            int count = Integer.parseInt(countStr);
            for (int i = 0; i < count; i++) {
                String prefix = KEY_PREFIX + i + ".";
                String id = Settings.get(prefix + "id");
                String name = Settings.get(prefix + "name");
                String typeId = Settings.get(prefix + "typeId");
                String home = Settings.get(prefix + "home");
                String base = Settings.get(prefix + "base");
                String version = Settings.get(prefix + "version");
                String libsStr = Settings.get(prefix + "libs");

                if (name != null && typeId != null) {
                    ApplicationServer server = new ApplicationServer();
                    if (id != null) server.setId(id);
                    server.setName(name);
                    server.setTypeId(typeId);
                    server.setHomePath(home != null ? home : "");
                    server.setBaseDirectory(base != null ? base : "");
                    server.setVersion(version != null ? version : "");
                    if (libsStr != null && !libsStr.isBlank()) {
                        String[] parts = libsStr.split(";");
                        List<String> list = new ArrayList<>();
                        for (String p : parts) {
                            if (!p.isBlank()) list.add(p);
                        }
                        server.setLibraries(list);
                    }
                    servers.add(server);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public void saveServers() {
        Settings.set(KEY_SERVER_COUNT, String.valueOf(servers.size()));
        for (int i = 0; i < servers.size(); i++) {
            ApplicationServer server = servers.get(i);
            String prefix = KEY_PREFIX + i + ".";
            Settings.set(prefix + "id", server.getId());
            Settings.set(prefix + "name", server.getName());
            Settings.set(prefix + "typeId", server.getTypeId());
            Settings.set(prefix + "home", server.getHomePath() != null ? server.getHomePath() : "");
            Settings.set(prefix + "base", server.getBaseDirectory() != null ? server.getBaseDirectory() : "");
            Settings.set(prefix + "version", server.getVersion() != null ? server.getVersion() : "");
            Settings.set(prefix + "libs", String.join(";", server.getLibraries()));
        }
    }
}
