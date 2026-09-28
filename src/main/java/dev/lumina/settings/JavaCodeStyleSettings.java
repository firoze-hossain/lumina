package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Dedicated Java-specific code style settings model.
 * Encompasses all 9 configuration tabs:
 * 1. Tabs and Indents
 * 2. Spaces
 * 3. Wrapping and Braces
 * 4. Blank Lines
 * 5. JavaDoc
 * 6. Imports
 * 7. Arrangement
 * 8. Code Generation
 * 9. Java EE Names
 */
public class JavaCodeStyleSettings {

    public enum BracePlacement {
        END_OF_LINE("End of line"),
        NEXT_LINE("Next line"),
        NEXT_LINE_IF_WRAPPED("Next line if wrapped");

        private final String displayName;

        BracePlacement(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static class ImportEntry {
        private boolean isStatic;
        private String packageName;
        private boolean withSubpackages;

        public ImportEntry() {
            this(false, "", false);
        }

        public ImportEntry(boolean isStatic, String packageName, boolean withSubpackages) {
            this.isStatic = isStatic;
            this.packageName = packageName != null ? packageName : "";
            this.withSubpackages = withSubpackages;
        }

        public ImportEntry copy() {
            return new ImportEntry(isStatic, packageName, withSubpackages);
        }

        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName != null ? packageName : ""; }

        public boolean isWithSubpackages() { return withSubpackages; }
        public void setWithSubpackages(boolean withSubpackages) { this.withSubpackages = withSubpackages; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImportEntry that = (ImportEntry) o;
            return isStatic == that.isStatic &&
                    withSubpackages == that.withSubpackages &&
                    Objects.equals(packageName, that.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isStatic, packageName, withSubpackages);
        }
    }

    public static class ImportLayoutEntry {
        public enum EntryType {
            MODULE_IMPORTS,
            ALL_OTHER_IMPORTS,
            BLANK_LINE,
            PACKAGE,
            STATIC_ALL_OTHER_IMPORTS
        }

        private EntryType type;
        private boolean isStatic;
        private String packageName;
        private boolean withSubpackages;

        public ImportLayoutEntry() {
            this(EntryType.PACKAGE, false, "", false);
        }

        public ImportLayoutEntry(EntryType type, boolean isStatic, String packageName, boolean withSubpackages) {
            this.type = type != null ? type : EntryType.PACKAGE;
            this.isStatic = isStatic;
            this.packageName = packageName != null ? packageName : "";
            this.withSubpackages = withSubpackages;
        }

        public static ImportLayoutEntry moduleImports() {
            return new ImportLayoutEntry(EntryType.MODULE_IMPORTS, false, "module imports", false);
        }

        public static ImportLayoutEntry allOtherImports() {
            return new ImportLayoutEntry(EntryType.ALL_OTHER_IMPORTS, false, "all other imports", false);
        }

        public static ImportLayoutEntry blankLine() {
            return new ImportLayoutEntry(EntryType.BLANK_LINE, false, "<blank line>", false);
        }

        public static ImportLayoutEntry packageEntry(boolean isStatic, String packageName, boolean withSubpackages) {
            return new ImportLayoutEntry(EntryType.PACKAGE, isStatic, packageName, withSubpackages);
        }

        public static ImportLayoutEntry staticAllOtherImports() {
            return new ImportLayoutEntry(EntryType.STATIC_ALL_OTHER_IMPORTS, true, "static all other imports", false);
        }

        public ImportLayoutEntry copy() {
            return new ImportLayoutEntry(type, isStatic, packageName, withSubpackages);
        }

        public EntryType getType() { return type; }
        public void setType(EntryType type) { this.type = type != null ? type : EntryType.PACKAGE; }

        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName != null ? packageName : ""; }

        public boolean isWithSubpackages() { return withSubpackages; }
        public void setWithSubpackages(boolean withSubpackages) { this.withSubpackages = withSubpackages; }

        public String getDisplayText() {
            return switch (type) {
                case MODULE_IMPORTS -> "import module imports";
                case ALL_OTHER_IMPORTS -> "import all other imports";
                case BLANK_LINE -> "<blank line>";
                case STATIC_ALL_OTHER_IMPORTS -> "import static all other imports";
                case PACKAGE -> (isStatic ? "import static " : "import ") + packageName;
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImportLayoutEntry that = (ImportLayoutEntry) o;
            return isStatic == that.isStatic &&
                    withSubpackages == that.withSubpackages &&
                    type == that.type &&
                    Objects.equals(packageName, that.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, isStatic, packageName, withSubpackages);
        }
    }

    public static class ArrangementRule {
        private List<String> tags = new ArrayList<>();
        private String nameFilter = "";
        private String order = "keep order";
        private String alias = "";

        public ArrangementRule() {
        }

        public ArrangementRule(List<String> tags) {
            if (tags != null) {
                this.tags.addAll(tags);
            }
        }

        public ArrangementRule(List<String> tags, String nameFilter, String order, String alias) {
            if (tags != null) {
                this.tags.addAll(tags);
            }
            this.nameFilter = nameFilter != null ? nameFilter : "";
            this.order = order != null ? order : "keep order";
            this.alias = alias != null ? alias : "";
        }

        public ArrangementRule copy() {
            return new ArrangementRule(new ArrayList<>(tags), nameFilter, order, alias);
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        }

        public void addTag(String tag) {
            if (tag != null && !tag.isBlank() && !tags.contains(tag)) {
                tags.add(tag);
            }
        }

        public void removeTag(String tag) {
            tags.remove(tag);
        }

        public String getNameFilter() {
            return nameFilter;
        }

        public void setNameFilter(String nameFilter) {
            this.nameFilter = nameFilter != null ? nameFilter : "";
        }

        public String getOrder() {
            return order;
        }

        public void setOrder(String order) {
            this.order = order != null ? order : "keep order";
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias != null ? alias : "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArrangementRule that = (ArrangementRule) o;
            return Objects.equals(tags, that.tags) &&
                    Objects.equals(nameFilter, that.nameFilter) &&
                    Objects.equals(order, that.order) &&
                    Objects.equals(alias, that.alias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tags, nameFilter, order, alias);
        }

        public static List<ArrangementRule> getDefaultArrangementRules() {
            return List.of(
                    new ArrangementRule(List.of("field", "public", "static", "final")),
                    new ArrangementRule(List.of("field", "protected", "static", "final")),
                    new ArrangementRule(List.of("field", "package private", "static", "final")),
                    new ArrangementRule(List.of("field", "private", "static", "final")),
                    new ArrangementRule(List.of("field", "public", "static")),
                    new ArrangementRule(List.of("field", "protected", "static")),
                    new ArrangementRule(List.of("field", "package private", "static")),
                    new ArrangementRule(List.of("field", "private", "static")),
                    new ArrangementRule(List.of("initializer block", "static")),
                    new ArrangementRule(List.of("field", "public", "final")),
                    new ArrangementRule(List.of("field", "protected", "final")),
                    new ArrangementRule(List.of("field", "package private", "final")),
                    new ArrangementRule(List.of("field", "private", "final")),
                    new ArrangementRule(List.of("field", "public")),
                    new ArrangementRule(List.of("field", "protected")),
                    new ArrangementRule(List.of("field", "package private")),
                    new ArrangementRule(List.of("field", "private")),
                    new ArrangementRule(List.of("field")),
                    new ArrangementRule(List.of("initializer block")),
                    new ArrangementRule(List.of("constructor")),
                    new ArrangementRule(List.of("method", "static")),
                    new ArrangementRule(List.of("method")),
                    new ArrangementRule(List.of("enum")),
                    new ArrangementRule(List.of("interface")),
                    new ArrangementRule(List.of("class", "static")),
                    new ArrangementRule(List.of("class"))
            );
        }
    }

    public enum WrapOption {
        DO_NOT_WRAP("Do not wrap"),
        WRAP_IF_LONG("Wrap if long"),
        CHOP_DOWN_IF_LONG("Chop down if long"),
        WRAP_ALWAYS("Wrap always");

        private final String displayName;

