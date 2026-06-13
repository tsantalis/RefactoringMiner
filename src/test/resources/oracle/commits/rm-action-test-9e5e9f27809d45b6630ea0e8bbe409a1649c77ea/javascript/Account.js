export class Account {
    constructor(username) {
        this._username = username;
    }

    displayName() {
        return this._username.trim().toUpperCase();
    }
}
