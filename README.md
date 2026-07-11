# ✦ Lumina

*Write Java in a warmer light.*

Lumina is a lightweight, IntelliJ-inspired Java IDE built with **Java 25**, **JavaFX 25**, and **Maven**. Its identity is a warm amber accent on deep slate — a deliberate step away from the usual grey-on-grey editor look.

## Phase 6 features (new)

- **Git blame annotations** — Git → Toggle Blame Annotations (Ctrl/Cmd+Alt+B): every line's gutter shows `7/9/26  firoze-hossain` like IntelliJ's Annotate; hover a line for the commit message. Shows nothing (with a console note) when the file isn't in a git repo yet.
- **Find Usages** — Alt+F7 on the caret word, or **Ctrl/Cmd+Click smartly**: clicking a *declaration* opens the usages popup (★ marks the declaration, count shown, click to jump); clicking a *usage* jumps to the declaration. Ctrl/Cmd+hover shows a hand cursor.
- **Select Opened File** — ◎ button in the Project panel header (like IntelliJ's crosshair): expands the tree to the file in the active editor tab and selects it.
- **Testing like IDEA** —
  - **Run All Tests** (Ctrl/Cmd+Shift+T) → `mvn test` / `gradle test`
  - **Run Current Test Class** → `mvn -Dtest=Name test` / `gradle test --tests fqcn`; pressing ▶ on any class under `src/test/java` automatically runs it as a test
  - **Code → Generate Test for Current Class** → creates `XTest.java` in the mirrored `src/test/java` package — plain JUnit 5 skeleton, or `@SpringBootTest` with `contextLoads()` when the class is a Spring Boot application
  - New plain-Java projects now ship with JUnit 5 + Surefire (Maven) / `useJUnitPlatform()` (Gradle), so tests run out of the box

## Phase 5 features

- **Search Everywhere** — press **Shift twice** (or Ctrl/Cmd+Shift+A, or the 🔍 toolbar button): one popup with **All / Classes / Files / Symbols / Actions / Text** tabs like IntelliJ; symbols (classes + methods) are indexed in the background; Actions runs IDE commands (Run, Debug, Commit, panels…); Tab cycles categories
- **Go to Declaration** — **Ctrl/Cmd+Click** any identifier in the editor (or Ctrl/Cmd+B on the caret word): types resolve to their class file, methods to their declaration line, preferring the current file
- **Richer highlighting** — qualifier/field references now colored (soft purple), on top of the IntelliJ palette
- **Visible Debug action** — labelled **Debug 🐞** button in the toolbar next to ▶ (also Run → Debug, Ctrl/Cmd+D)
- **GitHub sign-in (real auth)** — 👤 toolbar button opens a dialog: browser opens a pre-filled token page, paste the token, Lumina verifies it against api.github.com and shows your username. Push / Pull / Fetch / Clone then authenticate automatically (via a GIT_ASKPASS helper — the token never enters git config or remote URLs). Click the button again for profile / sign-out.
- **Popup polish** — Search Everywhere / Go to File / Find in Files close on Esc from anywhere or when clicking outside, like IntelliJ
- **Syntax color fix** — token colors were being overridden by the base editor text rule (CSS specificity); all token selectors now scoped under `.code-area`, so the IntelliJ palette actually renders

## Phase 4 features

- **IntelliJ-style highlighting** — retuned palette (orange keywords, green strings, blue method calls, teal numbers, purple constants) plus XML highlighting for `pom.xml`, `.fxml`, `.html`, and friends
- **Maven tool window** — right-side panel (toolbar ▥ button or View → Tool Windows → Maven / Build) listing lifecycle goals; double-click runs them; switches to Gradle tasks for Gradle projects
- **Database tool window** — JDBC client with bundled PostgreSQL & MySQL drivers: connect, browse tables (double-click for rows), run ad-hoc SQL with a results grid; connection URL/user remembered
- **Find in Files** — Ctrl/Cmd+Shift+F, live search across the project with jump-to-line
- **Debug launch** — Debug button/menu (Ctrl/Cmd+D) starts any run configuration with JDWP suspended on port 5005 and auto-attaches `jdb` in the built-in Terminal (`stop in pkg.Class.method`, `cont`, `step`, `locals`); attach IntelliJ/VS Code remote debug to 5005 works too
- **Git: Clone Repository…** — clone by URL and auto-open when done
- **Last project restore** — reopens your last project on startup (unless you used File → Close Project); stored in `~/.lumina/lumina.properties`

## Phase 3 features

- **Full IntelliJ-style menu bar** — File, Edit, View, Navigate, Code, Refactor, Build, Run, Git, Tools, Window, Help
- **Run any project** — toolbar dropdown auto-detects run configurations:
  - Spring Boot (Maven `spring-boot:run` or Gradle `bootRun`, wrapper-aware: uses `./mvnw` / `gradlew` when present, `cmd /c` on Windows)
  - Plain Maven/Gradle: ▶ on a class compiles the whole project and runs that class on the project classpath (so cross-file imports work)
  - Falls back to the single-file source launcher outside build projects
- **Git integration** — branch chip in the toolbar (click to list & switch branches, like IntelliJ), Git menu with Init, Commit…, Push, Pull, Fetch, Status, New Branch…, "Sign in to GitHub" (opens browser), "Open Repository on GitHub" (parses origin URL)
- **Built-in Terminal** — bottom tool window next to Run (Ctrl/Cmd+T, Tools menu, icon rail); real system shell (bash/zsh/cmd) with command history (↑/↓); note: piped streams, so TUI apps like vim need an external terminal
- **IntelliJ-style project tree** — package chains flattened (`dev.lumina`), dimmed project path on the root node, `.class` ⚙ icons
- **Open .class files** — disassembled with `javap -p -c`, shown read-only
- **Navigate** — Go to File… (Ctrl/Cmd+P type-ahead popup), Go to Line… (Ctrl/Cmd+G)
- **Code** — Toggle Line Comment (Ctrl/Cmd+/); **Refactor** — Rename File; **Build** — Build/Clean Project

## Phase 2 features

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
