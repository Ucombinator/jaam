public class ClassLoading {
  public static void main(String[] args) {
    new ClassLoadingA().f();
    new ClassLoadingB().f();
  }
}

class ClassLoadingA {
  public void f() { }
}

class ClassLoadingB {
  public void f() { }
}
