package eu.openaire.dblp_benchmark.beans;

import java.util.List;

public class DBLPWork {
    String doi;

    List<DBLPAuthor> authors;

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public List<DBLPAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<DBLPAuthor> authors) {
        this.authors = authors;
    }
}
