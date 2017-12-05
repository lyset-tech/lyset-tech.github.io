---
layout:     post
title:      "你所不知道的Java之HashCode"
subtitle:   "如何正确使用hashcode?"
date:       2017-12-04
author:     "Lydia"
header-img: "img/post-bg-hashcode.jpg"
catalog: true
tags:
    - java
---

>之所以写HashCode，是因为平时我们总听到它。但你真的了解hashcode吗？它会在哪里使用？它应该怎样写？

>相信阅读完本文，能让你看到不一样的hashcode。

### 为什么需要hashcode?

在 `Java` 中，每一个对象都有一个容易理解但有时候仍然被遗忘或者被误用的 `hashCode` 方法。

>public native int hashcode();

Object类定义hashCode()方法返回一个对象的hash值（一个32位有符号整数）。

官方解释如下：
>This method is supported for the benefit of hash tables such as those provided by { `java.util.HashMap` }

>该方法是为哈希表提供一些优化，例如，`java.util.HashMap` 提供的哈希表。 

那么hashcode是如何优化HashMap的呢？我们先从下面一段代码开始。

```java
HashSet<Person> set = new HashSet<Person>();
System.out.println(one.equals(two));
set.add(new Person("Lydia", 24));
set.add(new Person("Lydia", 24));
System.out.println(set.size());
System.out.println(set.contains(new Person("Lydia", 24)));
```

我们知道，Set是元素不重复的。但是输出结果如下：

```
2
false
```

为何先后 `new Person("Lydia", 24)` ，contains方法判断它们不重复并且都成功添加了？

Set判断两个元素是否重复又是依据什么呢？ 

我们知道Object的equals()方法直接比较内存地址，上述代码先后 `new Person("Lydia", 24)`，构造了两个内容一样的对象，但是它们内存地址不一样，所以Set判断他们不是重复的。

但是这样如何保证Set的元素不重复呢？我们覆写equals()方法试试，代码如下：

```java
@Override
public boolean equals(Object o) {
    if (o != null && o.getClass() == this.getClass()) {
        Person p = (Person) o;
        if (p.getName() == null && this.name == null)
            return false;
        return this.name.equals(p.getName());
    }
    return false;
}
```

但是结果并不是我们期待的1和true，而是：

```
2
false
```

通过覆写equals()方法使每次 `new Person("Lydia", 24)` 通过equals比较的结果相等。但是我们发现Set还是添加了内容相同的元素，也就是只覆写equals()还是不够。

### 如果覆写对象的hashcode方法会发生什么呢？

再次尝试以下代码：

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

输出结果：

```
1
true
```

通过覆写hashcode方法真的实现了元素不重复，那么hashcode是如何影响Set判断一个元素重复的呢？

我们知道，HashSet继承了Set接口，HashSet其内部是使用HashMap实现，所有放入HashSet中的集合元素实际上由HashMap的key来保存。而HashMap使用哈希表来存储，也就是数组+链表+红黑树（JDK1.8增加了红黑树部分），存储结构入下图所示：

