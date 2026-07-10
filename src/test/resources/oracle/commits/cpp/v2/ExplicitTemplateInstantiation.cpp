template<typename Element>
class ValueBox {
public:
    explicit ValueBox(Element payload) : payload(payload) {
    }

    Element read_payload() const {
        return payload;
    }

private:
    Element payload;
};

template<typename Element>
Element combine_value(Element first, Element second) {
    return first + second;
}

// Explicit template instantiation for a class template.
template class ValueBox<int>;

// Explicit template instantiation for a function template.
template int combine_value<int>(int, int);

int exercise_explicit_template_instantiation() {
    ValueBox<int> box(10);
    return combine_value(box.read_payload(), 5);
}
