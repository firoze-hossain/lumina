package dev.lumina.folding;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for IntelliJ IDEA-style Editor > General > Code Folding settings.
 * Backed by ~/.lumina/lumina.properties, supporting listeners, dirty tracking, and real-time editor updates.
 */
public final class CodeFoldingSettings {

    public enum FoldingArrowsMode {
        ALWAYS("Always"),
        ON_MOUSE_HOVER("On mouse hover");

        private final String label;

        FoldingArrowsMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static FoldingArrowsMode fromLabel(String label) {
            for (FoldingArrowsMode m : values()) {
                if (m.label.equalsIgnoreCase(label) || m.name().equalsIgnoreCase(label)) {
                    return m;
                }
            }
            return ON_MOUSE_HOVER;
        }
    }

    private static final CodeFoldingSettings INSTANCE = new CodeFoldingSettings();

    public static CodeFoldingSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(CodeFoldingSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // Top controls
    private boolean showFoldingArrows = true;
    private FoldingArrowsMode codeFoldingArrowsMode = FoldingArrowsMode.ON_MOUSE_HOVER;
    private boolean showBottomArrows = false;

    // General
    private boolean foldFileHeader = true;
    private boolean foldImports = true;
    private boolean foldDocComments = false;
    private boolean foldMethodBodies = false;
    private boolean foldCustomRegions = false;

    // JPA QL
    private boolean foldJpaQueries = true;

    // JSON
    private boolean jsonShowKeyCount = true;
    private boolean jsonShowFirstKey = false;

    // Java
    private boolean javaOneLineMethods = true;
    private boolean javaSimplePropertyAccessors = false;
    private boolean javaInnerClasses = false;
    private boolean javaAnonymousClasses = false;
    private boolean javaAnnotations = false;
    private boolean javaClosures = true;
    private boolean javaGenericParams = true;
    private boolean javaReplaceVar = false;
    private boolean javaI18nStrings = true;
    private boolean javaSuppressWarnings = true;
    private boolean javaEndOfLineComments = false;
    private boolean javaMultilineComments = false;

    // JavaScript
    private boolean jsOneLineFunctions = false;
    private boolean jsObjectLiterals = false;
    private boolean jsArrayLiterals = false;
    private boolean jsXmlLiterals = false;

    // Kubernetes
    private boolean k8sHelmValueReferences = true;
    private boolean k8sEnvVarYaml = true;
    private boolean k8sExecActionYaml = true;

    // Markdown
    private boolean markdownCollapseFrontMatter = true;
    private boolean markdownCollapseLinks = true;
    private boolean markdownCollapseTables = false;
    private boolean markdownCollapseCodeFences = false;
    private boolean markdownCollapseToc = true;

    // PHP
    private boolean phpClassBody = false;
    private boolean phpImports = true;
    private boolean phpMethodBody = false;
    private boolean phpFunctionBody = false;
    private boolean phpTags = false;
    private boolean phpHeredoc = false;
    private boolean phpAttribute = false;
    private boolean phpAttributeList = false;

    // Python
    private boolean pythonLongStringLiterals = false;
    private boolean pythonLongCollectionLiterals = false;
    private boolean pythonSequentialComments = false;
    private boolean pythonTypeAnnotations = false;

    // Ruby i18n
    private boolean rubyI18nStrings = true;

    // Rust
    private boolean rustOneLineMethods = true;

    // SQL
    private boolean sqlUnderscoresInNumericLiterals = false;

    // Scala
    private boolean scalaBlockComments = false;
    private boolean scalaMethodCallBodies = false;
    private boolean scalaTemplateDefinitionBodies = false;
    private boolean scalaDefinitionBodies = false;
    private boolean scalaTypeLambdas = false;
    private boolean scalaPackages = false;
    private boolean scalaMultiLineStrings = false;
    private boolean scalaCustomRegions = false;
    private boolean scalaMultiLineBlocks = false;
    private boolean scalaShowOutlineForMultiLineBlocks = false;

    // XML
    private boolean xmlTags = false;
    private boolean xmlHtmlStyleAttribute = true;
    private boolean xmlEntities = true;
    private boolean xmlDataUris = true;

    // YAML
    private boolean yamlLimitFoldedKeysAndValues = true;
    private int yamlLimitCharacters = 20;

    // Go
    private boolean goOneLineIfErrorHandling = true;
    private boolean goOneLineFunctionsSingleReturn = true;
    private boolean goOneLineCaseClauses = true;
    private boolean goEmptyFunctions = true;
    private boolean goEmptyStructOrInterface = true;
    private boolean goFormattedStrings = false;

    public CodeFoldingSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        showFoldingArrows = true;
        codeFoldingArrowsMode = FoldingArrowsMode.ON_MOUSE_HOVER;
        showBottomArrows = false;

        foldFileHeader = true;
        foldImports = true;
        foldDocComments = false;
        foldMethodBodies = false;
        foldCustomRegions = false;

        foldJpaQueries = true;

        jsonShowKeyCount = true;
        jsonShowFirstKey = false;

        javaOneLineMethods = true;
        javaSimplePropertyAccessors = false;
        javaInnerClasses = false;
        javaAnonymousClasses = false;
        javaAnnotations = false;
        javaClosures = true;
        javaGenericParams = true;
        javaReplaceVar = false;
        javaI18nStrings = true;
        javaSuppressWarnings = true;
        javaEndOfLineComments = false;
        javaMultilineComments = false;

        jsOneLineFunctions = false;
        jsObjectLiterals = false;
        jsArrayLiterals = false;
        jsXmlLiterals = false;

        k8sHelmValueReferences = true;
        k8sEnvVarYaml = true;
        k8sExecActionYaml = true;

        markdownCollapseFrontMatter = true;
        markdownCollapseLinks = true;
        markdownCollapseTables = false;
        markdownCollapseCodeFences = false;
        markdownCollapseToc = true;

        phpClassBody = false;
        phpImports = true;
        phpMethodBody = false;
        phpFunctionBody = false;
        phpTags = false;
        phpHeredoc = false;
        phpAttribute = false;
        phpAttributeList = false;

        pythonLongStringLiterals = false;
        pythonLongCollectionLiterals = false;
        pythonSequentialComments = false;
        pythonTypeAnnotations = false;

        rubyI18nStrings = true;
        rustOneLineMethods = true;
        sqlUnderscoresInNumericLiterals = false;

        scalaBlockComments = false;
        scalaMethodCallBodies = false;
        scalaTemplateDefinitionBodies = false;
        scalaDefinitionBodies = false;
        scalaTypeLambdas = false;
        scalaPackages = false;
        scalaMultiLineStrings = false;
        scalaCustomRegions = false;
        scalaMultiLineBlocks = false;
        scalaShowOutlineForMultiLineBlocks = false;

        xmlTags = false;
        xmlHtmlStyleAttribute = true;
        xmlEntities = true;
        xmlDataUris = true;

        yamlLimitFoldedKeysAndValues = true;
        yamlLimitCharacters = 20;

        goOneLineIfErrorHandling = true;
        goOneLineFunctionsSingleReturn = true;
        goOneLineCaseClauses = true;
        goEmptyFunctions = true;
        goEmptyStructOrInterface = true;
        goFormattedStrings = false;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        showFoldingArrows = getBool("show_arrows", showFoldingArrows);
        codeFoldingArrowsMode = FoldingArrowsMode.fromLabel(getStr("arrows_mode", codeFoldingArrowsMode.getLabel()));
        showBottomArrows = getBool("show_bottom_arrows", showBottomArrows);

        foldFileHeader = getBool("general.file_header", foldFileHeader);
        foldImports = getBool("general.imports", foldImports);
        foldDocComments = getBool("general.doc_comments", foldDocComments);
        foldMethodBodies = getBool("general.method_bodies", foldMethodBodies);
        foldCustomRegions = getBool("general.custom_regions", foldCustomRegions);

        foldJpaQueries = getBool("jpa_ql.queries", foldJpaQueries);

        jsonShowKeyCount = getBool("json.key_count", jsonShowKeyCount);
        jsonShowFirstKey = getBool("json.first_key", jsonShowFirstKey);

        javaOneLineMethods = getBool("java.one_line_methods", javaOneLineMethods);
        javaSimplePropertyAccessors = getBool("java.simple_property_accessors", javaSimplePropertyAccessors);
        javaInnerClasses = getBool("java.inner_classes", javaInnerClasses);
        javaAnonymousClasses = getBool("java.anonymous_classes", javaAnonymousClasses);
        javaAnnotations = getBool("java.annotations", javaAnnotations);
        javaClosures = getBool("java.closures", javaClosures);
        javaGenericParams = getBool("java.generic_params", javaGenericParams);
        javaReplaceVar = getBool("java.replace_var", javaReplaceVar);
        javaI18nStrings = getBool("java.i18n_strings", javaI18nStrings);
        javaSuppressWarnings = getBool("java.suppress_warnings", javaSuppressWarnings);
        javaEndOfLineComments = getBool("java.end_of_line_comments", javaEndOfLineComments);
        javaMultilineComments = getBool("java.multiline_comments", javaMultilineComments);

        jsOneLineFunctions = getBool("js.one_line_functions", jsOneLineFunctions);
        jsObjectLiterals = getBool("js.object_literals", jsObjectLiterals);
        jsArrayLiterals = getBool("js.array_literals", jsArrayLiterals);
        jsXmlLiterals = getBool("js.xml_literals", jsXmlLiterals);

        k8sHelmValueReferences = getBool("k8s.helm_value_references", k8sHelmValueReferences);
        k8sEnvVarYaml = getBool("k8s.env_var_yaml", k8sEnvVarYaml);
        k8sExecActionYaml = getBool("k8s.exec_action_yaml", k8sExecActionYaml);

        markdownCollapseFrontMatter = getBool("markdown.collapse_front_matter", markdownCollapseFrontMatter);
        markdownCollapseLinks = getBool("markdown.collapse_links", markdownCollapseLinks);
        markdownCollapseTables = getBool("markdown.collapse_tables", markdownCollapseTables);
        markdownCollapseCodeFences = getBool("markdown.collapse_code_fences", markdownCollapseCodeFences);
        markdownCollapseToc = getBool("markdown.collapse_toc", markdownCollapseToc);

        phpClassBody = getBool("php.class_body", phpClassBody);
        phpImports = getBool("php.imports", phpImports);
        phpMethodBody = getBool("php.method_body", phpMethodBody);
        phpFunctionBody = getBool("php.function_body", phpFunctionBody);
        phpTags = getBool("php.tags", phpTags);
        phpHeredoc = getBool("php.heredoc", phpHeredoc);
        phpAttribute = getBool("php.attribute", phpAttribute);
        phpAttributeList = getBool("php.attribute_list", phpAttributeList);

        pythonLongStringLiterals = getBool("python.long_string_literals", pythonLongStringLiterals);
        pythonLongCollectionLiterals = getBool("python.long_collection_literals", pythonLongCollectionLiterals);
        pythonSequentialComments = getBool("python.sequential_comments", pythonSequentialComments);
        pythonTypeAnnotations = getBool("python.type_annotations", pythonTypeAnnotations);

        rubyI18nStrings = getBool("ruby_i18n.strings", rubyI18nStrings);
        rustOneLineMethods = getBool("rust.one_line_methods", rustOneLineMethods);
        sqlUnderscoresInNumericLiterals = getBool("sql.underscores_numeric", sqlUnderscoresInNumericLiterals);

        scalaBlockComments = getBool("scala.block_comments", scalaBlockComments);
        scalaMethodCallBodies = getBool("scala.method_call_bodies", scalaMethodCallBodies);
        scalaTemplateDefinitionBodies = getBool("scala.template_definition_bodies", scalaTemplateDefinitionBodies);
        scalaDefinitionBodies = getBool("scala.definition_bodies", scalaDefinitionBodies);
        scalaTypeLambdas = getBool("scala.type_lambdas", scalaTypeLambdas);
        scalaPackages = getBool("scala.packages", scalaPackages);
        scalaMultiLineStrings = getBool("scala.multi_line_strings", scalaMultiLineStrings);
        scalaCustomRegions = getBool("scala.custom_regions", scalaCustomRegions);
        scalaMultiLineBlocks = getBool("scala.multi_line_blocks", scalaMultiLineBlocks);
        scalaShowOutlineForMultiLineBlocks = getBool("scala.show_outline_multi_line_blocks", scalaShowOutlineForMultiLineBlocks);

        xmlTags = getBool("xml.tags", xmlTags);
        xmlHtmlStyleAttribute = getBool("xml.html_style_attribute", xmlHtmlStyleAttribute);
        xmlEntities = getBool("xml.entities", xmlEntities);
        xmlDataUris = getBool("xml.data_uris", xmlDataUris);

        yamlLimitFoldedKeysAndValues = getBool("yaml.limit_folded_keys_values", yamlLimitFoldedKeysAndValues);
        yamlLimitCharacters = getInt("yaml.limit_characters", yamlLimitCharacters);

        goOneLineIfErrorHandling = getBool("go.one_line_if_error_handling", goOneLineIfErrorHandling);
        goOneLineFunctionsSingleReturn = getBool("go.one_line_functions_single_return", goOneLineFunctionsSingleReturn);
        goOneLineCaseClauses = getBool("go.one_line_case_clauses", goOneLineCaseClauses);
        goEmptyFunctions = getBool("go.empty_functions", goEmptyFunctions);
        goEmptyStructOrInterface = getBool("go.empty_struct_or_interface", goEmptyStructOrInterface);
        goFormattedStrings = getBool("go.formatted_strings", goFormattedStrings);
    }

