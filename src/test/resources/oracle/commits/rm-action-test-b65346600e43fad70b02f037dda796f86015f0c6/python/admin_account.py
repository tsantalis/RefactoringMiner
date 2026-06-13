from account import Account


class AdminAccount(Account):
    def __init__(self, username):
        super().__init__(username)
        self._access_level = "admin"

    def can_manage_users(self):
        return self._access_level == "admin"
