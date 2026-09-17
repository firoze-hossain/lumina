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
 * Client and models for launch.micronaut.io REST API:
 * - /versions: fetches current Micronaut framework version
 * - /application-types/{type}/features: fetches features catalog organized by category
 *
 * Provides live fetching with timeout and robust offline fallback catalog matching
 * IntelliJ IDEA's Micronaut wizard.
 */
public final class MicronautMetadata {

    public record MicronautFeature(
            String name,
            String title,
            String category,
            String description,
            boolean preview,
            boolean community
    ) {
        @Override
        public String toString() {
            return title;
        }
    }

    public record MicronautCategory(String name, List<MicronautFeature> features) {
    }

    public record MicronautCatalog(String version, List<MicronautCategory> categories) {
    }

    public static final String DEFAULT_SERVER_URL = "https://launch.micronaut.io";
    public static final String DEFAULT_VERSION = "5.1.5";

    private MicronautMetadata() {
    }

    /**
     * Normalizes a server URL string (e.g. "launch.micronaut.io" -> "https://launch.micronaut.io").
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
     * Fetches version and features catalog from the given Micronaut Launch server.
     */
    public static MicronautCatalog fetchCatalog(String serverUrl, String appType) throws IOException {
        String base = normalizeServerUrl(serverUrl);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        String type = (appType == null || appType.isBlank()) ? "default" : appType.toLowerCase();
        String version = fetchVersion(client, base);
        List<MicronautCategory> categories = fetchFeatures(client, base, type);

        return new MicronautCatalog(version, categories);
    }

    public static String fetchVersion(HttpClient client, String baseUrl) throws IOException {
        URI uri = URI.create(baseUrl + "/versions");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                return parseVersion(response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Micronaut version request interrupted", e);
        } catch (Exception ignored) {
        }
        return DEFAULT_VERSION;
    }

