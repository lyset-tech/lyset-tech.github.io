package date20171129;

import java.lang.reflect.Field;

/**
 * 测试交换Integer
 *
 * @author baifan
 * @version V1.0
 * @since 2017-11-30 19:07
 */
public class SwapIntegerMethod {

    public static void main(String[] args) throws Exception {
        System.out.println(Integer.valueOf(255) == Integer.valueOf(255));
        System.out.println(Integer.valueOf(256) == Integer.valueOf(256));
        System.out.println(Integer.valueOf(257) == Integer.valueOf(257));
        //        以下方法不能同时测试，否则会影响输出效果。
        //        System.out.println("=============ONE================");
        //        testOne();
        //        System.out.println("=============TWO================");
        //        testTwo();
        System.out.println("=============THREE================");
        testThree();
        //        System.out.println("=============FOUR================");
        //        testFour();
        System.out.println(System.getProperty("java.lang.Integer.IntegerCache.high"));
    }

    public static void testOne() throws Exception {
        Integer a = 1, b = 2;
        swapOne(a, b);
        System.out.println("a=" + a + "; b=" + b);
    }

    public static void swapOne(Integer a, Integer b) throws Exception {
        Integer aTempValue = a;
        a = b;
        b = aTempValue;
    }

    public static void testTwo() throws Exception {
        Integer a = 1, b = 2;
        swapTwo(a, b);
        System.out.println("a=" + a + "; b=" + b);
    }

    public static void swapTwo(Integer a1, Integer b1) throws Exception {
        Field valueField = Integer.class.getDeclaredField("value");
        valueField.setAccessible(true);
        int tempAValue = valueField.getInt(a1);
        valueField.setInt(a1, b1.intValue());
        valueField.setInt(b1, tempAValue);
    }

    public static void testThree() throws Exception {
        Integer a = 1, b = 2;
        swapTwo(a, b);
        System.out.println("a=" + a + "; b=" + b);
        Integer c = 1, d = 2;
        System.out.println("c=" + c + "; d=" + d);
    }

    public static void testFour() throws Exception {
        Integer a = 128, b = 2;
        swapTwo(a, b);
        System.out.println("a=" + a + "; b=" + b);
        Integer c = 128, d = 2;
        System.out.println("c=" + c + "; d=" + d);
    }

}
