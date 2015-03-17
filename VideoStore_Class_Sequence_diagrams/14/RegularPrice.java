public class RegularPrice extends Price {
    public int getPriceCode() {
        return Movie.REGULAR;
    }

    public double getCharge(int _daysRented) {
        double result = 2;
        if (_daysRented > 2)
            result += (_daysRented - 2) * 1.5;
        return result;
    }
}
