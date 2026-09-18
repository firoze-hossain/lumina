package dev.lumina.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Metadata and scaffolding engine for Google App Engine (Go) projects,
 * matching IntelliJ IDEA / GoLand.
 */
public final class AppEngineMetadata {

    public static final String DEFAULT_PROJECT_FORMAT = ".idea (directory-based)";
    public static final String PROJECT_FORMAT_FILE_BASED = ".ipr (file-based)";

    public static final String DEFAULT_SQL_DIALECT = "Project Default";

    public static final List<String> SQL_DIALECTS = List.of(
            "Project Default",
            "Amazon DynamoDB",
            "Amazon Redshift",
            "Apache Cassandra",
            "Apache Derby",
            "Apache Hive",
            "Apache Spark",
            "Azure SQL Database",
            "BigQuery",
            "ClickHouse",
            "Couchbase",
            "DB2 (LUW)",
            "DB2 (z/OS)",
            "Exasol",
            "Generic SQL",
            "Greenplum",
            "H2",
            "Hive",
            "HSQLDB",
            "Informix",
            "MariaDB",
            "Microsoft SQL Server",
            "MongoDB",
            "MySQL",
            "Oracle",
            "PostgreSQL",
            "Presto",
            "Redis",
            "Snowflake",
            "SQLite",
            "Sybase (ASE)",
            "Trino",
            "Vertica"
    );

