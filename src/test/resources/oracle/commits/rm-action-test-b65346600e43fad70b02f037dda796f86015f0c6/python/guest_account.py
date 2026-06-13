from account import Account


class GuestAccount(Account):
    def __init__(self, username):
        super().__init__(username)

    def can_browse_catalog(self):
        return len(self._username) > 0
