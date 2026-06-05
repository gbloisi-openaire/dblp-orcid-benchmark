package eu.openaire.dblp_benchmark;

import eu.openaire.common.author.AuthorMatch;
import eu.openaire.common.author.AuthorMatcherStep;
import eu.openaire.common.author.AuthorMatchers;
import eu.openaire.dblp_benchmark.beans.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.*;
import scala.Tuple2;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes false positives and false negatives produced by OpenAIRE (AIDER) name matching.
 *
 * <p>Input: the joined ORCID/DBLP parquet cache produced by {@link DBLPBenchmark} (the path
 * {@code <output>_joined_cache} from that job).
 *
 * <p>Output (JSON): one record per erroneous classification:
 * <ul>
 *   <li><b>FP (false positive)</b>: OpenAIRE matched a DBLP author to an ORCID author whose ORCID
 *       does not match the ground-truth ORCID stored in the DBLP record.</li>
 *   <li><b>FN (false negative)</b>: OpenAIRE failed to match a DBLP author that has a ground-truth
 *       ORCID, <em>but only when ORCID-ID matching would have succeeded</em> (i.e. the correct
 *       author IS present in the ORCID dataset for that DOI). FNs where the correct author is
 *       absent from the ORCID dataset are excluded because those are data-coverage gaps, not
 *       algorithm failures.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * spark-submit --class eu.openaire.dblp_enricher.DBLPFalsePositiveAnalyzer \
 *   target/dblp-orcid-benchmark-0.1.1-SNAPSHOT.jar \
 *   --cachePath <output>_joined_cache \
 *   --output <output-path>
 * }</pre>
 */
public class DBLPResultAnalyzer implements Serializable {

