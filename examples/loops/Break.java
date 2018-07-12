public class Break {
  public static void main(String[] args) {
    int i = 0;
    while (true) {
      if (i > 10) { break; }
      System.out.println("foo");
      i++;
    }

    int j = 0;
    while (j <= 100) {
      System.out.println("bar");
      j++;
    }
  }
}