        WrapOption(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // 1. Tabs and Indents
    private boolean useTabCharacter = false;
    private boolean smartTabs = false;
    private int tabSize = 4;
    private int indent = 4;
    private int continuationIndent = 8;
    private boolean keepIndentsOnEmptyLines = false;
    private int labelIndent = 0;
    private boolean absoluteLabelIndent = false;
    private boolean doNotIndentTopLevelMembers = false;
    private boolean useIndentsRelativeToExpressionStart = false;

    // 2. Spaces - Before parentheses
    private boolean spaceBeforeMethodDeclParen = false;
    private boolean spaceBeforeMethodCallParen = false;
    private boolean spaceBeforeIfParen = true;
    private boolean spaceBeforeForParen = true;
    private boolean spaceBeforeWhileParen = true;
    private boolean spaceBeforeSwitchParen = true;
    private boolean spaceBeforeTryParen = true;
    private boolean spaceBeforeCatchParen = true;
    private boolean spaceBeforeSynchronizedParen = true;
    private boolean spaceBeforeAnnotationParens = false;
    private boolean spaceBeforeDeconstructionList = false;

    // 2. Spaces - Around operators
    private boolean spaceAroundAssignmentOps = true;
    private boolean spaceAroundLogicalOps = true;
    private boolean spaceAroundEqualityOps = true;
    private boolean spaceAroundRelationalOps = true;
    private boolean spaceAroundBitwiseOps = true;
    private boolean spaceAroundAdditiveOps = true;
    private boolean spaceAroundMultiplicativeOps = true;
    private boolean spaceAroundShiftOps = true;
    private boolean spaceAroundUnaryOps = false;
    private boolean spaceAroundLambdaArrow = true;
    private boolean spaceAroundMethodRefDoubleColon = false;
    private boolean spaceBeforeClassLeftBrace = true;
    private boolean spaceBeforeMethodLeftBrace = true;
    private boolean spaceBeforeIfLeftBrace = true;
    private boolean spaceBeforeElseLeftBrace = true;
    private boolean spaceBeforeWhileLeftBrace = true;
    private boolean spaceBeforeForLeftBrace = true;
    private boolean spaceBeforeDoLeftBrace = true;
    private boolean spaceBeforeSwitchLeftBrace = true;
    private boolean spaceBeforeTryLeftBrace = true;
    private boolean spaceBeforeCatchLeftBrace = true;
    private boolean spaceBeforeFinallyLeftBrace = true;
    private boolean spaceBeforeElse = true;
    private boolean spaceBeforeWhile = true;
    private boolean spaceBeforeCatch = true;
    private boolean spaceBeforeFinally = true;
    private boolean spaceAfterComma = true;
    private boolean spaceBeforeComma = false;
    private boolean spaceAfterSemicolon = true;
    private boolean spaceBeforeSemicolon = false;
    private boolean spaceAfterColon = true;
    private boolean spaceBeforeColon = true;

    // Spaces: Within
    private boolean withinCodeBraces = false;
    private boolean withinBrackets = false;
    private boolean withinArrayInitializerBraces = false;
    private boolean withinEmptyArrayInitializerBraces = false;
    private boolean withinGroupingParens = false;
    private boolean withinMethodDeclParens = false;
    private boolean withinEmptyMethodDeclParens = false;
    private boolean withinMethodCallParens = false;
    private boolean withinEmptyMethodCallParens = false;
    private boolean withinIfParens = false;
    private boolean withinForParens = false;
    private boolean withinWhileParens = false;
    private boolean withinSwitchParens = false;
    private boolean withinTryParens = false;
    private boolean withinCatchParens = false;
    private boolean withinSynchronizedParens = false;
    private boolean withinTypeCastParens = false;
    private boolean withinAnnotationParens = false;
    private boolean withinAngleBrackets = false;
    private boolean withinRecordHeader = false;
    private boolean withinDeconstructionList = false;
    private boolean withinBlockBracesWhenBodyPresent = false;

    // Spaces: In ternary operator (?:)
    private boolean ternaryBeforeQuestion = true;
    private boolean ternaryAfterQuestion = true;
    private boolean ternaryBeforeColon = true;
    private boolean ternaryAfterColon = true;

    // Spaces: Type arguments
    private boolean typeArgsAfterComma = true;
    private boolean typeArgsBeforeOpenAngle = false;
    private boolean typeArgsAfterCloseAngle = false;

    // Spaces: Other
    private boolean spaceBeforeForSemicolon = false;
    private boolean spaceAfterForSemicolon = true;
    private boolean spaceAfterTypeCast = true;
    private boolean spaceAroundEqualsInAnnotation = true;
    private boolean spaceBeforeColonInForEach = true;
    private boolean spaceInsideOneLineEnumBraces = false;

    // Spaces: Type parameters
    private boolean typeParamsBeforeOpenAngle = false;
    private boolean typeParamsAroundTypeBounds = true;

    // 3. Wrapping and Braces
    private int hardWrapAt = 120;
    private boolean wrapOnTyping = false;
    private String visualGuides = "None";
    private boolean keepLineBreaks = true;
    private boolean commentAtFirstColumn = true;
    private boolean controlStatementInOneLine = true;
    private boolean multipleExpressionsInOneLine = false;
    private boolean keepSimpleBlocksInOneLine = false;
    private boolean keepSimpleMethodsInOneLine = false;
    private boolean keepSimpleLambdasInOneLine = false;
    private boolean keepSimpleClassesInOneLine = false;
    private boolean ensureRightMargin = false;
    private BracePlacement classBracePlacement = BracePlacement.END_OF_LINE;
    private BracePlacement methodBracePlacement = BracePlacement.END_OF_LINE;
    private BracePlacement lambdaBracePlacement = BracePlacement.END_OF_LINE;
    private BracePlacement otherBracePlacement = BracePlacement.END_OF_LINE;
    private WrapOption extendsImplementsWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignExtendsImplementsMultiline = false;
    private WrapOption extendsImplementsKeywordWrap = WrapOption.DO_NOT_WRAP;
    private WrapOption throwsWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignThrowsMultiline = false;
    private boolean alignThrowsToMethodStart = false;
    private WrapOption throwsKeywordWrap = WrapOption.DO_NOT_WRAP;
    private WrapOption methodDeclParametersWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignMethodDeclParameters = true;
    private boolean newLineAfterMethodDeclLParen = false;
    private boolean placeMethodDeclRParenOnNewLine = false;

    private WrapOption methodCallArgumentsWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignMethodCallArguments = false;
    private boolean takePriorityOverCallChainWrapping = false;
    private boolean newLineAfterMethodCallLParen = false;
    private boolean placeMethodCallRParenOnNewLine = false;

    private boolean alignMethodParenthesesMultiline = false;
    private boolean newLineWhenMethodBodyPresented = false;

    private WrapOption chainedMethodCallsWrap = WrapOption.DO_NOT_WRAP;
    private boolean wrapFirstCall = false;
    private boolean alignChainedCallsMultiline = false;
    private String builderMethods = "Method names";
    private boolean keepBuilderMethodsIndents = false;
    private boolean moveSemicolonToNewLine = false;

    private String ifForceBraces = "Do not force";
    private boolean elseOnNewLine = false;
    private boolean specialElseIfTreatment = true;

    private WrapOption forStatementWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignForMultiline = true;
    private boolean newLineAfterForLParen = false;
    private boolean placeForRParenOnNewLine = false;
    private String forForceBraces = "Do not force";

    private String whileForceBraces = "Do not force";

    private String doWhileForceBraces = "Do not force";
    private boolean whileOnNewLine = false;

    private WrapOption switchWrap = WrapOption.WRAP_IF_LONG;
    private boolean indentCaseBranches = true;
    private boolean eachCaseOnSeparateLine = true;

    private WrapOption tryWithResourcesWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignTryWithResourcesMultiline = true;
    private boolean newLineAfterTryWithResourcesLParen = false;
    private boolean placeTryWithResourcesRParenOnNewLine = false;

    private boolean catchOnNewLine = false;
    private boolean finallyOnNewLine = false;
    private WrapOption typesInMultiCatchWrap = WrapOption.WRAP_IF_LONG;
    private boolean alignTypesInMultiCatch = true;

    private WrapOption binaryExpressionsWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignBinaryExpressionsMultiline = false;
    private boolean operationSignOnNextLine = false;
    private boolean alignParenthesisedMultiline = false;
    private boolean newLineAfterBinaryLParen = false;
    private boolean placeBinaryRParenOnNewLine = false;

    private WrapOption assignmentWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignAssignmentMultiline = false;
    private boolean assignmentSignOnNextLine = false;

    private boolean alignFieldsInColumns = false;
    private boolean alignVariablesInColumns = false;
    private boolean alignAssignmentsInColumns = false;
    private boolean alignSimpleMethodsInColumns = false;

    private WrapOption ternaryWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignTernaryMultiline = false;
    private boolean ternarySignsOnNextLine = false;

    private WrapOption arrayInitializerWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignArrayInitializerMultiline = false;
    private boolean newLineAfterArrayInitializerLBrace = false;
    private boolean placeArrayInitializerRBraceOnNewLine = false;

    private boolean wrapAfterModifierList = false;

    private WrapOption assertStatementWrap = WrapOption.DO_NOT_WRAP;
    private WrapOption enumConstantsWrap = WrapOption.DO_NOT_WRAP;
    private WrapOption classAnnotationsWrap = WrapOption.WRAP_ALWAYS;
    private WrapOption methodAnnotationsWrap = WrapOption.WRAP_ALWAYS;
    private WrapOption fieldAnnotationsWrap = WrapOption.WRAP_ALWAYS;
    private boolean doNotWrapAfterSingleFieldAnnotation = false;
    private WrapOption parameterAnnotationsWrap = WrapOption.DO_NOT_WRAP;
    private boolean doNotWrapAfterSingleParameterAnnotation = false;
    private WrapOption localVariableAnnotationsWrap = WrapOption.DO_NOT_WRAP;
    private WrapOption enumFieldAnnotationsWrap = WrapOption.DO_NOT_WRAP;
    private WrapOption annotationParametersWrap = WrapOption.DO_NOT_WRAP;
    private boolean alignAnnotationParametersMultiline = false;
    private boolean newLineAfterAnnotationParametersLParen = false;
    private boolean placeAnnotationParametersRParenOnNewLine = false;

    private boolean alignTextBlocksMultiline = false;

    private WrapOption recordComponentsWrap = WrapOption.WRAP_IF_LONG;
    private boolean alignRecordComponentsMultiline = true;
    private boolean newLineAfterRecordComponentsLParen = false;
    private boolean placeRecordComponentsRParenOnNewLine = false;
    private boolean newLineForRecordComponentAnnotations = false;

    private WrapOption deconstructionPatternsWrap = WrapOption.WRAP_IF_LONG;
    private boolean alignDeconstructionPatternsMultiline = true;
    private boolean newLineAfterDeconstructionPatternsLParen = true;
    private boolean placeDeconstructionPatternsRParenOnNewLine = true;

    // 4. Blank Lines
    private int keepBlankLinesInDeclarations = 2;
    private int keepBlankLinesInCode = 2;
    private int keepBlankLinesBeforeRBrace = 2;
    private int keepBlankLinesBetweenHeaderAndPackage = 2;
    private int blankLinesBeforePackage = 0;
    private int blankLinesAfterPackage = 1;
    private int blankLinesBeforeImports = 1;
    private int blankLinesAfterImports = 1;
    private int blankLinesAroundClass = 1;
    private int blankLinesAfterClassHeader = 0;
    private int blankLinesBeforeClassEnd = 0;
    private int blankLinesAfterAnonymousClassHeader = 0;
    private int blankLinesBeforeFieldInInterface = 0;
    private int blankLinesBeforeFieldWithoutAnnotations = 0;
    private int blankLinesBeforeFieldWithAnnotations = 0;
    private int blankLinesAroundMethodInInterface = 1;
    private int blankLinesAroundMethod = 1;
    private int blankLinesBeforeMethodBody = 0;
    private int blankLinesAroundInitializer = 1;
    private int blankLinesBetweenRecordComponents = 0;

    // 5. JavaDoc
    private boolean enableJavaDocFormatting = true;
    private boolean alignParamDescriptions = true;
    private boolean alignThrownExceptions = true;
    private boolean formatComments = true;
    private boolean blankLinesAfterDescription = true;
    private boolean blankLinesAfterParamDescriptions = false;
    private boolean blankLinesAfterReturnTag = false;
    private boolean keepInvalidTags = true;
    private boolean keepEmptyParamTags = true;
    private boolean keepEmptyReturnTags = true;
    private boolean keepEmptyThrowsTags = true;
    private boolean wrapAtRightMargin = false;
    private boolean enableLeadingAsterisks = true;
    private boolean useThrowsRatherThanException = true;
    private boolean generatePOnEmptyLines = true;
    private boolean keepEmptyLines = true;
    private boolean doNotWrapOneLineComments = false;
    private boolean preserveLineFeeds = false;
    private boolean paramDescriptionsOnNewLine = false;
    private boolean indentContinuationLines = false;

    // 6. Imports
    private boolean useSingleClassImport = true;
    private boolean useFullyQualifiedClassNames = false;
    private boolean insertInnerClassImports = false;
    private List<String> excludedInnerClasses = new ArrayList<>();
    private boolean doNotSeparateModuleImports = true;
    private boolean deleteUnusedModuleImports = false;
    private String useFqNamesInJavadoc = "If not already imported";
    private int classCountToUseImportOnDemand = 5;
    private int namesCountToUseStaticImportOnDemand = 3;
    private List<ImportEntry> packagesToUseImportOnDemand = new ArrayList<>(List.of(
            new ImportEntry(false, "java.awt.*", false),
            new ImportEntry(false, "javax.swing.*", false)
    ));
    private boolean placeOnDemandImportBeforeSingleClassImports = true;
    private boolean layoutStaticImportsSeparately = true;
    private List<ImportLayoutEntry> importLayout = new ArrayList<>(List.of(
            ImportLayoutEntry.moduleImports(),
            ImportLayoutEntry.allOtherImports(),
            ImportLayoutEntry.blankLine(),
            ImportLayoutEntry.packageEntry(false, "javax.*", true),
            ImportLayoutEntry.packageEntry(false, "java.*", true),
            ImportLayoutEntry.blankLine(),
            ImportLayoutEntry.staticAllOtherImports()
    ));

    // 7. Arrangement
    private boolean keepGettersAndSettersTogether = true;
    private boolean keepOverriddenMethodsTogether = false;
    private String overriddenMethodsOrder = "keep order";
    private boolean keepDependentMethodsTogether = false;
    private String dependentMethodsOrder = "breadth-first order";
    private List<ArrangementRule> matchingRules = new ArrayList<>(ArrangementRule.getDefaultArrangementRules());

    // 8. Code Generation
    private boolean preferLongerNames = true;
    private String fieldPrefix = "";
    private String fieldSuffix = "";
    private String staticFieldPrefix = "";
    private String staticFieldSuffix = "";
    private String parameterPrefix = "";
    private String parameterSuffix = "";
    private String localVariablePrefix = "";
    private String localVariableSuffix = "";
    private String subclassPrefix = "";
    private String subclassSuffix = "Impl";
    private String testClassPrefix = "";
    private String testClassSuffix = "Test";
    private String defaultVisibility = "Public";
    private boolean makeGeneratedLocalsFinal = false;
    private boolean makeGeneratedParametersFinal = false;
    private boolean useVarForLocalVariables = false;
    private boolean lineCommentAtFirstColumn = true;
    private boolean addSpaceAtLineCommentStart = false;
    private boolean enforceOnReformat = false;
    private boolean blockCommentAtFirstColumn = true;
    private boolean addSpacesAroundBlockComments = false;
    private boolean insertOverride = true;
    private boolean repeatSynchronized = false;
    private List<String> annotationsToCopy = new ArrayList<>();
    private boolean useExternalAnnotations = false;
    private boolean insertTypeUseAnnotationsBeforeType = true;
    private boolean useClassIsInstanceAndCast = false;
    private boolean replaceNullCheckWithObjectsNonNull = true;
    private boolean useIntegerSumWhenPossible = true;

    // 9. Java EE Names
    // Entity Bean
    private String entityEjbClassPrefix = "";
    private String entityEjbClassSuffix = "Bean";
    private String entityHomeInterfacePrefix = "";
    private String entityHomeInterfaceSuffix = "Home";
    private String entityRemoteInterfacePrefix = "";
    private String entityRemoteInterfaceSuffix = "";
    private String entityLocalHomeInterfacePrefix = "Local";
    private String entityLocalHomeInterfaceSuffix = "Home";
    private String entityLocalInterfacePrefix = "Local";
    private String entityLocalInterfaceSuffix = "";
    private String entityEjbNameTagPrefix = "";
    private String entityEjbNameTagSuffix = "EJB";
    private String entityTransferObjectPrefix = "";
    private String entityTransferObjectSuffix = "VO";
    private String entityDefaultPkClass = "java.lang.String";
    // Session Bean
    private String sessionEjbClassPrefix = "";
    private String sessionEjbClassSuffix = "Bean";
    private String sessionHomeInterfacePrefix = "";
    private String sessionHomeInterfaceSuffix = "Home";
    private String sessionRemoteInterfacePrefix = "";
    private String sessionRemoteInterfaceSuffix = "";
    private String sessionLocalHomeInterfacePrefix = "Local";
    private String sessionLocalHomeInterfaceSuffix = "Home";
    private String sessionLocalInterfacePrefix = "Local";
    private String sessionLocalInterfaceSuffix = "";
    private String sessionServiceEndpointPrefix = "";
    private String sessionServiceEndpointSuffix = "Service";
    private String sessionEjbNameTagPrefix = "";
    private String sessionEjbNameTagSuffix = "EJB";
    // Servlet
    private String servletClassPrefix = "";
    private String servletClassSuffix = "";
    private String servletNameTagPrefix = "";
    private String servletNameTagSuffix = "";
    // Message Driven Bean
    private String mdbEjbClassPrefix = "";
    private String mdbEjbClassSuffix = "Bean";
    private String mdbEjbNameTagPrefix = "";
    private String mdbEjbNameTagSuffix = "EJB";
    // Filter
    private String filterClassPrefix = "";
    private String filterClassSuffix = "";
    private String filterNameTagPrefix = "";
    private String filterNameTagSuffix = "";
    // Listener
    private String listenerClassPrefix = "";
    private String listenerClassSuffix = "";
    // Legacy compatibility fields
    private String entityBeanSuffix = "Entity";
    private String sessionBeanSuffix = "Bean";
    private String messageDrivenBeanSuffix = "MDB";
    private String daoSuffix = "DAO";
    private String serviceSuffix = "Service";
    private String serviceImplSuffix = "ServiceImpl";

    public JavaCodeStyleSettings() {
    }

    /**
     * Copies all state from an existing LanguageCodeStyleSettings instance into this Java-specific model.
     */
    public void syncFrom(LanguageCodeStyleSettings lcs) {
        if (lcs == null) return;
        this.useTabCharacter = lcs.isUseTabCharacter();
        this.smartTabs = lcs.isSmartTabs();
        this.tabSize = lcs.getTabSize();
        this.indent = lcs.getIndent();
        this.continuationIndent = lcs.getContinuationIndent();
        this.keepIndentsOnEmptyLines = lcs.isKeepIndentsOnEmptyLines();
        this.labelIndent = lcs.getLabelIndent();
        this.absoluteLabelIndent = lcs.isAbsoluteLabelIndent();
        this.doNotIndentTopLevelMembers = lcs.isDoNotIndentTopLevelMembers();
        this.useIndentsRelativeToExpressionStart = lcs.isUseIndentsRelativeToExpressionStart();
    }

    /**
     * Applies indent and tab properties back to LanguageCodeStyleSettings for serialization.
     */
    public void syncTo(LanguageCodeStyleSettings lcs) {
        if (lcs == null) return;
        lcs.setUseTabCharacter(this.useTabCharacter);
        lcs.setSmartTabs(this.smartTabs);
        lcs.setTabSize(this.tabSize);
        lcs.setIndent(this.indent);
        lcs.setContinuationIndent(this.continuationIndent);
        lcs.setKeepIndentsOnEmptyLines(this.keepIndentsOnEmptyLines);
        lcs.setLabelIndent(this.labelIndent);
        lcs.setAbsoluteLabelIndent(this.absoluteLabelIndent);
        lcs.setDoNotIndentTopLevelMembers(this.doNotIndentTopLevelMembers);
        lcs.setUseIndentsRelativeToExpressionStart(this.useIndentsRelativeToExpressionStart);
    }

    public JavaCodeStyleSettings copy() {
        JavaCodeStyleSettings c = new JavaCodeStyleSettings();
        c.useTabCharacter = this.useTabCharacter;
        c.smartTabs = this.smartTabs;
        c.tabSize = this.tabSize;
        c.indent = this.indent;
        c.continuationIndent = this.continuationIndent;
        c.keepIndentsOnEmptyLines = this.keepIndentsOnEmptyLines;
        c.labelIndent = this.labelIndent;
        c.absoluteLabelIndent = this.absoluteLabelIndent;
        c.doNotIndentTopLevelMembers = this.doNotIndentTopLevelMembers;
        c.useIndentsRelativeToExpressionStart = this.useIndentsRelativeToExpressionStart;

        c.spaceBeforeMethodDeclParen = this.spaceBeforeMethodDeclParen;
        c.spaceBeforeMethodCallParen = this.spaceBeforeMethodCallParen;
        c.spaceBeforeIfParen = this.spaceBeforeIfParen;
        c.spaceBeforeForParen = this.spaceBeforeForParen;
        c.spaceBeforeWhileParen = this.spaceBeforeWhileParen;
        c.spaceBeforeSwitchParen = this.spaceBeforeSwitchParen;
        c.spaceBeforeTryParen = this.spaceBeforeTryParen;
        c.spaceBeforeCatchParen = this.spaceBeforeCatchParen;
        c.spaceBeforeSynchronizedParen = this.spaceBeforeSynchronizedParen;
        c.spaceBeforeAnnotationParens = this.spaceBeforeAnnotationParens;
        c.spaceBeforeDeconstructionList = this.spaceBeforeDeconstructionList;

        c.spaceAroundAssignmentOps = this.spaceAroundAssignmentOps;
        c.spaceAroundLogicalOps = this.spaceAroundLogicalOps;
        c.spaceAroundEqualityOps = this.spaceAroundEqualityOps;
        c.spaceAroundRelationalOps = this.spaceAroundRelationalOps;
        c.spaceAroundBitwiseOps = this.spaceAroundBitwiseOps;
        c.spaceAroundAdditiveOps = this.spaceAroundAdditiveOps;
        c.spaceAroundMultiplicativeOps = this.spaceAroundMultiplicativeOps;
        c.spaceAroundShiftOps = this.spaceAroundShiftOps;
        c.spaceAroundUnaryOps = this.spaceAroundUnaryOps;
        c.spaceAroundLambdaArrow = this.spaceAroundLambdaArrow;
        c.spaceAroundMethodRefDoubleColon = this.spaceAroundMethodRefDoubleColon;
        c.spaceBeforeClassLeftBrace = this.spaceBeforeClassLeftBrace;
        c.spaceBeforeMethodLeftBrace = this.spaceBeforeMethodLeftBrace;
        c.spaceBeforeIfLeftBrace = this.spaceBeforeIfLeftBrace;
        c.spaceBeforeElseLeftBrace = this.spaceBeforeElseLeftBrace;
        c.spaceBeforeWhileLeftBrace = this.spaceBeforeWhileLeftBrace;
        c.spaceBeforeForLeftBrace = this.spaceBeforeForLeftBrace;
        c.spaceBeforeDoLeftBrace = this.spaceBeforeDoLeftBrace;
        c.spaceBeforeSwitchLeftBrace = this.spaceBeforeSwitchLeftBrace;
        c.spaceBeforeTryLeftBrace = this.spaceBeforeTryLeftBrace;
        c.spaceBeforeCatchLeftBrace = this.spaceBeforeCatchLeftBrace;
        c.spaceBeforeFinallyLeftBrace = this.spaceBeforeFinallyLeftBrace;
        c.spaceBeforeElse = this.spaceBeforeElse;
        c.spaceBeforeWhile = this.spaceBeforeWhile;
        c.spaceBeforeCatch = this.spaceBeforeCatch;
        c.spaceBeforeFinally = this.spaceBeforeFinally;
        c.spaceAfterComma = this.spaceAfterComma;
        c.spaceBeforeComma = this.spaceBeforeComma;
        c.spaceAfterSemicolon = this.spaceAfterSemicolon;
        c.spaceBeforeSemicolon = this.spaceBeforeSemicolon;
        c.spaceAfterColon = this.spaceAfterColon;
        c.spaceBeforeColon = this.spaceBeforeColon;

        c.withinCodeBraces = this.withinCodeBraces;
        c.withinBrackets = this.withinBrackets;
        c.withinArrayInitializerBraces = this.withinArrayInitializerBraces;
        c.withinEmptyArrayInitializerBraces = this.withinEmptyArrayInitializerBraces;
        c.withinGroupingParens = this.withinGroupingParens;
        c.withinMethodDeclParens = this.withinMethodDeclParens;
        c.withinEmptyMethodDeclParens = this.withinEmptyMethodDeclParens;
        c.withinMethodCallParens = this.withinMethodCallParens;
        c.withinEmptyMethodCallParens = this.withinEmptyMethodCallParens;
        c.withinIfParens = this.withinIfParens;
        c.withinForParens = this.withinForParens;
        c.withinWhileParens = this.withinWhileParens;
        c.withinSwitchParens = this.withinSwitchParens;
        c.withinTryParens = this.withinTryParens;
        c.withinCatchParens = this.withinCatchParens;
        c.withinSynchronizedParens = this.withinSynchronizedParens;
        c.withinTypeCastParens = this.withinTypeCastParens;
        c.withinAnnotationParens = this.withinAnnotationParens;
        c.withinAngleBrackets = this.withinAngleBrackets;
        c.withinRecordHeader = this.withinRecordHeader;
        c.withinDeconstructionList = this.withinDeconstructionList;
        c.withinBlockBracesWhenBodyPresent = this.withinBlockBracesWhenBodyPresent;

        c.ternaryBeforeQuestion = this.ternaryBeforeQuestion;
        c.ternaryAfterQuestion = this.ternaryAfterQuestion;
        c.ternaryBeforeColon = this.ternaryBeforeColon;
        c.ternaryAfterColon = this.ternaryAfterColon;

        c.typeArgsAfterComma = this.typeArgsAfterComma;
        c.typeArgsBeforeOpenAngle = this.typeArgsBeforeOpenAngle;
        c.typeArgsAfterCloseAngle = this.typeArgsAfterCloseAngle;

        c.spaceBeforeForSemicolon = this.spaceBeforeForSemicolon;
        c.spaceAfterForSemicolon = this.spaceAfterForSemicolon;
        c.spaceAfterTypeCast = this.spaceAfterTypeCast;
        c.spaceAroundEqualsInAnnotation = this.spaceAroundEqualsInAnnotation;
        c.spaceBeforeColonInForEach = this.spaceBeforeColonInForEach;
        c.spaceInsideOneLineEnumBraces = this.spaceInsideOneLineEnumBraces;

        c.typeParamsBeforeOpenAngle = this.typeParamsBeforeOpenAngle;
        c.typeParamsAroundTypeBounds = this.typeParamsAroundTypeBounds;

        c.hardWrapAt = this.hardWrapAt;
        c.wrapOnTyping = this.wrapOnTyping;
        c.visualGuides = this.visualGuides;
        c.keepLineBreaks = this.keepLineBreaks;
        c.commentAtFirstColumn = this.commentAtFirstColumn;
        c.controlStatementInOneLine = this.controlStatementInOneLine;
        c.multipleExpressionsInOneLine = this.multipleExpressionsInOneLine;
        c.keepSimpleBlocksInOneLine = this.keepSimpleBlocksInOneLine;
        c.keepSimpleMethodsInOneLine = this.keepSimpleMethodsInOneLine;
        c.keepSimpleLambdasInOneLine = this.keepSimpleLambdasInOneLine;
        c.keepSimpleClassesInOneLine = this.keepSimpleClassesInOneLine;
        c.ensureRightMargin = this.ensureRightMargin;
        c.classBracePlacement = this.classBracePlacement;
        c.methodBracePlacement = this.methodBracePlacement;
        c.lambdaBracePlacement = this.lambdaBracePlacement;
        c.otherBracePlacement = this.otherBracePlacement;
        c.extendsImplementsWrap = this.extendsImplementsWrap;
        c.alignExtendsImplementsMultiline = this.alignExtendsImplementsMultiline;
        c.extendsImplementsKeywordWrap = this.extendsImplementsKeywordWrap;
        c.throwsWrap = this.throwsWrap;
        c.alignThrowsMultiline = this.alignThrowsMultiline;
        c.alignThrowsToMethodStart = this.alignThrowsToMethodStart;
        c.throwsKeywordWrap = this.throwsKeywordWrap;
        c.methodDeclParametersWrap = this.methodDeclParametersWrap;
        c.alignMethodDeclParameters = this.alignMethodDeclParameters;
        c.newLineAfterMethodDeclLParen = this.newLineAfterMethodDeclLParen;
        c.placeMethodDeclRParenOnNewLine = this.placeMethodDeclRParenOnNewLine;
        c.methodCallArgumentsWrap = this.methodCallArgumentsWrap;
        c.alignMethodCallArguments = this.alignMethodCallArguments;
        c.takePriorityOverCallChainWrapping = this.takePriorityOverCallChainWrapping;
        c.newLineAfterMethodCallLParen = this.newLineAfterMethodCallLParen;
        c.placeMethodCallRParenOnNewLine = this.placeMethodCallRParenOnNewLine;

        c.alignMethodParenthesesMultiline = this.alignMethodParenthesesMultiline;
        c.newLineWhenMethodBodyPresented = this.newLineWhenMethodBodyPresented;

        c.chainedMethodCallsWrap = this.chainedMethodCallsWrap;
        c.wrapFirstCall = this.wrapFirstCall;
        c.alignChainedCallsMultiline = this.alignChainedCallsMultiline;
        c.builderMethods = this.builderMethods;
        c.keepBuilderMethodsIndents = this.keepBuilderMethodsIndents;
        c.moveSemicolonToNewLine = this.moveSemicolonToNewLine;

        c.ifForceBraces = this.ifForceBraces;
        c.elseOnNewLine = this.elseOnNewLine;
        c.specialElseIfTreatment = this.specialElseIfTreatment;

        c.forStatementWrap = this.forStatementWrap;
        c.alignForMultiline = this.alignForMultiline;
        c.newLineAfterForLParen = this.newLineAfterForLParen;
        c.placeForRParenOnNewLine = this.placeForRParenOnNewLine;
        c.forForceBraces = this.forForceBraces;

        c.whileForceBraces = this.whileForceBraces;

        c.doWhileForceBraces = this.doWhileForceBraces;
        c.whileOnNewLine = this.whileOnNewLine;

        c.switchWrap = this.switchWrap;
        c.indentCaseBranches = this.indentCaseBranches;
        c.eachCaseOnSeparateLine = this.eachCaseOnSeparateLine;

        c.tryWithResourcesWrap = this.tryWithResourcesWrap;
        c.alignTryWithResourcesMultiline = this.alignTryWithResourcesMultiline;
        c.newLineAfterTryWithResourcesLParen = this.newLineAfterTryWithResourcesLParen;
        c.placeTryWithResourcesRParenOnNewLine = this.placeTryWithResourcesRParenOnNewLine;

        c.catchOnNewLine = this.catchOnNewLine;
        c.finallyOnNewLine = this.finallyOnNewLine;
        c.typesInMultiCatchWrap = this.typesInMultiCatchWrap;
        c.alignTypesInMultiCatch = this.alignTypesInMultiCatch;

        c.binaryExpressionsWrap = this.binaryExpressionsWrap;
        c.alignBinaryExpressionsMultiline = this.alignBinaryExpressionsMultiline;
        c.operationSignOnNextLine = this.operationSignOnNextLine;
        c.alignParenthesisedMultiline = this.alignParenthesisedMultiline;
        c.newLineAfterBinaryLParen = this.newLineAfterBinaryLParen;
        c.placeBinaryRParenOnNewLine = this.placeBinaryRParenOnNewLine;

        c.assignmentWrap = this.assignmentWrap;
        c.alignAssignmentMultiline = this.alignAssignmentMultiline;
        c.assignmentSignOnNextLine = this.assignmentSignOnNextLine;

        c.alignFieldsInColumns = this.alignFieldsInColumns;
        c.alignVariablesInColumns = this.alignVariablesInColumns;
        c.alignAssignmentsInColumns = this.alignAssignmentsInColumns;
        c.alignSimpleMethodsInColumns = this.alignSimpleMethodsInColumns;

        c.ternaryWrap = this.ternaryWrap;
        c.alignTernaryMultiline = this.alignTernaryMultiline;
        c.ternarySignsOnNextLine = this.ternarySignsOnNextLine;

        c.arrayInitializerWrap = this.arrayInitializerWrap;
        c.alignArrayInitializerMultiline = this.alignArrayInitializerMultiline;
        c.newLineAfterArrayInitializerLBrace = this.newLineAfterArrayInitializerLBrace;
        c.placeArrayInitializerRBraceOnNewLine = this.placeArrayInitializerRBraceOnNewLine;

        c.wrapAfterModifierList = this.wrapAfterModifierList;

        c.assertStatementWrap = this.assertStatementWrap;
        c.enumConstantsWrap = this.enumConstantsWrap;
        c.classAnnotationsWrap = this.classAnnotationsWrap;
        c.methodAnnotationsWrap = this.methodAnnotationsWrap;
        c.fieldAnnotationsWrap = this.fieldAnnotationsWrap;
        c.doNotWrapAfterSingleFieldAnnotation = this.doNotWrapAfterSingleFieldAnnotation;
        c.parameterAnnotationsWrap = this.parameterAnnotationsWrap;
        c.doNotWrapAfterSingleParameterAnnotation = this.doNotWrapAfterSingleParameterAnnotation;
        c.localVariableAnnotationsWrap = this.localVariableAnnotationsWrap;
        c.enumFieldAnnotationsWrap = this.enumFieldAnnotationsWrap;
        c.annotationParametersWrap = this.annotationParametersWrap;
        c.alignAnnotationParametersMultiline = this.alignAnnotationParametersMultiline;
        c.newLineAfterAnnotationParametersLParen = this.newLineAfterAnnotationParametersLParen;
        c.placeAnnotationParametersRParenOnNewLine = this.placeAnnotationParametersRParenOnNewLine;

        c.alignTextBlocksMultiline = this.alignTextBlocksMultiline;

        c.recordComponentsWrap = this.recordComponentsWrap;
        c.alignRecordComponentsMultiline = this.alignRecordComponentsMultiline;
        c.newLineAfterRecordComponentsLParen = this.newLineAfterRecordComponentsLParen;
        c.placeRecordComponentsRParenOnNewLine = this.placeRecordComponentsRParenOnNewLine;
        c.newLineForRecordComponentAnnotations = this.newLineForRecordComponentAnnotations;

        c.deconstructionPatternsWrap = this.deconstructionPatternsWrap;
        c.alignDeconstructionPatternsMultiline = this.alignDeconstructionPatternsMultiline;
        c.newLineAfterDeconstructionPatternsLParen = this.newLineAfterDeconstructionPatternsLParen;
        c.placeDeconstructionPatternsRParenOnNewLine = this.placeDeconstructionPatternsRParenOnNewLine;

        c.keepBlankLinesInDeclarations = this.keepBlankLinesInDeclarations;
        c.keepBlankLinesInCode = this.keepBlankLinesInCode;
        c.keepBlankLinesBeforeRBrace = this.keepBlankLinesBeforeRBrace;
        c.keepBlankLinesBetweenHeaderAndPackage = this.keepBlankLinesBetweenHeaderAndPackage;
        c.blankLinesBeforePackage = this.blankLinesBeforePackage;
        c.blankLinesAfterPackage = this.blankLinesAfterPackage;
        c.blankLinesBeforeImports = this.blankLinesBeforeImports;
        c.blankLinesAfterImports = this.blankLinesAfterImports;
        c.blankLinesAroundClass = this.blankLinesAroundClass;
        c.blankLinesAfterClassHeader = this.blankLinesAfterClassHeader;
        c.blankLinesBeforeClassEnd = this.blankLinesBeforeClassEnd;
        c.blankLinesAfterAnonymousClassHeader = this.blankLinesAfterAnonymousClassHeader;
        c.blankLinesBeforeFieldInInterface = this.blankLinesBeforeFieldInInterface;
        c.blankLinesBeforeFieldWithoutAnnotations = this.blankLinesBeforeFieldWithoutAnnotations;
        c.blankLinesBeforeFieldWithAnnotations = this.blankLinesBeforeFieldWithAnnotations;
        c.blankLinesAroundMethodInInterface = this.blankLinesAroundMethodInInterface;
        c.blankLinesAroundMethod = this.blankLinesAroundMethod;
        c.blankLinesBeforeMethodBody = this.blankLinesBeforeMethodBody;
        c.blankLinesAroundInitializer = this.blankLinesAroundInitializer;
        c.blankLinesBetweenRecordComponents = this.blankLinesBetweenRecordComponents;

        c.alignParamDescriptions = this.alignParamDescriptions;
        c.alignThrownExceptions = this.alignThrownExceptions;
        c.enableJavaDocFormatting = this.enableJavaDocFormatting;
        c.alignParamDescriptions = this.alignParamDescriptions;
        c.alignThrownExceptions = this.alignThrownExceptions;
        c.formatComments = this.formatComments;
        c.blankLinesAfterDescription = this.blankLinesAfterDescription;
        c.blankLinesAfterParamDescriptions = this.blankLinesAfterParamDescriptions;
        c.blankLinesAfterReturnTag = this.blankLinesAfterReturnTag;
        c.keepInvalidTags = this.keepInvalidTags;
        c.keepEmptyParamTags = this.keepEmptyParamTags;
        c.keepEmptyReturnTags = this.keepEmptyReturnTags;
        c.keepEmptyThrowsTags = this.keepEmptyThrowsTags;
        c.wrapAtRightMargin = this.wrapAtRightMargin;
        c.enableLeadingAsterisks = this.enableLeadingAsterisks;
        c.useThrowsRatherThanException = this.useThrowsRatherThanException;
        c.generatePOnEmptyLines = this.generatePOnEmptyLines;
        c.keepEmptyLines = this.keepEmptyLines;
        c.doNotWrapOneLineComments = this.doNotWrapOneLineComments;
        c.preserveLineFeeds = this.preserveLineFeeds;
        c.paramDescriptionsOnNewLine = this.paramDescriptionsOnNewLine;
        c.indentContinuationLines = this.indentContinuationLines;

        c.useSingleClassImport = this.useSingleClassImport;
        c.useFullyQualifiedClassNames = this.useFullyQualifiedClassNames;
        c.insertInnerClassImports = this.insertInnerClassImports;
        c.excludedInnerClasses = new ArrayList<>(this.excludedInnerClasses);
        c.doNotSeparateModuleImports = this.doNotSeparateModuleImports;
        c.deleteUnusedModuleImports = this.deleteUnusedModuleImports;
        c.useFqNamesInJavadoc = this.useFqNamesInJavadoc;
        c.classCountToUseImportOnDemand = this.classCountToUseImportOnDemand;
        c.namesCountToUseStaticImportOnDemand = this.namesCountToUseStaticImportOnDemand;
        c.packagesToUseImportOnDemand = this.packagesToUseImportOnDemand != null ?
                this.packagesToUseImportOnDemand.stream().map(ImportEntry::copy).collect(Collectors.toList()) : new ArrayList<>();
        c.placeOnDemandImportBeforeSingleClassImports = this.placeOnDemandImportBeforeSingleClassImports;
        c.layoutStaticImportsSeparately = this.layoutStaticImportsSeparately;
        c.importLayout = this.importLayout != null ?
                this.importLayout.stream().map(ImportLayoutEntry::copy).collect(Collectors.toList()) : new ArrayList<>();

        c.keepGettersAndSettersTogether = this.keepGettersAndSettersTogether;
        c.keepOverriddenMethodsTogether = this.keepOverriddenMethodsTogether;
        c.overriddenMethodsOrder = this.overriddenMethodsOrder;
        c.keepDependentMethodsTogether = this.keepDependentMethodsTogether;
        c.dependentMethodsOrder = this.dependentMethodsOrder;
        c.matchingRules = this.matchingRules != null ?
                this.matchingRules.stream().map(ArrangementRule::copy).collect(Collectors.toList()) : new ArrayList<>();

        c.preferLongerNames = this.preferLongerNames;
        c.fieldPrefix = this.fieldPrefix;
        c.fieldSuffix = this.fieldSuffix;
        c.staticFieldPrefix = this.staticFieldPrefix;
        c.staticFieldSuffix = this.staticFieldSuffix;
        c.parameterPrefix = this.parameterPrefix;
        c.parameterSuffix = this.parameterSuffix;
        c.localVariablePrefix = this.localVariablePrefix;
        c.localVariableSuffix = this.localVariableSuffix;
        c.subclassPrefix = this.subclassPrefix;
        c.subclassSuffix = this.subclassSuffix;
        c.testClassPrefix = this.testClassPrefix;
        c.testClassSuffix = this.testClassSuffix;
        c.defaultVisibility = this.defaultVisibility;
        c.makeGeneratedLocalsFinal = this.makeGeneratedLocalsFinal;
        c.makeGeneratedParametersFinal = this.makeGeneratedParametersFinal;
        c.useVarForLocalVariables = this.useVarForLocalVariables;
        c.lineCommentAtFirstColumn = this.lineCommentAtFirstColumn;
        c.addSpaceAtLineCommentStart = this.addSpaceAtLineCommentStart;
        c.enforceOnReformat = this.enforceOnReformat;
        c.blockCommentAtFirstColumn = this.blockCommentAtFirstColumn;
        c.addSpacesAroundBlockComments = this.addSpacesAroundBlockComments;
        c.insertOverride = this.insertOverride;
        c.repeatSynchronized = this.repeatSynchronized;
        c.annotationsToCopy = new ArrayList<>(this.annotationsToCopy);
        c.useExternalAnnotations = this.useExternalAnnotations;
        c.insertTypeUseAnnotationsBeforeType = this.insertTypeUseAnnotationsBeforeType;
        c.useClassIsInstanceAndCast = this.useClassIsInstanceAndCast;
        c.replaceNullCheckWithObjectsNonNull = this.replaceNullCheckWithObjectsNonNull;
        c.useIntegerSumWhenPossible = this.useIntegerSumWhenPossible;

        c.entityEjbClassPrefix = this.entityEjbClassPrefix;
        c.entityEjbClassSuffix = this.entityEjbClassSuffix;
        c.entityHomeInterfacePrefix = this.entityHomeInterfacePrefix;
        c.entityHomeInterfaceSuffix = this.entityHomeInterfaceSuffix;
        c.entityRemoteInterfacePrefix = this.entityRemoteInterfacePrefix;
        c.entityRemoteInterfaceSuffix = this.entityRemoteInterfaceSuffix;
        c.entityLocalHomeInterfacePrefix = this.entityLocalHomeInterfacePrefix;
        c.entityLocalHomeInterfaceSuffix = this.entityLocalHomeInterfaceSuffix;
        c.entityLocalInterfacePrefix = this.entityLocalInterfacePrefix;
        c.entityLocalInterfaceSuffix = this.entityLocalInterfaceSuffix;
        c.entityEjbNameTagPrefix = this.entityEjbNameTagPrefix;
        c.entityEjbNameTagSuffix = this.entityEjbNameTagSuffix;
        c.entityTransferObjectPrefix = this.entityTransferObjectPrefix;
        c.entityTransferObjectSuffix = this.entityTransferObjectSuffix;
        c.entityDefaultPkClass = this.entityDefaultPkClass;

        c.sessionEjbClassPrefix = this.sessionEjbClassPrefix;
        c.sessionEjbClassSuffix = this.sessionEjbClassSuffix;
        c.sessionHomeInterfacePrefix = this.sessionHomeInterfacePrefix;
        c.sessionHomeInterfaceSuffix = this.sessionHomeInterfaceSuffix;
        c.sessionRemoteInterfacePrefix = this.sessionRemoteInterfacePrefix;
        c.sessionRemoteInterfaceSuffix = this.sessionRemoteInterfaceSuffix;
        c.sessionLocalHomeInterfacePrefix = this.sessionLocalHomeInterfacePrefix;
        c.sessionLocalHomeInterfaceSuffix = this.sessionLocalHomeInterfaceSuffix;
        c.sessionLocalInterfacePrefix = this.sessionLocalInterfacePrefix;
        c.sessionLocalInterfaceSuffix = this.sessionLocalInterfaceSuffix;
        c.sessionServiceEndpointPrefix = this.sessionServiceEndpointPrefix;
        c.sessionServiceEndpointSuffix = this.sessionServiceEndpointSuffix;
        c.sessionEjbNameTagPrefix = this.sessionEjbNameTagPrefix;
        c.sessionEjbNameTagSuffix = this.sessionEjbNameTagSuffix;

        c.servletClassPrefix = this.servletClassPrefix;
        c.servletClassSuffix = this.servletClassSuffix;
        c.servletNameTagPrefix = this.servletNameTagPrefix;
        c.servletNameTagSuffix = this.servletNameTagSuffix;

        c.mdbEjbClassPrefix = this.mdbEjbClassPrefix;
        c.mdbEjbClassSuffix = this.mdbEjbClassSuffix;
        c.mdbEjbNameTagPrefix = this.mdbEjbNameTagPrefix;
        c.mdbEjbNameTagSuffix = this.mdbEjbNameTagSuffix;

        c.filterClassPrefix = this.filterClassPrefix;
        c.filterClassSuffix = this.filterClassSuffix;
        c.filterNameTagPrefix = this.filterNameTagPrefix;
        c.filterNameTagSuffix = this.filterNameTagSuffix;

        c.listenerClassPrefix = this.listenerClassPrefix;
        c.listenerClassSuffix = this.listenerClassSuffix;

        c.entityBeanSuffix = this.entityBeanSuffix;
        c.sessionBeanSuffix = this.sessionBeanSuffix;
        c.messageDrivenBeanSuffix = this.messageDrivenBeanSuffix;
        c.daoSuffix = this.daoSuffix;
        c.serviceSuffix = this.serviceSuffix;
        c.serviceImplSuffix = this.serviceImplSuffix;

        return c;
    }

    public void applyGoogleStyle() {
        this.indent = 2;
        this.tabSize = 2;
        this.continuationIndent = 4;
        this.useTabCharacter = false;
        this.hardWrapAt = 100;
        this.classCountToUseImportOnDemand = 999;
        this.namesCountToUseStaticImportOnDemand = 999;
        this.keepSimpleBlocksInOneLine = false;
        this.keepSimpleMethodsInOneLine = false;
    }

    public void applyPlatformDefault() {
        this.indent = 4;
        this.tabSize = 4;
        this.continuationIndent = 8;
        this.useTabCharacter = false;
        this.hardWrapAt = 120;
        this.classCountToUseImportOnDemand = 5;
        this.namesCountToUseStaticImportOnDemand = 3;
        this.classBracePlacement = BracePlacement.END_OF_LINE;
        this.methodBracePlacement = BracePlacement.END_OF_LINE;
        this.otherBracePlacement = BracePlacement.END_OF_LINE;
    }

    // Getters and Setters
    public boolean isUseTabCharacter() { return useTabCharacter; }
    public void setUseTabCharacter(boolean useTabCharacter) { this.useTabCharacter = useTabCharacter; }

    public boolean isSmartTabs() { return smartTabs; }
    public void setSmartTabs(boolean smartTabs) { this.smartTabs = smartTabs; }

    public int getTabSize() { return tabSize; }
    public void setTabSize(int tabSize) { this.tabSize = Math.max(1, tabSize); }

    public int getIndent() { return indent; }
    public void setIndent(int indent) { this.indent = Math.max(1, indent); }

    public int getContinuationIndent() { return continuationIndent; }
    public void setContinuationIndent(int continuationIndent) { this.continuationIndent = Math.max(0, continuationIndent); }

    public boolean isKeepIndentsOnEmptyLines() { return keepIndentsOnEmptyLines; }
    public void setKeepIndentsOnEmptyLines(boolean keepIndentsOnEmptyLines) { this.keepIndentsOnEmptyLines = keepIndentsOnEmptyLines; }

    public int getLabelIndent() { return labelIndent; }
    public void setLabelIndent(int labelIndent) { this.labelIndent = labelIndent; }

    public boolean isAbsoluteLabelIndent() { return absoluteLabelIndent; }
    public void setAbsoluteLabelIndent(boolean absoluteLabelIndent) { this.absoluteLabelIndent = absoluteLabelIndent; }

    public boolean isDoNotIndentTopLevelMembers() { return doNotIndentTopLevelMembers; }
    public void setDoNotIndentTopLevelMembers(boolean doNotIndentTopLevelMembers) { this.doNotIndentTopLevelMembers = doNotIndentTopLevelMembers; }

    public boolean isUseIndentsRelativeToExpressionStart() { return useIndentsRelativeToExpressionStart; }
    public void setUseIndentsRelativeToExpressionStart(boolean useIndentsRelativeToExpressionStart) { this.useIndentsRelativeToExpressionStart = useIndentsRelativeToExpressionStart; }

    public boolean isSpaceBeforeMethodCallParen() { return spaceBeforeMethodCallParen; }
    public void setSpaceBeforeMethodCallParen(boolean spaceBeforeMethodCallParen) { this.spaceBeforeMethodCallParen = spaceBeforeMethodCallParen; }

    public boolean isSpaceBeforeMethodDeclParen() { return spaceBeforeMethodDeclParen; }
    public void setSpaceBeforeMethodDeclParen(boolean spaceBeforeMethodDeclParen) { this.spaceBeforeMethodDeclParen = spaceBeforeMethodDeclParen; }

    public boolean isSpaceBeforeIfParen() { return spaceBeforeIfParen; }
    public void setSpaceBeforeIfParen(boolean spaceBeforeIfParen) { this.spaceBeforeIfParen = spaceBeforeIfParen; }

    public boolean isSpaceBeforeForParen() { return spaceBeforeForParen; }
    public void setSpaceBeforeForParen(boolean spaceBeforeForParen) { this.spaceBeforeForParen = spaceBeforeForParen; }

    public boolean isSpaceBeforeWhileParen() { return spaceBeforeWhileParen; }
    public void setSpaceBeforeWhileParen(boolean spaceBeforeWhileParen) { this.spaceBeforeWhileParen = spaceBeforeWhileParen; }

    public boolean isSpaceBeforeSwitchParen() { return spaceBeforeSwitchParen; }
    public void setSpaceBeforeSwitchParen(boolean spaceBeforeSwitchParen) { this.spaceBeforeSwitchParen = spaceBeforeSwitchParen; }

    public boolean isSpaceBeforeTryParen() { return spaceBeforeTryParen; }
    public void setSpaceBeforeTryParen(boolean spaceBeforeTryParen) { this.spaceBeforeTryParen = spaceBeforeTryParen; }

    public boolean isSpaceBeforeCatchParen() { return spaceBeforeCatchParen; }
    public void setSpaceBeforeCatchParen(boolean spaceBeforeCatchParen) { this.spaceBeforeCatchParen = spaceBeforeCatchParen; }

    public boolean isSpaceBeforeSynchronizedParen() { return spaceBeforeSynchronizedParen; }
    public void setSpaceBeforeSynchronizedParen(boolean spaceBeforeSynchronizedParen) { this.spaceBeforeSynchronizedParen = spaceBeforeSynchronizedParen; }

    public boolean isSpaceBeforeAnnotationParens() { return spaceBeforeAnnotationParens; }
    public void setSpaceBeforeAnnotationParens(boolean spaceBeforeAnnotationParens) { this.spaceBeforeAnnotationParens = spaceBeforeAnnotationParens; }

    public boolean isSpaceBeforeDeconstructionList() { return spaceBeforeDeconstructionList; }
    public void setSpaceBeforeDeconstructionList(boolean spaceBeforeDeconstructionList) { this.spaceBeforeDeconstructionList = spaceBeforeDeconstructionList; }

    public boolean isSpaceAroundAssignmentOps() { return spaceAroundAssignmentOps; }
    public void setSpaceAroundAssignmentOps(boolean spaceAroundAssignmentOps) { this.spaceAroundAssignmentOps = spaceAroundAssignmentOps; }

    public boolean isSpaceAroundLogicalOps() { return spaceAroundLogicalOps; }
    public void setSpaceAroundLogicalOps(boolean spaceAroundLogicalOps) { this.spaceAroundLogicalOps = spaceAroundLogicalOps; }

    public boolean isSpaceAroundEqualityOps() { return spaceAroundEqualityOps; }
    public void setSpaceAroundEqualityOps(boolean spaceAroundEqualityOps) { this.spaceAroundEqualityOps = spaceAroundEqualityOps; }

    public boolean isSpaceAroundRelationalOps() { return spaceAroundRelationalOps; }
    public void setSpaceAroundRelationalOps(boolean spaceAroundRelationalOps) { this.spaceAroundRelationalOps = spaceAroundRelationalOps; }

    public boolean isSpaceAroundBitwiseOps() { return spaceAroundBitwiseOps; }
    public void setSpaceAroundBitwiseOps(boolean spaceAroundBitwiseOps) { this.spaceAroundBitwiseOps = spaceAroundBitwiseOps; }

    public boolean isSpaceAroundAdditiveOps() { return spaceAroundAdditiveOps; }
    public void setSpaceAroundAdditiveOps(boolean spaceAroundAdditiveOps) { this.spaceAroundAdditiveOps = spaceAroundAdditiveOps; }

    public boolean isSpaceAroundMultiplicativeOps() { return spaceAroundMultiplicativeOps; }
    public void setSpaceAroundMultiplicativeOps(boolean spaceAroundMultiplicativeOps) { this.spaceAroundMultiplicativeOps = spaceAroundMultiplicativeOps; }

    public boolean isSpaceAroundShiftOps() { return spaceAroundShiftOps; }
    public void setSpaceAroundShiftOps(boolean spaceAroundShiftOps) { this.spaceAroundShiftOps = spaceAroundShiftOps; }

    public boolean isSpaceAroundUnaryOps() { return spaceAroundUnaryOps; }
    public void setSpaceAroundUnaryOps(boolean spaceAroundUnaryOps) { this.spaceAroundUnaryOps = spaceAroundUnaryOps; }

    public boolean isSpaceAroundLambdaArrow() { return spaceAroundLambdaArrow; }
    public void setSpaceAroundLambdaArrow(boolean spaceAroundLambdaArrow) { this.spaceAroundLambdaArrow = spaceAroundLambdaArrow; }

    public boolean isSpaceAroundMethodRefDoubleColon() { return spaceAroundMethodRefDoubleColon; }
    public void setSpaceAroundMethodRefDoubleColon(boolean spaceAroundMethodRefDoubleColon) { this.spaceAroundMethodRefDoubleColon = spaceAroundMethodRefDoubleColon; }

    public boolean isSpaceBeforeClassLeftBrace() { return spaceBeforeClassLeftBrace; }
    public void setSpaceBeforeClassLeftBrace(boolean spaceBeforeClassLeftBrace) { this.spaceBeforeClassLeftBrace = spaceBeforeClassLeftBrace; }

    public boolean isSpaceBeforeMethodLeftBrace() { return spaceBeforeMethodLeftBrace; }
    public void setSpaceBeforeMethodLeftBrace(boolean spaceBeforeMethodLeftBrace) { this.spaceBeforeMethodLeftBrace = spaceBeforeMethodLeftBrace; }

    public boolean isSpaceBeforeIfLeftBrace() { return spaceBeforeIfLeftBrace; }
    public void setSpaceBeforeIfLeftBrace(boolean spaceBeforeIfLeftBrace) { this.spaceBeforeIfLeftBrace = spaceBeforeIfLeftBrace; }

    public boolean isSpaceBeforeElseLeftBrace() { return spaceBeforeElseLeftBrace; }
    public void setSpaceBeforeElseLeftBrace(boolean spaceBeforeElseLeftBrace) { this.spaceBeforeElseLeftBrace = spaceBeforeElseLeftBrace; }

    public boolean isSpaceBeforeWhileLeftBrace() { return spaceBeforeWhileLeftBrace; }
    public void setSpaceBeforeWhileLeftBrace(boolean spaceBeforeWhileLeftBrace) { this.spaceBeforeWhileLeftBrace = spaceBeforeWhileLeftBrace; }

    public boolean isSpaceBeforeForLeftBrace() { return spaceBeforeForLeftBrace; }
    public void setSpaceBeforeForLeftBrace(boolean spaceBeforeForLeftBrace) { this.spaceBeforeForLeftBrace = spaceBeforeForLeftBrace; }

    public boolean isSpaceBeforeDoLeftBrace() { return spaceBeforeDoLeftBrace; }
    public void setSpaceBeforeDoLeftBrace(boolean spaceBeforeDoLeftBrace) { this.spaceBeforeDoLeftBrace = spaceBeforeDoLeftBrace; }

    public boolean isSpaceBeforeSwitchLeftBrace() { return spaceBeforeSwitchLeftBrace; }
    public void setSpaceBeforeSwitchLeftBrace(boolean spaceBeforeSwitchLeftBrace) { this.spaceBeforeSwitchLeftBrace = spaceBeforeSwitchLeftBrace; }

    public boolean isSpaceBeforeTryLeftBrace() { return spaceBeforeTryLeftBrace; }
    public void setSpaceBeforeTryLeftBrace(boolean spaceBeforeTryLeftBrace) { this.spaceBeforeTryLeftBrace = spaceBeforeTryLeftBrace; }

    public boolean isSpaceBeforeCatchLeftBrace() { return spaceBeforeCatchLeftBrace; }
    public void setSpaceBeforeCatchLeftBrace(boolean spaceBeforeCatchLeftBrace) { this.spaceBeforeCatchLeftBrace = spaceBeforeCatchLeftBrace; }

    public boolean isSpaceBeforeFinallyLeftBrace() { return spaceBeforeFinallyLeftBrace; }
    public void setSpaceBeforeFinallyLeftBrace(boolean spaceBeforeFinallyLeftBrace) { this.spaceBeforeFinallyLeftBrace = spaceBeforeFinallyLeftBrace; }

    public boolean isSpaceBeforeElse() { return spaceBeforeElse; }
    public void setSpaceBeforeElse(boolean spaceBeforeElse) { this.spaceBeforeElse = spaceBeforeElse; }

    public boolean isSpaceBeforeWhile() { return spaceBeforeWhile; }
    public void setSpaceBeforeWhile(boolean spaceBeforeWhile) { this.spaceBeforeWhile = spaceBeforeWhile; }

    public boolean isSpaceBeforeCatch() { return spaceBeforeCatch; }
    public void setSpaceBeforeCatch(boolean spaceBeforeCatch) { this.spaceBeforeCatch = spaceBeforeCatch; }

    public boolean isSpaceBeforeFinally() { return spaceBeforeFinally; }
    public void setSpaceBeforeFinally(boolean spaceBeforeFinally) { this.spaceBeforeFinally = spaceBeforeFinally; }

    public boolean isSpaceAfterComma() { return spaceAfterComma; }
    public void setSpaceAfterComma(boolean spaceAfterComma) { this.spaceAfterComma = spaceAfterComma; }

    public boolean isSpaceBeforeComma() { return spaceBeforeComma; }
    public void setSpaceBeforeComma(boolean spaceBeforeComma) { this.spaceBeforeComma = spaceBeforeComma; }

    public boolean isSpaceAfterSemicolon() { return spaceAfterSemicolon; }
    public void setSpaceAfterSemicolon(boolean spaceAfterSemicolon) { this.spaceAfterSemicolon = spaceAfterSemicolon; }

    public boolean isSpaceBeforeSemicolon() { return spaceBeforeSemicolon; }
    public void setSpaceBeforeSemicolon(boolean spaceBeforeSemicolon) { this.spaceBeforeSemicolon = spaceBeforeSemicolon; }

    public boolean isSpaceAfterColon() { return spaceAfterColon; }
    public void setSpaceAfterColon(boolean spaceAfterColon) { this.spaceAfterColon = spaceAfterColon; }

    public boolean isSpaceBeforeColon() { return spaceBeforeColon; }
    public void setSpaceBeforeColon(boolean spaceBeforeColon) { this.spaceBeforeColon = spaceBeforeColon; }

    public boolean isWithinCodeBraces() { return withinCodeBraces; }
    public void setWithinCodeBraces(boolean v) { this.withinCodeBraces = v; }
    public boolean isWithinBrackets() { return withinBrackets; }
    public void setWithinBrackets(boolean v) { this.withinBrackets = v; }
    public boolean isWithinArrayInitializerBraces() { return withinArrayInitializerBraces; }
    public void setWithinArrayInitializerBraces(boolean v) { this.withinArrayInitializerBraces = v; }
    public boolean isWithinEmptyArrayInitializerBraces() { return withinEmptyArrayInitializerBraces; }
    public void setWithinEmptyArrayInitializerBraces(boolean v) { this.withinEmptyArrayInitializerBraces = v; }
    public boolean isWithinGroupingParens() { return withinGroupingParens; }
    public void setWithinGroupingParens(boolean v) { this.withinGroupingParens = v; }
    public boolean isWithinMethodDeclParens() { return withinMethodDeclParens; }
    public void setWithinMethodDeclParens(boolean v) { this.withinMethodDeclParens = v; }
    public boolean isWithinEmptyMethodDeclParens() { return withinEmptyMethodDeclParens; }
    public void setWithinEmptyMethodDeclParens(boolean v) { this.withinEmptyMethodDeclParens = v; }
    public boolean isWithinMethodCallParens() { return withinMethodCallParens; }
    public void setWithinMethodCallParens(boolean v) { this.withinMethodCallParens = v; }
    public boolean isWithinEmptyMethodCallParens() { return withinEmptyMethodCallParens; }
    public void setWithinEmptyMethodCallParens(boolean v) { this.withinEmptyMethodCallParens = v; }
    public boolean isWithinIfParens() { return withinIfParens; }
    public void setWithinIfParens(boolean v) { this.withinIfParens = v; }
    public boolean isWithinForParens() { return withinForParens; }
    public void setWithinForParens(boolean v) { this.withinForParens = v; }
    public boolean isWithinWhileParens() { return withinWhileParens; }
    public void setWithinWhileParens(boolean v) { this.withinWhileParens = v; }
    public boolean isWithinSwitchParens() { return withinSwitchParens; }
    public void setWithinSwitchParens(boolean v) { this.withinSwitchParens = v; }
    public boolean isWithinTryParens() { return withinTryParens; }
    public void setWithinTryParens(boolean v) { this.withinTryParens = v; }
    public boolean isWithinCatchParens() { return withinCatchParens; }
    public void setWithinCatchParens(boolean v) { this.withinCatchParens = v; }
    public boolean isWithinSynchronizedParens() { return withinSynchronizedParens; }
    public void setWithinSynchronizedParens(boolean v) { this.withinSynchronizedParens = v; }
    public boolean isWithinTypeCastParens() { return withinTypeCastParens; }
    public void setWithinTypeCastParens(boolean v) { this.withinTypeCastParens = v; }
    public boolean isWithinAnnotationParens() { return withinAnnotationParens; }
    public void setWithinAnnotationParens(boolean v) { this.withinAnnotationParens = v; }
    public boolean isWithinAngleBrackets() { return withinAngleBrackets; }
    public void setWithinAngleBrackets(boolean v) { this.withinAngleBrackets = v; }
    public boolean isWithinRecordHeader() { return withinRecordHeader; }
    public void setWithinRecordHeader(boolean v) { this.withinRecordHeader = v; }
    public boolean isWithinDeconstructionList() { return withinDeconstructionList; }
    public void setWithinDeconstructionList(boolean v) { this.withinDeconstructionList = v; }
    public boolean isWithinBlockBracesWhenBodyPresent() { return withinBlockBracesWhenBodyPresent; }
    public void setWithinBlockBracesWhenBodyPresent(boolean v) { this.withinBlockBracesWhenBodyPresent = v; }

    public boolean isTernaryBeforeQuestion() { return ternaryBeforeQuestion; }
    public void setTernaryBeforeQuestion(boolean v) { this.ternaryBeforeQuestion = v; }
    public boolean isTernaryAfterQuestion() { return ternaryAfterQuestion; }
    public void setTernaryAfterQuestion(boolean v) { this.ternaryAfterQuestion = v; }
    public boolean isTernaryBeforeColon() { return ternaryBeforeColon; }
    public void setTernaryBeforeColon(boolean v) { this.ternaryBeforeColon = v; }
    public boolean isTernaryAfterColon() { return ternaryAfterColon; }
    public void setTernaryAfterColon(boolean v) { this.ternaryAfterColon = v; }

    public boolean isTypeArgsAfterComma() { return typeArgsAfterComma; }
    public void setTypeArgsAfterComma(boolean v) { this.typeArgsAfterComma = v; }
    public boolean isTypeArgsBeforeOpenAngle() { return typeArgsBeforeOpenAngle; }
    public void setTypeArgsBeforeOpenAngle(boolean v) { this.typeArgsBeforeOpenAngle = v; }
    public boolean isTypeArgsAfterCloseAngle() { return typeArgsAfterCloseAngle; }
    public void setTypeArgsAfterCloseAngle(boolean v) { this.typeArgsAfterCloseAngle = v; }

    public boolean isSpaceBeforeForSemicolon() { return spaceBeforeForSemicolon; }
    public void setSpaceBeforeForSemicolon(boolean v) { this.spaceBeforeForSemicolon = v; }
    public boolean isSpaceAfterForSemicolon() { return spaceAfterForSemicolon; }
    public void setSpaceAfterForSemicolon(boolean v) { this.spaceAfterForSemicolon = v; }
    public boolean isSpaceAfterTypeCast() { return spaceAfterTypeCast; }
    public void setSpaceAfterTypeCast(boolean v) { this.spaceAfterTypeCast = v; }
    public boolean isSpaceAroundEqualsInAnnotation() { return spaceAroundEqualsInAnnotation; }
    public void setSpaceAroundEqualsInAnnotation(boolean v) { this.spaceAroundEqualsInAnnotation = v; }
    public boolean isSpaceBeforeColonInForEach() { return spaceBeforeColonInForEach; }
    public void setSpaceBeforeColonInForEach(boolean v) { this.spaceBeforeColonInForEach = v; }
    public boolean isSpaceInsideOneLineEnumBraces() { return spaceInsideOneLineEnumBraces; }
    public void setSpaceInsideOneLineEnumBraces(boolean v) { this.spaceInsideOneLineEnumBraces = v; }

    public boolean isTypeParamsBeforeOpenAngle() { return typeParamsBeforeOpenAngle; }
    public void setTypeParamsBeforeOpenAngle(boolean v) { this.typeParamsBeforeOpenAngle = v; }
    public boolean isTypeParamsAroundTypeBounds() { return typeParamsAroundTypeBounds; }
    public void setTypeParamsAroundTypeBounds(boolean v) { this.typeParamsAroundTypeBounds = v; }

    public String getVisualGuides() { return visualGuides; }
    public void setVisualGuides(String v) { this.visualGuides = v; }
    public boolean isCommentAtFirstColumn() { return commentAtFirstColumn; }
    public void setCommentAtFirstColumn(boolean v) { this.commentAtFirstColumn = v; }
    public boolean isControlStatementInOneLine() { return controlStatementInOneLine; }
    public void setControlStatementInOneLine(boolean v) { this.controlStatementInOneLine = v; }
    public boolean isMultipleExpressionsInOneLine() { return multipleExpressionsInOneLine; }
    public void setMultipleExpressionsInOneLine(boolean v) { this.multipleExpressionsInOneLine = v; }
    public BracePlacement getLambdaBracePlacement() { return lambdaBracePlacement; }
    public void setLambdaBracePlacement(BracePlacement v) { this.lambdaBracePlacement = v; }
    public boolean isAlignExtendsImplementsMultiline() { return alignExtendsImplementsMultiline; }
    public void setAlignExtendsImplementsMultiline(boolean v) { this.alignExtendsImplementsMultiline = v; }
    public WrapOption getExtendsImplementsKeywordWrap() { return extendsImplementsKeywordWrap; }
    public void setExtendsImplementsKeywordWrap(WrapOption v) { this.extendsImplementsKeywordWrap = v; }
    public boolean isAlignThrowsMultiline() { return alignThrowsMultiline; }
    public void setAlignThrowsMultiline(boolean v) { this.alignThrowsMultiline = v; }
    public boolean isAlignThrowsToMethodStart() { return alignThrowsToMethodStart; }
    public void setAlignThrowsToMethodStart(boolean v) { this.alignThrowsToMethodStart = v; }
    public WrapOption getThrowsKeywordWrap() { return throwsKeywordWrap; }
    public void setThrowsKeywordWrap(WrapOption v) { this.throwsKeywordWrap = v; }
    public boolean isAlignMethodDeclParameters() { return alignMethodDeclParameters; }
    public void setAlignMethodDeclParameters(boolean v) { this.alignMethodDeclParameters = v; }
    public boolean isNewLineAfterMethodDeclLParen() { return newLineAfterMethodDeclLParen; }
    public void setNewLineAfterMethodDeclLParen(boolean v) { this.newLineAfterMethodDeclLParen = v; }
    public boolean isPlaceMethodDeclRParenOnNewLine() { return placeMethodDeclRParenOnNewLine; }
    public void setPlaceMethodDeclRParenOnNewLine(boolean v) { this.placeMethodDeclRParenOnNewLine = v; }
    public boolean isAlignMethodCallArguments() { return alignMethodCallArguments; }
    public void setAlignMethodCallArguments(boolean v) { this.alignMethodCallArguments = v; }
    public boolean isTakePriorityOverCallChainWrapping() { return takePriorityOverCallChainWrapping; }
    public void setTakePriorityOverCallChainWrapping(boolean v) { this.takePriorityOverCallChainWrapping = v; }
    public boolean isNewLineAfterMethodCallLParen() { return newLineAfterMethodCallLParen; }
    public void setNewLineAfterMethodCallLParen(boolean v) { this.newLineAfterMethodCallLParen = v; }

    public int getHardWrapAt() { return hardWrapAt; }
    public void setHardWrapAt(int hardWrapAt) { this.hardWrapAt = Math.max(20, hardWrapAt); }

    public boolean isWrapOnTyping() { return wrapOnTyping; }
    public void setWrapOnTyping(boolean wrapOnTyping) { this.wrapOnTyping = wrapOnTyping; }

    public boolean isKeepLineBreaks() { return keepLineBreaks; }
    public void setKeepLineBreaks(boolean keepLineBreaks) { this.keepLineBreaks = keepLineBreaks; }

    public boolean isEnsureRightMargin() { return ensureRightMargin; }
    public void setEnsureRightMargin(boolean ensureRightMargin) { this.ensureRightMargin = ensureRightMargin; }

    public boolean isKeepSimpleBlocksInOneLine() { return keepSimpleBlocksInOneLine; }
    public void setKeepSimpleBlocksInOneLine(boolean keepSimpleBlocksInOneLine) { this.keepSimpleBlocksInOneLine = keepSimpleBlocksInOneLine; }

    public boolean isKeepSimpleMethodsInOneLine() { return keepSimpleMethodsInOneLine; }
    public void setKeepSimpleMethodsInOneLine(boolean keepSimpleMethodsInOneLine) { this.keepSimpleMethodsInOneLine = keepSimpleMethodsInOneLine; }

    public boolean isKeepSimpleClassesInOneLine() { return keepSimpleClassesInOneLine; }
    public void setKeepSimpleClassesInOneLine(boolean keepSimpleClassesInOneLine) { this.keepSimpleClassesInOneLine = keepSimpleClassesInOneLine; }

    public boolean isKeepSimpleLambdasInOneLine() { return keepSimpleLambdasInOneLine; }
    public void setKeepSimpleLambdasInOneLine(boolean keepSimpleLambdasInOneLine) { this.keepSimpleLambdasInOneLine = keepSimpleLambdasInOneLine; }

    public BracePlacement getClassBracePlacement() { return classBracePlacement; }
    public void setClassBracePlacement(BracePlacement classBracePlacement) { this.classBracePlacement = classBracePlacement; }

    public BracePlacement getMethodBracePlacement() { return methodBracePlacement; }
    public void setMethodBracePlacement(BracePlacement methodBracePlacement) { this.methodBracePlacement = methodBracePlacement; }

    public BracePlacement getOtherBracePlacement() { return otherBracePlacement; }
    public void setOtherBracePlacement(BracePlacement otherBracePlacement) { this.otherBracePlacement = otherBracePlacement; }

    public WrapOption getMethodDeclParametersWrap() { return methodDeclParametersWrap; }
    public void setMethodDeclParametersWrap(WrapOption methodDeclParametersWrap) { this.methodDeclParametersWrap = methodDeclParametersWrap; }

    public WrapOption getMethodCallArgumentsWrap() { return methodCallArgumentsWrap; }
    public void setMethodCallArgumentsWrap(WrapOption methodCallArgumentsWrap) { this.methodCallArgumentsWrap = methodCallArgumentsWrap; }

    public WrapOption getExtendsImplementsWrap() { return extendsImplementsWrap; }
    public void setExtendsImplementsWrap(WrapOption extendsImplementsWrap) { this.extendsImplementsWrap = extendsImplementsWrap; }

    public WrapOption getThrowsWrap() { return throwsWrap; }
    public void setThrowsWrap(WrapOption throwsWrap) { this.throwsWrap = throwsWrap; }

    public WrapOption getBinaryExpressionsWrap() { return binaryExpressionsWrap; }
    public void setBinaryExpressionsWrap(WrapOption binaryExpressionsWrap) { this.binaryExpressionsWrap = binaryExpressionsWrap; }

    public WrapOption getTernaryExpressionWrap() { return ternaryWrap; }
    public void setTernaryExpressionWrap(WrapOption ternaryExpressionWrap) { this.ternaryWrap = ternaryExpressionWrap; }

    public boolean isPlaceMethodCallRParenOnNewLine() { return placeMethodCallRParenOnNewLine; }
    public void setPlaceMethodCallRParenOnNewLine(boolean v) { this.placeMethodCallRParenOnNewLine = v; }

    public boolean isAlignMethodParenthesesMultiline() { return alignMethodParenthesesMultiline; }
    public void setAlignMethodParenthesesMultiline(boolean v) { this.alignMethodParenthesesMultiline = v; }

    public boolean isNewLineWhenMethodBodyPresented() { return newLineWhenMethodBodyPresented; }
    public void setNewLineWhenMethodBodyPresented(boolean v) { this.newLineWhenMethodBodyPresented = v; }

    public WrapOption getChainedMethodCallsWrap() { return chainedMethodCallsWrap; }
    public void setChainedMethodCallsWrap(WrapOption v) { this.chainedMethodCallsWrap = v; }

    public boolean isWrapFirstCall() { return wrapFirstCall; }
    public void setWrapFirstCall(boolean v) { this.wrapFirstCall = v; }

    public boolean isAlignChainedCallsMultiline() { return alignChainedCallsMultiline; }
    public void setAlignChainedCallsMultiline(boolean v) { this.alignChainedCallsMultiline = v; }

    public String getBuilderMethods() { return builderMethods; }
    public void setBuilderMethods(String v) { this.builderMethods = v != null ? v : "Method names"; }

    public boolean isKeepBuilderMethodsIndents() { return keepBuilderMethodsIndents; }
    public void setKeepBuilderMethodsIndents(boolean v) { this.keepBuilderMethodsIndents = v; }

    public boolean isMoveSemicolonToNewLine() { return moveSemicolonToNewLine; }
    public void setMoveSemicolonToNewLine(boolean v) { this.moveSemicolonToNewLine = v; }

    public String getIfForceBraces() { return ifForceBraces; }
    public void setIfForceBraces(String v) { this.ifForceBraces = v != null ? v : "Do not force"; }

    public boolean isElseOnNewLine() { return elseOnNewLine; }
    public void setElseOnNewLine(boolean v) { this.elseOnNewLine = v; }

    public boolean isSpecialElseIfTreatment() { return specialElseIfTreatment; }
    public void setSpecialElseIfTreatment(boolean v) { this.specialElseIfTreatment = v; }

    public WrapOption getForStatementWrap() { return forStatementWrap; }
    public void setForStatementWrap(WrapOption v) { this.forStatementWrap = v; }

    public boolean isAlignForMultiline() { return alignForMultiline; }
    public void setAlignForMultiline(boolean v) { this.alignForMultiline = v; }

    public boolean isNewLineAfterForLParen() { return newLineAfterForLParen; }
    public void setNewLineAfterForLParen(boolean v) { this.newLineAfterForLParen = v; }

    public boolean isPlaceForRParenOnNewLine() { return placeForRParenOnNewLine; }
    public void setPlaceForRParenOnNewLine(boolean v) { this.placeForRParenOnNewLine = v; }

    public String getForForceBraces() { return forForceBraces; }
    public void setForForceBraces(String v) { this.forForceBraces = v != null ? v : "Do not force"; }

    public String getWhileForceBraces() { return whileForceBraces; }
    public void setWhileForceBraces(String v) { this.whileForceBraces = v != null ? v : "Do not force"; }

    public String getDoWhileForceBraces() { return doWhileForceBraces; }
    public void setDoWhileForceBraces(String v) { this.doWhileForceBraces = v != null ? v : "Do not force"; }

    public boolean isWhileOnNewLine() { return whileOnNewLine; }
    public void setWhileOnNewLine(boolean v) { this.whileOnNewLine = v; }

    public WrapOption getSwitchWrap() { return switchWrap; }
    public void setSwitchWrap(WrapOption v) { this.switchWrap = v; }

    public boolean isIndentCaseBranches() { return indentCaseBranches; }
    public void setIndentCaseBranches(boolean v) { this.indentCaseBranches = v; }

    public boolean isEachCaseOnSeparateLine() { return eachCaseOnSeparateLine; }
    public void setEachCaseOnSeparateLine(boolean v) { this.eachCaseOnSeparateLine = v; }

    public WrapOption getTryWithResourcesWrap() { return tryWithResourcesWrap; }
    public void setTryWithResourcesWrap(WrapOption v) { this.tryWithResourcesWrap = v; }

    public boolean isAlignTryWithResourcesMultiline() { return alignTryWithResourcesMultiline; }
    public void setAlignTryWithResourcesMultiline(boolean v) { this.alignTryWithResourcesMultiline = v; }

    public boolean isNewLineAfterTryWithResourcesLParen() { return newLineAfterTryWithResourcesLParen; }
    public void setNewLineAfterTryWithResourcesLParen(boolean v) { this.newLineAfterTryWithResourcesLParen = v; }

    public boolean isPlaceTryWithResourcesRParenOnNewLine() { return placeTryWithResourcesRParenOnNewLine; }
    public void setPlaceTryWithResourcesRParenOnNewLine(boolean v) { this.placeTryWithResourcesRParenOnNewLine = v; }

    public boolean isCatchOnNewLine() { return catchOnNewLine; }
    public void setCatchOnNewLine(boolean v) { this.catchOnNewLine = v; }

    public boolean isFinallyOnNewLine() { return finallyOnNewLine; }
    public void setFinallyOnNewLine(boolean v) { this.finallyOnNewLine = v; }

    public WrapOption getTypesInMultiCatchWrap() { return typesInMultiCatchWrap; }
    public void setTypesInMultiCatchWrap(WrapOption v) { this.typesInMultiCatchWrap = v; }

    public boolean isAlignTypesInMultiCatch() { return alignTypesInMultiCatch; }
    public void setAlignTypesInMultiCatch(boolean v) { this.alignTypesInMultiCatch = v; }

    public boolean isAlignBinaryExpressionsMultiline() { return alignBinaryExpressionsMultiline; }
    public void setAlignBinaryExpressionsMultiline(boolean v) { this.alignBinaryExpressionsMultiline = v; }

    public boolean isOperationSignOnNextLine() { return operationSignOnNextLine; }
    public void setOperationSignOnNextLine(boolean v) { this.operationSignOnNextLine = v; }

    public boolean isAlignParenthesisedMultiline() { return alignParenthesisedMultiline; }
    public void setAlignParenthesisedMultiline(boolean v) { this.alignParenthesisedMultiline = v; }

    public boolean isNewLineAfterBinaryLParen() { return newLineAfterBinaryLParen; }
    public void setNewLineAfterBinaryLParen(boolean v) { this.newLineAfterBinaryLParen = v; }

    public boolean isPlaceBinaryRParenOnNewLine() { return placeBinaryRParenOnNewLine; }
    public void setPlaceBinaryRParenOnNewLine(boolean v) { this.placeBinaryRParenOnNewLine = v; }

    public WrapOption getAssignmentWrap() { return assignmentWrap; }
    public void setAssignmentWrap(WrapOption v) { this.assignmentWrap = v; }

    public boolean isAlignAssignmentMultiline() { return alignAssignmentMultiline; }
    public void setAlignAssignmentMultiline(boolean v) { this.alignAssignmentMultiline = v; }

    public boolean isAssignmentSignOnNextLine() { return assignmentSignOnNextLine; }
    public void setAssignmentSignOnNextLine(boolean v) { this.assignmentSignOnNextLine = v; }

    public boolean isAlignFieldsInColumns() { return alignFieldsInColumns; }
    public void setAlignFieldsInColumns(boolean v) { this.alignFieldsInColumns = v; }

    public boolean isAlignVariablesInColumns() { return alignVariablesInColumns; }
    public void setAlignVariablesInColumns(boolean v) { this.alignVariablesInColumns = v; }

    public boolean isAlignAssignmentsInColumns() { return alignAssignmentsInColumns; }
    public void setAlignAssignmentsInColumns(boolean v) { this.alignAssignmentsInColumns = v; }

    public boolean isAlignSimpleMethodsInColumns() { return alignSimpleMethodsInColumns; }
    public void setAlignSimpleMethodsInColumns(boolean v) { this.alignSimpleMethodsInColumns = v; }

    public WrapOption getTernaryWrap() { return ternaryWrap; }
    public void setTernaryWrap(WrapOption v) { this.ternaryWrap = v; }

    public boolean isAlignTernaryMultiline() { return alignTernaryMultiline; }
    public void setAlignTernaryMultiline(boolean v) { this.alignTernaryMultiline = v; }

    public boolean isTernarySignsOnNextLine() { return ternarySignsOnNextLine; }
    public void setTernarySignsOnNextLine(boolean v) { this.ternarySignsOnNextLine = v; }

    public WrapOption getArrayInitializerWrap() { return arrayInitializerWrap; }
    public void setArrayInitializerWrap(WrapOption v) { this.arrayInitializerWrap = v; }

    public boolean isAlignArrayInitializerMultiline() { return alignArrayInitializerMultiline; }
    public void setAlignArrayInitializerMultiline(boolean v) { this.alignArrayInitializerMultiline = v; }

    public boolean isNewLineAfterArrayInitializerLBrace() { return newLineAfterArrayInitializerLBrace; }
    public void setNewLineAfterArrayInitializerLBrace(boolean v) { this.newLineAfterArrayInitializerLBrace = v; }

    public boolean isPlaceArrayInitializerRBraceOnNewLine() { return placeArrayInitializerRBraceOnNewLine; }
    public void setPlaceArrayInitializerRBraceOnNewLine(boolean v) { this.placeArrayInitializerRBraceOnNewLine = v; }

    public boolean isWrapAfterModifierList() { return wrapAfterModifierList; }
    public void setWrapAfterModifierList(boolean v) { this.wrapAfterModifierList = v; }

    public WrapOption getAssertStatementWrap() { return assertStatementWrap; }
    public void setAssertStatementWrap(WrapOption v) { this.assertStatementWrap = v; }

    public WrapOption getEnumConstantsWrap() { return enumConstantsWrap; }
    public void setEnumConstantsWrap(WrapOption v) { this.enumConstantsWrap = v; }

    public WrapOption getClassAnnotationsWrap() { return classAnnotationsWrap; }
    public void setClassAnnotationsWrap(WrapOption v) { this.classAnnotationsWrap = v; }

    public WrapOption getMethodAnnotationsWrap() { return methodAnnotationsWrap; }
    public void setMethodAnnotationsWrap(WrapOption v) { this.methodAnnotationsWrap = v; }

    public WrapOption getFieldAnnotationsWrap() { return fieldAnnotationsWrap; }
    public void setFieldAnnotationsWrap(WrapOption v) { this.fieldAnnotationsWrap = v; }

    public boolean isDoNotWrapAfterSingleFieldAnnotation() { return doNotWrapAfterSingleFieldAnnotation; }
    public void setDoNotWrapAfterSingleFieldAnnotation(boolean v) { this.doNotWrapAfterSingleFieldAnnotation = v; }

    public WrapOption getParameterAnnotationsWrap() { return parameterAnnotationsWrap; }
    public void setParameterAnnotationsWrap(WrapOption v) { this.parameterAnnotationsWrap = v; }

    public boolean isDoNotWrapAfterSingleParameterAnnotation() { return doNotWrapAfterSingleParameterAnnotation; }
    public void setDoNotWrapAfterSingleParameterAnnotation(boolean v) { this.doNotWrapAfterSingleParameterAnnotation = v; }

    public WrapOption getLocalVariableAnnotationsWrap() { return localVariableAnnotationsWrap; }
    public void setLocalVariableAnnotationsWrap(WrapOption v) { this.localVariableAnnotationsWrap = v; }

    public WrapOption getEnumFieldAnnotationsWrap() { return enumFieldAnnotationsWrap; }
    public void setEnumFieldAnnotationsWrap(WrapOption v) { this.enumFieldAnnotationsWrap = v; }

    public WrapOption getAnnotationParametersWrap() { return annotationParametersWrap; }
    public void setAnnotationParametersWrap(WrapOption v) { this.annotationParametersWrap = v; }

    public boolean isAlignAnnotationParametersMultiline() { return alignAnnotationParametersMultiline; }
    public void setAlignAnnotationParametersMultiline(boolean v) { this.alignAnnotationParametersMultiline = v; }

    public boolean isNewLineAfterAnnotationParametersLParen() { return newLineAfterAnnotationParametersLParen; }
    public void setNewLineAfterAnnotationParametersLParen(boolean v) { this.newLineAfterAnnotationParametersLParen = v; }

    public boolean isPlaceAnnotationParametersRParenOnNewLine() { return placeAnnotationParametersRParenOnNewLine; }
    public void setPlaceAnnotationParametersRParenOnNewLine(boolean v) { this.placeAnnotationParametersRParenOnNewLine = v; }

    public boolean isAlignTextBlocksMultiline() { return alignTextBlocksMultiline; }
    public void setAlignTextBlocksMultiline(boolean v) { this.alignTextBlocksMultiline = v; }

    public WrapOption getRecordComponentsWrap() { return recordComponentsWrap; }
    public void setRecordComponentsWrap(WrapOption v) { this.recordComponentsWrap = v; }

    public boolean isAlignRecordComponentsMultiline() { return alignRecordComponentsMultiline; }
    public void setAlignRecordComponentsMultiline(boolean v) { this.alignRecordComponentsMultiline = v; }

    public boolean isNewLineAfterRecordComponentsLParen() { return newLineAfterRecordComponentsLParen; }
    public void setNewLineAfterRecordComponentsLParen(boolean v) { this.newLineAfterRecordComponentsLParen = v; }

    public boolean isPlaceRecordComponentsRParenOnNewLine() { return placeRecordComponentsRParenOnNewLine; }
    public void setPlaceRecordComponentsRParenOnNewLine(boolean v) { this.placeRecordComponentsRParenOnNewLine = v; }

    public boolean isNewLineForRecordComponentAnnotations() { return newLineForRecordComponentAnnotations; }
    public void setNewLineForRecordComponentAnnotations(boolean v) { this.newLineForRecordComponentAnnotations = v; }

    public WrapOption getDeconstructionPatternsWrap() { return deconstructionPatternsWrap; }
    public void setDeconstructionPatternsWrap(WrapOption v) { this.deconstructionPatternsWrap = v; }

    public boolean isAlignDeconstructionPatternsMultiline() { return alignDeconstructionPatternsMultiline; }
    public void setAlignDeconstructionPatternsMultiline(boolean v) { this.alignDeconstructionPatternsMultiline = v; }

    public boolean isNewLineAfterDeconstructionPatternsLParen() { return newLineAfterDeconstructionPatternsLParen; }
    public void setNewLineAfterDeconstructionPatternsLParen(boolean v) { this.newLineAfterDeconstructionPatternsLParen = v; }

    public boolean isPlaceDeconstructionPatternsRParenOnNewLine() { return placeDeconstructionPatternsRParenOnNewLine; }
    public void setPlaceDeconstructionPatternsRParenOnNewLine(boolean v) { this.placeDeconstructionPatternsRParenOnNewLine = v; }

    public int getKeepBlankLinesInDeclarations() { return keepBlankLinesInDeclarations; }
    public void setKeepBlankLinesInDeclarations(int keepBlankLinesInDeclarations) { this.keepBlankLinesInDeclarations = keepBlankLinesInDeclarations; }

    public int getKeepBlankLinesInCode() { return keepBlankLinesInCode; }
    public void setKeepBlankLinesInCode(int keepBlankLinesInCode) { this.keepBlankLinesInCode = keepBlankLinesInCode; }

    public int getKeepBlankLinesBeforeRBrace() { return keepBlankLinesBeforeRBrace; }
    public void setKeepBlankLinesBeforeRBrace(int keepBlankLinesBeforeRBrace) { this.keepBlankLinesBeforeRBrace = keepBlankLinesBeforeRBrace; }

    public int getKeepBlankLinesBetweenHeaderAndPackage() { return keepBlankLinesBetweenHeaderAndPackage; }
    public void setKeepBlankLinesBetweenHeaderAndPackage(int v) { this.keepBlankLinesBetweenHeaderAndPackage = v; }

    public int getBlankLinesBeforePackage() { return blankLinesBeforePackage; }
    public void setBlankLinesBeforePackage(int blankLinesBeforePackage) { this.blankLinesBeforePackage = blankLinesBeforePackage; }

    public int getBlankLinesAfterPackage() { return blankLinesAfterPackage; }
    public void setBlankLinesAfterPackage(int blankLinesAfterPackage) { this.blankLinesAfterPackage = blankLinesAfterPackage; }

    public int getBlankLinesBeforeImports() { return blankLinesBeforeImports; }
    public void setBlankLinesBeforeImports(int blankLinesBeforeImports) { this.blankLinesBeforeImports = blankLinesBeforeImports; }

    public int getBlankLinesAfterImports() { return blankLinesAfterImports; }
    public void setBlankLinesAfterImports(int blankLinesAfterImports) { this.blankLinesAfterImports = blankLinesAfterImports; }

    public int getBlankLinesAroundClass() { return blankLinesAroundClass; }
    public void setBlankLinesAroundClass(int blankLinesAroundClass) { this.blankLinesAroundClass = blankLinesAroundClass; }

    public int getBlankLinesAfterClassHeader() { return blankLinesAfterClassHeader; }
    public void setBlankLinesAfterClassHeader(int v) { this.blankLinesAfterClassHeader = v; }

    public int getBlankLinesBeforeClassEnd() { return blankLinesBeforeClassEnd; }
    public void setBlankLinesBeforeClassEnd(int v) { this.blankLinesBeforeClassEnd = v; }

    public int getBlankLinesAfterAnonymousClassHeader() { return blankLinesAfterAnonymousClassHeader; }
    public void setBlankLinesAfterAnonymousClassHeader(int v) { this.blankLinesAfterAnonymousClassHeader = v; }

    public int getBlankLinesBeforeFieldInInterface() { return blankLinesBeforeFieldInInterface; }
    public void setBlankLinesBeforeFieldInInterface(int v) { this.blankLinesBeforeFieldInInterface = v; }

    public int getBlankLinesBeforeFieldWithoutAnnotations() { return blankLinesBeforeFieldWithoutAnnotations; }
    public void setBlankLinesBeforeFieldWithoutAnnotations(int v) { this.blankLinesBeforeFieldWithoutAnnotations = v; }

    public int getBlankLinesBeforeFieldWithAnnotations() { return blankLinesBeforeFieldWithAnnotations; }
    public void setBlankLinesBeforeFieldWithAnnotations(int v) { this.blankLinesBeforeFieldWithAnnotations = v; }

    public int getBlankLinesAroundMethodInInterface() { return blankLinesAroundMethodInInterface; }
    public void setBlankLinesAroundMethodInInterface(int v) { this.blankLinesAroundMethodInInterface = v; }

    public int getBlankLinesAroundMethod() { return blankLinesAroundMethod; }
    public void setBlankLinesAroundMethod(int blankLinesAroundMethod) { this.blankLinesAroundMethod = blankLinesAroundMethod; }

    public int getBlankLinesBeforeMethodBody() { return blankLinesBeforeMethodBody; }
    public void setBlankLinesBeforeMethodBody(int v) { this.blankLinesBeforeMethodBody = v; }

    public int getBlankLinesAroundInitializer() { return blankLinesAroundInitializer; }
    public void setBlankLinesAroundInitializer(int v) { this.blankLinesAroundInitializer = v; }

    public int getBlankLinesBetweenRecordComponents() { return blankLinesBetweenRecordComponents; }
    public void setBlankLinesBetweenRecordComponents(int v) { this.blankLinesBetweenRecordComponents = v; }

    public boolean isAlignParamDescriptions() { return alignParamDescriptions; }
    public void setAlignParamDescriptions(boolean alignParamDescriptions) { this.alignParamDescriptions = alignParamDescriptions; }

    public boolean isAlignThrownExceptions() { return alignThrownExceptions; }
    public void setAlignThrownExceptions(boolean alignThrownExceptions) { this.alignThrownExceptions = alignThrownExceptions; }

    // JavaDoc getters and setters
    public boolean isEnableJavaDocFormatting() { return enableJavaDocFormatting; }
    public void setEnableJavaDocFormatting(boolean v) { this.enableJavaDocFormatting = v; }

    public boolean isFormatComments() { return formatComments; }
    public void setFormatComments(boolean formatComments) { this.formatComments = formatComments; }

    public boolean isBlankLinesAfterDescription() { return blankLinesAfterDescription; }
    public void setBlankLinesAfterDescription(boolean blankLinesAfterDescription) { this.blankLinesAfterDescription = blankLinesAfterDescription; }

    public boolean isBlankLinesAfterParamDescriptions() { return blankLinesAfterParamDescriptions; }
    public void setBlankLinesAfterParamDescriptions(boolean blankLinesAfterParamDescriptions) { this.blankLinesAfterParamDescriptions = blankLinesAfterParamDescriptions; }

    public boolean isBlankLinesAfterReturnTag() { return blankLinesAfterReturnTag; }
    public void setBlankLinesAfterReturnTag(boolean blankLinesAfterReturnTag) { this.blankLinesAfterReturnTag = blankLinesAfterReturnTag; }

    public boolean isKeepInvalidTags() { return keepInvalidTags; }
    public void setKeepInvalidTags(boolean v) { this.keepInvalidTags = v; }

    public boolean isKeepEmptyParamTags() { return keepEmptyParamTags; }
    public void setKeepEmptyParamTags(boolean v) { this.keepEmptyParamTags = v; }

    public boolean isKeepEmptyReturnTags() { return keepEmptyReturnTags; }
    public void setKeepEmptyReturnTags(boolean v) { this.keepEmptyReturnTags = v; }

    public boolean isKeepEmptyThrowsTags() { return keepEmptyThrowsTags; }
    public void setKeepEmptyThrowsTags(boolean v) { this.keepEmptyThrowsTags = v; }

    public boolean isWrapAtRightMargin() { return wrapAtRightMargin; }
    public void setWrapAtRightMargin(boolean wrapAtRightMargin) { this.wrapAtRightMargin = wrapAtRightMargin; }

    public boolean isEnableLeadingAsterisks() { return enableLeadingAsterisks; }
    public void setEnableLeadingAsterisks(boolean v) { this.enableLeadingAsterisks = v; }

    public boolean isUseThrowsRatherThanException() { return useThrowsRatherThanException; }
    public void setUseThrowsRatherThanException(boolean v) { this.useThrowsRatherThanException = v; }

    public boolean isGeneratePOnEmptyLines() { return generatePOnEmptyLines; }
    public void setGeneratePOnEmptyLines(boolean v) { this.generatePOnEmptyLines = v; }

    public boolean isKeepEmptyLines() { return keepEmptyLines; }
    public void setKeepEmptyLines(boolean v) { this.keepEmptyLines = v; }

    public boolean isDoNotWrapOneLineComments() { return doNotWrapOneLineComments; }
    public void setDoNotWrapOneLineComments(boolean v) { this.doNotWrapOneLineComments = v; }

    public boolean isPreserveLineFeeds() { return preserveLineFeeds; }
    public void setPreserveLineFeeds(boolean v) { this.preserveLineFeeds = v; }

    public boolean isParamDescriptionsOnNewLine() { return paramDescriptionsOnNewLine; }
    public void setParamDescriptionsOnNewLine(boolean v) { this.paramDescriptionsOnNewLine = v; }

    public boolean isIndentContinuationLines() { return indentContinuationLines; }
    public void setIndentContinuationLines(boolean v) { this.indentContinuationLines = v; }

    // Imports getters and setters
    public boolean isUseSingleClassImport() { return useSingleClassImport; }
    public void setUseSingleClassImport(boolean v) { this.useSingleClassImport = v; }

    public boolean isUseFullyQualifiedClassNames() { return useFullyQualifiedClassNames; }
    public void setUseFullyQualifiedClassNames(boolean v) { this.useFullyQualifiedClassNames = v; }

    public boolean isInsertInnerClassImports() { return insertInnerClassImports; }
    public void setInsertInnerClassImports(boolean insertInnerClassImports) { this.insertInnerClassImports = insertInnerClassImports; }

    public List<String> getExcludedInnerClasses() { return excludedInnerClasses; }
    public void setExcludedInnerClasses(List<String> list) { this.excludedInnerClasses = list != null ? new ArrayList<>(list) : new ArrayList<>(); }

    public boolean isDoNotSeparateModuleImports() { return doNotSeparateModuleImports; }
    public void setDoNotSeparateModuleImports(boolean v) { this.doNotSeparateModuleImports = v; }

    public boolean isDeleteUnusedModuleImports() { return deleteUnusedModuleImports; }
    public void setDeleteUnusedModuleImports(boolean v) { this.deleteUnusedModuleImports = v; }

    public String getUseFqNamesInJavadoc() { return useFqNamesInJavadoc; }
    public void setUseFqNamesInJavadoc(String v) { this.useFqNamesInJavadoc = v != null ? v : "If not already imported"; }
    public boolean isUseFqNamesInJavadoc() { return !"Never, use short name and add import".equals(useFqNamesInJavadoc); }
    public void setUseFqNamesInJavadoc(boolean v) { this.useFqNamesInJavadoc = v ? "If not already imported" : "Never, use short name and add import"; }

    public int getClassCountToUseImportOnDemand() { return classCountToUseImportOnDemand; }
    public void setClassCountToUseImportOnDemand(int classCountToUseImportOnDemand) { this.classCountToUseImportOnDemand = classCountToUseImportOnDemand; }

    public int getNamesCountToUseStaticImportOnDemand() { return namesCountToUseStaticImportOnDemand; }
    public void setNamesCountToUseStaticImportOnDemand(int namesCountToUseStaticImportOnDemand) { this.namesCountToUseStaticImportOnDemand = namesCountToUseStaticImportOnDemand; }

    public List<ImportEntry> getPackagesToUseImportOnDemand() { return packagesToUseImportOnDemand; }
    public void setPackagesToUseImportOnDemand(List<ImportEntry> list) {
        this.packagesToUseImportOnDemand = list != null ? list.stream().map(ImportEntry::copy).collect(Collectors.toList()) : new ArrayList<>();
    }

    public boolean isPlaceOnDemandImportBeforeSingleClassImports() { return placeOnDemandImportBeforeSingleClassImports; }
    public void setPlaceOnDemandImportBeforeSingleClassImports(boolean v) { this.placeOnDemandImportBeforeSingleClassImports = v; }

    public boolean isLayoutStaticImportsSeparately() { return layoutStaticImportsSeparately; }
    public void setLayoutStaticImportsSeparately(boolean v) { this.layoutStaticImportsSeparately = v; }

    public List<ImportLayoutEntry> getImportLayout() { return importLayout; }
    public void setImportLayout(List<ImportLayoutEntry> list) {
        this.importLayout = list != null ? list.stream().map(ImportLayoutEntry::copy).collect(Collectors.toList()) : new ArrayList<>();
    }

    // 7. Arrangement
    public boolean isKeepGettersAndSettersTogether() { return keepGettersAndSettersTogether; }
    public void setKeepGettersAndSettersTogether(boolean v) { this.keepGettersAndSettersTogether = v; }

    public boolean isKeepOverriddenMethodsTogether() { return keepOverriddenMethodsTogether; }
    public void setKeepOverriddenMethodsTogether(boolean v) { this.keepOverriddenMethodsTogether = v; }

    public boolean isKeepOverloadedMethodsTogether() { return keepOverriddenMethodsTogether; }
    public void setKeepOverloadedMethodsTogether(boolean v) { this.keepOverriddenMethodsTogether = v; }

    public String getOverriddenMethodsOrder() { return overriddenMethodsOrder; }
    public void setOverriddenMethodsOrder(String v) { this.overriddenMethodsOrder = v != null ? v : "keep order"; }

    public boolean isKeepDependentMethodsTogether() { return keepDependentMethodsTogether; }
    public void setKeepDependentMethodsTogether(boolean v) { this.keepDependentMethodsTogether = v; }

    public String getDependentMethodsOrder() { return dependentMethodsOrder; }
    public void setDependentMethodsOrder(String v) { this.dependentMethodsOrder = v != null ? v : "breadth-first order"; }

    public List<ArrangementRule> getMatchingRules() { return matchingRules; }
    public void setMatchingRules(List<ArrangementRule> rules) {
        this.matchingRules = rules != null ? rules.stream().map(ArrangementRule::copy).collect(Collectors.toList()) : new ArrayList<>();
    }

    // 8. Code Generation
    public boolean isPreferLongerNames() { return preferLongerNames; }
    public void setPreferLongerNames(boolean v) { this.preferLongerNames = v; }

    public String getFieldPrefix() { return fieldPrefix; }
    public void setFieldPrefix(String fieldPrefix) { this.fieldPrefix = fieldPrefix != null ? fieldPrefix : ""; }

    public String getFieldSuffix() { return fieldSuffix; }
    public void setFieldSuffix(String fieldSuffix) { this.fieldSuffix = fieldSuffix != null ? fieldSuffix : ""; }

    public String getStaticFieldPrefix() { return staticFieldPrefix; }
    public void setStaticFieldPrefix(String staticFieldPrefix) { this.staticFieldPrefix = staticFieldPrefix != null ? staticFieldPrefix : ""; }

    public String getStaticFieldSuffix() { return staticFieldSuffix; }
    public void setStaticFieldSuffix(String staticFieldSuffix) { this.staticFieldSuffix = staticFieldSuffix != null ? staticFieldSuffix : ""; }

    public String getParameterPrefix() { return parameterPrefix; }
    public void setParameterPrefix(String parameterPrefix) { this.parameterPrefix = parameterPrefix != null ? parameterPrefix : ""; }

    public String getParameterSuffix() { return parameterSuffix; }
    public void setParameterSuffix(String parameterSuffix) { this.parameterSuffix = parameterSuffix != null ? parameterSuffix : ""; }

    public String getLocalVariablePrefix() { return localVariablePrefix; }
    public void setLocalVariablePrefix(String localVariablePrefix) { this.localVariablePrefix = localVariablePrefix != null ? localVariablePrefix : ""; }

    public String getLocalVariableSuffix() { return localVariableSuffix; }
    public void setLocalVariableSuffix(String localVariableSuffix) { this.localVariableSuffix = localVariableSuffix != null ? localVariableSuffix : ""; }

    public String getSubclassPrefix() { return subclassPrefix; }
    public void setSubclassPrefix(String v) { this.subclassPrefix = v != null ? v : ""; }

    public String getSubclassSuffix() { return subclassSuffix; }
    public void setSubclassSuffix(String v) { this.subclassSuffix = v != null ? v : "Impl"; }

    public String getTestClassPrefix() { return testClassPrefix; }
    public void setTestClassPrefix(String v) { this.testClassPrefix = v != null ? v : ""; }

    public String getTestClassSuffix() { return testClassSuffix; }
    public void setTestClassSuffix(String v) { this.testClassSuffix = v != null ? v : "Test"; }

    public String getDefaultVisibility() { return defaultVisibility; }
    public void setDefaultVisibility(String v) { this.defaultVisibility = v != null ? v : "Public"; }

    public boolean isMakeGeneratedLocalsFinal() { return makeGeneratedLocalsFinal; }
    public void setMakeGeneratedLocalsFinal(boolean makeGeneratedLocalsFinal) { this.makeGeneratedLocalsFinal = makeGeneratedLocalsFinal; }

    public boolean isMakeGeneratedParametersFinal() { return makeGeneratedParametersFinal; }
    public void setMakeGeneratedParametersFinal(boolean makeGeneratedParametersFinal) { this.makeGeneratedParametersFinal = makeGeneratedParametersFinal; }

    public boolean isUseVarForLocalVariables() { return useVarForLocalVariables; }
    public void setUseVarForLocalVariables(boolean v) { this.useVarForLocalVariables = v; }

    public boolean isLineCommentAtFirstColumn() { return lineCommentAtFirstColumn; }
    public void setLineCommentAtFirstColumn(boolean v) { this.lineCommentAtFirstColumn = v; }

    public boolean isAddSpaceAtLineCommentStart() { return addSpaceAtLineCommentStart; }
    public void setAddSpaceAtLineCommentStart(boolean v) { this.addSpaceAtLineCommentStart = v; }

    public boolean isEnforceOnReformat() { return enforceOnReformat; }
    public void setEnforceOnReformat(boolean v) { this.enforceOnReformat = v; }

    public boolean isBlockCommentAtFirstColumn() { return blockCommentAtFirstColumn; }
    public void setBlockCommentAtFirstColumn(boolean v) { this.blockCommentAtFirstColumn = v; }

    public boolean isAddSpacesAroundBlockComments() { return addSpacesAroundBlockComments; }
    public void setAddSpacesAroundBlockComments(boolean v) { this.addSpacesAroundBlockComments = v; }

    public boolean isInsertOverride() { return insertOverride; }
    public void setInsertOverride(boolean insertOverride) { this.insertOverride = insertOverride; }

    public boolean isRepeatSynchronized() { return repeatSynchronized; }
    public void setRepeatSynchronized(boolean v) { this.repeatSynchronized = v; }

    public List<String> getAnnotationsToCopy() { return annotationsToCopy; }
    public void setAnnotationsToCopy(List<String> list) { this.annotationsToCopy = list != null ? new ArrayList<>(list) : new ArrayList<>(); }

    public boolean isUseExternalAnnotations() { return useExternalAnnotations; }
    public void setUseExternalAnnotations(boolean v) { this.useExternalAnnotations = v; }

    public boolean isInsertTypeUseAnnotationsBeforeType() { return insertTypeUseAnnotationsBeforeType; }
    public void setInsertTypeUseAnnotationsBeforeType(boolean v) { this.insertTypeUseAnnotationsBeforeType = v; }

    public boolean isUseClassIsInstanceAndCast() { return useClassIsInstanceAndCast; }
    public void setUseClassIsInstanceAndCast(boolean v) { this.useClassIsInstanceAndCast = v; }

    public boolean isReplaceNullCheckWithObjectsNonNull() { return replaceNullCheckWithObjectsNonNull; }
    public void setReplaceNullCheckWithObjectsNonNull(boolean v) { this.replaceNullCheckWithObjectsNonNull = v; }

    public boolean isUseIntegerSumWhenPossible() { return useIntegerSumWhenPossible; }
    public void setUseIntegerSumWhenPossible(boolean v) { this.useIntegerSumWhenPossible = v; }

    // 9. Java EE Names
    // Entity Bean
    public String getEntityEjbClassPrefix() { return entityEjbClassPrefix; }
    public void setEntityEjbClassPrefix(String v) { this.entityEjbClassPrefix = v != null ? v : ""; }

    public String getEntityEjbClassSuffix() { return entityEjbClassSuffix; }
    public void setEntityEjbClassSuffix(String v) { this.entityEjbClassSuffix = v != null ? v : "Bean"; }

    public String getEntityHomeInterfacePrefix() { return entityHomeInterfacePrefix; }
    public void setEntityHomeInterfacePrefix(String v) { this.entityHomeInterfacePrefix = v != null ? v : ""; }

    public String getEntityHomeInterfaceSuffix() { return entityHomeInterfaceSuffix; }
    public void setEntityHomeInterfaceSuffix(String v) { this.entityHomeInterfaceSuffix = v != null ? v : "Home"; }

    public String getEntityRemoteInterfacePrefix() { return entityRemoteInterfacePrefix; }
    public void setEntityRemoteInterfacePrefix(String v) { this.entityRemoteInterfacePrefix = v != null ? v : ""; }

    public String getEntityRemoteInterfaceSuffix() { return entityRemoteInterfaceSuffix; }
    public void setEntityRemoteInterfaceSuffix(String v) { this.entityRemoteInterfaceSuffix = v != null ? v : ""; }

    public String getEntityLocalHomeInterfacePrefix() { return entityLocalHomeInterfacePrefix; }
    public void setEntityLocalHomeInterfacePrefix(String v) { this.entityLocalHomeInterfacePrefix = v != null ? v : "Local"; }

    public String getEntityLocalHomeInterfaceSuffix() { return entityLocalHomeInterfaceSuffix; }
    public void setEntityLocalHomeInterfaceSuffix(String v) { this.entityLocalHomeInterfaceSuffix = v != null ? v : "Home"; }

    public String getEntityLocalInterfacePrefix() { return entityLocalInterfacePrefix; }
    public void setEntityLocalInterfacePrefix(String v) { this.entityLocalInterfacePrefix = v != null ? v : "Local"; }

    public String getEntityLocalInterfaceSuffix() { return entityLocalInterfaceSuffix; }
    public void setEntityLocalInterfaceSuffix(String v) { this.entityLocalInterfaceSuffix = v != null ? v : ""; }

    public String getEntityEjbNameTagPrefix() { return entityEjbNameTagPrefix; }
    public void setEntityEjbNameTagPrefix(String v) { this.entityEjbNameTagPrefix = v != null ? v : ""; }

    public String getEntityEjbNameTagSuffix() { return entityEjbNameTagSuffix; }
    public void setEntityEjbNameTagSuffix(String v) { this.entityEjbNameTagSuffix = v != null ? v : "EJB"; }

    public String getEntityTransferObjectPrefix() { return entityTransferObjectPrefix; }
    public void setEntityTransferObjectPrefix(String v) { this.entityTransferObjectPrefix = v != null ? v : ""; }

    public String getEntityTransferObjectSuffix() { return entityTransferObjectSuffix; }
    public void setEntityTransferObjectSuffix(String v) { this.entityTransferObjectSuffix = v != null ? v : "VO"; }

    public String getEntityDefaultPkClass() { return entityDefaultPkClass; }
    public void setEntityDefaultPkClass(String v) { this.entityDefaultPkClass = v != null ? v : "java.lang.String"; }

    // Session Bean
    public String getSessionEjbClassPrefix() { return sessionEjbClassPrefix; }
    public void setSessionEjbClassPrefix(String v) { this.sessionEjbClassPrefix = v != null ? v : ""; }

    public String getSessionEjbClassSuffix() { return sessionEjbClassSuffix; }
    public void setSessionEjbClassSuffix(String v) { this.sessionEjbClassSuffix = v != null ? v : "Bean"; }

    public String getSessionHomeInterfacePrefix() { return sessionHomeInterfacePrefix; }
    public void setSessionHomeInterfacePrefix(String v) { this.sessionHomeInterfacePrefix = v != null ? v : ""; }

    public String getSessionHomeInterfaceSuffix() { return sessionHomeInterfaceSuffix; }
    public void setSessionHomeInterfaceSuffix(String v) { this.sessionHomeInterfaceSuffix = v != null ? v : "Home"; }

    public String getSessionRemoteInterfacePrefix() { return sessionRemoteInterfacePrefix; }
    public void setSessionRemoteInterfacePrefix(String v) { this.sessionRemoteInterfacePrefix = v != null ? v : ""; }

    public String getSessionRemoteInterfaceSuffix() { return sessionRemoteInterfaceSuffix; }
    public void setSessionRemoteInterfaceSuffix(String v) { this.sessionRemoteInterfaceSuffix = v != null ? v : ""; }

    public String getSessionLocalHomeInterfacePrefix() { return sessionLocalHomeInterfacePrefix; }
    public void setSessionLocalHomeInterfacePrefix(String v) { this.sessionLocalHomeInterfacePrefix = v != null ? v : "Local"; }

    public String getSessionLocalHomeInterfaceSuffix() { return sessionLocalHomeInterfaceSuffix; }
    public void setSessionLocalHomeInterfaceSuffix(String v) { this.sessionLocalHomeInterfaceSuffix = v != null ? v : "Home"; }

    public String getSessionLocalInterfacePrefix() { return sessionLocalInterfacePrefix; }
    public void setSessionLocalInterfacePrefix(String v) { this.sessionLocalInterfacePrefix = v != null ? v : "Local"; }

    public String getSessionLocalInterfaceSuffix() { return sessionLocalInterfaceSuffix; }
    public void setSessionLocalInterfaceSuffix(String v) { this.sessionLocalInterfaceSuffix = v != null ? v : ""; }

    public String getSessionServiceEndpointPrefix() { return sessionServiceEndpointPrefix; }
    public void setSessionServiceEndpointPrefix(String v) { this.sessionServiceEndpointPrefix = v != null ? v : ""; }

    public String getSessionServiceEndpointSuffix() { return sessionServiceEndpointSuffix; }
    public void setSessionServiceEndpointSuffix(String v) { this.sessionServiceEndpointSuffix = v != null ? v : "Service"; }

    public String getSessionEjbNameTagPrefix() { return sessionEjbNameTagPrefix; }
    public void setSessionEjbNameTagPrefix(String v) { this.sessionEjbNameTagPrefix = v != null ? v : ""; }

    public String getSessionEjbNameTagSuffix() { return sessionEjbNameTagSuffix; }
    public void setSessionEjbNameTagSuffix(String v) { this.sessionEjbNameTagSuffix = v != null ? v : "EJB"; }

    // Servlet
    public String getServletClassPrefix() { return servletClassPrefix; }
    public void setServletClassPrefix(String v) { this.servletClassPrefix = v != null ? v : ""; }

    public String getServletClassSuffix() { return servletClassSuffix; }
    public void setServletClassSuffix(String v) { this.servletClassSuffix = v != null ? v : ""; }

    public String getServletNameTagPrefix() { return servletNameTagPrefix; }
    public void setServletNameTagPrefix(String v) { this.servletNameTagPrefix = v != null ? v : ""; }

    public String getServletNameTagSuffix() { return servletNameTagSuffix; }
    public void setServletNameTagSuffix(String v) { this.servletNameTagSuffix = v != null ? v : ""; }

    // Message Driven Bean
    public String getMdbEjbClassPrefix() { return mdbEjbClassPrefix; }
    public void setMdbEjbClassPrefix(String v) { this.mdbEjbClassPrefix = v != null ? v : ""; }

    public String getMdbEjbClassSuffix() { return mdbEjbClassSuffix; }
    public void setMdbEjbClassSuffix(String v) { this.mdbEjbClassSuffix = v != null ? v : "Bean"; }

    public String getMdbEjbNameTagPrefix() { return mdbEjbNameTagPrefix; }
    public void setMdbEjbNameTagPrefix(String v) { this.mdbEjbNameTagPrefix = v != null ? v : ""; }

    public String getMdbEjbNameTagSuffix() { return mdbEjbNameTagSuffix; }
    public void setMdbEjbNameTagSuffix(String v) { this.mdbEjbNameTagSuffix = v != null ? v : "EJB"; }

    // Filter
    public String getFilterClassPrefix() { return filterClassPrefix; }
    public void setFilterClassPrefix(String v) { this.filterClassPrefix = v != null ? v : ""; }

    public String getFilterClassSuffix() { return filterClassSuffix; }
    public void setFilterClassSuffix(String v) { this.filterClassSuffix = v != null ? v : ""; }

    public String getFilterNameTagPrefix() { return filterNameTagPrefix; }
    public void setFilterNameTagPrefix(String v) { this.filterNameTagPrefix = v != null ? v : ""; }

    public String getFilterNameTagSuffix() { return filterNameTagSuffix; }
    public void setFilterNameTagSuffix(String v) { this.filterNameTagSuffix = v != null ? v : ""; }

    // Listener
    public String getListenerClassPrefix() { return listenerClassPrefix; }
    public void setListenerClassPrefix(String v) { this.listenerClassPrefix = v != null ? v : ""; }

    public String getListenerClassSuffix() { return listenerClassSuffix; }
    public void setListenerClassSuffix(String v) { this.listenerClassSuffix = v != null ? v : ""; }

    // Legacy compatibility fields
    public String getEntityBeanSuffix() { return entityBeanSuffix; }
    public void setEntityBeanSuffix(String entityBeanSuffix) { this.entityBeanSuffix = entityBeanSuffix != null ? entityBeanSuffix : ""; }

    public String getSessionBeanSuffix() { return sessionBeanSuffix; }
    public void setSessionBeanSuffix(String sessionBeanSuffix) { this.sessionBeanSuffix = sessionBeanSuffix != null ? sessionBeanSuffix : ""; }

    public String getMessageDrivenBeanSuffix() { return messageDrivenBeanSuffix; }
    public void setMessageDrivenBeanSuffix(String messageDrivenBeanSuffix) { this.messageDrivenBeanSuffix = messageDrivenBeanSuffix != null ? messageDrivenBeanSuffix : ""; }

    public String getDaoSuffix() { return daoSuffix; }
    public void setDaoSuffix(String daoSuffix) { this.daoSuffix = daoSuffix != null ? daoSuffix : ""; }

    public String getServiceSuffix() { return serviceSuffix; }
    public void setServiceSuffix(String serviceSuffix) { this.serviceSuffix = serviceSuffix != null ? serviceSuffix : ""; }

    public String getServiceImplSuffix() { return serviceImplSuffix; }
    public void setServiceImplSuffix(String serviceImplSuffix) { this.serviceImplSuffix = serviceImplSuffix != null ? serviceImplSuffix : ""; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaCodeStyleSettings that = (JavaCodeStyleSettings) o;
        return useTabCharacter == that.useTabCharacter &&
                smartTabs == that.smartTabs &&
                tabSize == that.tabSize &&
                indent == that.indent &&
                continuationIndent == that.continuationIndent &&
                keepIndentsOnEmptyLines == that.keepIndentsOnEmptyLines &&
                labelIndent == that.labelIndent &&
                absoluteLabelIndent == that.absoluteLabelIndent &&
                doNotIndentTopLevelMembers == that.doNotIndentTopLevelMembers &&
                useIndentsRelativeToExpressionStart == that.useIndentsRelativeToExpressionStart &&
                spaceBeforeMethodDeclParen == that.spaceBeforeMethodDeclParen &&
                spaceBeforeMethodCallParen == that.spaceBeforeMethodCallParen &&
                spaceBeforeIfParen == that.spaceBeforeIfParen &&
                spaceBeforeForParen == that.spaceBeforeForParen &&
                spaceBeforeWhileParen == that.spaceBeforeWhileParen &&
                spaceBeforeSwitchParen == that.spaceBeforeSwitchParen &&
                spaceBeforeTryParen == that.spaceBeforeTryParen &&
                spaceBeforeCatchParen == that.spaceBeforeCatchParen &&
                spaceBeforeSynchronizedParen == that.spaceBeforeSynchronizedParen &&
                spaceBeforeAnnotationParens == that.spaceBeforeAnnotationParens &&
                spaceBeforeDeconstructionList == that.spaceBeforeDeconstructionList &&
                spaceAroundAssignmentOps == that.spaceAroundAssignmentOps &&
                spaceAroundLogicalOps == that.spaceAroundLogicalOps &&
                spaceAroundEqualityOps == that.spaceAroundEqualityOps &&
                spaceAroundRelationalOps == that.spaceAroundRelationalOps &&
                spaceAroundBitwiseOps == that.spaceAroundBitwiseOps &&
                spaceAroundAdditiveOps == that.spaceAroundAdditiveOps &&
                spaceAroundMultiplicativeOps == that.spaceAroundMultiplicativeOps &&
                spaceAroundShiftOps == that.spaceAroundShiftOps &&
                spaceAroundUnaryOps == that.spaceAroundUnaryOps &&
                spaceAroundLambdaArrow == that.spaceAroundLambdaArrow &&
                spaceAroundMethodRefDoubleColon == that.spaceAroundMethodRefDoubleColon &&
                spaceBeforeClassLeftBrace == that.spaceBeforeClassLeftBrace &&
                spaceBeforeMethodLeftBrace == that.spaceBeforeMethodLeftBrace &&
                spaceBeforeIfLeftBrace == that.spaceBeforeIfLeftBrace &&
                spaceBeforeElseLeftBrace == that.spaceBeforeElseLeftBrace &&
                spaceBeforeWhileLeftBrace == that.spaceBeforeWhileLeftBrace &&
                spaceBeforeForLeftBrace == that.spaceBeforeForLeftBrace &&
                spaceBeforeDoLeftBrace == that.spaceBeforeDoLeftBrace &&
                spaceBeforeSwitchLeftBrace == that.spaceBeforeSwitchLeftBrace &&
                spaceBeforeTryLeftBrace == that.spaceBeforeTryLeftBrace &&
                spaceBeforeCatchLeftBrace == that.spaceBeforeCatchLeftBrace &&
                spaceBeforeFinallyLeftBrace == that.spaceBeforeFinallyLeftBrace &&
                spaceBeforeElse == that.spaceBeforeElse &&
                spaceBeforeWhile == that.spaceBeforeWhile &&
                spaceBeforeCatch == that.spaceBeforeCatch &&
                spaceBeforeFinally == that.spaceBeforeFinally &&
                spaceAfterComma == that.spaceAfterComma &&
                spaceBeforeComma == that.spaceBeforeComma &&
                spaceAfterSemicolon == that.spaceAfterSemicolon &&
                spaceBeforeSemicolon == that.spaceBeforeSemicolon &&
                spaceAfterColon == that.spaceAfterColon &&
                spaceBeforeColon == that.spaceBeforeColon &&
                hardWrapAt == that.hardWrapAt &&
                wrapOnTyping == that.wrapOnTyping &&
                keepLineBreaks == that.keepLineBreaks &&
                ensureRightMargin == that.ensureRightMargin &&
                keepSimpleBlocksInOneLine == that.keepSimpleBlocksInOneLine &&
                keepSimpleMethodsInOneLine == that.keepSimpleMethodsInOneLine &&
                keepSimpleClassesInOneLine == that.keepSimpleClassesInOneLine &&
                keepSimpleLambdasInOneLine == that.keepSimpleLambdasInOneLine &&
                classBracePlacement == that.classBracePlacement &&
                methodBracePlacement == that.methodBracePlacement &&
                otherBracePlacement == that.otherBracePlacement &&
                methodDeclParametersWrap == that.methodDeclParametersWrap &&
                alignMethodDeclParameters == that.alignMethodDeclParameters &&
                newLineAfterMethodDeclLParen == that.newLineAfterMethodDeclLParen &&
                placeMethodDeclRParenOnNewLine == that.placeMethodDeclRParenOnNewLine &&
                methodCallArgumentsWrap == that.methodCallArgumentsWrap &&
                alignMethodCallArguments == that.alignMethodCallArguments &&
                takePriorityOverCallChainWrapping == that.takePriorityOverCallChainWrapping &&
                newLineAfterMethodCallLParen == that.newLineAfterMethodCallLParen &&
                placeMethodCallRParenOnNewLine == that.placeMethodCallRParenOnNewLine &&
                alignMethodParenthesesMultiline == that.alignMethodParenthesesMultiline &&
                newLineWhenMethodBodyPresented == that.newLineWhenMethodBodyPresented &&
                chainedMethodCallsWrap == that.chainedMethodCallsWrap &&
                wrapFirstCall == that.wrapFirstCall &&
                alignChainedCallsMultiline == that.alignChainedCallsMultiline &&
                Objects.equals(builderMethods, that.builderMethods) &&
                keepBuilderMethodsIndents == that.keepBuilderMethodsIndents &&
                moveSemicolonToNewLine == that.moveSemicolonToNewLine &&
                Objects.equals(ifForceBraces, that.ifForceBraces) &&
                elseOnNewLine == that.elseOnNewLine &&
                specialElseIfTreatment == that.specialElseIfTreatment &&
                forStatementWrap == that.forStatementWrap &&
                alignForMultiline == that.alignForMultiline &&
                newLineAfterForLParen == that.newLineAfterForLParen &&
                placeForRParenOnNewLine == that.placeForRParenOnNewLine &&
                Objects.equals(forForceBraces, that.forForceBraces) &&
                Objects.equals(whileForceBraces, that.whileForceBraces) &&
                Objects.equals(doWhileForceBraces, that.doWhileForceBraces) &&
                whileOnNewLine == that.whileOnNewLine &&
                switchWrap == that.switchWrap &&
                indentCaseBranches == that.indentCaseBranches &&
                eachCaseOnSeparateLine == that.eachCaseOnSeparateLine &&
                tryWithResourcesWrap == that.tryWithResourcesWrap &&
                alignTryWithResourcesMultiline == that.alignTryWithResourcesMultiline &&
                newLineAfterTryWithResourcesLParen == that.newLineAfterTryWithResourcesLParen &&
                placeTryWithResourcesRParenOnNewLine == that.placeTryWithResourcesRParenOnNewLine &&
                catchOnNewLine == that.catchOnNewLine &&
                finallyOnNewLine == that.finallyOnNewLine &&
                typesInMultiCatchWrap == that.typesInMultiCatchWrap &&
                alignTypesInMultiCatch == that.alignTypesInMultiCatch &&
                extendsImplementsWrap == that.extendsImplementsWrap &&
                alignExtendsImplementsMultiline == that.alignExtendsImplementsMultiline &&
                extendsImplementsKeywordWrap == that.extendsImplementsKeywordWrap &&
                throwsWrap == that.throwsWrap &&
                alignThrowsMultiline == that.alignThrowsMultiline &&
                alignThrowsToMethodStart == that.alignThrowsToMethodStart &&
                throwsKeywordWrap == that.throwsKeywordWrap &&
                binaryExpressionsWrap == that.binaryExpressionsWrap &&
                alignBinaryExpressionsMultiline == that.alignBinaryExpressionsMultiline &&
                operationSignOnNextLine == that.operationSignOnNextLine &&
                alignParenthesisedMultiline == that.alignParenthesisedMultiline &&
                newLineAfterBinaryLParen == that.newLineAfterBinaryLParen &&
                placeBinaryRParenOnNewLine == that.placeBinaryRParenOnNewLine &&
                assignmentWrap == that.assignmentWrap &&
                alignAssignmentMultiline == that.alignAssignmentMultiline &&
                assignmentSignOnNextLine == that.assignmentSignOnNextLine &&
                alignFieldsInColumns == that.alignFieldsInColumns &&
                alignVariablesInColumns == that.alignVariablesInColumns &&
                alignAssignmentsInColumns == that.alignAssignmentsInColumns &&
                alignSimpleMethodsInColumns == that.alignSimpleMethodsInColumns &&
                ternaryWrap == that.ternaryWrap &&
                alignTernaryMultiline == that.alignTernaryMultiline &&
                ternarySignsOnNextLine == that.ternarySignsOnNextLine &&
                arrayInitializerWrap == that.arrayInitializerWrap &&
                alignArrayInitializerMultiline == that.alignArrayInitializerMultiline &&
                newLineAfterArrayInitializerLBrace == that.newLineAfterArrayInitializerLBrace &&
                placeArrayInitializerRBraceOnNewLine == that.placeArrayInitializerRBraceOnNewLine &&
                wrapAfterModifierList == that.wrapAfterModifierList &&
                assertStatementWrap == that.assertStatementWrap &&
                enumConstantsWrap == that.enumConstantsWrap &&
                classAnnotationsWrap == that.classAnnotationsWrap &&
                methodAnnotationsWrap == that.methodAnnotationsWrap &&
                fieldAnnotationsWrap == that.fieldAnnotationsWrap &&
                doNotWrapAfterSingleFieldAnnotation == that.doNotWrapAfterSingleFieldAnnotation &&
                parameterAnnotationsWrap == that.parameterAnnotationsWrap &&
                doNotWrapAfterSingleParameterAnnotation == that.doNotWrapAfterSingleParameterAnnotation &&
                localVariableAnnotationsWrap == that.localVariableAnnotationsWrap &&
                enumFieldAnnotationsWrap == that.enumFieldAnnotationsWrap &&
                annotationParametersWrap == that.annotationParametersWrap &&
                alignAnnotationParametersMultiline == that.alignAnnotationParametersMultiline &&
                newLineAfterAnnotationParametersLParen == that.newLineAfterAnnotationParametersLParen &&
                placeAnnotationParametersRParenOnNewLine == that.placeAnnotationParametersRParenOnNewLine &&
                alignTextBlocksMultiline == that.alignTextBlocksMultiline &&
                recordComponentsWrap == that.recordComponentsWrap &&
                alignRecordComponentsMultiline == that.alignRecordComponentsMultiline &&
                newLineAfterRecordComponentsLParen == that.newLineAfterRecordComponentsLParen &&
                placeRecordComponentsRParenOnNewLine == that.placeRecordComponentsRParenOnNewLine &&
                newLineForRecordComponentAnnotations == that.newLineForRecordComponentAnnotations &&
                deconstructionPatternsWrap == that.deconstructionPatternsWrap &&
                alignDeconstructionPatternsMultiline == that.alignDeconstructionPatternsMultiline &&
                newLineAfterDeconstructionPatternsLParen == that.newLineAfterDeconstructionPatternsLParen &&
                placeDeconstructionPatternsRParenOnNewLine == that.placeDeconstructionPatternsRParenOnNewLine &&
                keepBlankLinesInDeclarations == that.keepBlankLinesInDeclarations &&
                keepBlankLinesInCode == that.keepBlankLinesInCode &&
                keepBlankLinesBeforeRBrace == that.keepBlankLinesBeforeRBrace &&
                keepBlankLinesBetweenHeaderAndPackage == that.keepBlankLinesBetweenHeaderAndPackage &&
                blankLinesBeforePackage == that.blankLinesBeforePackage &&
                blankLinesAfterPackage == that.blankLinesAfterPackage &&
                blankLinesBeforeImports == that.blankLinesBeforeImports &&
                blankLinesAfterImports == that.blankLinesAfterImports &&
                blankLinesAroundClass == that.blankLinesAroundClass &&
                blankLinesAfterClassHeader == that.blankLinesAfterClassHeader &&
                blankLinesBeforeClassEnd == that.blankLinesBeforeClassEnd &&
                blankLinesAfterAnonymousClassHeader == that.blankLinesAfterAnonymousClassHeader &&
                blankLinesBeforeFieldInInterface == that.blankLinesBeforeFieldInInterface &&
                blankLinesBeforeFieldWithoutAnnotations == that.blankLinesBeforeFieldWithoutAnnotations &&
                blankLinesBeforeFieldWithAnnotations == that.blankLinesBeforeFieldWithAnnotations &&
                blankLinesAroundMethodInInterface == that.blankLinesAroundMethodInInterface &&
                blankLinesAroundMethod == that.blankLinesAroundMethod &&
                blankLinesBeforeMethodBody == that.blankLinesBeforeMethodBody &&
                blankLinesAroundInitializer == that.blankLinesAroundInitializer &&
                blankLinesBetweenRecordComponents == that.blankLinesBetweenRecordComponents &&
                alignParamDescriptions == that.alignParamDescriptions &&
                alignThrownExceptions == that.alignThrownExceptions &&
                enableJavaDocFormatting == that.enableJavaDocFormatting &&
                formatComments == that.formatComments &&
                blankLinesAfterDescription == that.blankLinesAfterDescription &&
                blankLinesAfterParamDescriptions == that.blankLinesAfterParamDescriptions &&
                blankLinesAfterReturnTag == that.blankLinesAfterReturnTag &&
                keepInvalidTags == that.keepInvalidTags &&
                keepEmptyParamTags == that.keepEmptyParamTags &&
                keepEmptyReturnTags == that.keepEmptyReturnTags &&
                keepEmptyThrowsTags == that.keepEmptyThrowsTags &&
                wrapAtRightMargin == that.wrapAtRightMargin &&
                enableLeadingAsterisks == that.enableLeadingAsterisks &&
                useThrowsRatherThanException == that.useThrowsRatherThanException &&
                generatePOnEmptyLines == that.generatePOnEmptyLines &&
                keepEmptyLines == that.keepEmptyLines &&
                doNotWrapOneLineComments == that.doNotWrapOneLineComments &&
                preserveLineFeeds == that.preserveLineFeeds &&
                paramDescriptionsOnNewLine == that.paramDescriptionsOnNewLine &&
                indentContinuationLines == that.indentContinuationLines &&
                useSingleClassImport == that.useSingleClassImport &&
                useFullyQualifiedClassNames == that.useFullyQualifiedClassNames &&
                insertInnerClassImports == that.insertInnerClassImports &&
                Objects.equals(excludedInnerClasses, that.excludedInnerClasses) &&
                doNotSeparateModuleImports == that.doNotSeparateModuleImports &&
                deleteUnusedModuleImports == that.deleteUnusedModuleImports &&
                Objects.equals(useFqNamesInJavadoc, that.useFqNamesInJavadoc) &&
                classCountToUseImportOnDemand == that.classCountToUseImportOnDemand &&
                namesCountToUseStaticImportOnDemand == that.namesCountToUseStaticImportOnDemand &&
                Objects.equals(packagesToUseImportOnDemand, that.packagesToUseImportOnDemand) &&
                placeOnDemandImportBeforeSingleClassImports == that.placeOnDemandImportBeforeSingleClassImports &&
                layoutStaticImportsSeparately == that.layoutStaticImportsSeparately &&
                Objects.equals(importLayout, that.importLayout) &&
                keepGettersAndSettersTogether == that.keepGettersAndSettersTogether &&
                keepOverriddenMethodsTogether == that.keepOverriddenMethodsTogether &&
                Objects.equals(overriddenMethodsOrder, that.overriddenMethodsOrder) &&
                keepDependentMethodsTogether == that.keepDependentMethodsTogether &&
                Objects.equals(dependentMethodsOrder, that.dependentMethodsOrder) &&
                Objects.equals(matchingRules, that.matchingRules) &&
                preferLongerNames == that.preferLongerNames &&
                Objects.equals(fieldPrefix, that.fieldPrefix) &&
                Objects.equals(fieldSuffix, that.fieldSuffix) &&
                Objects.equals(staticFieldPrefix, that.staticFieldPrefix) &&
                Objects.equals(staticFieldSuffix, that.staticFieldSuffix) &&
                Objects.equals(parameterPrefix, that.parameterPrefix) &&
                Objects.equals(parameterSuffix, that.parameterSuffix) &&
                Objects.equals(localVariablePrefix, that.localVariablePrefix) &&
                Objects.equals(localVariableSuffix, that.localVariableSuffix) &&
                Objects.equals(subclassPrefix, that.subclassPrefix) &&
                Objects.equals(subclassSuffix, that.subclassSuffix) &&
                Objects.equals(testClassPrefix, that.testClassPrefix) &&
                Objects.equals(testClassSuffix, that.testClassSuffix) &&
                Objects.equals(defaultVisibility, that.defaultVisibility) &&
                makeGeneratedLocalsFinal == that.makeGeneratedLocalsFinal &&
                makeGeneratedParametersFinal == that.makeGeneratedParametersFinal &&
                useVarForLocalVariables == that.useVarForLocalVariables &&
                lineCommentAtFirstColumn == that.lineCommentAtFirstColumn &&
                addSpaceAtLineCommentStart == that.addSpaceAtLineCommentStart &&
                enforceOnReformat == that.enforceOnReformat &&
                blockCommentAtFirstColumn == that.blockCommentAtFirstColumn &&
                addSpacesAroundBlockComments == that.addSpacesAroundBlockComments &&
                insertOverride == that.insertOverride &&
                repeatSynchronized == that.repeatSynchronized &&
                Objects.equals(annotationsToCopy, that.annotationsToCopy) &&
                useExternalAnnotations == that.useExternalAnnotations &&
                insertTypeUseAnnotationsBeforeType == that.insertTypeUseAnnotationsBeforeType &&
                useClassIsInstanceAndCast == that.useClassIsInstanceAndCast &&
                replaceNullCheckWithObjectsNonNull == that.replaceNullCheckWithObjectsNonNull &&
                useIntegerSumWhenPossible == that.useIntegerSumWhenPossible &&
                Objects.equals(entityEjbClassPrefix, that.entityEjbClassPrefix) &&
                Objects.equals(entityEjbClassSuffix, that.entityEjbClassSuffix) &&
                Objects.equals(entityHomeInterfacePrefix, that.entityHomeInterfacePrefix) &&
                Objects.equals(entityHomeInterfaceSuffix, that.entityHomeInterfaceSuffix) &&
                Objects.equals(entityRemoteInterfacePrefix, that.entityRemoteInterfacePrefix) &&
                Objects.equals(entityRemoteInterfaceSuffix, that.entityRemoteInterfaceSuffix) &&
                Objects.equals(entityLocalHomeInterfacePrefix, that.entityLocalHomeInterfacePrefix) &&
                Objects.equals(entityLocalHomeInterfaceSuffix, that.entityLocalHomeInterfaceSuffix) &&
                Objects.equals(entityLocalInterfacePrefix, that.entityLocalInterfacePrefix) &&
                Objects.equals(entityLocalInterfaceSuffix, that.entityLocalInterfaceSuffix) &&
                Objects.equals(entityEjbNameTagPrefix, that.entityEjbNameTagPrefix) &&
                Objects.equals(entityEjbNameTagSuffix, that.entityEjbNameTagSuffix) &&
                Objects.equals(entityTransferObjectPrefix, that.entityTransferObjectPrefix) &&
                Objects.equals(entityTransferObjectSuffix, that.entityTransferObjectSuffix) &&
                Objects.equals(entityDefaultPkClass, that.entityDefaultPkClass) &&
                Objects.equals(sessionEjbClassPrefix, that.sessionEjbClassPrefix) &&
                Objects.equals(sessionEjbClassSuffix, that.sessionEjbClassSuffix) &&
                Objects.equals(sessionHomeInterfacePrefix, that.sessionHomeInterfacePrefix) &&
                Objects.equals(sessionHomeInterfaceSuffix, that.sessionHomeInterfaceSuffix) &&
                Objects.equals(sessionRemoteInterfacePrefix, that.sessionRemoteInterfacePrefix) &&
                Objects.equals(sessionRemoteInterfaceSuffix, that.sessionRemoteInterfaceSuffix) &&
                Objects.equals(sessionLocalHomeInterfacePrefix, that.sessionLocalHomeInterfacePrefix) &&
                Objects.equals(sessionLocalHomeInterfaceSuffix, that.sessionLocalHomeInterfaceSuffix) &&
                Objects.equals(sessionLocalInterfacePrefix, that.sessionLocalInterfacePrefix) &&
                Objects.equals(sessionLocalInterfaceSuffix, that.sessionLocalInterfaceSuffix) &&
                Objects.equals(sessionServiceEndpointPrefix, that.sessionServiceEndpointPrefix) &&
                Objects.equals(sessionServiceEndpointSuffix, that.sessionServiceEndpointSuffix) &&
                Objects.equals(sessionEjbNameTagPrefix, that.sessionEjbNameTagPrefix) &&
                Objects.equals(sessionEjbNameTagSuffix, that.sessionEjbNameTagSuffix) &&
                Objects.equals(servletClassPrefix, that.servletClassPrefix) &&
                Objects.equals(servletClassSuffix, that.servletClassSuffix) &&
                Objects.equals(servletNameTagPrefix, that.servletNameTagPrefix) &&
                Objects.equals(servletNameTagSuffix, that.servletNameTagSuffix) &&
                Objects.equals(mdbEjbClassPrefix, that.mdbEjbClassPrefix) &&
                Objects.equals(mdbEjbClassSuffix, that.mdbEjbClassSuffix) &&
                Objects.equals(mdbEjbNameTagPrefix, that.mdbEjbNameTagPrefix) &&
                Objects.equals(mdbEjbNameTagSuffix, that.mdbEjbNameTagSuffix) &&
                Objects.equals(filterClassPrefix, that.filterClassPrefix) &&
                Objects.equals(filterClassSuffix, that.filterClassSuffix) &&
                Objects.equals(filterNameTagPrefix, that.filterNameTagPrefix) &&
                Objects.equals(filterNameTagSuffix, that.filterNameTagSuffix) &&
                Objects.equals(listenerClassPrefix, that.listenerClassPrefix) &&
                Objects.equals(listenerClassSuffix, that.listenerClassSuffix) &&
                Objects.equals(entityBeanSuffix, that.entityBeanSuffix) &&
                Objects.equals(sessionBeanSuffix, that.sessionBeanSuffix) &&
                Objects.equals(messageDrivenBeanSuffix, that.messageDrivenBeanSuffix) &&
                Objects.equals(daoSuffix, that.daoSuffix) &&
                Objects.equals(serviceSuffix, that.serviceSuffix) &&
                Objects.equals(serviceImplSuffix, that.serviceImplSuffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useTabCharacter, smartTabs, tabSize, indent, continuationIndent, hardWrapAt, classBracePlacement);
    }
}
