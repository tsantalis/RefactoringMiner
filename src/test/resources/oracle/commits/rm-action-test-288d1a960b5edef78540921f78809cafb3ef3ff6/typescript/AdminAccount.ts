import { Account } from "./Account";

export class AdminAccount extends Account {
    private accessLevel = "admin";

    constructor(username: string) {
        super(username);
    }

    canManageUsers(): boolean {
        return this.accessLevel === "admin";
    }
}
