package dev.lumina.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metadata, dependency catalog, and dynamic version matrix for the Jakarta EE project wizard.
 * Provides live querying against Maven Central for the freshest release versions while
 * guaranteeing rock-solid offline fallback defaults for Jakarta EE 11, 10, 9.1, and 8.
 */
public final class JakartaMetadata {

    public record JakartaDep(
            String id,
            String name,
            String category,
            String description,
            String website,
            String specUrl,
            String groupId,
            String artifactId,
            String scope
    ) {
        public String label(String version) {
            if (version == null || version.isBlank()) {
                return name;
            }
            return name + " (" + version + ")";
        }
    }

    public record JakartaCategory(
            String name,
            String description,
            List<JakartaDep> dependencies
    ) {
    }

    public static final String EE_11 = "Jakarta EE 11";
    public static final String EE_10 = "Jakarta EE 10";
    public static final String EE_9_1 = "Jakarta EE 9.1";
    public static final String EE_8 = "Jakarta EE 8";

    public static final List<String> SUPPORTED_VERSIONS = List.of(EE_11, EE_10, EE_9_1, EE_8);

    public static final String TEMPLATE_REST = "REST service";
    public static final String TEMPLATE_WEB = "Web application";
    public static final String TEMPLATE_LIBRARY = "Library";

    public static final List<String> SUPPORTED_TEMPLATES = List.of(
            TEMPLATE_REST,
            TEMPLATE_WEB,
            TEMPLATE_LIBRARY
    );

    private static final Map<String, Map<String, String>> VERSION_MATRIX = new ConcurrentHashMap<>();
    private static final Map<String, JakartaDep> DEPS_BY_ID = new LinkedHashMap<>();
    private static final List<JakartaCategory> CATEGORIES = new ArrayList<>();

    // Dynamic cache for latest Maven Central versions queried at runtime
    private static final Map<String, String> DYNAMIC_VERSION_CACHE = new ConcurrentHashMap<>();

    static {
        initCatalog();
        initVersionMatrix();
    }

    private JakartaMetadata() {
    }

    public static List<JakartaCategory> getCategories() {
        return Collections.unmodifiableList(CATEGORIES);
    }

    public static JakartaDep getDependency(String id) {
        return DEPS_BY_ID.get(id);
    }

    public static List<String> getDefaultDependenciesForTemplate(String template) {
        if (template == null) return List.of();
        if (template.contains("REST") || template.equalsIgnoreCase(TEMPLATE_REST)) {
            return List.of("cdi", "jaxrs", "servlet");
        } else if (template.contains("Web") || template.equalsIgnoreCase(TEMPLATE_WEB)) {
            return List.of("servlet");
        }
        return List.of();
    }

    public static String getVersion(String depId, String jakartaVersion) {
        String jVersion = (jakartaVersion == null || jakartaVersion.isBlank()) ? EE_11 : jakartaVersion;
        Map<String, String> matrix = VERSION_MATRIX.get(jVersion);
        if (matrix != null && matrix.containsKey(depId)) {
            // If we dynamically resolved a newer version for this artifact, we can return it
            String cached = DYNAMIC_VERSION_CACHE.get(depId + "@" + jVersion);
            return cached != null ? cached : matrix.get(depId);
        }
        // Fallback to EE 11 matrix or default
        Map<String, String> fallback = VERSION_MATRIX.get(EE_11);
        if (fallback != null && fallback.containsKey(depId)) {
            return fallback.get(depId);
        }
        return "";
    }

