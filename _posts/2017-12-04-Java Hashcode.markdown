---
layout:     post
title:      "你所不知道的Java之HashCode"
subtitle:   ""
date:       2017-12-04
author:     "Lydia"
header-img: "img/post-bg-room.jpg"
catalog: true
tags:
    - java
---

## 覆写对象的hashcode方法，会发生什么？

请看一段代码
```java
@Test
public void testHashCode() {
   Set<Person> people = new HashSet<Person>();
   Person person = null;
   for (int i = 0; i < 3; i++) {
       person = new Person("Lydia-" + i, i);
       people.add(person);
   }
   person.age = 100;
   System.out.println(people.contains(person));
   people.add(person);
   System.out.println(people.size());
}
```
覆写hashcode，会得到什么结果？
```java
@Override
public int hashCode() {
   final int prime = 31;
   int result = 1;
   result = prime * result + age;
   result = prime * result + ((name == null) ? 0 : name.hashCode());
   return result;
}
```
运行结果：
```java
false
11
```
为什么只覆写hashcode方法不改变equals()方法，Set判断person不存在，并新增了该对象？

hashcode是怎样影响Set判断一个元素是否存在的呢？

我们知道，HashSet其内部是使用HashMap实现，所有放入HashSet中的集合元素实际上由HashMap的key来保存。而HashMap使用哈希表来存储，也就是数组+链表+红黑树（JDK1.8增加了红黑树部分），存储结构入下图所示：
![](/Users/nanfang/Desktop/hashmap存储结构.png)

我们先贴下HashSet关键代码
```java
public boolean contains(Object o) {
return map.containsKey(o);
}
public boolean containsKey(Object key) {
return getNode(hash(key), key) != null;
}
static final int hash(Object key) {
int h;
return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```
我们看到，在获取节点时用到了key的hash值。

那么hash值是如何计算，然后定位元素在HashMap中的存放位置的呢？

通过分析源码，得出流程图如下（暂不考虑HashMap扩容）：

![](/Users/nanfang/Desktop/hashmap存储过程.png)

那么，如何根据hashcode得到数组下标呢？

源码总结为以下三步：

* 第一步：h = key.hashCode() ** 获取key的原始hashCode值 **
* 第二步：h ^ (h >>> 16)  ** 高位参与运算 **
* 第三步：(length - 1) & hash  ** 取模得到数组下标 **

分析：
得到key的hashcode后，高16bit不变，低16bit和高16bit做了一个异或。

为什么是无符号右移16位？

我们看下hashcode的原始定义。
> public native int hashCode();

因为Object定义的默认返回一个对象的hash值是int类型--32位有符号整数，如果直接拿key的原始hash值作为下标访问HashMap数组，数值范围-2<sup>32</sup>~2<sup>32</sup>-1，只要hash值映射的比较均匀松散，很难发生哈希碰撞。但是问题是，这么大的数组，实际没有这么大的内存，HashMap扩容前默认初始容量是16。所以我们还需要利用hash函数对key的原始hashcode进行处理。

HashMap考虑了除留余数法。
当容量是2<sup>n</sup>时，h % length等价于
h & (length - 1)。（length - 1）正好相当于一个“低位掩码”，&操作的结果是高位全部归零，只保留低位值。如果以低位值作为数组下标来访问，那么哈希碰撞就会很严重。

所以HashMap设计hash函数时加了高位运算。
>h ^ (h >>> 16)高位

让自己的高半区和低半区做异或，扰乱原始hashcode的高低位，以此加大了低位的随机性，减少哈希碰撞。

## 哈希冲突

但是现实中的哈希函数不是完美的，如果两个key定位到了相同的位置，就会发生Hash碰撞，这个时候便需要解决冲突。

常见的冲突解决方法有开放定址法、链地址法等。

### 1. 开放定址法
当发生地址冲突时，按照某种方法继续探测哈希表中的其他存储单元，直到找到空位置为止。这个过程可用下式描述：

** f<sub>i</sub>(key) = (f(key) + d<sub>i</sub>) mod m (d<sub>i</sub>=1,2,3,...,m-1) **

不过需要注意的是，对于利用开放地址法处理冲突所产生的哈希表中删除一个元素时需要谨慎，不能直接地删除，因为这样将会截断其他具有相同哈希地址的元素的查找地址，所以通常采用设定一个特殊的标志以示该元素已被删除。 

因为开放寻址使用连续的空间，当数据量较小，能够放到系统缓存中时，开放寻址会表现出比链表更好的性能。 但是当数据量增长时，它的性能就会越差。 

### 2.链地址法

链表的好处表现在：

1. remove操作时效率高，只维护指针的变化即可，无需进行移位操作
2. 重新散列时，原来散落在同一个槽中的元素可能会被散落在不同的地方，对于数组需要进行移位操作，而链表只需维护指针。

