package dev.lumina.naturallang.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in provider supplying all standard linguistic rule categories and sub-rules
 * for the "Other rules" hierarchical tree matching reference IDE Screenshots 3, 4, and 5.
 */
public class BuiltInProofreadingTreeRulesProvider implements ProofreadingTreeRulesProvider {

    @Override
    public String getProviderName() {
        return "Built-in Proofreading Tree Rules";
    }

    @Override
    public List<ProofreadingTreeCategory> getTreeCategories() {
        List<ProofreadingTreeCategory> categories = new ArrayList<>();

        // 1. British English phrases
        categories.add(new ProofreadingTreeCategory("British English phrases", List.of(
                new ProofreadingTreeRule("tree.british.weekend", "British English phrases", "'at the weekend' (British) vs 'on the weekend' (American)", true),
                new ProofreadingTreeRule("tree.british.hospital", "British English phrases", "'in hospital' (British) vs 'in the hospital' (American)", true),
                new ProofreadingTreeRule("tree.british.bath", "British English phrases", "'have a bath' vs 'take a bath'", true)
        )));

        // 2. Capitalization
        categories.add(new ProofreadingTreeCategory("Capitalization", List.of(
                new ProofreadingTreeRule("tree.cap.days", "Capitalization", "Capitalization of days of the week", true),
                new ProofreadingTreeRule("tree.cap.months", "Capitalization", "Capitalization of months", true),
                new ProofreadingTreeRule("tree.cap.sentence_start", "Capitalization", "Sentence beginning with lower case", true)
        )));

        // 3. Collocations
        categories.add(new ProofreadingTreeCategory("Collocations", List.of(
                new ProofreadingTreeRule("tree.colloc.appointment", "Collocations", "Make an appointment (not 'do an appointment')", true),
                new ProofreadingTreeRule("tree.colloc.rain", "Collocations", "Heavy rain (not 'strong rain')", true),
                new ProofreadingTreeRule("tree.colloc.photo", "Collocations", "Take a photo (not 'make a photo')", false)
        )));

        // 4. Commonly Confused Words
        categories.add(new ProofreadingTreeCategory("Commonly Confused Words", List.of(
                new ProofreadingTreeRule("tree.confused.their_there", "Commonly Confused Words", "their / there / they're", true),
                new ProofreadingTreeRule("tree.confused.its_it_is", "Commonly Confused Words", "its / it's", true),
                new ProofreadingTreeRule("tree.confused.affect_effect", "Commonly Confused Words", "effect / affect", true),
                new ProofreadingTreeRule("tree.confused.then_than", "Commonly Confused Words", "then / than", true)
        )));

        // 5. Compounding
        categories.add(new ProofreadingTreeCategory("Compounding", List.of(
                new ProofreadingTreeRule("tree.compound.database", "Compounding", "Database (single word)", true),
                new ProofreadingTreeRule("tree.compound.opensource", "Compounding", "Open-source vs open source", true),
                new ProofreadingTreeRule("tree.compound.frontend", "Compounding", "Front end vs front-end", true)
        )));

        // 6. Grammar (Exact rules from Image 5)
        categories.add(new ProofreadingTreeCategory("Grammar", List.of(
                new ProofreadingTreeRule("tree.grammar.19_century", "Grammar", "'19 century' (19th century)", true),
                new ProofreadingTreeRule("tree.grammar.base_form_verbs", "Grammar", "'admit', 'appreciate', 'avoid', 'enjoy' etc. with a base form of a verb", true),
                new ProofreadingTreeRule("tree.grammar.gerund_infinitive", "Grammar", "'advise', 'help' and 'remind' used with gerund instead of infinitive", true),
                new ProofreadingTreeRule("tree.grammar.afford_verbs_use", "Grammar", "'afford', 'choose', 'deserve', 'pretend', 'learn', 'strive', 'want' and 'struggle' use...", true),
                new ProofreadingTreeRule("tree.grammar.afford_base_form", "Grammar", "'afford', 'choose', etc. used with base form instead of infinitive", true),
                new ProofreadingTreeRule("tree.grammar.afraid_of", "Grammar", "'afraid of' + singular", true),
                new ProofreadingTreeRule("tree.grammar.allow_to", "Grammar", "'allow' + 'to' + infinitive", true),
                new ProofreadingTreeRule("tree.grammar.an_and_any", "Grammar", "'an' vs. 'and' vs. 'any'", true),
                new ProofreadingTreeRule("tree.grammar.arrive_proper_noun", "Grammar", "'arrive' + proper noun ('arrive in' + proper noun)", true),
                new ProofreadingTreeRule("tree.grammar.did_past_tense", "Grammar", "'did' with past tense verb", true),
                new ProofreadingTreeRule("tree.grammar.does_base_verb", "Grammar", "'does' ... 3rd person verb (base verb)", true),
                new ProofreadingTreeRule("tree.grammar.subjunctive", "Grammar", "Use of subjunctive mood", false)
        )));

        // 7. Machine Learning
        categories.add(new ProofreadingTreeCategory("Machine Learning", List.of(
                new ProofreadingTreeRule("tree.ml.terminology", "Machine Learning", "AI / ML terminology standards", true),
                new ProofreadingTreeRule("tree.ml.weights", "Machine Learning", "Model checkpoint and weight conventions", true)
        )));

        // 8. Miscellaneous
        categories.add(new ProofreadingTreeCategory("Miscellaneous", List.of(
                new ProofreadingTreeRule("tree.misc.redundant_acronyms", "Miscellaneous", "Redundant acronym phrases (e.g., PIN number)", true),
                new ProofreadingTreeRule("tree.misc.double_negatives", "Miscellaneous", "Double negative phrases", false)
        )));

        // 9. Nonstandard Phrases
        categories.add(new ProofreadingTreeCategory("Nonstandard Phrases", List.of(
                new ProofreadingTreeRule("tree.nonstandard.irregardless", "Nonstandard Phrases", "Irregardless (use regardless)", true),
                new ProofreadingTreeRule("tree.nonstandard.could_of", "Nonstandard Phrases", "Could of (use could have)", true)
        )));

        // 10. Orthographic errors
        categories.add(new ProofreadingTreeCategory("Orthographic errors", List.of(
                new ProofreadingTreeRule("tree.ortho.repeated_words", "Orthographic errors", "Repeated adjacent words (e.g., 'the the')", true),
                new ProofreadingTreeRule("tree.ortho.space_before_punct", "Orthographic errors", "Accidental whitespace before punctuation", true)
        )));

        // 11. Possible Typo
        categories.add(new ProofreadingTreeCategory("Possible Typo", List.of(
                new ProofreadingTreeRule("tree.typo.dev_terms", "Possible Typo", "Common typos in developer and code terminology", true),
                new ProofreadingTreeRule("tree.typo.character_swap", "Possible Typo", "Accidental character transposition", false)
        )));

        // 12. Proper Nouns
        categories.add(new ProofreadingTreeCategory("Proper Nouns", List.of(
                new ProofreadingTreeRule("tree.proper.javascript", "Proper Nouns", "JavaScript capitalization (not Javascript)", true),
                new ProofreadingTreeRule("tree.proper.github", "Proper Nouns", "GitHub capitalization (not Github)", false)
        )));

        // 13. Punctuation
        categories.add(new ProofreadingTreeCategory("Punctuation", List.of(
                new ProofreadingTreeRule("tree.punct.spacing", "Punctuation", "Space before closing quotation mark or bracket", true),
                new ProofreadingTreeRule("tree.punct.unclosed_quotes", "Punctuation", "Unclosed quotation marks or brackets", false)
        )));

        // 14. Semantics
        categories.add(new ProofreadingTreeCategory("Semantics", List.of(
                new ProofreadingTreeRule("tree.sem.contradiction", "Semantics", "Self-contradictory modifier statements", true),
                new ProofreadingTreeRule("tree.sem.redundancy", "Semantics", "Redundant semantic qualifiers", false)
        )));

        // 15. Style
        categories.add(new ProofreadingTreeCategory("Style", List.of(
                new ProofreadingTreeRule("tree.style.cliches", "Style", "Overused cliches in technical prose", true),
                new ProofreadingTreeRule("tree.style.qualification", "Style", "Unnecessary qualification of absolute terms", false)
        )));

        // 16. Upper/Lowercase
        categories.add(new ProofreadingTreeCategory("Upper/Lowercase", List.of(
                new ProofreadingTreeRule("tree.case.all_caps", "Upper/Lowercase", "Unnecessary ALL CAPS words in sentences", true),
                new ProofreadingTreeRule("tree.case.mixed_prose", "Upper/Lowercase", "Mixed case words in continuous narrative", false)
        )));

        return Collections.unmodifiableList(categories);
    }
}
