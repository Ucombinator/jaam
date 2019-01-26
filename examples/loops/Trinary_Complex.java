public class Trinary_Complex {
  public static void main(String[] args) {
    boolean x = true;
    int y = 3, t = 2, z = 1;
    while ((x ? (y+t+1) : (y+z+2)) > 3) {
      int last = 5;
      System.out.println("foo");
      x = false;
    }
  }
}
