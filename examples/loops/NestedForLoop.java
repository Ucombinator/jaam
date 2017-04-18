class NestedForLoop {
  public static void main(String[] args) {
    int i, j, k;
    int sum = 0;
    for (i = 0; i <= 10; i++) {
      for (j = 0; j <= 10; j++) {
        for (k = 0; k <= 10; k++) {
          sum += (i + j);
        }
      }
      for (j = 0; j <= 10; j++) {
        for (k = 0; k <= 10; k++) {
          sum += (i + j);
          f();
        }
      }
    }
  }

  public static int g(int x) {
    int i, j, k;
    int sum = 0;
    for (i = 0; i <= 10; i++) {
      for (j = 0; j <= 10; j++) {
        for (k = 0; k <= 10; k++) {
          sum += (i + j);
        }
      }
      for (j = 0; j <= 10; j++) {
        for (k = 0; k <= 10; k++) {
          sum += (i + j);
        }
      }
    }
    return sum;
  }

  public static void f() {
    int i, j, k;
    int sum = 0;
    for (i = 0; i <= 10; i++) {
      for (j = 0; j <= 10; j++) {
        for (k = 0; k <= 10; k++) {
          sum += (i + j);
          int z = g(sum);
        }
      }
      int z = g(sum);
      for (j = 0; j <= 10; j++) {
        for (k = 0; k <= 10; k++) {
          sum += (i + j);
        }
      }
    }
  }
}

