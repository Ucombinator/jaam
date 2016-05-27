class Factorial {
  public static void main(String[] args) {
    factorial(6);
  }
  public static int factorial(int n) {
    if (n <= 1)
      return 1;
    else
      return n*factorial(n-1);
  }
}
