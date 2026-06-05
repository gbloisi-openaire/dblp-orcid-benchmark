
package eu.openaire.dblp_benchmark.beans;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class DBLPAuthor {

    private String name;

    private String orcid;

    public DBLPAuthor() {
    }

    public DBLPAuthor(String name) {
        this.name = name;
    }

    public static List<DBLPAuthor> of(String... fullNames) {
        return Arrays.stream(fullNames).map(DBLPAuthor::new).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, NO_CLASS_NAME_STYLE)
                .append("name", name)
                .toString();
    }
}
