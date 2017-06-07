import java.util.List;
import java.util.ArrayList;

class ForEachLoop {
  public static void main(String[] args) {
    for (String arg : args) {
      System.out.println(arg);
    }

    /* Looks like:
       for (int i = 0; i < args.length; i++) {
         System.out.println(arg);
       }
    */

    List<String> stringList = new ArrayList(5);

    for (String string : stringList) {
      System.out.println(string);
    }
  }
}
