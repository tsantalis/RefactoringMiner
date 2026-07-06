int normalize_control_value(int input) {
    return input + 1;
}

int exercise_control_header_declarations(int raw_value) {
    int total = 0;

    if(int normalized = normalize_control_value(raw_value)) {
        total += normalized;
    }

    while(int keep_running = total < 4) {
        total += keep_running;
        break;
    }

    switch(int bucket = total / 2) {
        case 0:
            total += bucket;
            break;
        default:
            total += 1;
            break;
    }

    for(int index = 0; int should_continue = index < 2; ++index) {
        total += should_continue;
    }

    return total;
}
