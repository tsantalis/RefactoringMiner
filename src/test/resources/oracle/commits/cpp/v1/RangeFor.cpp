int exercise_range_for_statement() {
    int readings[3] = {1, 2, 3};
    int total = 0;

    for(int reading : readings) {
        total += reading;
    }

    return total;
}
