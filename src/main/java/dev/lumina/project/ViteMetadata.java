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
 * Metadata and dynamic discovery for Vite project generation,
 * matching IntelliJ IDEA's Vite integration, dynamic npm registry version resolution
 * for {@code create-vite}, templates (Vanilla, React, Vue, Preact, Lit, Svelte, Solid, Qwik),
 * TypeScript support, and project scaffolding.
 */
public final class ViteMetadata {

    public static final String RUNNER_CREATE_VITE = "npx create-vite";
    public static final String ACTION_SELECT = "Select...";

    public static final String DEFAULT_CREATE_VITE_VERSION = "9.2.1";
    public static final String DEFAULT_TEMPLATE = "Vanilla";

    public static final List<String> TEMPLATES = List.of(
            "Vanilla",
            "React",
            "Vue",
            "Preact",
            "Lit",
            "Svelte",
            "Solid",
            "Qwik"
    );

    public static List<String> getTemplates() {
        return TEMPLATES;
    }

    private static final Map<String, String> CACHED_VERSIONS = new ConcurrentHashMap<>();

    static {
        CACHED_VERSIONS.put("create-vite", DEFAULT_CREATE_VITE_VERSION);
    }

    public record ViteCliEntry(
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

    private ViteMetadata() {}

    /**
     * Formats an npx runner display string matching IntelliJ IDEA Image 1 & 3.
     */
    public static String formatCliDisplay(String prefix, String version) {
        String ver = (version == null || version.isBlank()) ? DEFAULT_CREATE_VITE_VERSION : version.trim();
        int totalLen = 60;
        int padding = Math.max(2, totalLen - prefix.length() - ver.length());
        return prefix + " ".repeat(padding) + ver;
    }

    /**
     * Extracts version string from a formatted display or raw version.
     */
    public static String parseVersionFromDisplay(String display) {
        if (display == null || display.isBlank() || ACTION_SELECT.equals(display)) {
            return DEFAULT_CREATE_VITE_VERSION;
        }
        String trimmed = display.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            if (last.matches("\\d+(\\.\\d+)*.*")) {
                return last;
            }
        }
        return DEFAULT_CREATE_VITE_VERSION;
    }

    /**
     * Extracts runner name/command from a formatted display string.
     */
    public static String parseRunnerFromDisplay(String display) {
        if (display == null || display.isBlank()) {
            return RUNNER_CREATE_VITE;
        }
        if (display.startsWith(RUNNER_CREATE_VITE)) {
            return RUNNER_CREATE_VITE;
        }
        String[] parts = display.trim().split("\\s{2,}");
        return parts.length > 0 ? parts[0] : display.trim();
    }

    /**
     * Discovers available Vite CLI entries matching IntelliJ IDEA Image 3:
     * 1) npx create-vite                                     9.2.1
     * 2) Select...
     */
    public static List<ViteCliEntry> getCliEntries() {
        List<ViteCliEntry> entries = new ArrayList<>();
        entries.add(new ViteCliEntry(RUNNER_CREATE_VITE, null, CACHED_VERSIONS.getOrDefault("create-vite", DEFAULT_CREATE_VITE_VERSION), false));
        entries.add(new ViteCliEntry(ACTION_SELECT, null, null, true));
        return entries;
    }

