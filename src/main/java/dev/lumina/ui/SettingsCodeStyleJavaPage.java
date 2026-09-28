package dev.lumina.ui;

import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;
import dev.lumina.settings.JavaCodeStyleSettings;
import dev.lumina.settings.JavaCodeStyleSettings.BracePlacement;
import dev.lumina.settings.JavaCodeStyleSettings.WrapOption;
import dev.lumina.settings.JavaCodeStyleSettings.ImportEntry;
import dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dedicated Java Code Style settings page (Editor > Code Style > Java).
 * Designed 1:1 against reference screenshots:
 * - media_1790564091704.png (Spaces tab top)
 * - media_1790564117235.png (Spaces tab middle)
 * - media_1790564129237.png (Spaces tab bottom)
 * - media_1790564146694.png (Wrapping and Braces tab)
 *
 * Implements all 9 tabs:
 * 1. Tabs and Indents
 * 2. Spaces (Hierarchical collapsible categories + exact preview)
 * 3. Wrapping and Braces (Table property rows + collapsible sections + exact preview)
 * 4. Blank Lines
 * 5. JavaDoc
 * 6. Imports
 * 7. Arrangement
 * 8. Code Generation
 * 9. Java EE Names
 */
public class SettingsCodeStyleJavaPage extends VBox {

    private final CodeStyleHeaderBar headerBar;
    private final Hyperlink setFromLink = new Hyperlink("Set from...");

    // 9 Tabs
    private final HBox tabBar = new HBox(4);
    private final ToggleGroup tabGroup = new ToggleGroup();
    private String activeTab = "Tabs and Indents";

    // Split layout: Left controls, Right preview
    private final SplitPane splitPane = new SplitPane();
    private final StackPane leftPaneContainer = new StackPane();

    // Model
    private final JavaCodeStyleSettings currentSettings = new JavaCodeStyleSettings();
    private JavaCodeStyleSettings originalSettings = new JavaCodeStyleSettings();

    // 1. Tabs and Indents controls
    private final VBox tabsAndIndentsBox = new VBox(10);
    private final CheckBox useTabCharCheck = new CheckBox("Use tab character");
    private final CheckBox smartTabsCheck = new CheckBox("Smart tabs");
    private final TextField tabSizeField = new TextField("4");
    private final TextField indentField = new TextField("4");
    private final TextField continuationIndentField = new TextField("8");
    private final CheckBox keepIndentsEmptyLinesCheck = new CheckBox("Keep indents on empty lines");
    private final TextField labelIndentField = new TextField("0");
    private final CheckBox absoluteLabelIndentCheck = new CheckBox("Absolute label indent");
    private final CheckBox doNotIndentTopLevelMembersCheck = new CheckBox("Do not indent top level class members");
    private final CheckBox useIndentsRelativeToExprCheck = new CheckBox("Use indents relative to expression start");

    // 2. Spaces controls (Categorized matching reference screenshots)
    private final VBox spacesBox = new VBox(8);

    // Before parentheses
    private final CheckBox spaceBeforeMethodDeclParensCheck = new CheckBox("Method declaration parentheses");
    private final CheckBox spaceBeforeMethodCallParensCheck = new CheckBox("Method call parentheses");
    private final CheckBox spaceBeforeIfParensCheck = new CheckBox("'if' parentheses");
    private final CheckBox spaceBeforeForParensCheck = new CheckBox("'for' parentheses");
    private final CheckBox spaceBeforeWhileParensCheck = new CheckBox("'while' parentheses");
    private final CheckBox spaceBeforeSwitchParensCheck = new CheckBox("'switch' parentheses");
    private final CheckBox spaceBeforeTryParensCheck = new CheckBox("'try' parentheses");
    private final CheckBox spaceBeforeCatchParensCheck = new CheckBox("'catch' parentheses");
    private final CheckBox spaceBeforeSynchronizedParensCheck = new CheckBox("'synchronized' parentheses");
    private final CheckBox spaceBeforeAnnotationParensCheck = new CheckBox("Annotation parameters");
    private final CheckBox spaceBeforeDeconstructionListCheck = new CheckBox("Deconstruction list");

    // Around operators
    private final CheckBox spaceAroundAssignmentOpsCheck = new CheckBox("Assignment operators (=, +=, ...)");
    private final CheckBox spaceAroundLogicalOpsCheck = new CheckBox("Logical operators (&&, ||)");
    private final CheckBox spaceAroundEqualityOpsCheck = new CheckBox("Equality operators (==, !=)");
    private final CheckBox spaceAroundRelationalOpsCheck = new CheckBox("Relational operators (<, >, <=, >=)");
    private final CheckBox spaceAroundBitwiseOpsCheck = new CheckBox("Bitwise operators (&, |, ^)");
    private final CheckBox spaceAroundAdditiveOpsCheck = new CheckBox("Additive operators (+, -)");
    private final CheckBox spaceAroundMultiplicativeOpsCheck = new CheckBox("Multiplicative operators (*, /, %)");
    private final CheckBox spaceAroundShiftOpsCheck = new CheckBox("Shift operators (<<, >>, >>>)");
    private final CheckBox spaceAroundUnaryOpsCheck = new CheckBox("Unary operators (!, -, +, ++, --)");
    private final CheckBox spaceAroundLambdaArrowCheck = new CheckBox("Lambda arrow");
    private final CheckBox spaceAroundMethodRefDoubleColonCheck = new CheckBox("Method reference double colon");

    // Before left brace
    private final CheckBox spaceBeforeClassLeftBraceCheck = new CheckBox("Class left brace");
    private final CheckBox spaceBeforeMethodLeftBraceCheck = new CheckBox("Method left brace");
    private final CheckBox spaceBeforeIfLeftBraceCheck = new CheckBox("'if' left brace");
    private final CheckBox spaceBeforeElseLeftBraceCheck = new CheckBox("'else' left brace");
    private final CheckBox spaceBeforeWhileLeftBraceCheck = new CheckBox("'while' left brace");
    private final CheckBox spaceBeforeForLeftBraceCheck = new CheckBox("'for' left brace");
    private final CheckBox spaceBeforeDoLeftBraceCheck = new CheckBox("'do' left brace");
    private final CheckBox spaceBeforeSwitchLeftBraceCheck = new CheckBox("'switch' left brace");
    private final CheckBox spaceBeforeTryLeftBraceCheck = new CheckBox("'try' left brace");
    private final CheckBox spaceBeforeCatchLeftBraceCheck = new CheckBox("'catch' left brace");
    private final CheckBox spaceBeforeFinallyLeftBraceCheck = new CheckBox("'finally' left brace");
    // Before keywords
    private final CheckBox spaceBeforeElseCheck = new CheckBox("'else'");
    private final CheckBox spaceBeforeWhileCheck = new CheckBox("'while'");
    private final CheckBox spaceBeforeCatchCheck = new CheckBox("'catch'");
    private final CheckBox spaceBeforeFinallyCheck = new CheckBox("'finally'");
    // Within
    private final CheckBox withinCodeBracesCheck = new CheckBox("Code braces");
    private final CheckBox withinBracketsCheck = new CheckBox("Brackets");
    private final CheckBox withinArrayInitBracesCheck = new CheckBox("Array initializer braces");
    private final CheckBox withinEmptyArrayInitBracesCheck = new CheckBox("Empty array initializer braces");
    private final CheckBox withinGroupingParensCheck = new CheckBox("Grouping parentheses");
    private final CheckBox withinMethodDeclParensCheck = new CheckBox("Method declaration parentheses");
    private final CheckBox withinEmptyMethodDeclParensCheck = new CheckBox("Empty method declaration parentheses");
    private final CheckBox withinMethodCallParensCheck = new CheckBox("Method call parentheses");
    private final CheckBox withinEmptyMethodCallParensCheck = new CheckBox("Empty method call parentheses");
    private final CheckBox withinIfParensCheck = new CheckBox("'if' parentheses");
    private final CheckBox withinForParensCheck = new CheckBox("'for' parentheses");
    private final CheckBox withinWhileParensCheck = new CheckBox("'while' parentheses");
    private final CheckBox withinSwitchParensCheck = new CheckBox("'switch' parentheses");
    private final CheckBox withinTryParensCheck = new CheckBox("'try' parentheses");
    private final CheckBox withinCatchParensCheck = new CheckBox("'catch' parentheses");
    private final CheckBox withinSynchronizedParensCheck = new CheckBox("'synchronized' parentheses");
    private final CheckBox withinTypeCastParensCheck = new CheckBox("Type cast parentheses");
    private final CheckBox withinAnnotationParensCheck = new CheckBox("Annotation parentheses");
    private final CheckBox withinAngleBracketsCheck = new CheckBox("Angle brackets");
    private final CheckBox withinRecordHeaderCheck = new CheckBox("Record header");
    private final CheckBox withinDeconstructionListCheck = new CheckBox("Deconstruction list");
    private final CheckBox withinBlockBracesWhenBodyCheck = new CheckBox("Inside block braces when body is present");
    // In ternary operator (?:)
    private final CheckBox ternaryBeforeQuestionCheck = new CheckBox("Before '?'");
    private final CheckBox ternaryAfterQuestionCheck = new CheckBox("After '?'");
    private final CheckBox ternaryBeforeColonCheck = new CheckBox("Before ':'");
    private final CheckBox ternaryAfterColonCheck = new CheckBox("After ':'");
    // Type arguments
    private final CheckBox typeArgsAfterCommaCheck = new CheckBox("After comma");
    private final CheckBox typeArgsBeforeOpenAngleCheck = new CheckBox("Before opening angle bracket");
    private final CheckBox typeArgsAfterCloseAngleCheck = new CheckBox("After closing angle bracket");
    // Other
    private final CheckBox otherBeforeCommaCheck = new CheckBox("Before comma");
    private final CheckBox otherAfterCommaCheck = new CheckBox("After comma");
    private final CheckBox otherBeforeForSemicolonCheck = new CheckBox("Before 'for' semicolon");
    private final CheckBox otherAfterForSemicolonCheck = new CheckBox("After 'for' semicolon");
    private final CheckBox otherAfterTypeCastCheck = new CheckBox("After type cast");
    private final CheckBox otherAroundEqualsInAnnotationCheck = new CheckBox("Around '=' in annotation value pair");
    private final CheckBox otherBeforeColonInForEachCheck = new CheckBox("Before colon in foreach");
    private final CheckBox otherInsideOneLineEnumBracesCheck = new CheckBox("Inside one line enum braces");
    // Type parameters
    private final CheckBox typeParamsBeforeOpenAngleCheck = new CheckBox("Before opening angle bracket");
    private final CheckBox typeParamsAroundTypeBoundsCheck = new CheckBox("Around type bounds");

    // 3. Wrapping and Braces controls (Matching screenshot 5)
    private final VBox wrappingBox = new VBox(8);
    // Keep when reformatting
    private final CheckBox keepLineBreaksCheck = new CheckBox("Line breaks");
    private final CheckBox commentAtFirstColumnCheck = new CheckBox("Comment at first column");
    private final CheckBox controlStatementInOneLineCheck = new CheckBox("Control statement in one line");
    private final CheckBox multipleExpressionsInOneLineCheck = new CheckBox("Multiple expressions in one line");
    private final CheckBox keepSimpleBlocksCheck = new CheckBox("Simple blocks in one line");
    private final CheckBox keepSimpleMethodsCheck = new CheckBox("Simple methods in one line");
    private final CheckBox keepSimpleLambdasCheck = new CheckBox("Simple lambdas in one line");
    private final CheckBox keepSimpleClassesCheck = new CheckBox("Simple classes in one line");
    private final CheckBox ensureRightMarginCheck = new CheckBox("Ensure right margin is not exceeded");
    // Braces placement
    private final ComboBox<BracePlacement> classBraceCombo = new ComboBox<>();
    private final ComboBox<BracePlacement> methodBraceCombo = new ComboBox<>();
    private final ComboBox<BracePlacement> lambdaBraceCombo = new ComboBox<>();
    private final ComboBox<BracePlacement> otherBraceCombo = new ComboBox<>();
    // Extends/implements/permits list
    private final ComboBox<WrapOption> extendsImplementsWrapCombo = new ComboBox<>();
    private final CheckBox alignExtendsImplementsCheck = new CheckBox("Align when multiline");
    private final ComboBox<WrapOption> extendsImplementsKeywordWrapCombo = new ComboBox<>();
    // Throws list
    private final ComboBox<WrapOption> throwsWrapCombo = new ComboBox<>();
    private final CheckBox alignThrowsCheck = new CheckBox("Align when multiline");
    private final CheckBox alignThrowsToMethodStartCheck = new CheckBox("Align 'throws' to method start");
    private final ComboBox<WrapOption> throwsKeywordWrapCombo = new ComboBox<>();
    // Method declaration parameters
    private final ComboBox<WrapOption> methodDeclParamsWrapCombo = new ComboBox<>();
    private final CheckBox alignMethodDeclParamsCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineAfterMethodDeclLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeMethodDeclRParenOnNewLineCheck = new CheckBox("Place ')' on new line");
    // Method call arguments
    private final ComboBox<WrapOption> methodCallArgsWrapCombo = new ComboBox<>();
    private final CheckBox alignMethodCallArgsCheck = new CheckBox("Align when multiline");
    private final CheckBox takePriorityOverCallChainWrapCheck = new CheckBox("Take priority over call chain wrappin");
    private final CheckBox newLineAfterMethodCallLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeMethodCallRParenOnNewLineCheck = new CheckBox("Place ')' on new line");
    // Method parentheses
    private final CheckBox alignMethodParenthesesCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineWhenMethodBodyPresentedCheck = new CheckBox("New line when body is presented");
    // Chained method calls
    private final ComboBox<WrapOption> chainedMethodCallsWrapCombo = new ComboBox<>();
    private final CheckBox wrapFirstCallCheck = new CheckBox("Wrap first call");
    private final CheckBox alignChainedCallsMultilineCheck = new CheckBox("Align when multiline");
    private final ComboBox<String> builderMethodsCombo = new ComboBox<>();
    private final CheckBox keepBuilderMethodsIndentsCheck = new CheckBox("Keep builder methods indents");
    private final CheckBox moveSemicolonToNewLineCheck = new CheckBox("Move ';' to the new line");
    // 'if()' statement
    private final ComboBox<String> ifForceBracesCombo = new ComboBox<>();
    private final CheckBox elseOnNewLineCheck = new CheckBox("'else' on new line");
    private final CheckBox specialElseIfTreatmentCheck = new CheckBox("Special 'else if' treatment");
    // 'for()' statement
    private final ComboBox<WrapOption> forStatementWrapCombo = new ComboBox<>();
    private final CheckBox alignForMultilineCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineAfterForLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeForRParenOnNewLineCheck = new CheckBox("Place ')' on new line");
    private final ComboBox<String> forForceBracesCombo = new ComboBox<>();
    // 'while()' statement
    private final ComboBox<String> whileForceBracesCombo = new ComboBox<>();
    // 'do ... while()' statement
    private final ComboBox<String> doWhileForceBracesCombo = new ComboBox<>();
    private final CheckBox whileOnNewLineCheck = new CheckBox("'while' on new line");
    // 'switch' statement/expression
    private final ComboBox<WrapOption> switchWrapCombo = new ComboBox<>();
    private final CheckBox indentCaseBranchesCheck = new CheckBox("Indent 'case' branches");
    private final CheckBox eachCaseOnSeparateLineCheck = new CheckBox("Each 'case' on a separate line");
    // 'try-with-resources'
    private final ComboBox<WrapOption> tryWithResourcesWrapCombo = new ComboBox<>();
    private final CheckBox alignTryWithResourcesCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineAfterTryWithResourcesLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeTryWithResourcesRParenOnNewLineCheck = new CheckBox("Place ')' on new line");
    // 'try' statement
    private final CheckBox catchOnNewLineCheck = new CheckBox("'catch' on new line");
    private final CheckBox finallyOnNewLineCheck = new CheckBox("'finally' on new line");
    private final ComboBox<WrapOption> typesInMultiCatchWrapCombo = new ComboBox<>();
    private final CheckBox alignTypesInMultiCatchCheck = new CheckBox("Align types in multi-catch");
    // Binary expressions
    private final ComboBox<WrapOption> binaryExpressionsWrapCombo = new ComboBox<>();
    private final CheckBox alignBinaryExpressionsCheck = new CheckBox("Align when multiline");
    private final CheckBox operationSignOnNextLineCheck = new CheckBox("Operation sign on next line");
    private final CheckBox alignParenthesisedCheck = new CheckBox("Align parenthesised when multiline");
    private final CheckBox newLineAfterBinaryLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeBinaryRParenOnNewLineCheck = new CheckBox("Place ')' on new line");
    // Assignment statement
    private final ComboBox<WrapOption> assignmentWrapCombo = new ComboBox<>();
    private final CheckBox alignAssignmentCheck = new CheckBox("Align when multiline");
    private final CheckBox assignmentSignOnNextLineCheck = new CheckBox("Assignment sign on next line");
    // Group declarations
    private final CheckBox alignFieldsInColumnsCheck = new CheckBox("Align fields in columns");
    private final CheckBox alignVariablesInColumnsCheck = new CheckBox("Align variables in columns");
    private final CheckBox alignAssignmentsInColumnsCheck = new CheckBox("Align assignments in columns");
    private final CheckBox alignSimpleMethodsInColumnsCheck = new CheckBox("Align simple methods in columns");
    // Ternary operation
    private final ComboBox<WrapOption> ternaryWrapCombo = new ComboBox<>();
    private final CheckBox alignTernaryCheck = new CheckBox("Align when multiline");
    private final CheckBox ternarySignsOnNextLineCheck = new CheckBox("'?' and ':' signs on next line");
    // Array initializer
    private final ComboBox<WrapOption> arrayInitializerWrapCombo = new ComboBox<>();
    private final CheckBox alignArrayInitializerCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineAfterArrayInitializerLBraceCheck = new CheckBox("New line after '{'");
    private final CheckBox placeArrayInitializerRBraceOnNewLineCheck = new CheckBox("Place '}' on new line");
    // Modifier list
    private final CheckBox wrapAfterModifierListCheck = new CheckBox("Wrap after modifier list");
    // Assert statement
    private final ComboBox<WrapOption> assertStatementWrapCombo = new ComboBox<>();
    // Enum constants
    private final ComboBox<WrapOption> enumConstantsWrapCombo = new ComboBox<>();
    // Annotations
    private final ComboBox<WrapOption> classAnnotationsWrapCombo = new ComboBox<>();
    private final ComboBox<WrapOption> methodAnnotationsWrapCombo = new ComboBox<>();
    private final ComboBox<WrapOption> fieldAnnotationsWrapCombo = new ComboBox<>();
    private final CheckBox doNotWrapAfterSingleFieldAnnotationCheck = new CheckBox("Do not wrap after single annotation");
    private final ComboBox<WrapOption> parameterAnnotationsWrapCombo = new ComboBox<>();
    private final CheckBox doNotWrapAfterSingleParameterAnnotationCheck = new CheckBox("Do not wrap after single annotation");
    private final ComboBox<WrapOption> localVariableAnnotationsWrapCombo = new ComboBox<>();
    private final ComboBox<WrapOption> enumFieldAnnotationsWrapCombo = new ComboBox<>();
    private final ComboBox<WrapOption> annotationParametersWrapCombo = new ComboBox<>();
    private final CheckBox alignAnnotationParametersCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineAfterAnnotationParametersLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeAnnotationParametersRParenOnNewLineCheck = new CheckBox("Place ')' on new line");
    // Text blocks
    private final CheckBox alignTextBlocksCheck = new CheckBox("Align when multiline");
    // Record components
    private final ComboBox<WrapOption> recordComponentsWrapCombo = new ComboBox<>();
    private final CheckBox alignRecordComponentsCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineAfterRecordComponentsLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeRecordComponentsRParenOnNewLineCheck = new CheckBox("Place ')' on new line");
    private final CheckBox newLineForRecordComponentAnnotationsCheck = new CheckBox("New line for annotations");
    // Deconstruction patterns
    private final ComboBox<WrapOption> deconstructionPatternsWrapCombo = new ComboBox<>();
    private final CheckBox alignDeconstructionPatternsCheck = new CheckBox("Align when multiline");
    private final CheckBox newLineAfterDeconstructionPatternsLParenCheck = new CheckBox("New line after '('");
    private final CheckBox placeDeconstructionPatternsRParenOnNewLineCheck = new CheckBox("Place ')' on new line");

    // 4. Blank Lines controls
    private final VBox blankLinesBox = new VBox(6);
    private final TextField keepBlankLinesInDeclField = new TextField("2");
    private final TextField keepBlankLinesInCodeField = new TextField("2");
    private final TextField keepBlankLinesBeforeRBraceField = new TextField("2");
    private final TextField keepBlankLinesBetweenHeaderAndPackageField = new TextField("2");
    private final TextField blankLinesBeforePackageField = new TextField("0");
    private final TextField blankLinesAfterPackageField = new TextField("1");
    private final TextField blankLinesBeforeImportsField = new TextField("1");
    private final TextField blankLinesAfterImportsField = new TextField("1");
    private final TextField blankLinesAroundClassField = new TextField("1");
    private final TextField blankLinesAfterClassHeaderField = new TextField("0");
    private final TextField blankLinesBeforeClassEndField = new TextField("0");
    private final TextField blankLinesAfterAnonymousClassHeaderField = new TextField("0");
    private final TextField blankLinesBeforeFieldInInterfaceField = new TextField("0");
    private final TextField blankLinesBeforeFieldWithoutAnnotationsField = new TextField("0");
    private final TextField blankLinesBeforeFieldWithAnnotationsField = new TextField("0");
    private final TextField blankLinesAroundMethodInInterfaceField = new TextField("1");
    private final TextField blankLinesAroundMethodField = new TextField("1");
    private final TextField blankLinesBeforeMethodBodyField = new TextField("0");
    private final TextField blankLinesAroundInitializerField = new TextField("1");
    private final TextField blankLinesBetweenRecordComponentsField = new TextField("0");

    // 5. JavaDoc controls
    private final VBox javadocBox = new VBox(8);
    private final CheckBox enableJavaDocFormattingCheck = new CheckBox("Enable JavaDoc formatting");
    // Alignment
    private final CheckBox alignParamDescriptionsCheck = new CheckBox("Align parameter descriptions");
    private final CheckBox alignThrownExceptionsCheck = new CheckBox("Align thrown exception descriptions");
    // Blank lines
    private final CheckBox blankLinesAfterDescCheck = new CheckBox("After description");
    private final CheckBox blankLinesAfterParamDescCheck = new CheckBox("After parameter descriptions");
    private final CheckBox blankLinesAfterReturnTagCheck = new CheckBox("After return tag");
    // Invalid tags
    private final CheckBox keepInvalidTagsCheck = new CheckBox("Keep invalid tags");
    private final CheckBox keepEmptyParamTagsCheck = new CheckBox("Keep empty @param tags");
    private final CheckBox keepEmptyReturnTagsCheck = new CheckBox("Keep empty @return tags");
    private final CheckBox keepEmptyThrowsTagsCheck = new CheckBox("Keep empty @throws tags");
    // Other
    private final CheckBox wrapAtRightMarginCheck = new CheckBox("Wrap at right margin");
    private final CheckBox enableLeadingAsterisksCheck = new CheckBox("Enable leading asterisks");
    private final CheckBox useThrowsRatherThanExceptionCheck = new CheckBox("Use @throws rather than @exception");
    private final CheckBox generatePOnEmptyLinesCheck = new CheckBox("Generate \"<p>\" on empty lines");
    private final CheckBox keepEmptyLinesCheck = new CheckBox("Keep empty lines");
    private final CheckBox doNotWrapOneLineCommentsCheck = new CheckBox("Do not wrap one line comments");
    private final CheckBox preserveLineFeedsCheck = new CheckBox("Preserve line feeds");
    private final CheckBox paramDescriptionsOnNewLineCheck = new CheckBox("Parameter descriptions on new line");
    private final CheckBox indentContinuationLinesCheck = new CheckBox("Indent continuation lines");

    // 6. Imports controls
    private final VBox importsBox = new VBox(12);
    private final CheckBox useSingleClassImportCheck = new CheckBox("Use single class import");
    private final CheckBox useFullyQualifiedClassNamesCheck = new CheckBox("Use fully qualified class names");
    private final CheckBox insertInnerClassImportsCheck = new CheckBox("Insert imports for inner classes");
    private final ListView<String> excludeInnerClassesList = new ListView<>();
    private final Button addInnerClassBtn = createMiniToolbarButton("+");
    private final Button removeInnerClassBtn = createMiniToolbarButton("—");

    private final CheckBox doNotSeparateModuleImportsCheck = new CheckBox("Do not separate module imports into single or package imports");
    private final CheckBox deleteUnusedModuleImportsCheck = new CheckBox("Delete unused module imports");

    private final ComboBox<String> useFqNamesInJavadocCombo = new ComboBox<>();
    private final TextField classCountImportField = new TextField("5");
    private final TextField namesCountStaticImportField = new TextField("3");

    // Table 1: Packages to Use Import with '*'
    private final TableView<ImportEntry> packagesToUseImportTable = new TableView<>();
    private final Button addPackageImportBtn = createMiniToolbarButton("+");
    private final Button removePackageImportBtn = createMiniToolbarButton("—");

    private final CheckBox placeOnDemandImportBeforeSingleClassCheck = new CheckBox("Place on-demand import before single-class imports from the same package");
    private final CheckBox layoutStaticImportsSeparatelyCheck = new CheckBox("Layout static imports separately");

    // Table 2: Import layout
    private final TableView<ImportLayoutEntry> importLayoutTable = new TableView<>();
    private final Button addImportLayoutBtn = createMiniToolbarButton("+");
    private final Button removeImportLayoutBtn = createMiniToolbarButton("—");
    private final Button moveUpImportLayoutBtn = createMiniToolbarButton("↑");
    private final Button moveDownImportLayoutBtn = createMiniToolbarButton("↓");

