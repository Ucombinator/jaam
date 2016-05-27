public class Statics {
  static final int a = 1000;
  static int b = 2000;
  static int c;
  int d;

  static {
    c = 3000;
}

  public static void main(String[] args) {
    Statics s = new Statics();
    String x = args[a + b + c + s.d];
  }
}
