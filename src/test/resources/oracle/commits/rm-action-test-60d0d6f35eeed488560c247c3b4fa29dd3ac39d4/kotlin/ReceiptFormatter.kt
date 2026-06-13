class ReceiptFormatter {
    fun createReceiptHeader(customerName: String): String {
        return "Receipt for " + customerName.trim().uppercase()
    }
}
