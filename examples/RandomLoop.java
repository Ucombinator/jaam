public class RandomLoop {
  public static void main(String[] args) {
    int x = 3000;
    int q = 0;
    while (q < x) {
      System.out.println("bar");
      while (q < x && Math.random() < 0.5) {
        System.out.println("foo");
        ++q;
      }
    }
  }
}
