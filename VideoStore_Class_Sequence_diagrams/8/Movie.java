
public class Movie {
	
	public static final int CHILDRENS = 2;
	public static final int REGULAR = 0;
	public static final int NEW_RELEASE = 1;
	
	private String _title;
	private Price _price;
	
	public Movie(String title, int priceCode) {
		_title = title;
		setPriceCode(priceCode);
	}
	
	public int getPriceCode() {
		return _price.getPriceCode();
	}
	
	public void setPriceCode(int arg) {
		switch(arg) {
            case REGULAR:
                _price = new RegularPrice();
                  break;
            case NEW_RELEASE:
                _price = new NewReleasePrice();
                break;
            case CHILDRENS:
                _price = new ChildrensPrice();
                break;
            }
	}
	
	public String getTitle() {
		return _title;
	}

    public double getCharge(int _daysRented) {
        double result = 0;
        switch(_price.getPriceCode()) {
            case REGULAR:
               result += 2;
               if (_daysRented > 2)
                  result += (_daysRented - 2) * 1.5;
                  break;
            case NEW_RELEASE:
                result += _daysRented * 3;
                break;
            case CHILDRENS:
                result += 1.5;
                if (_daysRented > 3)
                    result += (_daysRented - 3) * 1.5;
                break;
            }
        return result;
    }

    public int getFrequentRenterPoints(int _daysRented) {
        //add bonus for a two day new release rental
        if((_price.getPriceCode() == NEW_RELEASE) && _daysRented > 1)
              return 2;
        else
              return 1;
    }
}
