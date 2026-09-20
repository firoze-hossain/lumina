package dev.lumina.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dynamically builds the "New" menu items matching IntelliJ IDEA:
 * <ul>
 *   <li>When a <b>Java Source Root</b> (e.g. {@code src/main/java}, {@code src/test/java},
 *       or any package/file within them) is selected: presents Java Class, Kotlin Class/File,
 *       File, Package, FXML File, JavaFX Application, package-info.java, module-info.java,
 *       Kotlin Notebook, and Resource Bundle.</li>
 *   <li>When the <b>Project Root</b> or any non-source folder is selected: presents project-level
 *       creations, multi-language file types (Python, Go, PHP, JavaScript, HTML, CSS, Dockerfile, etc.),
 *       and Data Sources.</li>
 * </ul>
 */
public final class NewMenuBuilder {

    public interface CreationHandlers {
        void onNewProject();
        void onNewProjectFromExisting();
        void onNewProjectFromVCS();
        void onNewModule();
        void onNewModuleFromExisting();
        void onNewJavaClass(Path dir);
        void onNewKotlinClass(Path dir);
        void onNewFile(Path dir);
        void onNewPackage(Path dir);
        void onNewDirectory(Path dir);
        void onNewFxml(Path dir);
        void onNewJavaFxApp(Path dir);
        void onNewPackageInfo(Path dir);
        void onNewModuleInfo(Path dir);
        void onNewKotlinNotebook(Path dir);
        void onNewResourceBundle(Path dir);
        void onNewScratchFile(Path dir);
        void onNewSpecificFile(Path dir, String defaultName, String defaultContent);
        void onPlaceholder(String title);
    }

    public enum IconType { NONE, CIRCLE, LETTER, FOLDER, PACKAGE, GIT }

    public record ItemSpec(
            String text,
            String accelerator,
            IconType iconType,
            String iconGlyph,
            String iconColor,
            double fontSize,
            boolean isSeparator,
            boolean disabled,
            List<ItemSpec> subItems,
            Consumer<CreationHandlers> action
    ) {
        public static ItemSpec separator() {
            return new ItemSpec(null, null, IconType.NONE, null, null, 0, true, false, List.of(), h -> {});
        }

        public static ItemSpec item(String text, String accel, IconType type, String glyph, String color, double size, Consumer<CreationHandlers> action) {
            return new ItemSpec(text, accel, type, glyph, color, size, false, false, List.of(), action);
        }

        public static ItemSpec disabledItem(String text, String accel, IconType type, String glyph, String color, double size, Consumer<CreationHandlers> action) {
            return new ItemSpec(text, accel, type, glyph, color, size, false, true, List.of(), action);
        }

        public static ItemSpec submenu(String text, IconType type, String glyph, String color, double size, List<ItemSpec> subItems) {
            return new ItemSpec(text, null, type, glyph, color, size, false, false, subItems, h -> {});
        }
    }

    private NewMenuBuilder() {}

    /**
     * Dynamically determines whether the specified path is inside or is a recognized source root.
     */
    public static boolean isInsideSourceRoot(Path path) {
        if (path == null) return false;
        Path curr = path.toAbsolutePath().normalize();
        while (curr != null) {
            String s = curr.toString().replace('\\', '/');
            for (String marker : new String[]{
                    "/src/main/java", "/src/test/java",
                    "/src/main/kotlin", "/src/test/kotlin",
                    "/src/main/groovy", "/src/test/groovy"
            }) {
                if (s.endsWith(marker)) {
                    return true;
                }
            }
            curr = curr.getParent();
        }
        return false;
    }

