struct MetricPair {
    int key;
    int value;
};

int exercise_structured_binding_range_for() {
    MetricPair pairs[2] = {{1, 10}, {2, 20}};
    int total = 0;

    for(auto [key, value] : pairs) {
        total += key + value;
    }

    return total;
}