    /**
     * Generates .idea/sqldialects.xml for project-level SQL dialect mapping matching IntelliJ IDEA.
     */
    public static String generateSqlDialectsXml(String dialect) {
        if (dialect == null || dialect.isBlank() || DEFAULT_SQL_DIALECT.equalsIgnoreCase(dialect.trim())) {
            return null;
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="SqlDialectMappings">
                    <file url="PROJECT" dialect="%s" />
                  </component>
                </project>
                """.formatted(dialect.trim());
    }

    private AppEngineMetadata() {
    }

    /**
     * Resolves the App Engine Go runtime identifier from a Go version string.
     * e.g. "go1.24.1" -> "go124", "1.23.5" -> "go123", "go1.22" -> "go122".
     * If unparseable or unknown, defaults to "go122".
     */
    public static String resolveAppEngineRuntime(String goVersion) {
        if (goVersion == null || goVersion.isBlank()) {
            return "go122";
        }
        String clean = goVersion.trim().toLowerCase();
        if (clean.startsWith("go")) {
            clean = clean.substring(2);
        }
        String[] parts = clean.split("\\.");
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                if (major == 1) {
                    return "go" + major + minor;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return "go122";
    }

    /**
     * Generates Google App Engine app.yaml configuration.
     */
    public static String generateAppYaml(String goVersion, boolean sqlSupport) {
        return generateAppYaml(goVersion, sqlSupport, DEFAULT_SQL_DIALECT);
    }

    public static String generateAppYaml(String goVersion, boolean sqlSupport, String sqlDialect) {
        String runtime = resolveAppEngineRuntime(goVersion);
        StringBuilder sb = new StringBuilder();
        sb.append("# Google App Engine standard environment configuration\n");
        sb.append("runtime: ").append(runtime).append("\n");
        sb.append("instance_class: F1\n\n");
        sb.append("automatic_scaling:\n");
        sb.append("  target_cpu_utilization: 0.65\n");

        if (sqlSupport) {
            sb.append("\n# Cloud SQL connection configuration:\n");
            sb.append("# env_variables:\n");
            sb.append("#   INSTANCE_CONNECTION_NAME: 'project:region:instance'\n");
            sb.append("#   DB_USER: 'root'\n");
            sb.append("#   DB_PASS: 'password'\n");
            sb.append("#   DB_NAME: 'app_db'\n");
            if (sqlDialect != null && !sqlDialect.isBlank() && !DEFAULT_SQL_DIALECT.equals(sqlDialect)) {
                sb.append("#   DB_DIALECT: '").append(sqlDialect.trim()).append("'\n");
            }
            sb.append("# beta_settings:\n");
            sb.append("#   cloudsql_instances: 'project:region:instance'\n");
        }

        return sb.toString();
    }

    /**
     * Generates standard main.go entry point for Google App Engine Go web service.
     */
    public static String generateMainGo(String moduleName, boolean sqlSupport) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Package main provides the entry point for the App Engine service.\n");
        sb.append("package main\n\n");
        sb.append("import (\n");
        sb.append("\t\"fmt\"\n");
        sb.append("\t\"log\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"os\"\n");
        sb.append(")\n\n");

        sb.append("func main() {\n");
        if (sqlSupport) {
            sb.append("\t// Initialize database connection if configured\n");
            sb.append("\tinitDatabase()\n\n");
        }
        sb.append("\thttp.HandleFunc(\"/\", indexHandler)\n");
        sb.append("\thttp.HandleFunc(\"/_ah/health\", healthHandler)\n\n");
        sb.append("\tport := os.Getenv(\"PORT\")\n");
        sb.append("\tif port == \"\" {\n");
        sb.append("\t\tport = \"8080\"\n");
        sb.append("\t\tlog.Printf(\"Defaulting to port %s\", port)\n");
        sb.append("\t}\n\n");
        sb.append("\tlog.Printf(\"Listening on port %s\", port)\n");
        sb.append("\tif err := http.ListenAndServe(\":\"+port, nil); err != nil {\n");
        sb.append("\t\tlog.Fatal(err)\n");
        sb.append("\t}\n");
        sb.append("}\n\n");

        sb.append("func indexHandler(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\tif r.URL.Path != \"/\" {\n");
        sb.append("\t\thttp.NotFound(w, r)\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n");
        sb.append("\tfmt.Fprintf(w, \"Hello from Google App Engine (%s)!\")\n".formatted(moduleName));
        sb.append("}\n\n");

        sb.append("func healthHandler(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\tfmt.Fprintf(w, \"ok\")\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates go.mod file.
     */
    public static String generateGoMod(String moduleName, String goVersion) {
        String langVer = GoMetadata.extractLanguageVersion(goVersion);
        return """
                module %s

                go %s
                """.formatted(moduleName, langVer);
    }

    /**
     * Generates db.go helper when SQL Support is enabled.
     */
    public static String generateDbGo() {
        return generateDbGo(DEFAULT_SQL_DIALECT);
    }

    public static String generateDbGo(String sqlDialect) {
        String dialectComment = (sqlDialect != null && !DEFAULT_SQL_DIALECT.equals(sqlDialect))
                ? " (" + sqlDialect + ")" : "";
        return """
                // Package main provides database connection boilerplate for Google Cloud SQL / standard SQL%s.
                package main

                import (
                	"database/sql"
                	"log"
                	"os"
                )

                var db *sql.DB

                func initDatabase() {
                	dbUser := os.Getenv("DB_USER")
                	dbPass := os.Getenv("DB_PASS")
                	dbName := os.Getenv("DB_NAME")
                	instanceConnName := os.Getenv("INSTANCE_CONNECTION_NAME")

                	if instanceConnName == "" {
                		log.Println("INSTANCE_CONNECTION_NAME not set; skipping Cloud SQL initialization.")
                		return
                	}

                	log.Printf("Connecting to Cloud SQL instance %%s (user: %%s, db: %%s)...", instanceConnName, dbUser, dbName)
                	// Placeholder for Cloud SQL driver connection:
                	// var err error
                	// db, err = sql.Open("cloudsql", fmt.Sprintf("%%s:%%s@cloudsql(%%s)/%%s", dbUser, dbPass, instanceConnName, dbName))
                	// if err != nil {
                	// 	log.Fatalf("Failed to connect to database: %%v", err)
                	// }
                }
                """.formatted(dialectComment);
    }

    /**
     * Generates companion python script when Python support is enabled.
     */
    public static String generatePythonCompanion(String moduleName) {
        return """
                #!/usr/bin/env python3
                \"\"\"
                Companion script for App Engine module %s.
                \"\"\"
                import sys

                def main():
                    print("App Engine Python Companion for %s")

                if __name__ == "__main__":
                    main()
                """.formatted(moduleName, moduleName);
    }

    /**
     * Generates standard .gitignore.
     */
    public static String generateGitignore() {
        return """
                # Binaries
                *.exe
                *.exe~
                *.dll
                *.so
                *.dylib
                bin/

                # Test binary, built with `go test -c`
                *.test

                # Output of the go coverage tool
                *.out

                # Dependency directories
                vendor/

                # Go workspace file
                go.work

                # IDE files
                .idea/
                *.iml
                """;
    }

    /**
     * Generates README.md with deployment and running instructions.
     */
    public static String generateReadme(String projectName, String goVersion) {
        String runtime = resolveAppEngineRuntime(goVersion);
        return """
                # %s

                Google App Engine Go service created with Lumina IDE.

                ## Prerequisites
                - [Go](https://go.dev/) (target runtime: `%s`)
                - [Google Cloud SDK (gcloud CLI)](https://cloud.google.com/sdk)

                ## Running Locally
                ```bash
                go run .
                ```
                The server will start at `http://localhost:8080`.

                ## Deploying to Google App Engine
                ```bash
                gcloud app deploy
                ```

                ## Viewing Live App
                ```bash
                gcloud app browse
                ```
                """.formatted(projectName, runtime);
    }

    /**
     * Generates .idea/<moduleName>.iml for App Engine Go project.
     */
    public static String generateIdeaAppEngineIml(String moduleName, boolean pythonSupport, boolean sqlSupport) {
        StringBuilder facets = new StringBuilder();
        facets.append("""
                    <facet type="google-app-engine-go" name="Google App Engine">
                      <configuration />
                    </facet>
                """);
        if (pythonSupport) {
            facets.append("""
                        <facet type="Python" name="Python">
                          <configuration sdkName="" />
                        </facet>
                    """);
        }
        if (sqlSupport) {
            facets.append("""
                        <facet type="SQL" name="SQL Support">
                          <configuration />
                        </facet>
                    """);
        }

        String facetManager = facets.isEmpty() ? "" : """
                  <component name="FacetManager">
                %s  </component>
                """.formatted(facets.toString());

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <module type="WEB_MODULE" version="4">
                %s  <component name="Go" enabled="true">
                    <buildTags>
                      <option name="customFlags">
                        <array>
                          <option value="appengine" />
                        </array>
                      </option>
                    </buildTags>
                  </component>
                  <component name="NewModuleRootManager">
                    <content url="file://$MODULE_DIR$" />
                    <orderEntry type="inheritedJdk" />
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                </module>
                """.formatted(facetManager);
    }

    /**
     * Generates .idea/modules.xml.
     */
    public static String generateIdeaModulesXml(String moduleName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectModuleManager">
                    <modules>
                      <module fileurl="file://$PROJECT_DIR$/.idea/%s.iml" filepath="$PROJECT_DIR$/.idea/%s.iml" />
                    </modules>
                  </component>
                </project>
                """.formatted(moduleName, moduleName);
    }

    /**
     * Generates .idea/misc.xml.
     */
    public static String generateIdeaMiscXml(String sdkName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectRootManager" version="2" languageLevel="DEFAULT" project-jdk-name="%s" project-jdk-type="Go SDK" />
                </project>
                """.formatted(sdkName != null && !sdkName.isBlank() ? sdkName : "Go");
    }

    /**
     * Scaffolds the complete Google App Engine Go project workspace.
     */
    public static void scaffoldProject(Path dir, String projectName, String goVersion,
                                       boolean sqlSupport, boolean pythonSupport,
                                       Consumer<String> log) throws IOException {
        scaffoldProject(dir, projectName, goVersion, sqlSupport, DEFAULT_SQL_DIALECT, pythonSupport, log);
    }

    public static void scaffoldProject(Path dir, String projectName, String goVersion,
                                       boolean sqlSupport, String sqlDialect, boolean pythonSupport,
                                       Consumer<String> log) throws IOException {
        Files.createDirectories(dir);

        // 1. app.yaml
        Files.writeString(dir.resolve("app.yaml"), generateAppYaml(goVersion, sqlSupport, sqlDialect));
        log.accept("Created app.yaml (runtime: " + resolveAppEngineRuntime(goVersion) + ")");

        // 2. main.go
        Files.writeString(dir.resolve("main.go"), generateMainGo(projectName, sqlSupport));
        log.accept("Created main.go with HTTP server and /_ah/health check");

        // 3. go.mod
        Files.writeString(dir.resolve("go.mod"), generateGoMod(projectName, goVersion));
        log.accept("Created go.mod");

        // 4. SQL Support if enabled
        if (sqlSupport) {
            Files.writeString(dir.resolve("db.go"), generateDbGo(sqlDialect));
            log.accept("Created db.go (Cloud SQL / database boilerplate" + (sqlDialect != null && !DEFAULT_SQL_DIALECT.equals(sqlDialect) ? " - " + sqlDialect : "") + ")");
        }

        // 5. Python companion if enabled
        if (pythonSupport) {
            Files.writeString(dir.resolve("companion.py"), generatePythonCompanion(projectName));
            log.accept("Created companion.py");
        }

        // 6. Documentation & ignores
        Files.writeString(dir.resolve("README.md"), generateReadme(projectName, goVersion));
        Files.writeString(dir.resolve(".gitignore"), generateGitignore());
        log.accept("Created README.md and .gitignore");
    }
}
