public class ReceiptFormatter {
    public String createReceiptHeader(String customerName) {
        return "Receipt for " + customerName.trim().toUpperCase();
    }
}
