package eu.openaire.dblp_benchmark;

import eu.openaire.common.author.AuthorMatch;
import eu.openaire.dblp_benchmark.beans.DBLPAuthor;
import eu.openaire.dblp_benchmark.beans.DBLPEnrichedWork;
import eu.openaire.dblp_benchmark.beans.ORCIDAuthor;

import java.io.Serializable;
import java.util.*;

public class ScoringResult implements Serializable {
    static final String UNMATCHED = "(unmatched)";

    private long tp;
    private long tn;
    private long fp;
    private long fn;

    private Map<String, Long> stepTp;
    private Map<String, Long> stepTn;
    private Map<String, Long> stepFp;
    private Map<String, Long> stepFn;

    public ScoringResult() {
        this.tp = 0;
        this.tn = 0;
        this.fp = 0;
        this.fn = 0;
    }

    private static void increment(Map<String, Long> map, String key) {
        map.merge(key, 1L, Long::sum);
    }

    /**
     * Scores a single enriched work, classifying every author match as TP/TN/FP/FN.
     * The result also carries a per-step breakdown keyed by
     * {@link AuthorMatch#getStepName()}.
     */
    static ScoringResult scoreWork(DBLPEnrichedWork work) {
        ScoringResult result = new ScoringResult();

        List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> matches = work.getMatches();
        if (matches == null) {
            return result;
        }

        for (AuthorMatch<DBLPAuthor, ORCIDAuthor> match : matches) {
            DBLPAuthor baseAuthor = match.getBaseAuthor();
            ORCIDAuthor enrichingAuthor = match.getEnrichingAuthor();

            String groundTruthOrcid = baseAuthor != null ? baseAuthor.getOrcid() : null;
            String predictedOrcid = enrichingAuthor != null ? enrichingAuthor.getOrcid() : null;

            boolean hasGroundTruth = groundTruthOrcid != null && !groundTruthOrcid.isEmpty();
            boolean hasPrediction = predictedOrcid != null && !predictedOrcid.isEmpty();

            String step = match.getStepName();
            if (step == null || step.isEmpty()) {
                step = UNMATCHED;
            }

            if (hasGroundTruth && hasPrediction) {
                if (Objects.equals(groundTruthOrcid, predictedOrcid)) {
                    result.tp++;
                    increment(result.ensureStepTp(), step);
                } else {
                    result.fp++;
                    increment(result.ensureStepFp(), step);
                }
            } else if (hasGroundTruth && !hasPrediction) {
                result.fn++;
                increment(result.ensureStepFn(), step);
            } else if (!hasGroundTruth && hasPrediction) {
                result.fp++;
                increment(result.ensureStepFp(), step);
            } else {
                result.tn++;
                increment(result.ensureStepTn(), step);
            }
        }

        return result;
    }

    static Map<String, Long> mergeMaps(Map<String, Long> a, Map<String, Long> b) {
        if ((a == null || a.isEmpty()) && (b == null || b.isEmpty())) {
            return null;
        }
        Map<String, Long> merged = new HashMap<>();
        if (a != null) a.forEach((k, v) -> merged.merge(k, v, Long::sum));
        if (b != null) b.forEach((k, v) -> merged.merge(k, v, Long::sum));
        return merged;
    }

    /**
     * Collects all step names from a ScoringResult, placing
     * {@link #UNMATCHED} last.
     */
    List<String> collectStepNames(List<String> orderedNames) {
        Set<String> all = new HashSet<>();
        if (getStepTp() != null) all.addAll(getStepTp().keySet());
        if (getStepFp() != null) all.addAll(getStepFp().keySet());
        if (getStepFn() != null) all.addAll(getStepFn().keySet());
        if (getStepTn() != null) all.addAll(getStepTn().keySet());

        List<String> steps = new ArrayList<>();
        for (String name : orderedNames) {
            if (all.remove(name)) {
                steps.add(name);
            }
        }
        all.remove(UNMATCHED);
        steps.addAll(all);
        steps.add(UNMATCHED);
        return steps;
    }

    static long getCount(Map<String, Long> map, String key) {
        return map != null ? map.getOrDefault(key, 0L) : 0L;
    }

    public ScoringResult merge(ScoringResult other) {
        ScoringResult m = new ScoringResult();
        m.tp = this.tp + other.tp;
        m.tn = this.tn + other.tn;
        m.fp = this.fp + other.fp;
        m.fn = this.fn + other.fn;
        m.stepTp = mergeMaps(this.stepTp, other.stepTp);
        m.stepTn = mergeMaps(this.stepTn, other.stepTn);
        m.stepFp = mergeMaps(this.stepFp, other.stepFp);
        m.stepFn = mergeMaps(this.stepFn, other.stepFn);
        return m;
    }

