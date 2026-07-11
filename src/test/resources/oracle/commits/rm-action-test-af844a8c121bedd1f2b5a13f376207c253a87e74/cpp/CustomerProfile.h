#pragma once

#include <string>

class CustomerProfile {
private:
    std::string fullName;
    std::string street;
    std::string city;
    std::string postalCode;

public:
    int loyaltyPoints;

    CustomerProfile(std::string fullName, std::string street, std::string city,
                    std::string postalCode, int loyaltyPoints)
        : fullName(std::move(fullName)), street(std::move(street)),
          city(std::move(city)), postalCode(std::move(postalCode)),
          loyaltyPoints(loyaltyPoints) {}

    std::string mailingLabel() const {
        return fullName + "\n" + street + "\n" + city + " " + postalCode;
    }

    bool canReceivePromotion(bool optedIn, bool bouncedEmail, int purchaseCount) const {
        if (optedIn && !bouncedEmail && purchaseCount > 0) {
            return true;
        }
        return false;
    }

    void addPurchase(int amountCents) {
        int points = amountCents / 100;
        loyaltyPoints += points;
    }
};
