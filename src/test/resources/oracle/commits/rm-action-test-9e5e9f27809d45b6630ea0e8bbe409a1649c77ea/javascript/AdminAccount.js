import { Account } from "./Account.js";

export class AdminAccount extends Account {
    constructor(username) {
        super(username);
        this._accessLevel = "admin";
    }

    canManageUsers() {
        return this._accessLevel === "admin";
    }
}
