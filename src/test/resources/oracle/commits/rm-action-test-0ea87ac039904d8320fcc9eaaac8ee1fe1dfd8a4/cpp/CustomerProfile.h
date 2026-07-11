#pragma once

#include <string>
#include "Address.h"

class CustomerProfile {
private:
    std::string displayName;
    Address address;
    int loyaltyPoints;

public:
    CustomerProfile(std::string fullName, std::string street, std::string city,
                    std::string postalCode, int loyaltyPoints)
        : displayName(std::move(fullName)),
          address(std::move(street), std::move(city), std::move(postalCode)),
          loyaltyPoints(loyaltyPoints) {}

    std::string mailingLabel() const {
        return displayName + "\n" + address.format();
    }

    bool canReceivePromotion(bool emailOptedIn, bool bouncedEmail, int purchaseCount) const {
        if (!emailOptedIn || bouncedEmail || purchaseCount <= 0) {
            return false;
        }
        return true;
    }

    void addPurchase(int amountCents) {
        int earnedPoints = amountCents / 100;
        loyaltyPoints += earnedPoints;
    }

    int getLoyaltyPoints() const {
        return loyaltyPoints;
    }
};
