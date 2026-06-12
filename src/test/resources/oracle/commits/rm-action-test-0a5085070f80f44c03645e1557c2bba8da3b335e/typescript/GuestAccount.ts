export class GuestAccount {
    protected username: string;

    constructor(username: string) {
        this.username = username;
    }

    displayName(): string {
        return this.username.trim().toUpperCase();
    }

    canBrowseCatalog(): boolean {
        return this.username.length > 0;
    }
}