    public void save() {
        putBool("show_arrows", showFoldingArrows);
        putStr("arrows_mode", codeFoldingArrowsMode.getLabel());
        putBool("show_bottom_arrows", showBottomArrows);

        putBool("general.file_header", foldFileHeader);
        putBool("general.imports", foldImports);
        putBool("general.doc_comments", foldDocComments);
        putBool("general.method_bodies", foldMethodBodies);
        putBool("general.custom_regions", foldCustomRegions);

        putBool("jpa_ql.queries", foldJpaQueries);

        putBool("json.key_count", jsonShowKeyCount);
        putBool("json.first_key", jsonShowFirstKey);

        putBool("java.one_line_methods", javaOneLineMethods);
        putBool("java.simple_property_accessors", javaSimplePropertyAccessors);
        putBool("java.inner_classes", javaInnerClasses);
        putBool("java.anonymous_classes", javaAnonymousClasses);
        putBool("java.annotations", javaAnnotations);
        putBool("java.closures", javaClosures);
        putBool("java.generic_params", javaGenericParams);
        putBool("java.replace_var", javaReplaceVar);
        putBool("java.i18n_strings", javaI18nStrings);
        putBool("java.suppress_warnings", javaSuppressWarnings);
        putBool("java.end_of_line_comments", javaEndOfLineComments);
        putBool("java.multiline_comments", javaMultilineComments);

        putBool("js.one_line_functions", jsOneLineFunctions);
        putBool("js.object_literals", jsObjectLiterals);
        putBool("js.array_literals", jsArrayLiterals);
        putBool("js.xml_literals", jsXmlLiterals);

        putBool("k8s.helm_value_references", k8sHelmValueReferences);
        putBool("k8s.env_var_yaml", k8sEnvVarYaml);
        putBool("k8s.exec_action_yaml", k8sExecActionYaml);

        putBool("markdown.collapse_front_matter", markdownCollapseFrontMatter);
        putBool("markdown.collapse_links", markdownCollapseLinks);
        putBool("markdown.collapse_tables", markdownCollapseTables);
        putBool("markdown.collapse_code_fences", markdownCollapseCodeFences);
        putBool("markdown.collapse_toc", markdownCollapseToc);

        putBool("php.class_body", phpClassBody);
        putBool("php.imports", phpImports);
        putBool("php.method_body", phpMethodBody);
        putBool("php.function_body", phpFunctionBody);
        putBool("php.tags", phpTags);
        putBool("php.heredoc", phpHeredoc);
        putBool("php.attribute", phpAttribute);
        putBool("php.attribute_list", phpAttributeList);

        putBool("python.long_string_literals", pythonLongStringLiterals);
        putBool("python.long_collection_literals", pythonLongCollectionLiterals);
        putBool("python.sequential_comments", pythonSequentialComments);
        putBool("python.type_annotations", pythonTypeAnnotations);

        putBool("ruby_i18n.strings", rubyI18nStrings);
        putBool("rust.one_line_methods", rustOneLineMethods);
        putBool("sql.underscores_numeric", sqlUnderscoresInNumericLiterals);

        putBool("scala.block_comments", scalaBlockComments);
        putBool("scala.method_call_bodies", scalaMethodCallBodies);
        putBool("scala.template_definition_bodies", scalaTemplateDefinitionBodies);
        putBool("scala.definition_bodies", scalaDefinitionBodies);
        putBool("scala.type_lambdas", scalaTypeLambdas);
        putBool("scala.packages", scalaPackages);
        putBool("scala.multi_line_strings", scalaMultiLineStrings);
        putBool("scala.custom_regions", scalaCustomRegions);
        putBool("scala.multi_line_blocks", scalaMultiLineBlocks);
        putBool("scala.show_outline_multi_line_blocks", scalaShowOutlineForMultiLineBlocks);

        putBool("xml.tags", xmlTags);
        putBool("xml.html_style_attribute", xmlHtmlStyleAttribute);
        putBool("xml.entities", xmlEntities);
        putBool("xml.data_uris", xmlDataUris);

        putBool("yaml.limit_folded_keys_values", yamlLimitFoldedKeysAndValues);
        putInt("yaml.limit_characters", yamlLimitCharacters);

        putBool("go.one_line_if_error_handling", goOneLineIfErrorHandling);
        putBool("go.one_line_functions_single_return", goOneLineFunctionsSingleReturn);
        putBool("go.one_line_case_clauses", goOneLineCaseClauses);
        putBool("go.empty_functions", goEmptyFunctions);
        putBool("go.empty_struct_or_interface", goEmptyStructOrInterface);
        putBool("go.formatted_strings", goFormattedStrings);

        notifyListeners();
    }

