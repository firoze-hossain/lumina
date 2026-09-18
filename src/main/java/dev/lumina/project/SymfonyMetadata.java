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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata, dynamic Packagist version resolution, and scaffolding for Symfony applications,
 * matching IntelliJ IDEA / PhpStorm's Symfony integration across Web, Console, and Demo project types.
 */
public final class SymfonyMetadata {

    public static final String TYPE_WEB = "Web";
    public static final String TYPE_CONSOLE = "Console";
    public static final String TYPE_DEMO = "Demo";

    public static final String DEFAULT_VERSION = "latest";

    public static final List<String> FALLBACK_SKELETON_VERSIONS = List.of(
            "latest",
            "8.2.x-dev",
            "8.1.x-dev",
            "v8.1.99",
            "8.0.x-dev",
            "v8.0.99",
            "7.4.x-dev",
            "v7.4.99",
            "7.3.x-dev",
            "v7.3.99",
            "7.2.x-dev",
            "v7.2.99",
            "7.1.x-dev",
            "v7.1.99",
            "7.0.x-dev",
            "v7.0.99",
            "6.4.x-dev",
            "6.3.x-dev",
            "6.2.x-dev",
            "6.1.x-dev",
            "6.0.x-dev",
            "5.4.x-dev"
    );

    public static final List<String> FALLBACK_DEMO_VERSIONS = List.of(
            "latest",
            "v3.1.0",
            "v3.0.2",
            "v3.0.1",
            "v3.0.0",
            "v2.9.0",
            "v2.8.1",
            "v2.8.0",
            "v2.7.0",
            "v2.6.0",
            "v2.5.1",
            "v2.5.0",
            "v2.4.0"
    );

    private static final Map<String, List<String>> CACHED_VERSIONS = new ConcurrentHashMap<>();

    static {
        CACHED_VERSIONS.put("skeleton", new ArrayList<>(FALLBACK_SKELETON_VERSIONS));
        CACHED_VERSIONS.put("demo", new ArrayList<>(FALLBACK_DEMO_VERSIONS));
    }

    private SymfonyMetadata() {
    }

    public static List<String> getSkeletonVersions() {
        return CACHED_VERSIONS.getOrDefault("skeleton", FALLBACK_SKELETON_VERSIONS);
    }

    public static List<String> getDemoVersions() {
        return CACHED_VERSIONS.getOrDefault("demo", FALLBACK_DEMO_VERSIONS);
    }

    public static List<String> getVersionsForType(String projectType) {
        if (TYPE_DEMO.equalsIgnoreCase(projectType)) {
            return getDemoVersions();
        }
        return getSkeletonVersions();
    }

