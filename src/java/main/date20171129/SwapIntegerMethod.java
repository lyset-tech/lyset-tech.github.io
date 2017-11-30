package date20171129;

import java.lang.reflect.Field;

/**
 * TODO
 *
 * @author baifan
 * @version V1.0
 * @since 2017-11-30 19:07
 */
public class SwapIntegerMethod {

    public static void main(String[] args) throws Exception {
        Integer a = 139, b = 290;
        swap(a, b);
        System.out.println("a=" + a + ";b=" + b);
        Integer c = 139, d = 290;
        System.out.println("c=" + c + ";d=" + d);
    }

    public static void swap(Integer a1, Integer b1) throws Exception {
        Field valueField = Integer.class.getDeclaredField("value");
        valueField.setAccessible(true);
        int tempAValue = valueField.getInt(a1);
        valueField.setInt(a1, b1.intValue());
        valueField.setInt(b1, tempAValue);
    }
}
