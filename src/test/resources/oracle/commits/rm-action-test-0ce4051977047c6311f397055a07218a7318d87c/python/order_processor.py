class OrderProcessor:
    def __init__(self):
        self._tax_rate = 0.08

    def build_receipt(self, customer_name, unit_price, quantity, discount):
        subtotal = unit_price * quantity
        discounted_subtotal = subtotal - discount
        tax = int(discounted_subtotal * self._tax_rate)
        total = discounted_subtotal + tax

        header = self._format_header(customer_name)
        dollars = self._cents_to_dollars(total)
        status = self._format_payment_status(total, discount)

        return (
            f"{header}\n"
            f"Subtotal: {subtotal}\n"
            f"Discount: {discount}\n"
            f"Tax: {tax}\n"
            f"Total: ${dollars}\n"
            f"{status}"
        )

    def _format_header(self, customer_name):
        return "Receipt for " + customer_name.strip().upper()

    def _cents_to_dollars(self, cents):
        return cents / 100.0

    def _format_payment_status(self, total, discount):
        if total > 0 and discount > 0:
            return "Discount applied"
        if total > 0:
            return "Payment required"
        return "No payment required"
