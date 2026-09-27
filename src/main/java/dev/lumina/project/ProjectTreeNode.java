package dev.lumina.project;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Universal tree node model for the Project tool window.
 * Represents project files/folders, external libraries (JDK modules, JAR dependencies, classes),
 * and scratches/consoles (extensions, scratch files).
 */
public class ProjectTreeNode {

    public enum NodeKind {
        PROJECT_ROOT,
        DIRECTORY,
        PACKAGE,
        FILE,
        EXTERNAL_LIBRARIES_ROOT,
        SDK_ROOT,
        JDK_MODULE,
        JDK_PACKAGE,
        JDK_CLASS,
        LIBRARY_ROOT,
        JAR_PACKAGE,
        JAR_CLASS,
        SCRATCHES_ROOT,
        EXTENSIONS_ROOT,
        EXTENSION_FOLDER,
        EXTENSION_FILE,
        SCRATCH_FILE
    }

    private final NodeKind kind;
    private final String displayName;
    private final String subText;
    private final Path path;
    private final String iconKind;
    private final Supplier<List<ProjectTreeNode>> childrenSupplier;
    private final Consumer<Consumer<Path>> onOpenAction;
    private final boolean leaf;

    public ProjectTreeNode(NodeKind kind,
                           String displayName,
                           String subText,
                           Path path,
                           String iconKind,
                           boolean leaf,
                           Supplier<List<ProjectTreeNode>> childrenSupplier,
                           Consumer<Consumer<Path>> onOpenAction) {
        this.kind = Objects.requireNonNull(kind);
        this.displayName = displayName;
        this.subText = subText;
        this.path = path;
        this.iconKind = iconKind;
        this.leaf = leaf;
        this.childrenSupplier = childrenSupplier != null ? childrenSupplier : Collections::emptyList;
        this.onOpenAction = onOpenAction;
    }

    public static ProjectTreeNode directory(Path dir, String displayName, boolean isPackage, Supplier<List<ProjectTreeNode>> childrenSupplier) {
        return new ProjectTreeNode(
                isPackage ? NodeKind.PACKAGE : NodeKind.DIRECTORY,
                displayName != null ? displayName : (dir.getFileName() != null ? dir.getFileName().toString() : dir.toString()),
                null,
                dir,
                isPackage ? "package" : "folder",
                false,
                childrenSupplier,
                null
        );
    }

    public static ProjectTreeNode file(Path file, String iconKind) {
        return new ProjectTreeNode(
                NodeKind.FILE,
                file.getFileName() != null ? file.getFileName().toString() : file.toString(),
                null,
                file,
                iconKind != null ? iconKind : "file",
                true,
                null,
                opener -> {
                    if (opener != null) opener.accept(file);
                }
        );
    }

    public static ProjectTreeNode externalLibrariesRoot(Supplier<List<ProjectTreeNode>> childrenSupplier) {
        return new ProjectTreeNode(
                NodeKind.EXTERNAL_LIBRARIES_ROOT,
                "External Libraries",
                null,
                null,
                "external-libraries",
                false,
                childrenSupplier,
                null
        );
    }

    public static ProjectTreeNode scratchesRoot(Supplier<List<ProjectTreeNode>> childrenSupplier) {
        return new ProjectTreeNode(
                NodeKind.SCRATCHES_ROOT,
                "Scratches and Consoles",
                null,
                null,
                "scratches",
                false,
                childrenSupplier,
                null
        );
    }

    public NodeKind getKind() {
        return kind;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSubText() {
        return subText;
    }

    public Path getPath() {
        return path;
    }

    public String getIconKind() {
        return iconKind;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public List<ProjectTreeNode> loadChildren() {
        if (leaf || childrenSupplier == null) return List.of();
        try {
            List<ProjectTreeNode> list = childrenSupplier.get();
            return list != null ? list : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public void open(Consumer<Path> fileOpener) {
        if (onOpenAction != null) {
            onOpenAction.accept(fileOpener);
        } else if (path != null && fileOpener != null) {
            fileOpener.accept(path);
        }
    }

    @Override
    public String toString() {
        return displayName + (subText != null && !subText.isBlank() ? " " + subText : "");
    }
}
