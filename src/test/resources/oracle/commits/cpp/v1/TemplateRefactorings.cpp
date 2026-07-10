template<typename T>
class ValueBox {
public:
    explicit ValueBox(T value) : value(value) {
    }

    const T &get_value() const {
        return value;
    }

    void replace_value(const T &next) {
        value = next;
    }

private:
    T value;
};

template<typename T, int Capacity>
class FixedBuffer {
public:
    FixedBuffer() : size(0) {
    }

    bool append(const T &entry) {
        if(size == Capacity) {
            return false;
        }
        entries[size] = entry;
        ++size;
        return true;
    }

    T first_or(const T &fallback) const {
        if(size == 0) {
            return fallback;
        }
        return entries[0];
    }

private:
    T entries[Capacity];
    int size;
};

template<typename FieldType>
class FieldAttributes {
public:
    FieldAttributes(const char *name, FieldType default_value, bool required)
        : name(name), default_value(default_value), required(required) {
    }

    const char *field_name() const {
        return name;
    }

    FieldType fallback_value() const {
        return default_value;
    }

    bool is_required() const {
        return required;
    }

private:
    const char *name;
    FieldType default_value;
    bool required;
};

template<typename T>
T clamp_value(T value, T low, T high) {
    if(value < low) {
        return low;
    }
    if(value > high) {
        return high;
    }
    return value;
}

template<typename Iterator, typename T>
T sum_values(Iterator first, Iterator last, T initial) {
    T total = initial;
    for(Iterator current = first; current != last; ++current) {
        total += *current;
    }
    return total;
}

int exercise_template_refactorings() {
    ValueBox<int> box(42);
    box.replace_value(clamp_value(box.get_value(), 0, 100));

    FieldAttributes<int> threshold_attributes("threshold", 10, true);

    FixedBuffer<int, 4> buffer;
    buffer.append(box.get_value());
    buffer.append(threshold_attributes.fallback_value());

    int readings[3] = {buffer.first_or(0), 5, 7};
    if(threshold_attributes.is_required()) {
        readings[1] += 1;
    }
    return sum_values(readings, readings + 3, 0);
}
