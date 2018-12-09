public class True_Complex {
  public static void main(String[] args) {
    boolean a = false, b = true, c = false, d = true, e = false;
    while (a ? (!b && (d || true)) : (c && (e || true))) {
      int last = 5;
      System.out.println("foo");
      a = false;
    }
  }
}
