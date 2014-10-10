/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package egrefctfowlerV11.org.ref;

/**
 *
 * @author THIAGO MAGELA
 */
abstract class Price {

    abstract int getPriceCode();
   

    double getCharge(int daysRented) {
        double result = 0;
        switch (getPriceCode()) {
            case Movie.REGULAR:
                result += 2;
                if (daysRented > 2) {
                    result += (daysRented - 2) * 1.5;
                }
                break;
            case Movie.NEW_RELEASE:
                result += daysRented * 3;
                break;
            case Movie.CHILDRENS:
                result += 1.5;
                if (daysRented > 3) {
                    result += (daysRented - 3) * 1.5;
                }
                break;
        }
        return result;
    }
}
