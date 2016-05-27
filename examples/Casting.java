public class Casting {
  class A {
  }
  class B extends A {
  }
  public static void main(String[] args) {
    B[] bs = new B[5];
    A[] as = (A[]) bs;
  }
}
