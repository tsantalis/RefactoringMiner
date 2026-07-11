#pragma once

#include <string>
#include <algorithm>
#include <cctype>

class Account {
protected:
    std::string username;

public:
    explicit Account(std::string username)
        : username(std::move(username)) {}

    std::string displayName() const {
        std::string result = username;
        std::size_t start = result.find_first_not_of(" \t\n\r\f\v");
        std::size_t end = result.find_last_not_of(" \t\n\r\f\v");
        result = (start == std::string::npos)
            ? ""
            : result.substr(start, end - start + 1);
        std::transform(result.begin(), result.end(), result.begin(),
                       [](unsigned char c) { return std::toupper(c); });
        return result;
    }
};
