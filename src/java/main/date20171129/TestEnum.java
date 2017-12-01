package date20171129;

/**
 * TODO
 *
 * @author baifan
 * @version V1.0
 * @since 2017-11-30 19:46
 */

public class TestEnum {

    public void test(EnumDemo enumDemo) {

        switch (enumDemo) {
        case A:
            System.out.println(enumDemo);
            break;
        case B:
            System.out.println(enumDemo);
        default:
            System.out.println();
        }
    }

    public static void main(String[] args) {
        for (EnumDemo enumDemo : EnumDemo.values()) {
            System.out.println(enumDemo.name() + ":" + enumDemo.ordinal());
        }
    }
}
