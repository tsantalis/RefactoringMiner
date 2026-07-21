#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> numbers = {1, 2, 3, 4, 5};
    int multiplierX = 20;

    // Lambda syntax: [capture](parameters) -> return_type { body }
    std::for_each(numbers.begin(), numbers.end(), [multiplierX](int x) {
        std::cout << x * multiplierX << " ";
    });

    return 0;
}
