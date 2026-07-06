int recover_from_error(int code) {
    return code;
}

int exercise_try_catch_statement(int raw_value) {
    try {
        if(raw_value < 0) {
            throw raw_value;
        }
        return raw_value;
    }
    catch(int error_code) {
        return recover_from_error(error_code);
    }
}
