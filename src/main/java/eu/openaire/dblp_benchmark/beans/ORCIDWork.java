package eu.openaire.dblp_benchmark.beans;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

public class ORCIDWork {
    String doi;
    List<ORCIDAuthor> authors;

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public List<ORCIDAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<ORCIDAuthor> authors) {
        this.authors = authors;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, NO_CLASS_NAME_STYLE)
                .append("doi", doi)
                .append("authors", authors)
                .toString();
    }
}
