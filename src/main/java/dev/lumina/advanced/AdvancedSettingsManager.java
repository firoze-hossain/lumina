package dev.lumina.advanced;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dynamic registry, persistence provider, and state manager for Advanced Settings.
 */
public class AdvancedSettingsManager {
    private static final String SETTINGS_PREFIX = "advanced.setting.";
    private static volatile AdvancedSettingsManager instance;

    private final Map<String, AdvancedSettingItem> registry = new LinkedHashMap<>();
    private final List<Consumer<String>> changeListeners = new CopyOnWriteArrayList<>();

    private AdvancedSettingsManager() {
        registerBuiltInSettings();
        loadAll();
    }

    public static AdvancedSettingsManager getInstance() {
        if (instance == null) {
            synchronized (AdvancedSettingsManager.class) {
                if (instance == null) {
                    instance = new AdvancedSettingsManager();
                }
            }
        }
        return instance;
    }

    /**
     * Resets the singleton instance (useful for test isolation).
     */
    public static void resetInstanceForTesting() {
        synchronized (AdvancedSettingsManager.class) {
            instance = null;
        }
    }

    private void registerBuiltInSettings() {
        // 1. AI Assistant
        register(AdvancedSettingItem.builder("ai.prompt.autodetection", "AI Assistant", "Inline AI prompt auto-detection in editor")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 2. Angular
        register(AdvancedSettingItem.builder("angular.show.navigation.popup", "Angular", "Show navigation popup in editor")
                .description("Switch quickly between files or sections within an Angular component")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 3. Appearance
        register(AdvancedSettingItem.builder("appearance.islands.ui", "Appearance", "Enable Islands UI for custom themes")
                .description("Requires restart")
                .requiresRestart(true)
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 4. Bookmarks
        register(AdvancedSettingItem.builder("bookmarks.show.only.line.bookmarks", "Bookmarks", "Show only line bookmarks in popup")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("bookmarks.context.restoration.branch.switch", "Bookmarks", "Enable bookmark context restoration on branch switching")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 5. Build Tools. Gradle
        register(AdvancedSettingItem.builder("compiler.gradle.run.using.gradle", "Build Tools. Gradle", "Run using Gradle")
                .description("By default, the IDE uses Gradle to build the project and run tasks. If disabled, application configurations will run using the IDE.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("gradle.download.sources", "Build Tools. Gradle", "Download sources")
                .description("Download sources for project dependencies during the project import. A project sync is required to download the sources after selecting this option.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("gradle.attach.script.dependencies.sources", "Build Tools. Gradle", "Attach scripts dependencies sources")
                .description("Attach sources for build scripts dependencies during the project import. A project sync is required to attach the sources after selecting this option.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("gradle.auto.download.sources.opened", "Build Tools. Gradle", "Automatically download sources for a file when opened")
                .description("Automatically download sources when navigating to decompiled code of the file.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 6. Code Review Tools
        register(AdvancedSettingItem.builder("code.review.all.in.one.diff", "Code Review Tools", "All-in-one Diff for Code Reviews")
                .description("If enabled, all changed files in code reviews are shown in a single scrollable Diff view")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("code.review.keep.increased.margins", "Code Review Tools", "Keep increased margins for Diff threads")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 7. Compiler
        register(AdvancedSettingItem.builder("compiler.automake.allow.when.app.running", "Compiler", "Allow auto-make to start even if developed application is currently running")
                .description("Automatically started make may eventually delete some classes that are required by the application")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("compiler.run.with.lower.priority", "Compiler", "Run compilation with lower priority")
                .description("Run external JPS process with IDLE priority on Windows and nice level 10 on Linux/macOS")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("compiler.unified.incremental.compilation", "Compiler", "Enable unified Java/Kotlin incremental compilation implementation")
                .description("External JPS process will use new incremental compilation implementation that can handle both Java- and Kotlin-produced bytecode")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("compiler.detailed.debug.log.build.failures", "Compiler", "Collect a detailed debug log report in case of build failures")
                .description("Saves detailed logs of failed builds to assist in debugging and troubleshooting. Logs are stored alongside other build logs")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 8. Database
        register(AdvancedSettingItem.builder("database.show.tab.in.search.everywhere", "Database", "Show Database tab in Search Everywhere")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("database.open.file.as.table.scripted.loader", "Database", "Open file as table if detected by scripted loader:")
                .description("Automatically add \"Data\" tab with table representation if there is a scripted loader for that file")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Table-first formats", "Always", "Never"))
                .defaultValue("Table-first formats").build());
        register(AdvancedSettingItem.builder("database.related.rows.action.behavior", "Database", "Related Rows action behavior:")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Navigate and filter all", "Navigate only", "Filter all only"))
                .defaultValue("Navigate and filter all").build());

        // 9. Debugger
        register(AdvancedSettingItem.builder("debugger.max.recent.expressions", "Debugger", "Maximum number of recent expressions:")
                .type(AdvancedSettingType.INTEGER).defaultValue(50).build());
        register(AdvancedSettingItem.builder("debugger.show.inlay.run.to.cursor", "Debugger", "Show inlay Run to Cursor popup")
                .description("Note, inlay Run to Cursor is implemented for the new UI only. In the classic UI, the click on line number will execute Run to Cursor.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 10. Dev Containers
        register(AdvancedSettingItem.builder("devcontainers.open.projects.natively", "Dev Containers", "Open devcontainer projects natively")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("devcontainers.clone.sources.depth.1", "Dev Containers", "Clone sources with --depth=1")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("devcontainers.choose.backend.version.automatically", "Dev Containers", "Choose the backend version automatically")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("devcontainers.notify.deployment.slow", "Dev Containers", "Display notification if deployment of dev container backend / frontend takes more than 10 sec")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("devcontainers.use.no.recreate.rebuild", "Dev Containers", "Use '--no-recreate' when rebuild Docker Compose Dev Container. The Main service will always be recreated.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 11. Docker
        register(AdvancedSettingItem.builder("docker.connect.automatically.at.restart", "Docker", "Connect to Docker automatically at restart")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("docker.image.registry.loading.limit", "Docker", "Image registry entities loading limit:")
                .type(AdvancedSettingType.INTEGER).defaultValue(100).build());
        register(AdvancedSettingItem.builder("docker.log.tab.first.for.container", "Docker", "The log tab should be the first tab for the docker container.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("docker.reconnection.delay.ms", "Docker", "The delay between periodic Docker status checks for automatic reconnection:")
                .description("Set the value to 0 if you want to disable periodic checking of Docker status.")
                .trailingUnit("milliseconds")
                .type(AdvancedSettingType.INTEGER).defaultValue(3000).build());
        register(AdvancedSettingItem.builder("docker.selinux.z.mount.option", "Docker", "Apply :z mount option to almost any bind volume for SELinux systems.")
                .description("This could be dangerous, you should understand what you're doing and read this one first:")
                .link("https://github.com/moby/moby/issues/30934 ↗", "https://github.com/moby/moby/issues/30934")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 12. Documentation Components
        register(AdvancedSettingItem.builder("doc.components.syntax.highlighting.inline.code", "Documentation Components", "Basic syntax highlighting of inline code:")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("As plain code", "Full syntax highlighting", "None"))
                .defaultValue("As plain code").build());
        register(AdvancedSettingItem.builder("doc.components.syntax.highlighting.multiline", "Documentation Components", "Basic syntax highlighting of multiline code blocks")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("doc.components.syntax.highlighting.links", "Documentation Components", "Syntax highlighting of links on code elements")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("doc.components.add.background.to.code.blocks", "Documentation Components", "Add background to inline and multiline code blocks")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("doc.components.show.quickdoc.completion.modal", "Documentation Components", "Show QuickDoc automatically during code completion in modal dialogs")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 13. Editor
        register(AdvancedSettingItem.builder("editor.undo.transparent.caret.movement", "Editor", "Use undo transparent caret movement")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("editor.dont.copy.cut.current.line.no.selection", "Editor", "Don't copy/cut the current line when invoking the Copy or Cut action with no selection")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("editor.dont.select.copied.line.no.selection", "Editor", "Don't select the copied line after invoking the Copy action with no selection")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("editor.paste.line.no.selection.behavior", "Editor", "When pasting a line copied with no selection:")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Paste above the caret line", "Paste at the caret position", "Paste below the caret line"))
                .defaultValue("Paste above the caret line").build());
        register(AdvancedSettingItem.builder("editor.render.special.chars.unicode.abbreviations", "Editor", "Render special characters, such as control codes, using their Unicode name abbreviations")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("editor.show.zoom.indicator", "Editor", "Show zoom indicator")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("editor.tab.character.rendering", "Editor", "Tab character rendering:")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Horizontal line", "Arrow", "Long arrow"))
                .defaultValue("Horizontal line").build());
        register(AdvancedSettingItem.builder("editor.distraction.free.left.margin", "Editor", "Left margin in distraction free mode:")
                .description("Specify -1 to use automatically calculated margin (centering the editor)")
                .trailingUnit("pixels")
                .type(AdvancedSettingType.INTEGER).defaultValue(-1).build());
        register(AdvancedSettingItem.builder("editor.line.number.font.delta", "Editor", "Line number's font delta:")
                .description("This value determines the increase or decrease in the editor line number font size compared to the editor's font size")
                .trailingUnit("points")
                .type(AdvancedSettingType.INTEGER).defaultValue(-1).build());
        register(AdvancedSettingItem.builder("editor.force.soft.wrap.line.limit", "Editor", "Force soft wrap in documents with lines longer than:")
                .description("Increasing this limit will negatively affect editor performance")
                .trailingUnit("characters")
                .type(AdvancedSettingType.INTEGER).defaultValue(100000).build());
        register(AdvancedSettingItem.builder("editor.select.whitespace.extend.selection", "Editor", "Select whitespace characters with 'Extend Selection'")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("editor.move.caret.down.after.line.comment", "Editor", "Move caret down after Comment with Line Comment action")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("editor.hide.floating.toolbar.code.editing", "Editor", "Hide floating toolbar for code editing")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 14. Editor Tabs
        register(AdvancedSettingItem.builder("editor.tabs.navigate.prefer.existing.inactive.split", "Editor Tabs", "When navigating to a file, prefer selecting existing tab in inactive split pane")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("editor.tabs.open.declaration.in.same.tab", "Editor Tabs", "Open declaration source in the same tab")
                .description("When navigating to a method/class/variable declaration, the source file that contains the declaration will replace the current tab if there are no changes")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("editor.tabs.open.declaration.from.detached.in.main.window", "Editor Tabs", "Open declaration source called from a detached window in the main IDE window")
                .description("When navigating to a method/class/variable declaration from a tab in the detached window, new tabs will be opened in the main IDE window. Overrides 'Open declaration source in the same tab'.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("editor.tabs.double.click.hide.restore.windows", "Editor Tabs", "Perform 'Hide All Tool Windows' / 'Restore Windows' with double-click on editor tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("editor.tabs.double.click.maximize.editor", "Editor Tabs", "Perform 'Maximize Editor' / 'Normalize Splits' with double-click on editor tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("editor.tabs.equalize.proportions.nested.splits", "Editor Tabs", "Equalize proportions in nested splits")
                .description("Adjust splitters to keep space equally distributed after split/unsplit.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("editor.tabs.keep.pinned.tabs.on.left", "Editor Tabs", "Keep pinned tabs on the left")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 15. Find/Replace
        register(AdvancedSettingItem.builder("find.replace.enable.similar.usages.clustering", "Find/Replace", "Enable similar usages clustering in Find Usages view")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("find.replace.max.results.preview", "Find/Replace", "Maximum number of results to show in Find in Files/Show Usages preview:")
                .description("Increasing this limit can significantly increase IDE memory usage")
                .type(AdvancedSettingType.INTEGER).defaultValue(100).build());

        // 16. Frameworks. Ktor
        register(AdvancedSettingItem.builder("frameworks.ktor.run.using.gradle", "Frameworks. Ktor", "Run using Gradle")
                .description("By default, IDE uses Gradle to build the project and run the tasks. If disabled, run Ktor configurations using IDE.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 17. Frameworks. Micronaut
        register(AdvancedSettingItem.builder("frameworks.micronaut.run.using.gradle", "Frameworks. Micronaut", "Run using Gradle")
                .description("By default, IDE uses Gradle to build the project and run the tasks. If disabled, run Micronaut configurations using IDE.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 18. Frameworks. Spring Boot
        register(AdvancedSettingItem.builder("frameworks.spring.boot.run.using.gradle", "Frameworks. Spring Boot", "Run using Gradle")
                .description("By default, IDE uses Gradle to build the project and run the tasks. If disabled, run Spring Boot configurations using IDE.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 19. Gateway WSL
        register(AdvancedSettingItem.builder("gateway.wsl.open.natively.local", "Gateway WSL", "Open WSL projects natively as local projects (without Remote Development)")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 20. GitHub actions
        register(AdvancedSettingItem.builder("github.actions.authoring.support", "GitHub actions", "GitHub actions authoring support (requires IDE restart)")
                .description("Enable GitHub actions support: parameter autocompletion, inspections and navigation inside the workflow, and custom action files")
                .requiresRestart(true)
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 21. Go
        register(AdvancedSettingItem.builder("go.suggest.optimal.regional.goproxy", "Go", "Suggest the optimal regional GOPROXY")
                .description("Suggests the optimal GOPROXY for your region when needed. This may improve the download speed of Go module dependencies.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("go.optimize.go.list.performance", "Go", "Optimize 'go list' performance")
                .description("Execute 'go list' concurrently")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 22. IDE
        register(AdvancedSettingItem.builder("ide.max.recent.projects", "IDE", "Maximum number of recent projects:")
                .type(AdvancedSettingType.INTEGER).defaultValue(50).build());
        register(AdvancedSettingItem.builder("ide.max.recent.files", "IDE", "Maximum number of recent files:")
                .type(AdvancedSettingType.INTEGER).defaultValue(50).build());
        register(AdvancedSettingItem.builder("ide.max.recent.locations", "IDE", "Maximum number of recent locations:")
                .type(AdvancedSettingType.INTEGER).defaultValue(25).build());
        register(AdvancedSettingItem.builder("ide.local.history.days", "IDE", "Duration of storing changes in Local History:")
                .description("Do not use Local History as the primary version control system since it may become corrupted if the IDE hangs")
                .trailingUnit("days")
                .type(AdvancedSettingType.INTEGER).defaultValue(5).build());

        // 23. JVM languages
        register(AdvancedSettingItem.builder("jvm.languages.highlight.terminal.class.names", "JVM languages", "Process terminal output to find class names and highlight them")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 24. Java
        register(AdvancedSettingItem.builder("java.code.vision.min.usages", "Java", "Code Vision: Minimum usages required to show inlay hints:")
                .type(AdvancedSettingType.INTEGER).defaultValue(0).build());
        register(AdvancedSettingItem.builder("java.completion.static.methods.qualifier.first.arg", "Java", "Enable completion for static methods, which uses qualifier as a first argument")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("java.listen.config.files.on.open", "Java", "Start listening for changes in configuration files ('.sdkmanrc', '.tool-versions', ...) on project opening")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("java.show.irrelevant.new.file.templates", "Java", "Show irrelevant New File templates in Java source roots")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 25. Java Bytecode Decompiler
        register(AdvancedSettingItem.builder("java.decompiler.max.direct.nodes", "Java Bytecode Decompiler", "Max count of direct nodes:")
                .type(AdvancedSettingType.INTEGER).defaultValue(20000).build());
        register(AdvancedSettingItem.builder("java.decompiler.max.variable.nodes", "Java Bytecode Decompiler", "Max count of variable nodes:")
                .type(AdvancedSettingType.INTEGER).defaultValue(30000).build());

        // 26. JavaScript and TypeScript
        register(AdvancedSettingItem.builder("js.ts.semantic.highlighting.accuracy", "JavaScript and TypeScript", "Semantic highlighting accuracy:")
                .description("Semantic highlighting assigns the code elements different colors to distinguish locals from globals, functions from objects etc. Higher accuracy analyzes the code deeper possibly consuming more CPU.")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Normal", "High", "Low"))
                .defaultValue("Normal").build());
        register(AdvancedSettingItem.builder("js.ts.advanced.js.annotator", "JavaScript and TypeScript", "Advanced JavaScript annotator")
                .description("JavaScript annotator checks basic semantic JS errors in JS / JSX. Advanced annotator can find more errors possibly consuming more CPU.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("js.ts.highlight.jsx.client.components", "JavaScript and TypeScript", "Highlight JSX client components")
                .description("Uses different colors for JSX server / client components. May consume additional CPU")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("js.ts.advanced.jsx.annotator", "JavaScript and TypeScript", "Advanced JSX annotator")
                .description("JSX annotator validates JSX tags in JSX and TSX. Advanced annotator can find more errors possibly consuming more CPU. Consider disabling the option if you use TypeScript language service.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("js.ts.search.in.library.files.find.in.files", "JavaScript and TypeScript", "Search in library files when \"Directory\" is selected in Find in Files")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("js.ts.fallback.ide.type.evaluator", "JavaScript and TypeScript", "Fallback to IDE type evaluator for files outside the current import graph when using types from server.")
                .description("The IDE can try to evaluate types from files, which are not within an import graph of the current TypeScript project (tsconfig.json). In such cases, server can load and parse a lot of new files to provide types. Falling back to the IDE evaluator significantly improves performance on large projects.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 27. Kotlin
        register(AdvancedSettingItem.builder("kotlin.experimental.multiplatform.features", "Kotlin", "Enable experimental Multiplatform IDE features")
                .description("Requires IDE restart")
                .requiresRestart(true)
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("kotlin.compiler.reference.index", "Kotlin", "Compiler reference index")
                .description("Enables find usages using references from compiler indices")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 28. Kubernetes
        register(AdvancedSettingItem.builder("kubernetes.max.kubeconfig.size.mb", "Kubernetes", "Maximum size of kubeconfig file to load:")
                .trailingUnit("MB")
                .type(AdvancedSettingType.INTEGER).defaultValue(10).build());
        register(AdvancedSettingItem.builder("kubernetes.show.empty.node.no.cluster", "Kubernetes", "Show an empty Kubernetes node if no cluster available")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("kubernetes.max.crd.yaml.size.mb", "Kubernetes", "Maximum size of CRD YAML file to load:")
                .trailingUnit("MB")
                .type(AdvancedSettingType.INTEGER).defaultValue(50).build());
        register(AdvancedSettingItem.builder("kubernetes.kubectl.cluster.timeout.seconds", "Kubernetes", "Timeout for kubectl cluster operations:")
                .trailingUnit("seconds")
                .type(AdvancedSettingType.INTEGER).defaultValue(20).build());
        register(AdvancedSettingItem.builder("kubernetes.http.request.timeout.seconds", "Kubernetes", "Timeout for HTTP request:")
                .trailingUnit("seconds")
                .type(AdvancedSettingType.INTEGER).defaultValue(20).build());

        // 29. Markdown
        register(AdvancedSettingItem.builder("markdown.hide.floating.toolbar", "Markdown", "Hide floating toolbar")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("markdown.squash.multiple.dashes.anchors", "Markdown", "Squash multiple dashes in header anchors")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 30. Other
        register(AdvancedSettingItem.builder("other.http.client.requests.log.max", "Other", "Maximum amount of requests in HTTP Client requests log:")
                .type(AdvancedSettingType.INTEGER).defaultValue(50).build());

        // 31. PHP
        register(AdvancedSettingItem.builder("php.max.depth.resolving.member.references", "PHP", "Maximum depth of resolving member references:")
                .type(AdvancedSettingType.INTEGER).defaultValue(150).build());
        register(AdvancedSettingItem.builder("php.code.vision.max.usages", "PHP", "Code vision: maximum number of usages to search:")
                .type(AdvancedSettingType.INTEGER).defaultValue(500).build());
        register(AdvancedSettingItem.builder("php.code.vision.max.inherited.entities", "PHP", "Code vision: maximum number of inherited entities to process:")
                .type(AdvancedSettingType.INTEGER).defaultValue(500).build());
        register(AdvancedSettingItem.builder("php.param.type.inference.complexity.limit", "PHP", "Parameter type inference complexity limit:")
                .type(AdvancedSettingType.INTEGER).defaultValue(40).build());
        register(AdvancedSettingItem.builder("php.type.completion.complexity.limit", "PHP", "Type completion complexity limit:")
                .type(AdvancedSettingType.INTEGER).defaultValue(30).build());
        register(AdvancedSettingItem.builder("php.run.builtin.formatter.before.external", "PHP", "Run built-in formatter before external formatter")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 32. Project View
        register(AdvancedSettingItem.builder("project.view.increase.font.size", "Project View", "Increase font size in Project view")
                .description("Requires IDE restart")
                .requiresRestart(true)
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("project.view.move.focus.editor.enter", "Project View", "Move focus to editor when Enter is pressed")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("project.view.collapse.all.expanded.nodes", "Project View", "When collapsing a node, also collapse all expanded nodes under it")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("project.view.dont.select.library.class", "Project View", "When navigating to a library class, do not select it in the project tree")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("project.view.enable.scratches.consoles", "Project View", "Enable Scratches and Consoles in the Project view")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 33. Python
        register(AdvancedSettingItem.builder("python.pytest.swap.actual.expected", "Python", "Swap the order of actual and expected assertions in Pytest")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("python.pytest.dont.add.no.header.summary", "Python", "Pytest: do not add \"--no-header --no-summary -q\"")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("python.code.vision.max.usages", "Python", "Code Vision: maximum number of usages to search:")
                .type(AdvancedSettingType.INTEGER).defaultValue(500).build());
        register(AdvancedSettingItem.builder("python.debugger.attach.timeout", "Python", "Debugger: Attach to Process connection timeout:")
                .type(AdvancedSettingType.INTEGER).defaultValue(20000).build());

        // 34. Rails
        register(AdvancedSettingItem.builder("rails.load.generators.automatically", "Rails", "Load generators automatically")
                .description("Generators will be loaded in the background on project open, and on dependency changes")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 35. Rake
        register(AdvancedSettingItem.builder("rake.load.tasks.automatically", "Rake", "Load tasks automatically")
                .description("Tasks will be loaded in the background on project open, and on dependency changes")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 36. Run/Debug
        register(AdvancedSettingItem.builder("run.debug.temporary.configurations.limit", "Run/Debug", "Temporary configurations limit:")
                .type(AdvancedSettingType.INTEGER).defaultValue(5).build());
        register(AdvancedSettingItem.builder("run.debug.confirm.rerun.process.termination", "Run/Debug", "Confirm rerun with process termination")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("run.debug.make.configurations.pinned.default", "Run/Debug", "Make configurations pinned by default")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("run.debug.max.recent.configurations", "Run/Debug", "Maximum number of recent run configurations:")
                .type(AdvancedSettingType.INTEGER).defaultValue(5).build());

        // 37. Rust
        register(AdvancedSettingItem.builder("rust.show.test.results.tool.window", "Rust", "Show test results in the Test tool window")
                .description("Displays structured test results. Not recommended when using the Rust 1.70.0+ stable toolchain due to possible inconsistencies.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("rust.enable.json.to.rust.conversion.paste", "Rust", "Enable JSON to Rust conversion on paste:")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Ask every time", "Yes", "Never"))
                .defaultValue("Ask every time").build());
        register(AdvancedSettingItem.builder("rust.base.url.external.doc", "Rust", "Base URL for external documentation:")
                .type(AdvancedSettingType.STRING).defaultValue("https://docs.rs/").build());
        register(AdvancedSettingItem.builder("rust.macro.expansion.recursion.limit", "Rust", "Maximum recursion limit for macro expansion:")
                .description("Reduce the number in case of performance issues")
                .type(AdvancedSettingType.INTEGER).defaultValue(1000).build());
        register(AdvancedSettingItem.builder("rust.show.attach.cargo.projects.dialog.on.open", "Rust", "Show Attach Cargo Projects dialog each time a new project is opened")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("rust.show.attach.cargo.projects.dialog.on.unattached", "Rust", "Show Attach Cargo Projects dialog each time an unattached file is opened")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("rust.use.legacy.colors.color.schemes", "Rust", "Use legacy colors for Rust color schemes (requires restart)")
                .requiresRestart(true)
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 38. SSH
        register(AdvancedSettingItem.builder("ssh.config.files.parser", "SSH", "Configuration files parser:")
                .description("If you select Legacy, ~/.ssh/config is parsed the same way as in 2021.3 and previous releases. If you select OpenSSH, the config file is parsed using the 'ssh -G' command. In this case, OpenSSH must be installed in your system.")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("OpenSSH", "Legacy"))
                .defaultValue("OpenSSH").build());
        register(AdvancedSettingItem.builder("ssh.custom.path.openssh.tool", "SSH", "Custom path to an OpenSSH tool:")
                .description("This option is used only when the OpenSSH config parser is selected.")
                .type(AdvancedSettingType.STRING).defaultValue("ssh").build());

        // 39. Search Everywhere
        register(AdvancedSettingItem.builder("search.everywhere.wait.all.contributors", "Search Everywhere", "Wait for all contributors to finish before showing results")
                .description("Enabling this option will stop the search items to change their position in the displayed search results. This process might cause a delay.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.show.notifications.above.results", "Search Everywhere", "Show notifications above search results")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.contributors.timeout.ms", "Search Everywhere", "Contributors waiting timeout (ms):")
                .type(AdvancedSettingType.INTEGER).defaultValue(2000).build());
        register(AdvancedSettingItem.builder("search.everywhere.show.recent.files.at.top", "Search Everywhere", "Show recent files at the top of results list")
                .description("When enabled, recent files are always displayed at the top of the results list. Therefore, even if these files match the search pattern less precisely than other results, they will remain at the top.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.show.text.search.results", "Search Everywhere", "Show text search results in Search Everywhere")
                .description("Show text search results in the \"All\" tab and display the \"Text\" tab in Search Everywhere")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.ml.sort.actions", "Search Everywhere", "Sort results in the Actions tab based on machine learning")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.ml.sort.files", "Search Everywhere", "Sort results in the Files tab based on machine learning")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.ml.sort.classes", "Search Everywhere", "Sort results in the Classes tab based on machine learning")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.semantic.actions", "Search Everywhere", "Enable semantic search in the Actions tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.semantic.files", "Search Everywhere", "Enable semantic search in the Files tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.semantic.symbols", "Search Everywhere", "Enable semantic search in the Symbols tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.semantic.classes", "Search Everywhere", "Enable semantic search in the Classes tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("search.everywhere.typo.tolerant.actions", "Search Everywhere", "Enable typo-tolerant search in the Search Everywhere Action tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 40. Search Scopes in Find, Replace, Rename
        register(AdvancedSettingItem.builder("search.scopes.keep.last.selected", "Search Scopes in Find, Replace, Rename", "Keep last selected search scope")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 41. Security Analysis
        register(AdvancedSettingItem.builder("security.analysis.show.problems.tab", "Security Analysis", "Show Problems Tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 42. Startup
        register(AdvancedSettingItem.builder("startup.open.readme.no.open.files", "Startup", "Open README.md file if there are no open files on project startup")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 43. Suggested refactoring
        register(AdvancedSettingItem.builder("suggested.refactoring.show.hint.in.editor", "Suggested refactoring", "Show a hint for suggested refactoring in the editor")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 44. Terminal
        register(AdvancedSettingItem.builder("terminal.show.application.title", "Terminal", "Show application title")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("terminal.scrollback.buffer.size", "Terminal", "Terminal scrollback buffer size:")
                .trailingUnit("lines")
                .type(AdvancedSettingType.INTEGER).defaultValue(5000).build());
        register(AdvancedSettingItem.builder("terminal.move.focus.to.editor.escape", "Terminal", "Move focus to the editor with Escape")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("terminal.typeahead", "Terminal", "Typeahead")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("terminal.typeahead.latency.threshold.ms", "Terminal", "Typeahead latency threshold:")
                .trailingUnit("ms")
                .type(AdvancedSettingType.INTEGER).defaultValue(100).build());
        register(AdvancedSettingItem.builder("terminal.use.one.point.zero.line.spacing.alt.screen", "Terminal", "Use 1.0 line spacing for alternative screen buffer (mc, vim, nano, etc.)")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("terminal.fill.character.background.including.line.spacing", "Terminal", "Fill character background including line spacing")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("terminal.character.encoding", "Terminal", "Terminal character encoding:")
                .type(AdvancedSettingType.STRING).defaultValue("UTF-8").build());
        register(AdvancedSettingItem.builder("terminal.new.output.capacity.kb", "Terminal", "New terminal output capacity:")
                .trailingUnit("KB")
                .type(AdvancedSettingType.INTEGER).defaultValue(1024).build());
        register(AdvancedSettingItem.builder("terminal.start.ssh.terminal.in.deployment.dir", "Terminal", "Start SSH terminal in the deployment directory")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 45. Tool Windows
        register(AdvancedSettingItem.builder("tool.windows.always.show.header.icons", "Tool Windows", "Always show tool window header icons")
                .description("If this option is disabled, tool window header icons are shown only on mouse hover or when the tool window is focused")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("tool.windows.allow.dragging.by.header", "Tool Windows", "Allow dragging tool windows by header to move to another location")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());

        // 46. User Interface
        register(AdvancedSettingItem.builder("ui.use.native.file.chooser", "User Interface", "Use native file chooser dialog on Windows/macOS")
                .description("When enabled, the IDE will use an OS-specific file chooser dialog instead of the custom one for opening files/directories.")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("ui.merge.main.menu.window.title", "User Interface", "Merge main menu with window title")
                .description("Requires restart")
                .requiresRestart(true)
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("ui.show.file.type.icon.frame.header", "User Interface", "Show file type icon in IDE frame header")
                .description("On macOS, the icon can be used to drag a file to a different application")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("ui.use.words.instead.of.symbols.macos", "User Interface", "Use words instead of symbols for macOS keyboard shortcuts")
                .description("Change shortcut symbols \u21E7, \u232B, \u2325, and others to Esc, Backspace, Option, etc")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("ui.activate.menu.items.right.mouse.release", "User Interface", "Activate menu items on right mouse button release")
                .description("For macOS and Linux only")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("ui.cyclic.scrolling.lists.trees", "User Interface", "Cyclic scrolling in lists and trees")
                .description("Select the last element when pressing Up on the first element, and vice versa")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("ui.position.mouse.cursor.default.button", "User Interface", "Position mouse cursor on default button in dialogs")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("ui.disable.double.modifier.shortcuts", "User Interface", "Disable double modifier key shortcuts")
                .description("Shift-Shift for Search Everywhere, Ctrl-Ctrl for Run Anything")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("ui.show.settings.window.as.editor.tab", "User Interface", "Show settings window as an editor tab")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 47. Version Control
        register(AdvancedSettingItem.builder("vcs.open.diff.as.editor.tab", "Version Control", "Open Diff as Editor Tab")
                .description("If enabled, Diff is shown in an editor tab instead of a separate window")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("vcs.all.in.one.diff", "Version Control", "All-in-One Diff")
                .description("If enabled, all changed files are shown in a single scrollable Diff view")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("vcs.load.file.annotations.on.open", "Version Control", "Load file annotations from VCS when file is opened in editor")
                .description("This allows showing them faster on demand")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("vcs.highlight.ignored.files", "Version Control", "Highlight ignored files")
                .description("Request the list of the ignored files from the VCS and show their status in the IDE")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("vcs.enable.commit.tool.window", "Version Control", "Enable Commit tool window")
                .description("Show the Local Changes and Shelf tabs in the separate Commit tool window")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("vcs.toggle.commit.controls", "Version Control", "Toggle commit controls")
                .description("Hide commit panel and checkboxes after commit is performed and allow toggling commit UI in Local Changes")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("vcs.auto.close.commit.window.float.mode", "Version Control", "Automatically close the Commit tool window in Window/Float mode after committing")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("vcs.select.all.repos.new.commits.push", "Version Control", "Select all repositories with new commits for push")
                .description("Preselect all repositories with commits to be pushed by default")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("vcs.no.warning.force.pushing", "Version Control", "Do not show a warning when force pushing")
                .description("When force pushing to unprotected branches, no confirmation will be asked")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("vcs.use.modal.commit.interface", "Version Control", "Use modal commit interface for Git and Mercurial")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());

        // 48. Version Control. Git
        register(AdvancedSettingItem.builder("vcs.git.recursively.clone.submodules", "Version Control. Git", "Recursively clone submodules in the project")
                .description("When enabled, Checkout from Git calls 'git clone --recurse-submodules', i.e. clones the main repository with all submodules if there are any")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("vcs.git.apply.content.transformation", "Version Control. Git", "Apply content transformation when reading from Git:")
                .description("Use '--filters' or '--textconv' flags for 'git cat-file' command when reading file content from Git")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Filters", "None", "TextConv"))
                .defaultValue("Filters").build());
        register(AdvancedSettingItem.builder("vcs.git.use.safe.force.push", "Version Control. Git", "Use Safe Force Push")
                .description("Use '--force-with-lease' when calling force push from IDE instead of just '--force'")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("vcs.git.check.incoming.outgoing.commits", "Version Control. Git", "Check for incoming and outgoing commits")
                .description("Update branches info that have incoming/outgoing commits in the Branches popup")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build());
        register(AdvancedSettingItem.builder("vcs.git.dont.run.commit.hooks", "Version Control. Git", "Do not run Git commit hooks")
                .description("When enabled, commit always runs with '--no-verify' option")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
        register(AdvancedSettingItem.builder("vcs.git.branch.name.cleanup.symbol", "Version Control. Git", "Branch name cleanup symbol:")
                .description("Symbol that will be used instead prohibited symbols in Git branch names")
                .type(AdvancedSettingType.STRING).defaultValue("-").build());
        register(AdvancedSettingItem.builder("vcs.git.show.branch.names.recent.projects", "Version Control. Git", "Show branch names in the list of recent projects:")
                .description("Branch names will be shown on the Welcome screen and in the list of recent projects in the main menu")
                .type(AdvancedSettingType.ENUM)
                .options(List.of("Projects with identical names", "Never", "All projects"))
                .defaultValue("Projects with identical names").build());
        register(AdvancedSettingItem.builder("vcs.git.show.tab.in.search.everywhere", "Version Control. Git", "Show Git tab in Search Everywhere")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(false).build());
    }

    public synchronized void register(AdvancedSettingItem item) {
        registry.put(item.getId(), item);
    }

    public synchronized AdvancedSettingItem getItem(String id) {
        return registry.get(id);
    }

    public synchronized List<AdvancedSettingItem> getAllItems() {
        return new ArrayList<>(registry.values());
    }

    public synchronized List<String> getGroups() {
        Set<String> groups = new LinkedHashSet<>();
        for (AdvancedSettingItem item : registry.values()) {
            groups.add(item.getGroup());
        }
        return new ArrayList<>(groups);
    }

    public synchronized List<AdvancedSettingItem> getItemsByGroup(String group) {
        List<AdvancedSettingItem> list = new ArrayList<>();
        for (AdvancedSettingItem item : registry.values()) {
            if (Objects.equals(item.getGroup(), group)) {
                list.add(item);
            }
        }
        return list;
    }

    public boolean getBoolean(String id) {
        AdvancedSettingItem item = getItem(id);
        return item != null ? item.asBoolean() : false;
    }

    public int getInt(String id) {
        AdvancedSettingItem item = getItem(id);
        return item != null ? item.asInt() : 0;
    }

    public String getString(String id) {
        AdvancedSettingItem item = getItem(id);
        return item != null ? item.asString() : "";
    }

    public void setBoolean(String id, boolean value) {
        AdvancedSettingItem item = getItem(id);
        if (item != null) {
            item.setCurrentValue(value);
            notifyChanged(id);
        }
    }

    public void setInt(String id, int value) {
        AdvancedSettingItem item = getItem(id);
        if (item != null) {
            item.setCurrentValue(value);
            notifyChanged(id);
        }
    }

    public void setString(String id, String value) {
        AdvancedSettingItem item = getItem(id);
        if (item != null) {
            item.setCurrentValue(value);
            notifyChanged(id);
        }
    }

    public synchronized Map<String, Object> createSnapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, AdvancedSettingItem> e : registry.entrySet()) {
            map.put(e.getKey(), e.getValue().getCurrentValue());
        }
        return map;
    }

    public synchronized void restoreSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null) return;
        for (Map.Entry<String, Object> e : snapshot.entrySet()) {
            AdvancedSettingItem item = registry.get(e.getKey());
            if (item != null) {
                item.setCurrentValue(e.getValue());
                notifyChanged(e.getKey());
            }
        }
    }

    public synchronized void applySnapshot(Map<String, Object> snapshot) {
        restoreSnapshot(snapshot);
        apply();
    }

    public synchronized void loadAll() {
        for (AdvancedSettingItem item : registry.values()) {
            String key = SETTINGS_PREFIX + item.getId();
            String saved = Settings.get(key);
            if (saved != null) {
                switch (item.getType()) {
                    case BOOLEAN -> item.setCurrentValue(Boolean.parseBoolean(saved));
                    case INTEGER -> {
                        try {
                            item.setCurrentValue(Integer.parseInt(saved));
                        } catch (NumberFormatException e) {
                            item.setCurrentValue(item.getDefaultValue());
                        }
                    }
                    case STRING, ENUM -> item.setCurrentValue(saved);
                }
            } else {
                item.setCurrentValue(item.getDefaultValue());
            }
        }
    }

    public synchronized void apply() {
        for (AdvancedSettingItem item : registry.values()) {
            String key = SETTINGS_PREFIX + item.getId();
            Settings.put(key, String.valueOf(item.getCurrentValue()));
        }
    }

    public synchronized void reset() {
        loadAll();
    }

    public void addChangeListener(Consumer<String> listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Consumer<String> listener) {
        changeListeners.remove(listener);
    }

    private void notifyChanged(String id) {
        for (Consumer<String> l : changeListeners) {
            try {
                l.accept(id);
            } catch (Exception ignored) {
            }
        }
    }
}
