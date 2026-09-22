package dev.lumina.git;

/**
 * Git status representation and IntelliJ IDEA color mapping for files in project tree and editor tabs.
 * Connects to VcsColorManager to support dynamic user customization in Settings > File Status Colors.
 */
public enum GitFileStatus {
    NORMAL("#DFE1E5"),
    ADDED("#59A869"),      // Green / Sky-green (Git added / staged, matches screenshot 'tuk')
    MODIFIED("#56A8F5"),   // Sky-blue (Git modified)
    UNTRACKED("#ED6C63"),  // Light red / Coral (Unversioned files, matches screenshot 'Hello')
    DELETED("#E06C75"),    // Red (Git deleted)
    IGNORED("#6F737A");    // Gray (Git ignored)

    private final String originalColorHex;

    GitFileStatus(String colorHex) {
        this.originalColorHex = colorHex;
    }

    public String getOriginalColorHex() {
        return originalColorHex;
    }

    public String getColorHex() {
        return VcsColorManager.getInstance().getColorHex(this);
    }

    public boolean isModifiedOrNew() {
        return this == ADDED || this == MODIFIED || this == UNTRACKED;
    }
}
