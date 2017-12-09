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

使用hashcode的目的在于：使用一个对象查找另一个对象。对于使用散列的数据结构，如`HashSet、HashMap、LinkedHashSet、LinkedHashMap`，如果没有很好的覆写键的hashcode()和equals()方法，那么将无法正确的处理键。

请对以下代码中`Person`覆写hashcode()方法，看看会发生什么？

```java
// 覆写hashcode
@Override
public int hashCode() {
    return age;
}

@Test
public void testHashCode() {
    Set<Person> people = new HashSet<Person>();
    Person person = null;
    for (int i = 0; i < 3 ; i++) {
        person = new Person("name-" + i, i);
        people.add(person);
    }
    person.age = 100;
    System.out.println(people.contains(person));
    people.add(person);
    System.out.println(people.size());
}
```

运行结果并不是预期的`true`和`3`，而是`false`和`4`！改变`person.age`后HashSet无法找到`person`这个对象了，可见覆写hahcode对HashSet的存储和查询造成了影响。

那么hashcode是如何影响HashSet的存储和查询呢？又会造成怎样的影响呢？

HashSet的内部使用HashMap实现，所有放入HashSet中的集合元素都会转为HashMap的key来保存。
HashMap使用散列表来存储，也就是数组+链表+红黑树（JDK1.8增加了红黑树部分）。
存储结构简图如下：

