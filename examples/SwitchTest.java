public class SwitchTest{
  public static void main(String[] args) {
    test1();
    test2();
  }

  static void test1() {
    int num = 2;
    int temp = 0;
    switch(num){
      case 1:
        temp = 5;
        break;
      case 2:
        temp = 4;
         break;
      case 3:
         temp = 3;
         break;

      default:
         temp = 2;
    }
  }

  static void test2() {
    int num = 2;
    int temp = 0;
    switch(num){
      case 1:
        temp = 5;
        break;
      case 2:
        temp = 4;
         break;
      case 3:
         temp = 3;
         break;
    }
  }
}
