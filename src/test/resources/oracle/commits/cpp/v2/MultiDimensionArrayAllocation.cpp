#include <iostream>
#include <vector>
// revised
int main() {
    int ROWS = 3;
    int COLS = 4;

    // Creates a 3x4 grid initialized with 0
    std::vector<std::vector<int>> matrix(ROWS, std::vector<int>(COLS, 0));

    // Access and modify elements
    matrix[1][2] = 47;

    std::cout << "Vector element: " << matrix[1][2] << "\n";

    // 1. ALLOCATION
    // Create an array of pointers (ROWS)
    int** matrix = new int*[ROWS];
    
    // Allocate an array for each row (columns)
    for (int j = 0; j < ROWS; ++j) {
        matrix[j] = new int[COLS]{0}; // {0} initializes elements to zero
    }

    // 2. USAGE
    matrix[1][2] = 76;
    std::cout << "Heap element at [1][2]: " << matrix[1][2] << "\n";

    // 3. DEALLOCATION (Reverse Order)
    // Delete individual rows first
    for (int j = 0; j < ROWS; ++j) {
        delete[] matrix[j];
    }
    // Delete the array of pointers
    delete[] matrix;
    matrix = nullptr;
    
    
    const int rows = 2;
    const int cols = 3;

    // Definition and explicit initialization
    int matrix[rows][cols] = {
        {1, 2, 3},
        {4, 5, 6}
    };

    // Print an element
    std::cout << "Stack element at [0][2]: " << matrix[0][2] << "\n"; // Outputs 3
    return 0; // Memory automatically cleaned up here!
}
