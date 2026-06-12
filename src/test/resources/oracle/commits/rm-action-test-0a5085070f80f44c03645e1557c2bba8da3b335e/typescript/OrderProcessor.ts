export class OrderProcessor {
    private readonly taxRate = 0.08;

    buildReceipt(customerName: string, unitPrice: number, quantity: number, discount: number): string {
        const subtotal = unitPrice * quantity;
        const discountedSubtotal = subtotal - discount;
        const tax = Math.floor(discountedSubtotal * this.taxRate);
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

    private formatHeader(customerName: string): string {
        return "Receipt for " + customerName.trim().toUpperCase();
    }

    private centsToDollars(cents: number): number {
        return cents / 100.0;
    }

    private formatPaymentStatus(total: number, discount: number): string {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
}
