class AdminAccount(username: String) : Account(username) {
    private val accessLevel = "admin"

    fun canManageUsers(): Boolean {
        return accessLevel == "admin"
    }
}
