export class CustomerProfile {
    constructor(fullName, street, city, postalCode, loyaltyPoints) {
        this._fullName = fullName;
        this._street = street;
        this._city = city;
        this._postalCode = postalCode;
        this.loyaltyPoints = loyaltyPoints;
    }

    mailingLabel() {
        return `${this._fullName}\n${this._street}\n${this._city} ${this._postalCode}`;
    }

    canReceivePromotion(optedIn, bouncedEmail, purchaseCount) {
        if (optedIn && !bouncedEmail && purchaseCount > 0) {
            return true;
        }
        return false;
    }

    addPurchase(amountCents) {
        const points = Math.floor(amountCents / 100);
        this.loyaltyPoints += points;
    }
}
