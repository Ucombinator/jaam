import java.util.*;
import java.io.IOException;

class ExceptionLoops {
    public static void main(String[] args) {}

    public static void throwException() throws RuntimeException {
        throw new RuntimeException();
    }

    public void nestless() {
        try {
            int i = 0;
            int i2 = 0;
            int i3 = 0;
            System.out.println("try");
        }
        catch (RuntimeException ex) {
            System.out.println("catch");
        }
        finally {
            System.out.println("finally");
        }

        return;
    }

    public int nestedOne() {
        int val = 0;

        try {
            val = 1;
            throwException();
        }
        catch (RuntimeException ex) {
            val = 2;
            return val;
        }
        finally {
            try {
                val = 3;
                throwException();
            }
            catch (RuntimeException ex2) {
                val = 4;
            }
        }

        return val;
    }

    public int nestedTwo() {
        int val = 0;

        try {
            val = 1;
            throwException();
        }
        catch (RuntimeException ex) {
            val = 2;
            return val;
        }
        finally {
            try {
                val = 3;
                throwException();
            }
            catch (RuntimeException ex2) {
                val = 4;
                return val;
            }
            finally {
                val = 5;
            }
        }

        return val;
    }
}