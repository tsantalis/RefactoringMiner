
public class Rental {
	/**
	 * @uml.property  name="_movie"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Movie _movie;
	/**
	 * @uml.property  name="_daysRented"
	 */
	private int _daysRented;
	
	public Rental(Movie movie, int daysRented) {
		_movie = movie;
		_daysRented = daysRented;
	}
	
	public int getDaysRented() {
		return _daysRented;
	}
	
	public Movie getMovie() {
		return _movie;
	}

    public double getCharge() {
        return getCharge(getMovie(), getDaysRented());
    }

    public double getCharge(Movie aMovie, int _daysRented) {
	  double result = 0;
        switch(aMovie.getPriceCode()) {
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

    public int getFrequentRenterPoints() {
        return getFrequentRenterPoints(getMovie(), getDaysRented());
    }

    public int getFrequentRenterPoints(Movie aMovie, int _daysRented) {
        //add bonus for a two day new release rental
        if((aMovie.getPriceCode() == Movie.NEW_RELEASE) && _daysRented > 1)
              return 2;
        else
              return 1;
    }
}
