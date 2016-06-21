class PrivateTests {
    public static int myInt = 0;
    private static int privateInt = 42;

    public static void main(String[] args) {
        doPrivateThing();
        System.out.println(myInt);
        System.out.println(privateInt);
        System.out.println(ReallyPrivate.returnPrivateBool());
        System.out.println(ReallyPrivate.returnPublicBool());
        System.out.println(ReallyPrivate.publicBool);
    }
    private static void doPrivateThing() { myInt = privateInt; }

    private static class ReallyPrivate {
        public static boolean publicBool = false;
        private static boolean privateBool = true;

        public static boolean returnPublicBool() { return publicBool; }
        public static boolean returnPrivateBool() { return privateBool; }

        private static int returnFortyTwo() { return 42; }
    }
}