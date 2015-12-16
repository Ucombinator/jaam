class MyException extends Exception {
    public MyException(String msg) {
        super(msg);
    }
}

class ThrowTest {
    public static void main(String[] args) {
        try {
            throw new MyException("something goes wrong");
        }
        catch (MyException e) {
            
        }
    }
}

