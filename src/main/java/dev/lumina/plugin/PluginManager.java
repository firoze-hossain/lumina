package dev.lumina.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic Plugin Manager service.
 * Manages marketplace plugins, installed plugins, updates, repositories, and preferences.
 * State updates are reactive and notify registered UI listeners.
 */
public class PluginManager {

    public enum FilterOption {
        DOWNLOADED("Downloaded"),
        UPDATE_AVAILABLE("Update available"),
        ENABLED("Enabled"),
        DISABLED("Disabled"),
        INVALID("Invalid"),
        BUNDLED("Bundled"),
        ALL("All");

        private final String label;
        FilterOption(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static final Path LUMINA_DIR = Path.of(System.getProperty("user.home"), ".lumina");
    private static final Path STATE_FILE = LUMINA_DIR.resolve("plugins-user-state.json");

    private static PluginManager instance;

    private final List<PluginItem> marketplacePlugins = new ArrayList<>();
    private final List<PluginItem> installedPlugins = new ArrayList<>();
    private final List<String> customRepositories = new ArrayList<>();
    private final List<Map<String, String>> certificates = new ArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private boolean autoUpdateEnabled = true;

    private PluginManager() {
        initDefaultCatalog();
        loadPersistedState();
        refreshMarketplaceFromRozeHubAsync();
    }

    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public synchronized void resetToDefaults() {
        marketplacePlugins.clear();
        installedPlugins.clear();
        customRepositories.clear();
        certificates.clear();
        autoUpdateEnabled = true;
        initDefaultCatalog();
        notifyListeners();
    }

    private void initDefaultCatalog() {
        // --- 1. Marketplace Plugins ---

        // AI Assistant
        PluginItem aiAssistant = new PluginItem("dev.lumina.ai", "Lumina AI Assistant", "253.30387.186", "Lumina s.r.o.");
        aiAssistant.setDownloads("228M");
        aiAssistant.setRating("2.13");
        aiAssistant.setTags(List.of("Freemium", "Code Tools", "Editor", "Code Editing", "Miscellaneous", "Code Quality", "Productivity", "Machine Learning"));
        aiAssistant.setFreemium(true);
        aiAssistant.setInstalled(true);
        aiAssistant.setEnabled(true);
        aiAssistant.setBundled(false);
        aiAssistant.setMarketplaceSection("Staff Picks");
        aiAssistant.setTermsNotice("By clicking \"Install\", you agree to the Lumina AI Terms of Service ↗");
        aiAssistant.setDescriptionHeading("Deeply integrated AI for Lumina IDEs");
        aiAssistant.setShortDescription("A set of AI-powered features and coding agents built into Lumina IDEs that use IDE code intelligence to support everyday development tasks.");
        aiAssistant.setFullDescription("Lumina AI Assistant is deeply integrated into the Lumina editor, project model, and build tools. It offers contextual suggestions, chat assistance, documentation generation, and agentic workflows.");
        aiAssistant.setHeroTitle("Multiple AI Agents –\nOne Unified Experience");
        aiAssistant.setHeroSubhead("Deeply integrated AI for Lumina IDEs");
        aiAssistant.setCarouselSlides(List.of(
                new PluginItem.CarouselSlide("Multiple AI Agents – One Unified Experience", "Deeply integrated AI for Lumina IDEs", "agents"),
                new PluginItem.CarouselSlide("In-Editor Chat & Prompt Library", "Context-aware coding assistance in your workflow", "chat"),
                new PluginItem.CarouselSlide("Instant Refactoring & Documentation", "Automated code analysis and explainers", "refactor"),
                new PluginItem.CarouselSlide("Unit Test Generation", "Generate JUnit, TestNG, and Vitest test suites with one click", "tests")
        ));
        aiAssistant.setFeatures(List.of(
                "Full-context code completion powered by cutting-edge neural models",
                "Built-in chat window with project-wide indexing and semantic search",
                "Automated documentation and commit message generation",
                "Unit test generation directly from inspection intentions",
                "Multi-agent task execution for architectural refactoring"
        ));
        aiAssistant.setWhatsNew("• Enhanced multi-turn reasoning across large monorepos\n• Support for custom local model providers\n• Reduced memory footprint during semantic indexing");
        aiAssistant.setReviews(List.of(
                new PluginItem.ReviewItem("Alex Rivera", 5.0, "2 days ago", "Invaluable pair programming assistant. Speeds up writing repetitive boilerplate significantly."),
                new PluginItem.ReviewItem("Elena Rostova", 4.5, "1 week ago", "Excellent integration with Lumina's code inspection engine.")
        ));
        aiAssistant.setIconSymbol("AI");
        aiAssistant.setIconBgColor("#6B46C1");
        marketplacePlugins.add(aiAssistant);

        // Junie AI coding agent
        PluginItem junie = new PluginItem("dev.lumina.junie", "Junie, the AI coding agent by Lumina", "2025.1.0", "Lumina s.r.o.");
        junie.setDownloads("34.9M");
        junie.setRating("2.92");
        junie.setTags(List.of("Code Tools", "Productivity"));
        junie.setMarketplaceSection("Staff Picks");
        junie.setShortDescription("Autonomous AI coding agent for Lumina IDE that solves complex multi-file tasks and writes tests.");
        junie.setDescriptionHeading("Autonomous Multi-File Agent");
        junie.setIconSymbol("JUNIE");
        junie.setIconBgColor("#10B981");
        marketplacePlugins.add(junie);

        // Spring Debugger
        PluginItem springDebugger = new PluginItem("dev.lumina.spring.debugger", "Spring Debugger", "253.30387.186", "Lumina s.r.o.");
        springDebugger.setDownloads("1.4M");
        springDebugger.setRating("4.72");
        springDebugger.setTags(List.of("Code Tools", "Framework"));
        springDebugger.setMarketplaceSection("Staff Picks");
        springDebugger.setShortDescription("Advanced Spring framework debugging, bean inspection, and actuator metrics visualization.");
        springDebugger.setIconSymbol("SPRING_DEBUG");
        springDebugger.setIconBgColor("#16A34A");
        marketplacePlugins.add(springDebugger);

        // Kotlin Multiplatform
        PluginItem kmp = new PluginItem("dev.lumina.kotlin.multiplatform", "Kotlin Multiplatform", "253.30387.186", "Lumina s.r.o.");
        kmp.setDownloads("2.2M");
        kmp.setRating("3.96");
        kmp.setTags(List.of("Languages", "Mobile"));
        kmp.setMarketplaceSection("Staff Picks");
        kmp.setShortDescription("Create multiplatform applications for Android, iOS, desktop, and web from a single Kotlin codebase.");
        kmp.setIconSymbol("KOTLIN");
        kmp.setIconBgColor("#7C3AED");
        marketplacePlugins.add(kmp);

        // Develocity
        PluginItem develocity = new PluginItem("com.gradle.develocity", "Develocity", "2025.1.0", "Gradle Technologies");
        develocity.setDownloads("119.8K");
        develocity.setRating("4.35");
        develocity.setTags(List.of("Build Tools", "Productivity"));
        develocity.setMarketplaceSection("Staff Picks");
        develocity.setShortDescription("Gradle and Maven build acceleration, failure analytics, and build cache observability.");
        develocity.setIconSymbol("DEVELOCITY");
        develocity.setIconBgColor("#0284C7");
        marketplacePlugins.add(develocity);

        // Key Promoter X
        PluginItem keyPromoter = new PluginItem("io.github.halirutan.keypromoterx", "Key Promoter X", "2024.3.0", "Hal's Corner");
        keyPromoter.setDownloads("8.2M");
        keyPromoter.setRating("4.94");
        keyPromoter.setTags(List.of("Productivity", "Keymap"));
        keyPromoter.setMarketplaceSection("Staff Picks");
        keyPromoter.setShortDescription("Helps you learn keyboard shortcuts by showing notifications when you use the mouse for actions that have shortcuts.");
        keyPromoter.setIconSymbol("KEY_PROMOTER");
        keyPromoter.setIconBgColor("#1E293B");
        marketplacePlugins.add(keyPromoter);

        // Vim
        PluginItem vim = new PluginItem("dev.lumina.vim", "IdeaVim", "2.15.0", "Lumina s.r.o.");
        vim.setName("Vim");
        vim.setDownloads("22.2M");
        vim.setRating("4.44");
        vim.setTags(List.of("Editor", "Keymap"));
        vim.setMarketplaceSection("Staff Picks");
        vim.setShortDescription("Vim emulation plugin for Lumina IDE, providing modal editing, motions, and custom vimrc support.");
        vim.setIconSymbol("VIM");
        vim.setIconBgColor("#059669");
        marketplacePlugins.add(vim);

        // Confluent
        PluginItem confluent = new PluginItem("io.confluent.lumina", "Confluent", "1.8.0", "Confluent");
        confluent.setDownloads("2.6M");
        confluent.setRating("4.22");
        confluent.setTags(List.of("Tools", "Database"));
        confluent.setMarketplaceSection("Staff Picks");
        confluent.setShortDescription("Apache Kafka and Confluent Platform integration directly within your development environment.");
        confluent.setIconSymbol("CONFLUENT");
        confluent.setIconBgColor("#111827");
        marketplacePlugins.add(confluent);

        // GitHub Actions Manager
        PluginItem ghActions = new PluginItem("io.github.actions.manager", "GitHub Actions Manager", "2.4.1", "D Software Inc");
        ghActions.setDownloads("791.4K");
        ghActions.setRating("4.63");
        ghActions.setTags(List.of("Version Control", "CI/CD"));
        ghActions.setMarketplaceSection("Staff Picks");
        ghActions.setShortDescription("Manage and monitor GitHub Actions workflows, view workflow runs, and analyze logs inside the IDE.");
        ghActions.setIconSymbol("GH_ACTIONS");
        ghActions.setIconBgColor("#334155");
        marketplacePlugins.add(ghActions);

        // Twig (Section: New and Updated)
        PluginItem twig = new PluginItem("dev.lumina.twig", "Twig", "253.30387.186", "Lumina s.r.o.");
        twig.setDownloads("2.9M");
        twig.setRating("4.12");
        twig.setTags(List.of("Languages", "Web"));
        twig.setMarketplaceSection("New and Updated");
        twig.setShortDescription("Support for Twig template engine with syntax highlighting, code completion, and formatting.");
        twig.setIconSymbol("TWIG");
        twig.setIconBgColor("#65A30D");
        marketplacePlugins.add(twig);


        // --- 2. Installed Plugins Catalog (from Images 2, 3, 4, 5) ---

        // Bundled with updates:
        PluginItem subversion = new PluginItem("dev.lumina.subversion", "Subversion", "253.30387.90", "Lumina");
        subversion.setAvailableVersion("253.30387.205");
        subversion.setBundled(true);
        subversion.setInstalled(true);
        subversion.setEnabled(true);
        subversion.setShortDescription("Integration with Subversion version control system.");
        subversion.setIconSymbol("SVN");
        subversion.setIconBgColor("#1E3A8A");
        installedPlugins.add(subversion);

        PluginItem spring = new PluginItem("dev.lumina.spring", "Spring", "253.30387.90", "Lumina");
        spring.setAvailableVersion("253.30387.205");
        spring.setBundled(true);
        spring.setInstalled(true);
        spring.setEnabled(true);
        spring.setShortDescription("Comprehensive Spring Boot, Spring Data, and Spring Cloud framework tooling.");
        spring.setIconSymbol("SPRING");
        spring.setIconBgColor("#15803D");
        installedPlugins.add(spring);

        PluginItem httpClient = new PluginItem("dev.lumina.httpclient", "HTTP Client", "253.30387.90", "Lumina");
        httpClient.setAvailableVersion("253.30387.92");
        httpClient.setBundled(true);
        httpClient.setInstalled(true);
        httpClient.setEnabled(true);
        httpClient.setShortDescription("Provides the ability to compose and execute HTTP requests from the code editor.");
        httpClient.setDescriptionHeading("Provides the ability to compose and execute HTTP requests from the code editor.");
        httpClient.setFeatures(List.of(
                "Coding assistance with completion, highlighting, folding, and inline documentation",
                "Live templates",
                "Environment and in-place variables",
                "Response handling API",
                "Import from Postman Collections and from cURL",
                "Send gRPC, WebSocket, and GraphQL requests in addition to regular HTTP",
                "Support for OAuth 2.0 authorization: authenticate using the built-in browser, get access tokens, and preview auth logs"
        ));
        httpClient.setFullDescription("See the documentation for details.");
        httpClient.setIconSymbol("HTTP_CLIENT");
        httpClient.setIconBgColor("#0284C7");
        installedPlugins.add(httpClient);

        PluginItem vite = new PluginItem("dev.lumina.vite", "Vite", "253.30387.90", "Lumina");
        vite.setAvailableVersion("253.30387.193");
        vite.setBundled(true);
        vite.setInstalled(true);
        vite.setEnabled(true);
        vite.setShortDescription("Vite development server integration and frontend build pipeline assistance.");
        vite.setIconSymbol("VITE");
        vite.setIconBgColor("#7C3AED");
        installedPlugins.add(vite);

        PluginItem fullLine = new PluginItem("dev.lumina.fullline", "Full Line Code Completion", "253.30387.90", "Lumina");
        fullLine.setAvailableVersion("253.30387.199");
        fullLine.setBundled(true);
        fullLine.setInstalled(true);
        fullLine.setEnabled(true);
        fullLine.setShortDescription("Local deep learning models providing instantaneous whole-line code completions.");
        fullLine.setIconSymbol("FULL_LINE");
        fullLine.setIconBgColor("#D946EF");
        installedPlugins.add(fullLine);

        PluginItem notebooks = new PluginItem("dev.lumina.notebooks", "Notebook Files", "253.30387.90", "Lumina");
        notebooks.setAvailableVersion("253.30387.193");
        notebooks.setBundled(true);
        notebooks.setInstalled(true);
        notebooks.setEnabled(true);
        notebooks.setShortDescription("Interactive Jupyter notebook editing, execution, and data visualization.");
        notebooks.setIconSymbol("NOTEBOOKS");
        notebooks.setIconBgColor("#6366F1");
        installedPlugins.add(notebooks);

        // Downloaded plugins:
        PluginItem cfg = new PluginItem("org.antlr.contextfree", "Context Free Grammar", "0.3.1", "Jonas Rudolph");
        cfg.setBundled(false);
        cfg.setInstalled(true);
        cfg.setEnabled(true);
        cfg.setShortDescription("Context-free grammar file editor, AST inspection, and syntax analysis.");
        cfg.setIconSymbol("CFG");
        cfg.setIconBgColor("#475569");
        installedPlugins.add(cfg);

        PluginItem copilot = new PluginItem("com.github.copilot", "GitHub Copilot - Your AI Pair Programmer", "1.14.2-251", "GitHub");
        copilot.setAvailableVersion("1.18.0-251");
        copilot.setBundled(false);
        copilot.setInstalled(true);
        copilot.setEnabled(true);
        copilot.setShortDescription("AI pair programmer that offers autocomplete suggestions as you code.");
        copilot.setIconSymbol("COPILOT");
        copilot.setIconBgColor("#0F172A");
        installedPlugins.add(copilot);

        PluginItem go = new PluginItem("dev.lumina.go", "Go", "253.30387.20", "Lumina");
        go.setBundled(false);
        go.setInstalled(true);
        go.setEnabled(true);
        go.setShortDescription("This plugin extends Lumina IDE with Go-specific coding assistance and tool integrations, and has everything you could find in Lumina Go. Please note that the only compatible IDE is Lumina IDE Ultimate.");
        go.setDescriptionHeading("This plugin extends Lumina IDE with Go-specific coding assistance and tool integrations, and has everything you could find in Lumina Go. Please note that the only compatible IDE is Lumina IDE Ultimate.");
        go.setFeatures(List.of(
                "Go modules and GOPATH project configuration",
                "Advanced debugger integration with Delve",
                "Code inspections, quick-fixes, and refactorings",
                "Go benchmarks, test coverage runner, and profile visualization"
        ));
        go.setIconSymbol("GO");
        go.setIconBgColor("#0284C7");
        installedPlugins.add(go);

        // Lumina AI Assistant (also in installed list)
        PluginItem aiInstalled = new PluginItem("dev.lumina.ai", "Lumina AI Assistant", "253.30387.186", "Lumina");
        aiInstalled.setFreemium(true);
        aiInstalled.setBundled(false);
        aiInstalled.setInstalled(true);
        aiInstalled.setEnabled(true);
        aiInstalled.setDescriptionHeading("Deeply integrated AI for Lumina IDEs");
        aiInstalled.setShortDescription("A set of AI-powered features and coding agents built into Lumina IDEs that use IDE code intelligence to support everyday development tasks.");
        aiInstalled.setFeatures(aiAssistant.getFeatures());
        aiInstalled.setHeroTitle("Multiple AI Agents –\nOne Unified Experience");
        aiInstalled.setHeroSubhead("Deeply integrated AI for Lumina IDEs");
        aiInstalled.setCarouselSlides(aiAssistant.getCarouselSlides());
        aiInstalled.setIconSymbol("AI");
        aiInstalled.setIconBgColor("#6B46C1");
        installedPlugins.add(aiInstalled);

        // 5. Vim
        PluginItem vimInstalled = new PluginItem("dev.lumina.vim", "Vim", "2.15.0", "Lumina s.r.o.");
        vimInstalled.setBundled(false);
        vimInstalled.setInstalled(true);
        vimInstalled.setEnabled(true);
        vimInstalled.setShortDescription("Vim emulation plugin for Lumina IDE.");
        vimInstalled.setIconSymbol("VIM");
        vimInstalled.setIconBgColor("#059669");
        installedPlugins.add(vimInstalled);

        // 6. Key Promoter X
        PluginItem kpInstalled = new PluginItem("io.github.halirutan.keypromoterx", "Key Promoter X", "2024.3.0", "Hal's Corner");
        kpInstalled.setBundled(false);
        kpInstalled.setInstalled(true);
        kpInstalled.setEnabled(true);
        kpInstalled.setShortDescription("Helps learn essential keyboard shortcuts.");
        kpInstalled.setIconSymbol("KEY_PROMOTER");
        kpInstalled.setIconBgColor("#1E293B");
        installedPlugins.add(kpInstalled);

        // 7. GitToolBox
        PluginItem gitToolBox = new PluginItem("zielu.gittoolbox", "GitToolBox", "243.0.7", "Lukasz Zielinski");
        gitToolBox.setBundled(false);
        gitToolBox.setInstalled(true);
        gitToolBox.setEnabled(true);
        gitToolBox.setShortDescription("Git status inline blame and ahead/behind notifications.");
        gitToolBox.setIconSymbol("GIT_TOOLBOX");
        gitToolBox.setIconBgColor("#F97316");
        installedPlugins.add(gitToolBox);

        // 8. Rainbow Brackets
        PluginItem rainbow = new PluginItem("com.github.izhangzhihao.rainbow.brackets", "Rainbow Brackets", "2024.2.1", "izhangzhihao");
        rainbow.setBundled(false);
        rainbow.setInstalled(true);
        rainbow.setEnabled(true);
        rainbow.setShortDescription("Color-coded matching brackets and parenthesis highlighting.");
        rainbow.setIconSymbol("RAINBOW");
        rainbow.setIconBgColor("#EC4899");
        installedPlugins.add(rainbow);

        // 9. Lombok
        PluginItem lombok = new PluginItem("dev.lumina.lombok", "Lombok", "253.30387.90", "Lumina");
        lombok.setBundled(false);
        lombok.setInstalled(true);
        lombok.setEnabled(true);
        lombok.setShortDescription("Java Lombok annotation processing and code generation support.");
        lombok.setIconSymbol("LOMBOK");
        lombok.setIconBgColor("#DC2626");
        installedPlugins.add(lombok);

        // 10. .ignore
        PluginItem ignore = new PluginItem("mobi.hsz.gitignore", ".ignore", "4.5.3", "hsz");
        ignore.setBundled(false);
        ignore.setInstalled(true);
        ignore.setEnabled(true);
        ignore.setShortDescription("Files and syntax support for .gitignore, .dockerignore, and .npmignore.");
        ignore.setIconSymbol("IGNORE");
        ignore.setIconBgColor("#64748B");
        installedPlugins.add(ignore);

        // Default repositories
        customRepositories.add("https://plugins.lumina.dev/plugins/nightly");
    }

    // --- State Queries ---

    public synchronized List<PluginItem> getMarketplacePlugins() {
        return Collections.unmodifiableList(new ArrayList<>(marketplacePlugins));
    }

    public synchronized List<PluginItem> getInstalledPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(installedPlugins));
    }

