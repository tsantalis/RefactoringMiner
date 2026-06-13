class OrderProcessor {
    private val formatter = ReceiptFormatter()

    fun buildReceipt(buyerName: String, unitPrice: Int, quantity: Int, discount: Int): String {
        val total = calculateTotal(unitPrice, quantity, discount)
        val subtotal = unitPrice * quantity
        val discountedSubtotal = subtotal - discount
        val tax = (discountedSubtotal * PricingPolicy().taxRate).toInt()

        val header = formatter.createReceiptHeader(buyerName)
        val dollars = total / 100.0
        val status = describePaymentStatus(total, discount)

        return header + "\n" +
                "Subtotal: " + subtotal + "\n" +
                "Discount: " + discount + "\n" +
                "Tax: " + tax + "\n" +
                "Total: $" + dollars + "\n" +
                status
    }

    private fun calculateTotal(unitPrice: Int, quantity: Int, discount: Int): Int {
        val subtotal = unitPrice * quantity
        val discountedSubtotal = subtotal - discount
        val tax = (discountedSubtotal * PricingPolicy().taxRate).toInt()
        return discountedSubtotal + tax
    }

    private fun describePaymentStatus(total: Int, discount: Int): String {
        if (total > 0 && discount > 0) {
            return "Discount applied"
        }
        if (total > 0) {
            return "Payment required"
        }
        return "No payment required"
    }
}
