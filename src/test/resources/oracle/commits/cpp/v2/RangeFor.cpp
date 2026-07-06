int exercise_range_for_statement() {
    int values[3] = {1, 2, 3};
    int sum = 0;

    for(int value : values) {
        sum += value;
    }

    return sum;
}
