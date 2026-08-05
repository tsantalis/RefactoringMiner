#include <iostream>

// 1. Forward Declaration (Function Prototype)
int add(int a, int b); 

int main() {
    // The compiler allows this call because it knows 'add' exists
    std::cout << add(8, 3) << std::endl; 
    return 0;
}

// 2. Function Definition (Implementation)
int add(int a, int b) {
    return a + b;
}
