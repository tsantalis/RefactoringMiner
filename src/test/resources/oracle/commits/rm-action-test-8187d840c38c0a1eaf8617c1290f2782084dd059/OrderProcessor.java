public class OrderProcessor {
    private final double taxRate = 0.08;

    public String buildReceipt(String customerName, int unitPrice, int quantity, int discount) {
        int subtotal = unitPrice * quantity;
        int discountedSubtotal = subtotal - discount;
        int tax = (int) (discountedSubtotal * taxRate);
        int total = discountedSubtotal + tax;

        String header = formatHeader(customerName);
        double dollars = centsToDollars(total);
        String status = formatPaymentStatus(total, discount);

        return header + "\n"
                + "Subtotal: " + subtotal + "\n"
                + "Discount: " + discount + "\n"
                + "Tax: " + tax + "\n"
                + "Total: $" + dollars + "\n"
                + status;
    }

    private String formatHeader(String customerName) {
        return "Receipt for " + customerName.trim().toUpperCase();
    }

    private double centsToDollars(int cents) {
        return cents / 100.0;
    }

    private String formatPaymentStatus(int total, int discount) {
        if (total > 0 && discount > 0) {
            return "Discount applied";
        }
        if (total > 0) {
            return "Payment required";
        }
        return "No payment required";
    }
}
