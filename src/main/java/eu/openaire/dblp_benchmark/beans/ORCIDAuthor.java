
package eu.openaire.dblp_benchmark.beans;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class ORCIDAuthor {
    String givenName;
    String familyName;
    String creditName;
    List<String> otherNames;
    String orcid;

    public ORCIDAuthor() {

    }

    public ORCIDAuthor(String name, String surname, String creditName, String orcid) {
        this.givenName = name;
        this.familyName = surname;
        this.creditName = creditName;
        this.orcid = orcid;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getCreditName() {
        return creditName;
    }

    public void setCreditName(String creditName) {
        this.creditName = creditName;
    }

    public List<String> getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(List<String> otherNames) {
        this.otherNames = otherNames;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public static String getFullName(ORCIDAuthor a) {
        return a.getGivenName() + " " + a.getFamilyName();
    }

    public static String getInvertedFullName(ORCIDAuthor a) {
        return a.getFamilyName() + " " + a.getGivenName();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, NO_CLASS_NAME_STYLE)
                .append("givenName", givenName)
                .append("familyName", familyName)
                .append("creditName", creditName)
                .append("otherNames", otherNames)
                .append("orcid", orcid)
                .toString();
    }
}
