public class Continue {
  public static void main(String[] args) {
    int i = 0;
    while (i > 2) {
      if (i > 10) { continue; }
      System.out.println("foo");
      i++;
    }
/*
    int j = 0;
    while (j <= 100) {
      System.out.println("bar");
      j++;
    }
*/
  }
}