    public void copyFrom(CodeFoldingSettings o) {
        this.showFoldingArrows = o.showFoldingArrows;
        this.codeFoldingArrowsMode = o.codeFoldingArrowsMode;
        this.showBottomArrows = o.showBottomArrows;

        this.foldFileHeader = o.foldFileHeader;
        this.foldImports = o.foldImports;
        this.foldDocComments = o.foldDocComments;
        this.foldMethodBodies = o.foldMethodBodies;
        this.foldCustomRegions = o.foldCustomRegions;

        this.foldJpaQueries = o.foldJpaQueries;
        this.jsonShowKeyCount = o.jsonShowKeyCount;
        this.jsonShowFirstKey = o.jsonShowFirstKey;

        this.javaOneLineMethods = o.javaOneLineMethods;
        this.javaSimplePropertyAccessors = o.javaSimplePropertyAccessors;
        this.javaInnerClasses = o.javaInnerClasses;
        this.javaAnonymousClasses = o.javaAnonymousClasses;
        this.javaAnnotations = o.javaAnnotations;
        this.javaClosures = o.javaClosures;
        this.javaGenericParams = o.javaGenericParams;
        this.javaReplaceVar = o.javaReplaceVar;
        this.javaI18nStrings = o.javaI18nStrings;
        this.javaSuppressWarnings = o.javaSuppressWarnings;
        this.javaEndOfLineComments = o.javaEndOfLineComments;
        this.javaMultilineComments = o.javaMultilineComments;

        this.jsOneLineFunctions = o.jsOneLineFunctions;
        this.jsObjectLiterals = o.jsObjectLiterals;
        this.jsArrayLiterals = o.jsArrayLiterals;
        this.jsXmlLiterals = o.jsXmlLiterals;

        this.k8sHelmValueReferences = o.k8sHelmValueReferences;
        this.k8sEnvVarYaml = o.k8sEnvVarYaml;
        this.k8sExecActionYaml = o.k8sExecActionYaml;

        this.markdownCollapseFrontMatter = o.markdownCollapseFrontMatter;
        this.markdownCollapseLinks = o.markdownCollapseLinks;
        this.markdownCollapseTables = o.markdownCollapseTables;
        this.markdownCollapseCodeFences = o.markdownCollapseCodeFences;
        this.markdownCollapseToc = o.markdownCollapseToc;

        this.phpClassBody = o.phpClassBody;
        this.phpImports = o.phpImports;
        this.phpMethodBody = o.phpMethodBody;
        this.phpFunctionBody = o.phpFunctionBody;
        this.phpTags = o.phpTags;
        this.phpHeredoc = o.phpHeredoc;
        this.phpAttribute = o.phpAttribute;
        this.phpAttributeList = o.phpAttributeList;

        this.pythonLongStringLiterals = o.pythonLongStringLiterals;
        this.pythonLongCollectionLiterals = o.pythonLongCollectionLiterals;
        this.pythonSequentialComments = o.pythonSequentialComments;
        this.pythonTypeAnnotations = o.pythonTypeAnnotations;

        this.rubyI18nStrings = o.rubyI18nStrings;
        this.rustOneLineMethods = o.rustOneLineMethods;
        this.sqlUnderscoresInNumericLiterals = o.sqlUnderscoresInNumericLiterals;

        this.scalaBlockComments = o.scalaBlockComments;
        this.scalaMethodCallBodies = o.scalaMethodCallBodies;
        this.scalaTemplateDefinitionBodies = o.scalaTemplateDefinitionBodies;
        this.scalaDefinitionBodies = o.scalaDefinitionBodies;
        this.scalaTypeLambdas = o.scalaTypeLambdas;
        this.scalaPackages = o.scalaPackages;
        this.scalaMultiLineStrings = o.scalaMultiLineStrings;
        this.scalaCustomRegions = o.scalaCustomRegions;
        this.scalaMultiLineBlocks = o.scalaMultiLineBlocks;
        this.scalaShowOutlineForMultiLineBlocks = o.scalaShowOutlineForMultiLineBlocks;

        this.xmlTags = o.xmlTags;
        this.xmlHtmlStyleAttribute = o.xmlHtmlStyleAttribute;
        this.xmlEntities = o.xmlEntities;
        this.xmlDataUris = o.xmlDataUris;

        this.yamlLimitFoldedKeysAndValues = o.yamlLimitFoldedKeysAndValues;
        this.yamlLimitCharacters = o.yamlLimitCharacters;

        this.goOneLineIfErrorHandling = o.goOneLineIfErrorHandling;
        this.goOneLineFunctionsSingleReturn = o.goOneLineFunctionsSingleReturn;
        this.goOneLineCaseClauses = o.goOneLineCaseClauses;
        this.goEmptyFunctions = o.goEmptyFunctions;
        this.goEmptyStructOrInterface = o.goEmptyStructOrInterface;
        this.goFormattedStrings = o.goFormattedStrings;
    }

