package eu.openaire.dblp_benchmark;

import eu.openaire.common.author.AuthorMatch;
import eu.openaire.common.author.AuthorMatcherStep;
import eu.openaire.dblp_benchmark.beans.DBLPAuthor;
import eu.openaire.dblp_benchmark.beans.ORCIDAuthor;
import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.Damerau;
import info.debatty.java.stringsimilarity.NGram;
import info.debatty.java.stringsimilarity.SorensenDice;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.codec.language.Nysiis;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LongestCommonSubsequence;

import java.io.Serializable;
import java.util.*;

class BenchmarkScenario implements Serializable {
    static final List<BenchmarkScenario> SCENARIOS = Arrays.asList(
            new BenchmarkScenario("ORCID Matching", orcidSteps()),
            new BenchmarkScenario("OpenAIRE matching", aiderSteps()),
            new BenchmarkScenario("fullName strip Matching", fullNameStripSteps()),
            new BenchmarkScenario("inverted fullName strip", invertedFullNameStripSteps()),
            new BenchmarkScenario("Levenshtein", levenshteinSteps()),
            new BenchmarkScenario("fullName Matching", fullNameSteps()),
            new BenchmarkScenario("invertedFullName", invertedFullNameSteps()),
            new BenchmarkScenario("orderedTokens", orderedTokensSteps()),
            new BenchmarkScenario("creditName", creditNameSteps()),
            new BenchmarkScenario("otherNames", otherNamesSteps()),
            new BenchmarkScenario("Jaro-Winkler", jaroWinklerSteps()),
            new BenchmarkScenario("Jaro-Winkler OrderedToken", jaroWinklerOrderedTokenSteps()),
            new BenchmarkScenario("Jaccard", jaccardSteps()),
            new BenchmarkScenario("LongestCommonSubsequence", longestCommonSubsequenceSteps()),
            new BenchmarkScenario("Soundex", soundexSteps()),
            new BenchmarkScenario("Metaphone", metaphoneSteps()),
            new BenchmarkScenario("DoubleMetaphone", doubleMetaphoneSteps()),
            new BenchmarkScenario("NYSIIS", nysiisteps()),
            new BenchmarkScenario("Damerau", damerauSteps()),
            new BenchmarkScenario("Cosine (n-gram)", cosineSteps()),
            new BenchmarkScenario("Sorensen-Dice", sorensenDiceSteps()),
            new BenchmarkScenario("N-Gram Distance", nGramDistanceSteps())
    );

    String name;

    List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> aiderSteps;

    public BenchmarkScenario() {

    }

