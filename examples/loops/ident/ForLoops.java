import java.util.*;

class ForLoops {
    public static void main(String[] args) {}

    public static int simpleFor() {
        int sum = 0;
        for (int i = 0; i < 100; ++i) {
            sum += i;
        }
        return sum;
    }

    public static int forWithBreak() {
        int sum = 0;
        for (int i = 10; i < 135; ++i) {
            sum += i;
            if (sum > 400) {
                break;
            }
        }
        return sum;
    }

    public static int varConstraintFor() {
        int sum = 0;
        int max = 75;
        for (int i = 0; i < max; ++i) {
            sum += i;
        }
        return sum;
    }

    public static int rhsConstraintFor() {
        int sum = 0;
        for (int i = 0; 45 > i; ++i) {
            sum += i;
        }
        return sum;
    }

    public static int nestedFor() {
        int sum = 0;
        for (int i = 0; i < 10; ++i) {
            for (int j = 1; j < 512; j *= 2) {
                sum += (i * j);
            }
        }
        return sum;
    }

    public static void infiniteFor() {
        int sum = 0;
        for (int i = 0; ; ++i) {
            sum += i;
            System.out.println(i);
        }
    }

    public static int infiniteBreakFor() {
        int sum = 0;
        for (int i = 0; ; ++i) {
            sum += i;
            if (sum > 142) {
                break;
            }
            sum *= 2;
        }
        return sum;
    }

    private static int return100() {
        return Math.abs(100);
    }

    public static int fakeWhileFor() {
        int sum = 0;
        int i = 0;
        for (; i < return100(); ) {
            sum += i;
            i += 1;
        }
        return sum;
    }

    public static int arrayIteratorLoop() {
        int[] intArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        int sum = 0;

        for (int i : intArr) {
            sum += i;
        }

        return sum;
    }

    public static int iteratableLoop() {
        ArrayList<Integer> intList = new ArrayList<>();
        intList.add(0);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.add(4);
        intList.add(5);
        intList.add(6);
        intList.add(7);
        intList.add(8);
        intList.add(9);

        int sum = 0;

        for (Integer i : intList) {
            sum += i;
        }

        return sum;
    }
}