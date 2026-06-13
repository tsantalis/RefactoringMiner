class OrderProcessor {
    private val taxRate = 0.08

    fun buildReceipt(customerName: String, unitPrice: Int, quantity: Int, discount: Int): String {
        val subtotal = unitPrice * quantity
        val discountedSubtotal = subtotal - discount
        val tax = (discountedSubtotal * taxRate).toInt()
        val total = discountedSubtotal + tax

        val header = formatHeader(customerName)
        val dollars = centsToDollars(total)
        val status = formatPaymentStatus(total, discount)

        return header + "\n" +
                "Subtotal: " + subtotal + "\n" +
                "Discount: " + discount + "\n" +
                "Tax: " + tax + "\n" +
                "Total: $" + dollars + "\n" +
                status
    }

    private fun formatHeader(customerName: String): String {
        return "Receipt for " + customerName.trim().uppercase()
    }

    private fun centsToDollars(cents: Int): Double {
        return cents / 100.0
    }

    private fun formatPaymentStatus(total: Int, discount: Int): String {
        if (total > 0 && discount > 0) {
            return "Discount applied"
        }
        if (total > 0) {
            return "Payment required"
        }
        return "No payment required"
    }
}
