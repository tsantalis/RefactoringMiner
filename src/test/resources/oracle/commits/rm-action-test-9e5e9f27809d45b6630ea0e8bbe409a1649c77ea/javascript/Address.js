export class Address {
    constructor(street, city, postalCode) {
        this._street = street;
        this._city = city;
        this._postalCode = postalCode;
    }

    format() {
        return `${this._street}\n${this._city} ${this._postalCode}`;
    }
}