    /**
     * Asynchronously fetches latest package version from npm registry and invokes callback.
     */
    public static void fetchVersionsAsync(Consumer<List<ViteCliEntry>> onUpdated) {
        CompletableFuture.runAsync(() -> {
            String latestVer = fetchLatestNpmVersion("create-vite", DEFAULT_CREATE_VITE_VERSION);
            if (!latestVer.equals(CACHED_VERSIONS.get("create-vite"))) {
                CACHED_VERSIONS.put("create-vite", latestVer);
                if (onUpdated != null) {
                    onUpdated.accept(getCliEntries());
                }
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
     * Scaffolds a Vite project from a ProjectSpec.
     */
    public static void scaffoldProject(ProjectSpec spec, Path dir, Consumer<String> log) throws IOException {
        String cli = spec.safeViteCliPackage();
        String ver = parseVersionFromDisplay(cli);
        String runner = parseRunnerFromDisplay(cli);
        scaffoldProject(
                dir,
                spec.name(),
                spec.safeViteNodeInterpreter(),
                runner,
                ver,
                spec.safeViteTemplate(),
                spec.safeViteTypeScript(),
                log
        );
    }

    /**
     * Scaffolds a complete Vite project matching official create-vite templates.
     */
    public static void scaffoldProject(
            Path projectDir,
            String projectName,
            String nodePath,
            String cliRunner,
            String cliVersion,
            String templateName,
            boolean useTypeScript,
            Consumer<String> log
    ) throws IOException {
        String cleanName = projectName != null && !projectName.isBlank() ? projectName.trim() : "vite-app";
        String pkgName = cleanName.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        String template = templateName != null && !templateName.isBlank() ? templateName.trim() : DEFAULT_TEMPLATE;

        log.accept("Creating Vite application: " + cleanName + " (" + template + (useTypeScript ? " + TypeScript" : "") + ") in " + projectDir + " …");
        if (nodePath != null && !nodePath.isBlank()) {
            log.accept("Node runtime: " + nodePath);
        }
        log.accept("Vite: " + cliRunner + " (" + (cliVersion != null ? cliVersion : DEFAULT_CREATE_VITE_VERSION) + ")");
        log.accept("Template: " + template);
        log.accept("TypeScript: " + useTypeScript);

        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("public"));
        Files.createDirectories(projectDir.resolve("src"));

        // Common public asset: vite.svg
        Files.writeString(projectDir.resolve("public/vite.svg"), """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img" class="iconify iconify--logos" width="31.88" height="32" preserveAspectRatio="xMidYMid meet" viewBox="0 0 256 257"><defs><linearGradient id="IconifyId1813088fe1fbc01fb466" x1="-.828%" x2="57.636%" y1="7.652%" y2="78.411%"><stop offset="0%" stop-color="#41D1FF"></stop><stop offset="100%" stop-color="#BD34FE"></stop></linearGradient><linearGradient id="IconifyId1813088fe1fbc01fb467" x1="43.376%" x2="50.316%" y1="2.242%" y2="89.03%"><stop offset="0%" stop-color="#FFEA83"></stop><stop offset="8.333%" stop-color="#FFDD35"></stop><stop offset="100%" stop-color="#FFA800"></stop></linearGradient></defs><path fill="url(#IconifyId1813088fe1fbc01fb466)" d="M255.153 37.938L134.897 252.976c-2.483 4.44-8.862 4.466-11.382.048L.875 37.958c-2.746-4.814 1.371-10.646 6.827-9.67l120.385 21.517a6.537 6.537 0 0 0 2.322-.004l117.867-21.483c5.438-.991 9.574 4.796 6.877 9.62Z"></path><path fill="url(#IconifyId1813088fe1fbc01fb467)" d="M185.432.063L96.44 17.501a3.268 3.268 0 0 0-2.634 3.014l-5.474 92.456a3.268 3.268 0 0 0 3.997 3.378l24.777-5.718c2.318-.535 4.413 1.507 3.936 3.838l-7.361 36.047c-.495 2.426 1.782 4.5 4.151 3.78l15.304-4.649c2.372-.72 4.652 1.36 4.15 3.788l-11.698 56.621c-.732 3.542 3.979 5.473 5.943 2.437l1.313-2.028l72.516-144.72c1.215-2.423-.88-5.186-3.54-4.672l-25.505 4.922c-2.396.462-4.435-1.77-3.759-4.114l16.646-57.705c.677-2.35-1.37-4.583-3.769-4.113Z"></path></svg>
                """);

        // .gitignore
        Files.writeString(projectDir.resolve(".gitignore"), """
                # Logs
                logs
                *.log
                npm-debug.log*
                yarn-debug.log*
                yarn-error.log*
                pnpm-debug.log*
                lerna-debug.log*

                node_modules
                dist
                dist-ssr
                *.local

                # Editor directories and files
                .vscode/*
                !.vscode/extensions.json
                .idea
                .DS_Store
                *.suo
                *.ntvs*
                *.njsproj
                *.sln
                *.sw?
                """);

        // README.md
        Files.writeString(projectDir.resolve("README.md"), """
                # %s

                This project was created with Vite template `%s`%s in Lumina IDE.

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
                """.formatted(cleanName, template, useTypeScript ? " (TypeScript)" : ""));

        if (useTypeScript) {
            Files.writeString(projectDir.resolve("tsconfig.json"), """
                    {
                      "compilerOptions": {
                        "target": "ES2020",
                        "useDefineForClassFields": true,
                        "module": "ESNext",
                        "lib": ["ES2020", "DOM", "DOM.Iterable"],
                        "skipLibCheck": true,
                        "moduleResolution": "bundler",
                        "allowImportingTsExtensions": true,
                        "resolveJsonModule": true,
                        "isolatedModules": true,
                        "noEmit": true,
                        "strict": true,
                        "noUnusedLocals": true,
                        "noUnusedParameters": true,
                        "noFallthroughCasesInSwitch": true
                      },
                      "include": ["src"]
                    }
                    """);
            Files.writeString(projectDir.resolve("tsconfig.node.json"), """
                    {
                      "compilerOptions": {
                        "composite": true,
                        "skipLibCheck": true,
                        "module": "ESNext",
                        "moduleResolution": "bundler",
                        "allowSyntheticDefaultImports": true
                      },
                      "include": ["vite.config.ts"]
                    }
                    """);
            Files.writeString(projectDir.resolve("src/vite-env.d.ts"), "/// <reference types=\"vite/client\" />\n");
        }

        switch (template.toLowerCase()) {
            case "react" -> scaffoldReact(projectDir, cleanName, pkgName, useTypeScript);
            case "vue" -> scaffoldVue(projectDir, cleanName, pkgName, useTypeScript);
            case "preact" -> scaffoldPreact(projectDir, cleanName, pkgName, useTypeScript);
            case "lit" -> scaffoldLit(projectDir, cleanName, pkgName, useTypeScript);
            case "svelte" -> scaffoldSvelte(projectDir, cleanName, pkgName, useTypeScript);
            case "solid" -> scaffoldSolid(projectDir, cleanName, pkgName, useTypeScript);
            case "qwik" -> scaffoldQwik(projectDir, cleanName, pkgName, useTypeScript);
            default -> scaffoldVanilla(projectDir, cleanName, pkgName, useTypeScript);
        }

        // IntelliJ IDEA workspace files (.idea/)
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

        log.accept("Configured Vite project: " + cleanName);
    }

    private static void scaffoldVanilla(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String mainExt = ts ? "ts" : "js";
        String configExt = ts ? "ts" : "js";

        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "%s",
                    "preview": "vite preview"
                  },
                  "devDependencies": {
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? "tsc && vite build" : "vite build", ts ? ",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="app"></div>
                    <script type="module" src="/src/main.%s"></script>
                  </body>
                </html>
                """.formatted(cleanName, mainExt));

        Files.writeString(projectDir.resolve("src/style.css"), """
                :root {
                  font-family: Inter, system-ui, Avenir, Helvetica, Arial, sans-serif;
                  line-height: 1.5;
                  font-weight: 400;
                  color-scheme: light dark;
                  color: rgba(255, 255, 255, 0.87);
                  background-color: #242424;
                  font-synthesis: none;
                  text-rendering: optimizeLegibility;
                  -webkit-font-smoothing: antialiased;
                  -moz-osx-font-smoothing: grayscale;
                }

                #app {
                  max-width: 1280px;
                  margin: 0 auto;
                  padding: 2rem;
                  text-align: center;
                }

                .logo {
                  height: 6em;
                  padding: 1.5em;
                  will-change: filter;
                  transition: filter 300ms;
                }
                .logo:hover {
                  filter: drop-shadow(0 0 2em #646cffaa);
                }

                .card {
                  padding: 2em;
                }

                button {
                  border-radius: 8px;
                  border: 1px solid transparent;
                  padding: 0.6em 1.2em;
                  font-size: 1em;
                  font-weight: 500;
                  font-family: inherit;
                  background-color: #1a1a1a;
                  cursor: pointer;
                  transition: border-color 0.25s;
                }
                button:hover {
                  border-color: #646cff;
                }
                button:focus,
                button:focus-visible {
                  outline: 4px auto -webkit-focus-ring-color;
                }
                """);

        Files.writeString(projectDir.resolve("src/counter." + mainExt), """
                export function setupCounter(element%s) {
                  let counter = 0
                  const setCounter = (count%s) => {
                    counter = count
                    element.innerHTML = `count is ${counter}`
                  }
                  element.addEventListener('click', () => setCounter(counter + 1))
                  setCounter(0)
                }
                """.formatted(ts ? ": HTMLButtonElement" : "", ts ? ": number" : ""));

        Files.writeString(projectDir.resolve("src/main." + mainExt), """
                import './style.css'
                import { setupCounter } from './counter.%s'

                document.querySelector%s('#app')!.innerHTML = `
                  <div>
                    <a href="https://vite.dev" target="_blank">
                      <img src="/vite.svg" class="logo" alt="Vite logo" />
                    </a>
                    <h1>Hello Vite!</h1>
                    <div class="card">
                      <button id="counter" type="button"></button>
                    </div>
                    <p class="read-the-docs">
                      Click on the Vite logo to learn more
                    </p>
                  </div>
                `

                setupCounter(document.querySelector%s('#counter')!)
                """.formatted(mainExt, ts ? "<HTMLDivElement>" : "", ts ? "<HTMLButtonElement>" : ""));

        Files.writeString(projectDir.resolve("vite.config." + configExt), """
                import { defineConfig } from 'vite'

                export default defineConfig({
                  server: {
                    port: 5173
                  }
                })
                """);
    }

    private static void scaffoldReact(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String ext = ts ? "tsx" : "jsx";
        String cfgExt = ts ? "ts" : "js";

        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "%s",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "react": "^19.0.0",
                    "react-dom": "^19.0.0"
                  },
                  "devDependencies": {
                    "@vitejs/plugin-react": "^4.3.4",
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? "tsc -b && vite build" : "vite build",
                ts ? ",\n    \"@types/react\": \"^19.0.8\",\n    \"@types/react-dom\": \"^19.0.3\",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("vite.config." + cfgExt), """
                import { defineConfig } from 'vite'
                import react from '@vitejs/plugin-react'

                export default defineConfig({
                  plugins: [react()],
                })
                """);

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="root"></div>
                    <script type="module" src="/src/main.%s"></script>
                  </body>
                </html>
                """.formatted(cleanName, ext));

        Files.writeString(projectDir.resolve("src/index.css"), """
                :root {
                  font-family: Inter, system-ui, Avenir, Helvetica, Arial, sans-serif;
                  line-height: 1.5;
                  font-weight: 400;
                  color-scheme: light dark;
                  color: rgba(255, 255, 255, 0.87);
                  background-color: #242424;
                  font-synthesis: none;
                  text-rendering: optimizeLegibility;
                  -webkit-font-smoothing: antialiased;
                  -moz-osx-font-smoothing: grayscale;
                }

                #root {
                  max-width: 1280px;
                  margin: 0 auto;
                  padding: 2rem;
                  text-align: center;
                }
                """);

        Files.writeString(projectDir.resolve("src/App.css"), """
                .logo {
                  height: 6em;
                  padding: 1.5em;
                  will-change: filter;
                  transition: filter 300ms;
                }
                .logo:hover {
                  filter: drop-shadow(0 0 2em #646cffaa);
                }
                .card {
                  padding: 2em;
                }
                """);

        Files.writeString(projectDir.resolve("src/App." + ext), """
                import { useState } from 'react'
                import './App.css'

                function App() {
                  const [count, setCount] = useState(0)

                  return (
                    <>
                      <div>
                        <a href="https://vite.dev" target="_blank">
                          <img src="/vite.svg" className="logo" alt="Vite logo" />
                        </a>
                      </div>
                      <h1>Vite + React</h1>
                      <div className="card">
                        <button onClick={() => setCount((count) => count + 1)}>
                          count is {count}
                        </button>
                      </div>
                    </>
                  )
                }

                export default App
                """);

        Files.writeString(projectDir.resolve("src/main." + ext), """
                import { StrictMode } from 'react'
                import { createRoot } from 'react-dom/client'
                import './index.css'
                import App from './App.%s'

                createRoot(document.getElementById('root')!).render(
                  <StrictMode>
                    <App />
                  </StrictMode>,
                )
                """.formatted(ext));
    }

    private static void scaffoldVue(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String mainExt = ts ? "ts" : "js";
        String cfgExt = ts ? "ts" : "js";

        Files.createDirectories(projectDir.resolve("src/components"));

        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "%s",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "vue": "^3.5.13"
                  },
                  "devDependencies": {
                    "@vitejs/plugin-vue": "^5.2.1",
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? "vue-tsc -b && vite build" : "vite build",
                ts ? ",\n    \"vue-tsc\": \"^2.2.0\",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("vite.config." + cfgExt), """
                import { defineConfig } from 'vite'
                import vue from '@vitejs/plugin-vue'

                export default defineConfig({
                  plugins: [vue()],
                })
                """);

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="app"></div>
                    <script type="module" src="/src/main.%s"></script>
                  </body>
                </html>
                """.formatted(cleanName, mainExt));

        Files.writeString(projectDir.resolve("src/style.css"), """
                :root {
                  font-family: Inter, system-ui, Avenir, Helvetica, Arial, sans-serif;
                  color-scheme: light dark;
                  color: rgba(255, 255, 255, 0.87);
                  background-color: #242424;
                }
                #app {
                  max-width: 1280px;
                  margin: 0 auto;
                  padding: 2rem;
                  text-align: center;
                }
                """);

        Files.writeString(projectDir.resolve("src/components/HelloWorld.vue"), """
                <script setup%s>
                import { ref } from 'vue'

                defineProps({
                  msg: String,
                })

                const count = ref(0)
                </script>

                <template>
                  <h1>{{ msg }}</h1>
                  <div class="card">
                    <button type="button" @click="count++">count is {{ count }}</button>
                  </div>
                </template>
                """.formatted(ts ? " lang=\"ts\"" : ""));

        Files.writeString(projectDir.resolve("src/App.vue"), """
                <script setup%s>
                import HelloWorld from './components/HelloWorld.vue'
                </script>

                <template>
                  <div>
                    <a href="https://vite.dev" target="_blank">
                      <img src="/vite.svg" class="logo" alt="Vite logo" />
                    </a>
                  </div>
                  <HelloWorld msg="Vite + Vue" />
                </template>
                """.formatted(ts ? " lang=\"ts\"" : ""));

        Files.writeString(projectDir.resolve("src/main." + mainExt), """
                import { createApp } from 'vue'
                import './style.css'
                import App from './App.vue'

                createApp(App).mount('#app')
                """);
    }

    private static void scaffoldPreact(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String ext = ts ? "tsx" : "jsx";
        String cfgExt = ts ? "ts" : "js";

        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "vite build",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "preact": "^10.26.4"
                  },
                  "devDependencies": {
                    "@preact/preset-vite": "^2.10.1",
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? ",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("vite.config." + cfgExt), """
                import { defineConfig } from 'vite'
                import preact from '@preact/preset-vite'

                export default defineConfig({
                  plugins: [preact()],
                })
                """);

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="app"></div>
                    <script type="module" src="/src/main.%s"></script>
                  </body>
                </html>
                """.formatted(cleanName, ext));

        Files.writeString(projectDir.resolve("src/app." + ext), """
                import { useState } from 'preact/hooks'

                export function App() {
                  const [count, setCount] = useState(0)

                  return (
                    <div>
                      <h1>Vite + Preact</h1>
                      <button onClick={() => setCount((count) => count + 1)}>
                        count is {count}
                      </button>
                    </div>
                  )
                }
                """);

        Files.writeString(projectDir.resolve("src/main." + ext), """
                import { render } from 'preact'
                import { App } from './app.%s'

                render(<App />, document.getElementById('app')!)
                """.formatted(ext));
    }

