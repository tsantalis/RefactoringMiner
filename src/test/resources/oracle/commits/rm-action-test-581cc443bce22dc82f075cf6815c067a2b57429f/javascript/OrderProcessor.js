export class OrderProcessor {
    constructor() {
        this._taxRate = 0.08;
    }

    buildReceipt(customerName, unitPrice, quantity, discount) {
        const subtotal = unitPrice * quantity;
        const discountedSubtotal = subtotal - discount;
        const tax = Math.floor(discountedSubtotal * this._taxRate);
        const total = discountedSubtotal + tax;

        const header = this.formatHeader(customerName);
        const dollars = this.centsToDollars(total);
        const status = this.formatPaymentStatus(total, discount);

        return (
            `${header}\n` +
            `Subtotal: ${subtotal}\n` +
            `Discount: ${discount}\n` +
            `Tax: ${tax}\n` +
            `Total: $${dollars}\n` +
            `${status}`
        );
    }

    formatHeader(customerName) {
        return "Receipt for " + customerName.trim().toUpperCase();
    }

    centsToDollars(cents) {
        return cents / 100.0;
    }

    formatPaymentStatus(total, discount) {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
}
