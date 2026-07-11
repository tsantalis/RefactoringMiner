#pragma once

#include <string>
#include <algorithm>
#include <cctype>

class ReceiptFormatter {
public:
    std::string createReceiptHeader(const std::string& customerName) const {
        std::string trimmed = customerName;
        std::size_t start = trimmed.find_first_not_of(" \t\n\r\f\v");
        std::size_t end = trimmed.find_last_not_of(" \t\n\r\f\v");
        trimmed = (start == std::string::npos)
            ? ""
            : trimmed.substr(start, end - start + 1);
        std::transform(trimmed.begin(), trimmed.end(), trimmed.begin(),
                       [](unsigned char c) { return std::toupper(c); });
        return "Receipt for " + trimmed;
    }
};