    public CodeFoldingSettings copy() {
        CodeFoldingSettings c = new CodeFoldingSettings();
        c.copyFrom(this);
        return c;
    }

    public boolean isModified(CodeFoldingSettings o) {
        if (o == null) return true;
        return this.showFoldingArrows != o.showFoldingArrows
                || this.codeFoldingArrowsMode != o.codeFoldingArrowsMode
                || this.showBottomArrows != o.showBottomArrows
                || this.foldFileHeader != o.foldFileHeader
                || this.foldImports != o.foldImports
                || this.foldDocComments != o.foldDocComments
                || this.foldMethodBodies != o.foldMethodBodies
                || this.foldCustomRegions != o.foldCustomRegions
                || this.foldJpaQueries != o.foldJpaQueries
                || this.jsonShowKeyCount != o.jsonShowKeyCount
                || this.jsonShowFirstKey != o.jsonShowFirstKey
                || this.javaOneLineMethods != o.javaOneLineMethods
                || this.javaSimplePropertyAccessors != o.javaSimplePropertyAccessors
                || this.javaInnerClasses != o.javaInnerClasses
                || this.javaAnonymousClasses != o.javaAnonymousClasses
                || this.javaAnnotations != o.javaAnnotations
                || this.javaClosures != o.javaClosures
                || this.javaGenericParams != o.javaGenericParams
                || this.javaReplaceVar != o.javaReplaceVar
                || this.javaI18nStrings != o.javaI18nStrings
                || this.javaSuppressWarnings != o.javaSuppressWarnings
                || this.javaEndOfLineComments != o.javaEndOfLineComments
                || this.javaMultilineComments != o.javaMultilineComments
                || this.jsOneLineFunctions != o.jsOneLineFunctions
                || this.jsObjectLiterals != o.jsObjectLiterals
                || this.jsArrayLiterals != o.jsArrayLiterals
                || this.jsXmlLiterals != o.jsXmlLiterals
                || this.k8sHelmValueReferences != o.k8sHelmValueReferences
                || this.k8sEnvVarYaml != o.k8sEnvVarYaml
                || this.k8sExecActionYaml != o.k8sExecActionYaml
                || this.markdownCollapseFrontMatter != o.markdownCollapseFrontMatter
                || this.markdownCollapseLinks != o.markdownCollapseLinks
                || this.markdownCollapseTables != o.markdownCollapseTables
                || this.markdownCollapseCodeFences != o.markdownCollapseCodeFences
                || this.markdownCollapseToc != o.markdownCollapseToc
                || this.phpClassBody != o.phpClassBody
                || this.phpImports != o.phpImports
                || this.phpMethodBody != o.phpMethodBody
                || this.phpFunctionBody != o.phpFunctionBody
                || this.phpTags != o.phpTags
                || this.phpHeredoc != o.phpHeredoc
                || this.phpAttribute != o.phpAttribute
                || this.phpAttributeList != o.phpAttributeList
                || this.pythonLongStringLiterals != o.pythonLongStringLiterals
                || this.pythonLongCollectionLiterals != o.pythonLongCollectionLiterals
                || this.pythonSequentialComments != o.pythonSequentialComments
                || this.pythonTypeAnnotations != o.pythonTypeAnnotations
                || this.rubyI18nStrings != o.rubyI18nStrings
                || this.rustOneLineMethods != o.rustOneLineMethods
                || this.sqlUnderscoresInNumericLiterals != o.sqlUnderscoresInNumericLiterals
                || this.scalaBlockComments != o.scalaBlockComments
                || this.scalaMethodCallBodies != o.scalaMethodCallBodies
                || this.scalaTemplateDefinitionBodies != o.scalaTemplateDefinitionBodies
                || this.scalaDefinitionBodies != o.scalaDefinitionBodies
                || this.scalaTypeLambdas != o.scalaTypeLambdas
                || this.scalaPackages != o.scalaPackages
                || this.scalaMultiLineStrings != o.scalaMultiLineStrings
                || this.scalaCustomRegions != o.scalaCustomRegions
                || this.scalaMultiLineBlocks != o.scalaMultiLineBlocks
                || this.scalaShowOutlineForMultiLineBlocks != o.scalaShowOutlineForMultiLineBlocks
                || this.xmlTags != o.xmlTags
                || this.xmlHtmlStyleAttribute != o.xmlHtmlStyleAttribute
                || this.xmlEntities != o.xmlEntities
                || this.xmlDataUris != o.xmlDataUris
                || this.yamlLimitFoldedKeysAndValues != o.yamlLimitFoldedKeysAndValues
                || this.yamlLimitCharacters != o.yamlLimitCharacters
                || this.goOneLineIfErrorHandling != o.goOneLineIfErrorHandling
                || this.goOneLineFunctionsSingleReturn != o.goOneLineFunctionsSingleReturn
                || this.goOneLineCaseClauses != o.goOneLineCaseClauses
                || this.goEmptyFunctions != o.goEmptyFunctions
                || this.goEmptyStructOrInterface != o.goEmptyStructOrInterface
                || this.goFormattedStrings != o.goFormattedStrings;
    }

