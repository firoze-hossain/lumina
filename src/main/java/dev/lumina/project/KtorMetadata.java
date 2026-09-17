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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client and models for start.ktor.io API:
 * - /api/plugins: fetches available Ktor plugins catalog
 * - /api/project: generates and downloads Ktor project archives
 *
 * Provides live dynamic fetching with fallback catalog matching IntelliJ IDEA's Ktor wizard.
 */
public final class KtorMetadata {

    public record KtorPlugin(
            String id,
            String name,
            String group,
            String version,
            String description,
            String category,
            String githubUrl,
            String docsUrl,
            String usageMarkdown,
            List<String> dependencies
    ) {
        public KtorPlugin {
            if (dependencies == null) dependencies = List.of();
            if (githubUrl == null) githubUrl = "";
            if (docsUrl == null) docsUrl = "";
            if (usageMarkdown == null) usageMarkdown = "";
            if (version == null) version = "3.5.2";
            if (group == null) group = "Ktor";
            if (category == null) category = "General";
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public record KtorCatalog(List<String> versions, List<KtorPlugin> plugins) {
    }

    public static final String DEFAULT_SERVER_URL = "https://start.ktor.io";
    public static final String DEFAULT_VERSION = "3.5.2";
    public static final List<String> DEFAULT_VERSIONS = List.of("3.5.2", "3.1.1", "3.0.3", "2.3.13");

    private KtorMetadata() {
    }

    public static String normalizeServerUrl(String url) {
        if (url == null || url.isBlank()) return DEFAULT_SERVER_URL;
        String clean = url.trim();
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://" + clean;
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    /**
     * Fetches plugins dynamically from start.ktor.io/api/plugins or returns fallback catalog on failure.
     */
    public static List<KtorPlugin> fetchPlugins(String serverUrl) {
        String base = normalizeServerUrl(serverUrl);
        String url = base + "/api/plugins";
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Lumina-IDE")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                List<KtorPlugin> parsed = parsePlugins(response.body());
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception ignored) {
            // Fallback used on network failure or timeout
        }
        return FALLBACK_PLUGINS;
    }

    /**
     * Parses JSON array or object containing plugin list.
     */
    public static List<KtorPlugin> parsePlugins(String json) {
        List<KtorPlugin> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        try {
            JsonElement root = JsonParser.parseString(json);
            JsonArray array = null;
            if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("plugins") && obj.get("plugins").isJsonArray()) {
                    array = obj.getAsJsonArray("plugins");
                }
            }

            if (array != null) {
                for (JsonElement el : array) {
                    if (!el.isJsonObject()) continue;
                    JsonObject p = el.getAsJsonObject();
                    String id = p.has("id") ? p.get("id").getAsString() : "";
                    String name = p.has("name") ? p.get("name").getAsString() : id;
                    String group = p.has("group") ? p.get("group").getAsString() : "Ktor";
                    String version = p.has("version") ? p.get("version").getAsString() : DEFAULT_VERSION;
                    String desc = p.has("description") ? p.get("description").getAsString() : "";
                    String category = p.has("category") ? p.get("category").getAsString() : "General";
                    String githubUrl = p.has("githubUrl") ? p.get("githubUrl").getAsString()
                            : (p.has("url") ? p.get("url").getAsString() : "");
                    String docsUrl = p.has("docsUrl") ? p.get("docsUrl").getAsString() : "";
                    String usage = p.has("usage") ? p.get("usage").getAsString() : "";

                    List<String> deps = new ArrayList<>();
                    if (p.has("dependencies") && p.get("dependencies").isJsonArray()) {
                        for (JsonElement d : p.getAsJsonArray("dependencies")) {
                            deps.add(d.getAsString());
                        }
                    }

                    if (!id.isEmpty() && !name.isEmpty()) {
                        result.add(new KtorPlugin(id, name, group, version, desc, category, githubUrl, docsUrl, usage, deps));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * Bundled fallback plugins catalog containing exact items, descriptions, and usage
     * shown in IntelliJ IDEA screenshots.
     */
    public static final List<KtorPlugin> FALLBACK_PLUGINS;

    static {
        List<KtorPlugin> list = new ArrayList<>();

        list.add(new KtorPlugin(
                "asyncapi",
                "AsyncAPI",
                "AsyncAPI",
                "1.0.0",
                "Generates and serves AsyncAPI documentation",
                "Documentation",
                "https://github.com/asyncapi/kotlin-asyncapi",
                "https://github.com/asyncapi/kotlin-asyncapi",
                """
                The `AsyncAPI` plugin allows you to generate and serve AsyncAPI documentation for your Ktor application.

                ### Usage
                To serve your AsyncAPI specification via Ktor:
                - install the `AsyncApiPlugin` in your application
                - document your API with `AsyncApiExtension` and/or Kotlin scripting (see Kotlin script usage)
                - add annotations to auto-generate components (see annotation usage)

                You can register multiple extensions to extend and override AsyncAPI components. Extensions with a higher order override extensions with a lower order. Please note that you can only extend top-level components for now (`info`, `channels`, `servers`...). Subcomponents will always be overwritten.

                Example (simplified version of Gitter example)
                ```kotlin
                fun main() {
                    embeddedServer(Netty, port = 8080) {
                        install(AsyncApiPlugin) {
                            // specification configuration
                        }
                    }.start(wait = true)
                }
                ```
                """,
                List.of("org.openfolder:kotlin-asyncapi-ktor:1.0.0")
        ));

        list.add(new KtorPlugin(
                "cors",
                "CORS",
                "Ktor",
                "3.5.2",
                "Enables Cross-Origin Resource Sharing (CORS)",
                "Security",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-cors.html",
                """
                The `CORS` plugin allows you to configure Cross-Origin Resource Sharing (CORS) in your Ktor application.

                ### Usage
                ```kotlin
                install(CORS) {
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Put)
                    allowMethod(HttpMethod.Delete)
                    allowMethod(HttpMethod.Patch)
                    allowHeader(HttpHeaders.Authorization)
                    anyHost() // @TODO: Don't do this in production if security matters!
                }
                ```
                """,
                List.of("io.ktor:ktor-server-cors")
        ));

        list.add(new KtorPlugin(
                "caching-headers",
                "Caching Headers",
                "Ktor",
                "3.5.2",
                "Provides options for responding with standard cache headers",
                "HTTP",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-caching-headers.html",
                """
                The `CachingHeaders` plugin adds the capability to configure the `Cache-Control` and `Expires` headers automatically for responses.

                ### Usage
                ```kotlin
                install(CachingHeaders) {
                    options { call, outgoingContent ->
                        when (outgoingContent.contentType?.withoutParameters()) {
                            ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600))
                            else -> null
                        }
                    }
                }
                ```
                """,
                List.of("io.ktor:ktor-server-caching-headers")
        ));

        list.add(new KtorPlugin(
                "compression",
                "Compression",
                "Ktor",
                "3.5.2",
                "Compresses responses using encoding algorithms",
                "HTTP",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-compression.html",
                """
                The `Compression` plugin allows you to compress outgoing responses using `gzip`, `deflate`, or custom encoders.

                ### Usage
                ```kotlin
                install(Compression) {
                    gzip {
                        priority = 1.0
                    }
                    deflate {
                        priority = 10.0
                        minimumSize(1024)
                    }
                }
                ```
                """,
                List.of("io.ktor:ktor-server-compression")
        ));

        list.add(new KtorPlugin(
                "conditional-headers",
                "Conditional Headers",
                "Ktor",
                "3.5.2",
                "Skips response body, depending on ETag and Last-Modified",
                "HTTP",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-conditional-headers.html",
                """
                The `ConditionalHeaders` plugin avoids sending the entire response body when the client's cached content is still fresh.

                ### Usage
                ```kotlin
                install(ConditionalHeaders)
                ```
                """,
                List.of("io.ktor:ktor-server-conditional-headers")
        ));

        list.add(new KtorPlugin(
                "default-headers",
                "Default Headers",
                "Ktor",
                "3.5.2",
                "Adds a default set of headers to HTTP responses",
                "HTTP",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-default-headers.html",
                """
                The `DefaultHeaders` plugin adds standard server headers (e.g. `Date`, `Server`) to every response.

                ### Usage
                ```kotlin
                install(DefaultHeaders) {
                    header("X-Engine", "Ktor")
                }
                ```
                """,
                List.of("io.ktor:ktor-server-default-headers")
        ));

        list.add(new KtorPlugin(
                "forwarded-headers",
                "Forwarded Headers",
                "Ktor",
                "3.5.2",
                "Allows handling proxied headers (X-Forwarded-*)",
                "HTTP",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-forward-headers.html",
                """
                The `ForwardedHeaders` plugin allows Ktor to handle `X-Forwarded-For`, `X-Forwarded-Proto`, and `Forwarded` headers from reverse proxies.

                ### Usage
                ```kotlin
                install(ForwardedHeaders)
                ```
                """,
                List.of("io.ktor:ktor-server-forwarded-headers")
        ));

        list.add(new KtorPlugin(
                "hsts",
                "HSTS",
                "Ktor",
                "3.5.2",
                "Adds HTTP Strict Transport Security (HSTS) headers",
                "Security",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-hsts.html",
                """
                The `HSTS` plugin enforces HTTPS connections between clients and your server.

                ### Usage
                ```kotlin
                install(HSTS) {
                    maxAgeInSeconds = 31536000
                    includeSubDomains = true
                }
                ```
                """,
                List.of("io.ktor:ktor-server-hsts")
        ));

        list.add(new KtorPlugin(
                "routing",
                "Routing",
                "Ktor",
                "3.5.2",
                "Defines structured routes and endpoints for handling incoming HTTP requests",
                "Routing",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-routing.html",
                """
                Routing is the core plugin for defining HTTP endpoints in Ktor.

                ### Usage
                ```kotlin
                routing {
                    get("/") {
                        call.respondText("Hello, world!")
                    }
                }
                ```
                """,
                List.of("io.ktor:ktor-server-core")
        ));

        list.add(new KtorPlugin(
                "content-negotiation",
                "Content Negotiation",
                "Ktor",
                "3.5.2",
                "Provides automatic content conversion between HTTP body and Kotlin objects",
                "Serialization",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-content-negotiation.html",
                """
                The `ContentNegotiation` plugin allows automatic serialization and deserialization of request/response bodies.

                ### Usage
                ```kotlin
                install(ContentNegotiation) {
                    json()
                }
                ```
                """,
                List.of("io.ktor:ktor-server-content-negotiation", "io.ktor:ktor-serialization-kotlinx-json")
        ));

        list.add(new KtorPlugin(
                "kotlinx-serialization",
                "kotlinx.serialization",
                "Kotlin",
                "3.5.2",
                "Official Kotlin multiplatform JSON and binary serialization support",
                "Serialization",
                "https://github.com/Kotlin/kotlinx.serialization",
                "https://ktor.io/docs/server-serialization.html",
                """
                Kotlinx Serialization converter for Ktor ContentNegotiation.

                ### Usage
                ```kotlin
                install(ContentNegotiation) {
                    json()
                }
                ```
                """,
                List.of("io.ktor:ktor-serialization-kotlinx-json")
        ));

        list.add(new KtorPlugin(
                "jackson",
                "Jackson",
                "FasterXML",
                "3.5.2",
                "Provides Jackson JSON serialization converter for Content Negotiation",
                "Serialization",
                "https://github.com/FasterXML/jackson",
                "https://ktor.io/docs/server-serialization.html",
                """
                Jackson serializer for Ktor ContentNegotiation.
                """,
                List.of("io.ktor:ktor-serialization-jackson")
        ));

        list.add(new KtorPlugin(
                "status-pages",
                "Status Pages",
                "Ktor",
                "3.5.2",
                "Handles exceptions and status codes thrown in route handlers",
                "Routing",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-status-pages.html",
                """
                The `StatusPages` plugin allows handling exceptions centrally and generating custom error responses.

                ### Usage
                ```kotlin
                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
                    }
                }
                ```
                """,
                List.of("io.ktor:ktor-server-status-pages")
        ));

        list.add(new KtorPlugin(
                "call-logging",
                "Call Logging",
                "Ktor",
                "3.5.2",
                "Logs incoming client requests and HTTP response statuses",
                "Administration",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-call-logging.html",
                """
                The `CallLogging` plugin logs HTTP calls using SLF4J.

                ### Usage
                ```kotlin
                install(CallLogging) {
                    level = Level.INFO
                }
                ```
                """,
                List.of("io.ktor:ktor-server-call-logging")
        ));

        list.add(new KtorPlugin(
                "call-id",
                "Call ID",
                "Ktor",
                "3.5.2",
                "Assigns or extracts unique identifiers for tracing HTTP requests",
                "Administration",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-call-id.html",
                """
                The `CallId` plugin generates unique IDs for requests or retrieves them from headers.
                """,
                List.of("io.ktor:ktor-server-call-id")
        ));

        list.add(new KtorPlugin(
                "auth",
                "Authentication",
                "Ktor",
                "3.5.2",
                "Provides authentication and authorization framework for Ktor routes",
                "Security",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-auth.html",
                """
                The `Authentication` plugin handles user authentication via basic, form, digest, JWT, or OAuth schemes.
                """,
                List.of("io.ktor:ktor-server-auth")
        ));

        list.add(new KtorPlugin(
                "auth-jwt",
                "Authentication JWT",
                "Ktor",
                "3.5.2",
                "Validates JSON Web Tokens (JWT) for secure authenticated routes",
                "Security",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-jwt.html",
                """
                JWT provider for Ktor Authentication plugin.
                """,
                List.of("io.ktor:ktor-server-auth-jwt")
        ));

        list.add(new KtorPlugin(
                "websockets",
                "WebSockets",
                "Ktor",
                "3.5.2",
                "Enables full-duplex bi-directional communication over WebSocket connections",
                "Sockets",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-websockets.html",
                """
                The `WebSockets` plugin adds WebSocket protocol support.

                ### Usage
                ```kotlin
                install(WebSockets)
                routing {
                    webSocket("/chat") {
                        for (frame in incoming) {
                            // handle frame
                        }
                    }
                }
                ```
                """,
                List.of("io.ktor:ktor-server-websockets")
        ));

        list.add(new KtorPlugin(
                "sse",
                "SSE (Server-Sent Events)",
                "Ktor",
                "3.5.2",
                "Streams unidirectional push events from server to clients",
                "Sockets",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-server-sent-events.html",
                """
                The `SSE` plugin enables Server-Sent Events support.
                """,
                List.of("io.ktor:ktor-server-sse")
        ));

        list.add(new KtorPlugin(
                "swagger",
                "Swagger UI",
                "Ktor",
                "3.5.2",
                "Renders interactive Swagger UI documentation for OpenAPI endpoints",
                "Documentation",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-swagger-openapi.html",
                """
                Serves Swagger UI from your Ktor application.

                ### Usage
                ```kotlin
                routing {
                    swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
                }
                ```
                """,
                List.of("io.ktor:ktor-server-swagger")
        ));

        list.add(new KtorPlugin(
                "openapi",
                "OpenAPI",
                "Ktor",
                "3.5.2",
                "Exposes OpenAPI specifications and documentation endpoints",
                "Documentation",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-swagger-openapi.html",
                """
                Serves OpenAPI specification files.
                """,
                List.of("io.ktor:ktor-server-openapi")
        ));

        list.add(new KtorPlugin(
                "html-dsl",
                "HTML DSL",
                "Kotlin",
                "3.5.2",
                "Builds type-safe HTML pages directly using Kotlin DSL",
                "Templating",
                "https://github.com/Kotlin/kotlinx.html",
                "https://ktor.io/docs/server-html-dsl.html",
                """
                Kotlinx HTML templating engine.
                """,
                List.of("io.ktor:ktor-server-html-builder")
        ));

        list.add(new KtorPlugin(
                "metrics-micrometer",
                "Micrometer Metrics",
                "Micrometer",
                "3.5.2",
                "Collects and exposes application metrics to Prometheus or other monitoring systems",
                "Administration",
                "https://github.com/ktorio/ktor",
                "https://ktor.io/docs/server-micrometer-metrics.html",
                """
                The `MicrometerMetrics` plugin exposes metrics using the Micrometer library.
                """,
                List.of("io.ktor:ktor-server-metrics-micrometer")
        ));

        FALLBACK_PLUGINS = Collections.unmodifiableList(list);
    }

    public static KtorPlugin findPlugin(String id) {
        if (id == null) return null;
        for (KtorPlugin p : FALLBACK_PLUGINS) {
            if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }
}
