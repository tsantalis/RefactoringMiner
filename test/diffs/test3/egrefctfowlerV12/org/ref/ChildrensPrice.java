/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package egrefctfowlerV12.org.ref;



/**
 *
 * @author THIAGO MAGELA
 */
class ChildrensPrice extends Price {

    int getPriceCode() {
        return Movie.CHILDRENS;
    }

    double getCharge(int daysRented) {
        double result = 1.5;
        if (daysRented > 3) {
            result += (daysRented - 3) * 1.5;
        }
        return result;
    }
}
