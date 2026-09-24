package dev.lumina.gutter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Represents a dynamic gutter line marker:
 * - Implemented / Overridden method marker ((I) ↑)
 * - Implementing / Overridden by method marker ((I) ↓)
 * - Interface / Class declaration marker
 * - Inferred annotations marker (@)
 * - Spring Bean / Autowired dependency marker (↙)
 * - Test Class / Test Method run markers (▶▶, ▶, ▶✓, ▶✗)
 */
public record GutterMarker(
        int line, // 1-based real line number
        MarkerType type,
        String tooltipTitle,
        String tooltipSubtitle,
        String signaturePreview,
        List<NavigationTarget> targets
) {
    public enum MarkerType {
        IMPLEMENTS_METHOD,      // Green circle with 'I' and upward arrow ↑
        OVERRIDES_METHOD,       // Green circle with 'O' and upward arrow ↑
        IMPLEMENTED_METHOD,     // Green circle with 'I' and downward arrow ↓
        IMPLEMENTS_INTERFACE,   // Class implements interface
        IMPLEMENTED_INTERFACE,  // Interface implemented by class(es) ((I) ↓)
        INFERRED_ANNOTATION,    // Subtle circle with @
        BEAN_INJECTION,         // Green circle with down-left arrow ↙
        TEST_CLASS,             // Class-level run button (green double play ▶▶)
        TEST_METHOD,            // Method-level run button (green play ▶)
        TEST_METHOD_PASSED,     // Method-level passed test button (green checkmark ✓ with play ▶)
        TEST_METHOD_FAILED      // Method-level failed test button (red cross ✗ with play ▶)
    }

    public boolean isTestMarker() {
        return type == MarkerType.TEST_CLASS || type == MarkerType.TEST_METHOD
                || type == MarkerType.TEST_METHOD_PASSED || type == MarkerType.TEST_METHOD_FAILED;
    }

    public record NavigationTarget(
            Path file,
            int line,
            String targetName,
            String packageName,
            String signature,
            String description
    ) {}

    public GutterMarker {
        targets = targets == null ? Collections.emptyList() : Collections.unmodifiableList(targets);
    }
}
