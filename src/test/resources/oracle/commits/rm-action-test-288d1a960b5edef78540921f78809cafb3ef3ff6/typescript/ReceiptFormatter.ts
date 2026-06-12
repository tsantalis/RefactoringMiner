export class ReceiptFormatter {
    createReceiptHeader(customerName: string): string {
        return "Receipt for " + customerName.trim().toUpperCase();
    }
}
