package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RailsMetadataTest {

    @Test
    void testProjectTypes() {
        assertEquals(3, RailsMetadata.PROJECT_TYPES.size());
        assertTrue(RailsMetadata.PROJECT_TYPES.contains("Ruby on Rails"));
        assertTrue(RailsMetadata.PROJECT_TYPES.contains("Rails API"));
        assertTrue(RailsMetadata.PROJECT_TYPES.contains("Mountable Engine"));
    }

    @Test
    void testDatabaseOptions() {
        List<String> names = RailsMetadata.databaseDisplayNames();
        assertEquals(8, names.size());
        assertEquals("SQLite3", names.get(0));
        assertEquals("MySQL", names.get(1));
        assertEquals("PostgreSQL", names.get(2));
        assertEquals("Oracle", names.get(3));
        assertEquals("SQL Server", names.get(4));
        assertEquals("JDBC MySQL", names.get(5));
        assertEquals("JDBC SQLite3", names.get(6));
        assertEquals("JDBC PostgreSQL", names.get(7));

        RailsMetadata.DatabaseOption sqlite = RailsMetadata.findDatabase("SQLite3");
        assertEquals("sqlite3", sqlite.cliFlag());
        assertEquals("sqlite3", sqlite.gemName());

        RailsMetadata.DatabaseOption pg = RailsMetadata.findDatabase("PostgreSQL");
        assertEquals("postgresql", pg.cliFlag());
        assertEquals("pg", pg.gemName());

        RailsMetadata.DatabaseOption mysql = RailsMetadata.findDatabase("MySQL");
        assertEquals("mysql", mysql.cliFlag());
        assertEquals("mysql2", mysql.gemName());
    }

    @Test
    void testJsFrameworkOptions() {
        assertEquals(7, RailsMetadata.JS_FRAMEWORKS.size());
        assertEquals("Importmap", RailsMetadata.JS_FRAMEWORKS.get(0));
        assertEquals("Bun", RailsMetadata.JS_FRAMEWORKS.get(1));
        assertEquals("esbuild", RailsMetadata.JS_FRAMEWORKS.get(2));
        assertEquals("Webpack", RailsMetadata.JS_FRAMEWORKS.get(3));
        assertEquals("Rollup", RailsMetadata.JS_FRAMEWORKS.get(4));
        assertEquals("React", RailsMetadata.JS_FRAMEWORKS.get(5));
        assertEquals("Vue", RailsMetadata.JS_FRAMEWORKS.get(6));
    }

    @Test
    void testCompareVersions() {
        assertTrue(RailsMetadata.compareVersions("8.0.1", "7.2.2") > 0);
        assertTrue(RailsMetadata.compareVersions("7.1.0", "7.1.0") == 0);
        assertTrue(RailsMetadata.compareVersions("7.0.8", "7.1.0") < 0);
    }

    @Test
    void testGenerateGemfile() {
        RailsMetadata.DatabaseOption sqlite = RailsMetadata.findDatabase("SQLite3");
        String gemfile = RailsMetadata.generateGemfile("8.0.1", sqlite, false, true, "Importmap");
        assertTrue(gemfile.contains("gem \"rails\", \"~> 8.0.1\""));
        assertTrue(gemfile.contains("gem \"sqlite3\""));
        assertTrue(gemfile.contains("gem \"puma\""));
        assertTrue(gemfile.contains("gem \"importmap-rails\""));
        assertTrue(gemfile.contains("gem \"turbo-rails\""));
        assertTrue(gemfile.contains("gem \"stimulus-rails\""));

        String apiGemfile = RailsMetadata.generateGemfile("7.2.0", sqlite, true, false, "Importmap");
        assertTrue(apiGemfile.contains("gem \"rails\", \"~> 7.2.0\""));
        assertFalse(apiGemfile.contains("gem \"importmap-rails\""));
    }

    @Test
    void testGenerateDatabaseYml() {
        RailsMetadata.DatabaseOption sqlite = RailsMetadata.findDatabase("SQLite3");
        String sqliteYml = RailsMetadata.generateDatabaseYml("demo_app", sqlite);
        assertTrue(sqliteYml.contains("adapter: sqlite3"));
        assertTrue(sqliteYml.contains("storage/development.sqlite3"));

        RailsMetadata.DatabaseOption pg = RailsMetadata.findDatabase("PostgreSQL");
        String pgYml = RailsMetadata.generateDatabaseYml("demo_app", pg);
        assertTrue(pgYml.contains("adapter: postgresql"));
        assertTrue(pgYml.contains("database: demo_app_development"));
    }

    @Test
    void testGenerateApplicationRb() {
        String appRb = RailsMetadata.generateApplicationRb("my_rails_app", false);
        assertTrue(appRb.contains("module MyRailsApp"));
        assertTrue(appRb.contains("class Application < Rails::Application"));
        assertFalse(appRb.contains("config.api_only = true"));

        String apiAppRb = RailsMetadata.generateApplicationRb("my_api_app", true);
        assertTrue(apiAppRb.contains("config.api_only = true"));
    }

    @Test
    void testGenerateIdeaRailsIml() {
        String iml = RailsMetadata.generateIdeaRailsIml("demo_app");
        assertTrue(iml.contains("type=\"RUBY_MODULE\""));
        assertTrue(iml.contains("<facet type=\"RubyOnRails\" name=\"Ruby on Rails\">"));
        assertTrue(iml.contains("<excludeFolder url=\"file://$MODULE_DIR$/.bundle\" />"));
        assertTrue(iml.contains("<excludeFolder url=\"file://$MODULE_DIR$/tmp\" />"));
        assertTrue(iml.contains("<excludeFolder url=\"file://$MODULE_DIR$/log\" />"));
    }

    @Test
    void testScaffoldFullApp(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my_rails_app");
        Files.createDirectories(projectDir);

        RailsMetadata.scaffoldProject(projectDir, "my_rails_app", RailsMetadata.TYPE_FULL,
                "8.0.1", "SQLite3", true, "Importmap", s -> {});

        assertTrue(Files.exists(projectDir.resolve("Gemfile")));
        assertTrue(Files.exists(projectDir.resolve("Rakefile")));
        assertTrue(Files.exists(projectDir.resolve("config.ru")));
        assertTrue(Files.exists(projectDir.resolve("config/application.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/database.yml")));
        assertTrue(Files.exists(projectDir.resolve("config/routes.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/environments/development.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/environments/production.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/environments/test.rb")));
        assertTrue(Files.exists(projectDir.resolve("bin/rails")));
        assertTrue(Files.exists(projectDir.resolve("bin/setup")));
        assertTrue(Files.exists(projectDir.resolve("app/controllers/application_controller.rb")));
        assertTrue(Files.exists(projectDir.resolve("app/models/application_record.rb")));
        assertTrue(Files.exists(projectDir.resolve("app/views/layouts/application.html.erb")));
        assertTrue(Files.exists(projectDir.resolve("app/javascript/application.js")));
        assertTrue(Files.exists(projectDir.resolve("config/importmap.rb")));
    }

    @Test
    void testScaffoldApiApp(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my_api_app");
        Files.createDirectories(projectDir);

        RailsMetadata.scaffoldProject(projectDir, "my_api_app", RailsMetadata.TYPE_API,
                "8.0.1", "PostgreSQL", false, "Importmap", s -> {});

        assertTrue(Files.exists(projectDir.resolve("Gemfile")));
        assertTrue(Files.exists(projectDir.resolve("config/application.rb")));
        String appRb = Files.readString(projectDir.resolve("config/application.rb"));
        assertTrue(appRb.contains("config.api_only = true"));

        String controller = Files.readString(projectDir.resolve("app/controllers/application_controller.rb"));
        assertTrue(controller.contains("ActionController::API"));

        // API app does not have views or JS
        assertFalse(Files.exists(projectDir.resolve("app/views/layouts/application.html.erb")));
        assertFalse(Files.exists(projectDir.resolve("config/importmap.rb")));
    }

    @Test
    void testScaffoldMountableEngine(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my_engine");
        Files.createDirectories(projectDir);

        RailsMetadata.scaffoldProject(projectDir, "my_engine", RailsMetadata.TYPE_ENGINE,
                "8.0.1", "SQLite3", false, "Importmap", s -> {});

        assertTrue(Files.exists(projectDir.resolve("my_engine.gemspec")));
        assertTrue(Files.exists(projectDir.resolve("Gemfile")));
        assertTrue(Files.exists(projectDir.resolve("Rakefile")));
        assertTrue(Files.exists(projectDir.resolve("lib/my_engine.rb")));
        assertTrue(Files.exists(projectDir.resolve("lib/my_engine/version.rb")));
        assertTrue(Files.exists(projectDir.resolve("lib/my_engine/engine.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/routes.rb")));
        assertTrue(Files.exists(projectDir.resolve("app/controllers/my_engine/application_controller.rb")));

        String engineContent = Files.readString(projectDir.resolve("lib/my_engine/engine.rb"));
        assertTrue(engineContent.contains("class Engine < ::Rails::Engine"));
        assertTrue(engineContent.contains("isolate_namespace MyEngine"));
    }
}