    // 7. Arrangement controls
    private final VBox arrangementBox = new VBox(10);
    private final CheckBox keepGettersSettersTogetherCheck = new CheckBox("Keep getters and setters together");
    private final CheckBox keepOverriddenMethodsTogetherCheck = new CheckBox("Keep overridden methods together");
    private final CheckBox keepOverloadedMethodsTogetherCheck = keepOverriddenMethodsTogetherCheck;
    private final ComboBox<String> overriddenMethodsOrderCombo = new ComboBox<>();
    private final CheckBox keepDependentMethodsTogetherCheck = new CheckBox("Keep dependent methods together");
    private final ComboBox<String> dependentMethodsOrderCombo = new ComboBox<>();

    private final ListView<JavaCodeStyleSettings.ArrangementRule> matchingRulesList = new ListView<>();
    private final Button addRuleBtn = createMiniToolbarButton("+");
    private final Button addSectionRuleBtn = createMiniToolbarButton("+s");
    private final Button removeRuleBtn = createMiniToolbarButton("—");
    private final Button moveUpRuleBtn = createMiniToolbarButton("↑");
    private final Button moveDownRuleBtn = createMiniToolbarButton("↓");
    private final Button ruleSettingsBtn = createMiniToolbarButton("⚙");

    private final VBox ruleEditorCard = new VBox(8);
    private final Label ruleEditorHeader = new Label("<empty rule>");
    private final FlowPane ruleTypeFlow = new FlowPane(4, 4);
    private final FlowPane ruleModifierFlow = new FlowPane(4, 4);
    private final TextField ruleNameField = new TextField("");
    private final ComboBox<String> ruleOrderCombo = new ComboBox<>();
    private final ToggleButton ruleAliasBtn = new ToggleButton("by visibility");
    private final List<ToggleButton> typeToggleButtons = new ArrayList<>();
    private final List<ToggleButton> modifierToggleButtons = new ArrayList<>();

    // 8. Code Generation controls
    private final VBox codeGenBox = new VBox(10);
    private final CheckBox preferLongerNamesCheck = new CheckBox("Prefer longer names");
    private final TextField fieldPrefixField = new TextField("");
    private final TextField fieldSuffixField = new TextField("");
    private final TextField staticFieldPrefixField = new TextField("");
    private final TextField staticFieldSuffixField = new TextField("");
    private final TextField paramPrefixField = new TextField("");
    private final TextField paramSuffixField = new TextField("");
    private final TextField localPrefixField = new TextField("");
    private final TextField localSuffixField = new TextField("");
    private final TextField subclassPrefixField = new TextField("");
    private final TextField subclassSuffixField = new TextField("Impl");
    private final TextField testClassPrefixField = new TextField("");
    private final TextField testClassSuffixField = new TextField("Test");

    private final ToggleGroup defaultVisibilityGroup = new ToggleGroup();
    private final RadioButton visibilityEscalateRadio = new RadioButton("Escalate");
    private final RadioButton visibilityPrivateRadio = new RadioButton("Private");
    private final RadioButton visibilityPackageLocalRadio = new RadioButton("Package local");
    private final RadioButton visibilityProtectedRadio = new RadioButton("Protected");
    private final RadioButton visibilityPublicRadio = new RadioButton("Public");

    private final CheckBox makeLocalsFinalCheck = new CheckBox("Make generated local variables final");
    private final CheckBox makeParamsFinalCheck = new CheckBox("Make generated parameters final");
    private final CheckBox useVarForLocalVarsCheck = new CheckBox("Use 'var' for local variable declaration");

    private final CheckBox lineCommentAtFirstColCheck = new CheckBox("Line comment at first column");
    private final CheckBox addSpaceAtLineCommentStartCheck = new CheckBox("Add a space at line comment start");
    private final CheckBox enforceOnReformatCheck = new CheckBox("Enforce on reformat");
    private final CheckBox blockCommentAtFirstColCheck = new CheckBox("Block comment at first column");
    private final CheckBox addSpacesAroundBlockCommentsCheck = new CheckBox("Add spaces around block comments");

    private final CheckBox insertOverrideCheck = new CheckBox("Insert @Override annotation");
    private final CheckBox repeatSynchronizedCheck = new CheckBox("Repeat synchronized modifier");
    private final ListView<String> annotationsToCopyList = new ListView<>();
    private final Button addAnnotationBtn = createMiniToolbarButton("+");
    private final Button removeAnnotationBtn = createMiniToolbarButton("—");
    private final CheckBox useExternalAnnotationsCheck = new CheckBox("Use external annotations");
    private final CheckBox insertTypeUseAnnotationsBeforeTypeCheck = new CheckBox("Put generated annotations with target TYPE_USE between the modifiers and the type");

    private final CheckBox useClassIsInstanceCheck = new CheckBox("Use Class::isInstance and Class::cast when possible");
    private final CheckBox replaceNullCheckCheck = new CheckBox("Replace null-check with Objects::nonNull or Objects::isNull");
    private final CheckBox useIntegerSumCheck = new CheckBox("Use Integer::sum, etc. when possible");

    // 9. Java EE Names controls
    private final VBox javaEeBox = new VBox(10);
    // Entity Bean
    private final TextField entityEjbClassPrefixField = new TextField("");
    private final TextField entityEjbClassSuffixField = new TextField("Bean");
    private final TextField entityHomeInterfacePrefixField = new TextField("");
    private final TextField entityHomeInterfaceSuffixField = new TextField("Home");
    private final TextField entityRemoteInterfacePrefixField = new TextField("");
    private final TextField entityRemoteInterfaceSuffixField = new TextField("");
    private final TextField entityLocalHomeInterfacePrefixField = new TextField("Local");
    private final TextField entityLocalHomeInterfaceSuffixField = new TextField("Home");
    private final TextField entityLocalInterfacePrefixField = new TextField("Local");
    private final TextField entityLocalInterfaceSuffixField = new TextField("");
    private final TextField entityEjbNameTagPrefixField = new TextField("");
    private final TextField entityEjbNameTagSuffixField = new TextField("EJB");
    private final TextField entityTransferObjectPrefixField = new TextField("");
    private final TextField entityTransferObjectSuffixField = new TextField("VO");
    private final TextField entityDefaultPkClassField = new TextField("java.lang.String");
    // Session Bean
    private final TextField sessionEjbClassPrefixField = new TextField("");
    private final TextField sessionEjbClassSuffixField = new TextField("Bean");
    private final TextField sessionHomeInterfacePrefixField = new TextField("");
    private final TextField sessionHomeInterfaceSuffixField = new TextField("Home");
    private final TextField sessionRemoteInterfacePrefixField = new TextField("");
    private final TextField sessionRemoteInterfaceSuffixField = new TextField("");
    private final TextField sessionLocalHomeInterfacePrefixField = new TextField("Local");
    private final TextField sessionLocalHomeInterfaceSuffixField = new TextField("Home");
    private final TextField sessionLocalInterfacePrefixField = new TextField("Local");
    private final TextField sessionLocalInterfaceSuffixField = new TextField("");
    private final TextField sessionServiceEndpointPrefixField = new TextField("");
    private final TextField sessionServiceEndpointSuffixField = new TextField("Service");
    private final TextField sessionEjbNameTagPrefixField = new TextField("");
    private final TextField sessionEjbNameTagSuffixField = new TextField("EJB");
    // Servlet
    private final TextField servletClassPrefixField = new TextField("");
    private final TextField servletClassSuffixField = new TextField("");
    private final TextField servletNameTagPrefixField = new TextField("");
    private final TextField servletNameTagSuffixField = new TextField("");
    // Message Driven Bean
    private final TextField mdbEjbClassPrefixField = new TextField("");
    private final TextField mdbEjbClassSuffixField = new TextField("Bean");
    private final TextField mdbEjbNameTagPrefixField = new TextField("");
    private final TextField mdbEjbNameTagSuffixField = new TextField("EJB");
    // Filter
    private final TextField filterClassPrefixField = new TextField("");
    private final TextField filterClassSuffixField = new TextField("");
    private final TextField filterNameTagPrefixField = new TextField("");
    private final TextField filterNameTagSuffixField = new TextField("");
    // Listener
    private final TextField listenerClassPrefixField = new TextField("");
    private final TextField listenerClassSuffixField = new TextField("");
    // Legacy compatibility fields
    private final TextField entitySuffixField = entityEjbClassSuffixField;
    private final TextField sessionSuffixField = sessionEjbClassSuffixField;
    private final TextField mdbSuffixField = mdbEjbClassSuffixField;
    private final TextField daoSuffixField = new TextField("DAO");
    private final TextField serviceSuffixField = sessionServiceEndpointSuffixField;
    private final TextField serviceImplSuffixField = new TextField("ServiceImpl");

