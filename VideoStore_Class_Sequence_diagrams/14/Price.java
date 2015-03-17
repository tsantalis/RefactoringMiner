public abstract class Price {
    public abstract int getPriceCode();

    public abstract double getCharge(int _daysRented);

    public int getFrequentRenterPoints(int _daysRented) {
        return 1;
    }
}
