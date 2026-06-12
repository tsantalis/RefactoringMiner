import { Account } from "./Account";

export class GuestAccount extends Account {
    constructor(username: string) {
        super(username);
    }

    canBrowseCatalog(): boolean {
        return this.username.length > 0;
    }
}
