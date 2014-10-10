/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package egrefctfowlerV12.org.ref;



/**
 *
 * @author THIAGO MAGELA
 */
class RegularPrice extends Price {
int getPriceCode() {
return Movie.REGULAR;
}

double getCharge(int daysRented){
double result = 2;
if (daysRented > 2)
result += (daysRented - 2) * 1.5;
return result;
}
}
