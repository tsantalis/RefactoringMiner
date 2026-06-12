import { PricingPolicy } from "./PricingPolicy";
import { ReceiptFormatter } from "./ReceiptFormatter";

export class OrderProcessor {
    private readonly formatter = new ReceiptFormatter();

    buildReceipt(buyerName: string, unitPrice: number, quantity: number, discount: number): string {
        const total = this.calculateTotal(unitPrice, quantity, discount);
        const subtotal = unitPrice * quantity;
        const discountedSubtotal = subtotal - discount;
        const tax = Math.floor(discountedSubtotal * new PricingPolicy().taxRate);

        const header = this.formatter.createReceiptHeader(buyerName);
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

    private calculateTotal(unitPrice: number, quantity: number, discount: number): number {
        const subtotal = unitPrice * quantity;
        const discountedSubtotal = subtotal - discount;
        const tax = Math.floor(discountedSubtotal * new PricingPolicy().taxRate);
        return discountedSubtotal + tax;
    }

    private describePaymentStatus(total: number, discount: number): string {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
}
