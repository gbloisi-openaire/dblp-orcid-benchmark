package eu.openaire.dblp_benchmark;

import eu.openaire.common.author.AuthorMatch;
import eu.openaire.common.author.AuthorMatcherStep;
import eu.openaire.common.author.AuthorMatchers;
import eu.openaire.dblp_benchmark.beans.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.ReduceFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

public class DBLPBenchmark implements Serializable {

    enum DataFilterMode implements Serializable {
        ALL_DATA,
        WORKS_WITH_ORCID,
        AUTHORS_WITH_ORCID
    }

    static List<DBLPAuthor> filterAuthors(List<DBLPAuthor> authors, DataFilterMode mode) {
        switch (mode) {
            case ALL_DATA:
                return authors;
            case WORKS_WITH_ORCID:
                boolean hasAnyOrcid = authors.stream()
                        .anyMatch(a -> !StringUtils.isEmpty(a.getOrcid()));
                return hasAnyOrcid ? authors : Collections.emptyList();
            case AUTHORS_WITH_ORCID:
                return authors.stream()
                        .filter(a -> !StringUtils.isEmpty(a.getOrcid()))
                        .collect(Collectors.toList());
            default:
                throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }


    static public void main(String[] args) throws Exception {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                params.put(args[i].substring(2), args[++i]);
            }
        }

        String orcidAuthorsPath = params.get("orcidAuthorsPath");
        String orcidWorksPath = params.get("orcidWorksPath");
        String dblpDump = params.get("dblpDump");
        String output = params.get("output");

        SparkSession spark = SparkSession.builder().getOrCreate();

        Dataset<org.apache.spark.sql.Row> orcidAuthors = spark.read().json(orcidAuthorsPath);

        Dataset<org.apache.spark.sql.Row> orcidWorkDois = spark.read().json(orcidWorksPath)
                .withColumn("pid", expr("explode(pids)"))
                .filter("lower(pid.schema) = 'doi'")
                .select(
                        col("orcid"),
                        lower(regexp_replace(col("pid.value"), "(?i)^https://doi\\.org/", "")).alias("doi")
                );

        Dataset<ORCIDWork> orcidWorks = orcidWorkDois
                .join(orcidAuthors, "orcid")
                .groupBy("doi")
                .agg(expr("collect_set(struct(orcid, familyName, givenName, creditName, otherNames)) as authors"))
                .as(Encoders.bean(ORCIDWork.class))
                .map((MapFunction<ORCIDWork, ORCIDWork>) w -> {
                            w.getAuthors().forEach(a -> {
                                if (a.getCreditName() != null)
                                    a.setCreditName(CleaningFunctions.cleanAuthorName(a.getCreditName()));
                                if (a.getFamilyName() != null)
                                    a.setFamilyName(CleaningFunctions.cleanAuthorName(a.getFamilyName()));
                                if (a.getGivenName() != null)
                                    a.setGivenName(CleaningFunctions.cleanAuthorName(a.getGivenName()));
                                if (a.getOtherNames() != null)
                                    a.setOtherNames(a.getOtherNames().stream().map(CleaningFunctions::cleanAuthorName).collect(Collectors.toList()));
                            });

                            return w;
                        }, Encoders.bean(ORCIDWork.class)
                );


        Dataset<DBLPWork> dblpWorks = spark.read().json(dblpDump)
                .withColumn(
                        "doi",
                        lower(regexp_replace(col("doi"), "(?i)^https://doi\\.org/", ""))
                )
                .as(Encoders.bean(DBLPWork.class))
                .map((MapFunction<DBLPWork, DBLPWork>) w -> {
                            w.getAuthors().forEach(a -> {
                                a.setName(CleaningFunctions.cleanAuthorName(a.getName()));
                            });
                            return w;
                        }, Encoders.bean(DBLPWork.class)
                );

        String cachePath = output + "_joined_cache";
        FileSystem fs = FileSystem.get(spark.sparkContext().hadoopConfiguration());
        Dataset<Tuple2<ORCIDWork, DBLPWork>> joined;

