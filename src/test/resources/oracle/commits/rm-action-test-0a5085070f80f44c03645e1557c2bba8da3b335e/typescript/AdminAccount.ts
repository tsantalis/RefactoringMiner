export class AdminAccount {
    protected username: string;
    private accessLevel = "admin";

    constructor(username: string) {
        this.username = username;
    }

    displayName(): string {
        return this.username.trim().toUpperCase();
    }

    canManageUsers(): boolean {
        return this.accessLevel === "admin";
    }
}
