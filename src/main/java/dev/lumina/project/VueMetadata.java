package dev.lumina.project;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata and dynamic discovery for Vue.js project generation,
 * matching IntelliJ IDEA's Vue.js integration, version resolution from
 * npm registry for {@code create-vue} and {@code @vue/cli}, and scaffolding.
 */
public final class VueMetadata {

    public static final String RUNNER_CREATE_VUE = "npx create-vue";
    public static final String RUNNER_VUE_CLI = "npx --package @vue/cli vue";
    public static final String ACTION_SELECT = "Select...";

    public static final String DEFAULT_CREATE_VUE_VERSION = "3.24.0";
    public static final String DEFAULT_VUE_CLI_VERSION = "5.0.9";

    private static final Map<String, String> CACHED_VERSIONS = new ConcurrentHashMap<>();

    static {
        CACHED_VERSIONS.put("create-vue", DEFAULT_CREATE_VUE_VERSION);
        CACHED_VERSIONS.put("@vue/cli", DEFAULT_VUE_CLI_VERSION);
    }

    public record VueCliEntry(
            String runnerPrefix,
            String path,
            String version,
            boolean isAction
    ) {
        public String formatDisplay() {
            if (isAction) {
                return runnerPrefix;
            }
            String prefix = (path != null && !path.isBlank()) ? path : runnerPrefix;
            int totalLen = 60;
            int padding = Math.max(2, totalLen - prefix.length() - (version != null ? version.length() : 0));
            return prefix + " ".repeat(padding) + (version != null ? version : "");
        }

        @Override
        public String toString() {
            return formatDisplay();
        }
    }

    private VueMetadata() {}

    /**
     * Formats an npx runner display string matching IntelliJ IDEA Image 1 & 3.
     */
    public static String formatCliDisplay(String prefix, String version) {
        String ver = (version == null || version.isBlank()) ? DEFAULT_CREATE_VUE_VERSION : version.trim();
        int totalLen = 60;
        int padding = Math.max(2, totalLen - prefix.length() - ver.length());
        return prefix + " ".repeat(padding) + ver;
    }

