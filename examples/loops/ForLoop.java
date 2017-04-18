class ForLoop {
  public static void main(String[] args) {
    int i = 0;
    int sum = 0;
    for (i = 0; i <= 100; i++) {
      sum += i;
      if (sum == 50) break;
    }

    for (i = 0; i <= 100; i++) {
      /*
      A a = new B();
      a.f();
      */
      A.g();
    }
  }
}

class A {
  public void f() {
    int i = 0;
    int sum = 0;
    for (i = 0; i <= 100; i++) {
      sum += i;
    }
  }

  public static void g() {
  
  }
}

class B extends A {
  public void f() {
    int i = 0, j = 0;
    int sum = 0;
    for (i = 0; i <= 100; i++) {
      for (j = 0; j <= 100; j++) {
        sum += (i + j);
      }
    }
  }
}
