package dev.lumina.project;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches the live project catalog from start.spring.io/metadata/client \u2014
 * the same endpoint and the same JSON format IntelliJ's Spring Initializr
 * wizard queries. Current Spring Boot versions and the full, current
 * dependency list (with real descriptions) come from here, not from a
 * baked-in list, so the New Project wizard stays correct as Spring ships
 * new releases and starters.
 *
 * <p>Network access and JSON parsing are separated (fetch() vs. parse())
 * so the parsing logic can be exercised with a saved JSON sample without
 * a live connection.
 */
public final class SpringInitializrMetadata {

    public record Dependency(String id, String name, String description, String versionRange) {
        public Dependency(String id, String name, String description) {
            this(id, name, description, null);
        }

        public boolean isCompatibleWith(String bootVersion) {
            if (versionRange == null || versionRange.isBlank()) return true;
            if (bootVersion == null || bootVersion.isBlank()) return true;
            return isVersionCompatible(bootVersion.trim(), versionRange.trim());
        }
    }

    public record Category(String name, List<Dependency> dependencies) {
    }

    public record Metadata(
            List<String> bootVersions,
            String defaultBootVersion,
            List<String> javaVersions,
            String defaultJavaVersion,
            List<String> packagings,
            String defaultPackaging,
            List<String> languages,
            String defaultLanguage,
            List<String> types,
            String defaultType,
            String defaultGroupId,
            String defaultArtifactId,
            String defaultPackageName,
            List<Category> categories
    ) {
        public Metadata(List<String> bootVersions, String defaultBootVersion, List<Category> categories) {
            this(bootVersions, defaultBootVersion,
                    List.of("17", "21", "23", "25"), "17",
                    List.of("jar", "war"), "jar",
                    List.of("java", "kotlin", "groovy"), "java",
                    List.of("gradle-project", "gradle-project-kotlin", "maven-project"), "gradle-project",
                    "org.example", "demo", "com.example.demo",
                    categories);
        }
    }

    public static final String DEFAULT_SERVER_URL = "https://start.spring.io";
    private static final String ACCEPT = "application/vnd.initializr.v2.2+json";
    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userNodeForPackage(SpringInitializrMetadata.class);

    private SpringInitializrMetadata() {
    }

    private static volatile String activeServerUrl = DEFAULT_SERVER_URL;

    public static String getServerUrl() {
        try {
            return PREFS.get("spring.server.url", activeServerUrl);
        } catch (Exception ignored) {
            return activeServerUrl;
        }
    }

    public static void setServerUrl(String url) {
        if (url != null && !url.isBlank()) {
            activeServerUrl = normalizeServerUrl(url);
            try {
                PREFS.put("spring.server.url", activeServerUrl);
            } catch (Exception ignored) {}
        }
    }

