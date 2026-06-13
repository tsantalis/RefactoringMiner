export class ReceiptFormatter {
    createReceiptHeader(customerName) {
        return "Receipt for " + customerName.trim().toUpperCase();
    }
}
