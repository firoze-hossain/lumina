package dev.lumina.project;

import com.google.gson.JsonArray;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client and models for code.quarkus.io API:
 * - /api/streams: fetches available Quarkus versions/streams
 * - /api/extensions: fetches extensions catalog with categories and descriptions
 *
 * Provides live fetching with timeout and parsing, along with an offline
 * fallback catalog so the IDE functions seamlessly without an active connection.
 */
public final class QuarkusMetadata {

    public record QuarkusStream(String key, String version, boolean recommended, String status) {
        @Override
        public String toString() {
            return version;
        }
    }

    public record QuarkusExtension(
            String id,
            String name,
            String category,
            String description,
            String guide,
            int order,
            List<String> keywords
    ) {
    }

    public record QuarkusCategory(String name, List<QuarkusExtension> extensions) {
    }

    public record QuarkusCatalog(
            List<QuarkusStream> streams,
            String defaultStreamKey,
            List<QuarkusCategory> categories
    ) {
    }

    public static final String DEFAULT_SERVER_URL = "https://code.quarkus.io";

    private QuarkusMetadata() {
    }

    /**
     * Normalizes a server URL string (e.g. "code.quarkus.io" -> "https://code.quarkus.io").
     */
    public static String normalizeServerUrl(String url) {
        if (url == null || url.isBlank()) return DEFAULT_SERVER_URL;
        String clean = url.trim();
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://" + clean;
        }
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    /**
     * Fetches streams and extensions from the given server.
     * Throws on network failure so caller can fall back to offline catalog.
     */
    public static QuarkusCatalog fetchCatalog(String serverUrl, String streamKey) throws IOException {
        String base = normalizeServerUrl(serverUrl);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // 1. Fetch streams
        List<QuarkusStream> streams = fetchStreams(client, base);
        String defaultKey = streamKey;
        if (defaultKey == null || defaultKey.isBlank()) {
            defaultKey = streams.stream()
                    .filter(QuarkusStream::recommended)
                    .map(QuarkusStream::key)
                    .findFirst()
                    .orElse(streams.isEmpty() ? "io.quarkus.platform:3.39" : streams.get(0).key());
        }

        // 2. Fetch extensions for the selected stream
        List<QuarkusCategory> categories = fetchExtensions(client, base, defaultKey);

        return new QuarkusCatalog(streams, defaultKey, categories);
    }

