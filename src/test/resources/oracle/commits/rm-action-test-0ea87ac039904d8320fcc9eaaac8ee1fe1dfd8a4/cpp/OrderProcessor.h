#pragma once

#include <string>
#include <cmath>
#include "PricingPolicy.h"
#include "ReceiptFormatter.h"

class OrderProcessor {
private:
    ReceiptFormatter formatter;

public:
    std::string buildReceipt(const std::string& buyerName, int unitPrice,
                             int quantity, int discount) const {
        int total = calculateTotal(unitPrice, quantity, discount);
        int subtotal = unitPrice * quantity;
        int discountedSubtotal = subtotal - discount;
        int tax = static_cast<int>(std::floor(discountedSubtotal * PricingPolicy().taxRate));

        std::string header = formatter.createReceiptHeader(buyerName);
        double dollars = total / 100.0;
        std::string status = describePaymentStatus(total, discount);

        return header + "\n" +
               "Subtotal: " + std::to_string(subtotal) + "\n" +
               "Discount: " + std::to_string(discount) + "\n" +
               "Tax: " + std::to_string(tax) + "\n" +
               "Total: $" + std::to_string(dollars) + "\n" +
               status;
    }

private:
    int calculateTotal(int unitPrice, int quantity, int discount) const {
        int subtotal = unitPrice * quantity;
        int discountedSubtotal = subtotal - discount;
        int tax = static_cast<int>(std::floor(discountedSubtotal * PricingPolicy().taxRate));
        return discountedSubtotal + tax;
    }

    std::string describePaymentStatus(int total, int discount) const {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
};
