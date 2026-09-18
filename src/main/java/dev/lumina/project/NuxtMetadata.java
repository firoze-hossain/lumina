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
 * Metadata and dynamic discovery for Nuxt project generation,
 * matching IntelliJ IDEA / PhpStorm's Nuxt integration, dynamic version resolution
 * from npm registry for {@code nuxi}, and Nuxt 3 project scaffolding.
 */
public final class NuxtMetadata {

    public static final String RUNNER_NUXI_LATEST = "npx nuxi@latest";
    public static final String ACTION_SELECT = "Select...";

    public static final String DEFAULT_NUXI_VERSION = "3.37.0";

    private static final Map<String, String> CACHED_VERSIONS = new ConcurrentHashMap<>();

    static {
        CACHED_VERSIONS.put("nuxi", DEFAULT_NUXI_VERSION);
    }

    public record NuxtCliEntry(
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

    private NuxtMetadata() {}

    /**
     * Formats an npx runner display string matching IntelliJ IDEA Screenshots 1 & 2.
     */
    public static String formatCliDisplay(String prefix, String version) {
        String ver = (version == null || version.isBlank()) ? DEFAULT_NUXI_VERSION : version.trim();
        int totalLen = 60;
        int padding = Math.max(2, totalLen - prefix.length() - ver.length());
        return prefix + " ".repeat(padding) + ver;
    }

    /**
     * Extracts version string from a formatted display or raw version.
     */
    public static String parseVersionFromDisplay(String display) {
        if (display == null || display.isBlank() || ACTION_SELECT.equals(display)) {
            return DEFAULT_NUXI_VERSION;
        }
        String trimmed = display.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            if (last.matches("\\d+(\\.\\d+)*.*")) {
                return last;
            }
        }
        return DEFAULT_NUXI_VERSION;
    }

    /**
     * Extracts runner name/command from a formatted display string.
     */
    public static String parseRunnerFromDisplay(String display) {
        if (display == null || display.isBlank()) {
            return RUNNER_NUXI_LATEST;
        }
        if (display.startsWith(RUNNER_NUXI_LATEST)) {
            return RUNNER_NUXI_LATEST;
        }
        String[] parts = display.trim().split("\\s{2,}");
        return parts.length > 0 ? parts[0] : display.trim();
    }

    /**
     * Discovers available Nuxt CLI entries matching IntelliJ IDEA Screenshot 2:
     * 1) npx nuxi@latest                                     3.37.0
     * 2) Select...
     */
    public static List<NuxtCliEntry> getCliEntries() {
        List<NuxtCliEntry> entries = new ArrayList<>();
        entries.add(new NuxtCliEntry(RUNNER_NUXI_LATEST, null, CACHED_VERSIONS.getOrDefault("nuxi", DEFAULT_NUXI_VERSION), false));
        entries.add(new NuxtCliEntry(ACTION_SELECT, null, null, true));
        return entries;
    }