    private static void scaffoldLit(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String ext = ts ? "ts" : "js";
        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "vite build",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "lit": "^3.2.1"
                  },
                  "devDependencies": {
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? ",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                    <script type="module" src="/src/my-element.%s"></script>
                  </head>
                  <body>
                    <my-element>
                      <h1>Vite + Lit</h1>
                    </my-element>
                  </body>
                </html>
                """.formatted(cleanName, ext));

        Files.writeString(projectDir.resolve("src/my-element." + ext), """
                import { LitElement, html, css } from 'lit'
                import { customElement, property } from 'lit/decorators.js'

                @customElement('my-element')
                export class MyElement extends LitElement {
                  @property({ type: Number })
                  count = 0

                  render() {
                    return html`
                      <slot></slot>
                      <button @click=${() => this.count++}>
                        count is ${this.count}
                      </button>
                    `
                  }

                  static styles = css`
                    :host {
                      max-width: 1280px;
                      margin: 0 auto;
                      padding: 2rem;
                      text-align: center;
                    }
                  `
                }
                """);
    }

    private static void scaffoldSvelte(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String mainExt = ts ? "ts" : "js";
        String cfgExt = ts ? "ts" : "js";

        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "vite build",
                    "preview": "vite preview"
                  },
                  "devDependencies": {
                    "@sveltejs/vite-plugin-svelte": "^5.0.3",
                    "svelte": "^5.20.2",
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? ",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("vite.config." + cfgExt), """
                import { defineConfig } from 'vite'
                import { svelte } from '@sveltejs/vite-plugin-svelte'

                export default defineConfig({
                  plugins: [svelte()],
                })
                """);

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="app"></div>
                    <script type="module" src="/src/main.%s"></script>
                  </body>
                </html>
                """.formatted(cleanName, mainExt));

