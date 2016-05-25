public class Exceptions {
  public static void handler1() { }
  public static void handler2() { }

  public static void g(int k) throws Throwable {
    if (k == 1) {
      throw new Exception();
    } else if (k == 2) {
      throw new Throwable();
    } else {
      /* Do nothing */
    }
  }

  public static void f() throws Throwable {
    try {
      g(1);
      g(2);
      g(3);
    } catch (Exception e) {
      handler1();
    }
  }

  public static void main(String[] args) {
    try {
      f();
    } catch (Throwable e) {
      handler2();
    }
  }
}
