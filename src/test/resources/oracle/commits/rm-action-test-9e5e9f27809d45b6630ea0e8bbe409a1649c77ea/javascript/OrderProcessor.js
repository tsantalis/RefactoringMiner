import { PricingPolicy } from "./PricingPolicy.js";
import { ReceiptFormatter } from "./ReceiptFormatter.js";

export class OrderProcessor {
    constructor() {
        this._formatter = new ReceiptFormatter();
    }

    buildReceipt(buyerName, unitPrice, quantity, discount) {
        const total = this.calculateTotal(unitPrice, quantity, discount);
        const subtotal = unitPrice * quantity;
        const discountedSubtotal = subtotal - discount;
        const tax = Math.floor(discountedSubtotal * new PricingPolicy().taxRate);

        const header = this._formatter.createReceiptHeader(buyerName);
        const dollars = total / 100.0;
        const status = this.describePaymentStatus(total, discount);

        return (
            `${header}\n` +
            `Subtotal: ${subtotal}\n` +
            `Discount: ${discount}\n` +
            `Tax: ${tax}\n` +
            `Total: $${dollars}\n` +
            `${status}`
        );
    }

    calculateTotal(unitPrice, quantity, discount) {
        const subtotal = unitPrice * quantity;
        const discountedSubtotal = subtotal - discount;
        const tax = Math.floor(discountedSubtotal * new PricingPolicy().taxRate);
        return discountedSubtotal + tax;
    }

    describePaymentStatus(total, discount) {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
}