![](https://raw.githubusercontent.com/nanfangstation/image/master/blog/2017-12/hashmap存储结构.jpg)

数组的默认长度为16，数组里每个元素存储的是一个链表的头结点。组成链表的结点结构如下：

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
    ...
}
```

每一个Node都保存了一个hash----键对象的hashcode，如果键没有按照任何特定顺序保存，查找时通过equals()逐一与每一个数组元素进行比较，那么时间复杂度为O(n)，数组长度越大，效率越低。

所以瓶颈在于键的查询速度，如何通过键来快速的定位到存储位置呢？

HashMap将键的hash值与数组下标建立映射，通过键对象的hash函数生成一个值，以此作为数组的下标，这样我们就可以通过键来快速的定位到存储位置了。如果hash函数设计的完美的话，数组的每个位置只有较少的值，那么在O(1)的时间我们就可以找到需要的元素，从而不需要去遍历链表。这样就大大提高了查询速度。

那么HashMap根据hashcode是如何得到数组下标呢？可以拆分为以下几步：

* 第一步：`h = key.hashCode()`
* 第二步：`h ^ (h >>> 16)` 
* 第三步：`(length - 1) & hash` 

**分析**

第一步是得到key的hashcode值；第二步是将键的hashcode的高16位异或低16位(高位运算)，这样即使数组table的length比较小的时候，也能保证高低Bit都参与到Hash的计算中，同时不会有太大的开销；第三步是hash值和数组长度进行取模运算，这样元素的分布相对来说比较均匀。
当length总是2的n次方时，`h & (length-1)`运算等价于对length取模，这样模运算转化为位移运算速度更快。

但是，HashMap默认数组初始化容量大小为16。当数组长度远小于键的数量时，不同的键可能会产生相同的数组下标，也就是发生了哈希冲突！

对于哈希冲突有开放定址法、链地址法、公共溢出区法等解决方案。

开放定址法就是一旦发生冲突，就寻找下一个空的散列地址。过程可用下式描述：

`f<sub>i</sub>(key) = (f(key) + d<sub>i</sub>) mod m (d<sub>i</sub>=1,2,3,...,m-1)`

例如键集合为`{12,67,56,16,25,37,22,29,15,47,48,34}`，表长`n = 12`，取`f(key) = key mod 12`。

前5个计算都没有冲突，直接存入。如表所示

|数组下标|0|1|2|3|4|5|6|7|8|9|10|11|
|--|--|--|--|--|--|--|--|--|--|--|--|--|
|键|12|25|||16|||67|56||||

当`key = 37`时，`f(37) = 1`，与25的位置冲突。应用公式`f(37) = (f(37) + 1) mod 12 = 2`，所以37存入数组下标为2的位置。如表所示

|数组下标|0|1|2|3|4|5|6|7|8|9|10|11|
|--|--|--|--|--|--|--|--|--|--|--|--|--|
|键|12|25|37||16|||67|56||||

到了`key = 48`，与12所在的0冲突了。继续往下找，发现一直到`f(48) = (f(48) + 6) mod 12 = 6`时才有空位。如表所示

|数组下标|0|1|2|3|4|5|6|7|8|9|10|11|
|--|--|--|--|--|--|--|--|--|--|--|--|--|
|键|12|25|37||16|29|48|67|56||22|47|

所以在解决冲突的时候还会出现48和37冲突的情况，也就是出现了**堆积**，无论是查找还是存入效率大大降低。

链地址法解决冲突的做法是：如果哈希表空间为`[0～m-1]`，设置一个由m个指针分量组成的一维数组`Array[m]`, 凡哈希地址为i的数据元素都插入到头指针为`Array[i]`的链表中。

它的基本思想是：为每个Hash值建立一个单链表，当发生冲突时，将记录插入到链表中。如图所示：

![](https://raw.githubusercontent.com/nanfangstation/image/master/blog/2017-12/链地址法.jpg)

链表的好处表现在：

1. remove操作时效率高，只维护指针的变化即可，无需进行移位操作
2. 重新散列时，原来散落在同一个槽中的元素可能会被散落在不同的地方，对于数组需要进行移位操作，而链表只需维护指针。
但是，这也带来了需要遍历单链表的性能损耗。

公共溢出法就是我们为所有冲突的键单独放一个公共的溢出区存放。
例如前面例子中`{37,48,34}`有冲突，将他们存入溢出表。如图所示。

![](https://raw.githubusercontent.com/nanfangstation/image/master/blog/2017-12/公共溢出区.jpg)

在查找时，先与基本表进行比对，如果相等则查找成功，如果不等则在溢出表中进行顺序查找。公共溢出法适用于冲突数据很少的情况。

HashMap解决冲突采取的是链地址法。整体流程图如下：

![](https://raw.githubusercontent.com/nanfangstation/image/master/blog/2017-12/hashmap存储流程图.jpg)

理解了hashcode和哈希冲突即解决方案后，我们如何设计自己的hashcode()
方法呢？

Effective Java一书中对覆写hashcode()给出以下指导:

* 给int变量result赋予某个非零常量值

* 为对象内每个有意义的域f计算一个int散列码c

|域类型|计算|
|--|--|
|boolean|`c = (f ? 0 : 1)`|
|byte、char、short、int|`c = (int)f`|
|long|`c = (int)(f ^ (f >>> 32))`|
|float|`c = Float.floatToIntBits(f)`|
|double|`long l = Double.doubleToIntLongBits(f)`|
||`c = (int)(l ^ (l >>> 32))`|
|Object|`c = f.hashcode()`|
|数组|`每个元素应用上述规则`|
|boolean|`c = (f ? 0 : 1)`|
|boolean|`c = (f ? 0 : 1)`|

* 合并计算得到散列码 `result = 37 * result + c` 

现代IDE通过点击右键上下文菜单可以自动生成hashcode方法，比如通过IDEA生成的hashcode如下：

```java
@Override
public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + age;
    return result;
}
```

但是在企业级代码中，最好使用第三方库如`Apache commons`来生成hashocde方法。使用第三方库的优势是可以反复验证尝试代码。下面代码显示了如何使用`Apache Commons hash code` 为一个自定义类构建生成hashcode。

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

## 总结

通过上述分析，我们设计hashcode()应该注意的是：

* 无论何时，对同一个对象调用hashcode()都应该生成同样的值。
* hashcode()尽量使用对象内有意义的识别信息。
* 好的hashcode()应该产生分布均匀的散列值。
