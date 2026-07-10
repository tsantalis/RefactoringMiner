template<typename T>
class Box {
public:
    explicit Box(T value) : value(value) {
    }

    T get_value() const {
        return value;
    }

private:
    T value;
};

template<typename T>
T add_value(T left, T right) {
    return left + right;
}

// Explicit template instantiation for a class template.
template class Box<int>;

// Explicit template instantiation for a function template.
template int add_value<int>(int, int);

int exercise_explicit_template_instantiation() {
    Box<int> box(10);
    return add_value(box.get_value(), 5);
}
