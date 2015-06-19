class Fibonacci{
  public static void main(String[] args){
    //fibonacci(0);
  }
  public static int fibonacci(int n) {
    if(n<=1)
      return n;
    else
      return fibonacci(n-1)+fibonacci(n-2);
  }
}

