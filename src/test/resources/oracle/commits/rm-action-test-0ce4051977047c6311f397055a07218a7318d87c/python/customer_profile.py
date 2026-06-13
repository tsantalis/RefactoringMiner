class CustomerProfile:
    def __init__(self, full_name, street, city, postal_code, loyalty_points):
        self._full_name = full_name
        self._street = street
        self._city = city
        self._postal_code = postal_code
        self.loyalty_points = loyalty_points

    def mailing_label(self):
        return f"{self._full_name}\n{self._street}\n{self._city} {self._postal_code}"

    def can_receive_promotion(self, opted_in, bounced_email, purchase_count):
        if opted_in and not bounced_email and purchase_count > 0:
            return True
        return False

    def add_purchase(self, amount_cents):
        points = amount_cents // 100
        self.loyalty_points += points
