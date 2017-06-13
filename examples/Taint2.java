public class Taint2 {
  public static void main(String[] args) {
    int x = 3;
    int y = x;
    System.out.println("y: " + y);
  }
}