    /**
     * Queries Maven Central dynamically off-thread to retrieve the latest version for a dependency.
     */
    public static String fetchLatestMavenCentralVersion(String groupId, String artifactId) throws IOException {
        String cacheKey = groupId + ":" + artifactId;
        if (DYNAMIC_VERSION_CACHE.containsKey(cacheKey)) {
            return DYNAMIC_VERSION_CACHE.get(cacheKey);
        }

        String query = "g:\"" + groupId + "\" AND a:\"" + artifactId + "\"";
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://search.maven.org/solrsearch/select?q=" + encoded + "&rows=1&wt=json";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("response")) {
                    JsonObject respObj = root.getAsJsonObject("response");
                    if (respObj.has("docs")) {
                        JsonArray docs = respObj.getAsJsonArray("docs");
                        if (!docs.isEmpty()) {
                            JsonObject doc = docs.get(0).getAsJsonObject();
                            if (doc.has("latestVersion")) {
                                String v = doc.get("latestVersion").getAsString();
                                DYNAMIC_VERSION_CACHE.put(cacheKey, v);
                                return v;
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Search interrupted", e);
        } catch (Exception e) {
            throw new IOException("Failed to fetch Maven Central metadata: " + e.getMessage(), e);
        }

        throw new IOException("No version found for " + groupId + ":" + artifactId);
    }

    private static void initCatalog() {
        // --- Specifications ---
        List<JakartaDep> specs = new ArrayList<>();

        specs.add(new JakartaDep(
                "full-platform",
                "Full Platform",
                "Specifications",
                "Includes most of the Jakarta EE specifications. Compatible servers: GlassFish 7.x, Wildfly 27.x",
                "https://jakarta.ee",
                "https://jakarta.ee/specifications/platform/",
                "jakarta.platform",
                "jakarta.jakartaee-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "web-profile",
                "Web Profile",
                "Specifications",
                "A profile designed for modern web applications including Servlet, Pages, REST, CDI, and Validation.",
                "https://jakarta.ee",
                "https://jakarta.ee/specifications/webprofile/",
                "jakarta.platform",
                "jakarta.jakartaee-web-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "core-profile",
                "Core Profile",
                "Specifications",
                "A targeted minimal profile aimed at modern, lightweight cloud-native runtimes and microservices.",
                "https://jakarta.ee",
                "https://jakarta.ee/specifications/coreprofile/",
                "jakarta.platform",
                "jakarta.jakartaee-core-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "batch",
                "Batch",
                "Specifications",
                "Specification for batch processing applications in Java.",
                "https://jakarta.ee/specifications/batch/",
                "https://jakarta.ee/specifications/batch/",
                "jakarta.batch",
                "jakarta.batch-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "validation",
                "Bean Validation",
                "Specifications",
                "Standard validation framework for JavaBeans and model constraints.",
                "https://beanvalidation.org/",
                "https://jakarta.ee/specifications/bean-validation/",
                "jakarta.validation",
                "jakarta.validation-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "cdi",
                "Contexts and Dependency Injection (CDI)",
                "Specifications",
                "Type-safe dependency injection and contextual lifecycle management for Java EE/Jakarta EE components.",
                "https://jakarta.ee/specifications/cdi/",
                "https://jakarta.ee/specifications/cdi/",
                "jakarta.enterprise",
                "jakarta.enterprise.cdi-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "concurrency",
                "Concurrency Utils",
                "Specifications",
                "Provides utilities for concurrent and asynchronous programming in managed enterprise environments.",
                "https://jakarta.ee/specifications/concurrency/",
                "https://jakarta.ee/specifications/concurrency/",
                "jakarta.enterprise.concurrent",
                "jakarta.enterprise.concurrent-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "connector",
                "Connector Architecture (JCA)",
                "Specifications",
                "Standard architecture for integrating Java applications with legacy enterprise information systems (EIS).",
                "https://jakarta.ee/specifications/connectors/",
                "https://jakarta.ee/specifications/connectors/",
                "jakarta.resource",
                "jakarta.resource-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "data",
                "Data",
                "Specifications",
                "Standard repository programming model for convenient data access across relational and non-relational databases.",
                "https://jakarta.ee/specifications/data/",
                "https://jakarta.ee/specifications/data/",
                "jakarta.data",
                "jakarta.data-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "ejb",
                "Enterprise Java Beans (EJB)",
                "Specifications",
                "Component architecture for building distributed, transactional, and scalable business logic.",
                "https://jakarta.ee/specifications/enterprise-beans/",
                "https://jakarta.ee/specifications/enterprise-beans/",
                "jakarta.ejb",
                "jakarta.ejb-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "json-b",
                "JSON Binding (JSON-B)",
                "Specifications",
                "Standard binding layer for converting Java objects to and from JSON messages.",
                "https://jakarta.ee/specifications/jsonb/",
                "https://jakarta.ee/specifications/jsonb/",
                "jakarta.json.bind",
                "jakarta.json.bind-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "json-p",
                "JSON Processing (JSON-P)",
                "Specifications",
                "Standard API for parsing, generating, transforming, and querying JSON documents.",
                "https://jakarta.ee/specifications/jsonp/",
                "https://jakarta.ee/specifications/jsonp/",
                "jakarta.json",
                "jakarta.json-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "jms",
                "Message Service (JMS)",
                "Specifications",
                "Enterprise messaging API that allows applications to create, send, receive, and read messages asynchronously.",
                "https://jakarta.ee/specifications/messaging/",
                "https://jakarta.ee/specifications/messaging/",
                "jakarta.jms",
                "jakarta.jms-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "mvc",
                "Model View Controller (MVC)",
                "Specifications",
                "Action-based Model-View-Controller web framework specification for Jakarta EE.",
                "https://jakarta.ee/specifications/mvc/",
                "https://jakarta.ee/specifications/mvc/",
                "jakarta.mvc",
                "jakarta.mvc-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "nosql",
                "NoSQL",
                "Specifications",
                "Standard Java API designed to streamline communication and integration with NoSQL database engines.",
                "https://jakarta.ee/specifications/nosql/",
                "https://jakarta.ee/specifications/nosql/",
                "jakarta.nosql",
                "jakarta.nosql-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "jpa",
                "Persistence (JPA)",
                "Specifications",
                "Standard specification for Object-Relational Mapping (ORM) and management of persistent relational data.",
                "https://jakarta.ee/specifications/persistence/",
                "https://jakarta.ee/specifications/persistence/",
                "jakarta.persistence",
                "jakarta.persistence-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "jaxrs",
                "RESTful Web Services (JAX-RS)",
                "Specifications",
                "Standard Java API for building lightweight RESTful web services and HTTP resources.",
                "https://jakarta.ee/specifications/restful-ws/",
                "https://jakarta.ee/specifications/restful-ws/",
                "jakarta.ws.rs",
                "jakarta.ws.rs-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "security",
                "Security",
                "Specifications",
                "Standard security API covering authentication mechanisms, identity stores, and security contexts.",
                "https://jakarta.ee/specifications/security/",
                "https://jakarta.ee/specifications/security/",
                "jakarta.security.enterprise",
                "jakarta.security.enterprise-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "servlet",
                "Servlet",
                "Specifications",
                "Core web component specification for handling HTTP requests, responses, filters, and sessions.",
                "https://jakarta.ee/specifications/servlet/",
                "https://jakarta.ee/specifications/servlet/",
                "jakarta.servlet",
                "jakarta.servlet-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "faces",
                "Server Faces",
                "Specifications",
                "Server-side component-oriented user interface framework for building rich web applications.",
                "https://jakarta.ee/specifications/faces/",
                "https://jakarta.ee/specifications/faces/",
                "jakarta.faces",
                "jakarta.faces-api",
                "provided"
        ));

        specs.add(new JakartaDep(
                "websocket",
                "WebSocket",
                "Specifications",
                "Specification for bidirectional, full-duplex interactive communication over a single TCP socket.",
                "https://jakarta.ee/specifications/websocket/",
                "https://jakarta.ee/specifications/websocket/",
                "jakarta.websocket",
                "jakarta.websocket-api",
                "provided"
        ));

        for (JakartaDep d : specs) {
            DEPS_BY_ID.put(d.id(), d);
        }
        CATEGORIES.add(new JakartaCategory(
                "Specifications",
                "Java Enterprise API dependencies.",
                specs
        ));

        // --- Implementations ---
        List<JakartaDep> impls = new ArrayList<>();

        impls.add(new JakartaDep(
                "jersey-server",
                "Eclipse Jersey Server",
                "Implementations",
                "Reference implementation of Jakarta RESTful Web Services (JAX-RS) server container.",
                "https://eclipse-ee4j.github.io/jersey/",
                "https://eclipse-ee4j.github.io/jersey/",
                "org.glassfish.jersey.containers",
                "jersey-container-servlet",
                "compile"
        ));

        impls.add(new JakartaDep(
                "jersey-client",
                "Eclipse Jersey Client",
                "Implementations",
                "Reference implementation of Jakarta RESTful Web Services (JAX-RS) HTTP client.",
                "https://eclipse-ee4j.github.io/jersey/",
                "https://eclipse-ee4j.github.io/jersey/",
                "org.glassfish.jersey.core",
                "jersey-client",
                "compile"
        ));

        impls.add(new JakartaDep(
                "eclipselink",
                "EclipseLink",
                "Implementations",
                "EclipseLink persistence service: comprehensive Object-Relational Mapping (ORM) provider for JPA.",
                "https://www.eclipse.org/eclipselink/",
                "https://www.eclipse.org/eclipselink/",
                "org.eclipse.persistence",
                "org.eclipse.persistence.jpa",
                "compile"
        ));

        impls.add(new JakartaDep(
                "hibernate",
                "Hibernate",
                "Implementations",
                "Industry-standard high performance Object/Relational Mapping (ORM) engine and JPA provider.",
                "https://hibernate.org/orm/",
                "https://hibernate.org/orm/",
                "org.hibernate.orm",
                "hibernate-core",
                "compile"
        ));

        impls.add(new JakartaDep(
                "hibernate-validator",
                "Hibernate Validator",
                "Implementations",
                "Reference implementation of the Bean Validation specification.",
                "https://hibernate.org/validator/",
                "https://hibernate.org/validator/",
                "org.hibernate.validator",
                "hibernate-validator",
                "compile"
        ));

        impls.add(new JakartaDep(
                "mojarra",
                "Mojarra Server Faces",
                "Implementations",
                "Reference implementation of Jakarta Server Faces (JSF).",
                "https://github.com/eclipse-ee4j/mojarra",
                "https://github.com/eclipse-ee4j/mojarra",
                "org.glassfish",
                "jakarta.faces",
                "compile"
        ));

        impls.add(new JakartaDep(
                "tyrus-server",
                "Tyrus Server",
                "Implementations",
                "Reference implementation of Jakarta WebSocket server component.",
                "https://eclipse-ee4j.github.io/tyrus/",
                "https://eclipse-ee4j.github.io/tyrus/",
                "org.glassfish.tyrus",
                "tyrus-server",
                "compile"
        ));

        impls.add(new JakartaDep(
                "tyrus-client",
                "Tyrus Client",
                "Implementations",
                "Reference implementation of Jakarta WebSocket client component.",
                "https://eclipse-ee4j.github.io/tyrus/",
                "https://eclipse-ee4j.github.io/tyrus/",
                "org.glassfish.tyrus",
                "tyrus-client",
                "compile"
        ));

        impls.add(new JakartaDep(
                "weld-se",
                "Weld SE",
                "Implementations",
                "Reference implementation of Contexts and Dependency Injection (CDI) with SE bootstrap support.",
                "https://weld.cdi-spec.org/",
                "https://weld.cdi-spec.org/",
                "org.jboss.weld.se",
                "weld-se-core",
                "compile"
        ));

        for (JakartaDep d : impls) {
            DEPS_BY_ID.put(d.id(), d);
        }
        CATEGORIES.add(new JakartaCategory(
                "Implementations",
                "Third-party implementations of Jakarta EE specifications.",
                impls
        ));
    }

    private static void initVersionMatrix() {
        // --- Jakarta EE 11 ---
        Map<String, String> v11 = new LinkedHashMap<>();
        v11.put("full-platform", "11.0.0");
        v11.put("web-profile", "11.0.0");
        v11.put("core-profile", "11.0.0");
        v11.put("batch", "2.1.1");
        v11.put("validation", "3.1.1");
        v11.put("cdi", "4.1.0");
        v11.put("concurrency", "3.1.1");
        v11.put("connector", "2.1.0");
        v11.put("data", "1.0.1");
        v11.put("ejb", "4.0.1");
        v11.put("json-b", "3.0.1");
        v11.put("json-p", "2.1.3");
        v11.put("jms", "3.1.0");
        v11.put("mvc", "3.0.0");
        v11.put("nosql", "1.0.1");
        v11.put("jpa", "3.2.0");
        v11.put("jaxrs", "4.0.0");
        v11.put("security", "4.0.0");
        v11.put("servlet", "6.1.0");
        v11.put("faces", "4.1.0");
        v11.put("websocket", "2.2.0");

        v11.put("jersey-server", "4.0.0-M2");
        v11.put("jersey-client", "4.0.0-M2");
        v11.put("eclipselink", "4.0.7");
        v11.put("hibernate", "7.0.4.Final");
        v11.put("hibernate-validator", "9.0.1.Final");
        v11.put("mojarra", "4.1.3");
        v11.put("tyrus-server", "2.2.0");
        v11.put("tyrus-client", "2.2.0");
        v11.put("weld-se", "6.0.3.Final");
        VERSION_MATRIX.put(EE_11, v11);

        // --- Jakarta EE 10 ---
        Map<String, String> v10 = new LinkedHashMap<>();
        v10.put("full-platform", "10.0.0");
        v10.put("web-profile", "10.0.0");
        v10.put("core-profile", "10.0.0");
        v10.put("batch", "2.1.1");
        v10.put("validation", "3.0.2");
        v10.put("cdi", "4.0.1");
        v10.put("concurrency", "3.0.0");
        v10.put("connector", "2.1.0");
        v10.put("data", "1.0.0");
        v10.put("ejb", "4.0.1");
        v10.put("json-b", "3.0.0");
        v10.put("json-p", "2.1.1");
        v10.put("jms", "3.1.0");
        v10.put("mvc", "2.1.0");
        v10.put("nosql", "1.0.0");
        v10.put("jpa", "3.1.0");
        v10.put("jaxrs", "3.1.0");
        v10.put("security", "3.0.0");
        v10.put("servlet", "6.0.0");
        v10.put("faces", "4.0.1");
        v10.put("websocket", "2.1.1");

        v10.put("jersey-server", "3.1.5");
        v10.put("jersey-client", "3.1.5");
        v10.put("eclipselink", "4.0.2");
        v10.put("hibernate", "6.4.4.Final");
        v10.put("hibernate-validator", "8.0.1.Final");
        v10.put("mojarra", "4.0.4");
        v10.put("tyrus-server", "2.1.4");
        v10.put("tyrus-client", "2.1.4");
        v10.put("weld-se", "5.1.2.Final");
        VERSION_MATRIX.put(EE_10, v10);

        // --- Jakarta EE 9.1 ---
        Map<String, String> v9 = new LinkedHashMap<>();
        v9.put("full-platform", "9.1.0");
        v9.put("web-profile", "9.1.0");
        v9.put("core-profile", "9.1.0");
        v9.put("batch", "2.0.0");
        v9.put("validation", "3.0.0");
        v9.put("cdi", "3.0.0");
        v9.put("concurrency", "2.0.0");
        v9.put("connector", "2.0.0");
        v9.put("data", "");
        v9.put("ejb", "4.0.0");
        v9.put("json-b", "2.0.0");
        v9.put("json-p", "2.0.0");
        v9.put("jms", "3.0.0");
        v9.put("mvc", "2.0.0");
        v9.put("nosql", "");
        v9.put("jpa", "3.0.0");
        v9.put("jaxrs", "3.0.0");
        v9.put("security", "2.0.0");
        v9.put("servlet", "5.0.0");
        v9.put("faces", "3.0.0");
        v9.put("websocket", "2.0.0");

        v9.put("jersey-server", "3.0.3");
        v9.put("jersey-client", "3.0.3");
        v9.put("eclipselink", "3.0.2");
        v9.put("hibernate", "5.6.15.Final");
        v9.put("hibernate-validator", "7.0.5.Final");
        v9.put("mojarra", "3.0.2");
        v9.put("tyrus-server", "2.0.3");
        v9.put("tyrus-client", "2.0.3");
        v9.put("weld-se", "4.0.3.Final");
        VERSION_MATRIX.put(EE_9_1, v9);

        // --- Jakarta EE 8 ---
        Map<String, String> v8 = new LinkedHashMap<>();
        v8.put("full-platform", "8.0.0");
        v8.put("web-profile", "8.0.0");
        v8.put("core-profile", "8.0.0");
        v8.put("batch", "1.0.2");
        v8.put("validation", "2.0.2");
        v8.put("cdi", "2.0.2");
        v8.put("concurrency", "1.1.2");
        v8.put("connector", "1.7.3");
        v8.put("data", "");
        v8.put("ejb", "3.2.6");
        v8.put("json-b", "1.0.2");
        v8.put("json-p", "1.1.6");
        v8.put("jms", "2.0.3");
        v8.put("mvc", "1.0.1");
        v8.put("nosql", "");
        v8.put("jpa", "2.2.3");
        v8.put("jaxrs", "2.1.6");
        v8.put("security", "1.0.2");
        v8.put("servlet", "4.0.4");
        v8.put("faces", "2.3.2");
        v8.put("websocket", "1.1.2");

        v8.put("jersey-server", "2.35");
        v8.put("jersey-client", "2.35");
        v8.put("eclipselink", "2.7.12");
        v8.put("hibernate", "5.4.33.Final");
        v8.put("hibernate-validator", "6.2.5.Final");
        v8.put("mojarra", "2.3.17");
        v8.put("tyrus-server", "1.18");
        v8.put("tyrus-client", "1.18");
        v8.put("weld-se", "3.1.9.Final");
        VERSION_MATRIX.put(EE_8, v8);
    }
}
