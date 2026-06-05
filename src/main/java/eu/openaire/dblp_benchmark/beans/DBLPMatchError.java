package eu.openaire.dblp_benchmark.beans;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

public class DBLPMatchError implements Serializable {

    private String errorType;           // "FP" or "FN"
    private String doi;
    private String dblpAuthorName;
    private String dblpOrcid;

    // FP-specific: the wrong author that was matched
    private String predictedOrcid;
    private String predictedAuthorGivenName;
    private String predictedAuthorFamilyName;
    private String matchStepName;

    // FN-specific: what the ORCID dataset has for the correct author (to diagnose why name matching failed)
    private String orcidDatasetGivenName;
    private String orcidDatasetFamilyName;
    private String orcidDatasetCreditName;
    private String orcidDatasetOtherNames;

    public DBLPMatchError() {
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getDblpAuthorName() {
        return dblpAuthorName;
    }

    public void setDblpAuthorName(String dblpAuthorName) {
        this.dblpAuthorName = dblpAuthorName;
    }

    public String getDblpOrcid() {
        return dblpOrcid;
    }

    public void setDblpOrcid(String dblpOrcid) {
        this.dblpOrcid = dblpOrcid;
    }

    public String getPredictedOrcid() {
        return predictedOrcid;
    }

    public void setPredictedOrcid(String predictedOrcid) {
        this.predictedOrcid = predictedOrcid;
    }

    public String getPredictedAuthorGivenName() {
        return predictedAuthorGivenName;
    }

    public void setPredictedAuthorGivenName(String predictedAuthorGivenName) {
        this.predictedAuthorGivenName = predictedAuthorGivenName;
    }

    public String getPredictedAuthorFamilyName() {
        return predictedAuthorFamilyName;
    }

    public void setPredictedAuthorFamilyName(String predictedAuthorFamilyName) {
        this.predictedAuthorFamilyName = predictedAuthorFamilyName;
    }

    public String getMatchStepName() {
        return matchStepName;
    }

    public void setMatchStepName(String matchStepName) {
        this.matchStepName = matchStepName;
    }

    public String getOrcidDatasetGivenName() {
        return orcidDatasetGivenName;
    }

    public void setOrcidDatasetGivenName(String orcidDatasetGivenName) {
        this.orcidDatasetGivenName = orcidDatasetGivenName;
    }

    public String getOrcidDatasetFamilyName() {
        return orcidDatasetFamilyName;
    }

    public void setOrcidDatasetFamilyName(String orcidDatasetFamilyName) {
        this.orcidDatasetFamilyName = orcidDatasetFamilyName;
    }

    public String getOrcidDatasetCreditName() {
        return orcidDatasetCreditName;
    }

    public void setOrcidDatasetCreditName(String orcidDatasetCreditName) {
        this.orcidDatasetCreditName = orcidDatasetCreditName;
    }

    public String getOrcidDatasetOtherNames() {
        return orcidDatasetOtherNames;
    }

    public void setOrcidDatasetOtherNames(String orcidDatasetOtherNames) {
        this.orcidDatasetOtherNames = orcidDatasetOtherNames;
    }

    public void setOrcidDatasetOtherNames(List<String> orcidDatasetOtherNames) {
        this.orcidDatasetOtherNames = Joiner.on('|').join(orcidDatasetOtherNames);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, NO_CLASS_NAME_STYLE)
                .append("errorType", errorType)
                .append("doi", doi)
                .append("dblpAuthorName", dblpAuthorName)
                .append("groundTruthOrcid", dblpOrcid)
                .append("predictedOrcid", predictedOrcid)
                .append("predictedAuthorGivenName", predictedAuthorGivenName)
                .append("predictedAuthorFamilyName", predictedAuthorFamilyName)
                .append("matchStepName", matchStepName)
                .append("orcidDatasetGivenName", orcidDatasetGivenName)
                .append("orcidDatasetFamilyName", orcidDatasetFamilyName)
                .append("orcidDatasetCreditName", orcidDatasetCreditName)
                .append("orcidDatasetOtherNames", orcidDatasetOtherNames)
                .toString();
    }
}