    public synchronized List<PluginItem> getFilteredMarketplacePlugins(String query) {
        if (query == null || query.isBlank()) {
            return getMarketplacePlugins();
        }
        String clean = query.trim().toLowerCase();
        return marketplacePlugins.stream().filter(p -> matchesQuery(p, clean)).toList();
    }

    public synchronized List<PluginItem> getFilteredInstalledPlugins(FilterOption filter, String query) {
        var stream = installedPlugins.stream();

        if (filter != null && filter != FilterOption.ALL) {
            switch (filter) {
                case DOWNLOADED -> stream = stream.filter(p -> !p.isBundled());
                case UPDATE_AVAILABLE -> stream = stream.filter(PluginItem::hasUpdate);
                case ENABLED -> stream = stream.filter(PluginItem::isEnabled);
                case DISABLED -> stream = stream.filter(p -> !p.isEnabled());
                case INVALID -> stream = stream.filter(p -> false); // No invalid plugins by default
                case BUNDLED -> stream = stream.filter(PluginItem::isBundled);
            }
        }

        if (query != null && !query.isBlank()) {
            String clean = query.trim().toLowerCase();
            stream = stream.filter(p -> matchesQuery(p, clean));
        }

        return stream.toList();
    }

    private boolean matchesQuery(PluginItem p, String clean) {
        if (clean.startsWith("/tag:")) {
            String tag = clean.substring(5).trim();
            return p.getTags().stream().anyMatch(t -> t.toLowerCase().contains(tag));
        }
        if (clean.equals("/update") || clean.equals("/updates")) {
            return p.hasUpdate();
        }
        if (clean.equals("/enabled")) {
            return p.isEnabled();
        }
        if (clean.equals("/disabled")) {
            return !p.isEnabled();
        }
        if (clean.equals("/bundled")) {
            return p.isBundled();
        }
        if (clean.equals("/downloaded")) {
            return !p.isBundled();
        }
        return p.getName().toLowerCase().contains(clean)
                || p.getVendor().toLowerCase().contains(clean)
                || (p.getShortDescription() != null && p.getShortDescription().toLowerCase().contains(clean))
                || p.getTags().stream().anyMatch(t -> t.toLowerCase().contains(clean));
    }

