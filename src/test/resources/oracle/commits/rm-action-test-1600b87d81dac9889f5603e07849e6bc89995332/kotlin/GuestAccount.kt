class GuestAccount(private val username: String) {
    fun displayName(): String {
        return username.trim().uppercase()
    }

    fun canBrowseCatalog(): Boolean {
        return username.isNotEmpty()
    }
}