    public BenchmarkScenario(String orcidMatching, List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> authorMatcherSteps) {
        this.name = orcidMatching;
        this.aiderSteps = authorMatcherSteps;
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> jaroWinklerSteps() {
        double threshold = 0.85;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = new JaroWinklerSimilarity().apply(ca, cb);
                            if (score < threshold) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Jaro-Winkler", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Jaro-Winkler")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> jaroWinklerOrderedTokenSteps() {
        double threshold = 0.9;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = JaroWinklerOrderedToken.compare(ca, cb);
                            if (score < threshold) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Jaro-Winkler OrderedToken", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Jaro-Winkler OrderedToken")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> jaccardSteps() {
        double threshold = 0.75;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = new JaccardSimilarity().apply(ca, cb);
                            if (score < threshold) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Jaccard", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Jaccard")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> longestCommonSubsequenceSteps() {
        double threshold = 0.80;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            int maxdistance = (int) Math.floor((1 - threshold) * Math.max(ca.length(), cb.length()));

                            int score = new LongestCommonSubsequence().apply(ca, cb);
                            if (score < maxdistance) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "LongestCommonSubsequence", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("LongestCommonSubsequence")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> soundexSteps() {
        Soundex enc = new Soundex();

        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            try {
                                if (!enc.soundex(ca).equalsIgnoreCase(enc.soundex(cb))) {
                                    return Optional.empty();
                                }
                                return Optional.of(new AuthorMatch<>(dblp, orcid, "Soundex", 1.0));
                            } catch (IllegalArgumentException e) {
                                return Optional.empty();
                            }
                        })
                        .name("Soundex")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> metaphoneSteps() {
        Metaphone enc = new Metaphone();

        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            try {
                                if (!enc.metaphone(ca).equalsIgnoreCase(enc.metaphone(cb))) {
                                    return Optional.empty();
                                }
                                return Optional.of(new AuthorMatch<>(dblp, orcid, "Metaphone", 1.0));
                            } catch (IllegalArgumentException e) {
                                return Optional.empty();
                            }
                        })
                        .name("Metaphone")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> doubleMetaphoneSteps() {
        DoubleMetaphone enc = new DoubleMetaphone();

        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            try {
                                if (!enc.doubleMetaphone(ca).equalsIgnoreCase(enc.doubleMetaphone(cb))) {
                                    return Optional.empty();
                                }
                                return Optional.of(new AuthorMatch<>(dblp, orcid, "DoubleMetaphone", 1.0));
                            } catch (IllegalArgumentException e) {
                                return Optional.empty();
                            }
                        })
                        .name("DoubleMetaphone")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> nysiisteps() {
        Nysiis enc = new Nysiis();

        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            if (!enc.nysiis(ca).equalsIgnoreCase(enc.nysiis(cb))) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "NYSIIS", 1.0));
                        })
                        .name("NYSIIS")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> damerauSteps() {
        double threshold = 2.0;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = new Damerau().distance(ca, cb);
                            if (score > threshold) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Damerau", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Damerau")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> cosineSteps() {
        double threshold = 0.75;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = new Cosine().similarity(ca, cb);
                            if (score < threshold) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Cosine (n-gram)", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Cosine (n-gram)")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> sorensenDiceSteps() {
        double threshold = 0.75;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = new SorensenDice().similarity(ca, cb);
                            if (score < threshold) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Sorensen-Dice", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Sorensen-Dice")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> nGramDistanceSteps() {
        double threshold = 0.25;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = new NGram().distance(ca, cb);
                            if (score > threshold) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "N-Gram Distance", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("N-Gram Distance")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> levenshteinSteps() {
        double threshold = 0.85;
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            int maxdistance = (int) Math.floor((1 - threshold) * Math.max(ca.length(), cb.length()));
                            int score = StringUtils.getLevenshteinDistance(ca, cb, maxdistance);
                            if (score == -1) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Levenshtein", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Levenshtein")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> orcidSteps() {
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            if (Objects.equals(orcid.getOrcid(), dblp.getOrcid())) {
                                return Optional.of(new AuthorMatch<>(dblp, orcid, "ORCID", 1.0));
                            }

                            return Optional.empty();
                        })
                        .name("ORCID")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> fullNameSteps() {
        return Arrays.asList(
                AuthorMatcherStep
                        .stringIgnoreCaseMatcher(DBLPAuthor::getName, ORCIDAuthor::getFullName)
                        .name("fullName")
                        .build()
        );
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> invertedFullNameSteps() {
        return Arrays.asList(
                AuthorMatcherStep
                        .stringIgnoreCaseMatcher(DBLPAuthor::getName, ORCIDAuthor::getInvertedFullName)
                        .name("invertedFullName")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> fullNameStripSteps() {
        return Arrays.asList(
                AuthorMatcherStep
                        .<DBLPAuthor, ORCIDAuthor>stringIgnoreCaseMatcher(a1 -> a1.getName().replaceAll("[ \\-]", ""), a2 -> ORCIDAuthor.getFullName(a2).replaceAll("[ \\-]", ""))
                        .name("fullNameStrip")
                        .build()
        );
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> invertedFullNameStripSteps() {
        return Arrays.asList(
                AuthorMatcherStep
                        .<DBLPAuthor, ORCIDAuthor>stringIgnoreCaseMatcher(a1 -> a1.getName().replaceAll("[ \\-]", ""), a2 -> ORCIDAuthor.getInvertedFullName(a2).replaceAll("[ \\-]", ""))
                        .name("invertedFullNameStrip")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> orderedTokensSteps() {
        return Arrays.asList(
                AuthorMatcherStep
                        .abbreviationsMatcher(DBLPAuthor::getName, ORCIDAuthor::getFullName)
                        .name("orderedTokens")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> creditNameSteps() {
        return Arrays.asList(
                AuthorMatcherStep
                        .stringIgnoreCaseMatcher(DBLPAuthor::getName, ORCIDAuthor::getCreditName)
                        .name("creditName")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> otherNamesSteps() {
        return Arrays.asList(
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            if (orcid.getOtherNames() != null &&
                                    orcid.getOtherNames().contains(dblp.getName())) {
                                return Optional.of(new AuthorMatch<>(dblp, orcid, "creditName", 1.0));
                            }

                            return Optional.empty();
                        })
                        .name("otherNames")
                        .build());
    }

    static List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> aiderSteps() {
        return Arrays.asList(
                AuthorMatcherStep
                        .stringIgnoreCaseMatcher(DBLPAuthor::getName, ORCIDAuthor::getFullName)
                        .name("fullName")
                        .build(),
                AuthorMatcherStep
                        .stringIgnoreCaseMatcher(DBLPAuthor::getName, ORCIDAuthor::getInvertedFullName)
                        .name("invertedFullName")
                        .build(),
                AuthorMatcherStep
                        .abbreviationsMatcher(DBLPAuthor::getName, ORCIDAuthor::getFullName)
                        .name("orderedTokens")
                        .build(),
                AuthorMatcherStep
                        .stringIgnoreCaseMatcher(DBLPAuthor::getName, ORCIDAuthor::getCreditName)
                        .name("creditName")
                        .build(),
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            if (orcid.getOtherNames() != null &&
                                    orcid.getOtherNames().contains(dblp.getName())) {
                                return Optional.of(new AuthorMatch<>(dblp, orcid, "creditName", 1.0));
                            }

                            return Optional.empty();
                        })
                        .name("otherNames")
                        .build(),
                AuthorMatcherStep
                        .<DBLPAuthor, ORCIDAuthor>stringIgnoreCaseMatcher(a1 -> a1.getName().replaceAll("[ \\-]", ""), a2 -> ORCIDAuthor.getFullName(a2).replaceAll("[ \\-]", ""))
                        .name("fullNameStrip")
                        .build(),
                AuthorMatcherStep
                        .<DBLPAuthor, ORCIDAuthor>stringIgnoreCaseMatcher(a1 -> a1.getName().replaceAll("[ \\-]", ""), a2 -> ORCIDAuthor.getInvertedFullName(a2).replaceAll("[ \\-]", ""))
                        .name("invertedFullNameStrip")
                        .build(),
                new AuthorMatcherStep.Builder<DBLPAuthor, ORCIDAuthor>()
                        .matchingFunc((dblp, orcid) -> {
                            String ca = dblp.getName().toLowerCase(Locale.ROOT);
                            String cb = ORCIDAuthor.getFullName(orcid).toLowerCase(Locale.ROOT);

                            double score = JaroWinklerOrderedToken.compare(ca, cb);
                            if (score < 0.9) {
                                return Optional.empty();
                            }
                            return Optional.of(new AuthorMatch<>(dblp, orcid, "Jaro-Winkler OrderedToken", 1.0 - (((double) Math.abs(score)) / Math.max(ca.length(), cb.length()))));
                        })
                        .name("Jaro-Winkler OrderedToken")
                        .build());
    }
}
