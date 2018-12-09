public class True {
  public static void main(String[] args) {
    boolean i = false, j = true, k = false;
    while (i ? true : true) {
      int last = 5;
      System.out.println("foo");
      i = false;
    }
  }
}