        if (fs.exists(new Path(cachePath))) {
            System.out.println("Reading joined dataset from cache: " + cachePath);
            joined = spark.read().parquet(cachePath)
                    .as(Encoders.tuple(Encoders.bean(ORCIDWork.class), Encoders.bean(DBLPWork.class)));
        } else {
            System.out.println("Computing joined dataset and saving to cache: " + cachePath);
            joined = orcidWorks
                    .joinWith(dblpWorks, orcidWorks.col("doi").equalTo(dblpWorks.col("doi")));
            joined.write().mode(SaveMode.Overwrite).parquet(cachePath);
            joined = spark.read().parquet(cachePath)
                    .as(Encoders.tuple(Encoders.bean(ORCIDWork.class), Encoders.bean(DBLPWork.class)));
        }

        for (DataFilterMode filterMode : DataFilterMode.values()) {
            System.out.println("\n### FILTER MODE: " + filterMode + " ###\n");

            List<String> scenarioNames = new ArrayList<>();
            List<ScoringResult> allResults = new ArrayList<>();

            for (int i = 0; i < BenchmarkScenario.SCENARIOS.size(); i++) {
                final int stepNo = i;
                String scenarioName = BenchmarkScenario.SCENARIOS.get(stepNo).name;
                System.out.println("=== [" + filterMode + "] Scenario: " + scenarioName + " ===");

                ScoringResult results = joined.map((MapFunction<Tuple2<ORCIDWork, DBLPWork>, DBLPEnrichedWork>) t -> {
                                    DBLPWork dblp = t._2();
                                    List<DBLPAuthor> baseAuthors = filterAuthors(dblp.getAuthors(), filterMode);
                                    if (baseAuthors.isEmpty()) {
                                        return new DBLPEnrichedWork(dblp.getDoi(), Collections.emptyList());
                                    }
                                    List<ORCIDAuthor> enrichingAuthors = t._1().getAuthors();

                                    List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> result =
                                            AuthorMatchers.findMatches(
                                                    baseAuthors,
                                                    enrichingAuthors,
                                                    BenchmarkScenario.SCENARIOS.get(stepNo).aiderSteps
                                            );

                                    List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> enrichedResult = baseAuthors.stream().map(ba -> {
                                                return result.stream().filter(r -> r.getBaseAuthor() == ba).findFirst()
                                                        .orElse(new AuthorMatch<DBLPAuthor, ORCIDAuthor>(ba, null, null, 0));

                                            }
                                    ).collect(Collectors.toList());


                                    // Manage ORCID Matching as a special scenario:
                                    // whatever is not matched by orcid id has to be accounted as a false positive against DBLP to account:
                                    // when dblp assign another orcid than the public one (case of journals)
                                    // when dblp genuinely miss the ORCID assignment
                                    // those Fp are a baseline also for the Openaire matching as it uses ORCID data and could incurr in same problems
                                    if (scenarioName.equalsIgnoreCase("ORCID Matching")) {
                                        List<ORCIDAuthor> orcidMatched = result.stream().filter(r -> r.getBaseAuthor() != null).map(r -> r.getEnrichingAuthor()).collect(Collectors.toList());
                                        List<ORCIDAuthor> orcidAdditions = new ArrayList<>(enrichingAuthors);
                                        orcidAdditions.removeAll(orcidMatched);
                                        enrichedResult.addAll(
                                                orcidAdditions
                                                        .stream()
                                                        .map(oa -> new AuthorMatch<DBLPAuthor, ORCIDAuthor>(null, oa, "ORCID Match", 1))
                                                        .collect(Collectors.toList())
                                        );

                                    }

                                    return new DBLPEnrichedWork(dblp.getDoi(), enrichedResult);
                                },
                                Encoders.bean(DBLPEnrichedWork.class))
                        .map(
                                (MapFunction<DBLPEnrichedWork, ScoringResult>) ScoringResult::scoreWork,
                                Encoders.bean(ScoringResult.class)
                        )
                        .reduce((ReduceFunction<ScoringResult>) ScoringResult::merge);

                results.printResults();

                if (BenchmarkScenario.SCENARIOS.get(stepNo).aiderSteps.size() > 1) {
                    results.printStepDrilldown(BenchmarkScenario.SCENARIOS.get(stepNo).aiderSteps.stream().map(AuthorMatcherStep::getName).collect(Collectors.toList()));
                }

                scenarioNames.add(scenarioName);
                allResults.add(results);
            }

            String modeSuffix = "_" + filterMode.name().toLowerCase();

            // Write comparative summary CSV
            String csvPath = output + "_benchmark_summary" + modeSuffix + ".csv";
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(fs.create(new Path(csvPath), true), StandardCharsets.UTF_8))) {
                pw.println("Scenario,TP,TN,FP,FN,Precision,Recall,F1,Accuracy,Specificity,Total");
                for (int i = 0; i < scenarioNames.size(); i++) {
                    ScoringResult r = allResults.get(i);
                    long tp = r.getTp(), tn = r.getTn(), fp = r.getFp(), fn = r.getFn();
                    double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
                    double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
                    double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0.0;
                    double accuracy = (tp + tn + fp + fn) > 0 ? (double) (tp + tn) / (tp + tn + fp + fn) : 0.0;
                    double specificity = (tn + fp) > 0 ? (double) tn / (tn + fp) : 0.0;
                    long total = tp + tn + fp + fn;
                    pw.printf("%s,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%d%n",
                            scenarioNames.get(i), tp, tn, fp, fn,
                            precision, recall, f1, accuracy, specificity, total);
                }
            }
            System.out.println("Benchmark summary CSV written to: " + csvPath);

            // Write step drilldown CSV for multi-step scenarios
            boolean hasMultiStep = false;
            for (BenchmarkScenario s : BenchmarkScenario.SCENARIOS) {
                if (s.aiderSteps.size() > 1) { hasMultiStep = true; break; }
            }
            if (hasMultiStep) {
                String stepCsvPath = output + "_step_drilldown" + modeSuffix + ".csv";
                try (PrintWriter pw = new PrintWriter(
                        new OutputStreamWriter(fs.create(new Path(stepCsvPath), true), StandardCharsets.UTF_8))) {
                    pw.println("Scenario,Step,TP,FP,FN,TN,StepPrecision,PctOfTotalTP,PctOfTotalFP");
                    for (int i = 0; i < scenarioNames.size(); i++) {
                        if (BenchmarkScenario.SCENARIOS.get(i).aiderSteps.size() <= 1) continue;
                        ScoringResult r = allResults.get(i);
                        List<String> steps = r.collectStepNames(BenchmarkScenario.SCENARIOS.get(i).aiderSteps.stream().map(AuthorMatcherStep::getName).collect(Collectors.toList()));

                        for (String step : steps) {
                            long stp = ScoringResult.getCount(r.getStepTp(), step);
                            long sfp = ScoringResult.getCount(r.getStepFp(), step);
                            long sfn = ScoringResult.getCount(r.getStepFn(), step);
                            long stn = ScoringResult.getCount(r.getStepTn(), step);
                            double stepPrec = (stp + sfp) > 0 ? (double) stp / (stp + sfp) : 0.0;
                            double pctTp = r.getTp() > 0 ? 100.0 * stp / r.getTp() : 0.0;
                            double pctFp = r.getFp() > 0 ? 100.0 * sfp / r.getFp() : 0.0;
                            pw.printf("%s,%s,%d,%d,%d,%d,%.4f,%.2f,%.2f%n",
                                    scenarioNames.get(i), step, stp, sfp, sfn, stn,
                                    stepPrec, pctTp, pctFp);
                        }
                    }
                }
                System.out.println("Step drilldown CSV written to: " + stepCsvPath);
            }
        }

    }


}
