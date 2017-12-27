class WhileLoops {
    public static void main(String[] args) {
    }

    public static int simpleWhile() {
        int sum = 0;
        int i = 0;
        while (i < 100) {
            sum += i;
            i++;
        }
        return sum;
    }

    public static void infiniteWhile() {
        int sum = 0;
        int i = 0;
        while (true) {
            System.out.println(sum);
            sum += i;
            i++;
        }
    }

    public static int trickyInfiniteWhile() {
        int sum = 0;
        int i = 0;
        while (i < 100) {
            sum += i;
            // i never increments.
        }
        return sum;
    }
}