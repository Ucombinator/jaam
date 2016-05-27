public class CFGTest {
  public static void main(String[] args) {
    int z = 5;
    int x = z-2;
    int y = 2*z;
    int q = z / y;
    int p = x << 1;
    int r = x >> 1;

    if(z > 5){
      x = x+1;
      y = y+1;
    }
    else {
      x = x-1;
      y = y-1;
    }
    z = x + y;
  }
}
