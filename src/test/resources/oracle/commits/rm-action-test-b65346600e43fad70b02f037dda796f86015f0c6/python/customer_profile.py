from address import Address


class CustomerProfile:
    def __init__(self, full_name, street, city, postal_code, loyalty_points):
        self._display_name = full_name
        self._address = Address(street, city, postal_code)
        self._loyalty_points = loyalty_points

    def mailing_label(self):
        return f"{self._display_name}\n{self._address.format()}"

    def can_receive_promotion(self, email_opted_in, bounced_email, purchase_count):
        if not email_opted_in or bounced_email or purchase_count <= 0:
            return False
        return True

    def add_purchase(self, amount_cents):
        earned_points = amount_cents // 100
        self._loyalty_points += earned_points

    def get_loyalty_points(self):
        return self._loyalty_points
