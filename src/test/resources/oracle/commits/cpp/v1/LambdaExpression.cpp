#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> numbers = {1, 2, 3, 4, 5};
    int multiplier = 10;

    // Lambda syntax: [capture](parameters) -> return_type { body }
    std::for_each(numbers.begin(), numbers.end(), [multiplier](int n) {
        std::cout << n * multiplier << " ";
    });
    
    int speed = 80;
    std::string status;

    if (speed > 65) {
        status = "Speeding";
    } else {
        status = "Normal";
    }

    return 0;
}
