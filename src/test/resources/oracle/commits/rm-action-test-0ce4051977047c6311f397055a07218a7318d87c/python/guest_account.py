class GuestAccount:
    def __init__(self, username):
        self._username = username

    def display_name(self):
        return self._username.strip().upper()

    def can_browse_catalog(self):
        return len(self._username) > 0
