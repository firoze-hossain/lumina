package dev.lumina.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Service providing dynamic Ruby on Rails version discovery, database configurations,
 * project templates, and scaffolding matching IntelliJ IDEA / RubyMine.
 */
public final class RailsMetadata {

    public static final String NO_VERSION_LABEL = "[No version selected]";

    public static final String TYPE_FULL = "Ruby on Rails";
    public static final String TYPE_API = "Rails API";
    public static final String TYPE_ENGINE = "Mountable Engine";

    public static final List<String> PROJECT_TYPES = List.of(TYPE_FULL, TYPE_API, TYPE_ENGINE);

    public record DatabaseOption(
            String displayName, // e.g. "SQLite3", "MySQL"
            String cliFlag,      // e.g. "sqlite3", "mysql"
            String gemName,      // e.g. "sqlite3", "mysql2"
            String gemVersion,   // e.g. ">= 2.1", "~> 0.5"
            String adapterName   // e.g. "sqlite3", "mysql2"
    ) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    public static final List<DatabaseOption> DATABASE_OPTIONS = List.of(
            new DatabaseOption("SQLite3", "sqlite3", "sqlite3", ">= 2.1", "sqlite3"),
            new DatabaseOption("MySQL", "mysql", "mysql2", "~> 0.5", "mysql2"),
            new DatabaseOption("PostgreSQL", "postgresql", "pg", "~> 1.1", "postgresql"),
            new DatabaseOption("Oracle", "oracle", "ruby-oci8", "~> 2.2", "oracle_enhanced"),
            new DatabaseOption("SQL Server", "sqlserver", "activerecord-sqlserver-adapter", "~> 7.0", "sqlserver"),
            new DatabaseOption("JDBC MySQL", "jdbcmysql", "activerecord-jdbcmysql-adapter", "~> 70.0", "jdbcmysql"),
            new DatabaseOption("JDBC SQLite3", "jdbcsqlite3", "activerecord-jdbcsqlite3-adapter", "~> 70.0", "jdbcsqlite3"),
            new DatabaseOption("JDBC PostgreSQL", "jdbcpostgresql", "activerecord-jdbcpostgresql-adapter", "~> 70.0", "jdbcpostgresql")
    );

    public static final List<String> JS_FRAMEWORKS = List.of(
            "Importmap",
            "Bun",
            "esbuild",
            "Webpack",
            "Rollup",
            "React",
            "Vue"
    );

    private static volatile List<String> cachedRemoteVersions = null;

    private RailsMetadata() {
    }

    public static DatabaseOption findDatabase(String nameOrFlag) {
        if (nameOrFlag == null || nameOrFlag.isBlank()) {
            return DATABASE_OPTIONS.getFirst();
        }
        for (DatabaseOption opt : DATABASE_OPTIONS) {
            if (opt.displayName().equalsIgnoreCase(nameOrFlag) || opt.cliFlag().equalsIgnoreCase(nameOrFlag)) {
                return opt;
            }
        }
        return DATABASE_OPTIONS.getFirst();
    }

    /**
     * Dynamically probes installed Rails gems in the specified Ruby environment.
     */
    public static List<String> discoverInstalledRails(String rubyExecutable) {
        List<String> versions = new ArrayList<>();
        if (rubyExecutable == null || rubyExecutable.isBlank()) {
            return versions;
        }

        File f = new File(rubyExecutable);
        if (!f.exists() || !f.canExecute()) {
            return versions;
        }

        // 1. Probe via Ruby gem specification query
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    rubyExecutable,
                    "-e",
                    "puts Gem::Specification.find_all_by_name('rails').map(&:version).map(&:to_s).join('\n')"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+\\.\\d+(\\.\\d+)?.*") && !versions.contains(line)) {
                        versions.add(line);
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {
        }

        // 2. If empty, check `rails -v` beside ruby executable
        if (versions.isEmpty()) {
            File binDir = f.getParentFile();
            if (binDir != null) {
                File railsBin = new File(binDir, "rails");
                if (railsBin.exists() && railsBin.canExecute()) {
                    String ver = probeRailsVersion(railsBin.getAbsolutePath());
                    if (ver != null && !versions.contains(ver)) {
                        versions.add(ver);
                    }
                }
            }
        }

        // Sort versions descending (newest first)
        versions.sort((a, b) -> compareVersions(b, a));
        return versions;
    }

