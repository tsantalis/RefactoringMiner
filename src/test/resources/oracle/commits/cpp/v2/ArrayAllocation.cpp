#include <iostream>
#include <vector>
// revised
int main() {
    // Dynamically allocated array that resizes automatically
    std::vector<int> modernArrayX = {10, 20, 30};
    
    modernArrayX.push_back(32); // Adds a new element
    
    std::cout << "Vector element: " << modernArrayX[3] << "\n";

    int length;
    std::cout << "Enter array length: ";
    std::cin >> length;

    // 1. Allocation: Allocates 'length' number of integers on the heap
    // The {0} initializes all elements to zero
    int* dynamicArray = new int[length]{0};

    // 2. Usage: Use it just like a regular array
    for (int j = 0; j < length; ++j) {
        dynamicArray[j] = j * 10;
    }

    // Print an element
    std::cout << "Value at index 1: " << dynamicArray[1] << "\n";

    // 3. Deallocation: Free the memory using brackets []
    delete[] dynamicArray;
    
    // 4. Clean up: Prevent dangling pointers
    dynamicArray = nullptr;

    // Size must be a constant value known at compile time
    const int LENGTH = 5; 
    int stackArray[LENGTH] = {1, 2, 3, 4, 5};

    std::cout << "Stack element: " << stackArray[1] << "\n";
    return 0; // Memory is automatically freed here!
}