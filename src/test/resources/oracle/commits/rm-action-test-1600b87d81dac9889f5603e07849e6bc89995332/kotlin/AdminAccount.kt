class AdminAccount(private val username: String) {
    private val accessLevel = "admin"

    fun displayName(): String {
        return username.trim().uppercase()
    }

    fun canManageUsers(): Boolean {
        return accessLevel == "admin"
    }
}
