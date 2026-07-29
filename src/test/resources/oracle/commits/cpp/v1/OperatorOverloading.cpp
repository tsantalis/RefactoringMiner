#include <iostream>

class Point {
private:
    int x, y;

public:
    // Default constructor
    Point(int x = 0, int y = 0) : x(x), y(y) {}

    // 1. BINARY ARITHMETIC OPERATOR (+)
    // Implemented as a member function. Doesn't modify the operands; returns a new value.
    Point operator+(const Point& other) const {
        return Point(this->x + other.x, this->y + other.y);
    }

    // 2. ASSIGNMENT OPERATOR (+=)
    // Modifies the current object, always return *this by reference.
    Point& operator+=(const Point& other) {
        this->x += other.x;
        this->y += other.y;
        return *this;
    }

    // 3. COMPARISON OPERATOR (==)
    // Evaluates a conditional check. Returns a boolean.
    bool operator==(const Point& other) const {
        return (this->x == other.x && this->y == other.y);
    }

    // 4. STREAM INSERTION OPERATOR (<<)
    // Must be a non-member friend because std::ostream is the left-hand operand.
    friend std::ostream& operator<<(std::ostream& os, const Point& p) {
        os << "(" << p.x << ", " << p.y << ")";
        return os; // Returns stream reference to allow chaining
    }
};

int main() {
    Point p1(3, 4);
    Point p2(1, 2);

    Point p3 = p1 + p2;   // Uses operator+
    p1 += p2;            // Uses operator+=

    std::cout << "p3: " << p3 << "\n";
    std::cout << "p1: " << p1 << "\n";
    std::cout << "Are they equal? " << std::boolalpha << (p1 == p3) << "\n";

    return 0;
}
