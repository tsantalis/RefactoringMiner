public class ChildrensPrice extends Price {
    public int getPriceCode() {
        return Movie.CHILDRENS;
    }

    public double getCharge(int _daysRented) {
        double result = 1.5;
        if (_daysRented > 3)
            result += (_daysRented - 3) * 1.5;
        return result;
    }
}
