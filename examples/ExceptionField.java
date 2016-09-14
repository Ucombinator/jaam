/**
 * If an exception is a field of class, it's initialized as null, 
 * then assigned to some exception object, and throw it, the interpreter
 * would try to throw an AnyAtomicValue.
 */

class MyException extends Exception {
    public MyException(String msg) {
        super(msg);
    }
}

class MyClass {
    private Exception exc;

    public void test() {
        exc = new MyException("something goes wrong");
        try {
            throw exc;
        }
        catch (Exception e) { }
    }
}
class ExceptionField {
    public static void main(String[] args) {
        MyClass c = new MyClass();
        c.test();
    }
}
