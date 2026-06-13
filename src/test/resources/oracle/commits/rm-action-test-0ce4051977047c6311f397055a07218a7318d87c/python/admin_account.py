class AdminAccount:
    def __init__(self, username):
        self._username = username
        self._access_level = "admin"

    def display_name(self):
        return self._username.strip().upper()

    def can_manage_users(self):
        return self._access_level == "admin"
