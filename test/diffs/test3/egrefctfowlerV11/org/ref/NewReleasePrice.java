/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package egrefctfowlerV11.org.ref;

/**
 *
 * @author THIAGO MAGELA
 */
class NewReleasePrice extends Price {
int getPriceCode() {
return Movie.NEW_RELEASE;
}

double getCharge(int daysRented){
return daysRented * 3;
}
}
