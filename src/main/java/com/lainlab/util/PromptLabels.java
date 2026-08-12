package com.lainlab.util;

import com.lainlab.model.Difficulty;
import com.lainlab.model.QuestionType;

/**
 * English labels for LLM prompts only. UI translations stay in i18n/messages_*.properties.
 */
public final class PromptLabels {

    public record Entry(String title, String description) {}

    private PromptLabels() {
    }

    public static Entry forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case A1 -> new Entry(
                    "A1 — Beginner",
                    "CEFR A1. Topics: greetings, family, food, numbers, daily routine. " +
                    "Grammar: present simple, basic questions, pronouns, plural forms. " +
                    "Use only very common concrete words. " +
                    "Avoid: complex tenses, subordinate clauses, idioms, abstract or academic topics."
            );
            case A2 -> new Entry(
                    "A2 — Elementary",
                    "CEFR A2. Topics: shopping, travel, health, hobbies, simple past events. " +
                    "Grammar: past simple, future (will/going to), comparatives, basic modals (can/must), simple prepositions. " +
                    "Short straightforward sentences. " +
                    "Avoid: advanced passive, mixed conditionals, rare idioms, long multi-clause questions."
            );
            case B1 -> new Entry(
                    "B1 — Intermediate",
                    "CEFR B1. Topics: work, school, travel experiences, opinions, everyday media. " +
                    "Grammar: present perfect, past continuous, basic conditionals, relative clauses (who/which/that), simple reported speech. " +
                    "Vocabulary: frequent everyday words (~2000). " +
                    "Avoid: C1/C2 idioms, literary style, specialized jargon, overly long complex sentences."
            );
            case B2 -> new Entry(
                    "B2 — Upper Intermediate",
                    "CEFR B2. Topics: society, culture, technology, environment, arguments for/against. " +
                    "Grammar: full common tense range, passive voice, modals of deduction, linkers (although, despite, whereas), complex noun phrases. " +
                    "Some implicit meaning OK. " +
                    "Avoid: oversimplified A2 wording, highly technical terms unless the topic requires them."
            );
            case C1 -> new Entry(
                    "C1 — Advanced",
                    "CEFR C1. Topics: abstract ideas, professional contexts, nuanced opinions, tone and register. " +
                    "Grammar: inversion, cleft sentences, advanced modality, subtle contrast, formal/informal distinction. " +
                    "Expect implicit meaning and paraphrase. " +
                    "Avoid: beginner vocabulary, single-clause drill-only items unless testing a specific advanced point."
            );
            case C2 -> new Entry(
                    "C2 — Proficient",
                    "CEFR C2. Topics: any, including subtle, idiomatic, academic, and stylistic nuance. " +
                    "Grammar: full range, near-native complexity, implicit meaning, precision of word choice. " +
                    "Questions may test fine shades of meaning. " +
                    "Avoid: elementary phrasing, obvious answers, oversimplified grammar unless deliberately contrasting registers."
            );
        };
    }

    public static Entry forQuestionType(QuestionType type) {
        return switch (type) {
            case GRAMMAR -> new Entry(
                    "Grammar",
                    "General grammar rules and structures."
            );
            case TENSES -> new Entry(
                    "Tenses",
                    "Verb tenses: formation, usage, and differences."
            );
            case ARTICLES -> new Entry(
                    "Articles",
                    "Usage of a/an/the and zero article."
            );
            case VOCABULARY -> new Entry(
                    "Vocabulary",
                    "Lexical knowledge, expressions, and word usage."
            );
            case PHRASAL_VERBS -> new Entry(
                    "Phrasal Verbs",
                    "Meanings and usage of phrasal verbs."
            );
            case IDIOMS -> new Entry(
                    "Idioms",
                    "Fixed expressions and their meanings."
            );
            case PREPOSITIONS -> new Entry(
                    "Prepositions",
                    "Correct use of prepositions in context."
            );
            case WORD_FORMATION -> new Entry(
                    "Word Formation",
                    "Prefixes, suffixes, and part-of-speech changes."
            );
            case COLLOCATIONS -> new Entry(
                    "Collocations",
                    "Common word combinations."
            );
            case SYNONYMS -> new Entry(
                    "Synonyms & Antonyms",
                    "Words with similar or opposite meanings."
            );
            case READING -> new Entry(
                    "Reading",
                    "Short text comprehension and information extraction."
            );
            case DIALOGUES -> new Entry(
                    "Dialogues",
                    "Choosing or completing dialogue lines."
            );
            case ERROR_CORRECTION -> new Entry(
                    "Error Correction",
                    "Finding and correcting grammar or vocabulary mistakes."
            );
            case TRANSFORMATION -> new Entry(
                    "Sentence Transformation",
                    "Rewriting sentences while keeping the meaning."
            );
            case GAP_FILLING -> new Entry(
                    "Gap Filling",
                    "Inserting appropriate words or grammatical forms."
            );
            case WORD_ORDER -> new Entry(
                    "Word Order",
                    "Correct arrangement of words in a sentence."
            );
            case REAL_LIFE -> new Entry(
                    "Real-life language",
                    "Slang, informal expressions, and everyday communication."
            );
        };
    }
}
