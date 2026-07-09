# ✦ Lumina

*Write Java in a warmer light.*

Lumina is a lightweight, IntelliJ-inspired Java IDE built with **Java 25**, **JavaFX 25**, and **Maven**. Its identity is a warm amber accent on deep slate — a deliberate step away from the usual grey-on-grey editor look.

## Phase 2 features (new)

- **IntelliJ-style shell** — left icon rail (toggle Project/Console, Run, New Project), toolbar with project chip + run/stop controls, breadcrumb status bar
- **New Project wizard** (`Ctrl/Cmd+Shift+N`) — generator list left, form right, just like IntelliJ:
  - **Java** — generated locally: Maven *or* Gradle layout, package structure, Main class, .gitignore
  - **Spring Boot** — real project downloaded from start.spring.io (needs internet), Maven or Gradle, dependency list (e.g. `web,data-jpa,lombok`)
  - Group/Artifact/Package auto-derived from name, optional `git init`
- **File → New submenu** — Project…, Java Class (package auto-inferred from src/main/java), Package, File — created in the folder selected in the project tree
- Works the same on macOS, Windows, and Linux (in-window menu bar everywhere)

## Phase 1 features

- **Project explorer** — open any folder; lazy-loading file tree with double-click to open
- **Tabbed code editor** — multiple files, dirty-state dot on unsaved tabs, line numbers
- **Java syntax highlighting** — keywords, types, strings (incl. text blocks), numbers, comments, annotations
- **Auto-indent** and 4-space soft tabs
- **One-click run** — `Ctrl/Cmd+R` saves and runs the current file via the JDK's single-file source launcher, streaming output live
- **Console** — output panel with Stop / Clear, toggleable from the View menu
- **Status bar** — full file path and live line:column caret position
- **Welcome screen** — quick actions when nothing is open
- Full keyboard shortcuts (New, Open, Save, Undo/Redo, Close Tab, Run, Stop)

## Requirements

- JDK **25** (any distribution: Temurin, Oracle, Zulu…)
- Maven **3.9+**

JavaFX and RichTextFX are pulled in automatically by Maven — no manual SDK setup.

## Run it

```bash
mvn clean javafx:run
```

## Project layout

```
lumina-ide/
├── pom.xml
└── src/main/
    ├── java/dev/lumina/
    │   ├── LuminaApp.java            # window shell, menus, actions
    │   ├── syntax/
    │   │   └── JavaSyntaxHighlighter.java
    │   └── ui/
    │       ├── EditorTab.java        # CodeArea + highlighting + dirty state
    │       ├── FileExplorer.java     # lazy file tree
    │       ├── ConsolePane.java      # run & stream process output
    │       └── WelcomeView.java
    └── resources/css/
        └── lumina-dark.css           # the whole visual identity
```

## Keyboard shortcuts

| Action        | Shortcut              |
|---------------|-----------------------|
| New file      | Ctrl/Cmd + N          |
| Open file     | Ctrl/Cmd + O          |
| Open folder   | Ctrl/Cmd + Shift + O  |
| Save          | Ctrl/Cmd + S          |
| Run file      | Ctrl/Cmd + R          |
| Stop process  | Ctrl/Cmd + F2         |
| Close tab     | Ctrl/Cmd + W          |

## Roadmap (later phases)

- Phase 2: find & replace, editor settings, light theme, recent projects
- Phase 3: Maven project awareness (compile whole project, run configurations)
- Phase 4: code completion & error highlighting via the Java compiler API / LSP
- Phase 5: integrated Git, debugger
