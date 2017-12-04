---
layout:     post
title:      "switch(Long)的故事"
subtitle:   "一些Java编程的常识"
date:       2017-11-31
author:     "飞鸟"
header-img: "img/header-banner/coffee-bean_1920x1280.jpg"
catalog: true
tags:
    - java
---

# switch(Long)的故事

作为一个java新手在学习java的过程中，机缘巧合，我写了一段这样的代码
```java
  Long l = 0L;
  switch (l){
      ...
  }
```
出现了这样的错误：

>T.java:5: error: incompatible types: Long cannot be converted to int
>       switch (l){

## 学习过程中总会有些不能理解的地方，而我 ^ ^ 选择百度 ^ ^

[Java语言规范](https://docs.oracle.com/javase/specs/jls/se8/jls8.pdf)里是这样说的

>switch works with the byte, short, char, and int primitive data types.It also works with enumerated types (discussed in Enum Types), the String class, and a few special classes that wrap certain primitive types: Character, Byte, Short, and Integer .
     
？？？ only byte，short，char，int

？？？ Enum，String，Character，Byte，Short，Integer
     
嗯？String都能支持,Long居然不支持，为什么没有Long？

## 不能理解，我们接着  ^ ^ 百度 ^ ^

从"20年前"的Java虚拟机规范里上找到[Compile Switch](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-3.html#jvms-3.10)这一节 

里面是这样说的：
    
>Compilation of switch statements uses the tableswitch and lookupswitch instructions.The tableswitch instruction is used when the cases of the switch can be efficiently represented as indices into a table of target offsets. The default target of the switch is used if the value of the expression of the switch falls outside the range of valid indices. 

>编译switch 使用两种指令 tableswitch 和 lookupswitch
>当switch内的case值能被表示为一个表中的索引值时，则使用tableswitch.

接着看

>The Java Virtual Machine's tableswitch and lookupswitch instructions operate only on int data. Because operations on byte, char, or short values are internally promoted to int, a switch whose expression evaluates to one of those types is compiled as though it evaluated to type int. 

>tableswitch 和lookupswitch只操作在int数据上，对于byte char short的操作在内部都会提升为int


原来JVM底层提供两种只支持32位大小的偏移量（刚好是int类型的大小）的switch指令 `tableswitch`  和 `lookupswitch` .
所以在java中其实也只实现了byte, short, char, and int的switch，至于他们的包装类型以及Enum，String都是Java编译器给我们的语法糖,甚至于byte，short，char也会在运行时提升为int.

## 只有Int 别的都是语法糖，那我关心一下实现可以吧  ^ ^ 百度 ^ ^

既然都是语法糖，了解了解是怎么实现的吧，先看看原始类型的包装类是如何实现switch的.
 
### switch(Integer)
``` java
private void integerSwitch(){
 Integer integerS = 0;
 switch (integerS){
  case 1:
  case 0:
  case 2:
   System.out.println(integerS);
 }
}
```
使用 [jad](http://www.javadecompilers.com/) 反编译后：

``` java
private void integerSwitch()
{
  Integer integer = Integer.valueOf(0);
  switch(integer.intValue()){
    case 0: // '\0'
    case 1: // '\001'
    case 2: // '\002'
      System.out.println(integer);
      break;
  }
}
```
嗯，果然是真的，调用了Integer.intValue()返回原始类型，

等下，怎么顺序感觉怪怪的，说好的`1,0,2`，居然给我排了队，

还是用JDK自带的javap，看一眼bytecode吧

``` java
0: iconst_0
1: invokestatic  #2 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
4: astore_1
5: aload_1
6: invokevirtual #3 // Method java/lang/Integer.intValue:()I
9: tableswitch   { // 0 to 2
               0: 36
               1: 36
               2: 36
         default: 43
    }
36: getstatic     #4 // Field java/lang/System.out:Ljava/io/PrintStream;
39: aload_1
40: invokevirtual #5 // Method java/io/PrintStream.println:(Ljava/lang/Object;)V
43: return
```
嗯，jad还是可信的，前面Java虚拟机规范提到，当switch内的case值能被表示为一个表中的索引值时，则使用`tableswitch`，
看样子是，编译器为我们调整了顺序，似乎它更喜欢`tableswitch`，接着看下一个类型。

### switch( String )
``` java
private void test1(){
  String feiniao = "feiniao";
    switch(feiniao){
      case "FB":
        System.out.println("FB");
        break;
      case "Ea":
        System.out.println("Ea");
        break;
    }
}
```
使用[jad](http://www.javadecompilers.com/) 反编译后：
``` java
private void test1()
  {
    String s = "feiniao";
    String s1 = s;
    byte byte0 = -1;
    switch(s1.hashCode())
    {
    case 2236:
      if(s1.equals("Ea"))
          byte0 = 1;
      else
      if(s1.equals("FB"))
          byte0 = 0;
      break;
    }
    switch(byte0)
    {
    case 0: // '\0'
      System.out.println("FB");
      break;

    case 1: // '\001'
        System.out.println("Ea");
        break;
    }
  }
```
- 先来看看这个，使用了新的变量s1，防止并发操作，所造成的结果不可知.
- 然后使用了一个byte的变量和两次的switch，防止了[hash碰撞](https://zacard.net/2016/08/29/hash-collision/)

哇，这糖为什么可以这么甜那！继续看看还有没有更甜的。
### switch( Enum )
```java
enum Em {
  A,B,C,D,E;
}
private void test2(){
   Em em = Em.A;
   switch (em){
    case A:
     System.out.println("A");
     break;
    case C:
     System.out.println("C");
     break;
    case E:
     System.out.println("E");
     break;
    default:
   }
}
```   
使用[jad](http://www.javadecompilers.com/) 反编译后：
```java
private void test2()
  {
    Em em = Em.A;
    static class _cls1
    {

      static final int $SwitchMap$Em[];

      static
      {
        $SwitchMap$Em = new int[Em.values().length];
        try
        {
          $SwitchMap$Em[Em.A.ordinal()] = 1;
        }
        catch(NoSuchFieldError nosuchfielderror) { }
        try
        {
          $SwitchMap$Em[Em.C.ordinal()] = 2;
        }
        catch(NoSuchFieldError nosuchfielderror1) { }
        try
        {
          $SwitchMap$Em[Em.E.ordinal()] = 3;
        }
        catch(NoSuchFieldError nosuchfielderror2) { }
      }
    }

    switch(_cls1.SwitchMap.Em[em.ordinal()])
    {
    case 1: // '\001'
      System.out.println("A");
      break;

    case 2: // '\002'
      System.out.println("C");
      break;

    case 3: // '\003'
      System.out.println("E");
      break;
    }
}
```   
信息量有点大

- 首先方法内多了一个类，

- 然后目录里多了一个名 T$1的类，

- switch的东西也不是原先的我们定义的Em，

让我缓缓，，，先用javap看一下bytecode

```java
 0: getstatic     #9 // Field Em.A:LEm;
 3: astore_1
 4: getstatic     #10 // Field T$1.$SwitchMap$Em:[I
 7: aload_1
 8: invokevirtual #11 // Method Em.ordinal:()I
11: iaload
12: tableswitch   { // 1 to 3
               1: 40
               2: 51
               3: 62
         default: 73
    }
...
```   

从bytecode可以看到，并没有什么`_cls1`类，不过可以看到一个静态常量`T$1.$SwitchMap$Em`，目录里的也确实多了`T$1.class`这个类。看看这个类是个什么鬼。
```java
static class T$1
{

  static final int $SwitchMap$Em[];

  static
  {
    $SwitchMap$Em = new int[Em.values().length];
    try
    {
      $SwitchMap$Em[Em.A.ordinal()] = 1;
    }
    catch(NoSuchFieldError nosuchfielderror) { }
    try
    {
      $SwitchMap$Em[Em.C.ordinal()] = 2;
    }
    catch(NoSuchFieldError nosuchfielderror1) { }
    try
    {
      $SwitchMap$Em[Em.E.ordinal()] = 3;
    }
    catch(NoSuchFieldError nosuchfielderror2) { }
  }
}
```
好像明白了什么！
- 假如不使用这个额外的T$1类，我所想象的代码因该长这样
``` java
//常量池里的东西先不管了
0: getstatic     #9     
3: astore_1
4: aload_1
5: invokevirtual #11 // Method Em.ordinal:()I
9: lookupswitch   { // 1 to 3
            1: 40
            3: 51
            5: 62
        default: 73
}
...
```
编译器废了这么大力气，就为了把`lookupswitch`替换成`tableswitch` ?

看来区别就在于 `tableswitch` 和 `lookupswitch` 了

让我着回去翻Java虚拟机规范。

## tableswitch 和 lookupswitch 的区别?

>Where the cases of the switch are sparse, the table representation of the tableswitch instruction becomes inefficient in terms of space. The lookupswitch instruction may be used instead. The lookupswitch instruction pairs int keys (the values of the case labels) with target offsets in a table. When a lookupswitch instruction is executed, the value of the expression of the switch is compared against the keys in the table. If one of the keys matches the value of the expression, execution continues at the associated target offset. If no key matches, execution continues at the default target.

前面提到了，当switch内的case值能被表示为一个表中的索引值时，则使用`tableswitch`，
但是，当`switch`里的case值非常稀疏的时候，`tableswitch`的做法在空间损耗方面非常糟糕，
有道理，所以`lookupswitch`的做法是，将case的int值和转跳的偏移量作为一对放在了一个表里，
当`lookupswitch`被执行的时候，这switch的表达式的值和这个表里的keys逐一比较，
没有找到则使用默认值，似乎在空间上是省了，不过时间上就慢了。

我们再看一下编译器是怎么选择的源码：
```java
// For each case, store its label in an array.
int lo = Integer.MAX_VALUE;  // minimum label.
int hi = Integer.MIN_VALUE;  // maximum label.
int nlabels = 0;               // number of labels.
...
// Determine whether to issue a tableswitch or a lookupswitch
// instruction.
long table_space_cost = 4 + ((long) hi - lo + 1); // words
long table_time_cost = 3; // comparisons
long lookup_space_cost = 3 + 2 * (long) nlabels;
long lookup_time_cost = nlabels;
int opcode =
    nlabels > 0 &&
    table_space_cost + 3 * table_time_cost <=
    lookup_space_cost + 3 * lookup_time_cost
    ?
    tableswitch : lookupswitch;
```
从上面的code可以看到，在最大和最小case的差值不大，且label数偏多的情况下，会选择`tableswitch`，
当差值很大，label数不多的情况下，会选择`lookupswitch`。
 
## 好奇宝宝时间！
作为一名重度强迫症患者加好奇宝宝，我就是想知道，编译器废了这么大劲，性能到底能差多少？

从上面的code可以看到`tableswitch`的时间复杂度是O(1)，`lookupswitch`的时间复杂度是O(n)，好现在这是我们的假设，让我们看看结果。

首先我们选择使用[JMH](http://openjdk.java.net/projects/code-tools/jmh/)做我们的微基准测试框架
```java
@State(Scope.Benchmark)
public class SwitchBenchmark {
  @Param({"1", "2", "3", "4", "5", "6", "7", "8"})//类似与Junit的 Parameterized
  int n;

  @Benchmark
  public long lookupSwitch() {
    return Switch.lookupSwitch(n);
  }

  @Benchmark
  public long tableSwitch() {
    return Switch.tableSwitch(n);
  }
}
public class Switch {
  public Switch() {
  }

  public static int lookupSwitch(int var0) {
    switch(var0) {
    case 1:
      return 11;
    case 2:
      return 22;
    case 3:
      return 33;
    case 4:
      return 44;
    case 5:
      return 55;
    case 6:
      return 66;
    case 7:
      return 77;
    default:
      return -1;
    }
  }

  public static int tableSwitch(int var0) {
    switch(var0) {
    case 1:
      return 11;
    case 2:
      return 22;
    case 3:
      return 33;
    case 4:
      return 44;
    case 5:
      return 55;
    case 6:
      return 66;
    case 7:
      return 77;
    default:
      return -1;
    }
  }
}

```
当然我们直接java代码这样写Switch类是没用的，因为这样的`lookupSwitch`会被被编译器优化成`tableswitch`，
所以我们使用[jasmin](https://github.com/Sable/jasmin)写
```java
.class public nefk/Switch
.super java/lang/Object

.method public <init>()V
   aload_0
   invokenonvirtual java/lang/Object/<init>()V
   return
.end method

.method public static lookupSwitch(I)I
    .limit stack 1

    iload_0
    lookupswitch
      1 : One
      2 : Two
      3 : Three
      4 : Four
      5 : Five
      6 : Six
      7 : Seven
      default : Other

One:
    bipush 11
    ireturn
Two:
    bipush 22
    ireturn
Three:
    bipush 33
    ireturn
Four:
    bipush 44
    ireturn
Five:
    bipush 55
    ireturn
Six:
    bipush 66
    ireturn
Seven:
    bipush 77
    ireturn
Other:
    bipush -1
    ireturn
.end method

.method public static tableSwitch(I)I
    .limit stack 1

    iload_0
    tableswitch 1
      One
      Two
      Three
      Four
      Five
      Six
      Seven
      default : Other

One:
    bipush 11
    ireturn
Two:
    bipush 22
    ireturn
Three:
    bipush 33
    ireturn
Four:
    bipush 44
    ireturn
Five:
    bipush 55
    ireturn
Six:
    bipush 66
    ireturn
Seven:
    bipush 77
    ireturn
Other:
    bipush -1
    ireturn
.end method
```
从switch 1 到 switch 8 个迭代测试200次 吞吐量平均值是这样的
```
Benchmark      (n)   Mode  Cnt          Score          Error  Units
lookupSwitch    1  thrpt  200  323330446.034 ±  2445701.999  ops/s
tableSwitch     1  thrpt  200  319148199.518 ±  2598409.017  ops/s

lookupSwitch    2  thrpt  200  362462418.531 ±  2113632.446  ops/s
tableSwitch     2  thrpt  200  336827136.850 ± 13390837.980  ops/s

lookupSwitch    3  thrpt  200  358321384.210 ±  3603503.904  ops/s
tableSwitch     3  thrpt  200  357352579.969 ±  4501450.687  ops/s

lookupSwitch    4  thrpt  200  401580310.966 ±  4127000.494  ops/s
tableSwitch     4  thrpt  200  376005438.482 ± 14937683.947  ops/s

lookupSwitch    5  thrpt  200  318344827.113 ±  2553139.591  ops/s
tableSwitch     5  thrpt  200  316609397.120 ±  4390158.172  ops/s

lookupSwitch    6  thrpt  200  327729755.563 ±  3328732.069  ops/s
tableSwitch     6  thrpt  200  321749811.593 ±  2651391.425  ops/s

lookupSwitch    7  thrpt  200  361246393.893 ±  4742280.906  ops/s
tableSwitch     7  thrpt  200  358376601.529 ±  3350421.622  ops/s

lookupSwitch    8  thrpt  200  347337820.333 ±  4287812.567  ops/s
tableSwitch     8  thrpt  200  355080497.046 ±  3393523.154  ops/s
```
### 预测失败！！！
对与相同的case值`tableswitch`和`lookupswitch`性能是差不多的，差不多的！

嗯，前面编译器做了那么多，原来结果都是一样的，我只想问一句，大哥你是不是觉得心有点累，

我帮你监督一下你的小弟JIT。

### 好像被欺骗了 

我们先加上两个参数`-XX:+UnlockDiagnosticVMOptions  -XX:CompileCommand=print,nefk.Switch::*`，看看JIT编译出来的汇编代码

`tableswitch`的汇编代码:
``` java
# {method} {0x0000000108c4c008} 'tableSwitch' '(I)I' in 'nefk/Switch'
# parm0:    rsi       = int
#           [sp+0x20]  (sp of caller)
0x0000000109a49800: sub    $0x18,%rsp
0x0000000109a49807: mov    %rbp,0x10(%rsp)  ;*synchronization entry
                                            ; - nefk.Switch::tableSwitch@-1

0x0000000109a4980c: cmp    $0x4,%esi
0x0000000109a4980f: je     0x0000000109a49865
0x0000000109a49811: cmp    $0x4,%esi
0x0000000109a49814: jg     0x0000000109a49841
0x0000000109a49816: cmp    $0x2,%esi
0x0000000109a49819: je     0x0000000109a4983a
0x0000000109a4981b: cmp    $0x2,%esi
0x0000000109a4981e: jg     0x0000000109a49833
0x0000000109a49820: cmp    $0x1,%esi
0x0000000109a49823: jne    0x0000000109a4982c  ;*tableswitch
                                            ; - nefk.Switch::tableSwitch@1lookupswitch:
...
```
`lookupswitch`的汇编代码:
``` java
# {method} {0x000000010a7b30e8} 'lookupSwitch' '(I)I' in 'nefk/Switch'
# parm0:    rsi       = int
#           [sp+0x20]  (sp of caller)
0x000000010d692200: sub    $0x18,%rsp
0x000000010d692207: mov    %rbp,0x10(%rsp)    ;*synchronization entry
                                              ; - nefk.Switch::lookupSwitch@-1

0x000000010d69220c: cmp    $0x4,%esi
0x000000010d69220f: je     0x000000010d692265
0x000000010d692211: cmp    $0x4,%esi
0x000000010d692214: jg     0x000000010d692241
0x000000010d692216: cmp    $0x2,%esi
0x000000010d692219: je     0x000000010d69223a
0x000000010d69221b: cmp    $0x2,%esi
0x000000010d69221e: jg     0x000000010d692233
0x000000010d692220: cmp    $0x1,%esi
0x000000010d692223: jne    0x000000010d69222c  ;*lookupswitch
                                               ; - nefk.Switch::lookupSwitch@1
...
```
`lookupswitch`和`tableswitch`进过JIT优化生成的机器码，居然用得都是二分，
醉了，不是说好`tableswitch`直接使用跳表，感觉被欺骗了。

### Talk is cheap. Show me the code
看样子什么都不可靠，我们还是直接找代码吧，在[parse2.cpp](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/75d40493551f/src/share/vm/opto/parse2.cpp#l440)文件里的create_jump_table方法内，
终于找到了我要的！

源码是如此简单的：
```c++
...
if (num_cases < MinJumpTableSize || num_cases > MaxJumpTableSize)
  return false;
if (num_cases > (MaxJumpTableSparseness * num_range))
  return false;
...
```
- 默认 `MinJumpTableSize` 为 10 ,case的数量小于这个不会生成！
- 默认 `MaxJumpTableSize` 为 65000 ,case的数量大于这个不会生成！
- 默认 `MaxJumpTableSparseness` 为 5 ，case过于悉数不会生成！

## 结论
### 我们用一下午证明了以下几点
- Talk is cheap. Show me the code
- Talk is cheap. Show me the code
- Talk is cheap. Show me the code

#### 环境信息
- Mac OS
- JDK 1.8.0_151
