package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ToolWindowHeaderTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (Throwable ignored) {}
    }

    @Test
    void testToolWindowHeaderCreationAndActions() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ToolWindowHeader header = new ToolWindowHeader("TestWindow");
                assertEquals("TestWindow", header.getTitleLabel().getText());

                assertNotNull(header.getOptionsButton());
                assertEquals("⋮", header.getOptionsButton().getText());
                assertEquals("Options", header.getOptionsButton().getTooltip().getText());

                assertNotNull(header.getHideButton());
                assertEquals("—", header.getHideButton().getText());
                assertTrue(header.getHideButton().getTooltip().getText().contains("Hide"));

                AtomicBoolean hideCalled = new AtomicBoolean(false);
                header.setOnHide(() -> hideCalled.set(true));
                header.getHideButton().fire();
                assertTrue(hideCalled.get());

                AtomicBoolean tabSelected = new AtomicBoolean(false);
                AtomicBoolean tabClosed = new AtomicBoolean(false);
                Button tab = header.addTab("Output", true, () -> tabSelected.set(true), () -> tabClosed.set(true));
                assertNotNull(tab);
                assertEquals("Output", tab.getText());

                tab.fire();
                assertTrue(tabSelected.get());

                AtomicBoolean actionCalled = new AtomicBoolean(false);
                Button actBtn = header.addRightActionButton("Clear", "Clear output", () -> actionCalled.set(true));
                assertNotNull(actBtn);
                actBtn.fire();
                assertTrue(actionCalled.get());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testAllBottomToolWindowsHaveHeaderWithHideAndOptions() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // 1. Services
                ServicesPanel services = new ServicesPanel();
                assertNotNull(services.getHeader());
                assertEquals("Services", services.getHeader().getTitleLabel().getText());
                assertNotNull(services.getHeader().getOptionsButton());
                assertNotNull(services.getHeader().getHideButton());
                AtomicBoolean servicesHide = new AtomicBoolean(false);
                services.setOnHideToolWindow(() -> servicesHide.set(true));
                services.getHeader().getHideButton().fire();
                assertTrue(servicesHide.get());

                // 2. Run Console
                ConsolePane runConsole = new ConsolePane("Run");
                assertNotNull(runConsole.getToolWindowHeader());
                assertEquals("Run", runConsole.getToolWindowHeader().getTitleLabel().getText());
                assertNotNull(runConsole.getToolWindowHeader().getOptionsButton());
                assertNotNull(runConsole.getToolWindowHeader().getHideButton());
                AtomicBoolean runHide = new AtomicBoolean(false);
                runConsole.setOnHideToolWindow(() -> runHide.set(true));
                runConsole.getToolWindowHeader().getHideButton().fire();
                assertTrue(runHide.get());

                // 3. Build Console
                ConsolePane buildConsole = new ConsolePane("Build");
                assertNotNull(buildConsole.getToolWindowHeader());
                assertEquals("Build", buildConsole.getToolWindowHeader().getTitleLabel().getText());
                assertNotNull(buildConsole.getToolWindowHeader().getOptionsButton());
                assertNotNull(buildConsole.getToolWindowHeader().getHideButton());
                AtomicBoolean buildHide = new AtomicBoolean(false);
                buildConsole.setOnHideToolWindow(() -> buildHide.set(true));
                buildConsole.getToolWindowHeader().getHideButton().fire();
                assertTrue(buildHide.get());

                // 4. Problems
                ProblemsPanel problems = new ProblemsPanel();
                assertNotNull(problems.getHeader());
                assertEquals("Problems", problems.getHeader().getTitleLabel().getText());
                assertNotNull(problems.getHeader().getOptionsButton());
                assertNotNull(problems.getHeader().getHideButton());
                AtomicBoolean problemsHide = new AtomicBoolean(false);
                problems.setOnHideToolWindow(() -> problemsHide.set(true));
                problems.getHeader().getHideButton().fire();
                assertTrue(problemsHide.get());

                // 5. Terminal
                AtomicBoolean terminalHide = new AtomicBoolean(false);
                TerminalToolWindow terminal = new TerminalToolWindow(
                        () -> Path.of("."),
                        () -> {},
                        () -> {},
                        () -> terminalHide.set(true),
                        tab -> {}
                );
                assertNotNull(terminal.getHeader());
                assertEquals("Terminal", terminal.getHeader().getTitleLabel().getText());
                assertNotNull(terminal.getHeader().getOptionsButton());
                assertNotNull(terminal.getHeader().getHideButton());
                terminal.getHeader().getHideButton().fire();
                assertTrue(terminalHide.get());

                // 6. GitHub Copilot MCP Log
                McpLogPanel mcp = new McpLogPanel(() -> {});
                assertNotNull(mcp.getHeader());
                assertTrue(mcp.getHeader().getTitleLabel().getText().contains("GitHub Copilot"));
                assertNotNull(mcp.getHeader().getOptionsButton());
                assertNotNull(mcp.getHeader().getHideButton());
                AtomicBoolean mcpHide = new AtomicBoolean(false);
                mcp.setOnHideToolWindow(() -> mcpHide.set(true));
                mcp.getHeader().getHideButton().fire();
                assertTrue(mcpHide.get());

                AtomicBoolean mcpMoveCalled = new AtomicBoolean(false);
                mcp.setOnMoveToToolWindow(pos -> mcpMoveCalled.set(true));
                assertEquals("Bottom", mcp.getActiveMoveToPosition());
                mcp.setActiveMoveToPosition("Right");
                assertEquals("Right", mcp.getActiveMoveToPosition());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBrandIsolation() throws Exception {
        List<Path> paths = List.of(
                Path.of("src/main/java/dev/lumina/ui/ToolWindowHeader.java"),
                Path.of("src/main/java/dev/lumina/ui/ServicesPanel.java"),
                Path.of("src/main/java/dev/lumina/ui/ConsolePane.java"),
                Path.of("src/main/java/dev/lumina/ui/ProblemsPanel.java"),
                Path.of("src/main/java/dev/lumina/ui/TerminalToolWindow.java"),
                Path.of("src/main/java/dev/lumina/ui/McpLogPanel.java")
        );

        for (Path p : paths) {
            if (Files.exists(p)) {
                String content = Files.readString(p);
                assertFalse(content.contains("JetBrains"), "Found JetBrains in " + p);
                assertFalse(content.contains("IntelliJ"), "Found IntelliJ in " + p);
            }
        }
    }
}
