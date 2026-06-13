class Address:
    def __init__(self, street, city, postal_code):
        self._street = street
        self._city = city
        self._postal_code = postal_code

    def format(self):
        return f"{self._street}\n{self._city} {self._postal_code}"
