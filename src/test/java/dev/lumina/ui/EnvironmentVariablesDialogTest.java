package dev.lumina.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentVariablesDialogTest {

    @Test
    void testEnvVarProperties() {
        EnvironmentVariablesDialog.EnvVar var = new EnvironmentVariablesDialog.EnvVar("GOPROXY", "https://proxy.golang.org");
        assertEquals("GOPROXY", var.getName());
        assertEquals("https://proxy.golang.org", var.getValue());

        var.setName("GOPRIVATE");
        var.setValue("mycorp.internal/*");
        assertEquals("GOPRIVATE", var.getName());
        assertEquals("mycorp.internal/*", var.getValue());

        assertEquals("GOPRIVATE", var.nameProperty().get());
        assertEquals("mycorp.internal/*", var.valueProperty().get());
    }

    @Test
    void testEnvVarNullSafety() {
        EnvironmentVariablesDialog.EnvVar var = new EnvironmentVariablesDialog.EnvVar(null, null);
        assertEquals("", var.getName());
        assertEquals("", var.getValue());
    }
}
