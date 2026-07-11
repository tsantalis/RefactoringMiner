#pragma once

#include <string>
#include <algorithm>
#include <cctype>
#include <cmath>

class OrderProcessor {
private:
    const double taxRate = 0.08;

public:
    std::string buildReceipt(const std::string& customerName, int unitPrice,
                             int quantity, int discount) const {
        int subtotal = unitPrice * quantity;
        int discountedSubtotal = subtotal - discount;
        int tax = static_cast<int>(std::floor(discountedSubtotal * taxRate));
        int total = discountedSubtotal + tax;

        std::string header = formatHeader(customerName);
        double dollars = centsToDollars(total);
        std::string status = formatPaymentStatus(total, discount);

        return header + "\n" +
               "Subtotal: " + std::to_string(subtotal) + "\n" +
               "Discount: " + std::to_string(discount) + "\n" +
               "Tax: " + std::to_string(tax) + "\n" +
               "Total: $" + std::to_string(dollars) + "\n" +
               status;
    }

private:
    std::string formatHeader(const std::string& customerName) const {
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

    double centsToDollars(int cents) const {
        return cents / 100.0;
    }

    std::string formatPaymentStatus(int total, int discount) const {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
};
