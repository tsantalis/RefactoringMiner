#include <iostream>
#include <vector>

int main() {
    int rows = 3;
    int cols = 4;

    // Creates a 3x4 grid initialized with 0
    std::vector<std::vector<int>> matrix(rows, std::vector<int>(cols, 0));

    // Access and modify elements
    matrix[1][2] = 42;

    std::cout << "Vector element: " << matrix[1][2] << "\n";

    // 1. ALLOCATION
    // Create an array of pointers (rows)
    int** matrix = new int*[rows];
    
    // Allocate an array for each row (columns)
    for (int i = 0; i < rows; ++i) {
        matrix[i] = new int[cols]{0}; // {0} initializes elements to zero
    }

    // 2. USAGE
    matrix[1][2] = 75;
    std::cout << "Heap element at [1][2]: " << matrix[1][2] << "\n";

    // 3. DEALLOCATION (Reverse Order)
    // Delete individual rows first
    for (int i = 0; i < rows; ++i) {
        delete[] matrix[i];
    }
    // Delete the array of pointers
    delete[] matrix;
    matrix = nullptr;
    
    
    const int ROWS = 2;
    const int COLS = 3;

    // Definition and explicit initialization
    int matrix[ROWS][COLS] = {
        {1, 2, 3},
        {4, 5, 6}
    };

    // Print an element
    std::cout << "Stack element at [0][2]: " << matrix[0][2] << "\n"; // Outputs 3
    return 0; // Memory automatically cleaned up here!
}
