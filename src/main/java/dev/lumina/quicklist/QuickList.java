package dev.lumina.quicklist;

import java.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Model representing a user-defined or default Quick List.
 */
public class QuickList {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final ObservableList<QuickListItem> items = FXCollections.observableArrayList();

    public QuickList(String name) {
        this(name, "");
    }

    public QuickList(String name, String description) {
        this.name.set(name != null ? name : "");
        this.description.set(description != null ? description : "");
    }

    public QuickList(String name, String description, List<QuickListItem> initialItems) {
        this.name.set(name != null ? name : "");
        this.description.set(description != null ? description : "");
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        this.name.set(value != null ? value : "");
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String value) {
        this.description.set(value != null ? value : "");
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObservableList<QuickListItem> getItems() {
        return items;
    }

    public void addItem(QuickListItem item) {
        if (item != null) {
            items.add(item);
        }
    }

    public void addItem(int index, QuickListItem item) {
        if (item != null) {
            if (index >= 0 && index <= items.size()) {
                items.add(index, item);
            } else {
                items.add(item);
            }
        }
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void moveUp(int index) {
        if (index > 0 && index < items.size()) {
            QuickListItem item = items.remove(index);
            items.add(index - 1, item);
        }
    }

    public void moveDown(int index) {
        if (index >= 0 && index < items.size() - 1) {
            QuickListItem item = items.remove(index);
            items.add(index + 1, item);
        }
    }

    public QuickList deepCopy() {
        List<QuickListItem> copies = new ArrayList<>();
        for (QuickListItem it : items) {
            copies.add(it.deepCopy());
        }
        return new QuickList(getName(), getDescription(), copies);
    }

    @Override
    public String toString() {
        return getName();
    }
}
