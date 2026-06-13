import { Account } from "./Account.js";

export class GuestAccount extends Account {
    constructor(username) {
        super(username);
    }

    canBrowseCatalog() {
        return this._username.length > 0;
    }
}
