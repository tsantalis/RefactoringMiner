#include <iostream>
#include <string>

class Player {
private:
    std::string name;
    int health;

public:
    // 1. Default Constructor
    Player() : name{"Unknown"}, health{100} {
        std::cout << "Default constructor called for: " << name << "\n";
    }

    // 2. Parameterized Constructor
    Player(std::string name_val, int health_val) 
        : name{name_val}, health{health_val} {
        std::cout << "Parameterized constructor called for: " << name << "\n";
    }

    // 3. Destructor
    ~Player() {
        std::cout << "Destructor called for: " << name << "\n";
    }

    // Public method to print details
    void display() const {
        std::cout << "Player: " << name << " | Health: " << health << "\n";
    }
};

int main() {
    // 1. Default constructor
    Player player1; 
    
    // 2. Parameterized constructor (Direct initialization)
    Player player2("Bob", 100); 
    
    // 3. Uniform/Brace initialization (Modern C++11 approach, prevents most parsing bugs)
    Player player3{"Alice", 150}; 
    
    #include <memory>
    
    // Instantiates an object managed by a std::unique_ptr
    auto player4 = std::make_unique<Player>("Charlie Brown", 200); 
    
    // Instantiates an object managed by a std::shared_ptr
    auto player5 = std::make_shared<Player>("David", 250);
    
    // Instantiation
    Player* player7 = new Player("Adam", 300); 
    
    // Manual cleanup required
    delete player7;
}