package dev.lumina.todo;

import java.util.ArrayList;
import java.util.List;

/**
 * Default provider contributing the standard built-in TODO and FIXME patterns in Lumina IDE.
 * Faithfully reproduces the initial configuration shown in reference Image 5.
 */
public class DefaultTodoPatternProvider implements TodoPatternProvider {

    @Override
    public String getProviderName() {
        return "Bundled TODO Pattern Provider";
    }

    @Override
    public List<TodoPattern> getPatterns() {
        List<TodoPattern> list = new ArrayList<>();
        list.add(new TodoPattern(
                "todo-default-1",
                "\\btodo\\b.*",
                false,
                TodoIconType.TODO,
                "#00A8EC",
                null,
                false,
                false,
                true,
                true
        ));
        list.add(new TodoPattern(
                "todo-default-2",
                "\\bfixme\\b.*",
                false,
                TodoIconType.FIXME,
                "#FF6B68",
                null,
                false,
                false,
                true,
                true
        ));
        return list;
    }
}
