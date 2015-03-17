public abstract class Price {
    public abstract int getPriceCode();

    public double getCharge(int _daysRented) {
        double result = 0;
        switch(getPriceCode()) {
            case Movie.REGULAR:
               result += 2;
               if (_daysRented > 2)
                  result += (_daysRented - 2) * 1.5;
                  break;
            case Movie.NEW_RELEASE:
                result += _daysRented * 3;
                break;
            case Movie.CHILDRENS:
                result += 1.5;
                if (_daysRented > 3)
                    result += (_daysRented - 3) * 1.5;
                break;
            }
        return result;
    }
}
