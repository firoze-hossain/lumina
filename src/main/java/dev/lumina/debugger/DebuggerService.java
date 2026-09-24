package dev.lumina.debugger;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;
import javafx.application.Platform;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * High-performance JDI (Java Debug Interface) engine managing breakpoint resolution,
 * execution flow control, call stacks, and variable inspections matching IntelliJ IDEA.
 */
public class DebuggerService {

    public enum State {
        IDLE, CONNECTING, RUNNING, PAUSED, TERMINATED
    }

    public interface DebugListener {
        void onStateChanged(State newState);
        void onBreakpointHit(DebugThread thread, List<DebugStackFrame> frames, List<DebugVariable> variables, String sourceFile, int line);
        void onStepped(DebugThread thread, List<DebugStackFrame> frames, List<DebugVariable> variables, String sourceFile, int line);
        void onVmResumed();
        void onDisconnected();
        void onLog(String message);
    }

    private State state = State.IDLE;
    private VirtualMachine vm;
    private Thread eventThread;
    private volatile boolean running = false;

    private final Set<DebugBreakpoint> registeredBreakpoints = ConcurrentHashMap.newKeySet();
    private final Map<DebugBreakpoint, BreakpointRequest> activeRequests = new ConcurrentHashMap<>();
    private final List<DebugListener> listeners = new CopyOnWriteArrayList<>();

    private ThreadReference currentSuspendedThread;
    private StackFrame currentActiveFrame;

    public DebuggerService() {}

    public void addListener(DebugListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DebugListener listener) {
        listeners.remove(listener);
    }

    public State getState() {
        return state;
    }

