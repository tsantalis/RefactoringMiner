import { Address } from "./Address.js";

export class CustomerProfile {
    #loyaltyPoints;

    constructor(fullName, street, city, postalCode, loyaltyPoints) {
        this._displayName = fullName;
        this._address = new Address(street, city, postalCode);
        this.#loyaltyPoints = loyaltyPoints;
    }

    mailingLabel() {
        return `${this._displayName}\n${this._address.format()}`;
    }

    canReceivePromotion(emailOptedIn, bouncedEmail, purchaseCount) {
        if (!emailOptedIn || bouncedEmail || purchaseCount <= 0) {
            return false;
        }
        return true;
    }

    addPurchase(amountCents) {
        const earnedPoints = Math.floor(amountCents / 100);
        this.#loyaltyPoints += earnedPoints;
    }

    getLoyaltyPoints() {
        return this.#loyaltyPoints;
    }
}
