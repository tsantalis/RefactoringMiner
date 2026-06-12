export class Account {
    protected username: string;

    constructor(username: string) {
        this.username = username;
    }

    displayName(): string {
        return this.username.trim().toUpperCase();
    }
}
