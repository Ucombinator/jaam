public class LoopTree {
  public static void main(String[] args) {
    int i = 0;
    while (i > 10) {
      if (i > 20) break;
      System.out.println("break");
      i++;
    }
  }
  public static void foo4() {
    int j;

    j = 0;
    while (((j * 60) < 10 || j < 20) && (j >= 30 || j >= 40)) {
      System.out.println("(j < 10 || j < 20) && (j >= 30 || j >= 40)");
      j++;
    }

    j = 0;
    while ((j < 10 && j < 20) || (j >= 30 && j >= 40)) {
      System.out.println("(j < 10 && j < 20) || (j >= 30 && j >= 40)");
      j++;
    }
  }

  public static void foo3() {
    int i;

    i = 0;
    do {
      System.out.println("i > 10 && i > 20 && i > 30");
      i++;
    } while (i > 10 && i > 20 && i > 30);

    i = 0;
    do {
      System.out.println("i > 10 || i > 20 || i > 30");
      i++;
    } while (i > 10 || i > 20 || i > 30);
  }
  public static void foo2() {
    int i;

    i = 0;
    while (i > 10 && i > 20 && i > 30) {
      System.out.println("i > 10 && i > 20 && i > 30");
      i++;
    }

    i = 0;
    while (i > 10 || i > 20 || i > 30) {
      System.out.println("i > 10 || i > 20 || i > 30");
      i++;
    }
  }
  public static void foo() {
    for (int i = 0; i < 10; i++) {
      System.out.println("Foo");
    }

    int j = 0;
    while (j < 10) {
      System.out.println("Bar");
      j++;
    }

    int k = 0;
    do {
      System.out.println("Baz");
      k++;
    } while (k < 10);

    j = 0;
    while ((j < 10 || j < 20) && (j >= 0 || j >= -1)) {
      System.out.println("Quux");
      j++;
    }

    k = 0;
    do {
      System.out.println("FooBar");
      k++;
    } while (k < 10 && k >= 0);
  }
}
