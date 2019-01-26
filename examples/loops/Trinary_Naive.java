public class Trinary_Naive {
  public static void main(String[] args) {
    boolean i = false, j = true, k = false;
    while ((i && j) || (!i && k)) {
      int last = 5;
      System.out.println("foo");
      i = false;
    }
  }
}
