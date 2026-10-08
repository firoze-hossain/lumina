package dev.lumina.build;

import java.io.File;
import java.util.Objects;

/**
 * Configuration model for Build Tools > Cargo.
 * Matches standard IDE settings:
 *  - When a build fails, automatically show the first error in the editor
 *  - Offline mode (Pass the --offline option to Cargo commands to avoid network requests)
 *  - Dynamic Cargo executable discovery
 */
public class CargoSettings implements Cloneable {

    private boolean autoShowFirstError = true;
    private boolean offlineMode = false;
    private String cargoExecutable = "";

    public CargoSettings() {
    }

    public CargoSettings(CargoSettings other) {
        if (other != null) {
            this.autoShowFirstError = other.autoShowFirstError;
            this.offlineMode = other.offlineMode;
            this.cargoExecutable = other.cargoExecutable;
        }
    }

    /**
     * Dynamically discovers installed Cargo binary without hardcoding.
     */
    public static String discoverDefaultCargoPath() {
        String cargoHome = System.getenv("CARGO_HOME");
        if (cargoHome != null && !cargoHome.isBlank()) {
            File f = new File(cargoHome.trim(), "bin/cargo");
            if (f.exists() && f.canExecute()) return f.getAbsolutePath();
        }

        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File dotCargo = new File(userHome, ".cargo/bin/cargo");
            if (dotCargo.exists() && dotCargo.canExecute()) return dotCargo.getAbsolutePath();
        }

        File brewArm = new File("/opt/homebrew/bin/cargo");
        if (brewArm.exists() && brewArm.canExecute()) return brewArm.getAbsolutePath();

        File usrLocal = new File("/usr/local/bin/cargo");
        if (usrLocal.exists() && usrLocal.canExecute()) return usrLocal.getAbsolutePath();

        File usrBin = new File("/usr/bin/cargo");
        if (usrBin.exists() && usrBin.canExecute()) return usrBin.getAbsolutePath();

        return "";
    }

    public String getEffectiveCargoExecutable() {
        if (cargoExecutable != null && !cargoExecutable.isBlank()) {
            return cargoExecutable.trim();
        }
        return discoverDefaultCargoPath();
    }

    public boolean isAutoShowFirstError() {
        return autoShowFirstError;
    }

    public void setAutoShowFirstError(boolean autoShowFirstError) {
        this.autoShowFirstError = autoShowFirstError;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    public String getCargoExecutable() {
        return cargoExecutable != null ? cargoExecutable : "";
    }

    public void setCargoExecutable(String cargoExecutable) {
        this.cargoExecutable = cargoExecutable != null ? cargoExecutable.trim() : "";
    }

    @Override
    public CargoSettings clone() {
        return new CargoSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CargoSettings that = (CargoSettings) o;
        return autoShowFirstError == that.autoShowFirstError &&
                offlineMode == that.offlineMode &&
                Objects.equals(cargoExecutable, that.cargoExecutable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoShowFirstError, offlineMode, cargoExecutable);
    }
}
