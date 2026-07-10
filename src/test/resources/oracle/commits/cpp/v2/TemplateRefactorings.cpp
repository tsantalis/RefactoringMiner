template<typename Element>
class ValueBox {
public:
    explicit ValueBox(Element payload) : payload(payload) {
    }

    const Element &read_payload() const {
        return payload;
    }

    void assign_payload(const Element &candidate) {
        payload = candidate;
    }

private:
    Element payload;
};

template<typename Element, int Limit>
class FixedBuffer {
public:
    FixedBuffer() : count(0) {
    }

    bool push(const Element &item) {
        if(count == Limit) {
            return false;
        }
        items[count] = item;
        ++count;
        return true;
    }

    Element first_or_default(const Element &fallback) const {
        if(count == 0) {
            return fallback;
        }
        return items[0];
    }

private:
    Element items[Limit];
    int count;
};

template<typename AttributeType>
class FieldAttributes {
public:
    FieldAttributes(const char *key, AttributeType initial_value, bool mandatory)
        : key(key), initial_value(initial_value), mandatory(mandatory) {
    }

    const char *attribute_key() const {
        return key;
    }

    AttributeType default_candidate() const {
        return initial_value;
    }

    bool is_mandatory() const {
        return mandatory;
    }

private:
    const char *key;
    AttributeType initial_value;
    bool mandatory;
};

template<typename Element>
Element bound_value(Element candidate, Element minimum, Element maximum) {
    if(candidate < minimum) {
        return minimum;
    }
    if(candidate > maximum) {
        return maximum;
    }
    return candidate;
}

template<typename InputIterator, typename Element>
Element accumulate_values(InputIterator begin, InputIterator end, Element seed) {
    Element result = seed;
    for(InputIterator current = begin; current != end; ++current) {
        result += *current;
    }
    return result;
}

int exercise_template_refactorings() {
    TemplateBox<int> box(42);
    box.assign_payload(bound_value(box.read_payload(), 0, 100));

    FieldMetadata<int> threshold_metadata("threshold", 10, true);

    StaticBuffer<int, 4> buffer;
    buffer.push(box.read_payload());
    buffer.push(threshold_metadata.default_candidate());

    int readings[3] = {buffer.first_or_default(0), 5, 7};
    if(threshold_metadata.is_mandatory()) {
        readings[1] += 1;
    }
    return accumulate_values(readings, readings + 3, 0);
}