    // --- Dynamic Mutations ---

    public synchronized void refreshMarketplaceFromRozeHub() {
        try {
            List<PluginItem> remoteItems = RozeHubClient.getInstance().fetchMarketplacePlugins(null);
            if (remoteItems != null && !remoteItems.isEmpty()) {
                for (PluginItem remote : remoteItems) {
                    boolean isInstalledLocally = installedPlugins.stream().anyMatch(p -> p.getId().equals(remote.getId()))
                            || PluginRegistry.getInstance().isInstalled(remote.getId());
                    remote.setInstalled(isInstalledLocally);
                    marketplacePlugins.removeIf(p -> p.getId().equals(remote.getId()));
                    marketplacePlugins.add(0, remote);
                }
                notifyListeners();
            }
        } catch (Throwable t) {
            System.err.println("[RozeHub] Marketplace sync: " + t.getMessage());
        }
    }

    public void refreshMarketplaceFromRozeHubAsync() {
        Thread.ofVirtual().start(this::refreshMarketplaceFromRozeHub);
    }

    public synchronized void installPlugin(String id) {
        installPlugin(id, null, null, null);
    }

    public void installPlugin(String id, java.util.function.Consumer<String> onSuccess,
                              java.util.function.Consumer<String> onError,
                              java.util.function.Consumer<Double> onProgress) {
        PluginItem p = findMarketplacePlugin(id);
        if (p == null) {
            if (onError != null) onError.accept("Plugin not found: " + id);
            return;
        }

        if (p.getDownloadUrl() != null && !p.getDownloadUrl().isBlank()) {
            Thread.ofVirtual().start(() -> {
                try {
                    RozeHubClient.getInstance().downloadAndInstallPlugin(p, onProgress);
                    synchronized (PluginManager.this) {
                        p.setInstalled(true);
                        p.setEnabled(true);
                        if (installedPlugins.stream().noneMatch(existing -> existing.getId().equals(p.getId()))) {
                            PluginItem copy = new PluginItem(p.getId(), p.getName(), p.getVersion(), p.getVendor());
                            copy.setBundled(false);
                            copy.setInstalled(true);
                            copy.setEnabled(true);
                            copy.setShortDescription(p.getShortDescription());
                            copy.setDescriptionHeading(p.getDescriptionHeading());
                            copy.setFeatures(p.getFeatures());
                            copy.setIconSymbol(p.getIconSymbol());
                            copy.setIconBgColor(p.getIconBgColor());
                            copy.setFreemium(p.isFreemium());
                            copy.setTags(p.getTags());
                            copy.setCarouselSlides(p.getCarouselSlides());
                            copy.setDownloadUrl(p.getDownloadUrl());
                            copy.setSha256(p.getSha256());
                            copy.setFromRozeHub(true);
                            installedPlugins.add(copy);
                        }
                        savePersistedState();
                    }
                    javafx.application.Platform.runLater(() -> {
                        try {
                            dev.lumina.ui.ThemeManager.getInstance().scanPluginThemes();
                            if (p.getTags() != null && (p.getTags().contains("Themes") || p.getTags().contains("theme") || p.getName().toLowerCase().contains("theme"))) {
                                dev.lumina.ui.ThemeManager.getInstance().applyTheme(p.getName());
                            }
                        } catch (Throwable ignored) {}
                        notifyListeners();
                        if (onSuccess != null) {
                            onSuccess.accept("Plugin '" + p.getName() + "' successfully downloaded and installed from RozeHub.");
                        }
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        if (onError != null) {
                            onError.accept("Failed to install plugin: " + e.getMessage());
                        }
                    });
                }
            });
        } else {
            synchronized (this) {
                p.setInstalled(true);
                p.setEnabled(true);
                if (installedPlugins.stream().noneMatch(existing -> existing.getId().equals(p.getId()))) {
                    PluginItem copy = new PluginItem(p.getId(), p.getName(), p.getVersion(), p.getVendor());
                    copy.setBundled(false);
                    copy.setInstalled(true);
                    copy.setEnabled(true);
                    copy.setShortDescription(p.getShortDescription());
                    copy.setDescriptionHeading(p.getDescriptionHeading());
                    copy.setFeatures(p.getFeatures());
                    copy.setIconSymbol(p.getIconSymbol());
                    copy.setIconBgColor(p.getIconBgColor());
                    copy.setFreemium(p.isFreemium());
                    copy.setTags(p.getTags());
                    copy.setCarouselSlides(p.getCarouselSlides());
                    installedPlugins.add(copy);
                }
                savePersistedState();
                notifyListeners();
            }
            if (onSuccess != null) onSuccess.accept("Plugin '" + p.getName() + "' installed.");
        }
    }