    public static List<MicronautCategory> fetchFeatures(HttpClient client, String baseUrl, String appType)
            throws IOException {
        String endpoint = baseUrl + "/application-types/" + appType + "/features";
        URI uri = URI.create(endpoint);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                throw new IOException("HTTP " + response.statusCode() + " fetching features from " + uri);
            }
            return parseFeatures(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Micronaut features request interrupted", e);
        }
    }

    /**
     * Parses the version from /versions JSON response.
     */
    public static String parseVersion(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("micronaut.version")) {
                    return obj.get("micronaut.version").getAsString();
                }
                if (obj.has("micronaut") && obj.get("micronaut").isJsonObject()) {
                    JsonObject mObj = obj.getAsJsonObject("micronaut");
                    if (mObj.has("version")) {
                        return mObj.get("version").getAsString();
                    }
                }
                if (obj.has("version")) {
                    return obj.get("version").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_VERSION;
    }

    /**
     * Pure parser for /application-types/{type}/features JSON response.
     */
    public static List<MicronautCategory> parseFeatures(String json) throws IOException {
        Map<String, List<MicronautFeature>> categorized = new LinkedHashMap<>();
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) {
                throw new IOException("Expected JSON object with 'features' key");
            }
            JsonObject root = el.getAsJsonObject();
            if (!root.has("features") || !root.get("features").isJsonArray()) {
                throw new IOException("Missing 'features' array in response");
            }

            JsonArray featuresArray = root.getAsJsonArray("features");
            for (JsonElement item : featuresArray) {
                if (!item.isJsonObject()) continue;
                JsonObject obj = item.getAsJsonObject();
                String name = obj.has("name") ? obj.get("name").getAsString() : "";
                String title = obj.has("title") ? obj.get("title").getAsString() : name;
                String category = obj.has("category") && !obj.get("category").isJsonNull()
                        ? obj.get("category").getAsString() : "Uncategorized";
                String description = obj.has("description") && !obj.get("description").isJsonNull()
                        ? obj.get("description").getAsString() : "";
                boolean preview = obj.has("preview") && obj.get("preview").getAsBoolean();
                boolean community = obj.has("community") && obj.get("community").getAsBoolean();

                if (!name.isBlank()) {
                    categorized.computeIfAbsent(category, k -> new ArrayList<>())
                            .add(new MicronautFeature(name, title, category, description, preview, community));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse Micronaut features: " + e.getMessage(), e);
        }

        if (categorized.isEmpty()) {
            throw new IOException("No features found in response");
        }

        List<MicronautCategory> result = new ArrayList<>();
        for (Map.Entry<String, List<MicronautFeature>> entry : categorized.entrySet()) {
            result.add(new MicronautCategory(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    // =========================================================================
    // Bundled Offline Fallback Catalog (Matching IntelliJ IDEA Screenshots)
    // =========================================================================

    public static final List<MicronautCategory> FALLBACK_CATEGORIES = List.of(
            new MicronautCategory("Server", List.of(
                    new MicronautFeature("http-poja", "Plain Old Java HTTP Application", "Server",
                            "Add support for HTTP POJA based on Apache libraries", false, false),
                    new MicronautFeature("http-server-builtin", "Built-In Java HTTP Server Runtime", "Server",
                            "Adds support for the built-in Java HTTP server runtime (com.sun.net.httpserver)", false, false),
                    new MicronautFeature("jax-rs", "JAX-RS support", "Server",
                            "Adds support for JAX-RS annotations", false, false),
                    new MicronautFeature("jetty-server", "Jetty Server", "Server",
                            "Adds support for a Jetty server", false, false),
                    new MicronautFeature("ktor", "Ktor", "Server",
                            "Adds support for Ktor server routing and runtime", false, false),
                    new MicronautFeature("netty-server", "Netty Server", "Server",
                            "Adds support for a Netty server", false, false),
                    new MicronautFeature("tomcat-server", "Tomcat Server", "Server",
                            "Adds support for an Apache Tomcat server", false, false),
                    new MicronautFeature("undertow-server", "Undertow Server", "Server",
                            "Adds support for an Undertow server", false, false),
                    new MicronautFeature("websocket", "Websocket", "Server",
                            "Adds support for creating WebSocket clients and servers.", false, false)
            )),
            new MicronautCategory("Client", List.of(
                    new MicronautFeature("http-client", "HTTP Client", "Client",
                            "Adds support for the Micronaut HTTP client", false, false),
                    new MicronautFeature("http-client-jdk", "Java HTTP Client", "Client",
                            "Adds support for the standard java.net.http HTTP client", false, false)
            )),
            new MicronautCategory("Database", List.of(
                    new MicronautFeature("data-jpa", "Micronaut Data JPA", "Database",
                            "Adds support for Micronaut Data JPA with Hibernate", false, false),
                    new MicronautFeature("data-jdbc", "Micronaut Data JDBC", "Database",
                            "Adds support for Micronaut Data JDBC repositories", false, false),
                    new MicronautFeature("data-r2dbc", "Micronaut Data R2DBC", "Database",
                            "Adds support for reactive database access with R2DBC", false, false),
                    new MicronautFeature("data-mongodb", "Micronaut Data MongoDB", "Database",
                            "Adds support for Micronaut Data MongoDB repositories", false, false),
                    new MicronautFeature("flyway", "Flyway Database Migration", "Database",
                            "Adds support for Flyway database migrations", false, false),
                    new MicronautFeature("liquibase", "Liquibase Database Migration", "Database",
                            "Adds support for Liquibase database migrations", false, false),
                    new MicronautFeature("h2", "H2 Database Driver", "Database",
                            "Adds the H2 database driver", false, false),
                    new MicronautFeature("postgres", "PostgreSQL Driver", "Database",
                            "Adds the PostgreSQL driver", false, false),
                    new MicronautFeature("mysql", "MySQL Driver", "Database",
                            "Adds the MySQL driver", false, false),
                    new MicronautFeature("mariadb", "MariaDB Driver", "Database",
                            "Adds the MariaDB driver", false, false),
                    new MicronautFeature("oracle", "Oracle Driver", "Database",
                            "Adds the Oracle database driver", false, false),
                    new MicronautFeature("sqlserver", "Microsoft SQL Server Driver", "Database",
                            "Adds the Microsoft SQL Server driver", false, false)
            )),
            new MicronautCategory("Validation", List.of(
                    new MicronautFeature("validation", "Micronaut Validation", "Validation",
                            "Adds support for Micronaut Validation", false, false),
                    new MicronautFeature("hibernate-validator", "Hibernate Validator", "Validation",
                            "Adds support for Hibernate Validator", false, false)
            )),
            new MicronautCategory("Logging", List.of(
                    new MicronautFeature("logback", "Logback Logging", "Logging",
                            "Adds Logback logging framework", false, false),
                    new MicronautFeature("log4j2", "Log4j 2 Logging", "Logging",
                            "Adds Log4j 2 logging framework", false, false),
                    new MicronautFeature("amazon-cloudwatch-logging", "Amazon CloudWatch Logging", "Logging",
                            "Provides integration with Amazon CloudWatch Logs", false, false)
            )),
            new MicronautCategory("Security", List.of(
                    new MicronautFeature("security", "Micronaut Security", "Security",
                            "Adds security and authentication support", false, false),
                    new MicronautFeature("security-jwt", "Security JWT", "Security",
                            "Adds support for JSON Web Token (JWT) authentication", false, false),
                    new MicronautFeature("security-oauth2", "Security OAuth 2.0", "Security",
                            "Adds support for OAuth 2.0 and OpenID Connect authentication", false, false),
                    new MicronautFeature("amazon-cognito", "Amazon Cognito", "Security",
                            "Applies Micronaut Security OAuth 2.0 and configuration for AWS Cognito", false, false)
            )),
            new MicronautCategory("Management", List.of(
                    new MicronautFeature("management", "Micronaut Management", "Management",
                            "Adds management and monitoring endpoints (health, info, metrics, etc.)", false, false),
                    new MicronautFeature("micrometer", "Micrometer Metrics", "Management",
                            "Adds support for Micrometer metrics collection", false, false)
            )),
            new MicronautCategory("Messaging", List.of(
                    new MicronautFeature("kafka", "Apache Kafka", "Messaging",
                            "Adds support for Apache Kafka messaging", false, false),
                    new MicronautFeature("kafka-streams", "Kafka Streams", "Messaging",
                            "Adds support for Kafka Streams", false, false),
                    new MicronautFeature("rabbitmq", "RabbitMQ", "Messaging",
                            "Adds support for RabbitMQ message broker", false, false),
                    new MicronautFeature("jms", "JMS Support", "Messaging",
                            "Adds support for Java Message Service (JMS)", false, false),
                    new MicronautFeature("mqtt", "MQTT Client", "Messaging",
                            "Adds support for MQTT messaging protocol", false, false),
                    new MicronautFeature("nats", "NATS Messaging", "Messaging",
                            "Adds support for NATS messaging system", false, false),
                    new MicronautFeature("pulsar", "Apache Pulsar", "Messaging",
                            "Adds support for Apache Pulsar messaging", false, false)
            )),
            new MicronautCategory("Reactive", List.of(
                    new MicronautFeature("reactor", "Project Reactor", "Reactive",
                            "Adds Project Reactor reactive library support", false, false),
                    new MicronautFeature("rxjava3", "RxJava 3", "Reactive",
                            "Adds RxJava 3 reactive streams support", false, false)
            )),
            new MicronautCategory("View Rendering", List.of(
                    new MicronautFeature("views-thymeleaf", "Thymeleaf Views", "View Rendering",
                            "Adds support for Server-Side View Rendering using Thymeleaf", false, false),
                    new MicronautFeature("views-freemarker", "Freemarker Views", "View Rendering",
                            "Adds support for Server-Side View Rendering using Apache Freemarker", false, false),
                    new MicronautFeature("views-handlebars", "Handlebars Views", "View Rendering",
                            "Adds support for Server-Side View Rendering using Handlebars", false, false),
                    new MicronautFeature("views-jte", "JTE Views", "View Rendering",
                            "Adds support for Server-Side View Rendering using JTE", false, false),
                    new MicronautFeature("views-velocity", "Velocity Views", "View Rendering",
                            "Adds support for Server-Side View Rendering using Apache Velocity", false, false),
                    new MicronautFeature("views-react", "React SSR", "View Rendering",
                            "Adds support for Server-Side View Rendering of ReactJS components", true, false),
                    new MicronautFeature("turbo", "Turbo", "View Rendering",
                            "Adds support for Hotwire Turbo", false, false)
            )),
            new MicronautCategory("SSL", List.of(
                    new MicronautFeature("acme", "ACME", "SSL",
                            "Adds support for ACME (Automated Certificate Management Environment)", false, false)
            )),
            new MicronautCategory("Serverless", List.of(
                    new MicronautFeature("aws-lambda", "AWS Lambda Function", "Serverless",
                            "Support for deploying Micronaut applications to AWS Lambda", false, false),
                    new MicronautFeature("amazon-api-gateway", "Amazon API Gateway REST API", "Serverless",
                            "Combines with CDK to define an API Gateway REST API", false, false),
                    new MicronautFeature("azure-function", "Azure Functions", "Serverless",
                            "Support for deploying Micronaut applications to Azure Functions", false, false),
                    new MicronautFeature("gcp-function", "Google Cloud Functions", "Serverless",
                            "Support for deploying Micronaut applications to Google Cloud Functions", false, false)
            )),
            new MicronautCategory("API", List.of(
                    new MicronautFeature("openapi", "OpenAPI (Swagger)", "API",
                            "Adds support for OpenAPI specification generation and Swagger UI", false, false),
                    new MicronautFeature("graphql", "GraphQL", "API",
                            "Adds support for GraphQL schema and query execution", false, false),
                    new MicronautFeature("grpc", "gRPC", "API",
                            "Adds support for gRPC client and server services", false, false),
                    new MicronautFeature("annotation-api", "Jakarta Annotations API", "API",
                            "Adds Jakarta annotations API dependency (@PostConstruct, @PreDestroy)", false, false)
            )),
            new MicronautCategory("Cloud", List.of(
                    new MicronautFeature("aws-sdk-v2", "AWS SDK v2", "Cloud",
                            "Provides integration with the AWS SDK v2", false, false),
                    new MicronautFeature("gcp-sdk", "Google Cloud Platform", "Cloud",
                            "Provides integration with Google Cloud Platform services", false, false),
                    new MicronautFeature("azure-sdk", "Microsoft Azure", "Cloud",
                            "Provides integration with Microsoft Azure services", false, false),
                    new MicronautFeature("oracle-cloud-sdk", "Oracle Cloud Infrastructure", "Cloud",
                            "Provides integration with Oracle Cloud Infrastructure", false, false),
                    new MicronautFeature("arm", "ARM CPU Architecture", "Cloud",
                            "Generate infrastructure optimized for ARM CPU architecture", false, false),
                    new MicronautFeature("x86", "x86 CPU Architecture", "Cloud",
                            "Generate infrastructure for x86 CPU architecture", false, false)
            )),
            new MicronautCategory("Documentation", List.of(
                    new MicronautFeature("asciidoctor", "Asciidoctor Documentation", "Documentation",
                            "Adds support for creating Asciidoctor documentation", false, false)
            )),
            new MicronautCategory("Development Tools", List.of(
                    new MicronautFeature("assertj", "AssertJ Framework", "Development Tools",
                            "AssertJ fluent assertions framework", false, false),
                    new MicronautFeature("graalvm", "GraalVM Native Image", "Development Tools",
                            "Allows compiling the application into a GraalVM native executable", false, false),
                    new MicronautFeature("testcontainers", "Testcontainers", "Development Tools",
                            "Adds support for Testcontainers for integration testing with real databases and services", false, false),
                    new MicronautFeature("micronaut-aot", "Micronaut AOT", "Development Tools",
                            "Enables Ahead-Of-Time optimization for faster startup and lower memory footprint", false, false)
            )),
            new MicronautCategory("Internet of Things", List.of(
                    new MicronautFeature("coap", "CoAP Protocol", "Internet of Things",
                            "Adds support for the Constrained Application Protocol (CoAP)", false, false)
            )),
            new MicronautCategory("CI/CD", List.of(
                    new MicronautFeature("github-workflow", "GitHub Actions Workflow", "CI/CD",
                            "Adds GitHub Actions CI workflow to build and test the project", false, false),
                    new MicronautFeature("gitlab-pipeline", "GitLab CI/CD", "CI/CD",
                            "Adds GitLab CI pipeline configuration", false, false)
            )),
            new MicronautCategory("Distributed Configuration", List.of(
                    new MicronautFeature("config-consul", "Consul Distributed Configuration", "Distributed Configuration",
                            "Adds support for distributed configuration via HashiCorp Consul", false, false),
                    new MicronautFeature("config-aws-secrets-manager", "AWS Secrets Manager", "Distributed Configuration",
                            "Loads application configuration from AWS Secrets Manager", false, false)
            )),
            new MicronautCategory("Distributed Tracing", List.of(
                    new MicronautFeature("tracing-opentelemetry-otlp", "OpenTelemetry Exporter OTLP", "Distributed Tracing",
                            "Adds the OpenTelemetry exporter dependency for OTLP", false, false),
                    new MicronautFeature("tracing-zipkin", "Zipkin Tracing", "Distributed Tracing",
                            "Adds support for distributed tracing with Zipkin", false, false),
                    new MicronautFeature("tracing-jaeger", "Jaeger Tracing", "Distributed Tracing",
                            "Adds support for distributed tracing with Jaeger", false, false)
            )),
            new MicronautCategory("Cache", List.of(
                    new MicronautFeature("cache-caffeine", "Caffeine Cache", "Cache",
                            "Adds support for Caffeine in-memory cache", false, false),
                    new MicronautFeature("redis-lettuce", "Redis (Lettuce)", "Cache",
                            "Adds support for Redis cache using the Lettuce driver", false, false),
                    new MicronautFeature("cache-hazelcast", "Hazelcast Cache", "Cache",
                            "Adds support for Hazelcast distributed cache", false, false)
            )),
            new MicronautCategory("ChatBots", List.of(
                    new MicronautFeature("chatbots-telegram", "Telegram Bot Support", "ChatBots",
                            "Provides integration for building Telegram bots with Micronaut", false, false)
            )),
            new MicronautCategory("Configuration", List.of(
                    new MicronautFeature("yaml", "Yaml Configuration", "Configuration",
                            "Adds support for using YAML for configuration", false, false),
                    new MicronautFeature("toml", "TOML Configuration", "Configuration",
                            "Adds support for using TOML for configuration", false, false)
            )),
            new MicronautCategory("Packaging", List.of(
                    new MicronautFeature("jib", "Jib Container Image", "Packaging",
                            "Builds optimized Docker and OCI images without Docker daemon", false, false),
                    new MicronautFeature("docker", "Docker Support", "Packaging",
                            "Adds Dockerfile and container build configuration", false, false)
            )),
            new MicronautCategory("Service Discovery", List.of(
                    new MicronautFeature("discovery-consul", "Consul Service Discovery", "Service Discovery",
                            "Enables registration and discovery using HashiCorp Consul", false, false),
                    new MicronautFeature("discovery-eureka", "Eureka Service Discovery", "Service Discovery",
                            "Enables registration and discovery using Netflix Eureka", false, false)
            )),
            new MicronautCategory("Search Engine", List.of(
                    new MicronautFeature("elasticsearch", "Elasticsearch", "Search Engine",
                            "Adds support for Elasticsearch client", false, false),
                    new MicronautFeature("opensearch", "OpenSearch", "Search Engine",
                            "Adds support for OpenSearch client", false, false)
            )),
            new MicronautCategory("Email", List.of(
                    new MicronautFeature("email-javamail", "JavaMail Email Support", "Email",
                            "Adds support for sending emails via standard JavaMail", false, false),
                    new MicronautFeature("email-sendgrid", "SendGrid Email Support", "Email",
                            "Adds support for sending emails using SendGrid API", false, false)
            )),
            new MicronautCategory("Languages", List.of(
                    new MicronautFeature("graal-languages", "GraalVM Polyglot Languages", "Languages",
                            "Adds support for GraalVM Polyglot multi-language features", false, false)
            )),
            new MicronautCategory("Groovy Optional Modules", List.of(
                    new MicronautFeature("groovy-templates", "Groovy Template Engine", "Groovy Optional Modules",
                            "Adds Groovy template engine support", false, false)
            )),
            new MicronautCategory("Dependency Injection", List.of(
                    new MicronautFeature("inject-java", "Micronaut Java Annotation Processor", "Dependency Injection",
                            "Core compile-time dependency injection and AOP engine", false, false),
                    new MicronautFeature("guice", "Google Guice Compatibility", "Dependency Injection",
                            "Adds compatibility layer for Google Guice modules", false, false)
            )),
            new MicronautCategory("Testing", List.of(
                    new MicronautFeature("junit", "JUnit 5 Test Framework", "Testing",
                            "Adds JUnit 5 test framework and assertions", false, false),
                    new MicronautFeature("kotest", "Kotest Framework", "Testing",
                            "Adds Kotest testing framework for Kotlin", false, false),
                    new MicronautFeature("spock", "Spock Framework", "Testing",
                            "Adds Spock testing framework for Groovy/Java", false, false),
                    new MicronautFeature("mockito", "Mockito Mocking", "Testing",
                            "Adds Mockito library for unit test mocks", false, false),
                    new MicronautFeature("rest-assured", "REST Assured", "Testing",
                            "Testing and validating REST services in Java", false, false)
            )),
            new MicronautCategory("AI", List.of(
                    new MicronautFeature("langchain4j", "LangChain4j Integration", "AI",
                            "Supercharge your Micronaut application with LLMs and AI capabilities", false, false)
            )),
            new MicronautCategory("Language Models", List.of(
                    new MicronautFeature("langchain4j-openai", "OpenAI Integration", "Language Models",
                            "Adds LangChain4j integration with OpenAI models", false, false),
                    new MicronautFeature("langchain4j-gemini", "Google Gemini Integration", "Language Models",
                            "Adds LangChain4j integration with Google Gemini models", false, false),
                    new MicronautFeature("langchain4j-ollama", "Ollama Local Models", "Language Models",
                            "Adds LangChain4j integration with Ollama for running models locally", false, false)
            )),
            new MicronautCategory("Embedded Store", List.of(
                    new MicronautFeature("eclipsestore", "EclipseStore", "Embedded Store",
                            "Superfast in-memory object graph persistence", false, false)
            )),
            new MicronautCategory("MCP", List.of(
                    new MicronautFeature("mcp-server", "Model Context Protocol Server", "MCP",
                            "Provides integration with Model Context Protocol (MCP) server specification", false, false)
            )),
            new MicronautCategory("Metrics", List.of(
                    new MicronautFeature("micrometer-prometheus", "Prometheus Metrics", "Metrics",
                            "Exposes Micrometer metrics in Prometheus format", false, false),
                    new MicronautFeature("micrometer-datadog", "Datadog Metrics", "Metrics",
                            "Exports metrics to Datadog", false, false)
            )),
            new MicronautCategory("Resilience", List.of(
                    new MicronautFeature("resilience4j", "Resilience4j", "Resilience",
                            "Adds fault tolerance patterns: CircuitBreaker, RateLimiter, Retry, Bulkhead", false, false)
            )),
            new MicronautCategory("Spring Framework", List.of(
                    new MicronautFeature("spring", "Spring Boot Annotation Compatibility", "Spring Framework",
                            "Allows running Spring-annotated beans and controllers on Micronaut", false, false)
            ))
    );

    public static final MicronautCatalog FALLBACK_CATALOG = new MicronautCatalog(DEFAULT_VERSION, FALLBACK_CATEGORIES);
}