    /**
     * Checks if module-info.java exists in the current source root or any enclosing directory.
     */
    public static boolean hasModuleInfo(Path path) {
        if (path == null) return false;
        try {
            Path curr = path.toAbsolutePath().normalize();
            while (curr != null) {
                if (java.nio.file.Files.exists(curr.resolve("module-info.java"))) {
                    return true;
                }
                if (java.nio.file.Files.exists(curr.resolve("src/main/java/module-info.java"))) {
                    return true;
                }
                curr = curr.getParent();
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Dynamically determines whether the specified path is a project root.
     */
    public static boolean isProjectRoot(Path path) {
        if (path == null) return false;
        try {
            Path norm = path.toAbsolutePath().normalize();
            if (!java.nio.file.Files.isDirectory(norm)) return false;
            return java.nio.file.Files.exists(norm.resolve("pom.xml"))
                    || java.nio.file.Files.exists(norm.resolve("build.gradle"))
                    || java.nio.file.Files.exists(norm.resolve("build.gradle.kts"))
                    || java.nio.file.Files.exists(norm.resolve(".idea"))
                    || java.nio.file.Files.exists(norm.resolve(".lumina"))
                    || java.nio.file.Files.exists(norm.resolve(".git"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Returns the structured item specifications for the given path context.
     * This method does not instantiate JavaFX nodes and can safely be evaluated in any environment.
     */
    public static List<ItemSpec> getMenuItemSpecs(Path targetPath, boolean isContextMenu, boolean isRoot) {
        List<ItemSpec> specs = new ArrayList<>();
        boolean sourceRoot = isInsideSourceRoot(targetPath);

        if (isContextMenu) {
            // ================== RIGHT-CLICK CONTEXT MENU ==================
            if (sourceRoot) {
                // Inside Source Root (Image 1 from earlier prompt: src/main/java/.../diagnostics)
                boolean modExists = hasModuleInfo(targetPath);
                specs.add(ItemSpec.item("Java Class", null, IconType.CIRCLE, "C", "#3592C4", 7.5, h -> h.onNewJavaClass(targetPath)));
                specs.add(ItemSpec.item("Kotlin Class/File", null, IconType.CIRCLE, "K", "#8A65D6", 7.5, h -> h.onNewKotlinClass(targetPath)));
                specs.add(ItemSpec.item("File", "Shortcut+N", IconType.LETTER, "\u25A2", "#BCBEC4", 8, h -> h.onNewFile(targetPath)));
                specs.add(ItemSpec.item("Package", null, IconType.PACKAGE, null, "#5A8FC2", 0, h -> h.onNewPackage(targetPath)));
                specs.add(ItemSpec.item("FXML File", null, IconType.LETTER, "</>", "#E5534B", 8, h -> h.onNewFxml(targetPath)));
                specs.add(ItemSpec.item("JavaFX Application", null, IconType.CIRCLE, "FX", "#3592C4", 7, h -> h.onNewJavaFxApp(targetPath)));
                specs.add(ItemSpec.item("package-info.java", null, IconType.LETTER, "p", "#D9A03D", 8, h -> h.onNewPackageInfo(targetPath)));
                specs.add(modExists
                        ? ItemSpec.disabledItem("module-info.java", null, IconType.LETTER, "m", "#8B92A6", 8, h -> h.onNewModuleInfo(targetPath))
                        : ItemSpec.item("module-info.java", null, IconType.LETTER, "m", "#D9A03D", 8, h -> h.onNewModuleInfo(targetPath)));
                specs.add(ItemSpec.separator());
                specs.add(ItemSpec.item("Kotlin Notebook", null, IconType.LETTER, "NB", "#8A65D6", 7, h -> h.onNewKotlinNotebook(targetPath)));
                specs.add(ItemSpec.item("Resource Bundle", null, IconType.LETTER, "RB", "#E8B450", 7, h -> h.onNewResourceBundle(targetPath)));
            } else {
                // Outside Source Root: Project Root (Image 1 & 2) vs Under Root (Image 3: .lumina, src, src/main)
                if (isRoot) {
                    specs.add(ItemSpec.item("Module\u2026", null, IconType.LETTER, "M", "#D9A03D", 8, CreationHandlers::onNewModule));
                }
                addMultiLanguageFileSpecs(specs, targetPath, isRoot);
                specs.add(ItemSpec.separator());
                specs.add(ItemSpec.item("Data Source in Path", null, IconType.LETTER, "in", "#8B92A6", 7, h -> h.onPlaceholder("Data Source in Path")));
                List<ItemSpec> phpTestItems = List.of(
                        ItemSpec.item("PHPUnit Test", null, IconType.LETTER, "php", "#777BB4", 6.5, h -> h.onPlaceholder("PHPUnit Test"))
                );
                specs.add(ItemSpec.submenu("PHP Test", IconType.LETTER, "php", "#777BB4", 6.5, phpTestItems));
            }
            return specs;
        }

        // ================== TOP MENU BAR: File -> New ==================
        // Common top group: Project / Module creation
        specs.add(ItemSpec.item("Project\u2026", "Shortcut+Shift+N", IconType.LETTER, "P", "#3592C4", 8, CreationHandlers::onNewProject));
        specs.add(ItemSpec.item("Project from Existing Sources\u2026", null, IconType.LETTER, "P", "#8B92A6", 8, CreationHandlers::onNewProjectFromExisting));
        specs.add(ItemSpec.item("Project from Version Control\u2026", null, IconType.GIT, "git", "#E5534B", 7, CreationHandlers::onNewProjectFromVCS));
        specs.add(ItemSpec.item("Module\u2026", null, IconType.LETTER, "M", "#D9A03D", 8, CreationHandlers::onNewModule));
        specs.add(ItemSpec.item("Module from Existing Sources\u2026", null, IconType.LETTER, "M", "#8B92A6", 8, CreationHandlers::onNewModuleFromExisting));
        specs.add(ItemSpec.separator());

        if (targetPath == null) {
            // Images 2 & 3: NO folder selected in project explorer
            specs.add(ItemSpec.item("Scratch File", "Ctrl+Alt+Shift+Insert", IconType.LETTER, "\u270E", "#BCBEC4", 9, h -> h.onNewScratchFile(null)));
            List<ItemSpec> dataSources = List.of(
                    ItemSpec.item("DDL Data Source", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("DDL Data Source")),
                    ItemSpec.item("Data Source from URL", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source from URL")),
                    ItemSpec.item("Data Source from Cloud Provider", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source from Cloud Provider")),
                    ItemSpec.item("Data Source from Path", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source from Path")),
                    ItemSpec.item("Data Source in Path", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source in Path"))
            );
            specs.add(ItemSpec.submenu("Data Source", IconType.LETTER, "DB", "#3592C4", 7, dataSources));
            specs.add(ItemSpec.item("DDL Data Source", null, IconType.LETTER, "DDL", "#8B92A6", 6.5, h -> h.onPlaceholder("DDL Data Source")));
            specs.add(ItemSpec.item("Data Source from URL", null, IconType.LETTER, "URL", "#8B92A6", 6.5, h -> h.onPlaceholder("Data Source from URL")));

            List<ItemSpec> cloudProviders = List.of(
                    ItemSpec.item("Amazon Web Services (AWS)", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("AWS")),
                    ItemSpec.item("Microsoft Azure", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Azure")),
                    ItemSpec.item("Google Cloud Platform", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("GCP"))
            );
            specs.add(ItemSpec.submenu("Data Source from Cloud Provider", IconType.LETTER, "\u2601", "#8B92A6", 8, cloudProviders));
            specs.add(ItemSpec.item("Data Source from Path", null, IconType.LETTER, "P", "#8B92A6", 8, h -> h.onPlaceholder("Data Source from Path")));
            specs.add(ItemSpec.item("Driver", null, IconType.LETTER, "Drv", "#3592C4", 6.5, h -> h.onPlaceholder("Driver")));
            return specs;
        }

        if (sourceRoot) {
            // Source Root Context (Image 3 & 4 from earlier prompt)
            boolean modExists = hasModuleInfo(targetPath);
            specs.add(ItemSpec.item("Java Class", null, IconType.CIRCLE, "C", "#3592C4", 7.5, h -> h.onNewJavaClass(targetPath)));
            specs.add(ItemSpec.item("Kotlin Class/File", null, IconType.CIRCLE, "K", "#8A65D6", 7.5, h -> h.onNewKotlinClass(targetPath)));
            specs.add(ItemSpec.item("File", "Shortcut+N", IconType.LETTER, "\u25A2", "#BCBEC4", 8, h -> h.onNewFile(targetPath)));
            specs.add(ItemSpec.item("Package", null, IconType.PACKAGE, null, "#5A8FC2", 0, h -> h.onNewPackage(targetPath)));
            specs.add(ItemSpec.item("FXML File", null, IconType.LETTER, "</>", "#E5534B", 8, h -> h.onNewFxml(targetPath)));
            specs.add(ItemSpec.item("JavaFX Application", null, IconType.CIRCLE, "FX", "#3592C4", 7, h -> h.onNewJavaFxApp(targetPath)));
            specs.add(ItemSpec.item("package-info.java", null, IconType.LETTER, "p", "#D9A03D", 8, h -> h.onNewPackageInfo(targetPath)));
            specs.add(modExists
                    ? ItemSpec.disabledItem("module-info.java", null, IconType.LETTER, "m", "#8B92A6", 8, h -> h.onNewModuleInfo(targetPath))
                    : ItemSpec.item("module-info.java", null, IconType.LETTER, "m", "#D9A03D", 8, h -> h.onNewModuleInfo(targetPath)));
            specs.add(ItemSpec.separator());
            specs.add(ItemSpec.item("Kotlin Notebook", null, IconType.LETTER, "NB", "#8A65D6", 7, h -> h.onNewKotlinNotebook(targetPath)));
            specs.add(ItemSpec.item("Resource Bundle", null, IconType.LETTER, "RB", "#E8B450", 7, h -> h.onNewResourceBundle(targetPath)));
        } else {
            // Project Root / Non-Source Root Context (Images 1 & 5 from earlier prompt)
            addMultiLanguageFileSpecs(specs, targetPath, isRoot);
            specs.add(ItemSpec.separator());

            // Data source submenu and items
            List<ItemSpec> dataSources = List.of(
                    ItemSpec.item("DDL Data Source", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("DDL Data Source")),
                    ItemSpec.item("Data Source from URL", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source from URL")),
                    ItemSpec.item("Data Source from Cloud Provider", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source from Cloud Provider")),
                    ItemSpec.item("Data Source from Path", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source from Path")),
                    ItemSpec.item("Data Source in Path", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Data Source in Path"))
            );
            specs.add(ItemSpec.submenu("Data Source", IconType.LETTER, "DB", "#3592C4", 7, dataSources));
            specs.add(ItemSpec.item("DDL Data Source", null, IconType.LETTER, "DDL", "#8B92A6", 6.5, h -> h.onPlaceholder("DDL Data Source")));
            specs.add(ItemSpec.item("Data Source from URL", null, IconType.LETTER, "URL", "#8B92A6", 6.5, h -> h.onPlaceholder("Data Source from URL")));

            List<ItemSpec> cloudProviders = List.of(
                    ItemSpec.item("Amazon Web Services (AWS)", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("AWS")),
                    ItemSpec.item("Microsoft Azure", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("Azure")),
                    ItemSpec.item("Google Cloud Platform", null, IconType.NONE, null, null, 0, h -> h.onPlaceholder("GCP"))
            );
            specs.add(ItemSpec.submenu("Data Source from Cloud Provider", IconType.LETTER, "\u2601", "#8B92A6", 8, cloudProviders));
            specs.add(ItemSpec.item("Data Source from Path", null, IconType.LETTER, "P", "#8B92A6", 8, h -> h.onPlaceholder("Data Source from Path")));
            specs.add(ItemSpec.item("Data Source in Path", null, IconType.LETTER, "in", "#8B92A6", 7, h -> h.onPlaceholder("Data Source in Path")));
            specs.add(ItemSpec.item("Driver", null, IconType.LETTER, "Drv", "#3592C4", 6.5, h -> h.onPlaceholder("Driver")));
            List<ItemSpec> phpTestItems = List.of(
                    ItemSpec.item("PHPUnit Test", null, IconType.LETTER, "php", "#777BB4", 6.5, h -> h.onPlaceholder("PHPUnit Test"))
            );
            specs.add(ItemSpec.submenu("PHP Test", IconType.LETTER, "php", "#777BB4", 6.5, phpTestItems));
        }

        return specs;
    }

    public static List<ItemSpec> getMenuItemSpecs(Path targetPath, boolean isContextMenu) {
        return getMenuItemSpecs(targetPath, isContextMenu, isProjectRoot(targetPath));
    }

    public static List<ItemSpec> getMenuItemSpecs(Path targetPath) {
        return getMenuItemSpecs(targetPath, false, isProjectRoot(targetPath));
    }

    private static void addMultiLanguageFileSpecs(List<ItemSpec> specs, Path targetPath, boolean isRoot) {
        specs.add(ItemSpec.item("Python File", null, IconType.LETTER, "py", "#3592C4", 7, h -> h.onNewSpecificFile(targetPath, "untitled.py", "")));
        specs.add(ItemSpec.item("Jupyter Notebook", null, IconType.LETTER, "JN", "#D9A03D", 7, h -> h.onNewSpecificFile(targetPath, "untitled.ipynb", "{\n \"cells\": [],\n \"metadata\": {},\n \"nbformat\": 4,\n \"nbformat_minor\": 2\n}\n")));
        specs.add(ItemSpec.item("Go File", null, IconType.LETTER, "go", "#00ADD8", 7, h -> h.onNewSpecificFile(targetPath, "main.go", "package main\n\nfunc main() {\n}\n")));
        specs.add(ItemSpec.item("PHP File", null, IconType.LETTER, "php", "#777BB4", 6.5, h -> h.onNewSpecificFile(targetPath, "index.php", "<?php\n")));
        specs.add(ItemSpec.item("PHP Class", null, IconType.CIRCLE, "C", "#777BB4", 7.5, h -> h.onNewSpecificFile(targetPath, "MyClass.php", "<?php\n\nclass MyClass {\n}\n")));
        specs.add(ItemSpec.item("File", "Shortcut+N", IconType.LETTER, "\u25A2", "#BCBEC4", 8, h -> h.onNewFile(targetPath)));
        specs.add(ItemSpec.item("Go Modules File", null, IconType.LETTER, "mod", "#00ADD8", 6.5, h -> h.onNewSpecificFile(targetPath, "go.mod", "module example.com/mod\n\ngo 1.22\n")));
        specs.add(ItemSpec.item("Scratch File", "Ctrl+Alt+Shift+Insert", IconType.LETTER, "\u270E", "#BCBEC4", 9, h -> h.onNewScratchFile(targetPath)));
        specs.add(ItemSpec.item("Directory", null, IconType.FOLDER, null, "#DCB67A", 0, h -> h.onNewDirectory(targetPath)));
        specs.add(ItemSpec.item("Python Package", null, IconType.PACKAGE, null, "#3592C4", 0, h -> h.onNewDirectory(targetPath)));
        specs.add(ItemSpec.separator());
        specs.add(ItemSpec.item("Kotlin Script", null, IconType.LETTER, ".kts", "#8A65D6", 6.5, h -> h.onNewSpecificFile(targetPath, "script.kts", "")));
        specs.add(ItemSpec.item("Kotlin Notebook", null, IconType.LETTER, "NB", "#8A65D6", 7, h -> h.onNewKotlinNotebook(targetPath)));
        specs.add(ItemSpec.item("JavaScript File", null, IconType.LETTER, "JS", "#F7DF1E", 7, h -> h.onNewSpecificFile(targetPath, "index.js", "")));
        specs.add(ItemSpec.item("TypeScript File", null, IconType.LETTER, "TS", "#3178C6", 7, h -> h.onNewSpecificFile(targetPath, "index.ts", "")));
        specs.add(ItemSpec.item("HTML File", null, IconType.LETTER, "html", "#E34F26", 6.5, h -> h.onNewSpecificFile(targetPath, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n    <title></title>\n</head>\n<body>\n\n</body>\n</html>\n")));
        specs.add(ItemSpec.item("Stylesheet", null, IconType.LETTER, "CSS", "#1572B6", 7, h -> h.onNewSpecificFile(targetPath, "style.css", "")));
        specs.add(ItemSpec.item("Dockerfile", null, IconType.LETTER, "D", "#2496ED", 8, h -> h.onNewSpecificFile(targetPath, "Dockerfile", "FROM alpine:latest\n")));
        specs.add(ItemSpec.item("Dev Container Config\u2026", null, IconType.LETTER, "dev", "#58A6FF", 6.5, h -> h.onPlaceholder("Dev Container Config")));
        if (isRoot) {
            specs.add(ItemSpec.item("Jupyter Connection", null, IconType.LETTER, "J", "#D9A03D", 8, h -> h.onPlaceholder("Jupyter Connection")));
        }
        specs.add(ItemSpec.item("HTTP Request", null, IconType.LETTER, "http", "#22A783", 6.5, h -> h.onNewSpecificFile(targetPath, "requests.http", "### GET request\nGET https://httpbin.org/get\n")));
        specs.add(ItemSpec.item("OpenAPI Specification", null, IconType.LETTER, "API", "#85EA2D", 6.5, h -> h.onNewSpecificFile(targetPath, "openapi.yaml", "openapi: 3.0.0\ninfo:\n  title: Sample API\n  version: 1.0.0\npaths: {}\n")));
        specs.add(ItemSpec.item("Kubernetes Resource", null, IconType.LETTER, "K8s", "#326CE5", 6.5, h -> h.onNewSpecificFile(targetPath, "k8s-deployment.yaml", "apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: app\nspec:\n  replicas: 1\n")));
        specs.add(ItemSpec.item("Helm Chart", null, IconType.LETTER, "Helm", "#0F1689", 6, h -> h.onPlaceholder("Helm Chart")));
        specs.add(ItemSpec.item("ERB File", null, IconType.LETTER, "erb", "#CC342D", 6.5, h -> h.onNewSpecificFile(targetPath, "view.html.erb", "")));
        specs.add(ItemSpec.item("composer.json File", null, IconType.LETTER, "cmp", "#885630", 6.5, h -> h.onNewSpecificFile(targetPath, "composer.json", "{\n  \"name\": \"app/project\",\n  \"require\": {}\n}\n")));
        specs.add(ItemSpec.item("Resource Bundle", null, IconType.LETTER, "RB", "#E8B450", 7, h -> h.onNewResourceBundle(targetPath)));
        specs.add(ItemSpec.item("EditorConfig File", null, IconType.LETTER, ".ec", "#BCBEC4", 6.5, h -> h.onNewSpecificFile(targetPath, ".editorconfig", "root = true\n\n[*]\nindent_style = space\nindent_size = 4\nend_of_line = lf\ncharset = utf-8\ntrim_trailing_whitespace = true\ninsert_final_newline = true\n")));
    }

    /**
     * Populates the given {@link Menu} with context-appropriate items for {@code targetPath}.
     */
    public static void populateNewMenu(Menu menu, Path targetPath, boolean isContextMenu, boolean isRoot, CreationHandlers handlers) {
        menu.getItems().clear();
        List<ItemSpec> specs = getMenuItemSpecs(targetPath, isContextMenu, isRoot);

        for (ItemSpec spec : specs) {
            if (spec.isSeparator()) {
                menu.getItems().add(new SeparatorMenuItem());
            } else if (!spec.subItems().isEmpty()) {
                Menu subMenu = new Menu(spec.text());
                if (spec.iconType() != IconType.NONE) {
                    Node icon = createIcon(spec);
                    if (spec.disabled() && icon != null) {
                        icon.setOpacity(0.4);
                    }
                    subMenu.setGraphic(icon);
                }
                if (spec.disabled()) {
                    subMenu.setDisable(true);
                }
                for (ItemSpec sub : spec.subItems()) {
                    MenuItem subItem = new MenuItem(sub.text());
                    if (sub.iconType() != IconType.NONE) {
                        Node subIcon = createIcon(sub);
                        if (sub.disabled() && subIcon != null) {
                            subIcon.setOpacity(0.4);
                        }
                        subItem.setGraphic(subIcon);
                    }
                    if (sub.disabled()) {
                        subItem.setDisable(true);
                    }
                    subItem.setOnAction(e -> sub.action().accept(handlers));
                    subMenu.getItems().add(subItem);
                }
                menu.getItems().add(subMenu);
            } else {
                MenuItem item = new MenuItem(spec.text());
                if (spec.iconType() != IconType.NONE) {
                    Node icon = createIcon(spec);
                    if (spec.disabled() && icon != null) {
                        icon.setOpacity(0.4);
                    }
                    item.setGraphic(icon);
                }
                if (spec.disabled()) {
                    item.setDisable(true);
                }
                if (spec.accelerator() != null) {
                    try {
                        item.setAccelerator(javafx.scene.input.KeyCombination.valueOf(spec.accelerator()));
                    } catch (Throwable ignored) {}
                }
                item.setOnAction(e -> spec.action().accept(handlers));
                menu.getItems().add(item);
            }
        }
    }

    public static void populateNewMenu(Menu menu, Path targetPath, boolean isContextMenu, CreationHandlers handlers) {
        populateNewMenu(menu, targetPath, isContextMenu, isProjectRoot(targetPath), handlers);
    }

    public static void populateNewMenu(Menu menu, Path targetPath, CreationHandlers handlers) {
        populateNewMenu(menu, targetPath, false, isProjectRoot(targetPath), handlers);
    }

    private static Node createIcon(ItemSpec spec) {
        try {
            return switch (spec.iconType()) {
                case CIRCLE -> kindCircle(spec.iconGlyph(), spec.iconColor(), spec.fontSize());
                case LETTER -> letterBadge(spec.iconGlyph(), spec.iconColor(), spec.fontSize());
                case FOLDER -> folderShape(spec.iconColor());
                case PACKAGE -> packageShape(spec.iconColor());
                case GIT -> kindCircle(spec.iconGlyph(), spec.iconColor(), spec.fontSize());
                case NONE -> null;
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ------------------------------------------------------------- vector icons

    public static Node kindCircle(String letter, String colorHex, double fontSize) {
        Circle circle = new Circle(6.5);
        circle.setFill(Color.web(colorHex));
        Label text = new Label(letter);
        text.setStyle("-fx-text-fill: #0B0E14; -fx-font-size: " + fontSize + "px; -fx-font-weight: bold;");
        return sized(new StackPane(circle, text));
    }

    public static Node letterBadge(String glyph, String colorHex, double fontSize) {
        Label text = new Label(glyph);
        text.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: " + fontSize + "px; -fx-font-weight: bold;");
        return sized(text);
    }

    public static Node folderShape(String colorHex) {
        Polygon folder = new Polygon(
                0, 2,   4, 2,   5.5, 0,   13, 0,   13, 2,
                13, 10, 0, 10);
        folder.setFill(Color.web(colorHex));
        return sized(folder);
    }

    public static Node packageShape(String colorHex) {
        Rectangle box = new Rectangle(12, 10);
        box.setArcWidth(2.5);
        box.setArcHeight(2.5);
        box.setFill(Color.web(colorHex));

        Line seam = new Line(2, 4, 10, 4);
        seam.setStroke(Color.web("#14161E"));
        seam.setStrokeWidth(1.1);

        Line tape = new Line(6, 4, 6, 9.5);
        tape.setStroke(Color.web("#14161E"));
        tape.setStrokeWidth(1.1);

        return sized(new StackPane(box, seam, tape));
    }

    private static Node sized(Node n) {
        StackPane pane = new StackPane(n);
        pane.setPrefSize(15, 15);
        pane.setMinSize(15, 15);
        pane.setMaxSize(15, 15);
        StackPane.setAlignment(n, Pos.CENTER);
        return pane;
    }
}

