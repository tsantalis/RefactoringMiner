export class GuestAccount {
    constructor(username) {
        this._username = username;
    }

    displayName() {
        return this._username.trim().toUpperCase();
    }

    canBrowseCatalog() {
        return this._username.length > 0;
    }
}
