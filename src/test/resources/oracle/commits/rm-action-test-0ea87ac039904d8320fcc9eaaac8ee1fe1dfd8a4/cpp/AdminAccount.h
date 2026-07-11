#pragma once

#include <string>
#include "Account.h"

class AdminAccount : public Account {
private:
    std::string accessLevel = "admin";

public:
    explicit AdminAccount(std::string username)
        : Account(std::move(username)) {}

    bool canManageUsers() const {
        return accessLevel == "admin";
    }
};
