int normalize_control_value(int input) {
    return input + 1;
}

int exercise_control_header_declarations(int raw_value) {
    int sum = 0;

    if(int adjusted = normalize_control_value(raw_value)) {
        sum += adjusted;
    }

    while(int should_retry = sum < 4) {
        sum += should_retry;
        break;
    }

    switch(int group = sum / 2) {
        case 0:
            sum += group;
            break;
        default:
            sum += 1;
            break;
    }

    for(int position = 0; int keep_going = position < 2; ++position) {
        sum += keep_going;
    }

    return sum;
}
