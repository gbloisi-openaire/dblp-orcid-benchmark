package eu.openaire.dblp_benchmark.beans;

import eu.openaire.common.author.AuthorMatch;

import java.util.List;

public class DBLPEnrichedWork {
    String doi;

    List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> matches;

    public DBLPEnrichedWork() {
    }

    public DBLPEnrichedWork(String doi, List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> matches) {
        this.doi = doi;
        this.matches = matches;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> getMatches() {
        return matches;
    }

    public void setMatches(List<AuthorMatch<DBLPAuthor, ORCIDAuthor>> matches) {
        this.matches = matches;
    }
}
