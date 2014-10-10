package bad.robot.refactoring.chapter1;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CustomerTest {

    private static final Movie THE_HULK = new Movie("The Hulk", Movie.CHILDREN);
    private static final Movie IRON_MAN = new Movie("Iron Man 4", Movie.NEW_RELEASE);
    private static final Movie SPIDER_MAN = new Movie("Spiderman", Movie.REGULAR);

    private final Customer customer = new Customer("fred");

    @Test
    public void basicChildrenRental() {
        customer.addRental(new Rental(THE_HULK, 2));
        assertThat(customer.statement(), is("Rental record for fred\n\tThe Hulk\t1.5\nAmount owed is 1.5\nYou earned 1 frequent renter points"));
    }

    @Test
    public void shouldDiscountChildrensRentals() {
        customer.addRental(new Rental(THE_HULK, 4));
        assertThat(customer.statement(), is("Rental record for fred\n\tThe Hulk\t3.0\nAmount owed is 3.0\nYou earned 1 frequent renter points"));
    }

    @Test
    public void basicNewReleaseRental() {
        customer.addRental(new Rental(IRON_MAN, 1));
        assertThat(customer.statement(), is("Rental record for fred\n\tIron Man 4\t3.0\nAmount owed is 3.0\nYou earned 1 frequent renter points"));
    }

    @Test
    public void shouldNotDiscountNewReleaseRentalsButBonusFrequentRenterPoints() {
        customer.addRental(new Rental(IRON_MAN, 4));
        assertThat(customer.statement(), is("Rental record for fred\n\tIron Man 4\t12.0\nAmount owed is 12.0\nYou earned 2 frequent renter points"));
    }

    @Test
      public void basicRegularRental() {
          customer.addRental(new Rental(SPIDER_MAN, 2));
          assertThat(customer.statement(), is("Rental record for fred\n\tSpiderman\t2.0\nAmount owed is 2.0\nYou earned 1 frequent renter points"));
      }

      @Test
      public void shouldDiscountRegularRental() {
          customer.addRental(new Rental(SPIDER_MAN, 4));
          assertThat(customer.statement(), is("Rental record for fred\n\tSpiderman\t5.0\nAmount owed is 5.0\nYou earned 1 frequent renter points"));
      }

    @Test
    public void shouldSumVariousRentals() {
        customer.addRental(new Rental(THE_HULK, 2));
        customer.addRental(new Rental(SPIDER_MAN, 1));
        customer.addRental(new Rental(IRON_MAN, 3));
        assertThat(customer.statement(), is("Rental record for fred\n\tThe Hulk\t1.5\n\tSpiderman\t2.0\n\tIron Man 4\t9.0\nAmount owed is 12.5\nYou earned 4 frequent renter points"));

    }

}
