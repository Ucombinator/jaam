public class Fib {
  public static void main(String[] args) {
    System.out.println(fib(args.length));
  }

  static int fib(int i) {
    if (i <= 1) return 1;
    else return fib(i-1) + fib(i-2);
  }
}
