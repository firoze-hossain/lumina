package dev.lumina.debugger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DebuggerServiceTest {

    private DebuggerService debugger;

    @BeforeEach
    void setUp() {
        debugger = new DebuggerService();
    }

    @Test
    void testBreakpointRegistration() {
        DebugBreakpoint bp1 = new DebugBreakpoint("com.roze.nexacommerce.user.controller.AuthenticationController", 24);
        DebugBreakpoint bp2 = new DebugBreakpoint("com.roze.nexacommerce.user.service.impl.AuthenticationServiceImpl", 48);

        assertEquals("AuthenticationController:24", bp1.displayLabel());
        assertEquals("AuthenticationServiceImpl:48", bp2.displayLabel());

        debugger.addBreakpoint(bp1.fqcn(), bp1.line());
        debugger.addBreakpoint(bp2.fqcn(), bp2.line());

        debugger.removeBreakpoint(bp1.fqcn(), bp1.line());
        // bp2 should still be registered
    }

    @Test
    void testStackFrameFormatting() {
        DebugStackFrame frame = new DebugStackFrame(
                0,
                "login",
                24,
                "com.roze.nexacommerce.user.controller.AuthenticationController",
                "com.roze.nexacommerce.user.controller",
                "AuthenticationController.java",
                false,
                null
        );

        assertEquals("login:24, AuthenticationController (com.roze.nexacommerce.user.controller)", frame.formattedLabel());
        assertEquals("AuthenticationController", frame.simpleClassName());

        DebugStackFrame hiddenFrame = new DebugStackFrame(
                67,
                "doFilter",
                100,
                "org.apache.tomcat.FilterChain",
                "org.apache.tomcat",
                "FilterChain.java",
                true,
                null
        );
        assertEquals("67 hidden frames", hiddenFrame.formattedLabel());
    }

    @Test
    void testVariableHierarchy() {
        DebugVariable req = new DebugVariable("request", "LoginRequest@19222", "\"LoginRequest(email=superadmin@nexacommerce.com)\"", "param");
        req.addChild(new DebugVariable("email", "String", "\"superadmin@nexacommerce.com\"", "field"));
        req.addChild(new DebugVariable("password", "String", "\"firoze28\"", "field"));

        assertEquals("request = \"LoginRequest(email=superadmin@nexacommerce.com)\"", req.displayText());
        assertEquals(2, req.children().size());
        assertEquals("email = \"superadmin@nexacommerce.com\"", req.children().get(0).displayText());
        assertEquals("password = \"firoze28\"", req.children().get(1).displayText());

        DebugVariable thisVar = new DebugVariable("this", "AuthenticationServiceImpl@19223", "{AuthenticationServiceImpl@19223}", "this");
        assertEquals("this = {AuthenticationServiceImpl@19223}", thisVar.displayText());
    }

    @Test
    void testSimulatedBreakpointHitAndStep() {
        AtomicBoolean hitFired = new AtomicBoolean(false);
        AtomicReference<String> stoppedMethod = new AtomicReference<>();
        AtomicReference<Integer> stoppedLine = new AtomicReference<>();

        debugger.addListener(new DebuggerService.DebugListener() {
            @Override
            public void onStateChanged(DebuggerService.State newState) {}

            @Override
            public void onBreakpointHit(DebugThread thread, List<DebugStackFrame> frames, List<DebugVariable> variables, String sourceFile, int line) {
                hitFired.set(true);
                stoppedMethod.set(frames.get(0).methodName());
                stoppedLine.set(line);
            }

            @Override
            public void onStepped(DebugThread thread, List<DebugStackFrame> frames, List<DebugVariable> variables, String sourceFile, int line) {
                stoppedMethod.set(frames.get(0).methodName());
                stoppedLine.set(line);
            }

            @Override
            public void onVmResumed() {}

            @Override
            public void onDisconnected() {}

            @Override
            public void onLog(String message) {}
        });

        List<DebugVariable> vars = new ArrayList<>();
        vars.add(new DebugVariable("request", "LoginRequest", "LoginRequest(...)", "param"));

        // Simulate hit at AuthenticationController:24
        debugger.simulateBreakpointHit(
                "http-nio-8092-exec-1",
                "com.roze.nexacommerce.user.controller.AuthenticationController",
                "login",
                24,
                "AuthenticationController.java",
                vars
        );

        // RunLater tasks execute immediately in non-FX test runner or via direct invocation
        assertEquals(DebuggerService.State.PAUSED, debugger.getState());

        // Simulate step into AuthenticationServiceImpl:48
        debugger.simulateStep(
                "http-nio-8092-exec-1",
                "com.roze.nexacommerce.user.service.impl.AuthenticationServiceImpl",
                "login",
                48,
                "AuthenticationServiceImpl.java",
                vars
        );
        assertEquals(DebuggerService.State.PAUSED, debugger.getState());
    }
}
