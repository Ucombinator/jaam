public class Trinary {
  public static void main(String[] args) {
    boolean i = false, j = true, k = false;
    while ((i ? j : k)) {
      int last = 5;
      System.out.println("foo");
      i = false;
    }
  }
}
