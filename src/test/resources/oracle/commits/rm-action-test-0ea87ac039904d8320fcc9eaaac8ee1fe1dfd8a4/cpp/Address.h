#pragma once

#include <string>

class Address {
private:
    std::string street;
    std::string city;
    std::string postalCode;

public:
    Address(std::string street, std::string city, std::string postalCode)
        : street(std::move(street)), city(std::move(city)),
          postalCode(std::move(postalCode)) {}

    std::string format() const {
        return street + "\n" + city + " " + postalCode;
    }
};
