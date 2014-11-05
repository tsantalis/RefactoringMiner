import org.junit.internal.matchers.IsCollectionContaining;
import org.hamcrest.Matcher;

public class A<T> {
	
	public void foo() {
		String item = "x";
		org.hamcrest.Matcher<java.lang.Iterable<String>> it = hasItem(item);
	}

	public static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItem(T element) {
		return IsCollectionContaining.hasItem(element);
	}

	public void a(org.hamcrest.Matcher m) {
		b();
	}
	
	public void b() {
		
	}
}