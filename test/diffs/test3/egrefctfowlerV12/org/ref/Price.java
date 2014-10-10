/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package egrefctfowlerV12.org.ref;

/**
 *
 * @author THIAGO MAGELA
 */
abstract class Price {

    abstract int getPriceCode();

    abstract double getCharge(int daysRented);

    
    int getFrequentRenterPoints(int daysRented){
        return 1;
    }       
}
