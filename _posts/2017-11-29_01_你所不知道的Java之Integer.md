---
layout:     post
title:      "你所不知道的Java之Integer"
subtitle:   "你知道Integer么"
date:       2017-11-29
author:     "觉醒"
header-img: "img/header-banner/coffee-bean_1920x1280.jpg"
catalog: true
tags:
    - java
---

以下内容为作者辛苦原创，版权归作者所有，如转载演绎请在“光变”微信公众号留言申请，转载文章请在开始处显著标明出处。

### 实参形参
前些天看到朋友圈分享了一片文章[《Java函数的传参机制——你真的了解吗？》](http://blog.csdn.net/whuxinxie/article/details/54895768)

#### 交换

请用Java完成swap函数，交换两个整数类型的值。
```java
public static void testOne() throws Exception {
    Integer a = 1, b = 2;
    swapOne(a, b);
    System.out.println("a=" + a + ", b=" + b);
}

static void swapOne(Integer a, Integer b){
   // 需要实现的部分
}
```

**第一次**
如果你不了解Java对象在内存中的分配方式，以及方法传递参数的形式，你有可能会写出以下代码。
```java
public static void swapOne(Integer a, Integer b) throws Exception {
    Integer aTempValue = a;
    a = b;
    b = aTempValue;
}
```
运行的结果显示a和b两个值并没有交换。
那么让我们来看一下上述程序运行时，Java对象在内存中的分配方式:
![对象地址分配](/img/2017.11.29/01.Object_allocation.png)

由此可以看到，在两个方法的局部变量表中分别持有的是对a、b两个对象实际数据地址的引用。
上面实现的swap函数，仅仅交换了swap函数里局部变量a和局部变量b的引用，并没有交换JVM堆中的实际数据。
所以main函数中的a、b引用的数据没有发生交换，所以main函数中局部变量的a、b并不会发生变化。

那么要交换main函数中的数据要如何操作呢？

**第二次**
根据上面的实践，可以考虑交换a和b在JVM堆上的数据值？
简单了解一下Integer这个对象，它里面只有一个对象级int类型的value用以表示该对象的值。
所以我们使用反射来修改该值，代码如下：
```java
public static void swapTwo(Integer a1, Integer b1) throws Exception {
    Field valueField = Integer.class.getDeclaredField("value");
    valueField.setAccessible(true);
    int tempAValue = valueField.getInt(a1);
    valueField.setInt(a1, b1.intValue());
    valueField.setInt(b1, tempAValue);
}
```
运行结果，符合预期。


#### 惊喜
上面的程序运行成后，如果我在声明一个`Integer c = 1, d = 2;`会有什么结果

示例程序如下：
```java

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
```

输出的结果如下：
```text
a=2; b=1
c=2; d=1
```

> 惊喜不惊喜！意外不意外！刺激不刺激！

![惊喜不惊喜](/img/2017.11.29/01.suprise.jpeg)


#### 深入
究竟发生了什么？让我们来看一下反编译后的代码：

> 作者使用IDE工具，直接反编译了这个.class文件

```java
public static void testThree() throws Exception {
    Integer a = Integer.valueOf(1);
    Integer b = Integer.valueOf(2);
    swapTwo(a, b);
    System.out.println("a=" + a + "; b=" + b);
    Integer c = Integer.valueOf(1);
    Integer d = Integer.valueOf(2);
    System.out.println("c=" + c + "; d=" + d);
}
```

在Java对原始类型int自动装箱到Integer类型的过程中使用了`Integer.valueOf(int)`这个方法了。
肯定是这个方法在内部封装了一些操作，使得我们修改了`Integer.value`后，产生了全局影响。
所有这涉及该部分的代码一次性粘完（PS：不拖拉的作者是个好码农）：
```java
public class Integer{
    /**
     * @since  1.5
     */
    public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
    }
    
    private static class IntegerCache {
        static final int low = -128;
        static final int high;
        static final Integer cache[];

        static {
            // high value may be configured by property
            int h = 127;
            String integerCacheHighPropValue =
                sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
            if (integerCacheHighPropValue != null) {
                try {
                    int i = parseInt(integerCacheHighPropValue);
                    i = Math.max(i, 127);
                    // Maximum array size is Integer.MAX_VALUE
                    h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
                } catch( NumberFormatException nfe) {
                    // If the property cannot be parsed into an int, ignore it.
                }
            }
            high = h;

            cache = new Integer[(high - low) + 1];
            int j = low;
            for(int k = 0; k < cache.length; k++)
                cache[k] = new Integer(j++);

            // range [-128, 127] must be interned (JLS7 5.1.7)
            assert IntegerCache.high >= 127;
        }

        private IntegerCache() {}
    }
    
}

```

如上所示`Integer`内部有一个私有静态类`IntegerCache`，该类静态初始化了一个包含了`Integer.IntegerCache.low`到`java.lang.Integer.IntegerCache.high`的Integer数组。
其中`java.lang.Integer.IntegerCache.high`的取值范围在\[127\~Integer.MAX_VALUE - (-low) -1\]之间。
在该区间内所有的`Integer.valueOf(int)`函数返回的对象，是根据int值计算的偏移量，从数组`Integer.IntegerCache.cache`中获取，对象是同一个，不会新建对象。

所以当我们修改了`Integer.valueOf(1)`的`value`后，所有`Integer.IntegerCache.cache[ 1 - IntegerCache.low ]`的返回值都会变更。

我相信你们的智商应该理解了，如果不理解请在评论区`call 10086`。

好了，那么超过`IntegerCache.high`的部分呢？
很显然，它们是幸运的，没有被IntegerCache缓存到，法外之民，每次它们的到来，都会new一边，在JVM上分配一块土（内）地（存）。

#### 遐想
如果我把转换的参数换成类型换成`int`呢？
```java
public static void testOne() throws Exception {
    int a = 1, b = 2;
    swapOne(a, b);
    System.out.println("a=" + a + ", b=" + b);
}

static void swapOne(int a, int b){
   // 需要实现的部分
}
```

以作者目前的功力，无解。高手可以公众号留言，万分感谢！
至此`swap`部分已经讲完了。


#### 1 + 1
首先让我们来看一下代码：
```java
public static void testOne() {
    int one = 1;
    int two = one + one;
    System.out.printf("Two=%d", two);
}
```

请问输出是什么？
如果你肯定的说是`2`，那么你上面是白学了，请直接拨打`95169`。
我可以肯定的告诉你，它可以是\[`Integer.MIN_VALUE\~Integer.MAX_VALUE`\]区间的任意一个值。

> 惊喜不惊喜！意外不意外！刺激不刺激！

![惊喜不惊喜](/img/2017.11.29/01.suprise.jpeg)

让我们再撸（捋）一（一）串（遍）烧（代）烤（码）。

> 作者使用IDE工具，直接反编译了这个.class文件
```java
public static void testOne() {
    int one = 1;
    int two = one + one;
    System.out.printf("Two=%d", two);
}
```

这里的变量`two`竟然没有调用`Integer.valueOf(int)`，跟想象的不太一样，我怀疑这是IDE的锅。
所以果断查看编译后的字节码。以下为摘录的部分字节码：
```text
LDC "Two=%d"
ICONST_1
ANEWARRAY java/lang/Object
DUP
ICONST_0
ILOAD 2
INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
AASTORE
INVOKEVIRTUAL java/io/PrintStream.printf (Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintStream;
POP
```

可以看出确实是IDE的锅，这里不仅调用了一次`Integer.valueOf(int)`，而且还创建一个`Object`的数组。
完整的Java代码应该是如下所示：
```java
public static void testOne() {
    int one = 1;
    int two = one + one;
    Object[] params = { Integer.valueOf(two) };
    System.out.printf("Two=%d", params);
}
```

所以只要在方法调用前修改`Integer.IntegerCache.cache[2]`的值就可以了，所以在类的静态初始化部分加些代码。

```java
public class OnePlusOne {
    static {
        try {
            Class<?> cacheClazz = Class.forName("java.lang.Integer$IntegerCache");
            Field cacheField = cacheClazz.getDeclaredField("cache");
            cacheField.setAccessible(true);
            Integer[] cache = (Integer[]) cacheField.get(null);
            //这里修改为 1 + 1 = 3            
            cache[2 + 128] = new Integer(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testOne() {
        int one = 1;
        int two = one + one;
        System.out.printf("Two=%d", two);
    }
}
```

#### two == 2 ？
在修改完`Integer.IntegerCache.cache[2 + 128]`的值后，变量`two`还等于`2`么？
```java
public static void testTwo() {
    int one = 1;
    int two = one + one;
    System.out.println(two == 2);
    System.out.println(Integer.valueOf(two) == 2);
}
```

上述代码输出如下
> true
> false

因为`two == 2`不涉及到Integer装箱的转换，还是原始类型的比较，所以原始类型的`2`永远等于`2`。
`Integer.valueOf(two)==2`的真实形式是`Integer.valueOf(two).intValue == 2`，即`3==2`，所以是false。

> 这里可以看到如果拿一个值为null的Integer变量和一个int变量用双等号比较，会抛出NullPointException。

### 后记

#### XCache

| 类 | 是否有Cache | 最小值 | 最大值 |
|----|----|----|----|
|Boolean | 无| -- | -- |  
|Byte | ByteCache| -128 | 127(固定) |  
|Short | ShortCache| -128 | 127(固定) |  
|Character | CharacterCache| 0  | 127(固定) |  
|Integer | IntegerCache| -128 | java.lang.Integer.IntegerCache.high |
|Long | LongCache| -128 | 127(固定) |
|Float |  无| -- | -- |  
|Double |  无| -- | -- |  

#### java.lang.Integer.IntegerCache.high
看了IntegerCache类获取high的方法`sun.misc.VM.getSavedProperty`，可能大家会有以下疑问，我们不拖沓，采用一个问题一解答的方式。

**1. 这个值如何如何传递到JVM中？**
 
和系统属性一样在JVM启动时，通过设置`-Djava.lang.Integer.IntegerCache.high=xxx`传递进来。
 
**2. 这个方法和`System.getProperty`有什么区别？**

为了将JVM系统所需要的参数和用户使用的参数区别开，
`java.lang.System.initializeSystemClass`在启动时，会将启动参数保存在两个地方：
**2.1** `sun.misc.VM.savedProps`中保存全部JVM接收的系统参数。
JVM会在启动时，调用`java.lang.System.initializeSystemClass`方法，初始化该属性。
同时也会调用`sun.misc.VM.saveAndRemoveProperties`方法，从`java.lang.System.props`中删除以下属性：
  - sun.nio.MaxDirectMemorySize
  - sun.nio.PageAlignDirectMemory
  - sun.lang.ClassLoader.allowArraySyntax
  - java.lang.Integer.IntegerCache.high
  - sun.zip.disableMemoryMapping
  - sun.java.launcher.diag
以上罗列的属性都是JVM启动需要设置的系统参数，所以为了安全考虑和隔离角度考虑，将其从用户可访问的System.props分开。

**2.2** `java.lang.System.props`中保存除了以下JVM启动需要的参数外的其他参数。
  - sun.nio.MaxDirectMemorySize
  - sun.nio.PageAlignDirectMemory
  - sun.lang.ClassLoader.allowArraySyntax
  - java.lang.Integer.IntegerCache.high
  - sun.zip.disableMemoryMapping
  - sun.java.launcher.diag

> PS：作者使用的JDK 1.8.0_91时，以下参数会从全部JVM接收的系统参数中移除：

