from pricing_policy import PricingPolicy
from receipt_formatter import ReceiptFormatter


class OrderProcessor:
    def __init__(self):
        self._formatter = ReceiptFormatter()

    def build_receipt(self, buyer_name, unit_price, quantity, discount):
        total = self._calculate_total(unit_price, quantity, discount)
        subtotal = unit_price * quantity
        discounted_subtotal = subtotal - discount
        tax = int(discounted_subtotal * PricingPolicy().tax_rate)

        header = self._formatter.create_receipt_header(buyer_name)
        dollars = total / 100.0
        status = self._describe_payment_status(total, discount)

        return (
            f"{header}\n"
            f"Subtotal: {subtotal}\n"
            f"Discount: {discount}\n"
            f"Tax: {tax}\n"
            f"Total: ${dollars}\n"
            f"{status}"
        )

    def _calculate_total(self, unit_price, quantity, discount):
        subtotal = unit_price * quantity
        discounted_subtotal = subtotal - discount
        tax = int(discounted_subtotal * PricingPolicy().tax_rate)
        return discounted_subtotal + tax

    def _describe_payment_status(self, total, discount):
        if total > 0 and discount > 0:
            return "Discount applied"
        if total > 0:
            return "Payment required"
        return "No payment required"