    public static String normalizeServerUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_SERVER_URL;
        }
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://" + u;
        }
        u = u.replaceAll("/+(metadata(/client)?|starter(\\.zip)?)/*$", "");
        return u.replaceAll("/+$", "");
    }

    /**
     * Blocking network call using configured or default server URL.
     */
    public static Metadata fetch() throws IOException {
        return fetch(getServerUrl());
    }

    /**
     * Blocking network call for a specific server URL.
     */
    public static Metadata fetch(String serverUrl) throws IOException {
        String base = normalizeServerUrl(serverUrl);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // 1. Try /metadata/client
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/metadata/client"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", ACCEPT)
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                return parse(response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Metadata request interrupted", e);
        } catch (Exception ignored) {}

        // 2. Try root endpoint with Accept header
        try {
            HttpRequest rootReq = HttpRequest.newBuilder(URI.create(base))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", ACCEPT)
                    .header("User-Agent", "Lumina-IDE")
                    .GET()
                    .build();
            response = client.send(rootReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                return parse(response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Metadata request interrupted", e);
        } catch (Exception ignored) {}

        throw new IOException("Could not reach Spring Initializr at " + base
                + (response != null ? " (HTTP " + response.statusCode() + ")" : ""));
    }

    /** Pure parsing: no network, safe to unit-test with a saved JSON body. */
    public static Metadata parse(String json) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            List<String> versions = new ArrayList<>();
            String defaultVersion = "";
            if (root.has("bootVersion")) {
                JsonObject bootVersion = root.getAsJsonObject("bootVersion");
                if (bootVersion.has("default")) {
                    defaultVersion = bootVersion.get("default").getAsString();
                }
                if (bootVersion.has("values")) {
                    for (JsonElement el : bootVersion.getAsJsonArray("values")) {
                        JsonObject v = el.getAsJsonObject();
                        if (v.has("id")) versions.add(v.get("id").getAsString());
                    }
                }
            }
            if (versions.isEmpty()) {
                versions = List.of("3.4.3", "3.3.9", "3.2.12");
                defaultVersion = "3.4.3";
            }

            List<String> javaVersions = new ArrayList<>();
            String defaultJavaVersion = "17";
            if (root.has("javaVersion")) {
                JsonObject jv = root.getAsJsonObject("javaVersion");
                if (jv.has("default")) {
                    defaultJavaVersion = jv.get("default").getAsString();
                }
                if (jv.has("values")) {
                    for (JsonElement el : jv.getAsJsonArray("values")) {
                        JsonObject v = el.getAsJsonObject();
                        if (v.has("id")) javaVersions.add(v.get("id").getAsString());
                    }
                }
            }
            if (javaVersions.isEmpty()) {
                javaVersions = List.of("17", "21", "23", "25");
            }

            List<String> packagings = new ArrayList<>();
            String defaultPackaging = "jar";
            if (root.has("packaging")) {
                JsonObject pkg = root.getAsJsonObject("packaging");
                if (pkg.has("default")) {
                    defaultPackaging = pkg.get("default").getAsString();
                }
                if (pkg.has("values")) {
                    for (JsonElement el : pkg.getAsJsonArray("values")) {
                        JsonObject v = el.getAsJsonObject();
                        if (v.has("id")) packagings.add(v.get("id").getAsString());
                    }
                }
            }
            if (packagings.isEmpty()) {
                packagings = List.of("jar", "war");
            }

            List<String> languages = new ArrayList<>();
            String defaultLanguage = "java";
            if (root.has("language")) {
                JsonObject lang = root.getAsJsonObject("language");
                if (lang.has("default")) {
                    defaultLanguage = lang.get("default").getAsString();
                }
                if (lang.has("values")) {
                    for (JsonElement el : lang.getAsJsonArray("values")) {
                        JsonObject v = el.getAsJsonObject();
                        if (v.has("id")) languages.add(v.get("id").getAsString());
                    }
                }
            }
            if (languages.isEmpty()) {
                languages = List.of("java", "kotlin", "groovy");
            }

            List<String> types = new ArrayList<>();
            String defaultType = "gradle-project";
            if (root.has("type")) {
                JsonObject typ = root.getAsJsonObject("type");
                if (typ.has("default")) {
                    defaultType = typ.get("default").getAsString();
                }
                if (typ.has("values")) {
                    for (JsonElement el : typ.getAsJsonArray("values")) {
                        JsonObject v = el.getAsJsonObject();
                        if (v.has("id")) types.add(v.get("id").getAsString());
                    }
                }
            }
            if (types.isEmpty()) {
                types = List.of("gradle-project", "gradle-project-kotlin", "maven-project");
            }

            String defaultGroupId = root.has("groupId") && root.getAsJsonObject("groupId").has("default")
                    ? root.getAsJsonObject("groupId").get("default").getAsString() : "org.example";
            String defaultArtifactId = root.has("artifactId") && root.getAsJsonObject("artifactId").has("default")
                    ? root.getAsJsonObject("artifactId").get("default").getAsString() : "demo";
            String defaultPackageName = root.has("packageName") && root.getAsJsonObject("packageName").has("default")
                    ? root.getAsJsonObject("packageName").get("default").getAsString() : "com.example.demo";

            List<Category> categories = new ArrayList<>();
            if (root.has("dependencies")) {
                JsonObject dependencies = root.getAsJsonObject("dependencies");
                if (dependencies.has("values")) {
                    for (JsonElement catEl : dependencies.getAsJsonArray("values")) {
                        JsonObject cat = catEl.getAsJsonObject();
                        String categoryName = cat.has("name")
                                ? cat.get("name").getAsString() : "Other";
                        List<Dependency> deps = new ArrayList<>();
                        if (cat.has("values")) {
                            for (JsonElement depEl : cat.getAsJsonArray("values")) {
                                Dependency dep = readDependency(depEl.getAsJsonObject());
                                if (dep != null) deps.add(dep);
                            }
                        }
                        if (!deps.isEmpty()) {
                            categories.add(new Category(categoryName, deps));
                        }
                    }
                }
            }
            if (categories.isEmpty()) {
                categories = getFallbackCategories();
            }

            return new Metadata(
                    versions, defaultVersion,
                    javaVersions, defaultJavaVersion,
                    packagings, defaultPackaging,
                    languages, defaultLanguage,
                    types, defaultType,
                    defaultGroupId, defaultArtifactId, defaultPackageName,
                    categories
            );
        } catch (IllegalStateException | ClassCastException | NullPointerException e) {
            throw new IOException("Unexpected metadata format: " + e.getMessage(), e);
        }
    }

    private static Dependency readDependency(JsonObject obj) {
        if (!obj.has("id") || !obj.has("name")) return null;
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String description = obj.has("description") && !obj.get("description").isJsonNull()
                ? obj.get("description").getAsString() : "";
        String versionRange = obj.has("versionRange") && !obj.get("versionRange").isJsonNull()
                ? obj.get("versionRange").getAsString() : null;
        return new Dependency(id, name, description, versionRange);
    }

    /**
     * Evaluates whether a given Spring Boot version satisfies a Maven-style version range.
     * Examples: "[3.0.0, )", "[2.5.0, 3.2.0)", "3.0.0"
     */
    public static boolean isVersionCompatible(String version, String range) {
        if (range == null || range.isBlank()) return true;
        if (version == null || version.isBlank()) return true;

        String r = range.trim();
        if (!r.startsWith("[") && !r.startsWith("(")) {
            return version.equalsIgnoreCase(r);
        }

        boolean lowerInclusive = r.startsWith("[");
        boolean upperInclusive = r.endsWith("]");
        int comma = r.indexOf(',');
        if (comma < 0) {
            String target = r.substring(1, r.length() - 1).trim();
            return version.equalsIgnoreCase(target);
        }

        String lowerStr = r.substring(1, comma).trim();
        String upperStr = r.substring(comma + 1, r.length() - 1).trim();

        if (!lowerStr.isEmpty()) {
            int cmp = compareVersionStrings(version, lowerStr);
            if (lowerInclusive ? cmp < 0 : cmp <= 0) {
                return false;
            }
        }

        if (!upperStr.isEmpty()) {
            int cmp = compareVersionStrings(version, upperStr);
            if (upperInclusive ? cmp > 0 : cmp >= 0) {
                return false;
            }
        }

        return true;
    }

    public static int compareVersionStrings(String v1, String v2) {
        String[] parts1 = v1.split("[.\\-]");
        String[] parts2 = v2.split("[.\\-]");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            String p1 = i < parts1.length ? parts1[i] : "0";
            String p2 = i < parts2.length ? parts2[i] : "0";
            try {
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                if (n1 != n2) return Integer.compare(n1, n2);
            } catch (NumberFormatException e) {
                int cmp = p1.compareToIgnoreCase(p2);
                if (cmp != 0) return cmp;
            }
        }
        return 0;
    }

    public static Metadata getFallbackMetadata() {
        return new Metadata(
                List.of("3.4.3", "3.3.9", "3.2.12"),
                "3.4.3",
                List.of("17", "21", "23", "25"),
                "17",
                List.of("jar", "war"),
                "jar",
                List.of("java", "kotlin", "groovy"),
                "java",
                List.of("gradle-project", "gradle-project-kotlin", "maven-project"),
                "gradle-project",
                "org.example",
                "demo",
                "com.example.demo",
                getFallbackCategories()
        );
    }

    public static List<Category> getFallbackCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category("Developer Tools", List.of(
                new Dependency("native", "GraalVM Native Support", "Support for compiling Spring applications to native executables using the GraalVM native-image compiler.", "[3.0.0-M1,)"),
                new Dependency("graphql-dgs-codegen", "GraphQL DGS Code Generation", "Generate Java types and APIs from GraphQL schema.", "[3.0.0,)"),
                new Dependency("devtools", "Spring Boot DevTools", "Provides fast application restarts, LiveReload, and configurations for enhanced developing experience.", null),
                new Dependency("lombok", "Lombok", "Java annotation library which helps to reduce boilerplate code.", null),
                new Dependency("configuration-processor", "Spring Configuration Processor", "Generate metadata for developers to offer contextual help and code completion when working with custom configuration keys.", null),
                new Dependency("docker-compose", "Docker Compose Support", "Provides development-time Docker Compose integration.", "[3.1.0,)"),
                new Dependency("modulith", "Spring Modulith", "Opinionated toolkit for building modular, maintainable applications with Spring Boot.", "[3.1.0,)")
        )));
        list.add(new Category("Web", List.of(
                new Dependency("web", "Spring Web", "Build web, including RESTful, applications using Spring MVC. Uses Apache Tomcat as the default embedded container.", null),
                new Dependency("webflux", "Spring Reactive Web", "Build reactive web applications with Spring WebFlux and Netty.", null),
                new Dependency("graphql", "Spring for GraphQL", "Build GraphQL applications with Spring for GraphQL and GraphQL Java.", "[3.0.0,)"),
                new Dependency("websocket", "WebSocket", "Build WebSocket applications using Spring WebSocket message handling.", null),
                new Dependency("jersey", "Jersey", "Developing RESTful Web services using JAX-RS and Jersey.", null)
        )));
        list.add(new Category("Template Engines", List.of(
                new Dependency("thymeleaf", "Thymeleaf", "A modern server-side Java template engine for both web and standalone environments.", null),
                new Dependency("freemarker", "Apache Freemarker", "Java library to generate text output based on templates and changing data.", null),
                new Dependency("mustache", "Mustache", "Logic-less templates. There are no if statements, else clauses, or for loops.", null)
        )));
        list.add(new Category("Security", List.of(
                new Dependency("security", "Spring Security", "Highly customizable authentication and access-control framework for Spring applications.", null),
                new Dependency("oauth2-client", "OAuth2 Client", "Spring Boot integration for Spring Security OAuth 2.0 Client.", null),
                new Dependency("oauth2-resource-server", "OAuth2 Resource Server", "Spring Boot integration for Spring Security OAuth 2.0 Resource Server.", null)
        )));
        list.add(new Category("SQL", List.of(
                new Dependency("data-jpa", "Spring Data JPA", "Persist data in SQL stores with Java Persistence API using Spring Data and Hibernate.", null),
                new Dependency("data-jdbc", "Spring Data JDBC", "Persist data in SQL stores with plain JDBC using Spring Data.", null),
                new Dependency("jdbc", "JDBC API", "Database Connectivity with JDBC.", null),
                new Dependency("postgresql", "PostgreSQL Driver", "A JDBC and R2DBC driver that allows Java programs to connect to a PostgreSQL database.", null),
                new Dependency("mysql", "MySQL Driver", "MySQL JDBC driver.", null),
                new Dependency("h2", "H2 Database", "Fast in-memory database that supports JDBC APIs and R2DBC access.", null),
                new Dependency("flyway", "Flyway Migration", "Version control for your database so you can migrate from any version to the latest version of the database.", null)
        )));
        list.add(new Category("NoSQL", List.of(
                new Dependency("data-mongodb", "Spring Data MongoDB", "Store data in flexible, JSON-like documents with MongoDB using Spring Data.", null),
                new Dependency("data-redis", "Spring Data Redis", "Advanced and high-performance key-value store using Spring Data Redis.", null)
        )));
        list.add(new Category("Messaging", List.of(
                new Dependency("amqp", "Spring for RabbitMQ", "Give your applications a common platform to send and receive messages using RabbitMQ.", null),
                new Dependency("kafka", "Spring for Apache Kafka", "Publish, subscribe, and process streams of records with Apache Kafka.", null)
        )));
        list.add(new Category("I/O", List.of(
                new Dependency("mail", "Java Mail Sender", "Send email using JavaMail and Spring Framework's JavaMailSender.", null),
                new Dependency("validation", "Validation", "Bean Validation with Hibernate Validator.", null)
        )));
        list.add(new Category("Ops", List.of(
                new Dependency("actuator", "Spring Boot Actuator", "Supports built in (or custom) endpoints that let you monitor and manage your application.", null)
        )));
        list.add(new Category("Observability", List.of(
                new Dependency("micrometer-tracing", "Micrometer Tracing", "Facade for the most popular tracer libraries to collect traces without vendor lock-in.", "[3.0.0,)")
        )));
        list.add(new Category("Testing", List.of(
                new Dependency("testcontainers", "Testcontainers", "Provide lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container.", "[3.1.0,)")
        )));
        list.add(new Category("Spring Cloud", List.of(
                new Dependency("cloud-starter", "Cloud Bootstrap", "Non-specific Spring Cloud features.", null)
        )));
        return list;
    }
}
