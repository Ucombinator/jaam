public class Break {
  public static void main(String[] args) {
    FOO: while (true) {
      try {
        System.out.println("Try");
        break FOO;
      } catch (Exception e) {
        System.out.println("Exception");
      }
    }
  }
}
