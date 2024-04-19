package benchmark.metrics.computers.violation.models;

public record SemanticViolationRecord(
        String parentTypesPair,
        String first,
        String second,
        String url,
        // String temp,
        String filename) {
}


