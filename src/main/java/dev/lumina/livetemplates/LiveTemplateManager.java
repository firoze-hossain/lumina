package dev.lumina.livetemplates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Central manager for IntelliJ IDEA-style Live Templates in Lumina.
 * Dynamically handles loading, built-in catalog registration, user customizations,
 * file persistence at ~/.lumina/live-templates.json, and template expansion.
 */
public class LiveTemplateManager {

    private static final Path CONFIG_PATH = Path.of(
            System.getProperty("user.home"), ".lumina", "live-templates.json"
    );

    private static final LiveTemplateManager INSTANCE = new LiveTemplateManager();

    private String defaultExpandWith = "Tab";
    private final List<LiveTemplateGroup> groups = new ArrayList<>();
    private final List<LiveTemplateGroup> factoryDefaults = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static LiveTemplateManager getInstance() {
        return INSTANCE;
    }

    private LiveTemplateManager() {
        initializeFactoryDefaults();
        load();
    }

    public synchronized String getDefaultExpandWith() {
        return defaultExpandWith;
    }

    public synchronized void setDefaultExpandWith(String defaultExpandWith) {
        this.defaultExpandWith = defaultExpandWith != null ? defaultExpandWith : "Tab";
    }

    public synchronized List<LiveTemplateGroup> getGroups() {
        return groups;
    }

    public synchronized List<LiveTemplateGroup> copyGroups() {
        List<LiveTemplateGroup> list = new ArrayList<>();
        for (LiveTemplateGroup g : groups) {
            list.add(g.copy());
        }
        return list;
    }

    public synchronized LiveTemplateGroup findGroup(String name) {
        if (name == null) return null;
        for (LiveTemplateGroup g : groups) {
            if (name.equalsIgnoreCase(g.getName())) {
                return g;
            }
        }
        return null;
    }

    public synchronized LiveTemplate findTemplate(String groupName, String abbreviation) {
        LiveTemplateGroup g = findGroup(groupName);
        if (g != null) {
            return g.findTemplate(abbreviation);
        }
        return null;
    }

    public synchronized LiveTemplate findTemplateByAbbrev(String abbreviation) {
        if (abbreviation == null) return null;
        for (LiveTemplateGroup g : groups) {
            if (!g.isEnabled()) continue;
            for (LiveTemplate t : g.getTemplates()) {
                if (t.isEnabled() && abbreviation.equals(t.getAbbreviation())) {
                    return t;
                }
            }
        }
        return null;
    }

    public synchronized void addGroup(LiveTemplateGroup group) {
        if (group != null && findGroup(group.getName()) == null) {
            groups.add(group);
            save();
        }
    }

