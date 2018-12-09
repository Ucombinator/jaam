public class Cond_Complex {
  public static void main(String[] args) {
    boolean a = true, b = false;
    while (a ? (b || true) : false) {
      int last = 5;
      System.out.println("foo");
      a = false;
    }
  }
}
