package dev.lumina.naturallang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in provider supplying all standard natural languages and regional dialects
 * matching the reference IDE Available Languages popup.
 */
public class BuiltInNaturalLanguageProvider implements NaturalLanguageProvider {

    @Override
    public String getProviderName() {
        return "Built-in Proofreading Languages";
    }

    @Override
    public List<NaturalLanguage> getLanguages() {
        List<NaturalLanguage> list = new ArrayList<>();

        // Base English
        list.add(new NaturalLanguage("en-US", "English (USA)", 1, false));

        // Dialects displayed in top section
        list.add(new NaturalLanguage("en-CA", "English (Canada)", 1, true));
        list.add(new NaturalLanguage("en-GB", "English (Great Britain)", 1, true));

        // Alphabetical natural languages with dialect rule counts matching screenshot
        list.add(new NaturalLanguage("ast", "Asturianu", 1));
        list.add(new NaturalLanguage("br", "Brezhoneg", 2));
        list.add(new NaturalLanguage("ca", "Català", 4));
        list.add(new NaturalLanguage("ca-ES-valencia", "Català (Valencià)", 4));
        list.add(new NaturalLanguage("da", "Dansk", 1));
        list.add(new NaturalLanguage("de-DE", "Deutsch (Deutschland)", 22));
        list.add(new NaturalLanguage("de-CH", "Deutsch (Die Schweiz)", 22));
        list.add(new NaturalLanguage("de-AT", "Deutsch (Österreich)", 22));
        list.add(new NaturalLanguage("es", "Español", 3));
        list.add(new NaturalLanguage("eo", "Esperanto", 1));
        list.add(new NaturalLanguage("fr", "Français", 2));
        list.add(new NaturalLanguage("ga", "Gaeilge", 13));
        list.add(new NaturalLanguage("gl", "Galego", 5));
        list.add(new NaturalLanguage("it", "Italiano", 1));
        list.add(new NaturalLanguage("nl", "Nederlands", 37));
        list.add(new NaturalLanguage("pl", "Polski", 5));
        list.add(new NaturalLanguage("pt-AO", "Português (Angola)", 5));
        list.add(new NaturalLanguage("pt-BR", "Português (Brasil)", 5));
        list.add(new NaturalLanguage("pt-MZ", "Português (Moçambique)", 5));
        list.add(new NaturalLanguage("pt-PT", "Português (Portugal)", 5));
        list.add(new NaturalLanguage("ro", "Română", 2));
        list.add(new NaturalLanguage("sk", "Slovenčina", 3));
        list.add(new NaturalLanguage("sl", "Slovenščina", 1));
        list.add(new NaturalLanguage("sv", "Svenska", 1));
        list.add(new NaturalLanguage("tl", "Tagalog", 1));
        list.add(new NaturalLanguage("el", "Ελληνικά", 1));
        list.add(new NaturalLanguage("be", "Беларуская", 1));
        list.add(new NaturalLanguage("ru", "Русский", 7));
        list.add(new NaturalLanguage("uk", "Українська", 1));
        list.add(new NaturalLanguage("zh-CN", "中文 (简体)", 1));
        list.add(new NaturalLanguage("ja", "日本語", 1));

        return Collections.unmodifiableList(list);
    }
}