    private boolean getBool(String key, boolean def) {
        String val = Settings.get("editor.folding." + key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private void putBool(String key, boolean val) {
        Settings.put("editor.folding." + key, String.valueOf(val));
    }

    private int getInt(String key, int def) {
        String val = Settings.get("editor.folding." + key);
        if (val != null) {
            try { return Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private void putInt(String key, int val) {
        Settings.put("editor.folding." + key, String.valueOf(val));
    }

    private String getStr(String key, String def) {
        String val = Settings.get("editor.folding." + key);
        return val != null ? val : def;
    }

    private void putStr(String key, String val) {
        Settings.put("editor.folding." + key, val != null ? val : "");
    }

    // Getters and Setters
    public boolean isShowFoldingArrows() { return showFoldingArrows; }
    public void setShowFoldingArrows(boolean showFoldingArrows) { this.showFoldingArrows = showFoldingArrows; }

    public FoldingArrowsMode getCodeFoldingArrowsMode() { return codeFoldingArrowsMode; }
    public void setCodeFoldingArrowsMode(FoldingArrowsMode mode) { this.codeFoldingArrowsMode = mode; }

    public boolean isShowBottomArrows() { return showBottomArrows; }
    public void setShowBottomArrows(boolean showBottomArrows) { this.showBottomArrows = showBottomArrows; }

    // General
    public boolean isFoldFileHeader() { return foldFileHeader; }
    public void setFoldFileHeader(boolean foldFileHeader) { this.foldFileHeader = foldFileHeader; }

    public boolean isFoldImports() { return foldImports; }
    public void setFoldImports(boolean foldImports) { this.foldImports = foldImports; }

    public boolean isFoldDocComments() { return foldDocComments; }
    public void setFoldDocComments(boolean foldDocComments) { this.foldDocComments = foldDocComments; }

    public boolean isFoldMethodBodies() { return foldMethodBodies; }
    public void setFoldMethodBodies(boolean foldMethodBodies) { this.foldMethodBodies = foldMethodBodies; }

    public boolean isFoldCustomRegions() { return foldCustomRegions; }
    public void setFoldCustomRegions(boolean foldCustomRegions) { this.foldCustomRegions = foldCustomRegions; }

    // Backward-compatibility getters for CodeFoldingScanner
    public boolean isFoldImportsByDefault() { return isFoldImports(); }
    public void setFoldImportsByDefault(boolean v) { setFoldImports(v); }

    public boolean isFoldDocCommentsByDefault() { return isFoldDocComments(); }
    public void setFoldDocCommentsByDefault(boolean v) { setFoldDocComments(v); }

    public boolean isFoldMethodBodiesByDefault() { return isFoldMethodBodies(); }
    public void setFoldMethodBodiesByDefault(boolean v) { setFoldMethodBodies(v); }

    public boolean isFoldAnnotationsByDefault() { return isJavaAnnotations(); }
    public void setFoldAnnotationsByDefault(boolean v) { setJavaAnnotations(v); }

    public boolean isFoldCustomRegionsByDefault() { return isFoldCustomRegions(); }
    public void setFoldCustomRegionsByDefault(boolean v) { setFoldCustomRegions(v); }

    // JPA QL
    public boolean isFoldJpaQueries() { return foldJpaQueries; }
    public void setFoldJpaQueries(boolean foldJpaQueries) { this.foldJpaQueries = foldJpaQueries; }

    // JSON
    public boolean isJsonShowKeyCount() { return jsonShowKeyCount; }
    public void setJsonShowKeyCount(boolean jsonShowKeyCount) { this.jsonShowKeyCount = jsonShowKeyCount; }

    public boolean isJsonShowFirstKey() { return jsonShowFirstKey; }
    public void setJsonShowFirstKey(boolean jsonShowFirstKey) { this.jsonShowFirstKey = jsonShowFirstKey; }

    // Java
    public boolean isJavaOneLineMethods() { return javaOneLineMethods; }
    public void setJavaOneLineMethods(boolean javaOneLineMethods) { this.javaOneLineMethods = javaOneLineMethods; }

    public boolean isJavaSimplePropertyAccessors() { return javaSimplePropertyAccessors; }
    public void setJavaSimplePropertyAccessors(boolean javaSimplePropertyAccessors) { this.javaSimplePropertyAccessors = javaSimplePropertyAccessors; }

    public boolean isJavaInnerClasses() { return javaInnerClasses; }
    public void setJavaInnerClasses(boolean javaInnerClasses) { this.javaInnerClasses = javaInnerClasses; }

    public boolean isJavaAnonymousClasses() { return javaAnonymousClasses; }
    public void setJavaAnonymousClasses(boolean javaAnonymousClasses) { this.javaAnonymousClasses = javaAnonymousClasses; }

    public boolean isJavaAnnotations() { return javaAnnotations; }
    public void setJavaAnnotations(boolean javaAnnotations) { this.javaAnnotations = javaAnnotations; }

    public boolean isJavaClosures() { return javaClosures; }
    public void setJavaClosures(boolean javaClosures) { this.javaClosures = javaClosures; }

    public boolean isJavaGenericParams() { return javaGenericParams; }
    public void setJavaGenericParams(boolean javaGenericParams) { this.javaGenericParams = javaGenericParams; }

    public boolean isJavaReplaceVar() { return javaReplaceVar; }
    public void setJavaReplaceVar(boolean javaReplaceVar) { this.javaReplaceVar = javaReplaceVar; }

    public boolean isJavaI18nStrings() { return javaI18nStrings; }
    public void setJavaI18nStrings(boolean javaI18nStrings) { this.javaI18nStrings = javaI18nStrings; }

    public boolean isJavaSuppressWarnings() { return javaSuppressWarnings; }
    public void setJavaSuppressWarnings(boolean javaSuppressWarnings) { this.javaSuppressWarnings = javaSuppressWarnings; }

    public boolean isJavaEndOfLineComments() { return javaEndOfLineComments; }
    public void setJavaEndOfLineComments(boolean javaEndOfLineComments) { this.javaEndOfLineComments = javaEndOfLineComments; }

    public boolean isJavaMultilineComments() { return javaMultilineComments; }
    public void setJavaMultilineComments(boolean javaMultilineComments) { this.javaMultilineComments = javaMultilineComments; }

    // JavaScript
    public boolean isJsOneLineFunctions() { return jsOneLineFunctions; }
    public void setJsOneLineFunctions(boolean jsOneLineFunctions) { this.jsOneLineFunctions = jsOneLineFunctions; }

    public boolean isJsObjectLiterals() { return jsObjectLiterals; }
    public void setJsObjectLiterals(boolean jsObjectLiterals) { this.jsObjectLiterals = jsObjectLiterals; }

    public boolean isJsArrayLiterals() { return jsArrayLiterals; }
    public void setJsArrayLiterals(boolean jsArrayLiterals) { this.jsArrayLiterals = jsArrayLiterals; }

    public boolean isJsXmlLiterals() { return jsXmlLiterals; }
    public void setJsXmlLiterals(boolean jsXmlLiterals) { this.jsXmlLiterals = jsXmlLiterals; }

    // Kubernetes
    public boolean isK8sHelmValueReferences() { return k8sHelmValueReferences; }
    public void setK8sHelmValueReferences(boolean k8sHelmValueReferences) { this.k8sHelmValueReferences = k8sHelmValueReferences; }

    public boolean isK8sEnvVarYaml() { return k8sEnvVarYaml; }
    public void setK8sEnvVarYaml(boolean k8sEnvVarYaml) { this.k8sEnvVarYaml = k8sEnvVarYaml; }

    public boolean isK8sExecActionYaml() { return k8sExecActionYaml; }
    public void setK8sExecActionYaml(boolean k8sExecActionYaml) { this.k8sExecActionYaml = k8sExecActionYaml; }

    // Markdown
    public boolean isMarkdownCollapseFrontMatter() { return markdownCollapseFrontMatter; }
    public void setMarkdownCollapseFrontMatter(boolean markdownCollapseFrontMatter) { this.markdownCollapseFrontMatter = markdownCollapseFrontMatter; }

    public boolean isMarkdownCollapseLinks() { return markdownCollapseLinks; }
    public void setMarkdownCollapseLinks(boolean markdownCollapseLinks) { this.markdownCollapseLinks = markdownCollapseLinks; }

    public boolean isMarkdownCollapseTables() { return markdownCollapseTables; }
    public void setMarkdownCollapseTables(boolean markdownCollapseTables) { this.markdownCollapseTables = markdownCollapseTables; }

    public boolean isMarkdownCollapseCodeFences() { return markdownCollapseCodeFences; }
    public void setMarkdownCollapseCodeFences(boolean markdownCollapseCodeFences) { this.markdownCollapseCodeFences = markdownCollapseCodeFences; }

    public boolean isMarkdownCollapseToc() { return markdownCollapseToc; }
    public void setMarkdownCollapseToc(boolean markdownCollapseToc) { this.markdownCollapseToc = markdownCollapseToc; }

    // PHP
    public boolean isPhpClassBody() { return phpClassBody; }
    public void setPhpClassBody(boolean phpClassBody) { this.phpClassBody = phpClassBody; }

    public boolean isPhpImports() { return phpImports; }
    public void setPhpImports(boolean phpImports) { this.phpImports = phpImports; }

    public boolean isPhpMethodBody() { return phpMethodBody; }
    public void setPhpMethodBody(boolean phpMethodBody) { this.phpMethodBody = phpMethodBody; }

    public boolean isPhpFunctionBody() { return phpFunctionBody; }
    public void setPhpFunctionBody(boolean phpFunctionBody) { this.phpFunctionBody = phpFunctionBody; }

    public boolean isPhpTags() { return phpTags; }
    public void setPhpTags(boolean phpTags) { this.phpTags = phpTags; }

    public boolean isPhpHeredoc() { return phpHeredoc; }
    public void setPhpHeredoc(boolean phpHeredoc) { this.phpHeredoc = phpHeredoc; }

    public boolean isPhpAttribute() { return phpAttribute; }
    public void setPhpAttribute(boolean phpAttribute) { this.phpAttribute = phpAttribute; }

    public boolean isPhpAttributeList() { return phpAttributeList; }
    public void setPhpAttributeList(boolean phpAttributeList) { this.phpAttributeList = phpAttributeList; }

    // Python
    public boolean isPythonLongStringLiterals() { return pythonLongStringLiterals; }
    public void setPythonLongStringLiterals(boolean pythonLongStringLiterals) { this.pythonLongStringLiterals = pythonLongStringLiterals; }

    public boolean isPythonLongCollectionLiterals() { return pythonLongCollectionLiterals; }
    public void setPythonLongCollectionLiterals(boolean pythonLongCollectionLiterals) { this.pythonLongCollectionLiterals = pythonLongCollectionLiterals; }

    public boolean isPythonSequentialComments() { return pythonSequentialComments; }
    public void setPythonSequentialComments(boolean pythonSequentialComments) { this.pythonSequentialComments = pythonSequentialComments; }

    public boolean isPythonTypeAnnotations() { return pythonTypeAnnotations; }
    public void setPythonTypeAnnotations(boolean pythonTypeAnnotations) { this.pythonTypeAnnotations = pythonTypeAnnotations; }

    // Ruby i18n
    public boolean isRubyI18nStrings() { return rubyI18nStrings; }
    public void setRubyI18nStrings(boolean rubyI18nStrings) { this.rubyI18nStrings = rubyI18nStrings; }

    // Rust
    public boolean isRustOneLineMethods() { return rustOneLineMethods; }
    public void setRustOneLineMethods(boolean rustOneLineMethods) { this.rustOneLineMethods = rustOneLineMethods; }

    // SQL
    public boolean isSqlUnderscoresInNumericLiterals() { return sqlUnderscoresInNumericLiterals; }
    public void setSqlUnderscoresInNumericLiterals(boolean sqlUnderscoresInNumericLiterals) { this.sqlUnderscoresInNumericLiterals = sqlUnderscoresInNumericLiterals; }

    // Scala
    public boolean isScalaBlockComments() { return scalaBlockComments; }
    public void setScalaBlockComments(boolean scalaBlockComments) { this.scalaBlockComments = scalaBlockComments; }

    public boolean isScalaMethodCallBodies() { return scalaMethodCallBodies; }
    public void setScalaMethodCallBodies(boolean scalaMethodCallBodies) { this.scalaMethodCallBodies = scalaMethodCallBodies; }

    public boolean isScalaTemplateDefinitionBodies() { return scalaTemplateDefinitionBodies; }
    public void setScalaTemplateDefinitionBodies(boolean scalaTemplateDefinitionBodies) { this.scalaTemplateDefinitionBodies = scalaTemplateDefinitionBodies; }

    public boolean isScalaDefinitionBodies() { return scalaDefinitionBodies; }
    public void setScalaDefinitionBodies(boolean scalaDefinitionBodies) { this.scalaDefinitionBodies = scalaDefinitionBodies; }

    public boolean isScalaTypeLambdas() { return scalaTypeLambdas; }
    public void setScalaTypeLambdas(boolean scalaTypeLambdas) { this.scalaTypeLambdas = scalaTypeLambdas; }

    public boolean isScalaPackages() { return scalaPackages; }
    public void setScalaPackages(boolean scalaPackages) { this.scalaPackages = scalaPackages; }

    public boolean isScalaMultiLineStrings() { return scalaMultiLineStrings; }
    public void setScalaMultiLineStrings(boolean scalaMultiLineStrings) { this.scalaMultiLineStrings = scalaMultiLineStrings; }

    public boolean isScalaCustomRegions() { return scalaCustomRegions; }
    public void setScalaCustomRegions(boolean scalaCustomRegions) { this.scalaCustomRegions = scalaCustomRegions; }

    public boolean isScalaMultiLineBlocks() { return scalaMultiLineBlocks; }
    public void setScalaMultiLineBlocks(boolean scalaMultiLineBlocks) { this.scalaMultiLineBlocks = scalaMultiLineBlocks; }

    public boolean isScalaShowOutlineForMultiLineBlocks() { return scalaShowOutlineForMultiLineBlocks; }
    public void setScalaShowOutlineForMultiLineBlocks(boolean scalaShowOutlineForMultiLineBlocks) { this.scalaShowOutlineForMultiLineBlocks = scalaShowOutlineForMultiLineBlocks; }

    // XML
    public boolean isXmlTags() { return xmlTags; }
    public void setXmlTags(boolean xmlTags) { this.xmlTags = xmlTags; }

    public boolean isXmlHtmlStyleAttribute() { return xmlHtmlStyleAttribute; }
    public void setXmlHtmlStyleAttribute(boolean xmlHtmlStyleAttribute) { this.xmlHtmlStyleAttribute = xmlHtmlStyleAttribute; }

    public boolean isXmlEntities() { return xmlEntities; }
    public void setXmlEntities(boolean xmlEntities) { this.xmlEntities = xmlEntities; }

    public boolean isXmlDataUris() { return xmlDataUris; }
    public void setXmlDataUris(boolean xmlDataUris) { this.xmlDataUris = xmlDataUris; }

    // YAML
    public boolean isYamlLimitFoldedKeysAndValues() { return yamlLimitFoldedKeysAndValues; }
    public void setYamlLimitFoldedKeysAndValues(boolean yamlLimitFoldedKeysAndValues) { this.yamlLimitFoldedKeysAndValues = yamlLimitFoldedKeysAndValues; }

    public int getYamlLimitCharacters() { return yamlLimitCharacters; }
    public void setYamlLimitCharacters(int yamlLimitCharacters) { this.yamlLimitCharacters = yamlLimitCharacters; }

    // Go
    public boolean isGoOneLineIfErrorHandling() { return goOneLineIfErrorHandling; }
    public void setGoOneLineIfErrorHandling(boolean goOneLineIfErrorHandling) { this.goOneLineIfErrorHandling = goOneLineIfErrorHandling; }

    public boolean isGoOneLineFunctionsSingleReturn() { return goOneLineFunctionsSingleReturn; }
    public void setGoOneLineFunctionsSingleReturn(boolean goOneLineFunctionsSingleReturn) { this.goOneLineFunctionsSingleReturn = goOneLineFunctionsSingleReturn; }

    public boolean isGoOneLineCaseClauses() { return goOneLineCaseClauses; }
    public void setGoOneLineCaseClauses(boolean goOneLineCaseClauses) { this.goOneLineCaseClauses = goOneLineCaseClauses; }

    public boolean isGoEmptyFunctions() { return goEmptyFunctions; }
    public void setGoEmptyFunctions(boolean goEmptyFunctions) { this.goEmptyFunctions = goEmptyFunctions; }

    public boolean isGoEmptyStructOrInterface() { return goEmptyStructOrInterface; }
    public void setGoEmptyStructOrInterface(boolean goEmptyStructOrInterface) { this.goEmptyStructOrInterface = goEmptyStructOrInterface; }

    public boolean isGoFormattedStrings() { return goFormattedStrings; }
    public void setGoFormattedStrings(boolean goFormattedStrings) { this.goFormattedStrings = goFormattedStrings; }
}
