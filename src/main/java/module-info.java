module dev.lumina {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires java.compiler;
    requires java.net.http;
    requires java.sql;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;
    requires org.fxmisc.undo;
    requires com.github.javaparser.core;
    requires com.github.javaparser.symbolsolver.core;
    requires com.google.common;

    exports dev.lumina;
    exports dev.lumina.ui;
    exports dev.lumina.syntax;
    exports dev.lumina.project;
    exports dev.lumina.run;
    exports dev.lumina.semantics;
    exports dev.lumina.diagnostics;
    exports dev.lumina.git;
    exports dev.lumina.refactor;
    exports dev.lumina.util;
}
