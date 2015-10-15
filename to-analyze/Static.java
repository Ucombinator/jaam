public class Static {
  static int x = 0;
  static {
    System.out.println("In Static");
    if (true)
      throw new RuntimeException("Foo");
    System.out.println("Out Static");
  }
  public static void main(String[] arg) {
      System.out.println("x="+x);
  }
}
