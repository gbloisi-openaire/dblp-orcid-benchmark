## Project Overview
This is a research project to document and benchmark the https://github.com/openaire/author-name-matcher library: a library used in different contexts where high likely same authors needs to be disambiguated by using their textual name representation and where name could be spelled differently for different reasons (Initials, swapped order, typos, transliterations...)

This benchmark uses DBLP (Digital Bibliography & Library Project) publication records with ORCID author identifiers as reference ground truth. It matches authors from DBLP publications with their corresponding ORCID profiles using different matching algorithms.

## Build and Run

### Build
```bash
mvn clean package
```
Creates a shaded JAR at `target/dblp-orcid-benchmark-<version>-SNAPSHOT.jar`.

### Run (Spark cluster)
```bash
spark-submit --class eu.openaire.dblp_benchmark.DBLPBenchmark \
  target/dblp-orcid-benchmark-<version>-SNAPSHOT.jar \
    --orcidAuthorsPath  <path-to-orcid-authors-json> \
    --orcidWorksPath  <path-to-orcid-works-json>\
    --dblpDump <path-to-dblp-json>  \
    --output <output-path>
```

## Architecture

### Main Components
- **DBLPBenchmark.java** - Main Spark application entry point, orchestrates the ETL pipeline
- **beans/** - Data model POJOs for serialization:
    - `DBLPAuthor`, `DBLPWork` - DBLP publication models
    - `ORCIDAuthor`, `ORCIDWork` - ORCID profile models
    - `DBLPEnrichedWork` - Enriched output with matched author pairs

### Processing Pipeline
1. Read ORCID works JSON → explode DOIs → normalize DOI → group authors by DOI
2. Read DBLP works JSON → normalize DOI
3. Inner join on normalized DOI
4. Apply author matching algorithms in differente scenarios
5. Output benchmark metrics

## Key Dependencies

- **author-identifier-reconciler (AIDER)** v0.1.1 - Author matching library
- **Spark SQL** v4.0.0 (Scala 2.13) - Distributed data processing
- **JUnit 5** - Testing

## Coding Conventions

- Bean classes are simple POJOs with getters/setters for Spark SQL serialization
- Main class implements `Serializable` for Spark distributed computing
- Use Java Streams and lambdas for data transformation
- Use `ToStringBuilder` from Apache Commons for consistent toString implementations
- Static factory methods (e.g., `DBLPAuthor.of()`) for convenience constructors

## Repository Configuration

Custom Maven repositories for AIDER dependency:
- D-Net 45 Releases: `https://maven.d4science.org/nexus/content/repositories/dnet45-releases`
- D-Net 45 Snapshots: `https://maven.d4science.org/nexus/content/repositories/dnet45-snapshots`
