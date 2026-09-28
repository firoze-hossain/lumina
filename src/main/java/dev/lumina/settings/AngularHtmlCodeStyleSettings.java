package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dedicated code style settings and dynamic provider for Angular HTML template.
 * Strictly decoupled and brand-isolated.
 * Supports the 3 tabs matching reference screenshots:
 * 1. Spaces (media_1790574549424.png)
 * 2. Wrapping and Braces (media_1790574558704.png)
 * 3. Arrangement (media_1790574570361.png)
 */
public class AngularHtmlCodeStyleSettings extends LanguageCodeStyleSettings {

    public static final List<String> WRAP_OPTIONS = List.of(
            "Do not wrap",
            "Wrap if long",
            "Chop down if long",
            "Wrap always"
    );

    // ==========================================
    // Property Keys - Spaces
    // ==========================================
    public static final String SPACES_WITHIN_INTERPOLATIONS = "angular_spaces_within_interpolations";

    // ==========================================
    // Property Keys - Wrapping and Braces
    // ==========================================
    public static final String WRAP_VISUAL_GUIDES = "angular_wrap_visual_guides";
    public static final String WRAP_INTERPOLATIONS = "angular_wrap_interpolations";
    public static final String WRAP_NEW_LINE_AFTER_OPEN_INTERPOLATION = "angular_wrap_new_line_after_open_interpolation";
    public static final String WRAP_NEW_LINE_BEFORE_CLOSE_INTERPOLATION = "angular_wrap_new_line_before_close_interpolation";

    // Matching rules for Arrangement tab (media_1790574570361.png)
    private final List<String> matchingRules = new ArrayList<>();

    public AngularHtmlCodeStyleSettings() {
        this("Angular HTML template");
    }

    public AngularHtmlCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        setTabSize(2);
        setIndent(2);
        setContinuationIndent(4);
        setUseTabCharacter(false);

        // Spaces
        setBoolean(SPACES_WITHIN_INTERPOLATIONS, true);

        // Wrapping and Braces
        setString(WRAP_VISUAL_GUIDES, "Default: None");
        setString(WRAP_INTERPOLATIONS, "Do not wrap");
        setBoolean(WRAP_NEW_LINE_AFTER_OPEN_INTERPOLATION, true);
        setBoolean(WRAP_NEW_LINE_BEFORE_CLOSE_INTERPOLATION, true);

        // Arrangement
        matchingRules.clear();
        matchingRules.add("attribute");
    }

    @Override
    public AngularHtmlCodeStyleSettings copy() {
        AngularHtmlCodeStyleSettings copy = new AngularHtmlCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setProperties(getAllProperties());

        copy.matchingRules.clear();
        copy.matchingRules.addAll(this.matchingRules);

        return copy;
    }

    public boolean isSpacesWithinInterpolations() {
        return getBoolean(SPACES_WITHIN_INTERPOLATIONS, true);
    }

    public void setSpacesWithinInterpolations(boolean v) {
        setBoolean(SPACES_WITHIN_INTERPOLATIONS, v);
    }

    public String getVisualGuides() {
        return getString(WRAP_VISUAL_GUIDES, "Default: None");
    }

    public void setVisualGuides(String v) {
        setString(WRAP_VISUAL_GUIDES, v);
    }

    public String getInterpolationsWrap() {
        return getString(WRAP_INTERPOLATIONS, "Do not wrap");
    }

    public void setInterpolationsWrap(String v) {
        setString(WRAP_INTERPOLATIONS, v);
    }

    public boolean isNewLineAfterOpenInterpolation() {
        return getBoolean(WRAP_NEW_LINE_AFTER_OPEN_INTERPOLATION, true);
    }

    public void setNewLineAfterOpenInterpolation(boolean v) {
        setBoolean(WRAP_NEW_LINE_AFTER_OPEN_INTERPOLATION, v);
    }

    public boolean isNewLineBeforeCloseInterpolation() {
        return getBoolean(WRAP_NEW_LINE_BEFORE_CLOSE_INTERPOLATION, true);
    }

    public void setNewLineBeforeCloseInterpolation(boolean v) {
        setBoolean(WRAP_NEW_LINE_BEFORE_CLOSE_INTERPOLATION, v);
    }

    public List<String> getMatchingRules() {
        return matchingRules;
    }

    // ==========================================
    // Sample Code matching media_1790574549424.png & media_1790574558704.png verbatim
    // ==========================================
    public static final String SAMPLE_ANGULAR_HTML = """
<human-profile *ngIf="user.isHuman else robot"
               [data]="user"/>
<ng-template #robot>
   <p *ngIf="user.isRobot"> {{ user.name }} </p>
</ng-template>

@if (user.isHuman) {
   <human-profile [data]="user"/>
} @else if (user.isRobot) {
   {{
      user.name
   }}
} @else {
   <p>The profile is unknown!</p>
}
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "Angular HTML template";
            }

            @Override
            public String getDisplayName() {
                return "Angular HTML template";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Spaces", "Wrapping and Braces", "Arrangement");
            }

            @Override
            public boolean hasPreview(String tabName) {
                // Spaces and Wrapping have preview; Arrangement has full-width rules panel
                return !"Arrangement".equals(tabName);
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new AngularHtmlCodeStyleSettings("Angular HTML template");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_ANGULAR_HTML;
            }

            @Override
            public String getSampleCode(String tabName) {
                return SAMPLE_ANGULAR_HTML;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Spaces".equals(tabName)) {
                    CodeStyleGroup within = CodeStyleGroup.collapsible("Within");
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_INTERPOLATIONS, "Interpolations", true));
                    customizer.addGroup(within);

                } else if ("Wrapping and Braces".equals(tabName)) {
                    CodeStyleGroup general = CodeStyleGroup.flat("General");
                    general.addOption(CodeStyleOption.combo(
                            WRAP_VISUAL_GUIDES,
                            "Visual guides",
                            List.of("Default: None", "None", "80", "120"),
                            "Default: None"
                    ));
                    customizer.addGroup(general);

                    CodeStyleGroup interpolations = CodeStyleGroup.collapsibleWithCombo(
                            "Interpolations",
                            WRAP_INTERPOLATIONS,
                            WRAP_OPTIONS,
                            "Do not wrap"
                    );
                    interpolations.addOption(CodeStyleOption.checkbox(
                            WRAP_NEW_LINE_AFTER_OPEN_INTERPOLATION,
                            "New line after '{{'",
                            true
                    ));
                    interpolations.addOption(CodeStyleOption.checkbox(
                            WRAP_NEW_LINE_BEFORE_CLOSE_INTERPOLATION,
                            "New line before '}}'",
                            true
                    ));
                    customizer.addGroup(interpolations);
                }
            }
        };
    }
}
