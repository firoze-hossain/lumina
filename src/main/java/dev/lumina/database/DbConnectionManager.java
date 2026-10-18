package dev.lumina.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages database profiles, persistence to ~/.lumina/database_connections.json,
 * and active connections.
 */
public class DbConnectionManager {

    private static final DbConnectionManager INSTANCE = new DbConnectionManager();
    public static DbConnectionManager getInstance() { return INSTANCE; }

    private final List<DbConnectionProfile> profiles = new CopyOnWriteArrayList<>();
    private final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path storagePath;

    private DbConnectionManager() {
        String userHome = System.getProperty("user.home", ".");
        this.storagePath = Path.of(userHome, ".lumina", "database_connections.json");
        loadProfiles();
    }

    public List<DbConnectionProfile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    public synchronized void addOrUpdateProfile(DbConnectionProfile profile) {
        if (profile == null) return;
        int idx = -1;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(profile.getId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            profiles.set(idx, profile);
            disconnect(profile.getId());
        } else {
            profiles.add(profile);
        }
        saveProfiles();
        notifyListeners();
    }

    public synchronized void removeProfile(String profileId) {
        disconnect(profileId);
        profiles.removeIf(p -> p.getId().equals(profileId));
        saveProfiles();
        notifyListeners();
    }

    public synchronized Connection getConnection(DbConnectionProfile profile) throws Exception {
        Connection conn = activeConnections.get(profile.getId());
        if (conn != null && !conn.isClosed()) {
            return conn;
        }
        conn = DbMetadataService.createConnection(profile);
        activeConnections.put(profile.getId(), conn);
        return conn;
    }

    public boolean isConnected(String profileId) {
        Connection conn = activeConnections.get(profileId);
        try {
            return conn != null && !conn.isClosed();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public synchronized void disconnect(String profileId) {
        Connection conn = activeConnections.remove(profileId);
        if (conn != null) {
            try {
                conn.close();
            } catch (Throwable ignored) {}
        }
    }

    public synchronized void shutdown() {
        for (Connection c : activeConnections.values()) {
            try {
                c.close();
            } catch (Throwable ignored) {}
        }
        activeConnections.clear();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Throwable ignored) {}
        }
    }

    private synchronized void loadProfiles() {
        try {
            if (Files.exists(storagePath)) {
                try (Reader r = Files.newBufferedReader(storagePath)) {
                    Type listType = new TypeToken<List<DbConnectionProfile>>(){}.getType();
                    List<DbConnectionProfile> loaded = gson.fromJson(r, listType);
                    if (loaded != null) {
                        profiles.clear();
                        profiles.addAll(loaded);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // If completely empty, add a default sample SQLite / Postgres profile
        if (profiles.isEmpty()) {
            DbConnectionProfile postgres = new DbConnectionProfile(DbEngineType.POSTGRESQL);
            postgres.setName("PostgreSQL - localhost");
            profiles.add(postgres);
            saveProfiles();
        }
    }

    private synchronized void saveProfiles() {
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            try (Writer w = Files.newBufferedWriter(storagePath)) {
                gson.toJson(profiles, w);
            }
        } catch (Throwable ignored) {}
    }
}
