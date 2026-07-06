// Include directive: asks the preprocessor to load declarations/macros from another file.
#include "SensorPlatform.h"

// Pragma directive: compiler/preprocessor-specific instruction.
#pragma once

// Ifndef/define/endif guard: defines SENSOR_PLATFORM only when it was not already defined.
#ifndef SENSOR_PLATFORM
#define SENSOR_PLATFORM 2
#endif

// Ifdef/else/endif branch: chooses a macro based on whether SENSOR_PLATFORM is defined.
#ifdef SENSOR_PLATFORM
#define SENSOR_HAS_PLATFORM 1
#else
#define SENSOR_HAS_PLATFORM 0
#endif

// Object-style macro definitions: simple name-to-value/text substitutions.
#define FEATURE_DIAGNOSTICS 1
#define BUFFER_CAPACITY 64
#define DEFAULT_TIMEOUT_MS 250
#define MODULE_NAME "sensor"

// Undef directive: removes the previous MODULE_NAME definition before redefining it.
#undef MODULE_NAME
#define MODULE_NAME "sensor-config"

// If/elif/else/endif branch: selects exactly one BUFFER_BUCKET definition.
#if BUFFER_CAPACITY > 100
#define BUFFER_BUCKET 2
#elif BUFFER_CAPACITY == 64
#define BUFFER_BUCKET 1
#else
#define BUFFER_BUCKET 0
#endif

// Error directive: kept inactive so CDT can see the statement shape without failing the fixture.
#if 0
#error "disabled v1 fixture error"
#endif

// Function-style macros: behave like parameterized text substitutions.
#define SQUARE(x) ((x) * (x))
#define CLAMP(value, low, high) ((value) < (low) ? (low) : ((value) > (high) ? (high) : (value)))

// Token-pasting macro: counter_##name creates identifiers such as counter_samples.
#define DECLARE_COUNTER(name) static int counter_##name = 0

// Stringizing macro: #value turns the argument token into a string literal.
#define TO_STRING(value) #value

// Variadic macro: __VA_ARGS__ forwards the extra arguments.
#define LOG_EVENT(format, ...) record_event(MODULE_NAME, format, __VA_ARGS__)

// Conditional macro definition: DIAGNOSTIC_FIELD expands to a different field declaration.
#if FEATURE_DIAGNOSTICS
#define DIAGNOSTIC_FIELD int last_error
#else
#define DIAGNOSTIC_FIELD int diagnostics_disabled
#endif

class SensorConfig {
public:
    int sample_rate;
    int timeout_ms;
    int platform_enabled;
    int buffer_bucket;
    // Macro expansion inside a class declaration.
    DIAGNOSTIC_FIELD;
};

// Macro expansion creates a static counter declaration.
DECLARE_COUNTER(samples);

static void record_event(const char *module, const char *format, int value) {
    (void)module;
    (void)format;
    (void)value;
}

static int normalize_sample(int sample) {
    // CLAMP and BUFFER_CAPACITY are macro expansions inside a function body.
    int bounded = CLAMP(sample, 0, BUFFER_CAPACITY);
    counter_samples = counter_samples + 1;
    // SQUARE is a function-style macro expansion.
    return SQUARE(bounded);
}

int guarded_sensor_read(int raw_sample) try {
    if(raw_sample < 0) {
        throw raw_sample;
    }
    return normalize_sample(raw_sample);
}
catch(int error_code) {
    LOG_EVENT("error=%d", error_code);
    return 0;
}

int read_sensor_value(SensorConfig *config, int raw_sample) {
    config->timeout_ms = DEFAULT_TIMEOUT_MS;
    // These assignments use object-style macro expansions.
    config->platform_enabled = SENSOR_HAS_PLATFORM;
    config->buffer_bucket = BUFFER_BUCKET;
    config->last_error = 0;
    // Variadic macro expansion.
    LOG_EVENT("raw=%d", raw_sample);
    return normalize_sample(raw_sample);
}

int analyze_sensor_batch() {
    int readings[3] = {4, 12, 80};
    int total = 0;
    for(int reading : readings) {
        if(int adjusted = normalize_sample(reading)) {
            total += adjusted;
        }
    }
    try {
        if(total < 0) {
            throw total;
        }
        return total;
    }
    catch(int error_code) {
        LOG_EVENT("batch_error=%d", error_code);
        return 0;
    }
}

int route_sensor_state(int raw_sample) {
    int state = 0;
    if(int normalized = normalize_sample(raw_sample)) {
        state += normalized;
    }
    while(int retry = state < BUFFER_CAPACITY) {
        state += retry;
        break;
    }
    switch(int bucket = state / 10) {
        case 0:
            state += bucket;
            break;
        default:
            state += BUFFER_BUCKET;
            break;
    }
    int keep_running = 0;
    for(int attempt = 0; (keep_running = (attempt < 2)); ++attempt) {
        state += keep_running;
    }
    return state;
}

const char *sensor_macro_name() {
    // TO_STRING stringizes BUFFER_CAPACITY instead of evaluating it as an integer.
    return TO_STRING(BUFFER_CAPACITY);
}
