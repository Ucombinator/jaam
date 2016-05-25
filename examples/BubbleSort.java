public class BubbleSort {
  public static void main(String[] args) {
    int[] x = new int[100];
    sort(x);
    for (int i = 0; i < x.length; i++) {
      System.out.println(i);
    }
  }

  static void sort(int[] x) {
    for (int i = x.length; i > 0; i--) {
      for (int j = 1; j < i; j++) {
        if (x[j-1] < x[j]) {
          int tmp = x[j];
          x[j] = x[j-1];
          x[j-1] = tmp;
        }
      }
    }
  }
}
