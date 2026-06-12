export class Address {
    private street: string;
    private city: string;
    private postalCode: string;

    constructor(street: string, city: string, postalCode: string) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
    }

    format(): string {
        return `${this.street}\n${this.city} ${this.postalCode}`;
    }
}
