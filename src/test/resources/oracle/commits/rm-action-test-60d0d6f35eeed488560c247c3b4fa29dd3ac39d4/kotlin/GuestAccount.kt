class GuestAccount(username: String) : Account(username) {
    fun canBrowseCatalog(): Boolean {
        return username.isNotEmpty()
    }
}
