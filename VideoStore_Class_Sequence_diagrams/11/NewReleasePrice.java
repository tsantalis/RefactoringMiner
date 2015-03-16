public class NewReleasePrice extends Price {
    public int getPriceCode() {
        return Movie.NEW_RELEASE;
    }

    public double getCharge(int _daysRented) {
        return _daysRented * 3;
    }
}