    public synchronized boolean removeGroup(LiveTemplateGroup group) {
        if (group == null) return false;
        boolean removed = groups.removeIf(g -> g.getName().equalsIgnoreCase(group.getName()));
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized void addTemplate(String groupName, LiveTemplate template) {
        if (template == null) return;
        LiveTemplateGroup g = findGroup(groupName);
        if (g == null) {
            g = new LiveTemplateGroup(groupName, false);
            groups.add(g);
        }
        template.setGroupId(g.getName());
        g.getTemplates().add(template);
        save();
    }

    public synchronized boolean removeTemplate(LiveTemplate template) {
        if (template == null) return false;
        boolean removed = false;
        for (LiveTemplateGroup g : groups) {
            if (g.getTemplates().removeIf(t -> t.getId().equals(template.getId()))) {
                removed = true;
                break;
            }
        }
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized LiveTemplate duplicateTemplate(LiveTemplate source) {
        if (source == null) return null;
        LiveTemplate copy = source.copy();
        copy.setId(UUID.randomUUID().toString());
        copy.setBuiltin(false);
        copy.setAbbreviation(source.getAbbreviation() + "_copy");
        copy.setDescription(source.getDescription() + " (Copy)");

        LiveTemplateGroup g = findGroup(source.getGroupId());
        if (g != null) {
            int idx = g.getTemplates().indexOf(source);
            if (idx >= 0) {
                g.getTemplates().add(idx + 1, copy);
            } else {
                g.getTemplates().add(copy);
            }
            save();
        }
        return copy;
    }

    public synchronized boolean revertTemplate(LiveTemplate template) {
        if (template == null) return false;
        for (LiveTemplateGroup fg : factoryDefaults) {
            for (LiveTemplate ft : fg.getTemplates()) {
                if (ft.getGroupId().equalsIgnoreCase(template.getGroupId())
                        && ft.getAbbreviation().equals(template.getAbbreviation())) {
                    LiveTemplateGroup currentGroup = findGroup(template.getGroupId());
                    if (currentGroup != null) {
                        int idx = currentGroup.getTemplates().indexOf(template);
                        if (idx >= 0) {
                            currentGroup.getTemplates().set(idx, ft.copy());
                            save();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public synchronized boolean revertGroup(LiveTemplateGroup group) {
        if (group == null) return false;
        for (LiveTemplateGroup fg : factoryDefaults) {
            if (fg.getName().equalsIgnoreCase(group.getName())) {
                int idx = groups.indexOf(group);
                if (idx >= 0) {
                    groups.set(idx, fg.copy());
                    save();
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void apply(List<LiveTemplateGroup> newGroups, String newDefaultExpand) {
        this.defaultExpandWith = newDefaultExpand != null ? newDefaultExpand : "Tab";
        this.groups.clear();
        for (LiveTemplateGroup g : newGroups) {
            this.groups.add(g.copy());
        }
        save();
    }

    public synchronized boolean isModified(List<LiveTemplateGroup> currentGroups, String currentDefaultExpand) {
        if (!Objects.equals(this.defaultExpandWith, currentDefaultExpand)) {
            return true;
        }
        if (this.groups.size() != currentGroups.size()) {
            return true;
        }
        for (int i = 0; i < this.groups.size(); i++) {
            if (!this.groups.get(i).isEquivalentTo(currentGroups.get(i))) {
                return true;
            }
        }
        return false;
    }

    public synchronized void resetToDefaults() {
        this.defaultExpandWith = "Tab";
        this.groups.clear();
        for (LiveTemplateGroup g : factoryDefaults) {
            this.groups.add(g.copy());
        }
        save();
    }

    public synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            SavePayload payload = new SavePayload();
            payload.defaultExpandWith = this.defaultExpandWith;
            payload.groups = this.groups;
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                gson.toJson(payload, writer);
            }
        } catch (Exception e) {
            System.err.println("Could not save live templates: " + e.getMessage());
        }
    }

    public synchronized void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                SavePayload payload = gson.fromJson(reader, SavePayload.class);
                if (payload != null) {
                    if (payload.defaultExpandWith != null) {
                        this.defaultExpandWith = payload.defaultExpandWith;
                    }
                    if (payload.groups != null && !payload.groups.isEmpty()) {
                        this.groups.clear();
                        this.groups.addAll(payload.groups);
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not load live templates from file: " + e.getMessage());
            }
        }

        // Fallback to factory defaults
        resetToDefaults();
    }

    private static class SavePayload {
        String defaultExpandWith;
        List<LiveTemplateGroup> groups;
    }

    // ============================================================
    // Built-in Factory Templates Catalog (Matching IntelliJ Images)
    // ============================================================
    private void initializeFactoryDefaults() {
        factoryDefaults.clear();

        // 1. Angular
        LiveTemplateGroup angular = new LiveTemplateGroup("Angular");
        addT(angular, "@defer", "Surround with @defer block", "@defer {\n    $SELECTION$\n    $END$\n}", null, "HTML");
        addT(angular, "@else", "Surround with @else block", "@else {\n    $SELECTION$\n    $END$\n}", null, "HTML");
        addT(angular, "@else if", "Surround with @else if block", "@else if ($EXPR$) {\n    $SELECTION$\n    $END$\n}", null, "HTML");
        addT(angular, "@for", "Surround with @for block", "@for ($ITEM$ of $COLLECTION$; track $ID$) {\n    $SELECTION$\n    $END$\n}", null, "HTML");
        addT(angular, "@if", "Surround with @if block", "@if ($EXPR$) {\n    $SELECTION$\n    $END$\n}", null, "HTML");
        addT(angular, "@if + @else", "Surround with @if and @else block", "@if ($EXPR$) {\n    $SELECTION$\n} @else {\n    $END$\n}", null, "HTML");
        addT(angular, "@switch", "Surround with @switch", "@switch ($EXPR$) {\n    @case ($VAL$) {\n        $END$\n    }\n}", null, "HTML");
        addT(angular, "a-class", "Angular [class] binding", "[class]=\"$EXPR$\"$END$", null, "HTML");
        addT(angular, "a-component", "Angular component", "@Component({\n    selector: '$SELECTOR$',\n    templateUrl: './$NAME$.component.html',\n    styleUrls: ['./$NAME$.component.css']\n})\nexport class $CLASS_NAME$Component {\n    $END$\n}", null, "JAVASCRIPT");
        addT(angular, "a-common-inline", "Angular component with an inline template", "@Component({\n    selector: '$SELECTOR$',\n    template: `\n        $END$\n    `\n})\nexport class $CLASS_NAME$Component {\n}", null, "JAVASCRIPT");
        factoryDefaults.add(angular);

        // 2. Groovy
        LiveTemplateGroup groovy = new LiveTemplateGroup("Groovy");
        addT(groovy, "def", "Define variable", "def $NAME$ = $VALUE$$END$", null, "EVERYWHERE");
        addT(groovy, "class", "Class definition", "class $NAME$ {\n    $END$\n}", null, "EVERYWHERE");
        addT(groovy, "method", "Method definition", "def $NAME$($PARAMS$) {\n    $END$\n}", null, "EVERYWHERE");
        addT(groovy, "sout", "println", "println $EXPR$$END$", null, "EVERYWHERE");
        factoryDefaults.add(groovy);

        // 3. gRPC Request
        LiveTemplateGroup grpc = new LiveTemplateGroup("gRPC Request");
        addT(grpc, "unary", "Unary call", "GRPC /${PACKAGE}.${SERVICE}/${METHOD}\n{\n    $END$\n}", null, "EVERYWHERE");
        addT(grpc, "serverStream", "Server streaming", "GRPC /${PACKAGE}.${SERVICE}/${METHOD}\n{\n    $END$\n}", null, "EVERYWHERE");
        addT(grpc, "clientStream", "Client streaming", "GRPC /${PACKAGE}.${SERVICE}/${METHOD}\n{\n    $END$\n}", null, "EVERYWHERE");
        addT(grpc, "bidirectional", "Bidirectional streaming", "GRPC /${PACKAGE}.${SERVICE}/${METHOD}\n{\n    $END$\n}", null, "EVERYWHERE");
        factoryDefaults.add(grpc);

        // 4. HTML/XML
        LiveTemplateGroup htmlXml = new LiveTemplateGroup("HTML/XML");
        addT(htmlXml, "a", "Anchor tag", "<a href=\"$URL$\">$END$</a>", null, "HTML");
        addT(htmlXml, "div", "Div tag", "<div class=\"$CLASS$\">\n    $END$\n</div>", null, "HTML");
        addT(htmlXml, "img", "Image tag", "<img src=\"$SRC$\" alt=\"$ALT$\" />$END$", null, "HTML");
        addT(htmlXml, "input", "Input tag", "<input type=\"$TYPE$\" name=\"$NAME$\" value=\"$VALUE$\" />$END$", null, "HTML");
        addT(htmlXml, "script", "Script tag", "<script src=\"$SRC$\"></script>$END$", null, "HTML");
        addT(htmlXml, "style", "Style tag", "<style>\n    $END$\n</style>", null, "HTML");
        factoryDefaults.add(htmlXml);

        // 5. HTTP Request
        LiveTemplateGroup httpReq = new LiveTemplateGroup("HTTP Request");
        addT(httpReq, "GET", "GET request", "GET $URL$\nAccept: application/json\n\n$END$", null, "EVERYWHERE");
        addT(httpReq, "POST", "POST request", "POST $URL$\nContent-Type: application/json\n\n{\n    $END$\n}", null, "EVERYWHERE");
        addT(httpReq, "PUT", "PUT request", "PUT $URL$\nContent-Type: application/json\n\n{\n    $END$\n}", null, "EVERYWHERE");
        addT(httpReq, "DELETE", "DELETE request", "DELETE $URL$\n\n$END$", null, "EVERYWHERE");
        addT(httpReq, "PATCH", "PATCH request", "PATCH $URL$\nContent-Type: application/json\n\n{\n    $END$\n}", null, "EVERYWHERE");
        factoryDefaults.add(httpReq);

        // 6. Java (Images 1 & 5)
        LiveTemplateGroup java = new LiveTemplateGroup("Java");
        String compactMainSubgroup = "Instance 'main' methods for implicitly declared classes";
        addT(java, "main", "void main()", "void main(){\n    $END$\n}", compactMainSubgroup, LiveTemplateContext.JAVA_DECLARATION);
        addT(java, "maina", "void main(String[] args)", "void main(String[] args){\n    $END$\n}", compactMainSubgroup, LiveTemplateContext.JAVA_DECLARATION);
        addT(java, "psvm", "void main()", "void main(){\n    $END$\n}", compactMainSubgroup, LiveTemplateContext.JAVA_DECLARATION);
        addT(java, "psvma", "void main(String[] args)", "void main(String[] args){\n    $END$\n}", compactMainSubgroup, LiveTemplateContext.JAVA_DECLARATION);

        String normalMainSubgroup = "Instance 'main' methods for normal classes";
        addT(java, "main", "public static void main(String[] args)", "public static void main(String[] args) {\n    $END$\n}", normalMainSubgroup, LiveTemplateContext.JAVA_DECLARATION);
        addT(java, "psvm", "public static void main(String[] args)", "public static void main(String[] args) {\n    $END$\n}", normalMainSubgroup, LiveTemplateContext.JAVA_DECLARATION);

        addT(java, "C", "Surround with Callable", "java.util.concurrent.Callable<$RET$> callable = new java.util.concurrent.Callable<$RET$>() {\n    public $RET$ call() throws Exception {\n        $SELECTION$\n        $END$\n    }\n};", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "else-if", "Add else-if branch", "else if ($CONDITION$) {\n    $END$\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "fori", "Create iteration loop", "for(int $INDEX$ = 0; $INDEX$ < $LIMIT$; $INDEX$++) {\n    $END$\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "geti", "Inserts singleton method getInstance", "public static $CLASS$ getInstance() {\n    return $INSTANCE$;\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        addT(java, "I", "Iterate Iterable or array", "for ($TYPE$ $VAR$ : $ITERABLE$) {\n    $END$\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "itco", "Iterate elements of java.util.Collection", "for ($ITER$ = $COLLECTION$.iterator(); $ITER$.hasNext(); ) {\n    $TYPE$ $VAR$ = $CAST$$ITER$.next();\n    $END$\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "itli", "Iterate elements of java.util.List", "for (int $INDEX$ = 0; $INDEX$ < $LIST$.size(); $INDEX$++) {\n    $TYPE$ $VAR$ = $CAST$$LIST$.get($INDEX$);\n    $END$\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "sout", "System.out.println", "System.out.println($END$);", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "soutv", "System.out.println with variable", "System.out.println(\"$EXPR$ = \" + $EXPR$);", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "soutm", "System.out.println with class and method", "System.out.println(\"$CLASS$.$METHOD$\");", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "soutp", "System.out.println parameter values", "System.out.println($PARAMS$);", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "serr", "System.err.println", "System.err.println($END$);", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "lazy", "Performs lazy initialization", "if ($VAR$ == null) {\n    $VAR$ = new $TYPE$($END$);\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "inn", "Inserts if (expr != null)", "if ($EXPR$ != null) {\n    $END$\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        addT(java, "ifn", "Inserts if (expr == null)", "if ($EXPR$ == null) {\n    $END$\n}", null, LiveTemplateContext.JAVA_STATEMENT);
        factoryDefaults.add(java);

        // 7. JavaScript
        LiveTemplateGroup js = new LiveTemplateGroup("JavaScript");
        addT(js, "af", "Arrow function", "($PARAMS$) => {\n    $END$\n}", null, "JAVASCRIPT");
        addT(js, "cl", "console.log", "console.log($END$);", null, "JAVASCRIPT");
        addT(js, "fn", "Function declaration", "function $NAME$($PARAMS$) {\n    $END$\n}", null, "JAVASCRIPT");
        addT(js, "for", "for loop", "for (let $INDEX$ = 0; $INDEX$ < $ARRAY$.length; $INDEX$++) {\n    const $ELEMENT$ = $ARRAY$[$INDEX$];\n    $END$\n}", null, "JAVASCRIPT");
        addT(js, "fori", "for index loop", "for (let $INDEX$ = 0; $INDEX$ < $LIMIT$; $INDEX$++) {\n    $END$\n}", null, "JAVASCRIPT");
        addT(js, "if", "if statement", "if ($CONDITION$) {\n    $END$\n}", null, "JAVASCRIPT");
        factoryDefaults.add(js);

        // 8. JavaScript Testing
        LiveTemplateGroup jsTest = new LiveTemplateGroup("JavaScript Testing");
        addT(jsTest, "describe", "Describe suite block", "describe('$NAME$', () => {\n    $END$\n});", null, "JAVASCRIPT");
        addT(jsTest, "it", "Test case", "it('$SHOULD$', () => {\n    $END$\n});", null, "JAVASCRIPT");
        addT(jsTest, "test", "Test case", "test('$NAME$', () => {\n    $END$\n});", null, "JAVASCRIPT");
        addT(jsTest, "beforeEach", "beforeEach hook", "beforeEach(() => {\n    $END$\n});", null, "JAVASCRIPT");
        addT(jsTest, "afterEach", "afterEach hook", "afterEach(() => {\n    $END$\n});", null, "JAVASCRIPT");
        addT(jsTest, "expect", "expect assertion", "expect($ACTUAL$).toBe($EXPECTED$);$END$", null, "JAVASCRIPT");
        factoryDefaults.add(jsTest);

        // 9. Kotlin
        LiveTemplateGroup kotlin = new LiveTemplateGroup("Kotlin");
        addT(kotlin, "main", "main function", "fun main() {\n    $END$\n}", null, "KOTLIN");
        addT(kotlin, "sout", "println", "println($END$)", null, "KOTLIN");
        addT(kotlin, "soutv", "println with variable", "println(\"$VAR$ = $$VAR$\")", null, "KOTLIN");
        addT(kotlin, "for", "for loop", "for ($ITEM$ in $COLLECTION$) {\n    $END$\n}", null, "KOTLIN");
        addT(kotlin, "if", "if expression", "if ($CONDITION$) {\n    $END$\n}", null, "KOTLIN");
        addT(kotlin, "when", "when expression", "when ($EXPR$) {\n    $BRANCH$ -> $END$\n    else -> {}\n}", null, "KOTLIN");
        factoryDefaults.add(kotlin);

        // 10. Kubernetes
        LiveTemplateGroup k8s = new LiveTemplateGroup("Kubernetes");
        addT(k8s, "pod", "Pod definition", "apiVersion: v1\nkind: Pod\nmetadata:\n  name: $NAME$\nspec:\n  containers:\n  - name: $CONTAINER$\n    image: $IMAGE$\n    $END$", null, "YAML");
        addT(k8s, "service", "Service definition", "apiVersion: v1\nkind: Service\nmetadata:\n  name: $NAME$\nspec:\n  selector:\n    app: $APP$\n  ports:\n  - port: $PORT$\n    targetPort: $TARGET_PORT$\n    $END$", null, "YAML");
        addT(k8s, "deployment", "Deployment definition", "apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: $NAME$\nspec:\n  replicas: 1\n  selector:\n    matchLabels:\n      app: $APP$\n  template:\n    metadata:\n      labels:\n        app: $APP$\n    spec:\n      containers:\n      - name: $CONTAINER$\n        image: $IMAGE$\n        $END$", null, "YAML");
        addT(k8s, "configmap", "ConfigMap definition", "apiVersion: v1\nkind: ConfigMap\nmetadata:\n  name: $NAME$\ndata:\n  $KEY$: $VALUE$$END$", null, "YAML");
        addT(k8s, "secret", "Secret definition", "apiVersion: v1\nkind: Secret\nmetadata:\n  name: $NAME$\ntype: Opaque\ndata:\n  $KEY$: $BASE64_VALUE$$END$", null, "YAML");
        factoryDefaults.add(k8s);

        // 11. Maven
        LiveTemplateGroup maven = new LiveTemplateGroup("Maven");
        addT(maven, "dep", "Dependency definition", "<dependency>\n    <groupId>$GROUP$</groupId>\n    <artifactId>$ARTIFACT$</artifactId>\n    <version>$VERSION$</version>\n</dependency>$END$", null, "XML");
        addT(maven, "plugin", "Plugin definition", "<plugin>\n    <groupId>$GROUP$</groupId>\n    <artifactId>$ARTIFACT$</artifactId>\n    <version>$VERSION$</version>\n</plugin>$END$", null, "XML");
        addT(maven, "profile", "Profile definition", "<profile>\n    <id>$ID$</id>\n    $END$\n</profile>", null, "XML");
        addT(maven, "repo", "Repository definition", "<repository>\n    <id>$ID$</id>\n    <url>$URL$</url>\n</repository>$END$", null, "XML");
        factoryDefaults.add(maven);

        // 12. OpenAPI Specifications (.json)
        LiveTemplateGroup oasJson = new LiveTemplateGroup("OpenAPI Specifications (.json)");
        addT(oasJson, "openapi", "OpenAPI JSON root", "{\n  \"openapi\": \"3.0.3\",\n  \"info\": {\n    \"title\": \"$TITLE$\",\n    \"version\": \"1.0.0\"\n  },\n  \"paths\": {\n    $END$\n  }\n}", null, "JSON");
        addT(oasJson, "info", "Info section", "\"info\": {\n  \"title\": \"$TITLE$\",\n  \"version\": \"$VERSION$\"\n}$END$", null, "JSON");
        addT(oasJson, "paths", "Paths section", "\"paths\": {\n  \"/$PATH$\": {\n    \"get\": {\n      \"summary\": \"$SUMMARY$\",\n      \"responses\": {\n        \"200\": {\n          \"description\": \"OK\"\n        }\n      }\n    }\n  }\n}$END$", null, "JSON");
        factoryDefaults.add(oasJson);

        // 13. OpenAPI Specifications (.yaml)
        LiveTemplateGroup oasYaml = new LiveTemplateGroup("OpenAPI Specifications (.yaml)");
        addT(oasYaml, "openapi", "OpenAPI YAML root", "openapi: 3.0.3\ninfo:\n  title: $TITLE$\n  version: 1.0.0\npaths:\n  $END$", null, "YAML");
        addT(oasYaml, "info", "Info section", "info:\n  title: $TITLE$\n  version: $VERSION$$END$", null, "YAML");
        addT(oasYaml, "paths", "Paths section", "paths:\n  /$PATH$:\n    get:\n      summary: $SUMMARY$\n      responses:\n        '200':\n          description: OK$END$", null, "YAML");
        factoryDefaults.add(oasYaml);

        // 14. Qute
        LiveTemplateGroup qute = new LiveTemplateGroup("Qute");
        addT(qute, "if", "If expression", "{#if $CONDITION$}\n    $END$\n{/if}", null, "HTML");
        addT(qute, "for", "For loop", "{#for $ITEM$ in $LIST$}\n    $END$\n{/for}", null, "HTML");
        addT(qute, "let", "Let binding", "{#let $NAME$=$VAL$}\n    $END$\n{/let}", null, "HTML");
        addT(qute, "include", "Include template", "{#include $TEMPLATE$ /}$END$", null, "HTML");
        factoryDefaults.add(qute);

        // 15. React
        LiveTemplateGroup react = new LiveTemplateGroup("React");
        addT(react, "con", "Constructor with props argument", "constructor(props) {\n    super(props);\n    $END$\n}", null, "JAVASCRIPT");
        addT(react, "fsc", "React Flow arrow function component", "import React from 'react';\n\nconst $NAME$ = ($PROPS$) => {\n    return (\n        <div>\n            $END$\n        </div>\n    );\n};\n\nexport default $NAME$;", null, "JAVASCRIPT");
        addT(react, "fsf", "React Flow function component", "import React from 'react';\n\nfunction $NAME$($PROPS$) {\n    return (\n        <div>\n            $END$\n        </div>\n    );\n}\n\nexport default $NAME$;", null, "JAVASCRIPT");
        addT(react, "props", "this.props.", "this.props.$END$", null, "JAVASCRIPT");
        addT(react, "rcc", "React class component", "import React, { Component } from 'react';\n\nclass $NAME$ extends Component {\n    render() {\n        return (\n            <div>\n                $END$\n            </div>\n        );\n    }\n}\n\nexport default $NAME$;", null, "JAVASCRIPT");
        addT(react, "rccp", "React class component with PropTypes", "import React, { Component } from 'react';\nimport PropTypes from 'prop-types';\n\nclass $NAME$ extends Component {\n    render() {\n        return (\n            <div>\n                $END$\n            </div>\n        );\n    }\n}\n\n$NAME$.propTypes = {};\n\nexport default $NAME$;", null, "JAVASCRIPT");
        addT(react, "rcfc", "React class component with PropTypes and lifecycle methods", "import React, { Component } from 'react';\nimport PropTypes from 'prop-types';\n\nclass $NAME$ extends Component {\n    componentDidMount() {}\n    render() {\n        return (\n            <div>\n                $END$\n            </div>\n        );\n    }\n}\n\nexport default $NAME$;", null, "JAVASCRIPT");
        factoryDefaults.add(react);

        // 16. React hooks
        LiveTemplateGroup reactHooks = new LiveTemplateGroup("React hooks");
        addT(reactHooks, "useState", "useState hook", "const [$STATE$, set$CAP_STATE$] = useState($DEFAULT$);$END$", null, "JAVASCRIPT");
        addT(reactHooks, "useEffect", "useEffect hook", "useEffect(() => {\n    $END$\n}, []);", null, "JAVASCRIPT");
        addT(reactHooks, "useContext", "useContext hook", "const $VAL$ = useContext($CONTEXT$);$END$", null, "JAVASCRIPT");
        addT(reactHooks, "useRef", "useRef hook", "const $REF$ = useRef($INIT$);$END$", null, "JAVASCRIPT");
        addT(reactHooks, "useMemo", "useMemo hook", "const $MEMO$ = useMemo(() => $CALC$, [$DEPS$]);$END$", null, "JAVASCRIPT");
        addT(reactHooks, "useCallback", "useCallback hook", "const $CB$ = useCallback(($PARAMS$) => {\n    $END$\n}, [$DEPS$]);", null, "JAVASCRIPT");
        factoryDefaults.add(reactHooks);

        // 17. Shell Script
        LiveTemplateGroup shell = new LiveTemplateGroup("Shell Script");
        addT(shell, "shebang", "Bash shebang", "#!/usr/bin/env bash\nset -euo pipefail\n\n$END$", null, "SHELL");
        addT(shell, "if", "if statement", "if [ $CONDITION$ ]; then\n    $END$\nfi", null, "SHELL");
        addT(shell, "for", "for loop", "for $ITEM$ in $LIST$; do\n    $END$\ndone", null, "SHELL");
        addT(shell, "while", "while loop", "while [ $CONDITION$ ]; do\n    $END$\ndone", null, "SHELL");
        addT(shell, "func", "Function declaration", "function $NAME$() {\n    $END$\n}", null, "SHELL");
        factoryDefaults.add(shell);

        // 18. Spring Coroutine Router DSL Kotlin
        LiveTemplateGroup springCo = new LiveTemplateGroup("Spring Coroutine Router DSL Kotlin");
        addT(springCo, "coRouter", "coRouter DSL block", "coRouter {\n    $END$\n}", null, "KOTLIN");
        addT(springCo, "GET", "GET route handler", "GET(\"$PATH$\", $HANDLER$)$END$", null, "KOTLIN");
        addT(springCo, "POST", "POST route handler", "POST(\"$PATH$\", $HANDLER$)$END$", null, "KOTLIN");
        factoryDefaults.add(springCo);

        // 19. Spring Java
        LiveTemplateGroup springJava = new LiveTemplateGroup("Spring Java");
        addT(springJava, "getmap", "@GetMapping method", "@GetMapping(\"$PATH$\")\npublic ResponseEntity<$TYPE$> $METHOD$() {\n    $END$\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        addT(springJava, "postmap", "@PostMapping method", "@PostMapping(\"$PATH$\")\npublic ResponseEntity<$TYPE$> $METHOD$(@RequestBody $REQ_TYPE$ request) {\n    $END$\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        addT(springJava, "restctrl", "@RestController class", "@RestController\n@RequestMapping(\"/api/$PATH$\")\npublic class $NAME$Controller {\n    $END$\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        addT(springJava, "srv", "@Service class", "@Service\npublic class $NAME$Service {\n    $END$\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        addT(springJava, "repo", "@Repository interface", "@Repository\npublic interface $NAME$Repository extends JpaRepository<$ENTITY$, $ID$> {\n    $END$\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        factoryDefaults.add(springJava);

        // 20. Spring Kotlin
        LiveTemplateGroup springKotlin = new LiveTemplateGroup("Spring Kotlin");
        addT(springKotlin, "getmap", "@GetMapping method", "@GetMapping(\"$PATH$\")\nfun $METHOD$(): ResponseEntity<$TYPE$> {\n    $END$\n}", null, "KOTLIN");
        addT(springKotlin, "postmap", "@PostMapping method", "@PostMapping(\"$PATH$\")\nfun $METHOD$(@RequestBody request: $REQ_TYPE$): ResponseEntity<$TYPE$> {\n    $END$\n}", null, "KOTLIN");
        addT(springKotlin, "restctrl", "@RestController class", "@RestController\n@RequestMapping(\"/api/$PATH$\")\nclass $NAME$Controller {\n    $END$\n}", null, "KOTLIN");
        factoryDefaults.add(springKotlin);

        // 21. Spring MVC Java
        LiveTemplateGroup springMvcJava = new LiveTemplateGroup("Spring MVC Java");
        addT(springMvcJava, "ctrl", "@Controller class", "@Controller\n@RequestMapping(\"/$PATH$\")\npublic class $NAME$Controller {\n    $END$\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        addT(springMvcJava, "modelAttr", "@ModelAttribute method", "@ModelAttribute(\"$NAME$\")\npublic $TYPE$ $METHOD$() {\n    return new $TYPE$();$END$\n}", null, LiveTemplateContext.JAVA_DECLARATION);
        factoryDefaults.add(springMvcJava);

        // 22. Spring MVC Kotlin
        LiveTemplateGroup springMvcKotlin = new LiveTemplateGroup("Spring MVC Kotlin");
        addT(springMvcKotlin, "ctrl", "@Controller class", "@Controller\n@RequestMapping(\"/$PATH$\")\nclass $NAME$Controller {\n    $END$\n}", null, "KOTLIN");
        factoryDefaults.add(springMvcKotlin);

        // 23. xsl
        LiveTemplateGroup xsl = new LiveTemplateGroup("xsl");
        addT(xsl, "apply", "xsl:apply-templates", "<xsl:apply-templates select=\"$SELECT$\" />$END$", null, "XML");
        addT(xsl, "call", "xsl:call-template", "<xsl:call-template name=\"$NAME$\" />$END$", null, "XML");
        addT(xsl, "choose", "xsl:choose block", "<xsl:choose>\n    <xsl:when test=\"$TEST$\">\n        $END$\n    </xsl:when>\n    <xsl:otherwise>\n    </xsl:otherwise>\n</xsl:choose>", null, "XML");
        addT(xsl, "value", "xsl:value-of", "<xsl:value-of select=\"$SELECT$\" />$END$", null, "XML");
        factoryDefaults.add(xsl);

        // 24. Zen CSS
        LiveTemplateGroup zenCss = new LiveTemplateGroup("Zen CSS");
        addT(zenCss, "bd", "border", "border: $WIDTH$ $STYLE$ $COLOR$;$END$", null, "CSS");
        addT(zenCss, "bg", "background", "background: $COLOR$;$END$", null, "CSS");
        addT(zenCss, "d", "display", "display: $VALUE$;$END$", null, "CSS");
        addT(zenCss, "fl", "float", "float: $VALUE$;$END$", null, "CSS");
        addT(zenCss, "fs", "font-size", "font-size: $SIZE$;$END$", null, "CSS");
        addT(zenCss, "m", "margin", "margin: $VALUE$;$END$", null, "CSS");
        addT(zenCss, "p", "padding", "padding: $VALUE$;$END$", null, "CSS");
        addT(zenCss, "w", "width", "width: $VALUE$;$END$", null, "CSS");
        addT(zenCss, "h", "height", "height: $VALUE$;$END$", null, "CSS");
        factoryDefaults.add(zenCss);

        // 25. Zen HTML
        LiveTemplateGroup zenHtml = new LiveTemplateGroup("Zen HTML");
        addT(zenHtml, "!", "HTML5 doctype template", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>$TITLE$</title>\n</head>\n<body>\n    $END$\n</body>\n</html>", null, "HTML");
        addT(zenHtml, "a", "Anchor tag", "<a href=\"$URL$\">$END$</a>", null, "HTML");
        addT(zenHtml, "btn", "Button element", "<button type=\"$TYPE$\">$END$</button>", null, "HTML");
        addT(zenHtml, "div", "Div tag", "<div class=\"$CLASS$\">\n    $END$\n</div>", null, "HTML");
        addT(zenHtml, "form", "Form element", "<form action=\"$ACTION$\" method=\"$METHOD$\">\n    $END$\n</form>", null, "HTML");
        factoryDefaults.add(zenHtml);

        // 26. Zen XSL (Image 4)
        LiveTemplateGroup zenXsl = new LiveTemplateGroup("Zen XSL");
        addT(zenXsl, "!!!", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "ap", "<xsl:apply-templates select=\"...\" mode=\"...\" />", "<xsl:apply-templates select=\"$SELECT$\" mode=\"$MODE$\" />$END$", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "api", "<xsl:apply-imports/>", "<xsl:apply-imports/>$END$", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "attr", "<xsl:attribute name=\"...\">...</xsl:attribute>", "<xsl:attribute name=\"$NAME$\">$END$</xsl:attribute>", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "cho", "<xsl:choose>...</xsl:choose>", "<xsl:choose>\n    <xsl:when test=\"$TEST$\">\n        $END$\n    </xsl:when>\n</xsl:choose>", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "cp", "<xsl:copy-of select=\"...\" />", "<xsl:copy-of select=\"$SELECT$\" />$END$", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "val", "<xsl:value-of select=\"...\" />", "<xsl:value-of select=\"$SELECT$\" />$END$", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "var", "<xsl:variable name=\"...\">...</xsl:variable>", "<xsl:variable name=\"$NAME$\">$END$</xsl:variable>", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "wh", "<xsl:when test=\"...\">...</xsl:when>", "<xsl:when test=\"$TEST$\">\n    $END$\n</xsl:when>", null, LiveTemplateContext.XML_XSL_TEXT);
        addT(zenXsl, "ot", "<xsl:otherwise>...</xsl:otherwise>", "<xsl:otherwise>\n    $END$\n</xsl:otherwise>", null, LiveTemplateContext.XML_XSL_TEXT);
        factoryDefaults.add(zenXsl);
    }

    private void addT(LiveTemplateGroup group, String abbrev, String desc, String text, String subgroup, String context) {
        LiveTemplate t = new LiveTemplate(abbrev, desc, text, subgroup);
        t.setGroupId(group.getName());
        t.setBuiltin(true);
        if (context != null) {
            t.getContexts().add(context);
        }

        // Auto-extract variables like $VAR$
        extractVariables(t);
        group.getTemplates().add(t);
    }

    public static void extractVariables(LiveTemplate t) {
        if (t == null || t.getTemplateText() == null) return;
        String text = t.getTemplateText();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\$([A-Za-z0-9_]+)\\$").matcher(text);
        Set<String> found = new LinkedHashSet<>();
        while (m.find()) {
            String name = m.group(1);
            if (!"END".equals(name) && !"SELECTION".equals(name)) {
                found.add(name);
            }
        }
        // Preserve existing variables if already present
        Map<String, LiveTemplateVariable> existing = new HashMap<>();
        for (LiveTemplateVariable v : t.getVariables()) {
            existing.put(v.getName(), v);
        }
        List<LiveTemplateVariable> updated = new ArrayList<>();
        for (String name : found) {
            if (existing.containsKey(name)) {
                updated.add(existing.get(name));
            } else {
                updated.add(new LiveTemplateVariable(name, "", "", false));
            }
        }
        t.setVariables(updated);
    }

    /**
     * Expands a template text with provided variable values.
     * Returns a tuple/record of expanded text and cursor offset.
     */
    public static class ExpansionResult {
        public final String text;
        public final int cursorOffset;

        public ExpansionResult(String text, int cursorOffset) {
            this.text = text;
            this.cursorOffset = cursorOffset;
        }
    }

    public ExpansionResult expand(LiveTemplate template, Map<String, String> values) {
        if (template == null) return new ExpansionResult("", 0);
        String text = template.getTemplateText();
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                text = text.replace("$" + entry.getKey() + "$", entry.getValue());
            }
        }
        // Defaults for remaining variables
        for (LiveTemplateVariable v : template.getVariables()) {
            String placeholder = "$" + v.getName() + "$";
            if (text.contains(placeholder)) {
                String val = v.getDefaultValue().isEmpty() ? v.getName() : v.getDefaultValue();
                text = text.replace(placeholder, val);
            }
        }

        int cursor = text.indexOf("$END$");
        if (cursor >= 0) {
            text = text.replace("$END$", "");
        } else {
            cursor = text.length();
        }
        return new ExpansionResult(text, cursor);
    }
}
