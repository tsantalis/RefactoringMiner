import { Address } from "./Address";

export class CustomerProfile {
    private displayName: string;
    private address: Address;
    private loyaltyPoints: number;

    constructor(fullName: string, street: string, city: string, postalCode: string, loyaltyPoints: number) {
        this.displayName = fullName;
        this.address = new Address(street, city, postalCode);
        this.loyaltyPoints = loyaltyPoints;
    }

    mailingLabel(): string {
        return `${this.displayName}\n${this.address.format()}`;
    }

    canReceivePromotion(emailOptedIn: boolean, bouncedEmail: boolean, purchaseCount: number): boolean {
        if (!emailOptedIn || bouncedEmail || purchaseCount <= 0) {
            return false;
        }
        return true;
    }

    addPurchase(amountCents: number): void {
        const earnedPoints = Math.floor(amountCents / 100);
        this.loyaltyPoints += earnedPoints;
    }

    getLoyaltyPoints(): number {
        return this.loyaltyPoints;
    }
}