Java中HashMap采用了链地址法：如果哈希表空间为[0～m-1]，设置一个由m个指针分量组成的一维数组Array[m], 凡哈希地址为i的数据元素都插入到头指针为Array[i]的链表中。当发生冲突时，那么在同一个位子上的元素将以链表的形式存放，新加入的放在链头，最先加入的放在链尾。

通过分析，hashcode在HashSet的存储和查找扮演着至关重要的角色。

所以对于HashMap等类，要求映射中的key是不可变对象。不可变对象是该对象在创建后它的哈希值不会被改变。如果对象的哈希值发生变化，集合很可能就定位不到映射的位置了。

此时就有危险了。这种情况下，通过contains方法检索该对象时，将返回false。这将导致无法从HashSet集合中删除对象，从而造成内存泄露。

## String中的hash

如果Set其他类型是什么结果呢？

以String为例

```java
Set<String> stringSet = new HashSet<String>();
stringSet.add(new String("Lydia"));
stringSet.add(new String("Lydia"));
stringSet.add("Lydia");
System.out.println(stringSet.size());
```
输出结果：
```
1
```
发现无论是new一个String对象或者构造一个匿名对象，如果内容相同，Set都认为他们的hash值相等，是同一个元素。

我们看下String内部是如何实现hashcode()。
```java
public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        char val[] = value;

        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];
        }
        hash = h;
    }
    return h;
}
```
经过推导得到哈希函数：

** f(key)=s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]； **

为什么要用31作为基数呢？我们知道使用素数可以减少哈希碰撞，乘数是一个相对较大的素数可以理解。
那么为什么不是29，或37，甚至97？

Joshua Bloch的Effective Java写道：

>The value 31 was chosen because it is an odd prime. If it were even and the multiplication overflowed, information would be lost, as multiplication by 2 is equivalent to shifting. The advantage of using a prime is less clear, but it is traditional. A nice property of 31 is that the multiplication can be replaced by a shift and a subtraction for better performance: 31 * i == (i << 5) - i. Modern VMs do this sort of optimization automatically.

因为31是一个素数（指在一个大于1的自然数中，除了1和此整数自身外，没法被其他自然数整除的数）；根据素数的特性，与素数相乘得到的结果比其他方式更容易产生唯一性，也就是说产生hash值重复的概率比较小。

素数有很多，为什么是31呢？

31的二进制为：11111，占用5个二进制位，在进行乘法运算时，可以转换为(x << 5) - 1。除了位移运算更快，还需要考虑乘法的因素，在Java中如果相乘的数字太大会导致内存溢出问题，从而导致数据丢失。

## 如何重写hashcode()

但是如果想完整的使用HashSet<E>类，需要我们重写equals()和hashCode()方法，保证内容一样的对象计算出相同的hash值。

那么哈希函数如何设计呢？

应该尽量满足
>1. 计算简单
>2. 散列地址分布均匀

哈希函数的构造方法：

* 直接定址法

取关键字的线性函数作为散列地址。
例如 Hash(key) = A*key+B，其中，A和B都为常数
* 折叠法

将关键字分割成位数相同的几部分，最后一部分位数可以不同，然后取这几部分的叠加和（去除进位）作为散列地址。数位叠加可以有移位叠加和间界叠加两种方法。 
假如知道图书的ISBN号为8903-241-23，可以将address(key)=89+03+24+12+3作为Hash地址。
* 数字分析法

找出数字的规律，比如抽取使用关键字的一部分，尽可能利用这些数据来构造冲突几率较低的散列地址。
* 除留余数法

如果知道Hash表的最大长度为m，可以取不大于m的最大质数p，然后对关键字进行取余运算。
f(key)=key mod p(p<=m)
在这里p的选取非常关键，p选择的好的话，能够最大程度地减少冲突，p一般取不大于m的最大质数。
* 平方取中法

取关键字平方后的中间几位为哈希地址。由于一个数的平方的中间几位与这个数的每一位都有关，因而，平方取中法产生冲突的机会相对较小。平方取中法中所取的位数由表长决定。
例： 假如有以下关键字序列{421，423，436}，平方之后的结果为{177241，178929，190096}，那么可以取{72，89，00}作为Hash地址。
* 随机数法

选择一随机函数，取关键字的随机值作为散列地址，通常用于关键字长度不同的场合。

## 总结

如果我们要覆写hashcode()，一定要保证内容一样的对象计算出相等的hash值。

hash值应该尽可能的分布均匀，减少哈希碰撞。

然后牢记在java的集合中，判断两个对象是否相等的规则是：
> 判断两个对象的hashCode是否相等

* 如果不相等，认为两个对象也不相等，完毕
* 如果相等，转入equals判断

> 判断两个对象用equals运算是否相等

* 如果不相等，认为两个对象也不相等
* 如果相等，认为两个对象相等

## 后记

|类|hash|
|--|--|
|Byte|(int)value|
|Short|(int)value|
|Integer|int value|
|Long|(int)(value ^ (value >>> 32))|
|Boolean|value ? 1231 : 1237|