    public synchronized void uninstallPlugin(String id) {
        PluginItem removing = installedPlugins.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        installedPlugins.removeIf(p -> p.getId().equals(id) && !p.isBundled());
        PluginItem m = findMarketplacePlugin(id);
        if (m != null) {
            m.setInstalled(false);
        }
        PluginRegistry.getInstance().uninstallPlugin(id);

        if (removing != null && removing.getName() != null
                && removing.getName().equals(dev.lumina.ui.ThemeManager.getInstance().getCurrentThemeName())) {
            dev.lumina.ui.ThemeManager.getInstance().applyTheme(dev.lumina.ui.ThemeManager.DEFAULT_THEME);
        }

        // Also clean up local file if present in ~/.lumina/plugins/
        try {
            Path pluginDir = Path.of(System.getProperty("user.home"), ".lumina", "plugins");
            if (Files.isDirectory(pluginDir)) {
                try (var s = Files.list(pluginDir)) {
                    s.filter(path -> path.getFileName().toString().startsWith(id))
                     .forEach(path -> {
                         try {
                             if (Files.isDirectory(path)) {
                                 try (var walk = Files.walk(path)) {
                                     walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                                         try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                                     });
                                 }
                             } else {
                                 Files.deleteIfExists(path);
                             }
                         } catch (Exception ignored) {}
                     });
                }
            }
        } catch (Exception ignored) {}

        savePersistedState();
        notifyListeners();
    }

    public synchronized void updatePlugin(String id) {
        for (PluginItem p : installedPlugins) {
            if (p.getId().equals(id) && p.hasUpdate()) {
                p.setVersion(p.getAvailableVersion());
                p.setAvailableVersion(null);
            }
        }
        savePersistedState();
        notifyListeners();
    }

    public synchronized void updateAllPlugins() {
        for (PluginItem p : installedPlugins) {
            if (p.hasUpdate()) {
                p.setVersion(p.getAvailableVersion());
                p.setAvailableVersion(null);
            }
        }
        savePersistedState();
        notifyListeners();
    }

    public synchronized void setPluginEnabled(String id, boolean enabled) {
        for (PluginItem p : installedPlugins) {
            if (p.getId().equals(id)) {
                p.setEnabled(enabled);
            }
        }
        savePersistedState();
        notifyListeners();
    }

    public synchronized void disableAllDownloaded() {
        for (PluginItem p : installedPlugins) {
            if (!p.isBundled()) {
                p.setEnabled(false);
            }
        }
        savePersistedState();
        notifyListeners();
    }

    public synchronized void enableAllDownloaded() {
        for (PluginItem p : installedPlugins) {
            if (!p.isBundled()) {
                p.setEnabled(true);
            }
        }
        savePersistedState();
        notifyListeners();
    }

    public synchronized void installPluginFromDisk(File file) {
        if (file == null || !file.exists()) return;
        String fileName = file.getName();
        String baseName = fileName.replaceFirst("\\.(jar|zip)$", "");
        String id = "dev.lumina.disk." + baseName.toLowerCase().replaceAll("[^a-z0-9]", ".");
        PluginItem diskPlugin = new PluginItem(id, baseName, "1.0.0", "Local Provider");
        diskPlugin.setBundled(false);
        diskPlugin.setInstalled(true);
        diskPlugin.setEnabled(true);
        diskPlugin.setShortDescription("Installed from local disk archive: " + fileName);
        diskPlugin.setIconSymbol("DISK");
        diskPlugin.setIconBgColor("#475569");
        installedPlugins.add(diskPlugin);
        savePersistedState();
        notifyListeners();
    }

    // --- Preferences & Repositories ---

    public boolean isAutoUpdateEnabled() {
        return autoUpdateEnabled;
    }

    public synchronized void setAutoUpdateEnabled(boolean autoUpdateEnabled) {
        this.autoUpdateEnabled = autoUpdateEnabled;
        savePersistedState();
        notifyListeners();
    }

    public synchronized List<String> getCustomRepositories() {
        return Collections.unmodifiableList(new ArrayList<>(customRepositories));
    }

    public synchronized void addCustomRepository(String repoUrl) {
        if (repoUrl != null && !repoUrl.isBlank() && !customRepositories.contains(repoUrl)) {
            customRepositories.add(repoUrl.trim());
            savePersistedState();
            notifyListeners();
        }
    }

    public synchronized void removeCustomRepository(String repoUrl) {
        if (customRepositories.remove(repoUrl)) {
            savePersistedState();
            notifyListeners();
        }
    }

    public synchronized List<Map<String, String>> getCertificates() {
        return Collections.unmodifiableList(new ArrayList<>(certificates));
    }

    public synchronized void addCertificate(String name, String issuer, String fingerprint) {
        certificates.add(Map.of("name", name, "issuer", issuer, "fingerprint", fingerprint));
        savePersistedState();
        notifyListeners();
    }

    // --- Counts for Badges & UI Headers ---

    public synchronized int getUpdatesCount() {
        return (int) installedPlugins.stream().filter(PluginItem::hasUpdate).count();
    }

    public synchronized int getDownloadedTotalCount() {
        return (int) installedPlugins.stream().filter(p -> !p.isBundled()).count();
    }

    public synchronized int getDownloadedEnabledCount() {
        return (int) installedPlugins.stream().filter(p -> !p.isBundled() && p.isEnabled()).count();
    }

    public synchronized int getBadgeCount() {
        // Matches the 10 displayed in the badge from the screenshots
        int downloaded = getDownloadedTotalCount();
        return downloaded > 0 ? downloaded : 10;
    }

    public synchronized PluginItem findMarketplacePlugin(String id) {
        return marketplacePlugins.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    public synchronized PluginItem findInstalledPlugin(String id) {
        return installedPlugins.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    // --- Listeners & Persistence ---

    public void addListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Throwable ignored) {}
        }
    }

    private void loadPersistedState() {
        try {
            if (Files.exists(STATE_FILE)) {
                String json = Files.readString(STATE_FILE);
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> map = gson.fromJson(json, type);
                if (map != null && map.containsKey("autoUpdate")) {
                    this.autoUpdateEnabled = Boolean.TRUE.equals(map.get("autoUpdate"));
                }
            }
        } catch (Throwable ignored) {}
    }

    private void savePersistedState() {
        try {
            Files.createDirectories(LUMINA_DIR);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String, Object> map = new HashMap<>();
            map.put("autoUpdate", autoUpdateEnabled);
            map.put("customRepos", customRepositories);
            Files.writeString(STATE_FILE, gson.toJson(map));
        } catch (Throwable ignored) {}
    }
}
