struct MetricPair {
    int key;
    int value;
};

int exercise_structured_binding_range_for() {
    MetricPair entries[2] = {{1, 10}, {2, 20}};
    int sum = 0;

    for(auto [name, amount] : entries) {
        sum += name + amount;
    }

    return sum;
}
