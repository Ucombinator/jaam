class MyStaticClass {
    public static int n = 10;

    static int getN() {
        return n;
    }
}

class ClinitTest {
    public static void main(String[] args) {
        MyStaticClass.getN();
    }
}
