/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package egrefctfowlerV8.org.ref;


import java.util.Enumeration;
import java.util.Vector;

/**
 *
 * @author THIAGO MAGELA
 */
class Customer {

    private String _name;
    private Vector _rentals = new Vector();

    public Customer(String name) {
        _name = name;
    }

    ;
public void addRental(Rental arg) {
        _rentals.addElement(arg);
    }

    public String getName() {
        return _name;
    }

    ;



public String statement() {
double totalAmount = 0;
int frequentRenterPoints = 0;
Enumeration rentals = _rentals.elements();
String result = "Rental Record for " + getName() + "\n";
while (rentals.hasMoreElements()) {
Rental each = (Rental) rentals.nextElement();
frequentRenterPoints += each.getFrequentRenterPoints();
//show figures for this rental
result += "\t" + each.getMovie().getTitle()+ "\t" +
String.valueOf(each.getCharge()) + "\n";
totalAmount += each.getCharge();
}
//add footer lines
result += "Amount owed is " + String.valueOf(totalAmount) +
"\n";
result += "You earned " + String.valueOf(frequentRenterPoints)
+
" frequent renter points";
return result;
}

    private double amountFor(Rental aRental) {
        return aRental.getCharge();
    }
}