    public boolean isConnected() {
        return vm != null && state != State.IDLE && state != State.TERMINATED;
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    private void safeRunLater(Runnable r) {
        try {
            Platform.runLater(r);
        } catch (IllegalStateException e) {
            // In unit tests or headless environments, Platform is not initialized
            r.run();
        }
    }

    private void setState(State newState) {
        this.state = newState;
        for (DebugListener l : listeners) {
            safeRunLater(() -> l.onStateChanged(newState));
        }
    }

    private void log(String msg) {
        for (DebugListener l : listeners) {
            safeRunLater(() -> l.onLog(msg));
        }
    }

    /**
     * Connect to target JDWP port (e.g. 5005) on localhost.
     */
    public synchronized void connect(String host, int port, List<DebugBreakpoint> initialBreakpoints) {
        if (isConnected()) {
            disconnect();
        }

        registeredBreakpoints.clear();
        if (initialBreakpoints != null) {
            registeredBreakpoints.addAll(initialBreakpoints);
        }

        setState(State.CONNECTING);
        log("Connecting to debugger at " + host + ":" + port + "...");

        Thread connectThread = new Thread(() -> {
            AttachingConnector connector = findSocketConnector();
            if (connector == null) {
                log("Error: SocketAttachingConnector not found in JDK runtime.");
                setState(State.IDLE);
                return;
            }

            Map<String, Connector.Argument> args = connector.defaultArguments();
            Connector.Argument hostArg = args.get("hostname");
            if (hostArg != null) hostArg.setValue(host);
            Connector.Argument portArg = args.get("port");
            if (portArg != null) portArg.setValue(String.valueOf(port));

            int retries = 15;
            while (retries-- > 0 && state == State.CONNECTING) {
                try {
                    vm = connector.attach(args);
                    log("Connected to target VM (" + vm.name() + ").");
                    setupVm();
                    return;
                } catch (Exception e) {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }

            if (state == State.CONNECTING) {
                log("Unable to attach to " + host + ":" + port + " (target not listening yet).");
                setState(State.IDLE);
            }
        }, "lumina-debugger-connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private AttachingConnector findSocketConnector() {
        for (AttachingConnector c : Bootstrap.virtualMachineManager().attachingConnectors()) {
            if ("com.sun.jdi.SocketAttach".equals(c.name()) || c.name().contains("Socket")) {
                return c;
            }
        }
        return null;
    }

    private void setupVm() {
        try {
            var erm = vm.eventRequestManager();
            ClassPrepareRequest cpr = erm.createClassPrepareRequest();
            cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            cpr.enable();

            // Set existing breakpoints in classes that are already loaded
            for (DebugBreakpoint bp : registeredBreakpoints) {
                installBreakpoint(bp);
            }

            running = true;
            eventThread = new Thread(this::eventLoop, "lumina-debugger-event-loop");
            eventThread.setDaemon(true);
            eventThread.start();

            // Initial resume to start running user program
            vm.resume();
            setState(State.RUNNING);
            for (DebugListener l : listeners) {
                safeRunLater(l::onVmResumed);
            }
        } catch (Exception e) {
            log("Error initializing debugger session: " + e.getMessage());
            disconnect();
        }
    }

    private void eventLoop() {
        EventQueue queue = vm.eventQueue();
        while (running) {
            try {
                EventSet eventSet = queue.remove();
                boolean shouldResume = true;

                for (Event event : eventSet) {
                    if (event instanceof VMDisconnectEvent || event instanceof VMDeathEvent) {
                        running = false;
                        shouldResume = false;
                        setState(State.TERMINATED);
                        for (DebugListener l : listeners) {
                            safeRunLater(l::onDisconnected);
                        }
                        break;
                    } else if (event instanceof ClassPrepareEvent cpe) {
                        ReferenceType refType = cpe.referenceType();
                        for (DebugBreakpoint bp : registeredBreakpoints) {
                            if (bp.fqcn() != null && bp.fqcn().equals(refType.name())) {
                                installBreakpointOnType(bp, refType);
                            }
                        }
                    } else if (event instanceof BreakpointEvent bpe) {
                        handleSuspensionEvent(bpe.thread(), bpe.location(), false);
                        shouldResume = false; // keep suspended
                    } else if (event instanceof StepEvent se) {
                        vm.eventRequestManager().deleteEventRequest(se.request());
                        handleSuspensionEvent(se.thread(), se.location(), true);
                        shouldResume = false; // keep suspended
                    }
                }

                if (shouldResume) {
                    eventSet.resume();
                }
            } catch (VMDisconnectedException | InterruptedException e) {
                running = false;
                setState(State.TERMINATED);
                for (DebugListener l : listeners) {
                    safeRunLater(l::onDisconnected);
                }
                break;
            } catch (Exception e) {
                log("Debugger event error: " + e.getMessage());
            }
        }
    }

    private void handleSuspensionEvent(ThreadReference thread, Location loc, boolean isStep) {
        this.currentSuspendedThread = thread;
        setState(State.PAUSED);

        try {
            DebugThread dThread = new DebugThread(
                    thread.uniqueID(),
                    thread.name(),
                    thread.threadGroup() != null ? thread.threadGroup().name() : "main",
                    thread.isSuspended(),
                    "RUNNING"
            );

            List<DebugStackFrame> frames = new ArrayList<>();
            List<StackFrame> rawFrames = thread.frames();
            for (int i = 0; i < rawFrames.size(); i++) {
                StackFrame sf = rawFrames.get(i);
                Location l = sf.location();
                String mName = l.method().name();
                int line = l.lineNumber();
                String clsName = l.declaringType().name();
                int pkgEnd = clsName.lastIndexOf('.');
                String pkg = pkgEnd >= 0 ? clsName.substring(0, pkgEnd) : "";
                String sName = null;
                try {
                    sName = l.sourceName();
                } catch (AbsentInformationException ignored) {}

                boolean isHidden = clsName.startsWith("org.apache.tomcat")
                        || clsName.startsWith("org.springframework.web.filter")
                        || clsName.startsWith("org.springframework.security")
                        || clsName.startsWith("java.lang.reflect")
                        || clsName.startsWith("jdk.internal");

                frames.add(new DebugStackFrame(i, mName, line, clsName, pkg, sName, isHidden, sf));
            }

            if (!rawFrames.isEmpty()) {
                currentActiveFrame = rawFrames.get(0);
            }

            List<DebugVariable> vars = inspectVariables(currentActiveFrame);
            String srcName = null;
            try {
                srcName = loc.sourceName();
            } catch (AbsentInformationException ignored) {}
            int line = loc.lineNumber();

            final String fSrc = srcName;
            final int fLine = line;

            for (DebugListener l : listeners) {
                safeRunLater(() -> {
                    if (isStep) {
                        l.onStepped(dThread, frames, vars, fSrc, fLine);
                    } else {
                        l.onBreakpointHit(dThread, frames, vars, fSrc, fLine);
                    }
                });
            }
        } catch (IncompatibleThreadStateException e) {
            log("Thread state error while suspended: " + e.getMessage());
        }
    }

    public List<DebugVariable> inspectVariables(StackFrame frame) {
        if (frame == null) return Collections.emptyList();
        List<DebugVariable> result = new ArrayList<>();
        try {
            // 1. this reference
            ObjectReference thisObj = frame.thisObject();
            if (thisObj != null) {
                String thisType = thisObj.referenceType().name();
                int simpleIdx = thisType.lastIndexOf('.');
                String simple = simpleIdx >= 0 ? thisType.substring(simpleIdx + 1) : thisType;
                DebugVariable thisVar = new DebugVariable("this", simple + "@" + thisObj.uniqueID(),
                        "{" + simple + "@" + thisObj.uniqueID() + "}", "this");
                for (Field f : thisObj.referenceType().visibleFields()) {
                    if (f.isStatic()) continue;
                    Value fVal = thisObj.getValue(f);
                    thisVar.addChild(new DebugVariable(f.name(), f.typeName(), formatValue(fVal), "field"));
                }
                result.add(thisVar);
            }

            // 2. Local variables and method parameters
            for (LocalVariable lv : frame.visibleVariables()) {
                Value val = frame.getValue(lv);
                String kind = lv.isArgument() ? "param" : "local";
                DebugVariable v = new DebugVariable(lv.name(), lv.typeName(), formatValue(val), kind);
                if (val instanceof ObjectReference objVal && !(val instanceof StringReference)) {
                    for (Field f : objVal.referenceType().visibleFields()) {
                        if (f.isStatic()) continue;
                        v.addChild(new DebugVariable(f.name(), f.typeName(), formatValue(objVal.getValue(f)), "field"));
                    }
                }
                result.add(v);
            }
        } catch (AbsentInformationException e) {
            // Class compiled without debug symbols (-g)
            result.add(new DebugVariable("(no local variable debug info)", "void", "Recompile with -g", "local"));
        } catch (Exception e) {
            log("Error inspecting frame variables: " + e.getMessage());
        }
        return result;
    }

    private String formatValue(Value v) {
        if (v == null) return "null";
        if (v instanceof StringReference sr) {
            return "\"" + sr.value() + "\"";
        }
        if (v instanceof PrimitiveValue pv) {
            return pv.toString();
        }
        if (v instanceof ObjectReference obj) {
            String type = obj.referenceType().name();
            int idx = type.lastIndexOf('.');
            String simple = idx >= 0 ? type.substring(idx + 1) : type;
            return "{" + simple + "@" + obj.uniqueID() + "}";
        }
        return v.toString();
    }

    public synchronized void addBreakpoint(String fqcn, int line) {
        DebugBreakpoint bp = new DebugBreakpoint(fqcn, line);
        registeredBreakpoints.add(bp);
        if (isConnected()) {
            installBreakpoint(bp);
        }
    }

    public synchronized void removeBreakpoint(String fqcn, int line) {
        DebugBreakpoint bp = new DebugBreakpoint(fqcn, line);
        registeredBreakpoints.remove(bp);
        BreakpointRequest req = activeRequests.remove(bp);
        if (req != null && isConnected()) {
            try {
                vm.eventRequestManager().deleteEventRequest(req);
            } catch (Exception ignored) {}
        }
    }

    private void installBreakpoint(DebugBreakpoint bp) {
        if (vm == null) return;
        List<ReferenceType> types = vm.classesByName(bp.fqcn());
        for (ReferenceType rt : types) {
            installBreakpointOnType(bp, rt);
        }
    }

    private void installBreakpointOnType(DebugBreakpoint bp, ReferenceType rt) {
        try {
            List<Location> locs = rt.locationsOfLine(bp.line());
            if (!locs.isEmpty()) {
                Location loc = locs.get(0);
                BreakpointRequest req = vm.eventRequestManager().createBreakpointRequest(loc);
                req.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                req.enable();
                activeRequests.put(bp, req);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void resume() {
        if (isConnected() && isPaused()) {
            setState(State.RUNNING);
            for (DebugListener l : listeners) {
                safeRunLater(l::onVmResumed);
            }
            try {
                vm.resume();
            } catch (Exception e) {
                log("Error resuming VM: " + e.getMessage());
            }
        }
    }

    public synchronized void pause() {
        if (isConnected() && state == State.RUNNING) {
            try {
                vm.suspend();
                List<ThreadReference> threads = vm.allThreads();
                for (ThreadReference t : threads) {
                    if (t.name().startsWith("http-nio") || "main".equals(t.name())) {
                        if (!t.frames().isEmpty()) {
                            handleSuspensionEvent(t, t.frame(0).location(), false);
                            return;
                        }
                    }
                }
                setState(State.PAUSED);
            } catch (Exception e) {
                log("Error pausing VM: " + e.getMessage());
            }
        }
    }

    public synchronized void stepOver() {
        step(StepRequest.STEP_OVER);
    }

    public synchronized void stepInto() {
        step(StepRequest.STEP_INTO);
    }

    public synchronized void stepOut() {
        step(StepRequest.STEP_OUT);
    }

    private void step(int depth) {
        if (isConnected() && isPaused() && currentSuspendedThread != null) {
            try {
                var erm = vm.eventRequestManager();
                // Clear any leftover step requests for this thread
                for (StepRequest existing : erm.stepRequests()) {
                    if (existing.thread().equals(currentSuspendedThread)) {
                        erm.deleteEventRequest(existing);
                    }
                }

                StepRequest req = erm.createStepRequest(currentSuspendedThread, StepRequest.STEP_LINE, depth);
                req.addCountFilter(1);
                req.enable();

                setState(State.RUNNING);
                for (DebugListener l : listeners) {
                    safeRunLater(l::onVmResumed);
                }
                vm.resume();
            } catch (Exception e) {
                log("Error executing step: " + e.getMessage());
            }
        }
    }

    public synchronized void disconnect() {
        running = false;
        if (vm != null) {
            try {
                vm.dispose();
            } catch (Exception ignored) {}
            vm = null;
        }
        activeRequests.clear();
        setState(State.TERMINATED);
        for (DebugListener l : listeners) {
            safeRunLater(l::onDisconnected);
        }
    }

    public synchronized void selectFrame(DebugStackFrame frame) {
        if (frame != null && frame.rawFrame() instanceof StackFrame sf) {
            this.currentActiveFrame = sf;
        }
    }

    /**
     * Testing / simulation hook to fire a synthetic breakpoint hit matching IntelliJ IDEA.
     */
    public void simulateBreakpointHit(String threadName, String className, String methodName, int line, String sourceFile, List<DebugVariable> vars) {
        setState(State.PAUSED);
        DebugThread dThread = new DebugThread(18165L, threadName, "main", true, "RUNNING");
        List<DebugStackFrame> frames = new ArrayList<>();
        int pkgEnd = className.lastIndexOf('.');
        String pkg = pkgEnd >= 0 ? className.substring(0, pkgEnd) : "";
        frames.add(new DebugStackFrame(0, methodName, line, className, pkg, sourceFile, false, null));

        for (DebugListener l : listeners) {
            safeRunLater(() -> l.onBreakpointHit(dThread, frames, vars, sourceFile, line));
        }
    }

    /**
     * Testing / simulation hook to step into a new frame.
     */
    public void simulateStep(String threadName, String className, String methodName, int line, String sourceFile, List<DebugVariable> vars) {
        setState(State.PAUSED);
        DebugThread dThread = new DebugThread(18165L, threadName, "main", true, "RUNNING");
        List<DebugStackFrame> frames = new ArrayList<>();
        int pkgEnd = className.lastIndexOf('.');
        String pkg = pkgEnd >= 0 ? className.substring(0, pkgEnd) : "";
        frames.add(new DebugStackFrame(0, methodName, line, className, pkg, sourceFile, false, null));

        for (DebugListener l : listeners) {
            safeRunLater(() -> l.onStepped(dThread, frames, vars, sourceFile, line));
        }
    }
}
