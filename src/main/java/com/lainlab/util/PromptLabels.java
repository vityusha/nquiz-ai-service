package com.lainlab.util;

import com.lainlab.model.Difficulty;
import com.lainlab.model.QuestionType;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * English labels for LLM prompts only. UI translations stay in i18n/messages_*.properties.
 */
public final class PromptLabels {

    public record Entry(String title, String description) {}

    private PromptLabels() {
    }

    public static String randomVariation(QuestionType type) {
        return randomVariation(type, ThreadLocalRandom.current());
    }

    public static String randomVariation(QuestionType type, Random random) {
        List<String> options = variationsFor(type);
        return options.get(random.nextInt(options.size()));
    }

    private static List<String> variationsFor(QuestionType type) {
        return switch (type) {
            case GRAMMAR -> List.of(
                    "Across the batch, vary sentence structures (statements, questions, negatives).",
                    "Across the batch, test different grammar points — do not repeat the same rule twice.",
                    "Use different subjects and contexts while staying on the same grammar focus."
            );
            case TENSES -> List.of(
                    "Vary past, present, and future forms across questions.",
                    "Include different aspect uses (simple, continuous, perfect) where level allows.",
                    "Use different time markers (yesterday, now, next week) across questions."
            );
            case ARTICLES -> List.of(
                    "Vary a, an, the, and zero article contexts across questions.",
                    "Use different noun types: countable, uncountable, and proper nouns.",
                    "Cover different situations: first mention, specific reference, and general statements."
            );
            case VOCABULARY -> List.of(
                    "Use different word families, topics, and contexts in each question.",
                    "Vary word classes: nouns, verbs, adjectives, and adverbs.",
                    "Avoid repeating the same headword or root across questions."
            );
            case PHRASAL_VERBS -> List.of(
                    "Use different particles (up, out, off, in) and verb bases across questions.",
                    "Vary contexts: daily life, work, travel — do not repeat the same verb.",
                    "Mix literal and idiomatic uses where appropriate for the level."
            );
            case IDIOMS -> List.of(
                    "Use different idioms and themes — never repeat the same expression.",
                    "Vary formal and informal idioms appropriate to the level.",
                    "Test meaning in context, not dictionary definitions alone."
            );
            case PREPOSITIONS -> List.of(
                    "Vary prepositions of time, place, and abstract relations.",
                    "Use different collocations — do not repeat the same preposition.",
                    "Change context: movement, location, and fixed expressions."
            );
            case WORD_FORMATION -> List.of(
                    "Vary prefixes, suffixes, and part-of-speech conversions across questions.",
                    "Use different base words — do not repeat the same root.",
                    "Mix noun, verb, adjective, and adverb formation where level allows."
            );
            case COLLOCATIONS -> List.of(
                    "Use different verb–noun and adjective–noun pairs in each question.",
                    "Vary registers and topics — avoid repeating the same collocation pattern.",
                    "Test strong collocations, not random word pairs."
            );
            case SYNONYMS -> List.of(
                    "Mix synonym and antonym tasks across the batch.",
                    "Use different registers and shades of meaning.",
                    "Do not repeat the same keyword across questions."
            );
            case READING -> List.of(
                    "Use different short text types: notice, message, ad, or paragraph.",
                    "Vary question focus: main idea, detail, inference, and vocabulary in context.",
                    "Each question must stand alone — no shared passage between questions."
            );
            case DIALOGUES -> List.of(
                    "Use different situations: shop, work, travel, phone call.",
                    "Vary speakers' roles and communicative goals.",
                    "Do not reuse the same dialogue setting twice."
            );
            case ERROR_CORRECTION -> List.of(
                    "Vary error types: tense, agreement, word order, vocabulary, spelling.",
                    "Use only one clear error per question unless multi-error is explicit.",
                    "Do not repeat the same mistake pattern across questions."
            );
            case TRANSFORMATION -> List.of(
                    "Vary transformation types: passive, reported speech, conditionals, emphasis.",
                    "Change structure but keep meaning — use different cue words.",
                    "Do not repeat the same grammatical transformation twice."
            );
            case GAP_FILLING -> List.of(
                    "Vary gap types: verb form, preposition, linker, or key vocabulary word.",
                    "Use different sentence patterns and contexts.",
                    "Only one gap per question unless the format requires more."
            );
            case WORD_ORDER -> List.of(
                    "Vary sentence patterns: questions, negatives, and statements with modifiers.",
                    "Use different clause elements — subject, verb, object, adverbial.",
                    "Test one correct word order only; avoid ambiguous arrangements."
            );
            case REAL_LIFE -> List.of(
                    "Vary situations: café, office, street, social media, family.",
                    "Mix informal, slang, and everyday expressions appropriate to the level.",
                    "Use realistic spoken-style prompts, not textbook phrasing."
            );
        };
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
