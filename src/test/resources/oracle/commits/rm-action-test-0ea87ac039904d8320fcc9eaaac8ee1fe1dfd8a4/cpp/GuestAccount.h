#pragma once

#include <string>
#include "Account.h"

class GuestAccount : public Account {
public:
    explicit GuestAccount(std::string username)
        : Account(std::move(username)) {}

    bool canBrowseCatalog() const {
        return username.length() > 0;
    }
};