    /**
     * Asynchronously fetches Symfony skeleton versions from Packagist.
     */
    public static CompletableFuture<List<String>> fetchSkeletonVersionsAsync(Consumer<List<String>> onFetched) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> versions = queryPackagistVersions("symfony/skeleton", false);
            if (!versions.isEmpty()) {
                CACHED_VERSIONS.put("skeleton", versions);
                if (onFetched != null) {
                    onFetched.accept(versions);
                }
                return versions;
            }
            List<String> cached = getSkeletonVersions();
            if (onFetched != null) {
                onFetched.accept(cached);
            }
            return cached;
        });
    }

    /**
     * Asynchronously fetches Symfony demo versions from Packagist.
     */
    public static CompletableFuture<List<String>> fetchDemoVersionsAsync(Consumer<List<String>> onFetched) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> versions = queryPackagistVersions("symfony/symfony-demo", true);
            if (!versions.isEmpty()) {
                CACHED_VERSIONS.put("demo", versions);
                if (onFetched != null) {
                    onFetched.accept(versions);
                }
                return versions;
            }
            List<String> cached = getDemoVersions();
            if (onFetched != null) {
                onFetched.accept(cached);
            }
            return cached;
        });
    }

    private static List<String> queryPackagistVersions(String packageName, boolean demo) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://packagist.org/packages/" + packageName + ".json"))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Lumina-IDE")
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                List<String> parsed = parseVersionsFromJson(resp.body(), demo);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    public static List<String> parseVersionsFromJson(String json, boolean demo) {
        if (json == null || json.isBlank()) return List.of();

        Pattern versionPattern = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = versionPattern.matcher(json);
        List<String> found = new ArrayList<>();

        while (matcher.find()) {
            String v = matcher.group(1);
            if (v == null || v.isBlank()) continue;
            // Ignore generic dev- branches except .x-dev
            if (v.startsWith("dev-") && !v.endsWith(".x-dev")) continue;
            if (!found.contains(v)) {
                found.add(v);
            }
        }

        if (found.isEmpty()) return List.of();

        // Sort versions
        found.sort((a, b) -> compareSymfonyVersions(b, a)); // descending

        List<String> result = new ArrayList<>();
        result.add(DEFAULT_VERSION);
        for (String v : found) {
            if (!result.contains(v)) {
                result.add(v);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static int compareSymfonyVersions(String v1, String v2) {
        String c1 = v1.startsWith("v") ? v1.substring(1) : v1;
        String c2 = v2.startsWith("v") ? v2.substring(1) : v2;

        c1 = c1.replace(".x-dev", ".9999");
        c2 = c2.replace(".x-dev", ".9999");

        String[] p1 = c1.split("[.-]");
        String[] p2 = c2.split("[.-]");

        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < p1.length ? parseIntSafe(p1[i]) : 0;
            int n2 = i < p2.length ? parseIntSafe(p2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return v1.compareTo(v2);
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Scaffolds a complete Symfony application for ProjectSpec.
     */
    public static void scaffoldProject(ProjectSpec spec, Path targetDir, Consumer<String> log) throws IOException {
        String projectType = spec.safeSymfonyProjectType();
        String version = spec.safeSymfonyVersion();
        scaffoldProject(targetDir, spec.name(), projectType, version, spec.initGit(), log);
    }

    /**
     * Scaffolds a complete Symfony application for the specified project type (Web, Console, Demo).
     */
    public static void scaffoldProject(
            Path projectDir,
            String projectName,
            String projectType,
            String version,
            boolean initGit,
            Consumer<String> log
    ) throws IOException {
        String cleanName = projectName != null && !projectName.isBlank() ? projectName.trim() : "symfony-app";
        String pkgName = cleanName.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        String type = projectType != null && !projectType.isBlank() ? projectType.trim() : TYPE_WEB;
        String ver = version != null && !version.isBlank() ? version.trim() : DEFAULT_VERSION;

        log.accept("Creating Symfony application: " + cleanName + " (" + type + ", version: " + ver + ") in " + projectDir + " …");

        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("bin"));
        Files.createDirectories(projectDir.resolve("config/packages"));
        Files.createDirectories(projectDir.resolve("src"));
        Files.createDirectories(projectDir.resolve("var/cache"));
        Files.createDirectories(projectDir.resolve("var/log"));

        String symfonyReq = parseSymfonyComposerConstraint(ver);

        // .env and .env.test
        Files.writeString(projectDir.resolve(".env"), """
                # In all environments, the following keys are loaded if they exist.
                # Run "composer dump-env prod" to compile .env files for production use (requires symfony/flex >=1.2).
                # https://symfony.com/doc/current/best_practices.html#use-environment-variables-for-infrastructure-configuration

                ###> symfony/framework-bundle ###
                APP_ENV=dev
                APP_SECRET=%s
                ###< symfony/framework-bundle ###
                """.formatted(java.util.UUID.randomUUID().toString().replace("-", "")));

        Files.writeString(projectDir.resolve(".env.test"), """
                # define your env variables for the test env here
                KERNEL_CLASS='App\\Kernel'
                APP_SECRET='$ecretf0rt3st'
                SYMFONY_DEPRECATIONS_HELPER=999999
                PANTHER_APP_ENV=panther
                PANTHER_ERROR_SCREENSHOT_DIR=./var/error-screenshots
                """);

        // .gitignore
        Files.writeString(projectDir.resolve(".gitignore"), """
                ###> symfony/framework-bundle ###
                /.env.local
                /.env.local.php
                /.env.*.local
                /public/bundles/
                /var/
                /vendor/
                ###< symfony/framework-bundle ###

                # IDE
                .idea/
                .vscode/
                *.swp
                """);

        // bin/console
        Path consolePath = projectDir.resolve("bin/console");
        Files.writeString(consolePath, """
                #!/usr/bin/env php
                <?php

                use App\\Kernel;
                use Symfony\\Bundle\\FrameworkBundle\\Console\\Application;

                if (!is_dir(dirname(__DIR__).'/vendor')) {
                    throw new LogicException('Dependencies are missing. Try running "composer install".');
                }

                if (!is_file(dirname(__DIR__).'/vendor/autoload_runtime.php')) {
                    throw new LogicException('Symfony Runtime is missing. Try running "composer require symfony/runtime".');
                }

                require_once dirname(__DIR__).'/vendor/autoload_runtime.php';

                return function (array $context) {
                    $kernel = new Kernel($context['APP_ENV'], (bool) $context['APP_DEBUG']);

                    return new Application($kernel);
                };
                """);
        try {
            consolePath.toFile().setExecutable(true, false);
        } catch (Exception ignored) {
        }

        // src/Kernel.php
        Files.writeString(projectDir.resolve("src/Kernel.php"), """
                <?php

                namespace App;

                use Symfony\\Bundle\\FrameworkBundle\\Kernel\\MicroKernelTrait;
                use Symfony\\Component\\HttpKernel\\Kernel as BaseKernel;

                class Kernel extends BaseKernel
                {
                    use MicroKernelTrait;
                }
                """);

        // config/services.yaml
        Files.writeString(projectDir.resolve("config/services.yaml"), """
                # This file is the entry point to configure your own services.
                # Files in the packages/ subfolder configure your dependencies.

                # Put parameters here that don't need to change on each machine where the app is deployed
                # https://symfony.com/doc/current/best_practices.html#use-parameters-for-application-configuration
                parameters:

                services:
                    # default configuration for services in *this* file
                    _defaults:
                        autowire: true      # Automatically injects dependencies in your services.
                        autoconfigure: true # Automatically registers your services as commands, event subscribers, etc.

                    # makes classes in src/ available to be used as services
                    # this creates a service per class whose id is the fully-qualified class name
                    App\\:
                        resource: '../src/'
                        exclude:
                            - '../src/DependencyInjection/'
                            - '../src/Entity/'
                            - '../src/Kernel.php'
                """);

        if (TYPE_CONSOLE.equalsIgnoreCase(type)) {
            scaffoldConsole(projectDir, cleanName, pkgName, symfonyReq);
        } else if (TYPE_DEMO.equalsIgnoreCase(type)) {
            scaffoldDemo(projectDir, cleanName, pkgName, symfonyReq);
        } else {
            scaffoldWeb(projectDir, cleanName, pkgName, symfonyReq);
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
                      <sourceFolder url="file://$PROJECT_DIR$/src" isTestSource="false" packagePrefix="App\\" />
                      <sourceFolder url="file://$PROJECT_DIR$/tests" isTestSource="true" packagePrefix="App\\Tests\\" />
                      <excludeFolder url="file://$PROJECT_DIR$/var" />
                      <excludeFolder url="file://$PROJECT_DIR$/vendor" />
                    </content>
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                </module>
                """);

        Files.writeString(ideaDir.resolve("php.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="PhpProjectSharedConfiguration" php_language_level="8.2" />
                </project>
                """);

        Files.writeString(ideaDir.resolve("misc.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectRootManager" version="2" />
                </project>
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

        // Optional Git init
        if (initGit) {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(projectDir.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
                log.accept("Initialized Git repository in " + projectDir);
            } catch (Exception e) {
                log.accept("Git init skipped: " + e.getMessage());
            }
        }

        log.accept("Configured Symfony (" + type + ") project: " + cleanName);
    }

    private static void scaffoldWeb(Path projectDir, String cleanName, String pkgName, String symfonyReq) throws IOException {
        Files.createDirectories(projectDir.resolve("public"));
        Files.createDirectories(projectDir.resolve("src/Controller"));
        Files.createDirectories(projectDir.resolve("templates/home"));
        Files.createDirectories(projectDir.resolve("config/packages"));

        // composer.json
        Files.writeString(projectDir.resolve("composer.json"), """
                {
                    "name": "%s/%s",
                    "type": "project",
                    "description": "Symfony Web Application",
                    "license": "proprietary",
                    "minimum-stability": "stable",
                    "prefer-stable": true,
                    "require": {
                        "php": ">=8.2",
                        "ext-ctype": "*",
                        "ext-iconv": "*",
                        "symfony/console": "%s",
                        "symfony/dotenv": "%s",
                        "symfony/flex": "^2",
                        "symfony/framework-bundle": "%s",
                        "symfony/runtime": "%s",
                        "symfony/twig-bundle": "%s",
                        "symfony/yaml": "%s"
                    },
                    "config": {
                        "allow-contrib": false,
                        "sort-packages": true
                    },
                    "autoload": {
                        "psr-4": {
                            "App\\\\": "src/"
                        }
                    },
                    "autoload-dev": {
                        "psr-4": {
                            "App\\\\Tests\\\\": "tests/"
                        }
                    },
                    "scripts": {
                        "auto-scripts": {
                            "cache:clear": "symfony-cmd",
                            "assets:install %%PUBLIC_DIR%%": "symfony-cmd"
                        }
                    }
                }
                """.formatted(pkgName, pkgName, symfonyReq, symfonyReq, symfonyReq, symfonyReq, symfonyReq, symfonyReq));

        // public/index.php
        Files.writeString(projectDir.resolve("public/index.php"), """
                <?php

                use App\\Kernel;

                require_once dirname(__DIR__).'/vendor/autoload_runtime.php';

                return function (array $context) {
                    return new Kernel($context['APP_ENV'], (bool) $context['APP_DEBUG']);
                };
                """);

        // config/bundles.php
        Files.writeString(projectDir.resolve("config/bundles.php"), """
                <?php

                return [
                    Symfony\\Bundle\\FrameworkBundle\\FrameworkBundle::class => ['all' => true],
                    Symfony\\Bundle\\TwigBundle\\TwigBundle::class => ['all' => true],
                ];
                """);

        // config/routes.yaml
        Files.writeString(projectDir.resolve("config/routes.yaml"), """
                controllers:
                    resource:
                        path: ../src/Controller/
                        namespace: App\\Controller
                    type: attribute
                """);

        // config/packages/framework.yaml
        Files.writeString(projectDir.resolve("config/packages/framework.yaml"), """
                framework:
                    secret: '%env(APP_SECRET)%'
                    session: true
                """);

        // config/packages/twig.yaml
        Files.writeString(projectDir.resolve("config/packages/twig.yaml"), """
                twig:
                    file_name_pattern: '*.twig'
                """);

        // src/Controller/HomeController.php
        Files.writeString(projectDir.resolve("src/Controller/HomeController.php"), """
                <?php

                namespace App\\Controller;

                use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;
                use Symfony\\Component\\HttpFoundation\\Response;
                use Symfony\\Component\\Routing\\Attribute\\Route;

                class HomeController extends AbstractController
                {
                    #[Route('/', name: 'app_home')]
                    public function index(): Response
                    {
                        return $this->render('home/index.html.twig', [
                            'controller_name' => 'HomeController',
                        ]);
                    }
                }
                """);

        // templates/base.html.twig
        Files.writeString(projectDir.resolve("templates/base.html.twig"), """
                <!DOCTYPE html>
                <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>{% block title %}Welcome to Symfony!{% endblock %}</title>
                        <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 128 128%22><text y=%221.2em%22 font-size=%2296%22>⚫</text><text y=%221.3em%22 x=%220.2em%22 font-size=%2276%22 fill=%22%23fff%22>sf</text></svg>">
                        {% block stylesheets %}
                        <style>
                            body { font-family: system-ui, -apple-system, sans-serif; margin: 0; padding: 2rem; background: #f8fafc; color: #1e293b; }
                            .container { max-width: 800px; margin: 0 auto; background: #ffffff; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }
                            h1 { color: #0f172a; margin-top: 0; }
                            .badge { display: inline-block; padding: 0.25rem 0.5rem; background: #e2e8f0; border-radius: 4px; font-size: 0.875rem; font-weight: 600; }
                        </style>
                        {% endblock %}
                    </head>
                    <body>
                        <div class="container">
                            {% block body %}{% endblock %}
                        </div>
                    </body>
                </html>
                """);

        // templates/home/index.html.twig
        Files.writeString(projectDir.resolve("templates/home/index.html.twig"), """
                {% extends 'base.html.twig' %}

                {% block title %}Hello {{ controller_name }}!{% endblock %}

                {% block body %}
                <h1>Welcome to Symfony! ✅</h1>
                <p>Your Symfony Web application is up and running in Lumina IDE.</p>
                <p><span class="badge">{{ controller_name }}</span> rendered this view at <code>src/Controller/HomeController.php</code>.</p>
                {% endblock %}
                """);
    }

    private static void scaffoldConsole(Path projectDir, String cleanName, String pkgName, String symfonyReq) throws IOException {
        Files.createDirectories(projectDir.resolve("src/Command"));
        Files.createDirectories(projectDir.resolve("config/packages"));

        // composer.json
        Files.writeString(projectDir.resolve("composer.json"), """
                {
                    "name": "%s/%s",
                    "type": "project",
                    "description": "Symfony Console Application",
                    "license": "proprietary",
                    "minimum-stability": "stable",
                    "prefer-stable": true,
                    "require": {
                        "php": ">=8.2",
                        "ext-ctype": "*",
                        "ext-iconv": "*",
                        "symfony/console": "%s",
                        "symfony/dotenv": "%s",
                        "symfony/flex": "^2",
                        "symfony/framework-bundle": "%s",
                        "symfony/runtime": "%s",
                        "symfony/yaml": "%s"
                    },
                    "config": {
                        "allow-contrib": false,
                        "sort-packages": true
                    },
                    "autoload": {
                        "psr-4": {
                            "App\\\\": "src/"
                        }
                    }
                }
                """.formatted(pkgName, pkgName, symfonyReq, symfonyReq, symfonyReq, symfonyReq, symfonyReq));

        // config/bundles.php
        Files.writeString(projectDir.resolve("config/bundles.php"), """
                <?php

                return [
                    Symfony\\Bundle\\FrameworkBundle\\FrameworkBundle::class => ['all' => true],
                ];
                """);

        // config/packages/framework.yaml
        Files.writeString(projectDir.resolve("config/packages/framework.yaml"), """
                framework:
                    secret: '%env(APP_SECRET)%'
                """);

        // src/Command/AppCommand.php
        Files.writeString(projectDir.resolve("src/Command/AppCommand.php"), """
                <?php

                namespace App\\Command;

                use Symfony\\Component\\Console\\Attribute\\AsCommand;
                use Symfony\\Component\\Console\\Command\\Command;
                use Symfony\\Component\\Console\\Input\\InputArgument;
                use Symfony\\Component\\Console\\Input\\InputInterface;
                use Symfony\\Component\\Console\\Output\\OutputInterface;
                use Symfony\\Component\\Console\\Style\\SymfonyStyle;

                #[AsCommand(
                    name: 'app:greet',
                    description: 'Greets someone by name',
                )]
                class AppCommand extends Command
                {
                    protected function configure(): void
                    {
                        $this->addArgument('name', InputArgument::OPTIONAL, 'The name of the user', 'World');
                    }

                    protected function execute(InputInterface $input, OutputInterface $output): int
                    {
                        $io = new SymfonyStyle($input, $output);
                        $name = $input->getArgument('name');

                        $io->success(sprintf('Hello, %s! Welcome to your Symfony Console application.', $name));

                        return Command::SUCCESS;
                    }
                }
                """);
    }

    private static void scaffoldDemo(Path projectDir, String cleanName, String pkgName, String symfonyReq) throws IOException {
        Files.createDirectories(projectDir.resolve("public"));
        Files.createDirectories(projectDir.resolve("src/Controller"));
        Files.createDirectories(projectDir.resolve("src/Entity"));
        Files.createDirectories(projectDir.resolve("templates/blog"));
        Files.createDirectories(projectDir.resolve("config/packages"));

        // composer.json
        Files.writeString(projectDir.resolve("composer.json"), """
                {
                    "name": "symfony/symfony-demo",
                    "type": "project",
                    "description": "Symfony Demo Application",
                    "license": "MIT",
                    "require": {
                        "php": ">=8.2",
                        "ext-ctype": "*",
                        "ext-iconv": "*",
                        "symfony/console": "%s",
                        "symfony/dotenv": "%s",
                        "symfony/flex": "^2",
                        "symfony/framework-bundle": "%s",
                        "symfony/runtime": "%s",
                        "symfony/twig-bundle": "%s",
                        "symfony/yaml": "%s"
                    },
                    "autoload": {
                        "psr-4": {
                            "App\\\\": "src/"
                        }
                    }
                }
                """.formatted(symfonyReq, symfonyReq, symfonyReq, symfonyReq, symfonyReq, symfonyReq));

        // public/index.php
        Files.writeString(projectDir.resolve("public/index.php"), """
                <?php

                use App\\Kernel;

                require_once dirname(__DIR__).'/vendor/autoload_runtime.php';

                return function (array $context) {
                    return new Kernel($context['APP_ENV'], (bool) $context['APP_DEBUG']);
                };
                """);

        // config/bundles.php
        Files.writeString(projectDir.resolve("config/bundles.php"), """
                <?php

                return [
                    Symfony\\Bundle\\FrameworkBundle\\FrameworkBundle::class => ['all' => true],
                    Symfony\\Bundle\\TwigBundle\\TwigBundle::class => ['all' => true],
                ];
                """);

        // config/routes.yaml
        Files.writeString(projectDir.resolve("config/routes.yaml"), """
                controllers:
                    resource:
                        path: ../src/Controller/
                        namespace: App\\Controller
                    type: attribute
                """);

        // src/Entity/Post.php
        Files.writeString(projectDir.resolve("src/Entity/Post.php"), """
                <?php

                namespace App\\Entity;

                class Post
                {
                    public function __construct(
                        public ?int $id = null,
                        public string $title = '',
                        public string $slug = '',
                        public string $summary = '',
                        public string $content = '',
                    ) {}
                }
                """);

        // src/Controller/BlogController.php
        Files.writeString(projectDir.resolve("src/Controller/BlogController.php"), """
                <?php

                namespace App\\Controller;

                use App\\Entity\\Post;
                use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;
                use Symfony\\Component\\HttpFoundation\\Response;
                use Symfony\\Component\\Routing\\Attribute\\Route;

                class BlogController extends AbstractController
                {
                    #[Route('/', name: 'blog_index')]
                    public function index(): Response
                    {
                        $posts = [
                            new Post(1, 'Welcome to Symfony Demo!', 'welcome-to-symfony-demo', 'A quick tour of Symfony demo features.', 'Full content of post 1.'),
                            new Post(2, 'Building with Twig & Components', 'building-with-twig', 'Learn how Twig simplifies frontend templating.', 'Full content of post 2.'),
                        ];

                        return $this->render('blog/index.html.twig', [
                            'posts' => $posts,
                        ]);
                    }
                }
                """);

        // templates/base.html.twig
        Files.writeString(projectDir.resolve("templates/base.html.twig"), """
                <!DOCTYPE html>
                <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>{% block title %}Symfony Demo Application{% endblock %}</title>
                        <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 128 128%22><text y=%221.2em%22 font-size=%2296%22>⚫</text><text y=%221.3em%22 x=%220.2em%22 font-size=%2276%22 fill=%22%23fff%22>sf</text></svg>">
                        <style>
                            body { font-family: system-ui, sans-serif; margin: 0; padding: 2rem; background: #0f172a; color: #f8fafc; }
                            .container { max-width: 900px; margin: 0 auto; background: #1e293b; padding: 2rem; border-radius: 8px; }
                            h1 { color: #38bdf8; }
                            .post-card { background: #334155; padding: 1.25rem; border-radius: 6px; margin-bottom: 1rem; }
                            .post-title { font-size: 1.25rem; font-weight: bold; color: #f1f5f9; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            {% block body %}{% endblock %}
                        </div>
                    </body>
                </html>
                """);

        // templates/blog/index.html.twig
        Files.writeString(projectDir.resolve("templates/blog/index.html.twig"), """
                {% extends 'base.html.twig' %}

                {% block title %}Symfony Demo - Blog{% endblock %}

                {% block body %}
                <h1>Symfony Demo Application 🚀</h1>
                <p>Reference application created with Lumina IDE.</p>
                <hr style="border: 0; border-top: 1px solid #475569; margin: 1.5rem 0;" />
                {% for post in posts %}
                    <div class="post-card">
                        <div class="post-title">{{ post.title }}</div>
                        <p>{{ post.summary }}</p>
                    </div>
                {% endfor %}
                {% endblock %}
                """);
    }

    private static String parseSymfonyComposerConstraint(String version) {
        if (version == null || version.isBlank() || DEFAULT_VERSION.equalsIgnoreCase(version.trim())) {
            return "^7.2|^8.0";
        }
        String clean = version.trim();
        if (clean.startsWith("v")) {
            clean = clean.substring(1);
        }
        if (clean.endsWith(".x-dev")) {
            return clean.replace(".x-dev", ".*@dev");
        }
        if (clean.matches("\\d+\\.\\d+\\.99")) {
            String[] parts = clean.split("\\.");
            return parts[0] + "." + parts[1] + ".*";
        }
        return "^" + clean;
    }
}