    // Right pane: Live Code Preview
    private final ScrollPane previewScrollPane = new ScrollPane();
    private final VBox previewLinesBox = new VBox(1);

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsCodeStyleJavaPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 16, 16, 16));
        setSpacing(10);

        headerBar = new CodeStyleHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadFromCurrentScheme();
            notifyModified();
        });
        headerBar.setOnSettingsModified(this::notifyModified);

        buildUi();
        setupListeners();
        loadFromCurrentScheme();
        updatePreview();
    }

    private void buildUi() {
        // 1. Top row: Header bar with "Set from..." action on right
        setFromLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;");
        setFromLink.setOnMouseEntered(e -> setFromLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true;"));
        setFromLink.setOnMouseExited(e -> setFromLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;"));
        setFromLink.setOnAction(e -> showSetFromMenu());
        headerBar.addRightAction(setFromLink);

        HBox topRow = new HBox(headerBar);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerBar, Priority.ALWAYS);
        getChildren().add(topRow);

        // 2. Tabs Bar (9 tabs)
        tabBar.setStyle("-fx-border-color: #323438; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 4 0;");
        List<String> tabs = List.of(
                "Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines",
                "JavaDoc", "Imports", "Arrangement", "Code Generation", "Java EE Names"
        );

        for (String tabName : tabs) {
            ToggleButton btn = new ToggleButton(tabName);
            btn.setToggleGroup(tabGroup);
            btn.setMinWidth(Region.USE_PREF_SIZE);
            btn.getStyleClass().add("code-style-tab");
            applyTabButtonStyle(btn, tabName.equals(activeTab));
            if (tabName.equals(activeTab)) {
                btn.setSelected(true);
            }
            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    activeTab = tabName;
                    for (Toggle t : tabGroup.getToggles()) {
                        if (t instanceof ToggleButton tb) {
                            applyTabButtonStyle(tb, tb == btn);
                        }
                    }
                    switchTab(tabName);
                    updatePreview();
                } else {
                    btn.setSelected(true);
                }
            });
            tabBar.getChildren().add(btn);
        }

        ScrollPane tabScroll = new ScrollPane(tabBar);
        tabScroll.setFitToHeight(true);
        tabScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tabScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tabScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        getChildren().add(tabScroll);

        // 3. Build Tab Panels
        buildTabsAndIndentsPanel();
        buildSpacesPanel();
        buildWrappingPanel();
        buildBlankLinesPanel();
        buildJavaDocPanel();
        buildImportsPanel();
        buildArrangementPanel();
        buildCodeGenPanel();
        buildJavaEePanel();

        // 4. Split Pane (Left: Controls, Right: Live Preview)
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        ScrollPane leftScroll = new ScrollPane(leftPaneContainer);
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
        leftScroll.setPadding(new Insets(6, 12, 8, 4));

        previewLinesBox.setStyle("-fx-background-color: #1E1F22; -fx-padding: 10 14;");
        previewScrollPane.setContent(previewLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #323438; -fx-border-width: 0 0 0 1;");

        splitPane.getItems().addAll(leftScroll, previewScrollPane);
        splitPane.setDividerPositions(0.48);

        switchTab("Tabs and Indents");
        getChildren().add(splitPane);
    }

    private void switchTab(String tabName) {
        leftPaneContainer.getChildren().clear();
        switch (tabName) {
            case "Tabs and Indents" -> leftPaneContainer.getChildren().add(tabsAndIndentsBox);
            case "Spaces" -> leftPaneContainer.getChildren().add(spacesBox);
            case "Wrapping and Braces" -> leftPaneContainer.getChildren().add(wrappingBox);
            case "Blank Lines" -> leftPaneContainer.getChildren().add(blankLinesBox);
            case "JavaDoc" -> leftPaneContainer.getChildren().add(javadocBox);
            case "Imports" -> leftPaneContainer.getChildren().add(importsBox);
            case "Arrangement" -> leftPaneContainer.getChildren().add(arrangementBox);
            case "Code Generation" -> leftPaneContainer.getChildren().add(codeGenBox);
            case "Java EE Names" -> leftPaneContainer.getChildren().add(javaEeBox);
            default -> leftPaneContainer.getChildren().add(tabsAndIndentsBox);
        }

        boolean showPreview = !tabName.equals("Imports") && !tabName.equals("Arrangement")
                && !tabName.equals("Code Generation") && !tabName.equals("Java EE Names");
        if (showPreview) {
            if (!splitPane.getItems().contains(previewScrollPane)) {
                splitPane.getItems().add(previewScrollPane);
                splitPane.setDividerPositions(0.48);
            }
        } else {
            splitPane.getItems().remove(previewScrollPane);
        }
    }

    private void buildTabsAndIndentsPanel() {
        tabsAndIndentsBox.getChildren().clear();

        styleCheckbox(useTabCharCheck);
        styleCheckbox(smartTabsCheck);
        smartTabsCheck.setPadding(new Insets(0, 0, 0, 20));
        smartTabsCheck.disableProperty().bind(useTabCharCheck.selectedProperty().not());

        tabsAndIndentsBox.getChildren().addAll(useTabCharCheck, smartTabsCheck);
        tabsAndIndentsBox.getChildren().add(createLabeledField("Tab size:", tabSizeField));
        tabsAndIndentsBox.getChildren().add(createLabeledField("Indent:", indentField));
        tabsAndIndentsBox.getChildren().add(createLabeledField("Continuation indent:", continuationIndentField));

        styleCheckbox(keepIndentsEmptyLinesCheck);
        tabsAndIndentsBox.getChildren().add(keepIndentsEmptyLinesCheck);

        tabsAndIndentsBox.getChildren().add(createLabeledField("Label indent:", labelIndentField));

        styleCheckbox(absoluteLabelIndentCheck);
        absoluteLabelIndentCheck.setPadding(new Insets(0, 0, 0, 20));
        tabsAndIndentsBox.getChildren().add(absoluteLabelIndentCheck);

        styleCheckbox(doNotIndentTopLevelMembersCheck);
        tabsAndIndentsBox.getChildren().add(doNotIndentTopLevelMembersCheck);

        styleCheckbox(useIndentsRelativeToExprCheck);
        tabsAndIndentsBox.getChildren().add(useIndentsRelativeToExprCheck);
    }

    private void buildSpacesPanel() {
        spacesBox.getChildren().clear();

        // 1. Before parentheses (collapsed matching reference screenshot 1 & 2)
        styleCheckbox(spaceBeforeMethodDeclParensCheck);
        styleCheckbox(spaceBeforeMethodCallParensCheck);
        styleCheckbox(spaceBeforeIfParensCheck);
        styleCheckbox(spaceBeforeForParensCheck);
        styleCheckbox(spaceBeforeWhileParensCheck);
        styleCheckbox(spaceBeforeSwitchParensCheck);
        styleCheckbox(spaceBeforeTryParensCheck);
        styleCheckbox(spaceBeforeCatchParensCheck);
        styleCheckbox(spaceBeforeSynchronizedParensCheck);
        styleCheckbox(spaceBeforeAnnotationParensCheck);
        styleCheckbox(spaceBeforeDeconstructionListCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Before parentheses", false,
                spaceBeforeMethodDeclParensCheck, spaceBeforeMethodCallParensCheck,
                spaceBeforeIfParensCheck, spaceBeforeForParensCheck,
                spaceBeforeWhileParensCheck, spaceBeforeSwitchParensCheck,
                spaceBeforeTryParensCheck, spaceBeforeCatchParensCheck,
                spaceBeforeSynchronizedParensCheck, spaceBeforeAnnotationParensCheck,
                spaceBeforeDeconstructionListCheck
        ));

        // 2. Around operators (collapsed matching reference screenshot 1 & 2)
        styleCheckbox(spaceAroundAssignmentOpsCheck);
        styleCheckbox(spaceAroundLogicalOpsCheck);
        styleCheckbox(spaceAroundEqualityOpsCheck);
        styleCheckbox(spaceAroundRelationalOpsCheck);
        styleCheckbox(spaceAroundBitwiseOpsCheck);
        styleCheckbox(spaceAroundAdditiveOpsCheck);
        styleCheckbox(spaceAroundMultiplicativeOpsCheck);
        styleCheckbox(spaceAroundShiftOpsCheck);
        styleCheckbox(spaceAroundUnaryOpsCheck);
        styleCheckbox(spaceAroundLambdaArrowCheck);
        styleCheckbox(spaceAroundMethodRefDoubleColonCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Around operators", false,
                spaceAroundAssignmentOpsCheck, spaceAroundLogicalOpsCheck,
                spaceAroundEqualityOpsCheck, spaceAroundRelationalOpsCheck,
                spaceAroundBitwiseOpsCheck, spaceAroundAdditiveOpsCheck,
                spaceAroundMultiplicativeOpsCheck, spaceAroundShiftOpsCheck,
                spaceAroundUnaryOpsCheck, spaceAroundLambdaArrowCheck,
                spaceAroundMethodRefDoubleColonCheck
        ));

        // 3. Before left brace (collapsed)
        styleCheckbox(spaceBeforeClassLeftBraceCheck);
        styleCheckbox(spaceBeforeMethodLeftBraceCheck);
        styleCheckbox(spaceBeforeIfLeftBraceCheck);
        styleCheckbox(spaceBeforeElseLeftBraceCheck);
        styleCheckbox(spaceBeforeWhileLeftBraceCheck);
        styleCheckbox(spaceBeforeForLeftBraceCheck);
        styleCheckbox(spaceBeforeDoLeftBraceCheck);
        styleCheckbox(spaceBeforeSwitchLeftBraceCheck);
        styleCheckbox(spaceBeforeTryLeftBraceCheck);
        styleCheckbox(spaceBeforeCatchLeftBraceCheck);
        styleCheckbox(spaceBeforeFinallyLeftBraceCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Before left brace", false,
                spaceBeforeClassLeftBraceCheck, spaceBeforeMethodLeftBraceCheck,
                spaceBeforeIfLeftBraceCheck, spaceBeforeElseLeftBraceCheck,
                spaceBeforeWhileLeftBraceCheck, spaceBeforeForLeftBraceCheck,
                spaceBeforeDoLeftBraceCheck, spaceBeforeSwitchLeftBraceCheck,
                spaceBeforeTryLeftBraceCheck, spaceBeforeCatchLeftBraceCheck,
                spaceBeforeFinallyLeftBraceCheck
        ));

        // 4. Before keywords (collapsed)
        styleCheckbox(spaceBeforeElseCheck);
        styleCheckbox(spaceBeforeWhileCheck);
        styleCheckbox(spaceBeforeCatchCheck);
        styleCheckbox(spaceBeforeFinallyCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Before keywords", false,
                spaceBeforeElseCheck, spaceBeforeWhileCheck, spaceBeforeCatchCheck, spaceBeforeFinallyCheck
        ));

        // 5. Within (collapsed matching reference screenshot 1)
        styleCheckbox(withinCodeBracesCheck);
        styleCheckbox(withinBracketsCheck);
        styleCheckbox(withinArrayInitBracesCheck);
        styleCheckbox(withinEmptyArrayInitBracesCheck);
        styleCheckbox(withinGroupingParensCheck);
        styleCheckbox(withinMethodDeclParensCheck);
        styleCheckbox(withinEmptyMethodDeclParensCheck);
        styleCheckbox(withinMethodCallParensCheck);
        styleCheckbox(withinEmptyMethodCallParensCheck);
        styleCheckbox(withinIfParensCheck);
        styleCheckbox(withinForParensCheck);
        styleCheckbox(withinWhileParensCheck);
        styleCheckbox(withinSwitchParensCheck);
        styleCheckbox(withinTryParensCheck);
        styleCheckbox(withinCatchParensCheck);
        styleCheckbox(withinSynchronizedParensCheck);
        styleCheckbox(withinTypeCastParensCheck);
        styleCheckbox(withinAnnotationParensCheck);
        styleCheckbox(withinAngleBracketsCheck);
        styleCheckbox(withinRecordHeaderCheck);
        styleCheckbox(withinDeconstructionListCheck);
        styleCheckbox(withinBlockBracesWhenBodyCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Within", false,
                withinCodeBracesCheck, withinBracketsCheck, withinArrayInitBracesCheck,
                withinEmptyArrayInitBracesCheck, withinGroupingParensCheck, withinMethodDeclParensCheck,
                withinEmptyMethodDeclParensCheck, withinMethodCallParensCheck, withinEmptyMethodCallParensCheck,
                withinIfParensCheck, withinForParensCheck, withinWhileParensCheck,
                withinSwitchParensCheck, withinTryParensCheck, withinCatchParensCheck,
                withinSynchronizedParensCheck, withinTypeCastParensCheck, withinAnnotationParensCheck,
                withinAngleBracketsCheck, withinRecordHeaderCheck, withinDeconstructionListCheck,
                withinBlockBracesWhenBodyCheck
        ));

        // 6. In ternary operator (?:) (collapsed matching reference screenshot 1)
        ternaryBeforeQuestionCheck.setSelected(true);
        ternaryAfterQuestionCheck.setSelected(true);
        ternaryBeforeColonCheck.setSelected(true);
        ternaryAfterColonCheck.setSelected(true);
        styleCheckbox(ternaryBeforeQuestionCheck);
        styleCheckbox(ternaryAfterQuestionCheck);
        styleCheckbox(ternaryBeforeColonCheck);
        styleCheckbox(ternaryAfterColonCheck);
        spacesBox.getChildren().add(createCollapsibleSection("In ternary operator (?:)", false,
                ternaryBeforeQuestionCheck, ternaryAfterQuestionCheck,
                ternaryBeforeColonCheck, ternaryAfterColonCheck
        ));

        // 7. Type arguments (collapsed matching reference screenshot 1)
        typeArgsAfterCommaCheck.setSelected(true);
        styleCheckbox(typeArgsAfterCommaCheck);
        styleCheckbox(typeArgsBeforeOpenAngleCheck);
        styleCheckbox(typeArgsAfterCloseAngleCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Type arguments", false,
                typeArgsAfterCommaCheck, typeArgsBeforeOpenAngleCheck, typeArgsAfterCloseAngleCheck
        ));

        // 8. Other (collapsed matching reference screenshot 1)
        otherAfterCommaCheck.setSelected(true);
        otherAfterForSemicolonCheck.setSelected(true);
        otherAfterTypeCastCheck.setSelected(true);
        otherAroundEqualsInAnnotationCheck.setSelected(true);
        otherBeforeColonInForEachCheck.setSelected(true);
        styleCheckbox(otherBeforeCommaCheck);
        styleCheckbox(otherAfterCommaCheck);
        styleCheckbox(otherBeforeForSemicolonCheck);
        styleCheckbox(otherAfterForSemicolonCheck);
        styleCheckbox(otherAfterTypeCastCheck);
        styleCheckbox(otherAroundEqualsInAnnotationCheck);
        styleCheckbox(otherBeforeColonInForEachCheck);
        styleCheckbox(otherInsideOneLineEnumBracesCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Other", false,
                otherBeforeCommaCheck, otherAfterCommaCheck, otherBeforeForSemicolonCheck,
                otherAfterForSemicolonCheck, otherAfterTypeCastCheck, otherAroundEqualsInAnnotationCheck,
                otherBeforeColonInForEachCheck, otherInsideOneLineEnumBracesCheck
        ));

        // 9. Type parameters (collapsed matching reference screenshot 1)
        typeParamsAroundTypeBoundsCheck.setSelected(true);
        styleCheckbox(typeParamsBeforeOpenAngleCheck);
        styleCheckbox(typeParamsAroundTypeBoundsCheck);
        spacesBox.getChildren().add(createCollapsibleSection("Type parameters", false,
                typeParamsBeforeOpenAngleCheck, typeParamsAroundTypeBoundsCheck
        ));
    }

    private void buildWrappingPanel() {
        wrappingBox.getChildren().clear();

        // Top table property list matching Screenshot 5
        VBox topPropertiesBox = new VBox(0);
        topPropertiesBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox hwRow = new HBox(12);
        hwRow.setAlignment(Pos.CENTER_LEFT);
        hwRow.setStyle("-fx-background-color: #2A364F; -fx-padding: 4 10;");
        Label hwLbl = new Label("Hard wrap at:");
        hwLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        hwLbl.setPrefWidth(210);
        Label hwVal = new Label("Default: 120");
        hwVal.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        hwRow.getChildren().addAll(hwLbl, hwVal);

        HBox wotRow = new HBox(12);
        wotRow.setAlignment(Pos.CENTER_LEFT);
        wotRow.setStyle("-fx-padding: 4 10;");
        Label wotLbl = new Label("Wrap on typing");
        wotLbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        wotLbl.setPrefWidth(210);
        Label wotVal = new Label("Default: No");
        wotVal.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        wotRow.getChildren().addAll(wotLbl, wotVal);

        HBox vgRow = new HBox(12);
        vgRow.setAlignment(Pos.CENTER_LEFT);
        vgRow.setStyle("-fx-padding: 4 10;");
        Label vgLbl = new Label("Visual guides");
        vgLbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        vgLbl.setPrefWidth(210);
        Label vgVal = new Label("Default: None");
        vgVal.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        vgRow.getChildren().addAll(vgLbl, vgVal);

        topPropertiesBox.getChildren().addAll(hwRow, wotRow, vgRow);
        wrappingBox.getChildren().add(topPropertiesBox);

        // Keep when reformatting (expanded matching screenshot 5)
        keepLineBreaksCheck.setSelected(true);
        commentAtFirstColumnCheck.setSelected(true);
        controlStatementInOneLineCheck.setSelected(true);
        styleCheckbox(keepLineBreaksCheck);
        styleCheckbox(commentAtFirstColumnCheck);
        styleCheckbox(controlStatementInOneLineCheck);
        styleCheckbox(multipleExpressionsInOneLineCheck);
        styleCheckbox(keepSimpleBlocksCheck);
        styleCheckbox(keepSimpleMethodsCheck);
        styleCheckbox(keepSimpleLambdasCheck);
        styleCheckbox(keepSimpleClassesCheck);
        styleCheckbox(ensureRightMarginCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("Keep when reformatting", true,
                keepLineBreaksCheck, commentAtFirstColumnCheck, controlStatementInOneLineCheck,
                multipleExpressionsInOneLineCheck, keepSimpleBlocksCheck, keepSimpleMethodsCheck,
                keepSimpleLambdasCheck, keepSimpleClassesCheck, ensureRightMarginCheck
        ));

        // Braces placement (expanded matching screenshot 5)
        initBraceCombo(classBraceCombo);
        initBraceCombo(methodBraceCombo);
        initBraceCombo(lambdaBraceCombo);
        initBraceCombo(otherBraceCombo);
        wrappingBox.getChildren().add(createCollapsibleSection("Braces placement", true,
                createPropertyRow("In class declaration", classBraceCombo),
                createPropertyRow("In method declaration", methodBraceCombo),
                createPropertyRow("In lambda declaration", lambdaBraceCombo),
                createPropertyRow("Other", otherBraceCombo)
        ));

        // Extends/implements/permits list (matching screenshot 5)
        initWrapCombo(extendsImplementsWrapCombo);
        styleCheckbox(alignExtendsImplementsCheck);
        alignExtendsImplementsCheck.setPadding(new Insets(0, 0, 0, 16));
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Extends/implements/permits list", extendsImplementsWrapCombo, true,
                alignExtendsImplementsCheck
        ));

        // Extends/implements/permits keyword
        initWrapCombo(extendsImplementsKeywordWrapCombo);
        wrappingBox.getChildren().add(createPropertyRow("Extends/implements/permits keyword", extendsImplementsKeywordWrapCombo));

        // Throws list (matching screenshot 5)
        initWrapCombo(throwsWrapCombo);
        styleCheckbox(alignThrowsCheck);
        alignThrowsCheck.setPadding(new Insets(0, 0, 0, 16));
        styleCheckbox(alignThrowsToMethodStartCheck);
        alignThrowsToMethodStartCheck.setPadding(new Insets(0, 0, 0, 16));
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Throws list", throwsWrapCombo, true,
                alignThrowsCheck, alignThrowsToMethodStartCheck
        ));

        // Throws keyword
        initWrapCombo(throwsKeywordWrapCombo);
        wrappingBox.getChildren().add(createPropertyRow("Throws keyword", throwsKeywordWrapCombo));

        // Method declaration parameters (matching screenshot 5)
        initWrapCombo(methodDeclParamsWrapCombo);
        alignMethodDeclParamsCheck.setSelected(true);
        styleCheckbox(alignMethodDeclParamsCheck);
        alignMethodDeclParamsCheck.setPadding(new Insets(0, 0, 0, 16));
        styleCheckbox(newLineAfterMethodDeclLParenCheck);
        newLineAfterMethodDeclLParenCheck.setPadding(new Insets(0, 0, 0, 16));
        styleCheckbox(placeMethodDeclRParenOnNewLineCheck);
        placeMethodDeclRParenOnNewLineCheck.setPadding(new Insets(0, 0, 0, 16));
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Method declaration parameters", methodDeclParamsWrapCombo, true,
                alignMethodDeclParamsCheck, newLineAfterMethodDeclLParenCheck, placeMethodDeclRParenOnNewLineCheck
        ));

        // Method call arguments (matching screenshot 1, 5)
        initWrapCombo(methodCallArgsWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(alignMethodCallArgsCheck);
        styleCheckbox(takePriorityOverCallChainWrapCheck);
        styleCheckbox(newLineAfterMethodCallLParenCheck);
        styleCheckbox(placeMethodCallRParenOnNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Method call arguments", methodCallArgsWrapCombo, true,
                alignMethodCallArgsCheck, takePriorityOverCallChainWrapCheck,
                newLineAfterMethodCallLParenCheck, placeMethodCallRParenOnNewLineCheck
        ));

        // Method parentheses
        styleCheckbox(alignMethodParenthesesCheck);
        styleCheckbox(newLineWhenMethodBodyPresentedCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("Method parentheses", true,
                alignMethodParenthesesCheck, newLineWhenMethodBodyPresentedCheck
        ));

        // Chained method calls
        initWrapCombo(chainedMethodCallsWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(wrapFirstCallCheck);
        styleCheckbox(alignChainedCallsMultilineCheck);
        initStringCombo(builderMethodsCombo, List.of("Method names", "None"), "Method names");
        styleCheckbox(keepBuilderMethodsIndentsCheck);
        styleCheckbox(moveSemicolonToNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Chained method calls", chainedMethodCallsWrapCombo, true,
                wrapFirstCallCheck, alignChainedCallsMultilineCheck,
                createPropertyRow("Builder methods", builderMethodsCombo),
                keepBuilderMethodsIndentsCheck, moveSemicolonToNewLineCheck
        ));

        // 'if()' statement
        initForceBracesCombo(ifForceBracesCombo, "Do not force");
        styleCheckbox(elseOnNewLineCheck);
        specialElseIfTreatmentCheck.setSelected(true);
        styleCheckbox(specialElseIfTreatmentCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("'if()' statement", true,
                createPropertyRow("Force braces", ifForceBracesCombo),
                elseOnNewLineCheck, specialElseIfTreatmentCheck
        ));

        // 'for()' statement
        initWrapCombo(forStatementWrapCombo, WrapOption.DO_NOT_WRAP);
        alignForMultilineCheck.setSelected(true);
        styleCheckbox(alignForMultilineCheck);
        styleCheckbox(newLineAfterForLParenCheck);
        styleCheckbox(placeForRParenOnNewLineCheck);
        initForceBracesCombo(forForceBracesCombo, "Do not force");
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("'for()' statement", forStatementWrapCombo, true,
                alignForMultilineCheck, newLineAfterForLParenCheck, placeForRParenOnNewLineCheck,
                createPropertyRow("Force braces", forForceBracesCombo)
        ));

        // 'while()' statement
        initForceBracesCombo(whileForceBracesCombo, "Do not force");
        wrappingBox.getChildren().add(createCollapsibleSection("'while()' statement", true,
                createPropertyRow("Force braces", whileForceBracesCombo)
        ));

        // 'do ... while()' statement
        initForceBracesCombo(doWhileForceBracesCombo, "Do not force");
        styleCheckbox(whileOnNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("'do ... while()' statement", true,
                createPropertyRow("Force braces", doWhileForceBracesCombo),
                whileOnNewLineCheck
        ));

        // 'switch' statement/expression
        initWrapCombo(switchWrapCombo, WrapOption.WRAP_IF_LONG);
        indentCaseBranchesCheck.setSelected(true);
        styleCheckbox(indentCaseBranchesCheck);
        eachCaseOnSeparateLineCheck.setSelected(true);
        styleCheckbox(eachCaseOnSeparateLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("'switch' statement/expression", switchWrapCombo, true,
                indentCaseBranchesCheck, eachCaseOnSeparateLineCheck
        ));

        // 'try-with-resources'
        initWrapCombo(tryWithResourcesWrapCombo, WrapOption.DO_NOT_WRAP);
        alignTryWithResourcesCheck.setSelected(true);
        styleCheckbox(alignTryWithResourcesCheck);
        styleCheckbox(newLineAfterTryWithResourcesLParenCheck);
        styleCheckbox(placeTryWithResourcesRParenOnNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("'try-with-resources'", tryWithResourcesWrapCombo, true,
                alignTryWithResourcesCheck, newLineAfterTryWithResourcesLParenCheck, placeTryWithResourcesRParenOnNewLineCheck
        ));

        // 'try' statement
        styleCheckbox(catchOnNewLineCheck);
        styleCheckbox(finallyOnNewLineCheck);
        initWrapCombo(typesInMultiCatchWrapCombo, WrapOption.WRAP_IF_LONG);
        alignTypesInMultiCatchCheck.setSelected(true);
        styleCheckbox(alignTypesInMultiCatchCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("'try' statement", true,
                catchOnNewLineCheck, finallyOnNewLineCheck,
                createPropertyRow("Types in multi-catch", typesInMultiCatchWrapCombo),
                alignTypesInMultiCatchCheck
        ));

        // Binary expressions
        initWrapCombo(binaryExpressionsWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(alignBinaryExpressionsCheck);
        styleCheckbox(operationSignOnNextLineCheck);
        styleCheckbox(alignParenthesisedCheck);
        styleCheckbox(newLineAfterBinaryLParenCheck);
        styleCheckbox(placeBinaryRParenOnNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Binary expressions", binaryExpressionsWrapCombo, true,
                alignBinaryExpressionsCheck, operationSignOnNextLineCheck, alignParenthesisedCheck,
                newLineAfterBinaryLParenCheck, placeBinaryRParenOnNewLineCheck
        ));

        // Assignment statement
        initWrapCombo(assignmentWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(alignAssignmentCheck);
        styleCheckbox(assignmentSignOnNextLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Assignment statement", assignmentWrapCombo, true,
                alignAssignmentCheck, assignmentSignOnNextLineCheck
        ));

        // Group declarations
        styleCheckbox(alignFieldsInColumnsCheck);
        styleCheckbox(alignVariablesInColumnsCheck);
        styleCheckbox(alignAssignmentsInColumnsCheck);
        styleCheckbox(alignSimpleMethodsInColumnsCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("Group declarations", true,
                alignFieldsInColumnsCheck, alignVariablesInColumnsCheck,
                alignAssignmentsInColumnsCheck, alignSimpleMethodsInColumnsCheck
        ));

        // Ternary operation
        initWrapCombo(ternaryWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(alignTernaryCheck);
        styleCheckbox(ternarySignsOnNextLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Ternary operation", ternaryWrapCombo, true,
                alignTernaryCheck, ternarySignsOnNextLineCheck
        ));

        // Array initializer
        initWrapCombo(arrayInitializerWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(alignArrayInitializerCheck);
        styleCheckbox(newLineAfterArrayInitializerLBraceCheck);
        styleCheckbox(placeArrayInitializerRBraceOnNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Array initializer", arrayInitializerWrapCombo, true,
                alignArrayInitializerCheck, newLineAfterArrayInitializerLBraceCheck, placeArrayInitializerRBraceOnNewLineCheck
        ));

        // Modifier list
        styleCheckbox(wrapAfterModifierListCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("Modifier list", true,
                wrapAfterModifierListCheck
        ));

        // Assert statement
        initWrapCombo(assertStatementWrapCombo, WrapOption.DO_NOT_WRAP);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Assert statement", assertStatementWrapCombo, false));

        // Enum constants
        initWrapCombo(enumConstantsWrapCombo, WrapOption.DO_NOT_WRAP);
        wrappingBox.getChildren().add(createPropertyRow("Enum constants", enumConstantsWrapCombo));

        // Class annotations
        initWrapCombo(classAnnotationsWrapCombo, WrapOption.WRAP_ALWAYS);
        wrappingBox.getChildren().add(createPropertyRow("Class annotations", classAnnotationsWrapCombo));

        // Method annotations
        initWrapCombo(methodAnnotationsWrapCombo, WrapOption.WRAP_ALWAYS);
        wrappingBox.getChildren().add(createPropertyRow("Method annotations", methodAnnotationsWrapCombo));

        // Field annotations
        initWrapCombo(fieldAnnotationsWrapCombo, WrapOption.WRAP_ALWAYS);
        styleCheckbox(doNotWrapAfterSingleFieldAnnotationCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Field annotations", fieldAnnotationsWrapCombo, true,
                doNotWrapAfterSingleFieldAnnotationCheck
        ));

        // Parameter annotations
        initWrapCombo(parameterAnnotationsWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(doNotWrapAfterSingleParameterAnnotationCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Parameter annotations", parameterAnnotationsWrapCombo, true,
                doNotWrapAfterSingleParameterAnnotationCheck
        ));

        // Local variable annotations
        initWrapCombo(localVariableAnnotationsWrapCombo, WrapOption.DO_NOT_WRAP);
        wrappingBox.getChildren().add(createPropertyRow("Local variable annotations", localVariableAnnotationsWrapCombo));

        // Enum field annotations
        initWrapCombo(enumFieldAnnotationsWrapCombo, WrapOption.DO_NOT_WRAP);
        wrappingBox.getChildren().add(createPropertyRow("Enum field annotations", enumFieldAnnotationsWrapCombo));

        // Annotation parameters
        initWrapCombo(annotationParametersWrapCombo, WrapOption.DO_NOT_WRAP);
        styleCheckbox(alignAnnotationParametersCheck);
        styleCheckbox(newLineAfterAnnotationParametersLParenCheck);
        styleCheckbox(placeAnnotationParametersRParenOnNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Annotation parameters", annotationParametersWrapCombo, true,
                alignAnnotationParametersCheck, newLineAfterAnnotationParametersLParenCheck, placeAnnotationParametersRParenOnNewLineCheck
        ));

        // Text blocks
        styleCheckbox(alignTextBlocksCheck);
        wrappingBox.getChildren().add(createCollapsibleSection("Text blocks", true,
                alignTextBlocksCheck
        ));

        // Record components
        initWrapCombo(recordComponentsWrapCombo, WrapOption.WRAP_IF_LONG);
        alignRecordComponentsCheck.setSelected(true);
        styleCheckbox(alignRecordComponentsCheck);
        styleCheckbox(newLineAfterRecordComponentsLParenCheck);
        styleCheckbox(placeRecordComponentsRParenOnNewLineCheck);
        styleCheckbox(newLineForRecordComponentAnnotationsCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Record components", recordComponentsWrapCombo, true,
                alignRecordComponentsCheck, newLineAfterRecordComponentsLParenCheck,
                placeRecordComponentsRParenOnNewLineCheck, newLineForRecordComponentAnnotationsCheck
        ));

        // Deconstruction patterns
        initWrapCombo(deconstructionPatternsWrapCombo, WrapOption.WRAP_IF_LONG);
        alignDeconstructionPatternsCheck.setSelected(true);
        newLineAfterDeconstructionPatternsLParenCheck.setSelected(true);
        placeDeconstructionPatternsRParenOnNewLineCheck.setSelected(true);
        styleCheckbox(alignDeconstructionPatternsCheck);
        styleCheckbox(newLineAfterDeconstructionPatternsLParenCheck);
        styleCheckbox(placeDeconstructionPatternsRParenOnNewLineCheck);
        wrappingBox.getChildren().add(createCollapsibleSectionWithControl("Deconstruction patterns", deconstructionPatternsWrapCombo, true,
                alignDeconstructionPatternsCheck, newLineAfterDeconstructionPatternsLParenCheck,
                placeDeconstructionPatternsRParenOnNewLineCheck
        ));
    }

    private void initBraceCombo(ComboBox<BracePlacement> combo) {
        combo.getItems().setAll(BracePlacement.values());
        combo.setValue(BracePlacement.END_OF_LINE);
        styleComboBox(combo);
    }

    private void initWrapCombo(ComboBox<WrapOption> combo) {
        initWrapCombo(combo, WrapOption.DO_NOT_WRAP);
    }

    private void initWrapCombo(ComboBox<WrapOption> combo, WrapOption defaultVal) {
        combo.getItems().setAll(WrapOption.values());
        combo.setValue(defaultVal != null ? defaultVal : WrapOption.DO_NOT_WRAP);
        styleComboBox(combo);
    }

    private void initForceBracesCombo(ComboBox<String> combo, String defaultVal) {
        combo.getItems().setAll("Do not force", "When multiline", "Always");
        combo.setValue(defaultVal != null ? defaultVal : "Do not force");
        styleComboBox(combo);
    }

    private void initStringCombo(ComboBox<String> combo, List<String> items, String defaultVal) {
        combo.getItems().setAll(items);
        combo.setValue(defaultVal != null ? defaultVal : (items.isEmpty() ? "" : items.get(0)));
        styleComboBox(combo);
    }

    private VBox createCollapsibleSection(String title, boolean initiallyExpanded, Node... contentNodes) {
        VBox container = new VBox(2);
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand; -fx-padding: 3 0 2 0;");

        Label arrow = new Label(initiallyExpanded ? "▾" : "▸");
        arrow.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-min-width: 12px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        header.getChildren().addAll(arrow, titleLbl);

        VBox content = new VBox(4);
        content.setPadding(new Insets(2, 0, 4, 18));
        content.getChildren().addAll(contentNodes);
        content.setVisible(initiallyExpanded);
        content.setManaged(initiallyExpanded);

        header.setOnMouseClicked(e -> {
            boolean exp = !content.isVisible();
            content.setVisible(exp);
            content.setManaged(exp);
            arrow.setText(exp ? "▾" : "▸");
        });

        container.getChildren().addAll(header, content);
        return container;
    }

    private VBox createCollapsibleSectionWithControl(String title, Node control, boolean initiallyExpanded, Node... contentNodes) {
        VBox container = new VBox(2);
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 3 0 2 0;");

        Label arrow = new Label(initiallyExpanded ? "▾" : "▸");
        arrow.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-min-width: 12px; -fx-cursor: hand;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        titleLbl.setPrefWidth(222);

        header.getChildren().addAll(arrow, titleLbl, control);

        VBox content = new VBox(4);
        content.setPadding(new Insets(2, 0, 4, 18));
        content.getChildren().addAll(contentNodes);
        content.setVisible(initiallyExpanded);
        content.setManaged(initiallyExpanded);

        Runnable toggle = () -> {
            boolean exp = !content.isVisible();
            content.setVisible(exp);
            content.setManaged(exp);
            arrow.setText(exp ? "▾" : "▸");
        };

        arrow.setOnMouseClicked(e -> toggle.run());
        titleLbl.setOnMouseClicked(e -> toggle.run());

        container.getChildren().addAll(header, content);
        return container;
    }

    private HBox createPropertyRow(String labelText, Node control) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 3 0;");
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        lbl.setPrefWidth(240);
        row.getChildren().addAll(lbl, control);
        return row;
    }

    private void buildBlankLinesPanel() {
        blankLinesBox.getChildren().clear();
        blankLinesBox.setSpacing(4);

        // Section 1: Keep maximum blank lines matching Screenshot 4
        blankLinesBox.getChildren().add(createBlankLinesSectionHeader("Keep maximum blank lines"));
        blankLinesBox.getChildren().add(createBlankLinesRow("In declarations:", keepBlankLinesInDeclField));
        blankLinesBox.getChildren().add(createBlankLinesRow("In code:", keepBlankLinesInCodeField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before '}':", keepBlankLinesBeforeRBraceField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Between header and package:", keepBlankLinesBetweenHeaderAndPackageField));

        // Section 2: Minimum blank lines matching Screenshot 4 & 5
        blankLinesBox.getChildren().add(createBlankLinesSectionHeader("Minimum blank lines"));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before package statement:", blankLinesBeforePackageField));
        blankLinesBox.getChildren().add(createBlankLinesRow("After package statement:", blankLinesAfterPackageField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before imports:", blankLinesBeforeImportsField));
        blankLinesBox.getChildren().add(createBlankLinesRow("After imports:", blankLinesAfterImportsField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Around class:", blankLinesAroundClassField));
        blankLinesBox.getChildren().add(createBlankLinesRow("After class header:", blankLinesAfterClassHeaderField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before class end:", blankLinesBeforeClassEndField));
        blankLinesBox.getChildren().add(createBlankLinesRow("After anonymous class header:", blankLinesAfterAnonymousClassHeaderField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before field in interface:", blankLinesBeforeFieldInInterfaceField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before field without annotations:", blankLinesBeforeFieldWithoutAnnotationsField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before field with annotations:", blankLinesBeforeFieldWithAnnotationsField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Around method in interface:", blankLinesAroundMethodInInterfaceField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Around method:", blankLinesAroundMethodField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Before method body:", blankLinesBeforeMethodBodyField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Around initializer:", blankLinesAroundInitializerField));
        blankLinesBox.getChildren().add(createBlankLinesRow("Between record components:", blankLinesBetweenRecordComponentsField));
    }

    private HBox createBlankLinesRow(String labelText, TextField tf) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 2 0;");
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        lbl.setPrefWidth(240);
        styleNumericTextField(tf, 48);
        row.getChildren().addAll(lbl, tf);
        return row;
    }

    private void styleNumericTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setPrefHeight(26);
        String normalStyle = "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6; -fx-font-size: 13px;";
        String focusedStyle = "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6; -fx-font-size: 13px;";
        tf.setStyle(normalStyle);
        tf.focusedProperty().addListener((obs, old, isFocused) -> {
            tf.setStyle(isFocused ? focusedStyle : normalStyle);
        });
    }

    private VBox createBlankLinesSectionHeader(String title) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 0, 4, 0));
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.6;");
        box.getChildren().addAll(lbl, sep);
        return box;
    }

    private static Button createMiniToolbarButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #BCBEC4; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 2 8; " +
                "-fx-border-color: transparent;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #393B40; " +
                "-fx-background-radius: 4; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 2 8; " +
                "-fx-border-color: transparent;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #BCBEC4; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 2 8; " +
                "-fx-border-color: transparent;"
        ));
        return btn;
    }

    private VBox createJavaDocCollapsibleSection(String title, Node... nodes) {
        VBox sectionBox = new VBox(4);

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand; -fx-padding: 3 0;");

        Label arrow = new Label("▼");
        arrow.setStyle("-fx-font-size: 9px; -fx-text-fill: #868A91;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        header.getChildren().addAll(arrow, titleLabel);

        VBox content = new VBox(6);
        content.setPadding(new Insets(2, 0, 4, 16));
        content.getChildren().addAll(nodes);
        content.disableProperty().bind(enableJavaDocFormattingCheck.selectedProperty().not());

        header.setOnMouseClicked(e -> {
            boolean visible = !content.isVisible();
            content.setVisible(visible);
            content.setManaged(visible);
            arrow.setText(visible ? "▼" : "▶");
        });

        sectionBox.getChildren().addAll(header, content);
        return sectionBox;
    }

    private void buildJavaDocPanel() {
        javadocBox.getChildren().clear();
        javadocBox.setStyle("-fx-padding: 4 10 20 4;");

        styleCheckbox(enableJavaDocFormattingCheck);
        javadocBox.getChildren().add(enableJavaDocFormattingCheck);

        // Alignment
        styleCheckbox(alignParamDescriptionsCheck);
        styleCheckbox(alignThrownExceptionsCheck);
        VBox alignGroup = createJavaDocCollapsibleSection("Alignment",
                alignParamDescriptionsCheck, alignThrownExceptionsCheck);

        // Blank lines
        styleCheckbox(blankLinesAfterDescCheck);
        styleCheckbox(blankLinesAfterParamDescCheck);
        styleCheckbox(blankLinesAfterReturnTagCheck);
        VBox blankGroup = createJavaDocCollapsibleSection("Blank lines",
                blankLinesAfterDescCheck, blankLinesAfterParamDescCheck, blankLinesAfterReturnTagCheck);

        // Invalid tags
        styleCheckbox(keepInvalidTagsCheck);
        styleCheckbox(keepEmptyParamTagsCheck);
        styleCheckbox(keepEmptyReturnTagsCheck);
        styleCheckbox(keepEmptyThrowsTagsCheck);
        VBox invalidGroup = createJavaDocCollapsibleSection("Invalid tags",
                keepInvalidTagsCheck, keepEmptyParamTagsCheck, keepEmptyReturnTagsCheck, keepEmptyThrowsTagsCheck);

        // Other
        styleCheckbox(wrapAtRightMarginCheck);
        styleCheckbox(enableLeadingAsterisksCheck);
        styleCheckbox(useThrowsRatherThanExceptionCheck);
        styleCheckbox(generatePOnEmptyLinesCheck);
        styleCheckbox(keepEmptyLinesCheck);
        styleCheckbox(doNotWrapOneLineCommentsCheck);
        styleCheckbox(preserveLineFeedsCheck);
        styleCheckbox(paramDescriptionsOnNewLineCheck);
        styleCheckbox(indentContinuationLinesCheck);
        VBox otherGroup = createJavaDocCollapsibleSection("Other",
                wrapAtRightMarginCheck, enableLeadingAsterisksCheck,
                useThrowsRatherThanExceptionCheck, generatePOnEmptyLinesCheck,
                keepEmptyLinesCheck, doNotWrapOneLineCommentsCheck,
                preserveLineFeedsCheck, paramDescriptionsOnNewLineCheck,
                indentContinuationLinesCheck);

        javadocBox.getChildren().addAll(alignGroup, blankGroup, invalidGroup, otherGroup);
    }

    private void setupPackagesImportTable() {
        if (!packagesToUseImportTable.getColumns().isEmpty()) return;

        packagesToUseImportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        packagesToUseImportTable.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-width: 1; " +
                "-fx-table-cell-border-color: transparent;"
        );
        packagesToUseImportTable.setPrefHeight(100);
        packagesToUseImportTable.setMinHeight(100);

        TableColumn<ImportEntry, Boolean> staticCol = new TableColumn<>("Static");
        staticCol.setPrefWidth(55);
        staticCol.setMinWidth(50);
        staticCol.setMaxWidth(65);
        staticCol.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isStatic()));
        staticCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                styleCheckbox(cb);
                cb.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ImportEntry entry = getTableView().getItems().get(getIndex());
                        entry.setStatic(cb.isSelected());
                        onFieldChanged();
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    cb.setSelected(item);
                    setAlignment(Pos.CENTER);
                    setGraphic(cb);
                }
            }
        });

        TableColumn<ImportEntry, String> pkgCol = new TableColumn<>("Package");
        pkgCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPackageName()));
        pkgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Text kw = new Text("import ");
                    kw.setFill(Color.web("#CF8E6D"));
                    kw.setFont(Font.font("Consolas", 12));

                    Text pkg = new Text(item);
                    pkg.setFill(Color.web("#DFE1E5"));
                    pkg.setFont(Font.font("Consolas", 12));

                    TextFlow flow = new TextFlow(kw, pkg);
                    // left aligned by default
                    setGraphic(flow);
                }
            }
        });

        TableColumn<ImportEntry, Boolean> subCol = new TableColumn<>("With Subpackages");
        subCol.setPrefWidth(140);
        subCol.setMinWidth(120);
        subCol.setMaxWidth(160);
        subCol.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isWithSubpackages()));
        subCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                styleCheckbox(cb);
                cb.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ImportEntry entry = getTableView().getItems().get(getIndex());
                        entry.setWithSubpackages(cb.isSelected());
                        onFieldChanged();
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    cb.setSelected(item);
                    setAlignment(Pos.CENTER);
                    setGraphic(cb);
                }
            }
        });

        packagesToUseImportTable.getColumns().addAll(staticCol, pkgCol, subCol);

        addPackageImportBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("org.example.*");
            dialog.setTitle("Add Package to Use Import with '*'");
            dialog.setHeaderText("Specify package pattern:");
            dialog.setContentText("Package:");
            dialog.showAndWait().ifPresent(pkg -> {
                if (!pkg.isBlank()) {
                    packagesToUseImportTable.getItems().add(new ImportEntry(false, pkg.trim(), false));
                    onFieldChanged();
                }
            });
        });

        removePackageImportBtn.setOnAction(e -> {
            int idx = packagesToUseImportTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < packagesToUseImportTable.getItems().size()) {
                packagesToUseImportTable.getItems().remove(idx);
                onFieldChanged();
            }
        });
    }

    private static Node renderImportLayoutCell(ImportLayoutEntry entry) {
        if (entry.getType() == ImportLayoutEntry.EntryType.BLANK_LINE) {
            Label lbl = new Label("<blank line>");
            lbl.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic; -fx-font-family: Consolas, monospace; -fx-font-size: 12px;");
            return lbl;
        }

        Text kw = new Text();
        kw.setFill(Color.web("#CF8E6D"));
        kw.setFont(Font.font("Consolas", 12));

        Text text = new Text();
        text.setFill(Color.web("#BCBEC4"));
        text.setFont(Font.font("Consolas", 12));

        switch (entry.getType()) {
            case MODULE_IMPORTS -> {
                kw.setText("import ");
                text.setText("module imports");
            }
            case ALL_OTHER_IMPORTS -> {
                kw.setText("import ");
                text.setText("all other imports");
            }
            case STATIC_ALL_OTHER_IMPORTS -> {
                kw.setText("import static ");
                text.setText("all other imports");
            }
            case PACKAGE -> {
                kw.setText(entry.isStatic() ? "import static " : "import ");
                text.setText(entry.getPackageName());
            }
            default -> text.setText(entry.getPackageName());
        }

        TextFlow flow = new TextFlow(kw, text);
        // left aligned by default
        return flow;
    }

    private void setupImportLayoutTable() {
        if (!importLayoutTable.getColumns().isEmpty()) return;

        importLayoutTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        importLayoutTable.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-width: 1; " +
                "-fx-table-cell-border-color: transparent;"
        );
        importLayoutTable.setPrefHeight(180);
        importLayoutTable.setMinHeight(160);

        TableColumn<ImportLayoutEntry, Boolean> staticCol = new TableColumn<>("Static");
        staticCol.setPrefWidth(55);
        staticCol.setMinWidth(50);
        staticCol.setMaxWidth(65);
        staticCol.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isStatic()));
        staticCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                styleCheckbox(cb);
                cb.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ImportLayoutEntry entry = getTableView().getItems().get(getIndex());
                        entry.setStatic(cb.isSelected());
                        onFieldChanged();
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    ImportLayoutEntry entry = getTableView().getItems().get(getIndex());
                    if (entry.getType() == ImportLayoutEntry.EntryType.PACKAGE) {
                        cb.setSelected(entry.isStatic());
                        setAlignment(Pos.CENTER);
                        setGraphic(cb);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        TableColumn<ImportLayoutEntry, String> pkgCol = new TableColumn<>("Package");
        pkgCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPackageName()));
        pkgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ImportLayoutEntry entry = getTableView().getItems().get(getIndex());
                    setGraphic(renderImportLayoutCell(entry));
                }
            }
        });

        TableColumn<ImportLayoutEntry, Boolean> subCol = new TableColumn<>("With Subpackages");
        subCol.setPrefWidth(140);
        subCol.setMinWidth(120);
        subCol.setMaxWidth(160);
        subCol.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isWithSubpackages()));
        subCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                styleCheckbox(cb);
                cb.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ImportLayoutEntry entry = getTableView().getItems().get(getIndex());
                        entry.setWithSubpackages(cb.isSelected());
                        onFieldChanged();
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    ImportLayoutEntry entry = getTableView().getItems().get(getIndex());
                    if (entry.getType() == ImportLayoutEntry.EntryType.PACKAGE) {
                        cb.setSelected(entry.isWithSubpackages());
                        setAlignment(Pos.CENTER);
                        setGraphic(cb);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        importLayoutTable.getColumns().addAll(staticCol, pkgCol, subCol);

        addImportLayoutBtn.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 2;");

            MenuItem addPkgItem = new MenuItem("Add Package...");
            addPkgItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            addPkgItem.setOnAction(ev -> {
                TextInputDialog dialog = new TextInputDialog("com.example.*");
                dialog.setTitle("Add Package Import");
                dialog.setHeaderText("Specify package pattern:");
                dialog.setContentText("Package:");
                dialog.showAndWait().ifPresent(pkg -> {
                    if (!pkg.isBlank()) {
                        importLayoutTable.getItems().add(ImportLayoutEntry.packageEntry(false, pkg.trim(), true));
                        onFieldChanged();
                    }
                });
            });

            MenuItem addBlankItem = new MenuItem("Add Blank");
            addBlankItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            addBlankItem.setOnAction(ev -> {
                importLayoutTable.getItems().add(ImportLayoutEntry.blankLine());
                onFieldChanged();
            });

            menu.getItems().addAll(addPkgItem, addBlankItem);
            menu.show(addImportLayoutBtn, Side.BOTTOM, 0, 0);
        });

        removeImportLayoutBtn.setOnAction(e -> {
            int idx = importLayoutTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < importLayoutTable.getItems().size()) {
                importLayoutTable.getItems().remove(idx);
                onFieldChanged();
            }
        });

        moveUpImportLayoutBtn.setOnAction(e -> {
            int idx = importLayoutTable.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                ImportLayoutEntry entry = importLayoutTable.getItems().remove(idx);
                importLayoutTable.getItems().add(idx - 1, entry);
                importLayoutTable.getSelectionModel().select(idx - 1);
                onFieldChanged();
            }
        });

        moveDownImportLayoutBtn.setOnAction(e -> {
            int idx = importLayoutTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < importLayoutTable.getItems().size() - 1) {
                ImportLayoutEntry entry = importLayoutTable.getItems().remove(idx);
                importLayoutTable.getItems().add(idx + 1, entry);
                importLayoutTable.getSelectionModel().select(idx + 1);
                onFieldChanged();
            }
        });
    }

    private void buildImportsPanel() {
        importsBox.getChildren().clear();
        importsBox.setStyle("-fx-padding: 4 10 20 4;");

        // General
        importsBox.getChildren().add(createSectionHeader("General"));

        styleCheckbox(useSingleClassImportCheck);
        styleCheckbox(useFullyQualifiedClassNamesCheck);
        styleCheckbox(insertInnerClassImportsCheck);

        // Exclude inner classes sub-panel
        VBox innerClassesSubBox = new VBox(4);
        innerClassesSubBox.setPadding(new Insets(2, 0, 4, 20));

        HBox innerClassesToolbar = new HBox(4);
        innerClassesToolbar.setAlignment(Pos.CENTER_LEFT);
        innerClassesToolbar.getChildren().addAll(addInnerClassBtn, removeInnerClassBtn);

        Label excludeLabel = new Label("Exclude inner classes by short name:");
        excludeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        excludeInnerClassesList.setPlaceholder(new Label("No inner classes defined"));
        excludeInnerClassesList.setPrefHeight(90);
        excludeInnerClassesList.setMaxHeight(90);
        excludeInnerClassesList.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;"
        );

        addInnerClassBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Exclude Inner Class");
            dialog.setHeaderText("Specify short name of inner class to exclude from import:");
            dialog.setContentText("Class name:");
            dialog.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    excludeInnerClassesList.getItems().add(name.trim());
                    onFieldChanged();
                }
            });
        });

        removeInnerClassBtn.setOnAction(e -> {
            int idx = excludeInnerClassesList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < excludeInnerClassesList.getItems().size()) {
                excludeInnerClassesList.getItems().remove(idx);
                onFieldChanged();
            }
        });

        innerClassesSubBox.getChildren().addAll(innerClassesToolbar, excludeLabel, excludeInnerClassesList);

        styleCheckbox(doNotSeparateModuleImportsCheck);
        styleCheckbox(deleteUnusedModuleImportsCheck);

        // Use fully qualified class names in JavaDoc
        HBox fqJavadocRow = new HBox(8);
        fqJavadocRow.setAlignment(Pos.CENTER_LEFT);
        Label fqJavadocLabel = new Label("Use fully qualified class names in JavaDoc:");
        fqJavadocLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        useFqNamesInJavadocCombo.getItems().setAll(
                "Always",
                "Never, use short name and add import",
                "If not already imported"
        );
        useFqNamesInJavadocCombo.setValue("If not already imported");
        useFqNamesInJavadocCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-font-size: 12px;");
        fqJavadocRow.getChildren().addAll(fqJavadocLabel, useFqNamesInJavadocCombo);

        styleNumericTextField(classCountImportField, 48);
        styleNumericTextField(namesCountStaticImportField, 48);

        importsBox.getChildren().addAll(
                useSingleClassImportCheck,
                useFullyQualifiedClassNamesCheck,
                insertInnerClassImportsCheck,
                innerClassesSubBox,
                doNotSeparateModuleImportsCheck,
                deleteUnusedModuleImportsCheck,
                fqJavadocRow,
                createLabeledField("Class count to use import with *:", classCountImportField),
                createLabeledField("Names count to use static import with *:", namesCountStaticImportField)
        );

        // Packages to Use Import with *
        importsBox.getChildren().add(createSectionHeader("Packages to Use Import with *:"));

        HBox pkgToolbar = new HBox(4);
        pkgToolbar.setAlignment(Pos.CENTER_LEFT);
        pkgToolbar.getChildren().addAll(addPackageImportBtn, removePackageImportBtn);

        setupPackagesImportTable();

        styleCheckbox(placeOnDemandImportBeforeSingleClassCheck);
        styleCheckbox(layoutStaticImportsSeparatelyCheck);

        importsBox.getChildren().addAll(
                pkgToolbar,
                packagesToUseImportTable,
                placeOnDemandImportBeforeSingleClassCheck,
                layoutStaticImportsSeparatelyCheck
        );

        // Import layout
        importsBox.getChildren().add(createSectionHeader("Import layout:"));

        HBox layoutToolbar = new HBox(4);
        layoutToolbar.setAlignment(Pos.CENTER_LEFT);
        layoutToolbar.getChildren().addAll(addImportLayoutBtn, removeImportLayoutBtn, moveUpImportLayoutBtn, moveDownImportLayoutBtn);

        setupImportLayoutTable();

        importsBox.getChildren().addAll(
                layoutToolbar,
                importLayoutTable
        );
    }

    private Node createGroupHeaderWithLine(String title) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(sep, Priority.ALWAYS);
        box.getChildren().addAll(lbl, sep);
        box.setPadding(new Insets(8, 0, 4, 0));
        return box;
    }

    private Node createCircleBadge(int number) {
        Label badge = new Label(String.valueOf(number));
        badge.setMinSize(18, 18);
        badge.setMaxSize(18, 18);
        badge.setPrefSize(18, 18);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-border-color: #4E5157; -fx-border-radius: 9; -fx-text-fill: #868A91; -fx-font-size: 10px;");
        return badge;
    }

    private GridPane createPrefixSuffixGrid(String prefixColHeader, String suffixColHeader) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label pHeader = new Label(prefixColHeader);
        pHeader.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        grid.add(pHeader, 1, 0);

        Label sHeader = new Label(suffixColHeader);
        sHeader.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        grid.add(sHeader, 2, 0);

        return grid;
    }

    private void addPrefixSuffixRow(GridPane grid, int rowIdx, String labelText, TextField prefixField, TextField suffixField, double labelWidth) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        lbl.setPrefWidth(labelWidth);
        lbl.setAlignment(Pos.CENTER_RIGHT);

        styleTextField(prefixField, 115);
        styleTextField(suffixField, 115);

        grid.add(lbl, 0, rowIdx);
        grid.add(prefixField, 1, rowIdx);
        grid.add(suffixField, 2, rowIdx);
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
    }

    private void stylePillButton(ToggleButton btn) {
        btn.setStyle("-fx-background-color: #26282B; -fx-text-fill: #BCBEC4; -fx-border-color: #393B40; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 2 10; -fx-font-size: 12px; -fx-cursor: hand;");
        btn.selectedProperty().addListener((obs, o, selected) -> {
            if (selected) {
                btn.setStyle("-fx-background-color: #2A4B7C; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; " +
                        "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 2 10; -fx-font-size: 12px; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: #26282B; -fx-text-fill: #BCBEC4; -fx-border-color: #393B40; " +
                        "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 2 10; -fx-font-size: 12px; -fx-cursor: hand;");
            }
        });
    }

    private void buildArrangementPanel() {
        arrangementBox.getChildren().clear();
        arrangementBox.setStyle("-fx-padding: 4 10 20 4;");

        // 1. Grouping rules
        HBox groupHeaderBox = new HBox(8);
        groupHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label groupHeaderLbl = new Label("Grouping rules:");
        groupHeaderLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Region groupSpacer = new Region();
        HBox.setHgrow(groupSpacer, Priority.ALWAYS);
        Button groupSortBtn = createMiniToolbarButton("↑");
        groupHeaderBox.getChildren().addAll(groupHeaderLbl, groupSpacer, groupSortBtn);

        // Row 1
        HBox gRow1 = new HBox(8);
        gRow1.setAlignment(Pos.CENTER_LEFT);
        styleCheckbox(keepGettersSettersTogetherCheck);
        gRow1.getChildren().addAll(createCircleBadge(1), keepGettersSettersTogetherCheck);

        // Row 2
        HBox gRow2 = new HBox(8);
        gRow2.setAlignment(Pos.CENTER_LEFT);
        styleCheckbox(keepOverriddenMethodsTogetherCheck);
        overriddenMethodsOrderCombo.getItems().setAll("keep order", "order by name");
        overriddenMethodsOrderCombo.setValue("keep order");
        styleComboBox(overriddenMethodsOrderCombo);
        overriddenMethodsOrderCombo.setPrefWidth(120);
        gRow2.getChildren().addAll(createCircleBadge(2), keepOverriddenMethodsTogetherCheck, overriddenMethodsOrderCombo);

        // Row 3
        HBox gRow3 = new HBox(8);
        gRow3.setAlignment(Pos.CENTER_LEFT);
        styleCheckbox(keepDependentMethodsTogetherCheck);
        dependentMethodsOrderCombo.getItems().setAll("breadth-first order", "depth-first order");
        dependentMethodsOrderCombo.setValue("breadth-first order");
        styleComboBox(dependentMethodsOrderCombo);
        dependentMethodsOrderCombo.setPrefWidth(150);
        gRow3.getChildren().addAll(createCircleBadge(3), keepDependentMethodsTogetherCheck, dependentMethodsOrderCombo);

        VBox groupingRulesBox = new VBox(6);
        groupingRulesBox.getChildren().addAll(groupHeaderBox, gRow1, gRow2, gRow3);

        // 2. Matching rules
        HBox matchHeaderBox = new HBox(8);
        matchHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label matchHeaderLbl = new Label("Matching rules:");
        matchHeaderLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Region matchSpacer = new Region();
        HBox.setHgrow(matchSpacer, Priority.ALWAYS);

        addRuleBtn.setTooltip(new Tooltip("Add rule Alt+Insert"));
        addSectionRuleBtn.setTooltip(new Tooltip("Add section rule"));
        removeRuleBtn.setTooltip(new Tooltip("Remove rule"));
        moveUpRuleBtn.setTooltip(new Tooltip("Move rule up"));
        moveDownRuleBtn.setTooltip(new Tooltip("Move rule down"));
        ruleSettingsBtn.setTooltip(new Tooltip("Rule settings"));

        HBox matchToolbar = new HBox(4);
        matchToolbar.setAlignment(Pos.CENTER_RIGHT);
        matchToolbar.getChildren().addAll(addRuleBtn, addSectionRuleBtn, removeRuleBtn, moveUpRuleBtn, moveDownRuleBtn, ruleSettingsBtn);
        matchHeaderBox.getChildren().addAll(matchHeaderLbl, matchSpacer, matchToolbar);

        setupMatchingRulesList();
        setupRuleEditorCard();

        arrangementBox.getChildren().addAll(
                groupingRulesBox,
                matchHeaderBox,
                matchingRulesList,
                ruleEditorCard
        );
    }

    private void setupMatchingRulesList() {
        matchingRulesList.setPrefHeight(280);
        matchingRulesList.setMaxHeight(320);
        matchingRulesList.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;"
        );

        matchingRulesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JavaCodeStyleSettings.ArrangementRule rule, boolean empty) {
                super.updateItem(rule, empty);
                if (empty || rule == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    int index = getIndex() + 1;
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(2, 6, 2, 6));

                    Node badge = createCircleBadge(index);
                    HBox tagsBox = new HBox(6);
                    tagsBox.setAlignment(Pos.CENTER_LEFT);

                    for (String tag : rule.getTags()) {
                        Label tagPill = new Label(tag);
                        tagPill.setStyle("-fx-background-color: #26282B; -fx-text-fill: #BCBEC4; " +
                                "-fx-border-color: #393B40; -fx-border-radius: 12; -fx-background-radius: 12; " +
                                "-fx-padding: 2 10; -fx-font-size: 12px;");
                        tagsBox.getChildren().add(tagPill);
                    }

                    Region rowSpacer = new Region();
                    HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                    row.getChildren().addAll(badge, tagsBox, rowSpacer);

                    if (isSelected()) {
                        Label editIcon = new Label("✏");
                        editIcon.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
                        row.getChildren().add(editIcon);
                        setStyle("-fx-background-color: #2E3136; -fx-background-radius: 4;");
                    } else {
                        setStyle("-fx-background-color: transparent;");
                    }
                    setGraphic(row);
                }
            }
        });

        matchingRulesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadRuleIntoEditor(newVal);
        });

        addRuleBtn.setOnAction(e -> {
            JavaCodeStyleSettings.ArrangementRule newRule = new JavaCodeStyleSettings.ArrangementRule(new ArrayList<>(List.of("field")));
            matchingRulesList.getItems().add(newRule);
            matchingRulesList.getSelectionModel().select(newRule);
            onFieldChanged();
        });

        addSectionRuleBtn.setOnAction(e -> {
            JavaCodeStyleSettings.ArrangementRule newRule = new JavaCodeStyleSettings.ArrangementRule(new ArrayList<>(List.of("class")));
            matchingRulesList.getItems().add(newRule);
            matchingRulesList.getSelectionModel().select(newRule);
            onFieldChanged();
        });

        removeRuleBtn.setOnAction(e -> {
            int idx = matchingRulesList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < matchingRulesList.getItems().size()) {
                matchingRulesList.getItems().remove(idx);
                onFieldChanged();
            }
        });

        moveUpRuleBtn.setOnAction(e -> {
            int idx = matchingRulesList.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                var item = matchingRulesList.getItems().remove(idx);
                matchingRulesList.getItems().add(idx - 1, item);
                matchingRulesList.getSelectionModel().select(idx - 1);
                onFieldChanged();
            }
        });

        moveDownRuleBtn.setOnAction(e -> {
            int idx = matchingRulesList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < matchingRulesList.getItems().size() - 1) {
                var item = matchingRulesList.getItems().remove(idx);
                matchingRulesList.getItems().add(idx + 1, item);
                matchingRulesList.getSelectionModel().select(idx + 1);
                onFieldChanged();
            }
        });
    }

    private void setupRuleEditorCard() {
        ruleEditorCard.setStyle("-fx-background-color: #26282B; -fx-border-color: #393B40; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;");

        ruleEditorHeader.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-alignment: center;");
        HBox headerBox = new HBox(ruleEditorHeader);
        headerBox.setAlignment(Pos.CENTER);

        // Types
        String[] types = {"field", "initializer block", "constructor", "method", "class", "interface", "enum", "getter", "setter", "overridden"};
        typeToggleButtons.clear();
        ruleTypeFlow.getChildren().clear();
        for (String t : types) {
            ToggleButton tb = new ToggleButton(t);
            stylePillButton(tb);
            tb.setOnAction(e -> onEditorTagToggled(t, tb.isSelected()));
            typeToggleButtons.add(tb);
            ruleTypeFlow.getChildren().add(tb);
        }

        HBox typeRow = new HBox(8);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        Label typeLbl = new Label("Type:");
        typeLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        typeLbl.setPrefWidth(60);
        typeRow.getChildren().addAll(typeLbl, ruleTypeFlow);

        // Modifiers
        String[] modifiers = {"public", "protected", "package private", "private", "static", "final", "abstract", "synchronized", "transient", "volatile"};
        modifierToggleButtons.clear();
        ruleModifierFlow.getChildren().clear();
        for (String m : modifiers) {
            ToggleButton mb = new ToggleButton(m);
            stylePillButton(mb);
            mb.setOnAction(e -> onEditorTagToggled(m, mb.isSelected()));
            modifierToggleButtons.add(mb);
            ruleModifierFlow.getChildren().add(mb);
        }

        HBox modRow = new HBox(8);
        modRow.setAlignment(Pos.CENTER_LEFT);
        Label modLbl = new Label("Modifier:");
        modLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        modLbl.setPrefWidth(60);
        modRow.getChildren().addAll(modLbl, ruleModifierFlow);

        // Name
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label("Name:");
        nameLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        nameLbl.setPrefWidth(60);
        styleTextField(ruleNameField, 160);
        ruleNameField.textProperty().addListener((obs, o, n) -> {
            var selected = matchingRulesList.getSelectionModel().getSelectedItem();
            if (selected != null && !suppressEvents) {
                selected.setNameFilter(n);
                onFieldChanged();
            }
        });
        nameRow.getChildren().addAll(nameLbl, ruleNameField);

        // Order
        HBox orderRow = new HBox(8);
        orderRow.setAlignment(Pos.CENTER_LEFT);
        Label orderLbl = new Label("Order:");
        orderLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        orderLbl.setPrefWidth(60);
        ruleOrderCombo.getItems().setAll("keep order", "order by name");
        ruleOrderCombo.setValue("keep order");
        styleComboBox(ruleOrderCombo);
        ruleOrderCombo.setPrefWidth(120);
        ruleOrderCombo.valueProperty().addListener((obs, o, n) -> {
            var selected = matchingRulesList.getSelectionModel().getSelectedItem();
            if (selected != null && !suppressEvents && n != null) {
                selected.setOrder(n);
                onFieldChanged();
            }
        });
        orderRow.getChildren().addAll(orderLbl, ruleOrderCombo);

        // Aliases
        HBox aliasRow = new HBox(8);
        aliasRow.setAlignment(Pos.CENTER_LEFT);
        Label aliasLbl = new Label("Aliases:");
        aliasLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        aliasLbl.setPrefWidth(60);
        stylePillButton(ruleAliasBtn);
        ruleAliasBtn.setOnAction(e -> {
            var selected = matchingRulesList.getSelectionModel().getSelectedItem();
            if (selected != null && !suppressEvents) {
                selected.setAlias(ruleAliasBtn.isSelected() ? "by visibility" : "");
                onFieldChanged();
            }
        });
        aliasRow.getChildren().addAll(aliasLbl, ruleAliasBtn);

        ruleEditorCard.getChildren().addAll(headerBox, typeRow, modRow, nameRow, orderRow, aliasRow);
    }

    private void loadRuleIntoEditor(JavaCodeStyleSettings.ArrangementRule rule) {
        if (rule == null) {
            ruleEditorHeader.setText("<empty rule>");
            typeToggleButtons.forEach(b -> b.setSelected(false));
            modifierToggleButtons.forEach(b -> b.setSelected(false));
            ruleNameField.setText("");
            ruleOrderCombo.setValue("keep order");
            ruleAliasBtn.setSelected(false);
            return;
        }

        List<String> tags = rule.getTags();
        ruleEditorHeader.setText(tags.isEmpty() ? "<empty rule>" : String.join(" ", tags));

        for (ToggleButton tb : typeToggleButtons) {
            tb.setSelected(tags.contains(tb.getText()));
        }
        for (ToggleButton mb : modifierToggleButtons) {
            mb.setSelected(tags.contains(mb.getText()));
        }
        ruleNameField.setText(rule.getNameFilter());
        ruleOrderCombo.setValue(rule.getOrder());
        ruleAliasBtn.setSelected("by visibility".equals(rule.getAlias()));
    }

    private void onEditorTagToggled(String tag, boolean selected) {
        var rule = matchingRulesList.getSelectionModel().getSelectedItem();
        if (rule == null) return;

        if (selected) {
            rule.addTag(tag);
        } else {
            rule.removeTag(tag);
        }
        ruleEditorHeader.setText(rule.getTags().isEmpty() ? "<empty rule>" : String.join(" ", rule.getTags()));
        matchingRulesList.refresh();
        onFieldChanged();
    }

    private void buildCodeGenPanel() {
        codeGenBox.getChildren().clear();
        codeGenBox.setStyle("-fx-padding: 4 10 20 4;");

        HBox twoCols = new HBox(28);
        twoCols.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = new VBox(10);
        leftCol.setPrefWidth(420);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox rightCol = new VBox(10);
        rightCol.setPrefWidth(420);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // --- Left Col: Naming ---
        leftCol.getChildren().add(createGroupHeaderWithLine("Naming"));
        styleCheckbox(preferLongerNamesCheck);
        leftCol.getChildren().add(preferLongerNamesCheck);

        GridPane namingGrid = createPrefixSuffixGrid("Name prefix:", "Name suffix:");
        addPrefixSuffixRow(namingGrid, 1, "Field:", fieldPrefixField, fieldSuffixField, 110);
        addPrefixSuffixRow(namingGrid, 2, "Static field:", staticFieldPrefixField, staticFieldSuffixField, 110);
        addPrefixSuffixRow(namingGrid, 3, "Parameter:", paramPrefixField, paramSuffixField, 110);
        addPrefixSuffixRow(namingGrid, 4, "Local variable:", localPrefixField, localSuffixField, 110);
        addPrefixSuffixRow(namingGrid, 5, "Subclass:", subclassPrefixField, subclassSuffixField, 110);
        addPrefixSuffixRow(namingGrid, 6, "Test class:", testClassPrefixField, testClassSuffixField, 110);
        leftCol.getChildren().add(namingGrid);

        // --- Left Col: Variable declaration ---
        leftCol.getChildren().add(createGroupHeaderWithLine("Variable declaration"));
        styleCheckbox(makeLocalsFinalCheck);
        styleCheckbox(makeParamsFinalCheck);
        styleCheckbox(useVarForLocalVarsCheck);
        leftCol.getChildren().addAll(makeLocalsFinalCheck, makeParamsFinalCheck, useVarForLocalVarsCheck);

        // --- Left Col: Override Method Signature ---
        leftCol.getChildren().add(createGroupHeaderWithLine("Override Method Signature"));
        styleCheckbox(insertOverrideCheck);
        styleCheckbox(repeatSynchronizedCheck);
        leftCol.getChildren().addAll(insertOverrideCheck, repeatSynchronizedCheck);

        Label copyLbl = new Label("Annotations to Copy");
        copyLbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 12px;");

        HBox annotToolbar = new HBox(4);
        annotToolbar.setAlignment(Pos.CENTER_LEFT);
        annotToolbar.getChildren().addAll(addAnnotationBtn, removeAnnotationBtn);

        annotationsToCopyList.setPlaceholder(new Label("Nothing to show"));
        annotationsToCopyList.setPrefHeight(90);
        annotationsToCopyList.setMaxHeight(90);
        annotationsToCopyList.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;"
        );

        addAnnotationBtn.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog("");
            dlg.setTitle("Add Annotation");
            dlg.setHeaderText("Specify annotation to copy:");
            dlg.setContentText("Annotation:");
            dlg.showAndWait().ifPresent(annot -> {
                if (!annot.isBlank()) {
                    annotationsToCopyList.getItems().add(annot.trim());
                    onFieldChanged();
                }
            });
        });

        removeAnnotationBtn.setOnAction(e -> {
            int idx = annotationsToCopyList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < annotationsToCopyList.getItems().size()) {
                annotationsToCopyList.getItems().remove(idx);
                onFieldChanged();
            }
        });

        styleCheckbox(useExternalAnnotationsCheck);
        styleCheckbox(insertTypeUseAnnotationsBeforeTypeCheck);
        insertTypeUseAnnotationsBeforeTypeCheck.setWrapText(true);

        leftCol.getChildren().addAll(
                copyLbl,
                annotToolbar,
                annotationsToCopyList,
                useExternalAnnotationsCheck,
                insertTypeUseAnnotationsBeforeTypeCheck
        );

        // --- Right Col: Default Visibility ---
        rightCol.getChildren().add(createGroupHeaderWithLine("Default Visibility"));
        visibilityEscalateRadio.setToggleGroup(defaultVisibilityGroup);
        visibilityPrivateRadio.setToggleGroup(defaultVisibilityGroup);
        visibilityPackageLocalRadio.setToggleGroup(defaultVisibilityGroup);
        visibilityProtectedRadio.setToggleGroup(defaultVisibilityGroup);
        visibilityPublicRadio.setToggleGroup(defaultVisibilityGroup);
        visibilityPublicRadio.setSelected(true);

        styleRadioButton(visibilityEscalateRadio);
        styleRadioButton(visibilityPrivateRadio);
        styleRadioButton(visibilityPackageLocalRadio);
        styleRadioButton(visibilityProtectedRadio);
        styleRadioButton(visibilityPublicRadio);

        VBox visibilityBox = new VBox(4);
        visibilityBox.getChildren().addAll(
                visibilityEscalateRadio,
                visibilityPrivateRadio,
                visibilityPackageLocalRadio,
                visibilityProtectedRadio,
                visibilityPublicRadio
        );
        rightCol.getChildren().add(visibilityBox);

        // --- Right Col: Comment Code ---
        rightCol.getChildren().add(createGroupHeaderWithLine("Comment Code"));
        styleCheckbox(lineCommentAtFirstColCheck);
        styleCheckbox(addSpaceAtLineCommentStartCheck);
        styleCheckbox(enforceOnReformatCheck);
        enforceOnReformatCheck.setPadding(new Insets(0, 0, 0, 20));
        enforceOnReformatCheck.disableProperty().bind(addSpaceAtLineCommentStartCheck.selectedProperty().not());
        styleCheckbox(blockCommentAtFirstColCheck);
        styleCheckbox(addSpacesAroundBlockCommentsCheck);

        rightCol.getChildren().addAll(
                lineCommentAtFirstColCheck,
                addSpaceAtLineCommentStartCheck,
                enforceOnReformatCheck,
                blockCommentAtFirstColCheck,
                addSpacesAroundBlockCommentsCheck
        );

        // --- Right Col: Lambda Body ---
        rightCol.getChildren().add(createGroupHeaderWithLine("Lambda Body"));
        styleCheckbox(useClassIsInstanceCheck);
        styleCheckbox(replaceNullCheckCheck);
        styleCheckbox(useIntegerSumCheck);

        rightCol.getChildren().addAll(
                useClassIsInstanceCheck,
                replaceNullCheckCheck,
                useIntegerSumCheck
        );

        twoCols.getChildren().addAll(leftCol, rightCol);
        codeGenBox.getChildren().add(twoCols);
    }

    private void buildJavaEePanel() {
        javaEeBox.getChildren().clear();
        javaEeBox.setStyle("-fx-padding: 4 10 20 4;");

        HBox twoCols = new HBox(28);
        twoCols.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = new VBox(10);
        leftCol.setPrefWidth(420);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox rightCol = new VBox(10);
        rightCol.setPrefWidth(420);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // --- Left Col: Entity Bean ---
        leftCol.getChildren().add(createGroupHeaderWithLine("Entity Bean"));
        GridPane entityGrid = createPrefixSuffixGrid("Prefix:", "Suffix:");
        addPrefixSuffixRow(entityGrid, 1, "EJB Class:", entityEjbClassPrefixField, entityEjbClassSuffixField, 140);
        addPrefixSuffixRow(entityGrid, 2, "Home Interface:", entityHomeInterfacePrefixField, entityHomeInterfaceSuffixField, 140);
        addPrefixSuffixRow(entityGrid, 3, "Remote Interface:", entityRemoteInterfacePrefixField, entityRemoteInterfaceSuffixField, 140);
        addPrefixSuffixRow(entityGrid, 4, "Local Home Interface:", entityLocalHomeInterfacePrefixField, entityLocalHomeInterfaceSuffixField, 140);
        addPrefixSuffixRow(entityGrid, 5, "Local Interface:", entityLocalInterfacePrefixField, entityLocalInterfaceSuffixField, 140);
        addPrefixSuffixRow(entityGrid, 6, "<ejb-name> tag:", entityEjbNameTagPrefixField, entityEjbNameTagSuffixField, 140);
        addPrefixSuffixRow(entityGrid, 7, "Transfer Object:", entityTransferObjectPrefixField, entityTransferObjectSuffixField, 140);

        HBox pkRow = new HBox(8);
        pkRow.setAlignment(Pos.CENTER_LEFT);
        Label pkLbl = new Label("Default PK Class:");
        pkLbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        pkLbl.setPrefWidth(140);
        pkLbl.setAlignment(Pos.CENTER_RIGHT);
        styleTextField(entityDefaultPkClassField, 238);
        pkRow.getChildren().addAll(pkLbl, entityDefaultPkClassField);

        leftCol.getChildren().addAll(entityGrid, pkRow);

        // --- Left Col: Servlet ---
        leftCol.getChildren().add(createGroupHeaderWithLine("Servlet"));
        GridPane servletGrid = createPrefixSuffixGrid("Prefix:", "Suffix:");
        addPrefixSuffixRow(servletGrid, 1, "Servlet Class:", servletClassPrefixField, servletClassSuffixField, 140);
        addPrefixSuffixRow(servletGrid, 2, "<servlet-name> tag:", servletNameTagPrefixField, servletNameTagSuffixField, 140);
        leftCol.getChildren().add(servletGrid);

        // --- Left Col: Filter ---
        leftCol.getChildren().add(createGroupHeaderWithLine("Filter"));
        GridPane filterGrid = createPrefixSuffixGrid("Prefix:", "Suffix:");
        addPrefixSuffixRow(filterGrid, 1, "Filter Class:", filterClassPrefixField, filterClassSuffixField, 140);
        addPrefixSuffixRow(filterGrid, 2, "<filter-name> tag:", filterNameTagPrefixField, filterNameTagSuffixField, 140);
        leftCol.getChildren().add(filterGrid);

        // --- Right Col: Session Bean ---
        rightCol.getChildren().add(createGroupHeaderWithLine("Session Bean"));
        GridPane sessionGrid = createPrefixSuffixGrid("Prefix:", "Suffix:");
        addPrefixSuffixRow(sessionGrid, 1, "EJB Class:", sessionEjbClassPrefixField, sessionEjbClassSuffixField, 160);
        addPrefixSuffixRow(sessionGrid, 2, "Home Interface:", sessionHomeInterfacePrefixField, sessionHomeInterfaceSuffixField, 160);
        addPrefixSuffixRow(sessionGrid, 3, "Remote Interface:", sessionRemoteInterfacePrefixField, sessionRemoteInterfaceSuffixField, 160);
        addPrefixSuffixRow(sessionGrid, 4, "Local Home Interface:", sessionLocalHomeInterfacePrefixField, sessionLocalHomeInterfaceSuffixField, 160);
        addPrefixSuffixRow(sessionGrid, 5, "Local Interface:", sessionLocalInterfacePrefixField, sessionLocalInterfaceSuffixField, 160);
        addPrefixSuffixRow(sessionGrid, 6, "Service Endpoint Interface:", sessionServiceEndpointPrefixField, sessionServiceEndpointSuffixField, 160);
        addPrefixSuffixRow(sessionGrid, 7, "<ejb-name> tag:", sessionEjbNameTagPrefixField, sessionEjbNameTagSuffixField, 160);
        rightCol.getChildren().add(sessionGrid);

        // --- Right Col: Message Driven Bean ---
        rightCol.getChildren().add(createGroupHeaderWithLine("Message Driven Bean"));
        GridPane mdbGrid = createPrefixSuffixGrid("Prefix:", "Suffix:");
        addPrefixSuffixRow(mdbGrid, 1, "EJB Class:", mdbEjbClassPrefixField, mdbEjbClassSuffixField, 160);
        addPrefixSuffixRow(mdbGrid, 2, "<ejb-name> tag:", mdbEjbNameTagPrefixField, mdbEjbNameTagSuffixField, 160);
        rightCol.getChildren().add(mdbGrid);

        // --- Right Col: Listener ---
        rightCol.getChildren().add(createGroupHeaderWithLine("Listener"));
        GridPane listenerGrid = createPrefixSuffixGrid("Prefix:", "Suffix:");
        addPrefixSuffixRow(listenerGrid, 1, "Listener Class:", listenerClassPrefixField, listenerClassSuffixField, 160);
        rightCol.getChildren().add(listenerGrid);

        twoCols.getChildren().addAll(leftCol, rightCol);
        javaEeBox.getChildren().add(twoCols);
    }

    private HBox createLabeledField(String labelText, TextField textField) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        lbl.setPrefWidth(260);

        styleTextField(textField, 60);

        row.getChildren().addAll(lbl, textField);
        return row;
    }

    private HBox createPrefixSuffixRow(String labelText, TextField prefixField, TextField suffixField) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
        lbl.setPrefWidth(120);

        Label pLbl = new Label("Prefix:");
        pLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        styleTextField(prefixField, 60);

        Label sLbl = new Label("Suffix:");
        sLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        styleTextField(suffixField, 60);

        row.getChildren().addAll(lbl, pLbl, prefixField, sLbl, suffixField);
        return row;
    }

    private Label createSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 4 0;");
        return lbl;
    }

    private void styleCheckbox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #BCBEC4; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 6; -fx-font-size: 13px;");
    }

    private <T> void styleComboBox(ComboBox<T> combo) {
        combo.setPrefWidth(140);
        combo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #BCBEC4; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
    }

    private void applyTabButtonStyle(ToggleButton btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; " +
                    "-fx-border-color: #3574F0; -fx-border-width: 0 0 2 0; -fx-padding: 4 10; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; " +
                    "-fx-border-color: transparent; -fx-padding: 4 10; -fx-cursor: hand;");
        }
    }

    private void setupListeners() {
        // Tabs & Indents
        useTabCharCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        smartTabsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        tabSizeField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        indentField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        continuationIndentField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        keepIndentsEmptyLinesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        labelIndentField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        absoluteLabelIndentCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        doNotIndentTopLevelMembersCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        useIndentsRelativeToExprCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Spaces
        // Before parentheses
        spaceBeforeMethodDeclParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeMethodCallParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeIfParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeForParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeWhileParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeSwitchParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeTryParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeCatchParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeSynchronizedParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeAnnotationParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeDeconstructionListCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Around operators
        spaceAroundAssignmentOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundLogicalOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundEqualityOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundRelationalOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundBitwiseOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundAdditiveOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundMultiplicativeOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundShiftOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundUnaryOpsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundLambdaArrowCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceAroundMethodRefDoubleColonCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Before left brace
        spaceBeforeClassLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeMethodLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeIfLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeElseLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeWhileLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeForLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeDoLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeSwitchLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeTryLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeCatchLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeFinallyLeftBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        spaceBeforeElseCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeWhileCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeCatchCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        spaceBeforeFinallyCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        withinCodeBracesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinBracketsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinArrayInitBracesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinEmptyArrayInitBracesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinGroupingParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinMethodDeclParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinEmptyMethodDeclParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinMethodCallParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinEmptyMethodCallParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinIfParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinForParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinWhileParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinSwitchParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinTryParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinCatchParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinSynchronizedParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinTypeCastParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinAnnotationParensCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinAngleBracketsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinRecordHeaderCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinDeconstructionListCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        withinBlockBracesWhenBodyCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        ternaryBeforeQuestionCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        ternaryAfterQuestionCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        ternaryBeforeColonCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        ternaryAfterColonCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        typeArgsAfterCommaCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        typeArgsBeforeOpenAngleCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        typeArgsAfterCloseAngleCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        otherBeforeCommaCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        otherAfterCommaCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        otherBeforeForSemicolonCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        otherAfterForSemicolonCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        otherAfterTypeCastCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        otherAroundEqualsInAnnotationCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        otherBeforeColonInForEachCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        otherInsideOneLineEnumBracesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        typeParamsBeforeOpenAngleCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        typeParamsAroundTypeBoundsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Wrapping
        keepLineBreaksCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        commentAtFirstColumnCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        controlStatementInOneLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        multipleExpressionsInOneLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepSimpleBlocksCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepSimpleMethodsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepSimpleLambdasCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepSimpleClassesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        ensureRightMarginCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        classBraceCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        methodBraceCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        lambdaBraceCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        otherBraceCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());

        extendsImplementsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignExtendsImplementsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        extendsImplementsKeywordWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());

        throwsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignThrowsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignThrowsToMethodStartCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        throwsKeywordWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());

        methodDeclParamsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignMethodDeclParamsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterMethodDeclLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeMethodDeclRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        methodCallArgsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignMethodCallArgsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        takePriorityOverCallChainWrapCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterMethodCallLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeMethodCallRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        alignMethodParenthesesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineWhenMethodBodyPresentedCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        chainedMethodCallsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        wrapFirstCallCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignChainedCallsMultilineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        builderMethodsCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        keepBuilderMethodsIndentsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        moveSemicolonToNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        ifForceBracesCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        elseOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        specialElseIfTreatmentCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        forStatementWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignForMultilineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterForLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeForRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        forForceBracesCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());

        whileForceBracesCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());

        doWhileForceBracesCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        whileOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        switchWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        indentCaseBranchesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        eachCaseOnSeparateLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        tryWithResourcesWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignTryWithResourcesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterTryWithResourcesLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeTryWithResourcesRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        catchOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        finallyOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        typesInMultiCatchWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignTypesInMultiCatchCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        binaryExpressionsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignBinaryExpressionsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        operationSignOnNextLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignParenthesisedCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterBinaryLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeBinaryRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        assignmentWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignAssignmentCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        assignmentSignOnNextLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        alignFieldsInColumnsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignVariablesInColumnsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignAssignmentsInColumnsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignSimpleMethodsInColumnsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        ternaryWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignTernaryCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        ternarySignsOnNextLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        arrayInitializerWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignArrayInitializerCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterArrayInitializerLBraceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeArrayInitializerRBraceOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        wrapAfterModifierListCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        assertStatementWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        enumConstantsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        classAnnotationsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        methodAnnotationsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        fieldAnnotationsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        doNotWrapAfterSingleFieldAnnotationCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        parameterAnnotationsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        doNotWrapAfterSingleParameterAnnotationCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        localVariableAnnotationsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        enumFieldAnnotationsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        annotationParametersWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignAnnotationParametersCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterAnnotationParametersLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeAnnotationParametersRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        alignTextBlocksCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        recordComponentsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignRecordComponentsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterRecordComponentsLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeRecordComponentsRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineForRecordComponentAnnotationsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        deconstructionPatternsWrapCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        alignDeconstructionPatternsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        newLineAfterDeconstructionPatternsLParenCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        placeDeconstructionPatternsRParenOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Blank lines
        keepBlankLinesInDeclField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        keepBlankLinesInCodeField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        keepBlankLinesBeforeRBraceField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        keepBlankLinesBetweenHeaderAndPackageField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBeforePackageField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAfterPackageField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBeforeImportsField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAfterImportsField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAroundClassField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAfterClassHeaderField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBeforeClassEndField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAfterAnonymousClassHeaderField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBeforeFieldInInterfaceField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBeforeFieldWithoutAnnotationsField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBeforeFieldWithAnnotationsField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAroundMethodInInterfaceField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAroundMethodField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBeforeMethodBodyField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAroundInitializerField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesBetweenRecordComponentsField.textProperty().addListener((obs, o, n) -> onFieldChanged());

        // JavaDoc
        enableJavaDocFormattingCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignParamDescriptionsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        alignThrownExceptionsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAfterDescCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAfterParamDescCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        blankLinesAfterReturnTagCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepInvalidTagsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepEmptyParamTagsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepEmptyReturnTagsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepEmptyThrowsTagsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        wrapAtRightMarginCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        enableLeadingAsterisksCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        useThrowsRatherThanExceptionCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        generatePOnEmptyLinesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepEmptyLinesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        doNotWrapOneLineCommentsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        preserveLineFeedsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        paramDescriptionsOnNewLineCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        indentContinuationLinesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Imports
        useSingleClassImportCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        useFullyQualifiedClassNamesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        insertInnerClassImportsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        doNotSeparateModuleImportsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        deleteUnusedModuleImportsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        useFqNamesInJavadocCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        classCountImportField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        namesCountStaticImportField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        placeOnDemandImportBeforeSingleClassCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        layoutStaticImportsSeparatelyCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Arrangement
        keepGettersSettersTogetherCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        keepOverriddenMethodsTogetherCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        overriddenMethodsOrderCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());
        keepDependentMethodsTogetherCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        dependentMethodsOrderCombo.valueProperty().addListener((obs, o, n) -> onFieldChanged());

        // Code Gen
        preferLongerNamesCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        fieldPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        fieldSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        staticFieldPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        staticFieldSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        paramPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        paramSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        localPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        localSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        subclassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        subclassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        testClassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        testClassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());

        defaultVisibilityGroup.selectedToggleProperty().addListener((obs, o, n) -> onFieldChanged());

        makeLocalsFinalCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        makeParamsFinalCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        useVarForLocalVarsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        lineCommentAtFirstColCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        addSpaceAtLineCommentStartCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        enforceOnReformatCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        blockCommentAtFirstColCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        addSpacesAroundBlockCommentsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        insertOverrideCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        repeatSynchronizedCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        useExternalAnnotationsCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        insertTypeUseAnnotationsBeforeTypeCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        useClassIsInstanceCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        replaceNullCheckCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());
        useIntegerSumCheck.selectedProperty().addListener((obs, o, n) -> onFieldChanged());

        // Java EE
        entityEjbClassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityEjbClassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityHomeInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityHomeInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityRemoteInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityRemoteInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityLocalHomeInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityLocalHomeInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityLocalInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityLocalInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityEjbNameTagPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityEjbNameTagSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityTransferObjectPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityTransferObjectSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        entityDefaultPkClassField.textProperty().addListener((obs, o, n) -> onFieldChanged());

        sessionEjbClassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionEjbClassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionHomeInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionHomeInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionRemoteInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionRemoteInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionLocalHomeInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionLocalHomeInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionLocalInterfacePrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionLocalInterfaceSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionServiceEndpointPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionServiceEndpointSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionEjbNameTagPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        sessionEjbNameTagSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());

        servletClassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        servletClassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        servletNameTagPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        servletNameTagSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());

        mdbEjbClassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        mdbEjbClassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        mdbEjbNameTagPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        mdbEjbNameTagSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());

        filterClassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        filterClassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        filterNameTagPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        filterNameTagSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());

        listenerClassPrefixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
        listenerClassSuffixField.textProperty().addListener((obs, o, n) -> onFieldChanged());
    }

    private void onFieldChanged() {
        if (!suppressEvents) {
            updateSettingsFromControls();
            updatePreview();
            notifyModified();
        }
    }

    private void showSetFromMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157;");

        MenuItem googleItem = new MenuItem("Google Style Guide");
        googleItem.setOnAction(e -> {
            currentSettings.applyGoogleStyle();
            loadControlsFromSettings(currentSettings);
            updatePreview();
            notifyModified();
        });

        MenuItem defaultItem = new MenuItem("Platform Default");
        defaultItem.setOnAction(e -> {
            currentSettings.applyPlatformDefault();
            loadControlsFromSettings(currentSettings);
            updatePreview();
            notifyModified();
        });

        menu.getItems().addAll(googleItem, defaultItem);
        menu.show(setFromLink, Side.BOTTOM, 0, 0);
    }

    public void loadFromCurrentScheme() {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;

        LanguageCodeStyleSettings lcs = scheme.getLanguageSettings("Java");
        currentSettings.syncFrom(lcs);
        originalSettings = currentSettings.copy();

        loadControlsFromSettings(currentSettings);
    }

    private void loadControlsFromSettings(JavaCodeStyleSettings s) {
        suppressEvents = true;
        try {
            // Tabs and Indents
            useTabCharCheck.setSelected(s.isUseTabCharacter());
            smartTabsCheck.setSelected(s.isSmartTabs());
            tabSizeField.setText(String.valueOf(s.getTabSize()));
            indentField.setText(String.valueOf(s.getIndent()));
            continuationIndentField.setText(String.valueOf(s.getContinuationIndent()));
            keepIndentsEmptyLinesCheck.setSelected(s.isKeepIndentsOnEmptyLines());
            labelIndentField.setText(String.valueOf(s.getLabelIndent()));
            absoluteLabelIndentCheck.setSelected(s.isAbsoluteLabelIndent());
            doNotIndentTopLevelMembersCheck.setSelected(s.isDoNotIndentTopLevelMembers());
            useIndentsRelativeToExprCheck.setSelected(s.isUseIndentsRelativeToExpressionStart());

            // Spaces
            // Before parentheses
            spaceBeforeMethodDeclParensCheck.setSelected(s.isSpaceBeforeMethodDeclParen());
            spaceBeforeMethodCallParensCheck.setSelected(s.isSpaceBeforeMethodCallParen());
            spaceBeforeIfParensCheck.setSelected(s.isSpaceBeforeIfParen());
            spaceBeforeForParensCheck.setSelected(s.isSpaceBeforeForParen());
            spaceBeforeWhileParensCheck.setSelected(s.isSpaceBeforeWhileParen());
            spaceBeforeSwitchParensCheck.setSelected(s.isSpaceBeforeSwitchParen());
            spaceBeforeTryParensCheck.setSelected(s.isSpaceBeforeTryParen());
            spaceBeforeCatchParensCheck.setSelected(s.isSpaceBeforeCatchParen());
            spaceBeforeSynchronizedParensCheck.setSelected(s.isSpaceBeforeSynchronizedParen());
            spaceBeforeAnnotationParensCheck.setSelected(s.isSpaceBeforeAnnotationParens());
            spaceBeforeDeconstructionListCheck.setSelected(s.isSpaceBeforeDeconstructionList());

            // Around operators
            spaceAroundAssignmentOpsCheck.setSelected(s.isSpaceAroundAssignmentOps());
            spaceAroundLogicalOpsCheck.setSelected(s.isSpaceAroundLogicalOps());
            spaceAroundEqualityOpsCheck.setSelected(s.isSpaceAroundEqualityOps());
            spaceAroundRelationalOpsCheck.setSelected(s.isSpaceAroundRelationalOps());
            spaceAroundBitwiseOpsCheck.setSelected(s.isSpaceAroundBitwiseOps());
            spaceAroundAdditiveOpsCheck.setSelected(s.isSpaceAroundAdditiveOps());
            spaceAroundMultiplicativeOpsCheck.setSelected(s.isSpaceAroundMultiplicativeOps());
            spaceAroundShiftOpsCheck.setSelected(s.isSpaceAroundShiftOps());
            spaceAroundUnaryOpsCheck.setSelected(s.isSpaceAroundUnaryOps());
            spaceAroundLambdaArrowCheck.setSelected(s.isSpaceAroundLambdaArrow());
            spaceAroundMethodRefDoubleColonCheck.setSelected(s.isSpaceAroundMethodRefDoubleColon());

            // Before left brace
            spaceBeforeClassLeftBraceCheck.setSelected(s.isSpaceBeforeClassLeftBrace());
            spaceBeforeMethodLeftBraceCheck.setSelected(s.isSpaceBeforeMethodLeftBrace());
            spaceBeforeIfLeftBraceCheck.setSelected(s.isSpaceBeforeIfLeftBrace());
            spaceBeforeElseLeftBraceCheck.setSelected(s.isSpaceBeforeElseLeftBrace());
            spaceBeforeWhileLeftBraceCheck.setSelected(s.isSpaceBeforeWhileLeftBrace());
            spaceBeforeForLeftBraceCheck.setSelected(s.isSpaceBeforeForLeftBrace());
            spaceBeforeDoLeftBraceCheck.setSelected(s.isSpaceBeforeDoLeftBrace());
            spaceBeforeSwitchLeftBraceCheck.setSelected(s.isSpaceBeforeSwitchLeftBrace());
            spaceBeforeTryLeftBraceCheck.setSelected(s.isSpaceBeforeTryLeftBrace());
            spaceBeforeCatchLeftBraceCheck.setSelected(s.isSpaceBeforeCatchLeftBrace());
            spaceBeforeFinallyLeftBraceCheck.setSelected(s.isSpaceBeforeFinallyLeftBrace());

            spaceBeforeElseCheck.setSelected(s.isSpaceBeforeElse());
            spaceBeforeWhileCheck.setSelected(s.isSpaceBeforeWhile());
            spaceBeforeCatchCheck.setSelected(s.isSpaceBeforeCatch());
            spaceBeforeFinallyCheck.setSelected(s.isSpaceBeforeFinally());

            withinCodeBracesCheck.setSelected(s.isWithinCodeBraces());
            withinBracketsCheck.setSelected(s.isWithinBrackets());
            withinArrayInitBracesCheck.setSelected(s.isWithinArrayInitializerBraces());
            withinEmptyArrayInitBracesCheck.setSelected(s.isWithinEmptyArrayInitializerBraces());
            withinGroupingParensCheck.setSelected(s.isWithinGroupingParens());
            withinMethodDeclParensCheck.setSelected(s.isWithinMethodDeclParens());
            withinEmptyMethodDeclParensCheck.setSelected(s.isWithinEmptyMethodDeclParens());
            withinMethodCallParensCheck.setSelected(s.isWithinMethodCallParens());
            withinEmptyMethodCallParensCheck.setSelected(s.isWithinEmptyMethodCallParens());
            withinIfParensCheck.setSelected(s.isWithinIfParens());
            withinForParensCheck.setSelected(s.isWithinForParens());
            withinWhileParensCheck.setSelected(s.isWithinWhileParens());
            withinSwitchParensCheck.setSelected(s.isWithinSwitchParens());
            withinTryParensCheck.setSelected(s.isWithinTryParens());
            withinCatchParensCheck.setSelected(s.isWithinCatchParens());
            withinSynchronizedParensCheck.setSelected(s.isWithinSynchronizedParens());
            withinTypeCastParensCheck.setSelected(s.isWithinTypeCastParens());
            withinAnnotationParensCheck.setSelected(s.isWithinAnnotationParens());
            withinAngleBracketsCheck.setSelected(s.isWithinAngleBrackets());
            withinRecordHeaderCheck.setSelected(s.isWithinRecordHeader());
            withinDeconstructionListCheck.setSelected(s.isWithinDeconstructionList());
            withinBlockBracesWhenBodyCheck.setSelected(s.isWithinBlockBracesWhenBodyPresent());

            ternaryBeforeQuestionCheck.setSelected(s.isTernaryBeforeQuestion());
            ternaryAfterQuestionCheck.setSelected(s.isTernaryAfterQuestion());
            ternaryBeforeColonCheck.setSelected(s.isTernaryBeforeColon());
            ternaryAfterColonCheck.setSelected(s.isTernaryAfterColon());

            typeArgsAfterCommaCheck.setSelected(s.isTypeArgsAfterComma());
            typeArgsBeforeOpenAngleCheck.setSelected(s.isTypeArgsBeforeOpenAngle());
            typeArgsAfterCloseAngleCheck.setSelected(s.isTypeArgsAfterCloseAngle());

            otherBeforeCommaCheck.setSelected(s.isSpaceBeforeComma());
            otherAfterCommaCheck.setSelected(s.isSpaceAfterComma());
            otherBeforeForSemicolonCheck.setSelected(s.isSpaceBeforeForSemicolon());
            otherAfterForSemicolonCheck.setSelected(s.isSpaceAfterForSemicolon());
            otherAfterTypeCastCheck.setSelected(s.isSpaceAfterTypeCast());
            otherAroundEqualsInAnnotationCheck.setSelected(s.isSpaceAroundEqualsInAnnotation());
            otherBeforeColonInForEachCheck.setSelected(s.isSpaceBeforeColonInForEach());
            otherInsideOneLineEnumBracesCheck.setSelected(s.isSpaceInsideOneLineEnumBraces());

            typeParamsBeforeOpenAngleCheck.setSelected(s.isTypeParamsBeforeOpenAngle());
            typeParamsAroundTypeBoundsCheck.setSelected(s.isTypeParamsAroundTypeBounds());

            // Wrapping
            keepLineBreaksCheck.setSelected(s.isKeepLineBreaks());
            commentAtFirstColumnCheck.setSelected(s.isCommentAtFirstColumn());
            controlStatementInOneLineCheck.setSelected(s.isControlStatementInOneLine());
            multipleExpressionsInOneLineCheck.setSelected(s.isMultipleExpressionsInOneLine());
            keepSimpleBlocksCheck.setSelected(s.isKeepSimpleBlocksInOneLine());
            keepSimpleMethodsCheck.setSelected(s.isKeepSimpleMethodsInOneLine());
            keepSimpleLambdasCheck.setSelected(s.isKeepSimpleLambdasInOneLine());
            keepSimpleClassesCheck.setSelected(s.isKeepSimpleClassesInOneLine());
            ensureRightMarginCheck.setSelected(s.isEnsureRightMargin());

            classBraceCombo.setValue(s.getClassBracePlacement());
            methodBraceCombo.setValue(s.getMethodBracePlacement());
            lambdaBraceCombo.setValue(s.getLambdaBracePlacement());
            otherBraceCombo.setValue(s.getOtherBracePlacement());

            extendsImplementsWrapCombo.setValue(s.getExtendsImplementsWrap());
            alignExtendsImplementsCheck.setSelected(s.isAlignExtendsImplementsMultiline());
            extendsImplementsKeywordWrapCombo.setValue(s.getExtendsImplementsKeywordWrap());

            throwsWrapCombo.setValue(s.getThrowsWrap());
            alignThrowsCheck.setSelected(s.isAlignThrowsMultiline());
            alignThrowsToMethodStartCheck.setSelected(s.isAlignThrowsToMethodStart());
            throwsKeywordWrapCombo.setValue(s.getThrowsKeywordWrap());

            methodDeclParamsWrapCombo.setValue(s.getMethodDeclParametersWrap());
            alignMethodDeclParamsCheck.setSelected(s.isAlignMethodDeclParameters());
            newLineAfterMethodDeclLParenCheck.setSelected(s.isNewLineAfterMethodDeclLParen());
            placeMethodDeclRParenOnNewLineCheck.setSelected(s.isPlaceMethodDeclRParenOnNewLine());

            methodCallArgsWrapCombo.setValue(s.getMethodCallArgumentsWrap());
            alignMethodCallArgsCheck.setSelected(s.isAlignMethodCallArguments());
            takePriorityOverCallChainWrapCheck.setSelected(s.isTakePriorityOverCallChainWrapping());
            newLineAfterMethodCallLParenCheck.setSelected(s.isNewLineAfterMethodCallLParen());
            placeMethodCallRParenOnNewLineCheck.setSelected(s.isPlaceMethodCallRParenOnNewLine());

            alignMethodParenthesesCheck.setSelected(s.isAlignMethodParenthesesMultiline());
            newLineWhenMethodBodyPresentedCheck.setSelected(s.isNewLineWhenMethodBodyPresented());

            chainedMethodCallsWrapCombo.setValue(s.getChainedMethodCallsWrap());
            wrapFirstCallCheck.setSelected(s.isWrapFirstCall());
            alignChainedCallsMultilineCheck.setSelected(s.isAlignChainedCallsMultiline());
            builderMethodsCombo.setValue(s.getBuilderMethods());
            keepBuilderMethodsIndentsCheck.setSelected(s.isKeepBuilderMethodsIndents());
            moveSemicolonToNewLineCheck.setSelected(s.isMoveSemicolonToNewLine());

            ifForceBracesCombo.setValue(s.getIfForceBraces());
            elseOnNewLineCheck.setSelected(s.isElseOnNewLine());
            specialElseIfTreatmentCheck.setSelected(s.isSpecialElseIfTreatment());

            forStatementWrapCombo.setValue(s.getForStatementWrap());
            alignForMultilineCheck.setSelected(s.isAlignForMultiline());
            newLineAfterForLParenCheck.setSelected(s.isNewLineAfterForLParen());
            placeForRParenOnNewLineCheck.setSelected(s.isPlaceForRParenOnNewLine());
            forForceBracesCombo.setValue(s.getForForceBraces());

            whileForceBracesCombo.setValue(s.getWhileForceBraces());

            doWhileForceBracesCombo.setValue(s.getDoWhileForceBraces());
            whileOnNewLineCheck.setSelected(s.isWhileOnNewLine());

            switchWrapCombo.setValue(s.getSwitchWrap());
            indentCaseBranchesCheck.setSelected(s.isIndentCaseBranches());
            eachCaseOnSeparateLineCheck.setSelected(s.isEachCaseOnSeparateLine());

            tryWithResourcesWrapCombo.setValue(s.getTryWithResourcesWrap());
            alignTryWithResourcesCheck.setSelected(s.isAlignTryWithResourcesMultiline());
            newLineAfterTryWithResourcesLParenCheck.setSelected(s.isNewLineAfterTryWithResourcesLParen());
            placeTryWithResourcesRParenOnNewLineCheck.setSelected(s.isPlaceTryWithResourcesRParenOnNewLine());

            catchOnNewLineCheck.setSelected(s.isCatchOnNewLine());
            finallyOnNewLineCheck.setSelected(s.isFinallyOnNewLine());
            typesInMultiCatchWrapCombo.setValue(s.getTypesInMultiCatchWrap());
            alignTypesInMultiCatchCheck.setSelected(s.isAlignTypesInMultiCatch());

            binaryExpressionsWrapCombo.setValue(s.getBinaryExpressionsWrap());
            alignBinaryExpressionsCheck.setSelected(s.isAlignBinaryExpressionsMultiline());
            operationSignOnNextLineCheck.setSelected(s.isOperationSignOnNextLine());
            alignParenthesisedCheck.setSelected(s.isAlignParenthesisedMultiline());
            newLineAfterBinaryLParenCheck.setSelected(s.isNewLineAfterBinaryLParen());
            placeBinaryRParenOnNewLineCheck.setSelected(s.isPlaceBinaryRParenOnNewLine());

            assignmentWrapCombo.setValue(s.getAssignmentWrap());
            alignAssignmentCheck.setSelected(s.isAlignAssignmentMultiline());
            assignmentSignOnNextLineCheck.setSelected(s.isAssignmentSignOnNextLine());

            alignFieldsInColumnsCheck.setSelected(s.isAlignFieldsInColumns());
            alignVariablesInColumnsCheck.setSelected(s.isAlignVariablesInColumns());
            alignAssignmentsInColumnsCheck.setSelected(s.isAlignAssignmentsInColumns());
            alignSimpleMethodsInColumnsCheck.setSelected(s.isAlignSimpleMethodsInColumns());

            ternaryWrapCombo.setValue(s.getTernaryWrap());
            alignTernaryCheck.setSelected(s.isAlignTernaryMultiline());
            ternarySignsOnNextLineCheck.setSelected(s.isTernarySignsOnNextLine());

            arrayInitializerWrapCombo.setValue(s.getArrayInitializerWrap());
            alignArrayInitializerCheck.setSelected(s.isAlignArrayInitializerMultiline());
            newLineAfterArrayInitializerLBraceCheck.setSelected(s.isNewLineAfterArrayInitializerLBrace());
            placeArrayInitializerRBraceOnNewLineCheck.setSelected(s.isPlaceArrayInitializerRBraceOnNewLine());

            wrapAfterModifierListCheck.setSelected(s.isWrapAfterModifierList());

            assertStatementWrapCombo.setValue(s.getAssertStatementWrap());
            enumConstantsWrapCombo.setValue(s.getEnumConstantsWrap());
            classAnnotationsWrapCombo.setValue(s.getClassAnnotationsWrap());
            methodAnnotationsWrapCombo.setValue(s.getMethodAnnotationsWrap());
            fieldAnnotationsWrapCombo.setValue(s.getFieldAnnotationsWrap());
            doNotWrapAfterSingleFieldAnnotationCheck.setSelected(s.isDoNotWrapAfterSingleFieldAnnotation());
            parameterAnnotationsWrapCombo.setValue(s.getParameterAnnotationsWrap());
            doNotWrapAfterSingleParameterAnnotationCheck.setSelected(s.isDoNotWrapAfterSingleParameterAnnotation());
            localVariableAnnotationsWrapCombo.setValue(s.getLocalVariableAnnotationsWrap());
            enumFieldAnnotationsWrapCombo.setValue(s.getEnumFieldAnnotationsWrap());
            annotationParametersWrapCombo.setValue(s.getAnnotationParametersWrap());
            alignAnnotationParametersCheck.setSelected(s.isAlignAnnotationParametersMultiline());
            newLineAfterAnnotationParametersLParenCheck.setSelected(s.isNewLineAfterAnnotationParametersLParen());
            placeAnnotationParametersRParenOnNewLineCheck.setSelected(s.isPlaceAnnotationParametersRParenOnNewLine());

            alignTextBlocksCheck.setSelected(s.isAlignTextBlocksMultiline());

            recordComponentsWrapCombo.setValue(s.getRecordComponentsWrap());
            alignRecordComponentsCheck.setSelected(s.isAlignRecordComponentsMultiline());
            newLineAfterRecordComponentsLParenCheck.setSelected(s.isNewLineAfterRecordComponentsLParen());
            placeRecordComponentsRParenOnNewLineCheck.setSelected(s.isPlaceRecordComponentsRParenOnNewLine());
            newLineForRecordComponentAnnotationsCheck.setSelected(s.isNewLineForRecordComponentAnnotations());

            deconstructionPatternsWrapCombo.setValue(s.getDeconstructionPatternsWrap());
            alignDeconstructionPatternsCheck.setSelected(s.isAlignDeconstructionPatternsMultiline());
            newLineAfterDeconstructionPatternsLParenCheck.setSelected(s.isNewLineAfterDeconstructionPatternsLParen());
            placeDeconstructionPatternsRParenOnNewLineCheck.setSelected(s.isPlaceDeconstructionPatternsRParenOnNewLine());

            // Blank lines
            keepBlankLinesInDeclField.setText(String.valueOf(s.getKeepBlankLinesInDeclarations()));
            keepBlankLinesInCodeField.setText(String.valueOf(s.getKeepBlankLinesInCode()));
            keepBlankLinesBeforeRBraceField.setText(String.valueOf(s.getKeepBlankLinesBeforeRBrace()));
            keepBlankLinesBetweenHeaderAndPackageField.setText(String.valueOf(s.getKeepBlankLinesBetweenHeaderAndPackage()));
            blankLinesBeforePackageField.setText(String.valueOf(s.getBlankLinesBeforePackage()));
            blankLinesAfterPackageField.setText(String.valueOf(s.getBlankLinesAfterPackage()));
            blankLinesBeforeImportsField.setText(String.valueOf(s.getBlankLinesBeforeImports()));
            blankLinesAfterImportsField.setText(String.valueOf(s.getBlankLinesAfterImports()));
            blankLinesAroundClassField.setText(String.valueOf(s.getBlankLinesAroundClass()));
            blankLinesAfterClassHeaderField.setText(String.valueOf(s.getBlankLinesAfterClassHeader()));
            blankLinesBeforeClassEndField.setText(String.valueOf(s.getBlankLinesBeforeClassEnd()));
            blankLinesAfterAnonymousClassHeaderField.setText(String.valueOf(s.getBlankLinesAfterAnonymousClassHeader()));
            blankLinesBeforeFieldInInterfaceField.setText(String.valueOf(s.getBlankLinesBeforeFieldInInterface()));
            blankLinesBeforeFieldWithoutAnnotationsField.setText(String.valueOf(s.getBlankLinesBeforeFieldWithoutAnnotations()));
            blankLinesBeforeFieldWithAnnotationsField.setText(String.valueOf(s.getBlankLinesBeforeFieldWithAnnotations()));
            blankLinesAroundMethodInInterfaceField.setText(String.valueOf(s.getBlankLinesAroundMethodInInterface()));
            blankLinesAroundMethodField.setText(String.valueOf(s.getBlankLinesAroundMethod()));
            blankLinesBeforeMethodBodyField.setText(String.valueOf(s.getBlankLinesBeforeMethodBody()));
            blankLinesAroundInitializerField.setText(String.valueOf(s.getBlankLinesAroundInitializer()));
            blankLinesBetweenRecordComponentsField.setText(String.valueOf(s.getBlankLinesBetweenRecordComponents()));

            // JavaDoc
            enableJavaDocFormattingCheck.setSelected(s.isEnableJavaDocFormatting());
            alignParamDescriptionsCheck.setSelected(s.isAlignParamDescriptions());
            alignThrownExceptionsCheck.setSelected(s.isAlignThrownExceptions());
            blankLinesAfterDescCheck.setSelected(s.isBlankLinesAfterDescription());
            blankLinesAfterParamDescCheck.setSelected(s.isBlankLinesAfterParamDescriptions());
            blankLinesAfterReturnTagCheck.setSelected(s.isBlankLinesAfterReturnTag());
            keepInvalidTagsCheck.setSelected(s.isKeepInvalidTags());
            keepEmptyParamTagsCheck.setSelected(s.isKeepEmptyParamTags());
            keepEmptyReturnTagsCheck.setSelected(s.isKeepEmptyReturnTags());
            keepEmptyThrowsTagsCheck.setSelected(s.isKeepEmptyThrowsTags());
            wrapAtRightMarginCheck.setSelected(s.isWrapAtRightMargin());
            enableLeadingAsterisksCheck.setSelected(s.isEnableLeadingAsterisks());
            useThrowsRatherThanExceptionCheck.setSelected(s.isUseThrowsRatherThanException());
            generatePOnEmptyLinesCheck.setSelected(s.isGeneratePOnEmptyLines());
            keepEmptyLinesCheck.setSelected(s.isKeepEmptyLines());
            doNotWrapOneLineCommentsCheck.setSelected(s.isDoNotWrapOneLineComments());
            preserveLineFeedsCheck.setSelected(s.isPreserveLineFeeds());
            paramDescriptionsOnNewLineCheck.setSelected(s.isParamDescriptionsOnNewLine());
            indentContinuationLinesCheck.setSelected(s.isIndentContinuationLines());

            // Imports
            useSingleClassImportCheck.setSelected(s.isUseSingleClassImport());
            useFullyQualifiedClassNamesCheck.setSelected(s.isUseFullyQualifiedClassNames());
            insertInnerClassImportsCheck.setSelected(s.isInsertInnerClassImports());
            excludeInnerClassesList.getItems().setAll(s.getExcludedInnerClasses());
            doNotSeparateModuleImportsCheck.setSelected(s.isDoNotSeparateModuleImports());
            deleteUnusedModuleImportsCheck.setSelected(s.isDeleteUnusedModuleImports());
            useFqNamesInJavadocCombo.setValue(s.getUseFqNamesInJavadoc());
            classCountImportField.setText(String.valueOf(s.getClassCountToUseImportOnDemand()));
            namesCountStaticImportField.setText(String.valueOf(s.getNamesCountToUseStaticImportOnDemand()));
            packagesToUseImportTable.getItems().setAll(
                    s.getPackagesToUseImportOnDemand().stream().map(ImportEntry::copy).collect(Collectors.toList())
            );
            placeOnDemandImportBeforeSingleClassCheck.setSelected(s.isPlaceOnDemandImportBeforeSingleClassImports());
            layoutStaticImportsSeparatelyCheck.setSelected(s.isLayoutStaticImportsSeparately());
            importLayoutTable.getItems().setAll(
                    s.getImportLayout().stream().map(ImportLayoutEntry::copy).collect(Collectors.toList())
            );

            // Arrangement
            keepGettersSettersTogetherCheck.setSelected(s.isKeepGettersAndSettersTogether());
            keepOverriddenMethodsTogetherCheck.setSelected(s.isKeepOverriddenMethodsTogether());
            overriddenMethodsOrderCombo.setValue(s.getOverriddenMethodsOrder());
            keepDependentMethodsTogetherCheck.setSelected(s.isKeepDependentMethodsTogether());
            dependentMethodsOrderCombo.setValue(s.getDependentMethodsOrder());
            matchingRulesList.getItems().setAll(
                    s.getMatchingRules().stream().map(JavaCodeStyleSettings.ArrangementRule::copy).collect(Collectors.toList())
            );
            if (!matchingRulesList.getItems().isEmpty()) {
                matchingRulesList.getSelectionModel().select(0);
            }

            // Code Gen
            preferLongerNamesCheck.setSelected(s.isPreferLongerNames());
            fieldPrefixField.setText(s.getFieldPrefix());
            fieldSuffixField.setText(s.getFieldSuffix());
            staticFieldPrefixField.setText(s.getStaticFieldPrefix());
            staticFieldSuffixField.setText(s.getStaticFieldSuffix());
            paramPrefixField.setText(s.getParameterPrefix());
            paramSuffixField.setText(s.getParameterSuffix());
            localPrefixField.setText(s.getLocalVariablePrefix());
            localSuffixField.setText(s.getLocalVariableSuffix());
            subclassPrefixField.setText(s.getSubclassPrefix());
            subclassSuffixField.setText(s.getSubclassSuffix());
            testClassPrefixField.setText(s.getTestClassPrefix());
            testClassSuffixField.setText(s.getTestClassSuffix());

            switch (s.getDefaultVisibility()) {
                case "Escalate" -> visibilityEscalateRadio.setSelected(true);
                case "Private" -> visibilityPrivateRadio.setSelected(true);
                case "Package local" -> visibilityPackageLocalRadio.setSelected(true);
                case "Protected" -> visibilityProtectedRadio.setSelected(true);
                default -> visibilityPublicRadio.setSelected(true);
            }

            makeLocalsFinalCheck.setSelected(s.isMakeGeneratedLocalsFinal());
            makeParamsFinalCheck.setSelected(s.isMakeGeneratedParametersFinal());
            useVarForLocalVarsCheck.setSelected(s.isUseVarForLocalVariables());

            lineCommentAtFirstColCheck.setSelected(s.isLineCommentAtFirstColumn());
            addSpaceAtLineCommentStartCheck.setSelected(s.isAddSpaceAtLineCommentStart());
            enforceOnReformatCheck.setSelected(s.isEnforceOnReformat());
            blockCommentAtFirstColCheck.setSelected(s.isBlockCommentAtFirstColumn());
            addSpacesAroundBlockCommentsCheck.setSelected(s.isAddSpacesAroundBlockComments());

            insertOverrideCheck.setSelected(s.isInsertOverride());
            repeatSynchronizedCheck.setSelected(s.isRepeatSynchronized());
            annotationsToCopyList.getItems().setAll(s.getAnnotationsToCopy());
            useExternalAnnotationsCheck.setSelected(s.isUseExternalAnnotations());
            insertTypeUseAnnotationsBeforeTypeCheck.setSelected(s.isInsertTypeUseAnnotationsBeforeType());

            useClassIsInstanceCheck.setSelected(s.isUseClassIsInstanceAndCast());
            replaceNullCheckCheck.setSelected(s.isReplaceNullCheckWithObjectsNonNull());
            useIntegerSumCheck.setSelected(s.isUseIntegerSumWhenPossible());

            // Java EE
            entityEjbClassPrefixField.setText(s.getEntityEjbClassPrefix());
            entityEjbClassSuffixField.setText(s.getEntityEjbClassSuffix());
            entityHomeInterfacePrefixField.setText(s.getEntityHomeInterfacePrefix());
            entityHomeInterfaceSuffixField.setText(s.getEntityHomeInterfaceSuffix());
            entityRemoteInterfacePrefixField.setText(s.getEntityRemoteInterfacePrefix());
            entityRemoteInterfaceSuffixField.setText(s.getEntityRemoteInterfaceSuffix());
            entityLocalHomeInterfacePrefixField.setText(s.getEntityLocalHomeInterfacePrefix());
            entityLocalHomeInterfaceSuffixField.setText(s.getEntityLocalHomeInterfaceSuffix());
            entityLocalInterfacePrefixField.setText(s.getEntityLocalInterfacePrefix());
            entityLocalInterfaceSuffixField.setText(s.getEntityLocalInterfaceSuffix());
            entityEjbNameTagPrefixField.setText(s.getEntityEjbNameTagPrefix());
            entityEjbNameTagSuffixField.setText(s.getEntityEjbNameTagSuffix());
            entityTransferObjectPrefixField.setText(s.getEntityTransferObjectPrefix());
            entityTransferObjectSuffixField.setText(s.getEntityTransferObjectSuffix());
            entityDefaultPkClassField.setText(s.getEntityDefaultPkClass());

            sessionEjbClassPrefixField.setText(s.getSessionEjbClassPrefix());
            sessionEjbClassSuffixField.setText(s.getSessionEjbClassSuffix());
            sessionHomeInterfacePrefixField.setText(s.getSessionHomeInterfacePrefix());
            sessionHomeInterfaceSuffixField.setText(s.getSessionHomeInterfaceSuffix());
            sessionRemoteInterfacePrefixField.setText(s.getSessionRemoteInterfacePrefix());
            sessionRemoteInterfaceSuffixField.setText(s.getSessionRemoteInterfaceSuffix());
            sessionLocalHomeInterfacePrefixField.setText(s.getSessionLocalHomeInterfacePrefix());
            sessionLocalHomeInterfaceSuffixField.setText(s.getSessionLocalHomeInterfaceSuffix());
            sessionLocalInterfacePrefixField.setText(s.getSessionLocalInterfacePrefix());
            sessionLocalInterfaceSuffixField.setText(s.getSessionLocalInterfaceSuffix());
            sessionServiceEndpointPrefixField.setText(s.getSessionServiceEndpointPrefix());
            sessionServiceEndpointSuffixField.setText(s.getSessionServiceEndpointSuffix());
            sessionEjbNameTagPrefixField.setText(s.getSessionEjbNameTagPrefix());
            sessionEjbNameTagSuffixField.setText(s.getSessionEjbNameTagSuffix());

            servletClassPrefixField.setText(s.getServletClassPrefix());
            servletClassSuffixField.setText(s.getServletClassSuffix());
            servletNameTagPrefixField.setText(s.getServletNameTagPrefix());
            servletNameTagSuffixField.setText(s.getServletNameTagSuffix());

            mdbEjbClassPrefixField.setText(s.getMdbEjbClassPrefix());
            mdbEjbClassSuffixField.setText(s.getMdbEjbClassSuffix());
            mdbEjbNameTagPrefixField.setText(s.getMdbEjbNameTagPrefix());
            mdbEjbNameTagSuffixField.setText(s.getMdbEjbNameTagSuffix());

            filterClassPrefixField.setText(s.getFilterClassPrefix());
            filterClassSuffixField.setText(s.getFilterClassSuffix());
            filterNameTagPrefixField.setText(s.getFilterNameTagPrefix());
            filterNameTagSuffixField.setText(s.getFilterNameTagSuffix());

            listenerClassPrefixField.setText(s.getListenerClassPrefix());
            listenerClassSuffixField.setText(s.getListenerClassSuffix());

        } finally {
            suppressEvents = false;
        }
    }

    private void updateSettingsFromControls() {
        // Tabs & indents
        currentSettings.setUseTabCharacter(useTabCharCheck.isSelected());
        currentSettings.setSmartTabs(smartTabsCheck.isSelected());
        currentSettings.setTabSize(parseInt(tabSizeField.getText(), 4));
        currentSettings.setIndent(parseInt(indentField.getText(), 4));
        currentSettings.setContinuationIndent(parseInt(continuationIndentField.getText(), 8));
        currentSettings.setKeepIndentsOnEmptyLines(keepIndentsEmptyLinesCheck.isSelected());
        currentSettings.setLabelIndent(parseInt(labelIndentField.getText(), 0));
        currentSettings.setAbsoluteLabelIndent(absoluteLabelIndentCheck.isSelected());
        currentSettings.setDoNotIndentTopLevelMembers(doNotIndentTopLevelMembersCheck.isSelected());
        currentSettings.setUseIndentsRelativeToExpressionStart(useIndentsRelativeToExprCheck.isSelected());

        // Spaces
        // Before parentheses
        currentSettings.setSpaceBeforeMethodDeclParen(spaceBeforeMethodDeclParensCheck.isSelected());
        currentSettings.setSpaceBeforeMethodCallParen(spaceBeforeMethodCallParensCheck.isSelected());
        currentSettings.setSpaceBeforeIfParen(spaceBeforeIfParensCheck.isSelected());
        currentSettings.setSpaceBeforeForParen(spaceBeforeForParensCheck.isSelected());
        currentSettings.setSpaceBeforeWhileParen(spaceBeforeWhileParensCheck.isSelected());
        currentSettings.setSpaceBeforeSwitchParen(spaceBeforeSwitchParensCheck.isSelected());
        currentSettings.setSpaceBeforeTryParen(spaceBeforeTryParensCheck.isSelected());
        currentSettings.setSpaceBeforeCatchParen(spaceBeforeCatchParensCheck.isSelected());
        currentSettings.setSpaceBeforeSynchronizedParen(spaceBeforeSynchronizedParensCheck.isSelected());
        currentSettings.setSpaceBeforeAnnotationParens(spaceBeforeAnnotationParensCheck.isSelected());
        currentSettings.setSpaceBeforeDeconstructionList(spaceBeforeDeconstructionListCheck.isSelected());

        // Around operators
        currentSettings.setSpaceAroundAssignmentOps(spaceAroundAssignmentOpsCheck.isSelected());
        currentSettings.setSpaceAroundLogicalOps(spaceAroundLogicalOpsCheck.isSelected());
        currentSettings.setSpaceAroundEqualityOps(spaceAroundEqualityOpsCheck.isSelected());
        currentSettings.setSpaceAroundRelationalOps(spaceAroundRelationalOpsCheck.isSelected());
        currentSettings.setSpaceAroundBitwiseOps(spaceAroundBitwiseOpsCheck.isSelected());
        currentSettings.setSpaceAroundAdditiveOps(spaceAroundAdditiveOpsCheck.isSelected());
        currentSettings.setSpaceAroundMultiplicativeOps(spaceAroundMultiplicativeOpsCheck.isSelected());
        currentSettings.setSpaceAroundShiftOps(spaceAroundShiftOpsCheck.isSelected());
        currentSettings.setSpaceAroundUnaryOps(spaceAroundUnaryOpsCheck.isSelected());
        currentSettings.setSpaceAroundLambdaArrow(spaceAroundLambdaArrowCheck.isSelected());
        currentSettings.setSpaceAroundMethodRefDoubleColon(spaceAroundMethodRefDoubleColonCheck.isSelected());

        // Before left brace
        currentSettings.setSpaceBeforeClassLeftBrace(spaceBeforeClassLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeMethodLeftBrace(spaceBeforeMethodLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeIfLeftBrace(spaceBeforeIfLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeElseLeftBrace(spaceBeforeElseLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeWhileLeftBrace(spaceBeforeWhileLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeForLeftBrace(spaceBeforeForLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeDoLeftBrace(spaceBeforeDoLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeSwitchLeftBrace(spaceBeforeSwitchLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeTryLeftBrace(spaceBeforeTryLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeCatchLeftBrace(spaceBeforeCatchLeftBraceCheck.isSelected());
        currentSettings.setSpaceBeforeFinallyLeftBrace(spaceBeforeFinallyLeftBraceCheck.isSelected());

        currentSettings.setSpaceBeforeElse(spaceBeforeElseCheck.isSelected());
        currentSettings.setSpaceBeforeWhile(spaceBeforeWhileCheck.isSelected());
        currentSettings.setSpaceBeforeCatch(spaceBeforeCatchCheck.isSelected());
        currentSettings.setSpaceBeforeFinally(spaceBeforeFinallyCheck.isSelected());

        currentSettings.setWithinCodeBraces(withinCodeBracesCheck.isSelected());
        currentSettings.setWithinBrackets(withinBracketsCheck.isSelected());
        currentSettings.setWithinArrayInitializerBraces(withinArrayInitBracesCheck.isSelected());
        currentSettings.setWithinEmptyArrayInitializerBraces(withinEmptyArrayInitBracesCheck.isSelected());
        currentSettings.setWithinGroupingParens(withinGroupingParensCheck.isSelected());
        currentSettings.setWithinMethodDeclParens(withinMethodDeclParensCheck.isSelected());
        currentSettings.setWithinEmptyMethodDeclParens(withinEmptyMethodDeclParensCheck.isSelected());
        currentSettings.setWithinMethodCallParens(withinMethodCallParensCheck.isSelected());
        currentSettings.setWithinEmptyMethodCallParens(withinEmptyMethodCallParensCheck.isSelected());
        currentSettings.setWithinIfParens(withinIfParensCheck.isSelected());
        currentSettings.setWithinForParens(withinForParensCheck.isSelected());
        currentSettings.setWithinWhileParens(withinWhileParensCheck.isSelected());
        currentSettings.setWithinSwitchParens(withinSwitchParensCheck.isSelected());
        currentSettings.setWithinTryParens(withinTryParensCheck.isSelected());
        currentSettings.setWithinCatchParens(withinCatchParensCheck.isSelected());
        currentSettings.setWithinSynchronizedParens(withinSynchronizedParensCheck.isSelected());
        currentSettings.setWithinTypeCastParens(withinTypeCastParensCheck.isSelected());
        currentSettings.setWithinAnnotationParens(withinAnnotationParensCheck.isSelected());
        currentSettings.setWithinAngleBrackets(withinAngleBracketsCheck.isSelected());
        currentSettings.setWithinRecordHeader(withinRecordHeaderCheck.isSelected());
        currentSettings.setWithinDeconstructionList(withinDeconstructionListCheck.isSelected());
        currentSettings.setWithinBlockBracesWhenBodyPresent(withinBlockBracesWhenBodyCheck.isSelected());

        currentSettings.setTernaryBeforeQuestion(ternaryBeforeQuestionCheck.isSelected());
        currentSettings.setTernaryAfterQuestion(ternaryAfterQuestionCheck.isSelected());
        currentSettings.setTernaryBeforeColon(ternaryBeforeColonCheck.isSelected());
        currentSettings.setTernaryAfterColon(ternaryAfterColonCheck.isSelected());

        currentSettings.setTypeArgsAfterComma(typeArgsAfterCommaCheck.isSelected());
        currentSettings.setTypeArgsBeforeOpenAngle(typeArgsBeforeOpenAngleCheck.isSelected());
        currentSettings.setTypeArgsAfterCloseAngle(typeArgsAfterCloseAngleCheck.isSelected());

        currentSettings.setSpaceBeforeComma(otherBeforeCommaCheck.isSelected());
        currentSettings.setSpaceAfterComma(otherAfterCommaCheck.isSelected());
        currentSettings.setSpaceBeforeForSemicolon(otherBeforeForSemicolonCheck.isSelected());
        currentSettings.setSpaceAfterForSemicolon(otherAfterForSemicolonCheck.isSelected());
        currentSettings.setSpaceAfterTypeCast(otherAfterTypeCastCheck.isSelected());
        currentSettings.setSpaceAroundEqualsInAnnotation(otherAroundEqualsInAnnotationCheck.isSelected());
        currentSettings.setSpaceBeforeColonInForEach(otherBeforeColonInForEachCheck.isSelected());
        currentSettings.setSpaceInsideOneLineEnumBraces(otherInsideOneLineEnumBracesCheck.isSelected());

        currentSettings.setTypeParamsBeforeOpenAngle(typeParamsBeforeOpenAngleCheck.isSelected());
        currentSettings.setTypeParamsAroundTypeBounds(typeParamsAroundTypeBoundsCheck.isSelected());

        // Wrapping
        currentSettings.setKeepLineBreaks(keepLineBreaksCheck.isSelected());
        currentSettings.setCommentAtFirstColumn(commentAtFirstColumnCheck.isSelected());
        currentSettings.setControlStatementInOneLine(controlStatementInOneLineCheck.isSelected());
        currentSettings.setMultipleExpressionsInOneLine(multipleExpressionsInOneLineCheck.isSelected());
        currentSettings.setKeepSimpleBlocksInOneLine(keepSimpleBlocksCheck.isSelected());
        currentSettings.setKeepSimpleMethodsInOneLine(keepSimpleMethodsCheck.isSelected());
        currentSettings.setKeepSimpleLambdasInOneLine(keepSimpleLambdasCheck.isSelected());
        currentSettings.setKeepSimpleClassesInOneLine(keepSimpleClassesCheck.isSelected());
        currentSettings.setEnsureRightMargin(ensureRightMarginCheck.isSelected());

        if (classBraceCombo.getValue() != null) currentSettings.setClassBracePlacement(classBraceCombo.getValue());
        if (methodBraceCombo.getValue() != null) currentSettings.setMethodBracePlacement(methodBraceCombo.getValue());
        if (lambdaBraceCombo.getValue() != null) currentSettings.setLambdaBracePlacement(lambdaBraceCombo.getValue());
        if (otherBraceCombo.getValue() != null) currentSettings.setOtherBracePlacement(otherBraceCombo.getValue());

        if (extendsImplementsWrapCombo.getValue() != null) currentSettings.setExtendsImplementsWrap(extendsImplementsWrapCombo.getValue());
        currentSettings.setAlignExtendsImplementsMultiline(alignExtendsImplementsCheck.isSelected());
        if (extendsImplementsKeywordWrapCombo.getValue() != null) currentSettings.setExtendsImplementsKeywordWrap(extendsImplementsKeywordWrapCombo.getValue());

        if (throwsWrapCombo.getValue() != null) currentSettings.setThrowsWrap(throwsWrapCombo.getValue());
        currentSettings.setAlignThrowsMultiline(alignThrowsCheck.isSelected());
        currentSettings.setAlignThrowsToMethodStart(alignThrowsToMethodStartCheck.isSelected());
        if (throwsKeywordWrapCombo.getValue() != null) currentSettings.setThrowsKeywordWrap(throwsKeywordWrapCombo.getValue());

        if (methodDeclParamsWrapCombo.getValue() != null) currentSettings.setMethodDeclParametersWrap(methodDeclParamsWrapCombo.getValue());
        currentSettings.setAlignMethodDeclParameters(alignMethodDeclParamsCheck.isSelected());
        currentSettings.setNewLineAfterMethodDeclLParen(newLineAfterMethodDeclLParenCheck.isSelected());
        currentSettings.setPlaceMethodDeclRParenOnNewLine(placeMethodDeclRParenOnNewLineCheck.isSelected());

        if (methodCallArgsWrapCombo.getValue() != null) currentSettings.setMethodCallArgumentsWrap(methodCallArgsWrapCombo.getValue());
        currentSettings.setAlignMethodCallArguments(alignMethodCallArgsCheck.isSelected());
        currentSettings.setTakePriorityOverCallChainWrapping(takePriorityOverCallChainWrapCheck.isSelected());
        currentSettings.setNewLineAfterMethodCallLParen(newLineAfterMethodCallLParenCheck.isSelected());
        currentSettings.setPlaceMethodCallRParenOnNewLine(placeMethodCallRParenOnNewLineCheck.isSelected());

        currentSettings.setAlignMethodParenthesesMultiline(alignMethodParenthesesCheck.isSelected());
        currentSettings.setNewLineWhenMethodBodyPresented(newLineWhenMethodBodyPresentedCheck.isSelected());

        if (chainedMethodCallsWrapCombo.getValue() != null) currentSettings.setChainedMethodCallsWrap(chainedMethodCallsWrapCombo.getValue());
        currentSettings.setWrapFirstCall(wrapFirstCallCheck.isSelected());
        currentSettings.setAlignChainedCallsMultiline(alignChainedCallsMultilineCheck.isSelected());
        if (builderMethodsCombo.getValue() != null) currentSettings.setBuilderMethods(builderMethodsCombo.getValue());
        currentSettings.setKeepBuilderMethodsIndents(keepBuilderMethodsIndentsCheck.isSelected());
        currentSettings.setMoveSemicolonToNewLine(moveSemicolonToNewLineCheck.isSelected());

        if (ifForceBracesCombo.getValue() != null) currentSettings.setIfForceBraces(ifForceBracesCombo.getValue());
        currentSettings.setElseOnNewLine(elseOnNewLineCheck.isSelected());
        currentSettings.setSpecialElseIfTreatment(specialElseIfTreatmentCheck.isSelected());

        if (forStatementWrapCombo.getValue() != null) currentSettings.setForStatementWrap(forStatementWrapCombo.getValue());
        currentSettings.setAlignForMultiline(alignForMultilineCheck.isSelected());
        currentSettings.setNewLineAfterForLParen(newLineAfterForLParenCheck.isSelected());
        currentSettings.setPlaceForRParenOnNewLine(placeForRParenOnNewLineCheck.isSelected());
        if (forForceBracesCombo.getValue() != null) currentSettings.setForForceBraces(forForceBracesCombo.getValue());

        if (whileForceBracesCombo.getValue() != null) currentSettings.setWhileForceBraces(whileForceBracesCombo.getValue());

        if (doWhileForceBracesCombo.getValue() != null) currentSettings.setDoWhileForceBraces(doWhileForceBracesCombo.getValue());
        currentSettings.setWhileOnNewLine(whileOnNewLineCheck.isSelected());

        if (switchWrapCombo.getValue() != null) currentSettings.setSwitchWrap(switchWrapCombo.getValue());
        currentSettings.setIndentCaseBranches(indentCaseBranchesCheck.isSelected());
        currentSettings.setEachCaseOnSeparateLine(eachCaseOnSeparateLineCheck.isSelected());

        if (tryWithResourcesWrapCombo.getValue() != null) currentSettings.setTryWithResourcesWrap(tryWithResourcesWrapCombo.getValue());
        currentSettings.setAlignTryWithResourcesMultiline(alignTryWithResourcesCheck.isSelected());
        currentSettings.setNewLineAfterTryWithResourcesLParen(newLineAfterTryWithResourcesLParenCheck.isSelected());
        currentSettings.setPlaceTryWithResourcesRParenOnNewLine(placeTryWithResourcesRParenOnNewLineCheck.isSelected());

        currentSettings.setCatchOnNewLine(catchOnNewLineCheck.isSelected());
        currentSettings.setFinallyOnNewLine(finallyOnNewLineCheck.isSelected());
        if (typesInMultiCatchWrapCombo.getValue() != null) currentSettings.setTypesInMultiCatchWrap(typesInMultiCatchWrapCombo.getValue());
        currentSettings.setAlignTypesInMultiCatch(alignTypesInMultiCatchCheck.isSelected());

        if (binaryExpressionsWrapCombo.getValue() != null) currentSettings.setBinaryExpressionsWrap(binaryExpressionsWrapCombo.getValue());
        currentSettings.setAlignBinaryExpressionsMultiline(alignBinaryExpressionsCheck.isSelected());
        currentSettings.setOperationSignOnNextLine(operationSignOnNextLineCheck.isSelected());
        currentSettings.setAlignParenthesisedMultiline(alignParenthesisedCheck.isSelected());
        currentSettings.setNewLineAfterBinaryLParen(newLineAfterBinaryLParenCheck.isSelected());
        currentSettings.setPlaceBinaryRParenOnNewLine(placeBinaryRParenOnNewLineCheck.isSelected());

        if (assignmentWrapCombo.getValue() != null) currentSettings.setAssignmentWrap(assignmentWrapCombo.getValue());
        currentSettings.setAlignAssignmentMultiline(alignAssignmentCheck.isSelected());
        currentSettings.setAssignmentSignOnNextLine(assignmentSignOnNextLineCheck.isSelected());

        currentSettings.setAlignFieldsInColumns(alignFieldsInColumnsCheck.isSelected());
        currentSettings.setAlignVariablesInColumns(alignVariablesInColumnsCheck.isSelected());
        currentSettings.setAlignAssignmentsInColumns(alignAssignmentsInColumnsCheck.isSelected());
        currentSettings.setAlignSimpleMethodsInColumns(alignSimpleMethodsInColumnsCheck.isSelected());

        if (ternaryWrapCombo.getValue() != null) currentSettings.setTernaryWrap(ternaryWrapCombo.getValue());
        currentSettings.setAlignTernaryMultiline(alignTernaryCheck.isSelected());
        currentSettings.setTernarySignsOnNextLine(ternarySignsOnNextLineCheck.isSelected());

        if (arrayInitializerWrapCombo.getValue() != null) currentSettings.setArrayInitializerWrap(arrayInitializerWrapCombo.getValue());
        currentSettings.setAlignArrayInitializerMultiline(alignArrayInitializerCheck.isSelected());
        currentSettings.setNewLineAfterArrayInitializerLBrace(newLineAfterArrayInitializerLBraceCheck.isSelected());
        currentSettings.setPlaceArrayInitializerRBraceOnNewLine(placeArrayInitializerRBraceOnNewLineCheck.isSelected());

        currentSettings.setWrapAfterModifierList(wrapAfterModifierListCheck.isSelected());

        if (assertStatementWrapCombo.getValue() != null) currentSettings.setAssertStatementWrap(assertStatementWrapCombo.getValue());
        if (enumConstantsWrapCombo.getValue() != null) currentSettings.setEnumConstantsWrap(enumConstantsWrapCombo.getValue());
        if (classAnnotationsWrapCombo.getValue() != null) currentSettings.setClassAnnotationsWrap(classAnnotationsWrapCombo.getValue());
        if (methodAnnotationsWrapCombo.getValue() != null) currentSettings.setMethodAnnotationsWrap(methodAnnotationsWrapCombo.getValue());
        if (fieldAnnotationsWrapCombo.getValue() != null) currentSettings.setFieldAnnotationsWrap(fieldAnnotationsWrapCombo.getValue());
        currentSettings.setDoNotWrapAfterSingleFieldAnnotation(doNotWrapAfterSingleFieldAnnotationCheck.isSelected());
        if (parameterAnnotationsWrapCombo.getValue() != null) currentSettings.setParameterAnnotationsWrap(parameterAnnotationsWrapCombo.getValue());
        currentSettings.setDoNotWrapAfterSingleParameterAnnotation(doNotWrapAfterSingleParameterAnnotationCheck.isSelected());
        if (localVariableAnnotationsWrapCombo.getValue() != null) currentSettings.setLocalVariableAnnotationsWrap(localVariableAnnotationsWrapCombo.getValue());
        if (enumFieldAnnotationsWrapCombo.getValue() != null) currentSettings.setEnumFieldAnnotationsWrap(enumFieldAnnotationsWrapCombo.getValue());
        if (annotationParametersWrapCombo.getValue() != null) currentSettings.setAnnotationParametersWrap(annotationParametersWrapCombo.getValue());
        currentSettings.setAlignAnnotationParametersMultiline(alignAnnotationParametersCheck.isSelected());
        currentSettings.setNewLineAfterAnnotationParametersLParen(newLineAfterAnnotationParametersLParenCheck.isSelected());
        currentSettings.setPlaceAnnotationParametersRParenOnNewLine(placeAnnotationParametersRParenOnNewLineCheck.isSelected());

        currentSettings.setAlignTextBlocksMultiline(alignTextBlocksCheck.isSelected());

        if (recordComponentsWrapCombo.getValue() != null) currentSettings.setRecordComponentsWrap(recordComponentsWrapCombo.getValue());
        currentSettings.setAlignRecordComponentsMultiline(alignRecordComponentsCheck.isSelected());
        currentSettings.setNewLineAfterRecordComponentsLParen(newLineAfterRecordComponentsLParenCheck.isSelected());
        currentSettings.setPlaceRecordComponentsRParenOnNewLine(placeRecordComponentsRParenOnNewLineCheck.isSelected());
        currentSettings.setNewLineForRecordComponentAnnotations(newLineForRecordComponentAnnotationsCheck.isSelected());

        if (deconstructionPatternsWrapCombo.getValue() != null) currentSettings.setDeconstructionPatternsWrap(deconstructionPatternsWrapCombo.getValue());
        currentSettings.setAlignDeconstructionPatternsMultiline(alignDeconstructionPatternsCheck.isSelected());
        currentSettings.setNewLineAfterDeconstructionPatternsLParen(newLineAfterDeconstructionPatternsLParenCheck.isSelected());
        currentSettings.setPlaceDeconstructionPatternsRParenOnNewLine(placeDeconstructionPatternsRParenOnNewLineCheck.isSelected());

        // Blank lines
        currentSettings.setKeepBlankLinesInDeclarations(parseInt(keepBlankLinesInDeclField.getText(), 2));
        currentSettings.setKeepBlankLinesInCode(parseInt(keepBlankLinesInCodeField.getText(), 2));
        currentSettings.setKeepBlankLinesBeforeRBrace(parseInt(keepBlankLinesBeforeRBraceField.getText(), 2));
        currentSettings.setKeepBlankLinesBetweenHeaderAndPackage(parseInt(keepBlankLinesBetweenHeaderAndPackageField.getText(), 2));
        currentSettings.setBlankLinesBeforePackage(parseInt(blankLinesBeforePackageField.getText(), 0));
        currentSettings.setBlankLinesAfterPackage(parseInt(blankLinesAfterPackageField.getText(), 1));
        currentSettings.setBlankLinesBeforeImports(parseInt(blankLinesBeforeImportsField.getText(), 1));
        currentSettings.setBlankLinesAfterImports(parseInt(blankLinesAfterImportsField.getText(), 1));
        currentSettings.setBlankLinesAroundClass(parseInt(blankLinesAroundClassField.getText(), 1));
        currentSettings.setBlankLinesAfterClassHeader(parseInt(blankLinesAfterClassHeaderField.getText(), 0));
        currentSettings.setBlankLinesBeforeClassEnd(parseInt(blankLinesBeforeClassEndField.getText(), 0));
        currentSettings.setBlankLinesAfterAnonymousClassHeader(parseInt(blankLinesAfterAnonymousClassHeaderField.getText(), 0));
        currentSettings.setBlankLinesBeforeFieldInInterface(parseInt(blankLinesBeforeFieldInInterfaceField.getText(), 0));
        currentSettings.setBlankLinesBeforeFieldWithoutAnnotations(parseInt(blankLinesBeforeFieldWithoutAnnotationsField.getText(), 0));
        currentSettings.setBlankLinesBeforeFieldWithAnnotations(parseInt(blankLinesBeforeFieldWithAnnotationsField.getText(), 0));
        currentSettings.setBlankLinesAroundMethodInInterface(parseInt(blankLinesAroundMethodInInterfaceField.getText(), 1));
        currentSettings.setBlankLinesAroundMethod(parseInt(blankLinesAroundMethodField.getText(), 1));
        currentSettings.setBlankLinesBeforeMethodBody(parseInt(blankLinesBeforeMethodBodyField.getText(), 0));
        currentSettings.setBlankLinesAroundInitializer(parseInt(blankLinesAroundInitializerField.getText(), 1));
        currentSettings.setBlankLinesBetweenRecordComponents(parseInt(blankLinesBetweenRecordComponentsField.getText(), 0));

        // JavaDoc
        currentSettings.setEnableJavaDocFormatting(enableJavaDocFormattingCheck.isSelected());
        currentSettings.setAlignParamDescriptions(alignParamDescriptionsCheck.isSelected());
        currentSettings.setAlignThrownExceptions(alignThrownExceptionsCheck.isSelected());
        currentSettings.setBlankLinesAfterDescription(blankLinesAfterDescCheck.isSelected());
        currentSettings.setBlankLinesAfterParamDescriptions(blankLinesAfterParamDescCheck.isSelected());
        currentSettings.setBlankLinesAfterReturnTag(blankLinesAfterReturnTagCheck.isSelected());
        currentSettings.setKeepInvalidTags(keepInvalidTagsCheck.isSelected());
        currentSettings.setKeepEmptyParamTags(keepEmptyParamTagsCheck.isSelected());
        currentSettings.setKeepEmptyReturnTags(keepEmptyReturnTagsCheck.isSelected());
        currentSettings.setKeepEmptyThrowsTags(keepEmptyThrowsTagsCheck.isSelected());
        currentSettings.setWrapAtRightMargin(wrapAtRightMarginCheck.isSelected());
        currentSettings.setEnableLeadingAsterisks(enableLeadingAsterisksCheck.isSelected());
        currentSettings.setUseThrowsRatherThanException(useThrowsRatherThanExceptionCheck.isSelected());
        currentSettings.setGeneratePOnEmptyLines(generatePOnEmptyLinesCheck.isSelected());
        currentSettings.setKeepEmptyLines(keepEmptyLinesCheck.isSelected());
        currentSettings.setDoNotWrapOneLineComments(doNotWrapOneLineCommentsCheck.isSelected());
        currentSettings.setPreserveLineFeeds(preserveLineFeedsCheck.isSelected());
        currentSettings.setParamDescriptionsOnNewLine(paramDescriptionsOnNewLineCheck.isSelected());
        currentSettings.setIndentContinuationLines(indentContinuationLinesCheck.isSelected());

        // Imports
        currentSettings.setUseSingleClassImport(useSingleClassImportCheck.isSelected());
        currentSettings.setUseFullyQualifiedClassNames(useFullyQualifiedClassNamesCheck.isSelected());
        currentSettings.setInsertInnerClassImports(insertInnerClassImportsCheck.isSelected());
        currentSettings.setExcludedInnerClasses(new ArrayList<>(excludeInnerClassesList.getItems()));
        currentSettings.setDoNotSeparateModuleImports(doNotSeparateModuleImportsCheck.isSelected());
        currentSettings.setDeleteUnusedModuleImports(deleteUnusedModuleImportsCheck.isSelected());
        currentSettings.setUseFqNamesInJavadoc(useFqNamesInJavadocCombo.getValue());
        currentSettings.setClassCountToUseImportOnDemand(parseInt(classCountImportField.getText(), 5));
        currentSettings.setNamesCountToUseStaticImportOnDemand(parseInt(namesCountStaticImportField.getText(), 3));
        currentSettings.setPackagesToUseImportOnDemand(
                packagesToUseImportTable.getItems().stream().map(ImportEntry::copy).collect(Collectors.toList())
        );
        currentSettings.setPlaceOnDemandImportBeforeSingleClassImports(placeOnDemandImportBeforeSingleClassCheck.isSelected());
        currentSettings.setLayoutStaticImportsSeparately(layoutStaticImportsSeparatelyCheck.isSelected());
        currentSettings.setImportLayout(
                importLayoutTable.getItems().stream().map(ImportLayoutEntry::copy).collect(Collectors.toList())
        );

        // Arrangement
        currentSettings.setKeepGettersAndSettersTogether(keepGettersSettersTogetherCheck.isSelected());
        currentSettings.setKeepOverriddenMethodsTogether(keepOverriddenMethodsTogetherCheck.isSelected());
        if (overriddenMethodsOrderCombo.getValue() != null) {
            currentSettings.setOverriddenMethodsOrder(overriddenMethodsOrderCombo.getValue());
        }
        currentSettings.setKeepDependentMethodsTogether(keepDependentMethodsTogetherCheck.isSelected());
        if (dependentMethodsOrderCombo.getValue() != null) {
            currentSettings.setDependentMethodsOrder(dependentMethodsOrderCombo.getValue());
        }
        currentSettings.setMatchingRules(
                matchingRulesList.getItems().stream().map(JavaCodeStyleSettings.ArrangementRule::copy).collect(Collectors.toList())
        );

        // Code Gen
        currentSettings.setPreferLongerNames(preferLongerNamesCheck.isSelected());
        currentSettings.setFieldPrefix(fieldPrefixField.getText());
        currentSettings.setFieldSuffix(fieldSuffixField.getText());
        currentSettings.setStaticFieldPrefix(staticFieldPrefixField.getText());
        currentSettings.setStaticFieldSuffix(staticFieldSuffixField.getText());
        currentSettings.setParameterPrefix(paramPrefixField.getText());
        currentSettings.setParameterSuffix(paramSuffixField.getText());
        currentSettings.setLocalVariablePrefix(localPrefixField.getText());
        currentSettings.setLocalVariableSuffix(localSuffixField.getText());
        currentSettings.setSubclassPrefix(subclassPrefixField.getText());
        currentSettings.setSubclassSuffix(subclassSuffixField.getText());
        currentSettings.setTestClassPrefix(testClassPrefixField.getText());
        currentSettings.setTestClassSuffix(testClassSuffixField.getText());

        String visibility = "Public";
        if (visibilityEscalateRadio.isSelected()) visibility = "Escalate";
        else if (visibilityPrivateRadio.isSelected()) visibility = "Private";
        else if (visibilityPackageLocalRadio.isSelected()) visibility = "Package local";
        else if (visibilityProtectedRadio.isSelected()) visibility = "Protected";
        currentSettings.setDefaultVisibility(visibility);

        currentSettings.setMakeGeneratedLocalsFinal(makeLocalsFinalCheck.isSelected());
        currentSettings.setMakeGeneratedParametersFinal(makeParamsFinalCheck.isSelected());
        currentSettings.setUseVarForLocalVariables(useVarForLocalVarsCheck.isSelected());

        currentSettings.setLineCommentAtFirstColumn(lineCommentAtFirstColCheck.isSelected());
        currentSettings.setAddSpaceAtLineCommentStart(addSpaceAtLineCommentStartCheck.isSelected());
        currentSettings.setEnforceOnReformat(enforceOnReformatCheck.isSelected());
        currentSettings.setBlockCommentAtFirstColumn(blockCommentAtFirstColCheck.isSelected());
        currentSettings.setAddSpacesAroundBlockComments(addSpacesAroundBlockCommentsCheck.isSelected());

        currentSettings.setInsertOverride(insertOverrideCheck.isSelected());
        currentSettings.setRepeatSynchronized(repeatSynchronizedCheck.isSelected());
        currentSettings.setAnnotationsToCopy(new ArrayList<>(annotationsToCopyList.getItems()));
        currentSettings.setUseExternalAnnotations(useExternalAnnotationsCheck.isSelected());
        currentSettings.setInsertTypeUseAnnotationsBeforeType(insertTypeUseAnnotationsBeforeTypeCheck.isSelected());

        currentSettings.setUseClassIsInstanceAndCast(useClassIsInstanceCheck.isSelected());
        currentSettings.setReplaceNullCheckWithObjectsNonNull(replaceNullCheckCheck.isSelected());
        currentSettings.setUseIntegerSumWhenPossible(useIntegerSumCheck.isSelected());

        // Java EE
        currentSettings.setEntityEjbClassPrefix(entityEjbClassPrefixField.getText());
        currentSettings.setEntityEjbClassSuffix(entityEjbClassSuffixField.getText());
        currentSettings.setEntityHomeInterfacePrefix(entityHomeInterfacePrefixField.getText());
        currentSettings.setEntityHomeInterfaceSuffix(entityHomeInterfaceSuffixField.getText());
        currentSettings.setEntityRemoteInterfacePrefix(entityRemoteInterfacePrefixField.getText());
        currentSettings.setEntityRemoteInterfaceSuffix(entityRemoteInterfaceSuffixField.getText());
        currentSettings.setEntityLocalHomeInterfacePrefix(entityLocalHomeInterfacePrefixField.getText());
        currentSettings.setEntityLocalHomeInterfaceSuffix(entityLocalHomeInterfaceSuffixField.getText());
        currentSettings.setEntityLocalInterfacePrefix(entityLocalInterfacePrefixField.getText());
        currentSettings.setEntityLocalInterfaceSuffix(entityLocalInterfaceSuffixField.getText());
        currentSettings.setEntityEjbNameTagPrefix(entityEjbNameTagPrefixField.getText());
        currentSettings.setEntityEjbNameTagSuffix(entityEjbNameTagSuffixField.getText());
        currentSettings.setEntityTransferObjectPrefix(entityTransferObjectPrefixField.getText());
        currentSettings.setEntityTransferObjectSuffix(entityTransferObjectSuffixField.getText());
        currentSettings.setEntityDefaultPkClass(entityDefaultPkClassField.getText());

        currentSettings.setSessionEjbClassPrefix(sessionEjbClassPrefixField.getText());
        currentSettings.setSessionEjbClassSuffix(sessionEjbClassSuffixField.getText());
        currentSettings.setSessionHomeInterfacePrefix(sessionHomeInterfacePrefixField.getText());
        currentSettings.setSessionHomeInterfaceSuffix(sessionHomeInterfaceSuffixField.getText());
        currentSettings.setSessionRemoteInterfacePrefix(sessionRemoteInterfacePrefixField.getText());
        currentSettings.setSessionRemoteInterfaceSuffix(sessionRemoteInterfaceSuffixField.getText());
        currentSettings.setSessionLocalHomeInterfacePrefix(sessionLocalHomeInterfacePrefixField.getText());
        currentSettings.setSessionLocalHomeInterfaceSuffix(sessionLocalHomeInterfaceSuffixField.getText());
        currentSettings.setSessionLocalInterfacePrefix(sessionLocalInterfacePrefixField.getText());
        currentSettings.setSessionLocalInterfaceSuffix(sessionLocalInterfaceSuffixField.getText());
        currentSettings.setSessionServiceEndpointPrefix(sessionServiceEndpointPrefixField.getText());
        currentSettings.setSessionServiceEndpointSuffix(sessionServiceEndpointSuffixField.getText());
        currentSettings.setSessionEjbNameTagPrefix(sessionEjbNameTagPrefixField.getText());
        currentSettings.setSessionEjbNameTagSuffix(sessionEjbNameTagSuffixField.getText());

        currentSettings.setServletClassPrefix(servletClassPrefixField.getText());
        currentSettings.setServletClassSuffix(servletClassSuffixField.getText());
        currentSettings.setServletNameTagPrefix(servletNameTagPrefixField.getText());
        currentSettings.setServletNameTagSuffix(servletNameTagSuffixField.getText());

        currentSettings.setMdbEjbClassPrefix(mdbEjbClassPrefixField.getText());
        currentSettings.setMdbEjbClassSuffix(mdbEjbClassSuffixField.getText());
        currentSettings.setMdbEjbNameTagPrefix(mdbEjbNameTagPrefixField.getText());
        currentSettings.setMdbEjbNameTagSuffix(mdbEjbNameTagSuffixField.getText());

        currentSettings.setFilterClassPrefix(filterClassPrefixField.getText());
        currentSettings.setFilterClassSuffix(filterClassSuffixField.getText());
        currentSettings.setFilterNameTagPrefix(filterNameTagPrefixField.getText());
        currentSettings.setFilterNameTagSuffix(filterNameTagSuffixField.getText());

        currentSettings.setListenerClassPrefix(listenerClassPrefixField.getText());
        currentSettings.setListenerClassSuffix(listenerClassSuffixField.getText());
    }

    private int parseInt(String text, int defaultVal) {
        if (text == null || text.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void updatePreview() {
        previewLinesBox.getChildren().clear();

        String rawSample = getSampleForActiveTab();
        LanguageCodeStyleSettings lcs = new LanguageCodeStyleSettings("Java");
        currentSettings.syncTo(lcs);

        String formattedCode = CodeStyleSettings.formatCodeSample(rawSample, lcs);

        String[] lines = formattedCode.split("\n", -1);
        int tabSize = currentSettings.getTabSize();

        for (String line : lines) {
            TextFlow flow = renderCodeLine(line, tabSize);
            previewLinesBox.getChildren().add(flow);
        }
    }

    String getSampleForActiveTab() {
        return switch (activeTab) {
            case "Spaces" -> """
@Annotation(param1 = "value1", param2 = "value2")
@SuppressWarnings({"ALL"})
public class Foo<T extends Bar & Abba, U> {
    int[] X = new int[]{1, 3, 5, 6, 7, 87, 1213, 2};
    int[] empty = new int[]{};

    public void foo(int x, int y) {
        Runnable r = () -> {
        };
        Runnable r1 = this::bar;
        for (int i = 0; i < x; i++) {
            y += (y ^ 0x123) << 2;
        }
        for (int a : X) {
            System.out.print(a);
        }
        do {
            try (MyResource r1 = getResource(); MyResource r2 = null) {
                if (0 < x && x < 10) {
                    while (x != y) {
                        x = f(x * 3 + 5);
                    }
                } else {
                    synchronized (this) {
                        switch (e.getCode()) {
                            //...
                        }
                    }
                }
            } catch (MyException e) {
            } finally {
                int[] arr = (int[]) g(y);
                x = y >= 0 ? arr[y] : -1;
                Map<String, String> sMap = new HashMap<String, String>();
                Bar.<String, Integer>mess(null);
            }
        }
        while (true);

        switch (o) {
            case Rec(String s, int i) r -> {
            }
        }
    }

    void bar() {
        {
            return;
        }
    }
}

class Bar {
    static <U, T> U mess(T t) {
        return null;
    }
}

interface Abba {
}

public record Rec(String s, int i) {
}

class SimpleClass {
    class EmptyClass {
    }

    void emptyMethod() {
    }

    void complexMethodWithEmptyCodeBlocks() {
        try {
        } catch (Exception e) {
        }
        Runnable r = () -> {
        };
    }

    void oneLineMethod() {
        int x = 10;
    }

    void complexMethodWithOneLineCodeBlocks() {
        try {
            int x = 10;
        } catch (Exception e) {
            int y = 10;
        }
        Runnable r = () -> {
            int z = 30;
        };
    }
}
""";
            case "Wrapping and Braces" -> """
/*
 * This is a sample file.
 */

public class ThisIsASampleClass extends C1 implements I1, I2, I3, I4, I5 {
    private int f1 = 1;
    private String field2 = "";

    public void foo1(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
    }

    public void fooNonEmptyBody() {
        int x = 1;
    }

    public static void longerMethod() throws Exception1, Exception2, Exception3 {
        // todo something
        int
                i = 0;
        int[] a = new int[]{1, 2, 0x0052, 0x0053, 0x0054};
        int[] empty = new int[]{};
        int var1 = 1;
        int var2 = 2;
        foo1(0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057);
        int x = (3 + 4 + 5 + 6) * (7 + 8 + 9 + 10) * (11 + 12 + 13 + 14 + 0);
        String s1, s2, s3;
        s1 = s2 = s3 = "012345678901456";
        assert i + j + k + l + n + m <= 2 : "assert description";
        int y = 2 > 3 ? 7 + 8 + 9 : 11 + 12 + 13;
        super.getFoo().foo().getBar().bar();

        label:
        if (2 < 3) {
            return;
        } else if (2 > 3) return;
        else return;
        for (int i = 0; i < 0xFFFFFF; i += 2)
            System.out.println(i);
        while (x < 50000) x++;
        do x++; while (x < 10000);
        switch (a) {
            case 0:
            case 1:
                doCase0();
                break;
            case 2:
            case 3:
                return;
            default:
                doDefault();
        }
        try (MyResource r1 = getResource(); MyResource r2 = null) {
            doSomething();
        } catch (Exception e) {
            processException(e);
        } finally {
            processFinally();
        }
        do {
            x--;
        } while (x > 10);
        try (MyResource r1 = getResource();
             MyResource r2 = null) {
            doSomething();
        }
        Runnable r = () -> {
        };
    }

    public static void test()
            throws Exception {
        foo.foo().bar("arg1",
                "arg2");
        new Object() {
        };
    }

    class TestInnerClass {
    }

    interface TestInnerInterface {
    }
}

enum Breed {
    Dalmatian(), Labrador(), Dachshund()
}

public record RecordWithAnnotatedComponents(
        @Annotation1 @Annotation2 String s,
        @Annotation1 @Annotation3(param1 = "value1", param2 = "value2") Integer t,
        @Annotation3(param1 = "value1", param2 = "value2") @Annotation1 Double u,
        @Annotation3(param1 = "value1", param2 = "value2") @Annotation5(param1 = "value1", param2 = "value2") Long v
) {
}

@Annotation1
@Annotation2
@Annotation3(param1 = "value1", param2 = "value2")
@Annotation4
class Foo {
    @Annotation1
    @Annotation3(param1 = "value1", param2 = "value2")
    public static void foo() {
    }

    @Annotation1
    @Annotation3(param1 = "value1", param2 = "value2")
    public static int myFoo;

    public void method(@Annotation1 @Annotation3(param1 = "value1", param2 = "value2") final int param1,
                       @Annotation1 @Annotation3(param1 = "value1", param2 = "value2") final int localVariable) {
    }
}
""";
            case "Blank Lines" -> """
/*
 * This is a sample file.
 */
package dev.lumina.samples;

import dev.lumina.samples.Main;

import javax.swing.*;
import java.util.Vector;

import dev.lumina.annotations.NotNull;
import dev.lumina.annotations.Nullable;

public class Foo {
    private int field1;
    private int field2;

    {
        field1 = 2;
    }

    public void foo1() {
        new Runnable() {
            public void run() {
            }
        };
    }

    public class InnerClass {
    }
}

class AnotherClass {
}

public class ClassWithAnnotatedFields {
    @NotNull
    public Boolean publicAnnotatedField = true;
    public Boolean publicNonAnnotatedField = true;
    @NotNull Boolean typeAnnotatedField = false;
    @NotNull
    private Boolean firstPrivateAnnotatedField = true;
    @NotNull
    private Boolean secondPrivateAnnotatedField = true;
}

public record RecordWithAnnotatedComponents(@NotNull Double s, @Nullable String t, @NotNull Double u, @Nullable String v) {
}

interface TestInterface {
    int MAX = 10;
    int MIN = 1;

    void method1();

    void method2();
}
""";
            case "JavaDoc" -> """
package sample;

public class Sample {
    /**
     * This is a method description that is long enough to exceed right margin.
     * <p>
     * Another paragraph of the description placed after blank line.
     * </p>
     * Line with manual
     * line feed.
     *
     * @param i                       short-named parameter description
     * @param longParameterName       long named parameter description
     * @param missingDescription
     * @return return description.
     * @throws XXXException description.
     * @throws YException   description.
     * @throws ZException
     * @invalidTag
     */
    public abstract String sampleMethod(int i, int longParameterName, int missingDescription) throws XXXException, YException, ZException;

    /**
     * One-line comment
     */
    public abstract String sampleMethod2();

    /**
     * Simple method description
     *
     * @return
     */
    public abstract String sampleMethod3();
}
""";
            case "Imports" -> """
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Foo {
    private List<File> files;
}
""";
            case "Arrangement" -> """
public class Foo {
    public static final int CONSTANT = 100;
    private String name;
    private int count;

    public Foo(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void execute() {
    }
}
""";
            case "Code Generation" -> """
public class Foo implements Runnable {
    private String myName;

    public String getMyName() {
        return myName;
    }

    public void setMyName(final String myName) {
        this.myName = myName;
    }

    @Override
    public void run() {
        System.out.println(myName);
    }
}
""";
            case "Java EE Names" -> """
@Entity
public class CustomerEntity {
    @Id
    private Long id;
}

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderDAO orderDAO;
}
""";
            default -> """
public class Foo {
    public int[] X = new int[]{1, 3, 5, 7, 9, 11};

    public void foo(boolean a, int x, int y, int z) {
    label1:
        do {
            try {
                if (x > 0) {
                    int someVariable = a ? x : y;
                    int anotherVariable = a ? x : y;
                } else if (x < 0) {
                    int someVariable = (y + z);
                    someVariable = x = x + y;
                } else {
                label2:
                    for (int i = 0; i < 5; i++) doSomething(i);
                }
                switch (a) {
                    case 0:
                        doCase0();
                        break;
                    default:
                        doDefault();
                }
            } catch (Exception e) {
                processException(e.getMessage(), x + y, z, a);
            } finally {
                processFinally();
            }
        }
        while (true);

        if (2 < 3) return;
        if (3 < 4) return;
        do {
            x++;
        }
        while (x < 10000);
        while (x < 50000) x++;
        for (int i = 0; i < 5; i++) System.out.println(i);
    }

    private class InnerClass implements I1, I2 {
        public void bar() throws E1, E2 {
        }
    }
}
""";
        };
    }

    private TextFlow renderCodeLine(String line, int tabSize) {
        TextFlow flow = new TextFlow();
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);

        int leadingSpaces = 0;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == ' ') {
                leadingSpaces++;
                i++;
            } else if (c == '\t') {
                leadingSpaces += tabSize;
                i++;
            } else {
                break;
            }
        }

        String codeText = line.substring(i);

        if (leadingSpaces > 0) {
            StringBuilder guideBuilder = new StringBuilder();
            for (int col = 0; col < leadingSpaces; col++) {
                if (col % tabSize == 0) {
                    guideBuilder.append("·");
                } else {
                    guideBuilder.append(" ");
                }
            }
            Text indentGuideText = new Text(guideBuilder.toString());
            indentGuideText.setFont(Font.font("monospace", 12));
            indentGuideText.setFill(Color.web("#393B40"));
            flow.getChildren().add(indentGuideText);
        }

        highlightAndAppend(flow, codeText);
        return flow;
    }

    private void highlightAndAppend(TextFlow flow, String code) {
        if (code.isEmpty()) return;

        Pattern tokenPattern = Pattern.compile(
                "\\b(public|private|protected|class|interface|enum|record|void|int|long|boolean|char|float|double|" +
                        "try|catch|finally|throw|throws|if|else|do|while|for|switch|case|default|break|continue|return|" +
                        "new|package|import|extends|implements|permits|static|final|synchronized|transient|volatile|" +
                        "assert|super|this)\\b|" +
                        "(@[a-zA-Z0-9_]+)|" +
                        "(\"[^\"]*\")|" +
                        "(\\b0x[0-9a-fA-F]+\\b|\\b\\d+\\b)|" +
                        "(\\b[A-Z][a-zA-Z0-9_]*\\b)|" +
                        "(//.*|/\\*.*|\\*.*|.*\\*/)"
        );

        Matcher matcher = tokenPattern.matcher(code);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                Text plain = new Text(code.substring(lastEnd, matcher.start()));
                plain.setFont(Font.font("monospace", 12));
                plain.setFill(Color.web("#BCBEC4"));
                flow.getChildren().add(plain);
            }

            Text token = new Text(matcher.group());
            token.setFont(Font.font("monospace", 12));

            if (matcher.group(1) != null) {
                token.setFill(Color.web("#CF8E6D")); // Keyword (orange)
            } else if (matcher.group(2) != null) {
                token.setFill(Color.web("#BBB529")); // Annotation (yellowish-olive)
            } else if (matcher.group(3) != null) {
                token.setFill(Color.web("#6AAB73")); // String (green)
            } else if (matcher.group(4) != null) {
                token.setFill(Color.web("#2AACB8")); // Number (cyan/blue)
            } else if (matcher.group(5) != null) {
                token.setFill(Color.web("#C0B271")); // Class/Type (gold)
            } else if (matcher.group(6) != null) {
                token.setFill(Color.web("#7A7E85")); // Comment (gray)
            } else {
                token.setFill(Color.web("#BCBEC4"));
            }

            flow.getChildren().add(token);
            lastEnd = matcher.end();
        }

        if (lastEnd < code.length()) {
            Text plain = new Text(code.substring(lastEnd));
            plain.setFont(Font.font("monospace", 12));
            plain.setFill(Color.web("#BCBEC4"));
            flow.getChildren().add(plain);
        }
    }

    public void apply() {
        updateSettingsFromControls();
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme != null) {
            LanguageCodeStyleSettings lcs = scheme.getLanguageSettings("Java");
            currentSettings.syncTo(lcs);
            CodeStyleSettings.getInstance().saveSettings();
        }
        originalSettings = currentSettings.copy();
    }

    public void reset() {
        loadFromCurrentScheme();
        updatePreview();
    }

    public boolean isModified() {
        return !Objects.equals(currentSettings, originalSettings);
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CodeStyleHeaderBar getHeaderBar() {
        return headerBar;
    }

    public JavaCodeStyleSettings getCurrentSettings() {
        return currentSettings;
    }

    public String getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(String tabName) {
        this.activeTab = tabName;
        for (Toggle t : tabGroup.getToggles()) {
            if (t instanceof ToggleButton tb) {
                boolean match = tb.getText().equals(tabName);
                tb.setSelected(match);
                applyTabButtonStyle(tb, match);
            }
        }
        switchTab(tabName);
        updatePreview();
    }
}