    public static List<QuarkusStream> fetchStreams(HttpClient client, String baseUrl) throws IOException {
        URI uri = URI.create(baseUrl + "/api/streams");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " fetching streams from " + uri);
            }
            return parseStreams(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Quarkus streams request interrupted", e);
        }
    }

    public static List<QuarkusCategory> fetchExtensions(HttpClient client, String baseUrl, String streamKey)
            throws IOException {
        String url = baseUrl + "/api/extensions";
        if (streamKey != null && !streamKey.isBlank()) {
            url += "?streamKey=" + java.net.URLEncoder.encode(streamKey, java.nio.charset.StandardCharsets.UTF_8);
        }
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " fetching extensions from " + uri);
            }
            return parseExtensions(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Quarkus extensions request interrupted", e);
        }
    }

    /**
     * Pure parser for /api/streams JSON array.
     */
    public static List<QuarkusStream> parseStreams(String json) throws IOException {
        List<QuarkusStream> result = new ArrayList<>();
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonArray()) {
                throw new IOException("Expected JSON array from /api/streams");
            }
            for (JsonElement item : el.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                JsonObject obj = item.getAsJsonObject();
                String key = obj.has("key") ? obj.get("key").getAsString() : "";
                String version = obj.has("quarkusCoreVersion")
                        ? obj.get("quarkusCoreVersion").getAsString()
                        : (obj.has("version") ? obj.get("version").getAsString() : key);
                boolean rec = obj.has("recommended") && obj.get("recommended").getAsBoolean();
                String status = obj.has("status") ? obj.get("status").getAsString() : "FINAL";
                if (!key.isBlank()) {
                    result.add(new QuarkusStream(key, version, rec, status));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse Quarkus streams: " + e.getMessage(), e);
        }
        if (result.isEmpty()) {
            throw new IOException("No streams found in response");
        }
        return result;
    }

    /**
     * Pure parser for /api/extensions JSON array.
     */
    public static List<QuarkusCategory> parseExtensions(String json) throws IOException {
        Map<String, List<QuarkusExtension>> categorized = new LinkedHashMap<>();
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonArray()) {
                throw new IOException("Expected JSON array from /api/extensions");
            }
            for (JsonElement item : el.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                JsonObject obj = item.getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : "";
                String name = obj.has("name") ? obj.get("name").getAsString() : id;
                String category = obj.has("category") && !obj.get("category").isJsonNull()
                        ? obj.get("category").getAsString() : "Uncategorized";
                String description = obj.has("description") && !obj.get("description").isJsonNull()
                        ? obj.get("description").getAsString() : "";
                String guide = obj.has("guide") && !obj.get("guide").isJsonNull()
                        ? obj.get("guide").getAsString() : "";
                int order = obj.has("order") ? obj.get("order").getAsInt() : 0;

                List<String> keywords = new ArrayList<>();
                if (obj.has("keywords") && obj.get("keywords").isJsonArray()) {
                    for (JsonElement kw : obj.getAsJsonArray("keywords")) {
                        keywords.add(kw.getAsString());
                    }
                }

                if (!id.isBlank()) {
                    categorized.computeIfAbsent(category, k -> new ArrayList<>())
                            .add(new QuarkusExtension(id, name, category, description, guide, order, keywords));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse Quarkus extensions: " + e.getMessage(), e);
        }

        if (categorized.isEmpty()) {
            throw new IOException("No extensions found in response");
        }

        List<QuarkusCategory> result = new ArrayList<>();
        for (Map.Entry<String, List<QuarkusExtension>> entry : categorized.entrySet()) {
            result.add(new QuarkusCategory(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    // =========================================================================
    // Comprehensive Bundled Offline Fallback Catalog (Matching IntelliJ Screenshots)
    // =========================================================================

    public static final List<QuarkusStream> FALLBACK_STREAMS = List.of(
            new QuarkusStream("io.quarkus.platform:3.39", "3.39.3", true, "FINAL"),
            new QuarkusStream("io.quarkus.platform:3.38", "3.38.1", false, "FINAL"),
            new QuarkusStream("io.quarkus.platform:3.15", "3.15.3", false, "LTS")
    );

    public static final List<QuarkusCategory> FALLBACK_CATEGORIES = List.of(
            new QuarkusCategory("Web", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-rest", "REST", "Web",
                            "Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)",
                            "https://quarkus.io/guides/rest", 1, List.of("jaxrs", "rest", "web", "http")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-jackson", "REST Jackson", "Web",
                            "Jackson serialization support for Quarkus REST",
                            "https://quarkus.io/guides/rest#json-jackson", 2, List.of("json", "jackson", "rest")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-jsonb", "REST JSON-B", "Web",
                            "JSON-B serialization support for Quarkus REST",
                            "https://quarkus.io/guides/rest#jsonb", 3, List.of("json", "jsonb", "rest")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-jaxb", "REST JAXB", "Web",
                            "XML JAXB serialization support for Quarkus REST",
                            "https://quarkus.io/guides/rest#xml-jaxb", 4, List.of("xml", "jaxb", "rest")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-kotlin-serialization", "REST Kotlin Serialization", "Web",
                            "Kotlin serialization support for Quarkus REST",
                            "https://quarkus.io/guides/rest#kotlin-serialization", 5, List.of("kotlin", "serialization")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-qute", "REST Qute", "Web",
                            "Qute templating engine integration for Quarkus REST",
                            "https://quarkus.io/guides/qute", 6, List.of("template", "qute", "html")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-links", "REST Links", "Web",
                            "Web Linking (RFC 8288) support for Quarkus REST",
                            "https://quarkus.io/guides/rest-links", 7, List.of("links", "hateoas")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-client", "REST Client", "Web",
                            "Jakarta REST client implementation for Quarkus REST",
                            "https://quarkus.io/guides/rest-client", 8, List.of("client", "http", "rest")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-client-jackson", "REST Client Jackson", "Web",
                            "Jackson serialization support for Quarkus REST Client",
                            "https://quarkus.io/guides/rest-client#json-jackson", 9, List.of("client", "jackson")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-client-jsonb", "REST Client JSON-B", "Web",
                            "JSON-B serialization support for Quarkus REST Client",
                            "https://quarkus.io/guides/rest-client#jsonb", 10, List.of("client", "jsonb")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-client-jaxb", "REST Client JAXB", "Web",
                            "XML JAXB serialization support for Quarkus REST Client",
                            "https://quarkus.io/guides/rest-client#jaxb", 11, List.of("client", "jaxb")),
                    new QuarkusExtension("io.quarkus:quarkus-rest-client-kotlin-serialization", "REST Client Kotlin Serialization", "Web",
                            "Kotlin serialization support for Quarkus REST Client",
                            "https://quarkus.io/guides/rest-client#kotlin", 12, List.of("client", "kotlin")),
                    new QuarkusExtension("io.quarkus:quarkus-resteasy", "RESTeasy Classic", "Web",
                            "RESTful web services using RESTEasy Classic specification",
                            "https://quarkus.io/guides/resteasy", 13, List.of("resteasy", "classic", "jaxrs")),
                    new QuarkusExtension("io.quarkus:quarkus-resteasy-jackson", "RESTeasy Classic Jackson", "Web",
                            "Jackson serialization support for RESTEasy Classic",
                            "https://quarkus.io/guides/resteasy#jackson", 14, List.of("resteasy", "jackson")),
                    new QuarkusExtension("io.quarkus:quarkus-resteasy-jsonb", "RESTeasy Classic JSON-B", "Web",
                            "JSON-B serialization support for RESTEasy Classic",
                            "https://quarkus.io/guides/resteasy#jsonb", 15, List.of("resteasy", "jsonb")),
                    new QuarkusExtension("io.quarkus:quarkus-resteasy-jaxb", "RESTeasy Classic JAXB", "Web",
                            "XML JAXB serialization support for RESTEasy Classic",
                            "https://quarkus.io/guides/resteasy#jaxb", 16, List.of("resteasy", "jaxb")),
                    new QuarkusExtension("io.quarkus:quarkus-resteasy-multipart", "RESTeasy Classic Multipart", "Web",
                            "Multipart/form-data support for RESTEasy Classic",
                            "https://quarkus.io/guides/resteasy-multipart", 17, List.of("resteasy", "multipart"))
            )),
            new QuarkusCategory("Data", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-hibernate-orm", "Hibernate ORM", "Data",
                            "Define persistent domain models using JPA and Hibernate ORM",
                            "https://quarkus.io/guides/hibernate-orm", 1, List.of("jpa", "orm", "database")),
                    new QuarkusExtension("io.quarkus:quarkus-hibernate-orm-panache", "Hibernate ORM with Panache", "Data",
                            "Simplify JPA entity and repository access with Panache active records",
                            "https://quarkus.io/guides/hibernate-orm-panache", 2, List.of("panache", "jpa", "orm")),
                    new QuarkusExtension("io.quarkus:quarkus-jdbc-postgresql", "JDBC Driver - PostgreSQL", "Data",
                            "Connect to PostgreSQL relational database using JDBC",
                            "https://quarkus.io/guides/datasource", 3, List.of("postgres", "postgresql", "sql", "jdbc")),
                    new QuarkusExtension("io.quarkus:quarkus-jdbc-mysql", "JDBC Driver - MySQL", "Data",
                            "Connect to MySQL relational database using JDBC",
                            "https://quarkus.io/guides/datasource", 4, List.of("mysql", "sql", "jdbc")),
                    new QuarkusExtension("io.quarkus:quarkus-jdbc-h2", "JDBC Driver - H2", "Data",
                            "Connect to H2 embedded relational database using JDBC",
                            "https://quarkus.io/guides/datasource", 5, List.of("h2", "embedded", "sql")),
                    new QuarkusExtension("io.quarkus:quarkus-flyway", "Flyway", "Data",
                            "Handle database migrations using Flyway",
                            "https://quarkus.io/guides/flyway", 6, List.of("flyway", "migration", "sql")),
                    new QuarkusExtension("io.quarkus:quarkus-mongodb-panache", "MongoDB with Panache", "Data",
                            "Document data modeling in MongoDB with Panache active records",
                            "https://quarkus.io/guides/mongodb-panache", 7, List.of("mongo", "mongodb", "nosql"))
            )),
            new QuarkusCategory("Messaging", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-smallrye-reactive-messaging", "SmallRye Reactive Messaging", "Messaging",
                            "Event-driven reactive streams and message channels",
                            "https://quarkus.io/guides/reactive-messaging", 1, List.of("reactive", "messaging", "events")),
                    new QuarkusExtension("io.quarkus:quarkus-smallrye-reactive-messaging-kafka", "SmallRye Reactive Messaging - Kafka Connector", "Messaging",
                            "Connect reactive messaging channels to Apache Kafka",
                            "https://quarkus.io/guides/kafka", 2, List.of("kafka", "reactive", "messaging")),
                    new QuarkusExtension("io.quarkus:quarkus-kafka-client", "Apache Kafka Client", "Messaging",
                            "Native Apache Kafka producer and consumer client",
                            "https://quarkus.io/guides/kafka", 3, List.of("kafka", "streaming", "events"))
            )),
            new QuarkusCategory("Core", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-config-yaml", "Config YAML", "Core",
                            "Use YAML formatting for application configuration files",
                            "https://quarkus.io/guides/config-reference", 1, List.of("config", "yaml")),
                    new QuarkusExtension("io.quarkus:quarkus-logging-json", "Logging JSON", "Core",
                            "Format structured application log lines as JSON",
                            "https://quarkus.io/guides/logging#json-logging", 2, List.of("logging", "json")),
                    new QuarkusExtension("io.quarkus:quarkus-mutiny", "Mutiny", "Core",
                            "Intuitive, event-driven reactive programming library",
                            "https://quarkus.io/guides/mutiny-primer", 3, List.of("reactive", "mutiny", "streams")),
                    new QuarkusExtension("io.quarkus:quarkus-scheduler", "Scheduler", "Core",
                            "Schedule periodic background tasks using cron expressions or intervals",
                            "https://quarkus.io/guides/scheduler", 4, List.of("cron", "timer", "schedule"))
            )),
            new QuarkusCategory("Reactive", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-vertx", "Eclipse Vert.x", "Reactive",
                            "Write reactive distributed applications with Eclipse Vert.x",
                            "https://quarkus.io/guides/vertx-reference", 1, List.of("vertx", "reactive", "eventloop")),
                    new QuarkusExtension("io.quarkus:quarkus-reactive-pg-client", "Reactive PostgreSQL Client", "Reactive",
                            "Non-blocking reactive driver for PostgreSQL",
                            "https://quarkus.io/guides/reactive-sql-clients", 2, List.of("postgres", "reactive", "database")),
                    new QuarkusExtension("io.quarkus:quarkus-hibernate-reactive", "Hibernate Reactive", "Reactive",
                            "Reactive object-relational mapping using Hibernate",
                            "https://quarkus.io/guides/hibernate-reactive", 3, List.of("reactive", "orm", "jpa"))
            )),
            new QuarkusCategory("Artificial Intelligence (AI)", List.of(
                    new QuarkusExtension("io.quarkiverse.langchain4j:quarkus-langchain4j-core", "LangChain4j", "Artificial Intelligence (AI)",
                            "Build LLM-powered applications and AI workflows with LangChain4j",
                            "https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html", 1, List.of("ai", "llm", "langchain")),
                    new QuarkusExtension("io.quarkiverse.langchain4j:quarkus-langchain4j-openai", "LangChain4j OpenAI", "Artificial Intelligence (AI)",
                            "Integrate OpenAI models (GPT-4o, embeddings) via LangChain4j",
                            "https://docs.quarkiverse.io/quarkus-langchain4j/dev/openai.html", 2, List.of("ai", "openai", "chatgpt")),
                    new QuarkusExtension("io.quarkiverse.langchain4j:quarkus-langchain4j-ollama", "LangChain4j Ollama", "Artificial Intelligence (AI)",
                            "Run local open-source LLMs through Ollama with LangChain4j",
                            "https://docs.quarkiverse.io/quarkus-langchain4j/dev/ollama.html", 3, List.of("ai", "ollama", "local"))
            )),
            new QuarkusCategory("Cloud", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-kubernetes", "Kubernetes", "Cloud",
                            "Generate Kubernetes manifests and deployment descriptors automatically",
                            "https://quarkus.io/guides/deploying-to-kubernetes", 1, List.of("k8s", "kubernetes", "cloud")),
                    new QuarkusExtension("io.quarkus:quarkus-kubernetes-client", "Kubernetes Client", "Cloud",
                            "Interact with the Kubernetes API server programmatically",
                            "https://quarkus.io/guides/kubernetes-client", 2, List.of("k8s", "client")),
                    new QuarkusExtension("io.quarkus:quarkus-openshift", "OpenShift", "Cloud",
                            "Generate OpenShift resources and deployment configs",
                            "https://quarkus.io/guides/deploying-to-openshift", 3, List.of("openshift", "redhat", "cloud")),
                    new QuarkusExtension("io.quarkus:quarkus-amazon-s3", "Amazon S3 Client", "Cloud",
                            "Connect to AWS Simple Storage Service (S3)",
                            "https://quarkus.io/guides/amazon-s3", 4, List.of("aws", "amazon", "s3", "cloud"))
            )),
            new QuarkusCategory("Observability", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-smallrye-openapi", "SmallRye OpenAPI (Swagger UI)", "Observability",
                            "Document APIs with OpenAPI and interactive Swagger UI",
                            "https://quarkus.io/guides/openapi-swaggerui", 1, List.of("swagger", "openapi", "api", "docs")),
                    new QuarkusExtension("io.quarkus:quarkus-micrometer", "Micrometer Metrics", "Observability",
                            "Collect dimensional application metrics for Prometheus, Datadog, etc.",
                            "https://quarkus.io/guides/micrometer", 2, List.of("metrics", "prometheus", "micrometer")),
                    new QuarkusExtension("io.quarkus:quarkus-opentelemetry", "OpenTelemetry", "Observability",
                            "Distributed tracing and telemetry export with OpenTelemetry",
                            "https://quarkus.io/guides/opentelemetry", 3, List.of("tracing", "opentelemetry", "otel")),
                    new QuarkusExtension("io.quarkus:quarkus-smallrye-health", "SmallRye Health", "Observability",
                            "Liveness and readiness health endpoints for Kubernetes probes",
                            "https://quarkus.io/guides/smallrye-health", 4, List.of("health", "liveness", "readiness"))
            )),
            new QuarkusCategory("Security", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-security", "Security", "Security",
                            "Core security architecture, identity management, and role checks",
                            "https://quarkus.io/guides/security-overview", 1, List.of("security", "auth")),
                    new QuarkusExtension("io.quarkus:quarkus-oidc", "OpenID Connect", "Security",
                            "Authenticate users with Keycloak, Auth0, Okta, and OAuth2/OIDC",
                            "https://quarkus.io/guides/security-openid-connect", 2, List.of("oidc", "oauth2", "jwt", "keycloak")),
                    new QuarkusExtension("io.quarkus:quarkus-smallrye-jwt", "SmallRye JWT", "Security",
                            "Secure microservices using MicroProfile JSON Web Tokens (JWT)",
                            "https://quarkus.io/guides/security-jwt", 3, List.of("jwt", "token", "security")),
                    new QuarkusExtension("io.quarkus:quarkus-security-jpa", "Security JPA", "Security",
                            "Store users, credentials, and roles in database tables via JPA",
                            "https://quarkus.io/guides/security-jpa", 4, List.of("security", "jpa", "database"))
            )),
            new QuarkusCategory("Serialization", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-jackson", "Jackson", "Serialization",
                            "ObjectMapper and high-performance JSON processing",
                            "https://quarkus.io/guides/rest#json-jackson", 1, List.of("jackson", "json")),
                    new QuarkusExtension("io.quarkus:quarkus-jsonb", "JSON-B", "Serialization",
                            "Standard Jakarta JSON Binding specification",
                            "https://quarkus.io/guides/rest#jsonb", 2, List.of("jsonb", "json")),
                    new QuarkusExtension("io.quarkus:quarkus-jaxb", "XML JAXB", "Serialization",
                            "Standard Jakarta XML Binding specification",
                            "https://quarkus.io/guides/rest#xml-jaxb", 3, List.of("jaxb", "xml"))
            )),
            new QuarkusCategory("Miscellaneous", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-mailer", "Mailer", "Miscellaneous",
                            "Send emails asynchronously or reactively using SMTP",
                            "https://quarkus.io/guides/mailer", 1, List.of("email", "smtp", "mail")),
                    new QuarkusExtension("io.quarkus:quarkus-qute", "Qute Templating", "Miscellaneous",
                            "Type-safe HTML and text templating engine",
                            "https://quarkus.io/guides/qute", 2, List.of("template", "qute", "html"))
            )),
            new QuarkusCategory("Compatibility", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-spring-web", "Spring Web API", "Compatibility",
                            "Use Spring Web annotations (@RestController, @GetMapping) on Quarkus",
                            "https://quarkus.io/guides/spring-web", 1, List.of("spring", "mvc", "rest")),
                    new QuarkusExtension("io.quarkus:quarkus-spring-data-jpa", "Spring Data JPA", "Compatibility",
                            "Use Spring Data repositories (CrudRepository) on Quarkus",
                            "https://quarkus.io/guides/spring-data-jpa", 2, List.of("spring", "data", "jpa")),
                    new QuarkusExtension("io.quarkus:quarkus-spring-di", "Spring DI", "Compatibility",
                            "Use Spring dependency injection annotations (@Autowired, @Service)",
                            "https://quarkus.io/guides/spring-di", 3, List.of("spring", "di", "beans")),
                    new QuarkusExtension("io.quarkus:quarkus-spring-security", "Spring Security", "Compatibility",
                            "Use Spring Security annotations (@Secured, @RolesAllowed)",
                            "https://quarkus.io/guides/spring-security", 4, List.of("spring", "security"))
            )),
            new QuarkusCategory("Packaging", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-container-image-jib", "Container Image Jib", "Packaging",
                            "Build container images using Google Jib without Docker daemon",
                            "https://quarkus.io/guides/container-image#jib", 1, List.of("docker", "jib", "container")),
                    new QuarkusExtension("io.quarkus:quarkus-container-image-docker", "Container Image Docker", "Packaging",
                            "Build container images using the local Docker daemon",
                            "https://quarkus.io/guides/container-image#docker", 2, List.of("docker", "container"))
            )),
            new QuarkusCategory("Integration", List.of(
                    new QuarkusExtension("org.apache.camel.quarkus:camel-quarkus-core", "Camel Quarkus Core", "Integration",
                            "Enterprise integration patterns with Apache Camel",
                            "https://camel.apache.org/camel-quarkus/latest/", 1, List.of("camel", "eip", "integration")),
                    new QuarkusExtension("org.apache.camel.quarkus:camel-quarkus-rest", "Camel Quarkus REST", "Integration",
                            "Expose Camel integration routes as REST endpoints",
                            "https://camel.apache.org/camel-quarkus/latest/reference/extensions/rest.html", 2, List.of("camel", "rest"))
            )),
            new QuarkusCategory("Uncategorized", List.of(
                    new QuarkusExtension("io.quarkus:quarkus-info", "Info", "Uncategorized",
                            "Expose build and git information via standard endpoints",
                            "https://quarkus.io/guides/info", 1, List.of("info", "metadata"))
            ))
    );

    public static final QuarkusCatalog FALLBACK_CATALOG = new QuarkusCatalog(
            FALLBACK_STREAMS,
            "io.quarkus.platform:3.39",
            FALLBACK_CATEGORIES
    );
}
