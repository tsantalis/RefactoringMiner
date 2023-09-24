package addSharp;

public class addSharpTest {
    public static void f1() {
        System.out.println(addSharp("f1"));
    }

    public static void f2() {
        System.out.println(addSharp("f2"));
    }

    public static void main(String[] args) {
        f2();
        test();
        f1();

    }

    public static String addSharp(String inp) {
        return inp + "#";
    }
}