    /**
     * Asynchronously fetches latest package version from npm registry and invokes callback.
     */
    public static void fetchVersionsAsync(Consumer<List<NuxtCliEntry>> onUpdated) {
        CompletableFuture.runAsync(() -> {
            String latestVer = fetchLatestNpmVersion("nuxi", DEFAULT_NUXI_VERSION);
            if (!latestVer.equals(CACHED_VERSIONS.get("nuxi"))) {
                CACHED_VERSIONS.put("nuxi", latestVer);
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
                String body = resp.body();
                Matcher m = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    /**
     * Scaffolds a complete modern Nuxt 3 application with IntelliJ IDEA workspace files.
     */
    public static void scaffoldProject(ProjectSpec spec, Path projectDir, Consumer<String> log) throws IOException {
        String projectName = spec.name();
        String cleanName = (projectName == null || projectName.isBlank()) ? "untitled1" : projectName.trim();
        String nodeInterpreter = spec.safeNuxtNodeInterpreter();
        String cliPackage = spec.safeNuxtCliPackage();
        boolean initGit = spec.initGit();

        if (log != null) {
            log.accept("Creating Nuxt 3 project in " + projectDir + " ...");
            if (nodeInterpreter != null && !nodeInterpreter.isBlank()) {
                log.accept("Node runtime: " + nodeInterpreter);
            }
            if (cliPackage != null && !cliPackage.isBlank()) {
                log.accept("Nuxt CLI: " + cliPackage);
            }
        }

        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("public"));
        Files.createDirectories(projectDir.resolve("server/api"));
        Files.createDirectories(projectDir.resolve("assets"));

        // package.json
        Files.writeString(projectDir.resolve("package.json"), """
                {
                  "name": "%s",
                  "private": true,
                  "type": "module",
                  "scripts": {
                    "build": "nuxt build",
                    "dev": "nuxt dev",
                    "generate": "nuxt generate",
                    "preview": "nuxt preview",
                    "postinstall": "nuxt prepare"
                  },
                  "devDependencies": {
                    "@types/node": "^22.10.2",
                    "nuxt": "^3.15.4",
                    "vue": "^3.5.13",
                    "vue-router": "^4.5.0"
                  }
                }
                """.formatted(cleanName));

        // nuxt.config.ts
        Files.writeString(projectDir.resolve("nuxt.config.ts"), """
                // https://nuxt.com/docs/api/configuration/nuxt-config
                export default defineNuxtConfig({
                  compatibilityDate: '2024-11-01',
                  devtools: { enabled: true }
                })
                """);

        // app.vue
        Files.writeString(projectDir.resolve("app.vue"), """
                <template>
                  <div>
                    <NuxtRouteAnnouncer />
                    <NuxtWelcome />
                  </div>
                </template>
                """);

        // tsconfig.json
        Files.writeString(projectDir.resolve("tsconfig.json"), """
                {
                  // https://nuxt.com/docs/guide/concepts/typescript
                  "extends": "./.nuxt/tsconfig.json"
                }
                """);

        // server/api/hello.ts
        Files.writeString(projectDir.resolve("server/api/hello.ts"), """
                export default defineEventHandler((event) => {
                  return {
                    hello: 'world'
                  }
                })
                """);

        // .gitignore
        Files.writeString(projectDir.resolve(".gitignore"), """
                # Nuxt dev / build outputs
                .output
                .data
                .nuxt
                .nitro
                .cache

                # Node dependencies
                node_modules

                # Logs
                logs
                *.log

                # Misc
                .DS_Store
                .env
                .env.*
                !.env.example

                # Local IDE files
                .idea
                """);

        // README.md
        Files.writeString(projectDir.resolve("README.md"), """
                # Nuxt Minimal Starter

                Look at the [Nuxt documentation](https://nuxt.com/docs/getting-started/introduction) to learn more.

                ## Setup

                Make sure to install dependencies:

                ```bash
                # npm
                npm install

                # pnpm
                pnpm install

                # yarn
                yarn install

                # bun
                bun install
                ```

                ## Development Server

                Start the development server on `http://localhost:3000`:

                ```bash
                npm run dev
                ```

                ## Production

                Build the application for production:

                ```bash
                npm run build
                ```

                Locally preview production build:

                ```bash
                npm run preview
                ```

                Check out the [deployment documentation](https://nuxt.com/docs/getting-started/deployment) for more information.
                """);

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
                      <excludeFolder url="file://$PROJECT_DIR$/.nuxt" />
                      <excludeFolder url="file://$PROJECT_DIR$/.output" />
                      <excludeFolder url="file://$PROJECT_DIR$/node_modules" />
                    </content>
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                </module>
                """);

        Files.writeString(ideaDir.resolve("workspace.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="PropertiesComponent"><![CDATA[{
                  "keyToString": {
                    "vue.rearranger.settings.migration": "true"
                  }
                }]]></component>
                </project>
                """);

        Files.writeString(ideaDir.resolve("misc.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectRootManager" version="2" />
                </project>
                """);

        // Optional Git init
        if (initGit) {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(projectDir.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
                if (log != null) {
                    log.accept("Initialized git repository.");
                }
            } catch (Exception e) {
                if (log != null) {
                    log.accept("Git initialization skipped: " + e.getMessage());
                }
            }
        }

        if (log != null) {
            log.accept("Nuxt 3 project scaffolded successfully.");
        }
    }
}