    /**
     * Extracts version string from a formatted display or raw version.
     */
    public static String parseVersionFromDisplay(String display) {
        if (display == null || display.isBlank() || ACTION_SELECT.equals(display)) {
            return DEFAULT_CREATE_VUE_VERSION;
        }
        String trimmed = display.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            if (last.matches("\\d+(\\.\\d+)*.*")) {
                return last;
            }
        }
        return DEFAULT_CREATE_VUE_VERSION;
    }

    /**
     * Extracts runner name/command from a formatted display string.
     */
    public static String parseRunnerFromDisplay(String display) {
        if (display == null || display.isBlank()) {
            return RUNNER_CREATE_VUE;
        }
        if (display.startsWith(RUNNER_CREATE_VUE)) {
            return RUNNER_CREATE_VUE;
        }
        if (display.startsWith(RUNNER_VUE_CLI)) {
            return RUNNER_VUE_CLI;
        }
        String[] parts = display.trim().split("\\s{2,}");
        return parts.length > 0 ? parts[0] : display.trim();
    }

    /**
     * Discovers installed or available Vue CLI entries matching IntelliJ IDEA Image 3:
     * 1) npx create-vue                                3.24.0
     * 2) npx --package @vue/cli vue                    5.0.9
     * 3) Select...
     */
    public static List<VueCliEntry> getCliEntries() {
        List<VueCliEntry> entries = new ArrayList<>();
        entries.add(new VueCliEntry(RUNNER_CREATE_VUE, null, CACHED_VERSIONS.getOrDefault("create-vue", DEFAULT_CREATE_VUE_VERSION), false));
        entries.add(new VueCliEntry(RUNNER_VUE_CLI, null, CACHED_VERSIONS.getOrDefault("@vue/cli", DEFAULT_VUE_CLI_VERSION), false));
        entries.add(new VueCliEntry(ACTION_SELECT, null, null, true));
        return entries;
    }

    /**
     * Asynchronously fetches latest package versions from npm registry and invokes callback.
     */
    public static void fetchVersionsAsync(Consumer<List<VueCliEntry>> onUpdated) {
        CompletableFuture.runAsync(() -> {
            boolean updated = false;
            String createVueVer = fetchLatestNpmVersion("create-vue", DEFAULT_CREATE_VUE_VERSION);
            if (!createVueVer.equals(CACHED_VERSIONS.get("create-vue"))) {
                CACHED_VERSIONS.put("create-vue", createVueVer);
                updated = true;
            }

            String vueCliVer = fetchLatestNpmVersion("@vue/cli", DEFAULT_VUE_CLI_VERSION);
            if (!vueCliVer.equals(CACHED_VERSIONS.get("@vue/cli"))) {
                CACHED_VERSIONS.put("@vue/cli", vueCliVer);
                updated = true;
            }

            if (updated && onUpdated != null) {
                onUpdated.accept(getCliEntries());
            }
        });
    }

    /**
     * Resolves the latest version of a package from the npm registry.
     */
    public static String fetchLatestNpmVersion(String packageName, String fallback) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://registry.npmjs.org/" + packageName + "/latest"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Pattern p = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
                Matcher m = p.matcher(resp.body());
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    /**
     * Scaffolds a Vue.js project from a ProjectSpec.
     */
    public static void scaffoldProject(ProjectSpec spec, Path dir, Consumer<String> log) throws IOException {
        String cli = spec.safeVueCliPackage();
        String ver = parseVersionFromDisplay(cli);
        String runner = parseRunnerFromDisplay(cli);
        scaffoldProject(
                dir,
                spec.name(),
                spec.safeVueNodeInterpreter(),
                runner,
                ver,
                spec.safeVueDefaultSetup(),
                log
        );
    }

    /**
     * Scaffolds a complete Vue.js project matching official Vite + Vue 3 tooling.
     */
    public static void scaffoldProject(
            Path projectDir,
            String projectName,
            String nodePath,
            String cliRunner,
            String cliVersion,
            boolean defaultSetup,
            Consumer<String> log
    ) throws IOException {
        String cleanName = projectName != null && !projectName.isBlank() ? projectName.trim() : "vue-app";
        String pkgName = cleanName.toLowerCase().replaceAll("[^a-z0-9_-]", "-");

        log.accept("Creating Vue.js application: " + cleanName + " in " + projectDir + " …");
        if (nodePath != null && !nodePath.isBlank()) {
            log.accept("Node runtime: " + nodePath);
        }
        log.accept("Vue CLI: " + cliRunner + " (" + (cliVersion != null ? cliVersion : DEFAULT_CREATE_VUE_VERSION) + ")");
        log.accept("Default project setup: " + defaultSetup);

        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("public"));
        Files.createDirectories(projectDir.resolve("src/assets"));
        Files.createDirectories(projectDir.resolve("src/components"));

        boolean isVueCli = cliRunner != null && cliRunner.contains("@vue/cli");

        // 1. package.json
        if (isVueCli) {
            Files.writeString(projectDir.resolve("package.json"), """
                    {
                      "name": "%s",
                      "version": "0.1.0",
                      "private": true,
                      "scripts": {
                        "serve": "vue-cli-service serve",
                        "build": "vue-cli-service build"
                      },
                      "dependencies": {
                        "vue": "^3.5.13"
                      },
                      "devDependencies": {
                        "@vue/cli-service": "~5.0.8"
                      }
                    }
                    """.formatted(pkgName));
        } else {
            Files.writeString(projectDir.resolve("package.json"), """
                    {
                      "name": "%s",
                      "version": "0.0.0",
                      "private": true,
                      "type": "module",
                      "scripts": {
                        "dev": "vite",
                        "build": "vite build",
                        "preview": "vite preview"
                      },
                      "dependencies": {
                        "vue": "^3.5.13"
                      },
                      "devDependencies": {
                        "@vitejs/plugin-vue": "^5.2.1",
                        "vite": "^6.1.0"
                      }
                    }
                    """.formatted(pkgName));

            // vite.config.js
            Files.writeString(projectDir.resolve("vite.config.js"), """
                    import { fileURLToPath, URL } from 'node:url'

                    import { defineConfig } from 'vite'
                    import vue from '@vitejs/plugin-vue'

                    // https://vite.dev/config/
                    export default defineConfig({
                      plugins: [
                        vue(),
                      ],
                      resolve: {
                        alias: {
                          '@': fileURLToPath(new URL('./src', import.meta.url))
                        },
                      },
                    })
                    """);
        }

        // 2. index.html
        Files.writeString(projectDir.resolve("index.html"), """
                <!DOCTYPE html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8">
                    <link rel="icon" href="/favicon.ico">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="app"></div>
                    <script type="module" src="/src/main.js"></script>
                  </body>
                </html>
                """.formatted(cleanName));

        // 3. src/main.js
        Files.writeString(projectDir.resolve("src/main.js"), """
                import './assets/main.css'

                import { createApp } from 'vue'
                import App from './App.vue'

                createApp(App).mount('#app')
                """);

        // 4. src/App.vue
        Files.writeString(projectDir.resolve("src/App.vue"), """
                <script setup>
                import HelloWorld from './components/HelloWorld.vue'
                </script>

                <template>
                  <header>
                    <div class="wrapper">
                      <HelloWorld msg="Welcome to Your Vue.js App" />
                    </div>
                  </header>

                  <main>
                    <p class="intro">
                      Recommended IDE Setup:
                      <a href="https://lumina.dev" target="_blank" rel="noopener">Lumina IDE</a>.
                    </p>
                  </main>
                </template>

                <style scoped>
                header {
                  line-height: 1.5;
                }

                .intro {
                  font-size: 1.1rem;
                  color: #42b883;
                  text-align: center;
                  margin-top: 1rem;
                }
                </style>
                """);

        // 5. src/components/HelloWorld.vue
        Files.writeString(projectDir.resolve("src/components/HelloWorld.vue"), """
                <script setup>
                defineProps({
                  msg: {
                    type: String,
                    required: true,
                  },
                })
                </script>

                <template>
                  <div class="greetings">
                    <h1 class="green">{{ msg }}</h1>
                    <h3>
                      You’ve successfully created a project with
                      <a href="https://vuejs.org/" target="_blank" rel="noopener">Vue 3</a>.
                    </h3>
                  </div>
                </template>

                <style scoped>
                h1 {
                  font-weight: 500;
                  font-size: 2.6rem;
                  position: relative;
                  top: -10px;
                }

                h3 {
                  font-size: 1.2rem;
                }

                .greetings h1,
                .greetings h3 {
                  text-align: center;
                }

                .green {
                  color: #42b883;
                }
                </style>
                """);

        // 6. CSS files
        Files.writeString(projectDir.resolve("src/assets/base.css"), """
                :root {
                  --vt-c-white: #ffffff;
                  --vt-c-white-soft: #f8f8f8;
                  --vt-c-white-mute: #f2f2f2;
                  --vt-c-black: #181818;
                  --vt-c-black-soft: #222222;
                  --vt-c-black-mute: #282828;
                  --vt-c-indigo: #2c3e50;
                  --vt-c-divider-light-1: rgba(60, 60, 60, 0.29);
                  --vt-c-divider-light-2: rgba(60, 60, 60, 0.12);
                  --vt-c-divider-dark-1: rgba(84, 84, 84, 0.65);
                  --vt-c-divider-dark-2: rgba(84, 84, 84, 0.48);
                  --vt-c-text-light-1: var(--vt-c-indigo);
                  --vt-c-text-light-2: rgba(60, 60, 60, 0.66);
                  --vt-c-text-dark-1: var(--vt-c-white);
                  --vt-c-text-dark-2: rgba(235, 235, 235, 0.64);
                }

                *,
                *::before,
                *::after {
                  box-sizing: border-box;
                  margin: 0;
                  font-weight: normal;
                }

                body {
                  min-height: 100vh;
                  color: var(--color-text);
                  background: var(--color-background);
                  transition:
                    color 0.5s,
                    background-color 0.5s;
                  line-height: 1.6;
                  font-family:
                    Inter,
                    -apple-system,
                    BlinkMacSystemFont,
                    'Segoe UI',
                    Roboto,
                    Oxygen,
                    Ubuntu,
                    Cantarell,
                    'Fira Sans',
                    'Droid Sans',
                    'Helvetica Neue',
                    sans-serif;
                  font-size: 15px;
                  text-rendering: optimizeLegibility;
                  -webkit-font-smoothing: antialiased;
                  -moz-osx-font-smoothing: grayscale;
                }
                """);

        Files.writeString(projectDir.resolve("src/assets/main.css"), """
                @import './base.css';

                #app {
                  max-width: 1280px;
                  margin: 0 auto;
                  padding: 2rem;
                  font-weight: normal;
                }

                a,
                .green {
                  text-decoration: none;
                  color: hsla(160, 100%, 37%, 1);
                  transition: 0.4s;
                  padding: 3px;
                }

                @media (hover: hover) {
                  a:hover {
                    background-color: hsla(160, 100%, 37%, 0.2);
                  }
                }
                """);

        // 7. .gitignore
        Files.writeString(projectDir.resolve(".gitignore"), """
                node_modules
                .pnp
                .pnp.js
                dist
                local
                .env.local
                .env.*.local
                npm-debug.log*
                yarn-debug.log*
                yarn-error.log*
                pnpm-debug.log*
                .DS_Store
                .idea
                *.suo
                *.ntvs*
                *.njsproj
                *.sln
                *.sw?
                """);

        // 8. README.md
        Files.writeString(projectDir.resolve("README.md"), """
                # %s

                This template helps get you started developing with Vue 3 in Vite.

                ## Recommended IDE Setup

                [Lumina IDE](https://lumina.dev/)

                ## Project Setup

                ```sh
                npm install
                ```

                ### Compile and Hot-Reload for Development

                ```sh
                npm run dev
                ```

                ### Compile and Minify for Production

                ```sh
                npm run build
                ```
                """.formatted(cleanName));

        // 9. IntelliJ IDEA project workspace files (.idea/)
        Path ideaDir = projectDir.resolve(".idea");
        Files.createDirectories(ideaDir);
        Files.writeString(ideaDir.resolve("modules.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectModuleManager">
                    <modules>
                      <module fileurl="file://$PROJECT_DIR$/.idea/%s.iml" filepath="$PROJECT_DIR$/.idea/%s.iml" />
                    </modules>
                  </component>
                </project>
                """.formatted(cleanName, cleanName));

        Files.writeString(ideaDir.resolve(cleanName + ".iml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <module type="WEB_MODULE" version="4">
                  <component name="NewModuleRootManager" inherit-compiler-output="true">
                    <exclude-output />
                    <content url="file://$PROJECT_DIR$">
                      <excludeFolder url="file://$PROJECT_DIR$/dist" />
                      <excludeFolder url="file://$PROJECT_DIR$/node_modules" />
                    </content>
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                </module>
                """);

        Files.write(projectDir.resolve("public/favicon.ico"), new byte[0]);

        Files.writeString(ideaDir.resolve("workspace.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectRootManager" />
                </project>
                """);

        Files.writeString(ideaDir.resolve("misc.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="JavaScriptSettings">
                    <option name="languageLevel" value="ES6" />
                  </component>
                </project>
                """);

        log.accept("Configured Vue.js application: " + cleanName);
    }
}