![](https://raw.githubusercontent.com/nanfangstation/image/master/blog/2017-12/hashmap存储结构.png)

对比存储结构，我们看下HashSet的关键代码。

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

根据hashcode是如何得到数组下标呢？分为以下几步：

* 第一步：`h = key.hashCode()`
* 第二步：`h ^ (h >>> 16)` 
* 第三步：`(length - 1) & hash` 

**分析**
第一步中，我们获取key的原始hashCode值。如果我们覆写了hashcode，那么将影响到key的hash值以及数组下标的计算，以致于影响到整个HashMap的查找和存储。所以一个好的哈希函数至关重要。

### 如何构造好的哈希函数？

如何构造好的哈希函数？将对象所有属性都组装一下就行吗？

显然不是。

《大话数据结构》中提到五种构造方法：

>**直接定址法**
取关键字的线性函数作为散列地址。
例如 Hash(key) = A*key+B，其中，A和B都为常数

>**折叠法**
将关键字分割成位数相同的几部分，最后一部分位数可以不同，然后取这几部分的叠加和（去除进位）作为散列地址。数位叠加可以有移位叠加和间界叠加两种方法。 
假如知道图书的ISBN号为8903-241-23，可以将address(key)=89+03+24+12+3作为Hash地址。

>**数字分析法**
找出数字的规律，比如抽取使用关键字的一部分，尽可能利用这些数据来构造冲突几率较低的散列地址。

>**除留余数法**
如果知道Hash表的最大长度为m，可以取不大于m的最大质数p，然后对关键字进行取余运算。
f(key)=key mod p(p<=m)
在这里p的选取非常关键，p选择的好的话，能够最大程度地减少冲突，p一般取不大于m的最大质数。

>**平方取中法**
取关键字平方后的中间几位为哈希地址。由于一个数的平方的中间几位与这个数的每一位都有关，因而，平方取中法产生冲突的机会相对较小。平方取中法中所取的位数由表长决定。
例： 假如有以下关键字序列{421，423，436}，平方之后的结果为{177241，178929，190096}，那么可以取{72，89，00}作为Hash地址。

>**随机数法**
>选择一随机函数，取关键字的随机值作为散列地址，通常用于关键字长度不同的场合。

通过直接定址法，我尝试将Person的name和age属性组合，覆写hashcode代码如下：

```java
@Override
public int hashCode() {
    int result = name != null ? name.length() : 0;
    result = result + age;
    return result;
}
```

测试代码如下：

```java
HashSet<Person> set = new HashSet<Person>();
Person one = new Person("Lydia", 24);
Person two = new Person("Feiniao", 22);
Person three = new Person("Juexing", 28);
set.add(one);
set.add(two);
set.add(three);
System.out.println("one's hashcode is: " + one.hashCode());
System.out.println("two's hashcode is: " + two.hashCode());
System.out.println("three's hashcode is: " + three.hashCode());
System.out.println("one == two??: " + one.equals(two));
System.out.println("set.size: " + set.size());
```

输出结果：

```
one's hashcode is: 29
two's hashcode is: 29
three's hashcode is: 35
one == two??: false
set.size: 3
```

可以看到，通过equals比较两个不相等的对象竟然有了相同的hashcode！

### 如何解决哈希碰撞？

我们已经知道HashMap的查询和存储和hashcode息息相关，如果出现两个equals不等的key有了相同的hashcode，定位到了相同的位置，就会发生Hash碰撞!

这个时候便需要解决冲突，常见的冲突解决方法有开放定址法，链地址法等。

**开放地址法**

当发生地址冲突时，按照某种方法继续探测哈希表中的其他存储单元，直到找到空位置为止。这个过程可用下式描述：

`f<sub>i</sub>(key) = (f(key) + d<sub>i</sub>) mod m (d<sub>i</sub>=1,2,3,...,m-1)`

不过需要注意的是，对于利用开放地址法处理冲突所产生的哈希表中删除一个元素时需要谨慎，不能直接地删除，因为这样将会截断其他具有相同哈希地址的元素的查找地址，所以，通常采用设定一个特殊的标志以示该元素已被删除。 

因为开放寻址使用连续的空间，当数据量较小，能够放到系统缓存中时，开放寻址会表现出比链表更好的性能。 但是当数据量增长时，它的性能就会越差。 

**链地址法**

链表的好处表现在：
1. remove操作时效率高，只维护指针的变化即可，无需进行移位操作
2. 重新散列时，原来散落在同一个槽中的元素可能会被散落在不同的地方，对于数组需要进行移位操作，而链表只需维护指针。

链地址法解决冲突的做法是：如果哈希表空间为`[0～m-1]`，设置一个由m个指针分量组成的一维数组`Array[m]`, 凡哈希地址为i的数据元素都插入到头指针为`Array[i]`的链表中。

它的基本思想是：为每个Hash值建立一个单链表，当发生冲突时，将记录插入到链表中。

Java中HashMap采用了链地址法。通过以下的存储流程图进行说明。

![](https://raw.githubusercontent.com/nanfangstation/image/master/blog/2017-12/hashmap存储过程.png)

当我们往HashMap中put元素的时候，先根据key的hash值得到这个元素在数组中的位置（即下标），然后就可以把这个元素放到对应的位置中了。

如果这个元素所在的位子上已经存放有其他元素了，那么在同一个位子上的元素将以链表的形式存放，新加入的放在链头，最先加入的放在链尾。

从HashMap中get元素时，首先计算key的hashcode，找到数组中对应位置的某一元素，然后通过key的equals方法在对应位置的链表中找到需要的元素。

如果每个数组项中都只有一个元素，则在O(1)的时间就可以得到需要的元素，从而不需要去遍历链表，比时间复杂度为O(n)的equals方法相比确实提高了效率。

### 你真的还会写hashcode吗？

那么如何才能写出计算简单且散列地址分布均匀，尽量避免哈希碰撞的hash函数呢？

我们再回归到HashMap，研究HashMap设计哈希函数的方法，可以获得一些思考。

* 第一步：`h = key.hashCode()` 
* 第二步：`h ^ (h >>> 16)`
* 第三步：`(length - 1) & hash`

试想，如果直接拿key的原始hash值（int类型--32位有符号整数）作为下标访问HashMap数组，数值范围-2<sup>32</sup>~2<sup>32</sup>-1，只要hash值映射的比较均匀松散，那么很难发生哈希碰撞。但是问题是，这么大的数组我们没有对应这么大的内存可以存储，HashMap扩容前默认初始容量是16。所以我们还需要利用hash函数对key的原始hashcode进一步处理。

HashMap采用了除留余数法。当容量是2<sup>n</sup>时，`h % length`等价于
`h & (length - 1)`。`（length - 1）`正好相当于一个“低位掩码”，进行`&`操作的结果是高位全部归零，只保留低位值。如果以低位值作为数组下标直接来访问，那么哈希碰撞就会很严重。

所以HashMap在第二步加了高位运算。让自己的高半区和低半区做异或，扰乱原始hashcode的高低位，以此加大了低位的随机性，减少哈希碰撞。

### 如何设计一个合适的hash函数？

一个差劲的hashcode算法不仅会降低基于哈希集合的性能，而且会导致异常结果。Java应用中有多种不同的方式来生成hashcode。

现代IDE通过点击右键上下文菜单可以自动生成hashcode方法，比如通过IDEA生成的hashcode如下：

```java
@Override
public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + age;
    return result;
}
```

但是在企业级代码中，最好使用第三方库如`Apache commons`来生成hashocde方法。

我们可以用`Apache Commons hashcode builder`来生成代码，使用这样一个第三方库的优势是可以反复验证尝试代码。下面代码显示了如何使用`Apache Commons hash code` 为一个自定义类构建生成hashcode 。

```java
public int hashCode(){
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(mostSignificantMemberVariable);
    ........................
    builder.append(leastSignificantMemberVariable);
    return builder.toHashCode();
}
```

如代码所示，最重要的签名成员变量应该首先传递然后跟随的是没那么重要的成员变量。

### 什么情况下需要覆写hashcode？

那么是不是所有的情况下都要覆写hashcode呢？

当然不是。

String的hash函数如下：

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

`f(key)=s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]；`

String的hashcode的算法就充分利用了字符串内部字符数组的所有字符。

选择31这个素数作为基数，比其他方式更容易产生唯一性，也就是说产生hash值重复的概率比较小；31的二进制表示为：11111，占用5个二进制位，在进行乘法运算时，可以转换为 `(x << 5) - 1`进行位运算；相乘的数字不会太大，相对不会导致内存溢出从而导致数据丢失。

那么什么情况下需要覆写hashcode呢？

* 覆写equals方法同时覆写hashcode
* 集合需要保证元素不可重复时需要覆写hashcode

## 总结

覆写hashcode注意事项：

* 尽量利用对象的属性，相同内容的对象生成相同的hash值
* hash值尽量散列均匀以减少哈希碰撞，提高查询和存储效率

牢记在java的集合中，判断两个对象是否相等的规则是：
> 判断两个对象的hashCode是否相等

* 如果不相等，认为两个对象也不相等，完毕
* 如果相等，转入equals判断

> 判断两个对象用equals运算是否相等

* 如果不相等，认为两个对象也不相等
* 如果相等，认为两个对象相等