        Files.writeString(projectDir.resolve("src/App.svelte"), """
                <script%s>
                  let count = $state(0)
                </script>

                <main>
                  <h1>Vite + Svelte</h1>
                  <button on:click={() => count++}>
                    count is {count}
                  </button>
                </main>
                """.formatted(ts ? " lang=\"ts\"" : ""));

        Files.writeString(projectDir.resolve("src/main." + mainExt), """
                import { mount } from 'svelte'
                import App from './App.svelte'

                const app = mount(App, {
                  target: document.getElementById('app')!,
                })

                export default app
                """);
    }

    private static void scaffoldSolid(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String ext = ts ? "tsx" : "jsx";
        String cfgExt = ts ? "ts" : "js";

        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "vite build",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "solid-js": "^1.9.4"
                  },
                  "devDependencies": {
                    "vite-plugin-solid": "^2.11.6",
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? ",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("vite.config." + cfgExt), """
                import { defineConfig } from 'vite'
                import solidPlugin from 'vite-plugin-solid'

                export default defineConfig({
                  plugins: [solidPlugin()],
                })
                """);

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="root"></div>
                    <script type="module" src="/src/index.%s"></script>
                  </body>
                </html>
                """.formatted(cleanName, ext));

        Files.writeString(projectDir.resolve("src/App." + ext), """
                import { createSignal } from 'solid-js'

                function App() {
                  const [count, setCount] = createSignal(0)

                  return (
                    <div>
                      <h1>Vite + Solid</h1>
                      <button onClick={() => setCount((c) => c + 1)}>
                        count is {count()}
                      </button>
                    </div>
                  )
                }

                export default App
                """);

        Files.writeString(projectDir.resolve("src/index." + ext), """
                import { render } from 'solid-js/web'
                import App from './App.%s'

                render(() => <App />, document.getElementById('root')!)
                """.formatted(ext));
    }

    private static void scaffoldQwik(Path projectDir, String cleanName, String pkgName, boolean ts) throws IOException {
        String ext = ts ? "tsx" : "jsx";
        String cfgExt = ts ? "ts" : "js";

        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "version": "0.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "vite build",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "@builder.io/qwik": "^1.12.0",
                    "@builder.io/qwik-city": "^1.12.0"
                  },
                  "devDependencies": {
                    "vite": "^6.2.0"%s
                  }
                }
                """.formatted(pkgName, ts ? ",\n    \"typescript\": \"^5.7.2\"" : ""));

        Files.writeString(projectDir.resolve("vite.config." + cfgExt), """
                import { defineConfig } from 'vite'
                import { qwikVite } from '@builder.io/qwik/optimizer'
                import { qwikCity } from '@builder.io/qwik-city/vite'

                export default defineConfig({
                  plugins: [qwikCity(), qwikVite()],
                })
                """);

        Files.writeString(projectDir.resolve("index.html"), """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8" />
                    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>%s</title>
                  </head>
                  <body>
                    <div id="qwik"></div>
                    <script type="module" src="/src/root.%s"></script>
                  </body>
                </html>
                """.formatted(cleanName, ext));

        Files.writeString(projectDir.resolve("src/root." + ext), """
                import { component$ } from '@builder.io/qwik'

                export default component$(() => {
                  return (
                    <div>
                      <h1>Vite + Qwik</h1>
                    </div>
                  )
                })
                """);
    }
}
