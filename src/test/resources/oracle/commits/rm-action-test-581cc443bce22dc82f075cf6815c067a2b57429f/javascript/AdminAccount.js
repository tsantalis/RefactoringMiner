export class AdminAccount {
    constructor(username) {
        this._username = username;
        this._accessLevel = "admin";
    }

    displayName() {
        return this._username.trim().toUpperCase();
    }

    canManageUsers() {
        return this._accessLevel === "admin";
    }
}
