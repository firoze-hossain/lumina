package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Features Trainer settings in Lumina IDE.
 */
public class FeaturesTrainerSettings implements Cloneable {

    private String mainLanguage = "Java";
    private boolean showNotificationsOnNewLessons = true;

    public FeaturesTrainerSettings() {
    }

    public String getMainLanguage() {
        return mainLanguage;
    }

    public void setMainLanguage(String mainLanguage) {
        this.mainLanguage = mainLanguage != null ? mainLanguage : "Java";
    }

    public boolean isShowNotificationsOnNewLessons() {
        return showNotificationsOnNewLessons;
    }

    public void setShowNotificationsOnNewLessons(boolean showNotificationsOnNewLessons) {
        this.showNotificationsOnNewLessons = showNotificationsOnNewLessons;
    }

    @Override
    public FeaturesTrainerSettings clone() {
        try {
            return (FeaturesTrainerSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeaturesTrainerSettings that = (FeaturesTrainerSettings) o;
        return showNotificationsOnNewLessons == that.showNotificationsOnNewLessons &&
                Objects.equals(mainLanguage, that.mainLanguage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainLanguage, showNotificationsOnNewLessons);
    }

    @Override
    public String toString() {
        return "FeaturesTrainerSettings{" +
                "mainLanguage='" + mainLanguage + '\'' +
                ", showNotificationsOnNewLessons=" + showNotificationsOnNewLessons +
                '}';
    }
}
