import org.junit.internal.matchers.IsCollectionContaining;


public class A<T> {
	
	public void foo() {
		String item = "x";
		org.hamcrest.Matcher<java.lang.Iterable<String>> it = hasItem(item);
	}

	public static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItem(T element) {
		return IsCollectionContaining.hasItem(element);
	}

	public void a() {
		b();
	}
	
	public void b() {
		
	}
}