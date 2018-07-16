class AndOrLoop {
  public static void main(String[] args) {
    int i = 0;
    int a = 5;
    int b = 10;

    while(((i >= 0) && (a != 10)) || ((b != 20) && (b != 10)) && ((a != 8) || (i != 6)) || ((i != 25) || (i != 30))) {
      b = a + i;
      a++;
    }

  }
}