    public static void main(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                params.put(args[i].substring(2), args[++i]);
            }
        }

        String orcidAuthorsPath = params.get("orcidAuthorsPath");
        String orcidWorksPath = params.get("orcidWorksPath");
        String cachePath = params.get("cachePath");
        String output = params.get("output");

        SparkSession spark = SparkSession.builder().getOrCreate();

        Dataset<org.apache.spark.sql.Row> authors = spark.read().json(orcidAuthorsPath).select("orcid");
        Dataset<org.apache.spark.sql.Row> authorsWithData = spark.read().json(orcidWorksPath).selectExpr("orcid as orcidWithData").distinct();

        Dataset<Tuple2<ORCIDWork, DBLPWork>> joined = spark.read().parquet(cachePath)
                .as(Encoders.tuple(Encoders.bean(ORCIDWork.class), Encoders.bean(DBLPWork.class)));

        Dataset<DBLPMatchError> errors = joined.flatMap(
                (FlatMapFunction<Tuple2<ORCIDWork, DBLPWork>, DBLPMatchError>) t -> {
                    DBLPWork dblp = t._2();
                    List<DBLPAuthor> baseAuthors = dblp.getAuthors().stream()
                            .filter(ba -> !StringUtils.isEmpty(ba.getOrcid()))
                            .collect(Collectors.toList());
                    List<ORCIDAuthor> orcidAuthors = t._1().getAuthors();
                    String doi = dblp.getDoi();

                    // Instantiate steps here (inside the task) to avoid serializing lambdas
                    List<AuthorMatcherStep<DBLPAuthor, ORCIDAuthor>> openAireSteps = BenchmarkScenario.aiderSteps();

                    List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> openAireMatches =
                            AuthorMatchers.findMatches(baseAuthors, orcidAuthors, openAireSteps);

                    List<DBLPMatchError> result = new ArrayList<>();

                    for (DBLPAuthor baseAuthor : baseAuthors) {
                        String dblpOrcid = baseAuthor.getOrcid();
                        //if (dblpOrcid == null || dblpOrcid.isEmpty()) {
                        //   continue; // no ground truth → skip
                        //}

                        Optional<AuthorMatch<DBLPAuthor, ORCIDAuthor>> match = openAireMatches.stream()
                                .filter(m -> m.getBaseAuthor() == baseAuthor)
                                .findFirst();

                        ORCIDAuthor matchedOrcidAuthor = match
                                .map(AuthorMatch::getEnrichingAuthor)
                                .orElse(null);

                        String predictedOrcid = matchedOrcidAuthor != null
                                ? matchedOrcidAuthor.getOrcid()
                                : null;
                        boolean hasPrediction = predictedOrcid != null && !predictedOrcid.isEmpty();

                        if (hasPrediction && !Objects.equals(dblpOrcid, predictedOrcid)) {
                            // FALSE POSITIVE: matched to wrong ORCID author
                            DBLPMatchError error = new DBLPMatchError();
                            error.setErrorType("FP");
                            error.setDoi(doi);
                            error.setDblpAuthorName(baseAuthor.getName());
                            error.setDblpOrcid(dblpOrcid);
                            error.setPredictedOrcid(predictedOrcid);
                            error.setPredictedAuthorGivenName(matchedOrcidAuthor.getGivenName());
                            error.setPredictedAuthorFamilyName(matchedOrcidAuthor.getFamilyName());
                            error.setMatchStepName(match.get().getStepName());
                            result.add(error);

                        } else if (!hasPrediction) {
                            // FALSE NEGATIVE: no match found by OpenAIRE.
                            // Only report if the correct ORCID author IS present in the ORCID dataset
                            // for this DOI (i.e. ORCID-ID matching would have succeeded).
                            // Cases where the author is absent from the dataset are data-coverage
                            // gaps and would also be FN by ORCID matching — those are excluded.
                            Optional<ORCIDAuthor> correctOrcidAuthor = orcidAuthors.stream()
                                    .filter(a -> Objects.equals(dblpOrcid, a.getOrcid()))
                                    .findFirst();

                            if (correctOrcidAuthor.isPresent()) {
                                ORCIDAuthor oa = correctOrcidAuthor.get();
                                DBLPMatchError error = new DBLPMatchError();
                                error.setErrorType("FN");
                                error.setDoi(doi);
                                error.setDblpAuthorName(baseAuthor.getName());
                                error.setDblpOrcid(dblpOrcid);
                                error.setOrcidDatasetGivenName(oa.getGivenName());
                                error.setOrcidDatasetFamilyName(oa.getFamilyName());
                                error.setOrcidDatasetCreditName(oa.getCreditName());
                                error.setOrcidDatasetOtherNames(oa.getOtherNames());
                                result.add(error);
                            }
                        }
                    }

                    return result.iterator();
                },
                Encoders.bean(DBLPMatchError.class)
        );

        errors.filter("errorType = 'FP'")
                .drop("orcidDatasetCreditName", "orcidDatasetFamilyName",
                        "orcidDatasetGivenName", "orcidDatasetOtherNames"
                )
                .join(authors, new Column("dblpOrcid").equalTo(authors.col("orcid")), "left")
                .join(authorsWithData, new Column("dblpOrcid").equalTo(authorsWithData.col("orcidWithData")),"left")
                .selectExpr(
                        "dblpAuthorName",
                        "dblpOrcid",
                        "CASE WHEN orcid IS NOT NULL THEN true ELSE false END as VisibleOrcid",
                        "CASE WHEN orcidWithData IS NOT NULL THEN true ELSE false END as orcidWithData",
                        "doi",
                        "matchStepName",
                        "predictedAuthorFamilyName",
                        "predictedAuthorGivenName",
                        "predictedOrcid"

                )
                .coalesce(1)
                .write()
                .mode(SaveMode.Overwrite)
                .option("header", "true")
                .csv(output + "_fp");

        errors.filter("errorType = 'FN'")
                .drop("predictedOrcid", "predictedAuthorGivenName",
                        "predictedAuthorFamilyName", "matchStepName")
                .coalesce(1)
                .write()
                .mode(SaveMode.Overwrite)
                .option("header", "true")
                .csv(output + "_fn");

        System.out.println("False positives written to: " + output + "_fp");
        System.out.println("False negatives written to: " + output + "_fn");
    }
}