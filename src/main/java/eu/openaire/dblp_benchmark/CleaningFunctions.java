package eu.openaire.dblp_benchmark;

import com.ibm.icu.text.Transliterator;

public class CleaningFunctions {
    private static final ThreadLocal<Transliterator> TRANSLITERATOR_THREAD_LOCAL = ThreadLocal.withInitial(() -> Transliterator.getInstance("Any-Latin; Latin-ASCII"));

    static public String cleanAuthorName(String authorName) {

        Transliterator transliterator = TRANSLITERATOR_THREAD_LOCAL.get();

        // remove trailing numbers (DBLP identifier )
        authorName = authorName.replaceAll("[\\d\\s]*$", "");

        // transliterate
        authorName = transliterator.transliterate(authorName);

        // remove common honorifics
        authorName = authorName.replaceAll("(?i)\\b(?:Dr|Prof|PhD)\\.?\\b", "");

        return authorName;
    }
}
