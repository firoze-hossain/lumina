package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppEngineMetadataTest {

    @Test
    void testResolveAppEngineRuntime() {
        assertEquals("go124", AppEngineMetadata.resolveAppEngineRuntime("go1.24.1"));
        assertEquals("go124", AppEngineMetadata.resolveAppEngineRuntime("1.24.0"));
        assertEquals("go123", AppEngineMetadata.resolveAppEngineRuntime("go1.23.5"));
        assertEquals("go122", AppEngineMetadata.resolveAppEngineRuntime("go1.22.4"));
        assertEquals("go121", AppEngineMetadata.resolveAppEngineRuntime("1.21"));
        assertEquals("go122", AppEngineMetadata.resolveAppEngineRuntime(null));
        assertEquals("go122", AppEngineMetadata.resolveAppEngineRuntime(""));
        assertEquals("go122", AppEngineMetadata.resolveAppEngineRuntime("invalid-version"));
    }

    @Test
    void testGenerateAppYamlWithoutSql() {
        String yaml = AppEngineMetadata.generateAppYaml("go1.24.1", false);
        assertNotNull(yaml);
        assertTrue(yaml.contains("runtime: go124"));
        assertTrue(yaml.contains("instance_class: F1"));
        assertTrue(yaml.contains("automatic_scaling:"));
        assertTrue(yaml.contains("target_cpu_utilization: 0.65"));
        assertFalse(yaml.contains("cloudsql_instances"));
    }

    @Test
    void testGenerateAppYamlWithSql() {
        String yaml = AppEngineMetadata.generateAppYaml("go1.23.0", true);
        assertNotNull(yaml);
        assertTrue(yaml.contains("runtime: go123"));
        assertTrue(yaml.contains("beta_settings:"));
        assertTrue(yaml.contains("cloudsql_instances:"));
        assertTrue(yaml.contains("DB_NAME:"));
    }

    @Test
    void testGenerateMainGoWithoutSql() {
        String mainGo = AppEngineMetadata.generateMainGo("test-app", false);
        assertNotNull(mainGo);
        assertTrue(mainGo.contains("package main"));
        assertTrue(mainGo.contains("net/http"));
        assertTrue(mainGo.contains("http.HandleFunc(\"/\", indexHandler)"));
        assertTrue(mainGo.contains("http.HandleFunc(\"/_ah/health\", healthHandler)"));
        assertTrue(mainGo.contains("PORT"));
        assertTrue(mainGo.contains("8080"));
        assertFalse(mainGo.contains("initDatabase()"));
    }

    @Test
    void testGenerateMainGoWithSql() {
        String mainGo = AppEngineMetadata.generateMainGo("test-app", true);
        assertNotNull(mainGo);
        assertTrue(mainGo.contains("initDatabase()"));
    }

    @Test
    void testGenerateGoMod() {
        String goMod = AppEngineMetadata.generateGoMod("my-module", "go1.24.1");
        assertNotNull(goMod);
        assertTrue(goMod.contains("module my-module"));
        assertTrue(goMod.contains("go 1.24"));

        String fallbackGoMod = AppEngineMetadata.generateGoMod("demo", null);
        assertTrue(fallbackGoMod.contains("module demo"));
        assertTrue(fallbackGoMod.contains("go 1.24"));
    }

    @Test
    void testGenerateDbGo() {
        String dbGo = AppEngineMetadata.generateDbGo();
        assertNotNull(dbGo);
        assertTrue(dbGo.contains("package main"));
        assertTrue(dbGo.contains("database/sql"));
        assertTrue(dbGo.contains("initDatabase()"));
        assertTrue(dbGo.contains("cloudsql"));
    }

    @Test
    void testGeneratePythonCompanion() {
        String py = AppEngineMetadata.generatePythonCompanion("my-app");
        assertNotNull(py);
        assertTrue(py.contains("#!/usr/bin/env python3"));
        assertTrue(py.contains("my-app"));
        assertTrue(py.contains("App Engine Python Companion"));
    }

    @Test
    void testGenerateIdeaDescriptors() {
        String iml = AppEngineMetadata.generateIdeaAppEngineIml("my-app", true, true);
        assertNotNull(iml);
        assertTrue(iml.contains("WEB_MODULE"));
        assertTrue(iml.contains("appengine"));
        assertTrue(iml.contains("Python"));
        assertTrue(iml.contains("SQL Support"));

        String modules = AppEngineMetadata.generateIdeaModulesXml("my-app");
        assertNotNull(modules);
        assertTrue(modules.contains("my-app.iml"));

        String misc = AppEngineMetadata.generateIdeaMiscXml("Go 1.24");
        assertNotNull(misc);
        assertTrue(misc.contains("Go 1.24"));
    }

    @Test
    void testSqlDialectsList() {
        assertNotNull(AppEngineMetadata.SQL_DIALECTS);
        assertEquals(33, AppEngineMetadata.SQL_DIALECTS.size());
        assertEquals("Project Default", AppEngineMetadata.SQL_DIALECTS.get(0));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("PostgreSQL"));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("MySQL"));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("Oracle"));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("SQLite"));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("Microsoft SQL Server"));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("BigQuery"));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("Snowflake"));
        assertTrue(AppEngineMetadata.SQL_DIALECTS.contains("ClickHouse"));
    }

    @Test
    void testGenerateSqlDialectsXml() {
        assertNull(AppEngineMetadata.generateSqlDialectsXml(null));
        assertNull(AppEngineMetadata.generateSqlDialectsXml(""));
        assertNull(AppEngineMetadata.generateSqlDialectsXml("Project Default"));

        String xml = AppEngineMetadata.generateSqlDialectsXml("PostgreSQL");
        assertNotNull(xml);
        assertTrue(xml.contains("<component name=\"SqlDialectMappings\">"));
        assertTrue(xml.contains("<file url=\"PROJECT\" dialect=\"PostgreSQL\" />"));

        String mySqlXml = AppEngineMetadata.generateSqlDialectsXml("MySQL");
        assertNotNull(mySqlXml);
        assertTrue(mySqlXml.contains("dialect=\"MySQL\""));
    }

    @Test
    void testGenerateAppYamlWithSpecificDialect() {
        String yaml = AppEngineMetadata.generateAppYaml("go1.24.1", true, "PostgreSQL");
        assertNotNull(yaml);
        assertTrue(yaml.contains("PostgreSQL"));
        assertTrue(yaml.contains("cloudsql_instances"));
    }

    @Test
    void testGenerateDbGoWithSpecificDialect() {
        String dbGo = AppEngineMetadata.generateDbGo("PostgreSQL");
        assertNotNull(dbGo);
        assertTrue(dbGo.contains("PostgreSQL"));
        assertTrue(dbGo.contains("package main"));
        assertTrue(dbGo.contains("initDatabase()"));
    }

    @Test
    void testScaffoldProjectDirect(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        Path appDir = tempDir.resolve("scaffold-app");

        AppEngineMetadata.scaffoldProject(appDir, "scaffold-app", "go1.24.1", true, "PostgreSQL", true, logs::add);

        assertTrue(Files.exists(appDir.resolve("app.yaml")));
        assertTrue(Files.exists(appDir.resolve("main.go")));
        assertTrue(Files.exists(appDir.resolve("go.mod")));
        assertTrue(Files.exists(appDir.resolve("db.go")));
        assertTrue(Files.exists(appDir.resolve("companion.py")));
        assertTrue(Files.exists(appDir.resolve("README.md")));
        assertTrue(Files.exists(appDir.resolve(".gitignore")));

        String dbGo = Files.readString(appDir.resolve("db.go"));
        assertTrue(dbGo.contains("PostgreSQL"));
    }
}
