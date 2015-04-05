public class Interfaces {
  static interface I {
    int f();
  }

  static class C1 { }

  static class C2 extends C1 implements I {
    public int f() { return 3; }
  }

  static class C3 extends C2 { }

  public static void main(String[] args) {
    C3 c3 = new C3();
    int x = c3.f();
    I i = c3;
    int y = i.f();
  }
}
