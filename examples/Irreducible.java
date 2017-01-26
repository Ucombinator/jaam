
import java.util.Random;

public class Irreducible {
	
	public static void main() {
		Random r = new Random();
		double x = r.nextDouble();
		
		if(x < 0.5)
			f();
		else
			g();
	}
	
	public static void f() {
		g();
	}
	
	public static void g() {
		f();
	}
}