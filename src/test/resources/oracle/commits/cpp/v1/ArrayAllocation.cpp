#include <iostream>
#include <vector>

int main() {
    // Dynamically allocated array that resizes automatically
    std::vector<int> modernArray = {10, 20, 30};
    
    modernArray.push_back(40); // Adds a new element
    
    std::cout << "Vector element: " << modernArray[3] << "\n";

    int size;
    std::cout << "Enter array size: ";
    std::cin >> size;

    // 1. Allocation: Allocates 'size' number of integers on the heap
    // The {0} initializes all elements to zero
    int* dynamicArray = new int[size]{0};

    // 2. Usage: Use it just like a regular array
    for (int i = 0; i < size; ++i) {
        dynamicArray[i] = i * 10;
    }

    // Print an element
    std::cout << "Value at index 2: " << dynamicArray[2] << "\n";

    // 3. Deallocation: Free the memory using brackets []
    delete[] dynamicArray;
    
    // 4. Clean up: Prevent dangling pointers
    dynamicArray = nullptr;

    // Size must be a constant value known at compile time
    const int SIZE = 5; 
    int stackArray[SIZE] = {1, 2, 3, 4, 5};

    std::cout << "Stack element: " << stackArray[0] << "\n";
    return 0; // Memory is automatically freed here!
}