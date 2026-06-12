export class CustomerProfile {
    private fullName: string;
    private street: string;
    private city: string;
    private postalCode: string;
    public loyaltyPoints: number;

    constructor(fullName: string, street: string, city: string, postalCode: string, loyaltyPoints: number) {
        this.fullName = fullName;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.loyaltyPoints = loyaltyPoints;
    }

    mailingLabel(): string {
        return `${this.fullName}\n${this.street}\n${this.city} ${this.postalCode}`;
    }

    canReceivePromotion(optedIn: boolean, bouncedEmail: boolean, purchaseCount: number): boolean {
        if (optedIn && !bouncedEmail && purchaseCount > 0) {
            return true;
        }
        return false;
    }

    addPurchase(amountCents: number): void {
        const points = Math.floor(amountCents / 100);
        this.loyaltyPoints += points;
    }
}