    /**
     * Probes `rails -v` output for its version.
     */
    public static String probeRailsVersion(String railsExecutable) {
        if (railsExecutable == null || railsExecutable.isBlank()) return null;
        try {
            ProcessBuilder pb = new ProcessBuilder(railsExecutable, "-v");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    Matcher m = Pattern.compile("Rails\\s+(\\d+\\.\\d+(\\.\\d+)?)").matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Asynchronously fetches known recent Rails versions from RubyGems API without blocking.
     */
    public static void fetchKnownRailsVersionsAsync(java.util.function.Consumer<List<String>> callback) {
        if (cachedRemoteVersions != null && !cachedRemoteVersions.isEmpty()) {
            callback.accept(cachedRemoteVersions);
            return;
        }

        Thread.ofVirtual().start(() -> {
            List<String> list = fetchKnownRailsVersionsFromRegistry();
            if (!list.isEmpty()) {
                cachedRemoteVersions = Collections.unmodifiableList(list);
            }
            callback.accept(cachedRemoteVersions != null ? cachedRemoteVersions : List.of());
        });
    }

    public static java.util.concurrent.CompletableFuture<List<String>> fetchKnownRailsVersionsAsync() {
        java.util.concurrent.CompletableFuture<List<String>> future = new java.util.concurrent.CompletableFuture<>();
        fetchKnownRailsVersionsAsync(future::complete);
        return future;
    }

    public static List<String> fetchKnownRailsVersionsFromRegistry() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://rubygems.org/api/v1/versions/rails.json"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
                List<String> versions = new ArrayList<>();
                for (JsonElement el : arr) {
                    if (el.isJsonObject() && el.getAsJsonObject().has("number")) {
                        String num = el.getAsJsonObject().get("number").getAsString();
                        // Ignore prereleases (alpha, beta, rc)
                        if (!num.contains("-") && !num.contains("a") && !num.contains("b") && !num.contains("rc")) {
                            versions.add(num);
                            if (versions.size() >= 15) break;
                        }
                    }
                }
                return versions;
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    public static int compareVersions(String v1, String v2) {
        String[] p1 = v1.split("[.-]");
        String[] p2 = v2.split("[.-]");
        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = 0;
            int n2 = 0;
            if (i < p1.length) {
                try { n1 = Integer.parseInt(p1[i]); } catch (NumberFormatException ignored) {}
            }
            if (i < p2.length) {
                try { n2 = Integer.parseInt(p2[i]); } catch (NumberFormatException ignored) {}
            }
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    // ---------------------------------------------------------------- Scaffolding Templates

    public static String toModuleName(String name) {
        return GemMetadata.toRubyModuleName(name);
    }

    /**
     * Generates Gemfile for Rails projects.
     */
    public static String generateGemfile(String railsVersion, DatabaseOption db, boolean isApi, boolean jsEnabled, String jsFramework) {
        String railsConstraint = (railsVersion != null && !railsVersion.isBlank())
                ? "\"~> " + railsVersion + "\""
                : "\">= 7.1.0\"";

        StringBuilder sb = new StringBuilder();
        sb.append("# frozen_string_literal: true\n\n");
        sb.append("source \"https://rubygems.org\"\n\n");
        sb.append("gem \"rails\", ").append(railsConstraint).append("\n");
        sb.append("gem \"").append(db.gemName()).append("\", \"").append(db.gemVersion()).append("\"\n");
        sb.append("gem \"puma\", \">= 5.0\"\n");
        sb.append("gem \"bootsnap\", require: false\n");
        sb.append("gem \"tzinfo-data\", platforms: %i[windows jruby]\n\n");

        if (!isApi && jsEnabled) {
            if ("importmap".equalsIgnoreCase(jsFramework)) {
                sb.append("gem \"importmap-rails\"\n");
                sb.append("gem \"turbo-rails\"\n");
                sb.append("gem \"stimulus-rails\"\n");
            } else {
                sb.append("gem \"jsbundling-rails\"\n");
                sb.append("gem \"turbo-rails\"\n");
                sb.append("gem \"stimulus-rails\"\n");
            }
        }

        sb.append("\ngroup :development, :test do\n");
        sb.append("  gem \"debug\", platforms: %i[mri windows], require: \"debug/precompile\"\n");
        sb.append("end\n\n");

        sb.append("group :development do\n");
        sb.append("  gem \"web-console\"\n");
        sb.append("end\n\n");

        sb.append("group :test do\n");
        sb.append("  gem \"capybara\"\n");
        sb.append("  gem \"selenium-webdriver\"\n");
        sb.append("end\n");

        return sb.toString();
    }

    /**
     * Generates config/database.yml matching the selected database.
     */
    public static String generateDatabaseYml(String appName, DatabaseOption db) {
        String cleanName = appName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (db.cliFlag().contains("sqlite")) {
            return """
                    default: &default
                      adapter: %s
                      pool: <%%= ENV.fetch("RAILS_MAX_THREADS") { 5 } %%>
                      timeout: 5000

                    development:
                      <<: *default
                      database: storage/development.sqlite3

                    test:
                      <<: *default
                      database: storage/test.sqlite3

                    production:
                      <<: *default
                      database: storage/production.sqlite3
                    """.formatted(db.adapterName());
        } else if (db.cliFlag().contains("mysql")) {
            return """
                    default: &default
                      adapter: %s
                      encoding: utf8mb4
                      pool: <%%= ENV.fetch("RAILS_MAX_THREADS") { 5 } %%>
                      username: root
                      password:
                      host: localhost

                    development:
                      <<: *default
                      database: %s_development

                    test:
                      <<: *default
                      database: %s_test

                    production:
                      <<: *default
                      database: %s_production
                      username: %s
                      password: <%%= ENV["%s_DATABASE_PASSWORD"] %%>
                    """.formatted(db.adapterName(), cleanName, cleanName, cleanName, cleanName, cleanName.toUpperCase());
        } else if (db.cliFlag().contains("postgres")) {
            return """
                    default: &default
                      adapter: %s
                      encoding: unicode
                      pool: <%%= ENV.fetch("RAILS_MAX_THREADS") { 5 } %%>

                    development:
                      <<: *default
                      database: %s_development

                    test:
                      <<: *default
                      database: %s_test

                    production:
                      <<: *default
                      database: %s_production
                      username: %s
                      password: <%%= ENV["%s_DATABASE_PASSWORD"] %%>
                    """.formatted(db.adapterName(), cleanName, cleanName, cleanName, cleanName, cleanName.toUpperCase());
        } else {
            return """
                    default: &default
                      adapter: %s
                      pool: <%%= ENV.fetch("RAILS_MAX_THREADS") { 5 } %%>

                    development:
                      <<: *default
                      database: %s_development

                    test:
                      <<: *default
                      database: %s_test

                    production:
                      <<: *default
                      database: %s_production
                    """.formatted(db.adapterName(), cleanName, cleanName, cleanName);
        }
    }

    /**
     * Generates config/application.rb.
     */
    public static String generateApplicationRb(String appName, boolean isApi) {
        String moduleName = toModuleName(appName);
        String apiLine = isApi ? "    config.api_only = true\n" : "";

        return """
                # frozen_string_literal: true

                require_relative "boot"

                require "rails/all"

                Bundler.require(*Rails.groups)

                module %s
                  class Application < Rails::Application
                    config.load_defaults Rails::VERSION::STRING.to_f
                %s  end
                end
                """.formatted(moduleName, apiLine);
    }

    /**
     * Generates config/routes.rb.
     */
    public static String generateRoutesRb() {
        return """
                # frozen_string_literal: true

                Rails.application.routes.draw do
                  # Define your application routes per the DSL in https://guides.rubyonrails.org/routing.html

                  # Reveal health status on /up that returns 200 if the app boots with no exceptions, otherwise 500.
                  # Can be used by load balancers and uptime monitors to verify that the app is live.
                  get "up" => "rails/health#show", as: :rails_health_check

                  # Defines the root path route ("/")
                  # root "posts#index"
                end
                """;
    }

    /**
     * Generates config/environments/development.rb.
     */
    public static String generateDevelopmentRb() {
        return """
                # frozen_string_literal: true

                require "active_support/core_ext/integer/time"

                Rails.application.configure do
                  config.enable_reloading = true
                  config.eager_load = false
                  config.consider_all_requests_local = true
                  config.server_timing = true
                  config.active_support.deprecation = :log
                  config.active_record.migration_error = :page_load
                  config.active_record.verbose_query_logs = true
                end
                """;
    }

    /**
     * Generates config/environments/production.rb.
     */
    public static String generateProductionRb() {
        return """
                # frozen_string_literal: true

                require "active_support/core_ext/integer/time"

                Rails.application.configure do
                  config.enable_reloading = false
                  config.eager_load = true
                  config.consider_all_requests_local = false
                  config.force_ssl = true
                  config.logger = ActiveSupport::Logger.new(STDOUT)
                  config.active_support.deprecation = :notify
                  config.active_record.dump_schema_after_migration = false
                end
                """;
    }

    /**
     * Generates config/environments/test.rb.
     */
    public static String generateTestRb() {
        return """
                # frozen_string_literal: true

                Rails.application.configure do
                  config.enable_reloading = false
                  config.eager_load = false
                  config.consider_all_requests_local = true
                  config.action_dispatch.show_exceptions = :rescuable
                  config.action_controller.allow_forgery_protection = false
                  config.active_support.deprecation = :stderr
                end
                """;
    }

    /**
     * Generates config/boot.rb.
     */
    public static String generateBootRb() {
        return """
                # frozen_string_literal: true

                ENV["BUNDLE_GEMFILE"] ||= File.expand_path("../Gemfile", __dir__)

                require "bundler/setup" # Set up gems listed in the Gemfile.
                require "bootsnap/setup" # Speed up boot time by caching expensive operations.
                """;
    }

    /**
     * Generates config/environment.rb.
     */
    public static String generateEnvironmentRb() {
        return """
                # frozen_string_literal: true

                # Load the Rails application.
                require_relative "application"

                # Initialize the Rails application.
                Rails.application.initialize!
                """;
    }

    /**
     * Generates config.ru.
     */
    public static String generateConfigRu() {
        return """
                # frozen_string_literal: true

                require_relative "config/environment"

                run Rails.application
                Rails.application.load_server
                """;
    }

    /**
     * Generates Rakefile.
     */
    public static String generateRakefile() {
        return """
                # frozen_string_literal: true

                require_relative "config/application"

                Rails.application.load_tasks
                """;
    }

    /**
     * Generates bin/rails.
     */
    public static String generateBinRails() {
        return """
                #!/usr/bin/env ruby
                # frozen_string_literal: true

                APP_PATH = File.expand_path("../config/application", __dir__)
                require_relative "../config/boot"
                require "rails/commands"
                """;
    }

    /**
     * Generates bin/setup.
     */
    public static String generateBinSetup() {
        return """
                #!/usr/bin/env ruby
                # frozen_string_literal: true

                require "fileutils"

                APP_ROOT = File.expand_path("..", __dir__)

                def system!(*args)
                  system(*args, exception: true)
                end

                FileUtils.chdir APP_ROOT do
                  puts "== Installing dependencies =="
                  system! "bundle install"

                  puts "\\n== Preparing database =="
                  system! "bin/rails db:prepare"

                  puts "\\n== Removing old logs and tempfiles =="
                  system! "bin/rails log:clear tmp:clear"
                end
                """;
    }

    /**
     * Generates app/controllers/application_controller.rb.
     */
    public static String generateApplicationController(boolean isApi) {
        if (isApi) {
            return """
                    # frozen_string_literal: true

                    class ApplicationController < ActionController::API
                    end
                    """;
        } else {
            return """
                    # frozen_string_literal: true

                    class ApplicationController < ActionController::Base
                    end
                    """;
        }
    }

    /**
     * Generates app/models/application_record.rb.
     */
    public static String generateApplicationRecord() {
        return """
                # frozen_string_literal: true

                class ApplicationRecord < ActiveRecord::Base
                  primary_abstract_class
                end
                """;
    }

    /**
     * Generates app/views/layouts/application.html.erb.
     */
    public static String generateApplicationLayout(String appName) {
        return """
                <!DOCTYPE html>
                <html>
                  <head>
                    <title>%s</title>
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <%%= csrf_meta_tags %%>
                    <%%= csp_meta_tag %%>

                    <%%= stylesheet_link_tag "application", "data-turbo-track": "reload" %%>
                    <%%= javascript_importmap_tags %%>
                  </head>

                  <body>
                    <%%= yield %%>
                  </body>
                </html>
                """.formatted(appName);
    }

    /**
     * Generates Engine gemspec for Mountable Engine projects.
     */
    public static String generateEngineGemspec(String engineName, DatabaseOption db) {
        String modName = toModuleName(engineName);
        return """
                # frozen_string_literal: true

                require_relative "lib/%s/version"

                Gem::Specification.new do |spec|
                  spec.name        = "%s"
                  spec.version     = %s::VERSION
                  spec.authors     = ["%s"]
                  spec.email       = ["TODO: Write your email"]
                  spec.homepage    = "https://example.com"
                  spec.summary     = "Summary of %s."
                  spec.description = "Description of %s."
                  spec.license     = "MIT"

                  spec.files = Dir["{app,config,db,lib}/**/*", "MIT-LICENSE", "Rakefile", "README.md"]

                  spec.add_dependency "rails", ">= 7.1.0"
                  spec.add_dependency "%s", "%s"
                end
                """.formatted(engineName, engineName, modName, System.getProperty("user.name", "TODO: Your Name"), engineName, engineName, db.gemName(), db.gemVersion());
    }

    /**
     * Generates lib/<engineName>/engine.rb for Mountable Engine.
     */
    public static String generateEngineClass(String engineName) {
        String modName = toModuleName(engineName);
        return """
                # frozen_string_literal: true

                module %s
                  class Engine < ::Rails::Engine
                    isolate_namespace %s
                  end
                end
                """.formatted(modName, modName);
    }

    public static List<String> databaseDisplayNames() {
        return DATABASE_OPTIONS.stream().map(DatabaseOption::displayName).toList();
    }

    /**
     * Generates standard .gitignore for Rails projects.
     */
    public static String generateGitignore() {
        return """
                /.bundle/
                /db/*.sqlite3
                /db/*.sqlite3-*
                /log/*
                /tmp/*
                !/log/.keep
                !/tmp/.keep
                /storage/*
                !/storage/.keep
                /public/assets
                .byebug_history
                .env
                .idea/
                """;
    }

    /**
     * Generates IntelliJ IDEA / RubyMine .iml module content with Rails facet.
     */
    public static String generateIdeaRailsIml(String moduleName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <module type="RUBY_MODULE" version="4">
                  <component name="FacetManager">
                    <facet type="RubyOnRails" name="Ruby on Rails">
                      <configuration>
                        <RAILS_FACET_CONFIG_ID NAME="RAILS_FACET_SUPPORT_REMOVED" VALUE="false" />
                        <RAILS_FACET_CONFIG_ID NAME="RAILS_ROOT" VALUE="$MODULE_DIR$" />
                      </configuration>
                    </facet>
                  </component>
                  <component name="NewModuleRootManager">
                    <content url="file://$MODULE_DIR$">
                      <sourceFolder url="file://$MODULE_DIR$/features" isTestSource="true" />
                      <sourceFolder url="file://$MODULE_DIR$/spec" isTestSource="true" />
                      <sourceFolder url="file://$MODULE_DIR$/test" isTestSource="true" />
                      <excludeFolder url="file://$MODULE_DIR$/.bundle" />
                      <excludeFolder url="file://$MODULE_DIR$/components" />
                      <excludeFolder url="file://$MODULE_DIR$/log" />
                      <excludeFolder url="file://$MODULE_DIR$/public/packs" />
                      <excludeFolder url="file://$MODULE_DIR$/public/assets" />
                      <excludeFolder url="file://$MODULE_DIR$/tmp" />
                      <excludeFolder url="file://$MODULE_DIR$/vendor/bundle" />
                      <excludeFolder url="file://$MODULE_DIR$/vendor/cache" />
                    </content>
                    <orderEntry type="inheritedJdk" />
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                  <component name="RModuleSettingsStorage">
                    <LOAD_PATH number="0" />
                    <I18N_FOLDERS number="1" string0="$MODULE_DIR$/config/locales" />
                  </component>
                </module>
                """;
    }

    /**
     * Scaffolds the entire project structure for Ruby on Rails, Rails API, or Mountable Engine.
     */
    public static void scaffoldProject(java.nio.file.Path dir, String projectName, String railsType,
                                       String railsVer, String dbName, boolean jsEnabled,
                                       String jsFramework, java.util.function.Consumer<String> log) throws java.io.IOException {
        DatabaseOption db = findDatabase(dbName);
        boolean isEngine = TYPE_ENGINE.equalsIgnoreCase(railsType);
        boolean isApi = TYPE_API.equalsIgnoreCase(railsType);

        if (isEngine) {
            java.nio.file.Path libDir = dir.resolve("lib");
            java.nio.file.Path engineLibDir = libDir.resolve(projectName);
            java.nio.file.Path appControllers = dir.resolve("app").resolve("controllers").resolve(projectName);
            java.nio.file.Path configDir = dir.resolve("config");
            java.nio.file.Files.createDirectories(engineLibDir);
            java.nio.file.Files.createDirectories(appControllers);
            java.nio.file.Files.createDirectories(configDir);

            String modName = toModuleName(projectName);

            java.nio.file.Files.writeString(dir.resolve(projectName + ".gemspec"), generateEngineGemspec(projectName, db));
            java.nio.file.Files.writeString(dir.resolve("Gemfile"), """
                    # frozen_string_literal: true

                    source "https://rubygems.org"

                    gemspec

                    gem "rails", ">= 7.1.0"
                    gem "%s", "%s"
                    """.formatted(db.gemName(), db.gemVersion()));

            java.nio.file.Files.writeString(dir.resolve("Rakefile"), """
                    # frozen_string_literal: true

                    require "bundler/gem_tasks"
                    require "rake/testtask"

                    Rake::TestTask.new(:test) do |t|
                      t.libs << "test"
                      t.pattern = "test/**/*_test.rb"
                    end

                    task default: :test
                    """);

            java.nio.file.Files.writeString(libDir.resolve(projectName + ".rb"), """
                    # frozen_string_literal: true

                    require "%s/version"
                    require "%s/engine"

                    module %s
                      # Engine entrypoint
                    end
                    """.formatted(projectName, projectName, modName));

            java.nio.file.Files.writeString(engineLibDir.resolve("version.rb"), """
                    # frozen_string_literal: true

                    module %s
                      VERSION = "0.1.0"
                    end
                    """.formatted(modName));

            java.nio.file.Files.writeString(engineLibDir.resolve("engine.rb"), generateEngineClass(projectName));

            java.nio.file.Files.writeString(configDir.resolve("routes.rb"), """
                    # frozen_string_literal: true

                    %s::Engine.routes.draw do
                    end
                    """.formatted(modName));

            java.nio.file.Files.writeString(appControllers.resolve("application_controller.rb"), """
                    # frozen_string_literal: true

                    module %s
                      class ApplicationController < ActionController::Base
                      end
                    end
                    """.formatted(modName));

            java.nio.file.Files.writeString(dir.resolve("MIT-LICENSE"), "Copyright (c) " + java.time.Year.now() + " "
                    + System.getProperty("user.name", "TODO") + "\n\nMIT License\n");
            java.nio.file.Files.writeString(dir.resolve("README.md"), "# " + projectName + "\n\nMountable Engine generated by Lumina.\n");
            java.nio.file.Files.writeString(dir.resolve(".gitignore"), generateGitignore());
            log.accept("Scaffolded Mountable Engine: " + projectName);
            return;
        }

        // Full application or API
        java.nio.file.Path appControllers = dir.resolve("app").resolve("controllers");
        java.nio.file.Path appModels = dir.resolve("app").resolve("models");
        java.nio.file.Path binDir = dir.resolve("bin");
        java.nio.file.Path configDir = dir.resolve("config");
        java.nio.file.Path configEnvs = configDir.resolve("environments");
        java.nio.file.Path dbDir = dir.resolve("db");

        java.nio.file.Files.createDirectories(appControllers);
        java.nio.file.Files.createDirectories(appModels);
        java.nio.file.Files.createDirectories(binDir);
        java.nio.file.Files.createDirectories(configEnvs);
        java.nio.file.Files.createDirectories(dbDir);
        java.nio.file.Files.createDirectories(dir.resolve("log"));
        java.nio.file.Files.createDirectories(dir.resolve("tmp"));
        java.nio.file.Files.createDirectories(dir.resolve("storage"));

        java.nio.file.Files.writeString(dir.resolve("Gemfile"), generateGemfile(railsVer, db, isApi, jsEnabled, jsFramework));
        java.nio.file.Files.writeString(configDir.resolve("database.yml"), generateDatabaseYml(projectName, db));
        java.nio.file.Files.writeString(configDir.resolve("application.rb"), generateApplicationRb(projectName, isApi));
        java.nio.file.Files.writeString(configDir.resolve("routes.rb"), generateRoutesRb());
        java.nio.file.Files.writeString(configDir.resolve("boot.rb"), generateBootRb());
        java.nio.file.Files.writeString(configDir.resolve("environment.rb"), generateEnvironmentRb());
        java.nio.file.Files.writeString(configEnvs.resolve("development.rb"), generateDevelopmentRb());
        java.nio.file.Files.writeString(configEnvs.resolve("production.rb"), generateProductionRb());
        java.nio.file.Files.writeString(configEnvs.resolve("test.rb"), generateTestRb());

        java.nio.file.Files.writeString(dir.resolve("config.ru"), generateConfigRu());
        java.nio.file.Files.writeString(dir.resolve("Rakefile"), generateRakefile());

        java.nio.file.Path binRails = binDir.resolve("rails");
        java.nio.file.Path binSetup = binDir.resolve("setup");
        java.nio.file.Files.writeString(binRails, generateBinRails());
        java.nio.file.Files.writeString(binSetup, generateBinSetup());
        try {
            binRails.toFile().setExecutable(true, false);
            binSetup.toFile().setExecutable(true, false);
        } catch (Exception ignored) {}

        java.nio.file.Files.writeString(appControllers.resolve("application_controller.rb"), generateApplicationController(isApi));
        java.nio.file.Files.writeString(appModels.resolve("application_record.rb"), generateApplicationRecord());

        if (!isApi) {
            java.nio.file.Path layoutsDir = dir.resolve("app").resolve("views").resolve("layouts");
            java.nio.file.Files.createDirectories(layoutsDir);
            java.nio.file.Files.writeString(layoutsDir.resolve("application.html.erb"), generateApplicationLayout(projectName));

            if (jsEnabled) {
                java.nio.file.Path jsDir = dir.resolve("app").resolve("javascript");
                java.nio.file.Files.createDirectories(jsDir);
                if ("importmap".equalsIgnoreCase(jsFramework)) {
                    java.nio.file.Files.writeString(configDir.resolve("importmap.rb"), """
                            # Pin npm packages by running ./bin/importmap

                            pin "application"
                            pin "@hotwired/turbo-rails", to: "turbo.min.js"
                            pin "@hotwired/stimulus", to: "stimulus.min.js"
                            """);
                    java.nio.file.Files.writeString(jsDir.resolve("application.js"), """
                            // Configure your import map in config/importmap.rb. Read more: https://github.com/rails/importmap-rails
                            import "@hotwired/turbo-rails"
                            import "controllers"
                            """);
                } else {
                    java.nio.file.Files.writeString(jsDir.resolve("application.js"), """
                            // Entry point for the build script in your package.json
                            import "@hotwired/turbo-rails"
                            import "./controllers"
                            """);
                }
            }
        }

        java.nio.file.Files.writeString(dir.resolve("README.md"), "# " + projectName + "\n\nRuby on Rails application generated by Lumina.\n");
        java.nio.file.Files.writeString(dir.resolve(".gitignore"), generateGitignore());
        log.accept("Scaffolded " + (isApi ? "Rails API" : "Ruby on Rails") + ": " + projectName);
    }
}
