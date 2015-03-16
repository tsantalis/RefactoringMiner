public abstract class Price {
    public abstract int getPriceCode();

    public abstract double getCharge(int _daysRented);

    public int getFrequentRenterPoints(int _daysRented) {
        //add bonus for a two day new release rental
        if((getPriceCode() == Movie.NEW_RELEASE) && _daysRented > 1)
              return 2;
        else
              return 1;
    }
}
