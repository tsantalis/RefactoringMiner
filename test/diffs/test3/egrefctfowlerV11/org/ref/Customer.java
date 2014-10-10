/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package egrefctfowlerV11.org.ref;


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
        Enumeration rentals = _rentals.elements();
        String result = "Rental Record for " + getName() + "\n";
        while (rentals.hasMoreElements()) {
            Rental each = (Rental) rentals.nextElement();
//show figures for this rental
            result += "\t" + each.getMovie().getName() + "\t"
                    + String.valueOf(each.getCharge()) + "\n";
        }
//add footer lines
        result += "Amount owed is "
                + String.valueOf(getTotalCharge()) + "\n";
        result += "You earned "
                + String.valueOf(getTotalFrequentRenterPoints())
                + " frequent renter points";
        return result;
    }

    private int getTotalFrequentRenterPoints() {
        int result = 0;
        Enumeration rentals = _rentals.elements();
        while (rentals.hasMoreElements()) {
            Rental each = (Rental) rentals.nextElement();
            result += each.getFrequentRenterPoints();
        }
        return result;
    }

    private double getTotalCharge() {
        double result = 0;
        Enumeration rentals = _rentals.elements();
        while (rentals.hasMoreElements()) {
            Rental each = (Rental) rentals.nextElement();
            result += each.getCharge();
        }
        return result;
    }

    private double amountFor(Rental aRental) {
        return aRental.getCharge();
    }
}