     Map<String, Long> ensureStepTp() {
        if (stepTp == null) stepTp = new HashMap<>();
        return stepTp;
    }

     Map<String, Long> ensureStepTn() {
        if (stepTn == null) stepTn = new HashMap<>();
        return stepTn;
    }

     Map<String, Long> ensureStepFp() {
        if (stepFp == null) stepFp = new HashMap<>();
        return stepFp;
    }

     Map<String, Long> ensureStepFn() {
        if (stepFn == null) stepFn = new HashMap<>();
        return stepFn;
    }

    // --- getters & setters ---

    public long getTp() {
        return tp;
    }

    public void setTp(long tp) {
        this.tp = tp;
    }

    public long getTn() {
        return tn;
    }

    public void setTn(long tn) {
        this.tn = tn;
    }

    public long getFp() {
        return fp;
    }

    public void setFp(long fp) {
        this.fp = fp;
    }

    public long getFn() {
        return fn;
    }

    public void setFn(long fn) {
        this.fn = fn;
    }

    public Map<String, Long> getStepTp() {
        return stepTp;
    }

    public void setStepTp(Map<String, Long> stepTp) {
        this.stepTp = stepTp;
    }

    public Map<String, Long> getStepTn() {
        return stepTn;
    }

    public void setStepTn(Map<String, Long> stepTn) {
        this.stepTn = stepTn;
    }

    public Map<String, Long> getStepFp() {
        return stepFp;
    }

    public void setStepFp(Map<String, Long> stepFp) {
        this.stepFp = stepFp;
    }

    public Map<String, Long> getStepFn() {
        return stepFn;
    }

    public void setStepFn(Map<String, Long> stepFn) {
        this.stepFn = stepFn;
    }


    void printResults() {
        long tp = getTp(), tn = getTn(), fp = getFp(), fn = getFn();

        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0.0;
        double accuracy = (tp + tn + fp + fn) > 0 ? (double) (tp + tn) / (tp + tn + fp + fn) : 0.0;
        double specificity = (tn + fp) > 0 ? (double) tn / (tn + fp) : 0.0;

        System.out.println("=== DBLP Enrichment Scoring Results ===");
        System.out.println();
        System.out.println("Confusion Matrix:");
        System.out.println("  True Positives  (TP): " + tp);
        System.out.println("  True Negatives  (TN): " + tn);
        System.out.println("  False Positives (FP): " + fp);
        System.out.println("  False Negatives (FN): " + fn);
        System.out.println();
        System.out.println("Metrics:");
        System.out.printf("  Precision:   %.4f%n", precision);
        System.out.printf("  Recall:      %.4f%n", recall);
        System.out.printf("  F1 Score:    %.4f%n", f1);
        System.out.printf("  Accuracy:    %.4f%n", accuracy);
        System.out.printf("  Specificity: %.4f%n", specificity);
        System.out.println();
        System.out.println("Total authors evaluated: " + (tp + tn + fp + fn));
    }

    /**
     * Prints a per-step drilldown table showing how each matching step
     * contributed to the overall confusion matrix.
     */
    void printStepDrilldown(List<String> orderedStepNames) {
        List<String> steps = collectStepNames(orderedStepNames);
        if (steps.isEmpty()) {
            return;
        }

        long totalTp = getTp();
        long totalFp = getFp();

        System.out.println();
        System.out.println("  --- Step Drilldown ---");
        System.out.printf("  %-30s %6s %6s %6s %6s %10s %12s %12s%n",
                "Step", "TP", "FP", "FN", "TN", "Precision", "% Total TP", "% Total FP");
        System.out.println("  " + String.join("", Collections.nCopies(100, "-")));

        for (String step : steps) {
            long stp = ScoringResult.getCount(getStepTp(), step);
            long sfp = ScoringResult.getCount(getStepFp(), step);
            long sfn = ScoringResult.getCount(getStepFn(), step);
            long stn = ScoringResult.getCount(getStepTn(), step);

            String precision = (stp + sfp) > 0
                    ? String.format("%.4f", (double) stp / (stp + sfp))
                    : "-";
            String pctTp = totalTp > 0 && stp > 0
                    ? String.format("%.2f%%", 100.0 * stp / totalTp)
                    : "-";
            String pctFp = totalFp > 0 && sfp > 0
                    ? String.format("%.2f%%", 100.0 * sfp / totalFp)
                    : "-";

            System.out.printf("  %-30s %6d %6d %6d %6d %10s %12s %12s%n",
                    step, stp, sfp, sfn, stn, precision, pctTp, pctFp);
        }
    }
}
