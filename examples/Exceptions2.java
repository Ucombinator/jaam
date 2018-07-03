public class Exceptions2 {
  public void foo() {
    try {
      if (System.out == System.err) return;
      System.out.println("foo");
    } catch (Exception e) {
      System.out.println("exception");
    } finally {
      System.out.println("finally");
    }
    System.out.println("bar");
  }
}
