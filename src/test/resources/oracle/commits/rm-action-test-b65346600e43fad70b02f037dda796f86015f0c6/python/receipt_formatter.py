class ReceiptFormatter:
    def create_receipt_header(self, customer_name):
        return "Receipt for " + customer_name.strip().upper()
