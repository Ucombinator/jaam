class WhileLoop {
  public static void main(String[] args) {
    int i = 0;
    int sum = 0;
    while (i < 100) {
      //int x = f();
      while (i < 50) {
        while (i < 10) {
          //int y = f();
          sum += (3 + i);
          ++i;
        }
      }
    }
    final int rl = (sum & 0xFFFFFFFF) << 32 | (sum & 0xFFFFFFFF);
    final int fp = f();
    while (i < 100) {
      //int x = f();
      while (i < 50) {
        while (i < 10) {
          //int y = f();
          sum += (3 + i);
          ++i;
        }
      }
    }
    int x = f();
  }

  public static int f() {
    int y = 0;
    for (y = 0; y < 10; y++) {
      y += y;
    }
    return y;
  }
}
