package dev.lumina.naturallang.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in provider contributing all standard proofreading, grammar, typography,
 * punctuation, and readability rules exactly matching the reference IDE screenshots.
 */
public class BuiltInProofreadingRuleProvider implements ProofreadingRuleProvider {

    @Override
    public String getProviderName() {
        return "Built-in Proofreading Rules";
    }

    @Override
    public List<ProofreadingRule> getRules() {
        List<ProofreadingRule> list = new ArrayList<>();

        // =====================================================================
        // Category 1: GENERAL (Screenshot 1)
        // =====================================================================

        list.add(new ProofreadingRule(
                "general.missing_articles",
                ProofreadingRuleCategory.GENERAL,
                "Missing articles",
                "Report missing articles. Learn more ↗",
                "https://lumina.dev/help/proofreading/missing-articles",
                List.of(new RuleExample("I read book yesterday.", "I read a book yesterday.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "general.prefer_contractions",
                ProofreadingRuleCategory.GENERAL,
                "Prefer contractions:",
                "Check the style configured via 'Prefer contractions' setting: Contractions help make your writing sound more informal and conversational.",
                null,
                List.of(new RuleExample("Do not touch that.", "Don't touch that.")),
                false,
                new RuleOption("prefer_contractions", List.of("Always", "When unambiguous", "Never"), "Always"),
                false,
                "• When unambiguous: words whose contracted forms end in 'd, 's, and 'l' are ignored because these can be unclear to people with lower levels of English proficiency.\n• Always: contractions are suggested whenever possible."
        ));

        list.add(new ProofreadingRule(
                "general.spell_out_numbers",
                ProofreadingRuleCategory.GENERAL,
                "Spell out numbers:",
                "In formal and professional writing, numbers from zero to nine are usually spelled out in words. Numbers 10 and above are generally written in numerals. Learn more ↗",
                "https://lumina.dev/help/proofreading/numbers",
                List.of(new RuleExample("We found 5 issues.", "We found five issues.")),
                true,
                new RuleOption("spell_out_numbers", List.of("0-9 including large numerals (five thousand)", "0-9", "Never"), "0-9 including large numerals (five thousand)"),
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "general.spell_out_numbers_sentence_start",
                ProofreadingRuleCategory.GENERAL,
                "Spell out numbers that start a sentence",
                "Using words instead of numbers to start a sentence improves readability and clarity. Learn more ↗",
                "https://lumina.dev/help/proofreading/numbers-sentence-start",
                List.of(new RuleExample("25 students enrolled today.", "Twenty-five students enrolled today.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "general.spell_out_large_round_numbers",
                ProofreadingRuleCategory.GENERAL,
                "Spell out large round numbers",
                "With large round numerals, use words instead of numbers to enhance readability. For large mixed numerals (such as '5,500,000'), use a number-plus-word combination. Learn more ↗",
                "https://lumina.dev/help/proofreading/large-round-numbers",
                List.of(new RuleExample("Revenue exceeded 5000000 dollars.", "Revenue exceeded 5 million dollars.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "general.avoid_condescending_language",
                ProofreadingRuleCategory.GENERAL,
                "Avoid condescending language",
                "Some words don't add anything to the sentence meaning, and also can be perceived as condescending. The text can state that something is obvious or simple or known, but that might not be true for the readers, who then might feel alienated or unintelligent at a vulnerable moment. Learn more ↗",
                "https://lumina.dev/help/proofreading/condescending-language",
                List.of(new RuleExample("Obviously, this configuration is very simple.", "This configuration is straightforward.")),
                true,
                null,
                false,
                null
        ));

        // =====================================================================
        // Category 2: PUNCTUATION (Screenshot 1 & 2)
        // =====================================================================

        list.add(new ProofreadingRule(
                "punctuation.use_serial_commas",
                ProofreadingRuleCategory.PUNCTUATION,
                "Use serial (Oxford) commas:",
                "Some style guides suggest putting a comma before the last item in comma-separated lists, while others suggest leaving it out. Whichever guide you follow, it's important to be consistent.",
                null,
                List.of(new RuleExample("Apples, oranges and bananas", "Apples, oranges, and bananas")),
                true,
                new RuleOption("serial_commas", List.of("Consistently", "Only when necessary", "Never"), "Consistently"),
                true, // Requires Lumina AI Cloud
                null
        ));

        list.add(new ProofreadingRule(
                "punctuation.avoid_exclamations",
                ProofreadingRuleCategory.PUNCTUATION,
                "Avoid exclamations",
                "Exclamation points may seem unprofessional or overly dramatic in formal contexts. Learn more ↗",
                "https://lumina.dev/help/proofreading/exclamations",
                List.of(new RuleExample("They delivered the mail today!", "They delivered the mail today.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "punctuation.avoid_expressive_punctuation",
                ProofreadingRuleCategory.PUNCTUATION,
                "Avoid expressive punctuation",
                "Multiple punctuation marks are typically used in informal or social media content and generally avoided in formal writing. Learn more ↗",
                "https://lumina.dev/help/proofreading/expressive-punctuation",
                List.of(new RuleExample("Are you sure??!", "Are you sure?")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "punctuation.avoid_emoticons_and_emoji",
                ProofreadingRuleCategory.PUNCTUATION,
                "Avoid emoticons and emoji",
                "Emoticons and emojis are considered a form of expressive punctuation and are mostly seen in casual and digital communication. It is best to avoid them in formal writing. Learn more ↗",
                "https://lumina.dev/help/proofreading/emoticons-emoji",
                List.of(new RuleExample("Thanks for the review :)", "Thanks for the review.")),
                false,
                null,
                false,
                null
        ));

        // =====================================================================
        // Category 3: TYPOGRAPHY (Screenshot 2 & 3)
        // =====================================================================

        list.add(new ProofreadingRule(
                "typography.replace_hyphens_with_dashes",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Replace hyphens that break sentences with:",
                "Hyphens are typically used to join words together; dashes are longer and are used to separate phrases and wrap parenthetical clauses. Learn more ↗",
                "https://lumina.dev/help/proofreading/dashes",
                List.of(new RuleExample("The release - scheduled for Monday - is ready.", "The release – scheduled for Monday – is ready.")),
                true,
                new RuleOption("hyphens_replacement", List.of("En dash – with spaces", "Em dash — without spaces", "Hyphen"), "En dash – with spaces"),
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "typography.use_en_dashes_for_ranges",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Use en dashes for ranges",
                "An en dash is used to show a range or span of numbers, dates, or time. It essentially means to or through. The same applies to scores, where it means to. Learn more ↗",
                "https://lumina.dev/help/proofreading/en-dash-ranges",
                List.of(new RuleExample("Read chapters 4-8.", "Read chapters 4–8.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "typography.check_period_comma_placement",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Check period and comma placement relative to closing quotation marks",
                "In American English, periods and commas precede closing quotation marks, while British English only places punctuation inside quotes when it is part of the quoted material. Learn more ↗",
                "https://lumina.dev/help/proofreading/quotation-punctuation",
                List.of(new RuleExample("He said \"hello\", and walked away.", "He said \"hello,\" and walked away.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "typography.format_names_initials",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Format names that contain initials using:",
                "Non-breaking spaces may be used when the separation of two adjacent words through line wrapping might result in a loss of meaning or legibility, e.g., after proper name initials. Learn more ↗",
                "https://lumina.dev/help/proofreading/name-initials",
                List.of(new RuleExample("J. R. R. Tolkien", "J.\u00A0R.\u00A0R.\u00A0Tolkien")),
                true,
                new RuleOption("names_initials", List.of("Non-breaking spaces (J. R. R. Tolkien)", "Standard spaces (J. R. R. Tolkien)", "No spaces (J.R.R. Tolkien)"), "Non-breaking spaces (J. R. R. Tolkien)"),
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "typography.add_diacritics",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Add diacritics",
                "Some words, phrases, and geographical names borrowed from other languages are usually written with accents or other diacritical marks. Learn more ↗",
                "https://lumina.dev/help/proofreading/diacritics",
                List.of(new RuleExample("resume or naive", "résumé or naïve")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "typography.use_curly_apostrophes",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Use curly apostrophes",
                "Curly or 'smart' apostrophes are preferred over their straight-shaped counterparts in professional publishing and print materials. Learn more ↗",
                "https://lumina.dev/help/proofreading/curly-apostrophes",
                List.of(new RuleExample("It's here.", "It’s here.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "typography.add_space_between_number_and_unit",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Add a space between number and unit of measurement",
                "Generally, there should be a space between the number and the unit of measurement. Learn more ↗",
                "https://lumina.dev/help/proofreading/units-spacing",
                List.of(new RuleExample("In this experiment, 2mg of the extract was dissolved in water.", "In this experiment, 2 mg of the extract was dissolved in water.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "typography.format_large_numbers",
                ProofreadingRuleCategory.TYPOGRAPHY,
                "Format large numbers using:",
                "To improve readability, use a delimiter to break down large numbers into groups of three digits.",
                null,
                List.of(
                        new RuleExample("We have 10000 people (use comma)", "We have 10,000 people (use comma)"),
                        new RuleExample("We have 10000 people (use narrow non-breaking space)", "We have 10 000 people (use narrow non-breaking space)")
                ),
                true,
                new RuleOption("large_numbers_format", List.of("Comma (10,000)", "Narrow non-breaking space (10 000)", "Period (10.000)"), "Comma (10,000)"),
                false,
                null
        ));

        // =====================================================================
        // Category 4: READABILITY (Screenshot 4 & 5)
        // =====================================================================

        list.add(new ProofreadingRule(
                "readability.prefer_active_voice",
                ProofreadingRuleCategory.READABILITY,
                "Prefer active voice for improved readability",
                "When a passive sentence contains the phrase by X and the subject of the preposition (X) is shorter or more important than the passive subject, it is usually easier to read when rewritten in active voice. Learn more ↗",
                "https://lumina.dev/help/proofreading/active-voice",
                List.of(new RuleExample("The bread is being baked by him.", "He is baking the bread.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.avoid_all_passive_constructions",
                ProofreadingRuleCategory.READABILITY,
                "Avoid all passive constructions",
                "Passive voice is generally considered bad for your writing style. Although it is not a mistake, passive voice often hides the performer of the action, making your text feel evasive. Learn more ↗",
                "https://lumina.dev/help/proofreading/passive-constructions",
                List.of(new RuleExample("In my opinion, this problem must be solved by a teacher", "In my opinion, a teacher must solve this problem")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.avoid_weak_adverbs",
                ProofreadingRuleCategory.READABILITY,
                "Avoid weak adverbs",
                "Overuse of the adverb very can make writing seem lazy, repetitive, and lacking in precision. It's generally more effective to use a single, more specific word instead of very plus a less descriptive term. Learn more ↗",
                "https://lumina.dev/help/proofreading/weak-adverbs",
                List.of(new RuleExample("The task was very hard.", "The task was arduous.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.avoid_tautology",
                ProofreadingRuleCategory.READABILITY,
                "Avoid tautology",
                "Statements that repeat the same idea in different words can result in unnecessary and verbose repetition. Learn more ↗",
                "https://lumina.dev/help/proofreading/tautology",
                List.of(new RuleExample("Free gift or close proximity", "Gift or proximity")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.avoid_of_when_redundant",
                ProofreadingRuleCategory.READABILITY,
                "Avoid \"of\" when redundant",
                "There are many situations where the preposition of can be removed without changing the meaning of the phrase.",
                null,
                List.of(new RuleExample("All of the files were updated.", "All the files were updated.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.avoid_prepositional_chains",
                ProofreadingRuleCategory.READABILITY,
                "Avoid prepositional chains that use \"of\"",
                "Too many prepositions can make a sentence difficult to follow and understand, particularly when the chain of prepositions is long.",
                null,
                List.of(new RuleExample("This is an example of a long chain of words of importance.", "This sentence uses a long chain of words.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.prefer_clear_language",
                ProofreadingRuleCategory.READABILITY,
                "Prefer clear language",
                "Simple writing uses everyday, understandable words, avoiding jargon, slang, and overly technical terms whenever possible. Learn more ↗",
                "https://lumina.dev/help/proofreading/clear-language",
                List.of(new RuleExample("Utilize this functionality.", "Use this feature.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.avoid_wordiness",
                ProofreadingRuleCategory.READABILITY,
                "Avoid wordiness",
                "Make your text more concise. Replace wordy expressions with shorter and clearer alternatives. Remove words that don't contribute to the meaning of the sentence. Learn more ↗",
                "https://lumina.dev/help/proofreading/wordiness",
                List.of(new RuleExample("Due to the fact that we were delayed", "Because we were delayed")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.shorten_relative_clauses",
                ProofreadingRuleCategory.READABILITY,
                "Shorten relative clauses",
                "You can often remove relative clauses starting with that is, who were, or similar expressions without changing the meaning of the sentence. This only applies to restrictive (defining) relative clauses where the predicate is a verb in the continuous form, in the passive voice, or a prepositional phrase. Learn more ↗",
                "https://lumina.dev/help/proofreading/relative-clauses",
                List.of(new RuleExample("The documentation that was written by the team", "The documentation written by the team")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.consistent_verb_forms",
                ProofreadingRuleCategory.READABILITY,
                "Use consistent verb forms in connected phrases",
                "To preserve clarity, readability, and balance, sentences with multiple predicates should have similar grammatical constructions. Learn more ↗",
                "https://lumina.dev/help/proofreading/verb-forms",
                List.of(new RuleExample("These features speed up the development process and tracks changes automatically.", "These features speed up the development process and track changes automatically.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "readability.long_sentences",
                ProofreadingRuleCategory.READABILITY,
                "Long sentences",
                "Shorter sentences are usually easier to read. Learn more ↗",
                "https://lumina.dev/help/proofreading/long-sentences",
                List.of(new RuleExample("Licenses allow people to use the software on their laptops/PCs for educational tasks, with distribution via a single invitation link (not applicable if students can apply directly for individual student licenses via official university emails, ISIC card or other means of student ID).", "Licenses allow people to use the software on their laptops/PCs for educational tasks, with simple distribution.")),
                true,
                null,
                false,
                null
        ));

        // =====================================================================
        // Category 5: FORMALITY (Image 1 & 2)
        // =====================================================================

        list.add(new ProofreadingRule(
                "formality.avoid_colloquialism",
                ProofreadingRuleCategory.FORMALITY,
                "Avoid colloquialism",
                "Formal and professional writing typically calls for a more neutral, standard level of language. Colloquialisms can come off as unprofessional or too casual in these contexts. Learn more ↗",
                "https://lumina.dev/help/proofreading/colloquialism",
                List.of(new RuleExample("That was a total rip-off.", "That was overpriced.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.avoid_shortened_word_forms",
                ProofreadingRuleCategory.FORMALITY,
                "Avoid shortened word forms",
                "Using full word forms contributes to a consistent, formal tone throughout your text. Learn more ↗",
                "https://lumina.dev/help/proofreading/shortened-forms",
                List.of(new RuleExample("Send pics.", "Send pictures.\nSend photographs.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.avoid_subject_ellipsis",
                ProofreadingRuleCategory.FORMALITY,
                "Avoid subject ellipsis",
                "Looks like, sounds like, and seems like are idiomatic expressions often used informally in conversation. In professional writing, it's generally better to include the subject it for added clarity.",
                null,
                List.of(new RuleExample("Looks like the password is incorrect", "It looks like the password is incorrect")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.check_pronouns_compound_subjects",
                ProofreadingRuleCategory.FORMALITY,
                "Check pronouns in compound subjects",
                "In compound subjects, pronouns like I, she, and they remain the same as they would if they were used alone. Learn more ↗",
                "https://lumina.dev/help/proofreading/compound-subjects",
                List.of(new RuleExample("My friends and me went on holiday to a little town on the south coast.", "My friends and I went on holiday to a little town on the south coast.")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.check_verb_agreement_compound_or",
                ProofreadingRuleCategory.FORMALITY,
                "Check verb agreement with compound 'or' subjects",
                "When a compound subject is joined by the word \"or\" or alike, the verb usually agrees with the noun closest to it. When each noun in a compound subject with \"or\" is singular, the verb should be singular too. These rules are followed in formal writing, but the choice between a singular and plural verb often varies in actual use. Learn more ↗",
                "https://lumina.dev/help/proofreading/verb-agreement-or",
                List.of(new RuleExample("Neither the teacher nor the students was present.", "Neither the teacher nor the students were present.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.spell_out_symbols_and",
                ProofreadingRuleCategory.FORMALITY,
                "Spell out symbols meaning 'and'",
                "Replace the ampersand (&) and plus sign (+) with words. Learn more ↗",
                "https://lumina.dev/help/proofreading/ampersand-symbols",
                List.of(new RuleExample("Design & implementation + testing", "Design and implementation plus testing")),
                true,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.avoid_first_person_singular",
                ProofreadingRuleCategory.FORMALITY,
                "Avoid first-person singular pronouns",
                "Formal writing often aims to have an objective or impersonal tone, rather than focusing on the writer's personal perspective. In this context, it's best to avoid pronouns like I, me, and my. Learn more ↗",
                "https://lumina.dev/help/proofreading/first-person",
                List.of(new RuleExample("I think the author is very convincing.", "The author presents a convincing argument.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.avoid_first_person_plural",
                ProofreadingRuleCategory.FORMALITY,
                "Avoid first-person plural pronouns",
                "Formal or academic writing often requires an objective, impartial tone. Using pronouns like we, us, or our can make the writing seem subjective or opinion-based. Learn more ↗",
                "https://lumina.dev/help/proofreading/first-person-plural",
                List.of(new RuleExample("We think the author is very convincing.", "The author presents a convincing argument.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "formality.avoid_second_person",
                ProofreadingRuleCategory.FORMALITY,
                "Avoid second-person pronouns",
                "Formal writing is typically written in third-person perspective to maintain an objective tone. Pronouns like you and your can come across as informal or conversational. Learn more ↗",
                "https://lumina.dev/help/proofreading/second-person",
                List.of(new RuleExample("You should not write in first or second person in formal writing.", "One should avoid first or second person in formal writing.")),
                false,
                null,
                false,
                null
        ));

        // =====================================================================
        // Category 6: INCLUSIVITY (Image 2 & 3)
        // =====================================================================

        list.add(new ProofreadingRule(
                "inclusivity.avoid_male_pronouns_gender_neutral",
                ProofreadingRuleCategory.INCLUSIVITY,
                "Avoid male pronouns in gender-neutral contexts",
                "People often use gendered pronouns even when they do not know the gender of the person they are talking about. This can perpetuate gender stereotypes, reinforcing commonly held expectations about the gender of people in certain roles. Learn more ↗",
                "https://lumina.dev/help/proofreading/gender-neutral-pronouns",
                List.of(new RuleExample("Every user must submit his profile.", "Every user must submit their profile.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "inclusivity.avoid_gender_specific_nouns",
                ProofreadingRuleCategory.INCLUSIVITY,
                "Avoid gender-specific nouns",
                "English uses gender-specific terms for some jobs and roles, like policeman, fireman, and chairman. Use neutral alternatives when gender is not an essential part of the message. Learn more ↗",
                "https://lumina.dev/help/proofreading/gender-specific-nouns",
                List.of(new RuleExample("Policeman, fireman, and chairman", "Police officer, firefighter, and chairperson")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "inclusivity.use_neutral_honorifics",
                ProofreadingRuleCategory.INCLUSIVITY,
                "Use neutral honorifics",
                "Titles that don't specify marital status, like Ms. instead of Mrs. and Miss, or gender, like Mx. instead of Mr. or Mrs., help promote equality and inclusivity. Learn more ↗",
                "https://lumina.dev/help/proofreading/neutral-honorifics",
                List.of(new RuleExample("Miss Elizabeth Smith was nominated for a Nobel Prize.", "Ms. Elizabeth Smith was nominated for a Nobel Prize.\nMx. Elizabeth Smith was nominated for a Nobel Prize.")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "inclusivity.avoid_words_racial_connotations",
                ProofreadingRuleCategory.INCLUSIVITY,
                "Avoid words with racial connotations or undertones",
                "Inclusive language ensures that all individuals feel valued and understood, regardless of their race, religion, nationality, origin, or other characteristics. Learn more ↗",
                "https://lumina.dev/help/proofreading/racial-connotations",
                List.of(new RuleExample("Blacklist and whitelist", "Blocklist and allowlist")),
                false,
                null,
                false,
                null
        ));

        list.add(new ProofreadingRule(
                "inclusivity.avoid_violent_language",
                ProofreadingRuleCategory.INCLUSIVITY,
                "Avoid violent language",
                "Using non-violent language promotes peaceful and respectful communication. Language that might implicitly promote physical power or dominance, including military associations, can be offensive or alienating to readers. Learn more ↗",
                "https://lumina.dev/help/proofreading/violent-language",
                List.of(new RuleExample("Kill the process and trigger bullet points", "Terminate the process and start list items")),
                false,
                null,
                false,
                null
        ));

        return Collections.unmodifiableList(list);
    }
}
