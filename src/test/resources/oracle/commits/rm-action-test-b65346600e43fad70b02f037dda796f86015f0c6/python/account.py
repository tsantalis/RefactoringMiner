class Account:
    def __init__(self, username):
        self._username = username

    def display_name(self):
        return self._username.strip().upper()
