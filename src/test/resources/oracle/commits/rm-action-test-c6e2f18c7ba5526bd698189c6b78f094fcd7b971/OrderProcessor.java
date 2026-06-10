public class OrderProcessor {
    private final ReceiptFormatter formatter = new ReceiptFormatter();

    public String buildReceipt(String buyerName, int unitPrice, int quantity, int discount) {
        int total = calculateTotal(unitPrice, quantity, discount);
        int subtotal = unitPrice * quantity;
        int discountedSubtotal = subtotal - discount;
        int tax = (int) (discountedSubtotal * new PricingPolicy().taxRate);

        String header = formatter.createReceiptHeader(buyerName);
        double dollars = total / 100.0;
        String status = describePaymentStatus(total, discount);

        return header + "\n"
                + "Subtotal: " + subtotal + "\n"
                + "Discount: " + discount + "\n"
                + "Tax: " + tax + "\n"
                + "Total: $" + dollars + "\n"
                + status;
    }

    private int calculateTotal(int unitPrice, int quantity, int discount) {
        int subtotal = unitPrice * quantity;
        int discountedSubtotal = subtotal - discount;
        int tax = (int) (discountedSubtotal * new PricingPolicy().taxRate);
        return discountedSubtotal + tax;
    }

    private String describePaymentStatus(int total, int discount) {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
}
