package com.whl.collection.map;

/**
 * @author whl
 * @version V1.0
 * @Title: JDK1.8 - HashMap再实现
 * @Description:
 *
 * 在这个HashMap中, 我们仅仅实现了基本的功能, 不包含树化的相关操作, 因为个人对于红黑树的理解有限
 * 并且涉及到迭代器相关的操作, 例如keySet、valueSet等, 都没有进行相应的实现
 *
 * 除此以外, 对一些后置处理操作, 例如 afterNodeAccess(e)、afterNodeInsertion(e)... 这类方法, 实际上HashMap对这些操作都是通过空方法实现的, 目的是为了方便子类LinkedHashMap继承
 *
 * 除此以外, 如果仔细阅读了源码, 你能够发现, HashMap的很多方法中的本地变量赋值操作都是写在 if 语句块中的, 而不是在声明时就赋值
 * 这样不仅仅是为了减少代码量, 也是能有效避免空指针的写法
 * 就比如这个语句：if ((tab = table) != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null)
 * 在table获取失败后, 后序的赋值动作就不会执行, 成功避免了出现空指针异常的问题
 *
 */
public class MyHashMap<K,V> implements MyMap<K,V> {
    //默认容器的长度为16, 且要求必须为2的n次方
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    //HashMap的最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;

    //扩容因子, 当容器内部的元素个数占容量的百分之75时, 触发扩容操作
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    //链表长度到达8时, 进行树化操作
//    static final int TREEIFY_THRESHOLD = 8;

    //链表长度退化到6时, 触发由树转换为链表的操作
//    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 可以触发树化的最小容量
     *
     * 但一般当容器中的元素个数过多时, 我们有可能会触发扩容操作
     *
     * 那么为了避免进行扩容、树形化选择的冲突, 这个值不能小于 4 * TREEIFY_THRESHOLD
     */
//    static final int MIN_TREEIFY_CAPACITY = 64;

    MyNode<K,V>[] table;

    private int size;//map中的元素数量

    int threshold;//阈值, HashMap中能容纳的最大键值对数量, 当超过threshold时会触发扩容, threshold = (capacity * load factor).

    final float loadFactor;// 扩容因子

    /**
     * Map中的键值对结点
     *
     * 原生JDK还实现了equals、重写HashCode方法操作, 这里为了关注核心逻辑, 我省略了这两个方法
     */
    static class MyNode<K,V> implements Entry<K,V> {
        final int hash;
        final K key;
        V value;
        MyNode<K,V> next;

        MyNode(int hash, K key, V value, MyNode<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
    }

    /**
     * 获取到key对象的hash值, 该hash值将用于计算数组中的下标
     *
     * 那么为什么JDK中要让key的hashCode对无符号右移16位的hashCode进行异或运算呢？
     * 这个答案要从计算下标的hash算法说起：
     *      HashMap的hash算法为：index = (n - 1) & hash
     *
     *      当两个key的hashCode(假设只有8位, 原本是32位)只有高4位不同, 低4位相同时, 比如keyA.hashCode() = 01110101; keyB.hashCode() = 01010101
     *      可以发现, 如果不对上面这两个hashCode进行处理, 那么通过hash运算得到的index是极有可能产生hash碰撞的 (因为只有相同的低位参与了运算, 不同的高位没有参与运算)
     *
     *      为此, 我们将keyA.hashCode()=01110101与无符号右移4位的00000111进行异或运算, 得到01110010
     *                 keyB.hashCode()=01010101与无符号右移4位的00000101进行异或运算, 得到01010000
     *      这样, 就能够让两个hashCode()的高位与低位都能够参与到hash运算中, 使得计算得到的index更加散列
     *
     * @param key
     * @return
     */
    static final int hash(Object key) {
        int h;
        return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 指定初始容量以及扩容因子创建HashMap
     *
     * 这里主要会进行一些参数校验
     *      1. 要求初始容量不小于0, 否则抛出异常
     *      2. 要求初始容量不能超过最大Map容量
     *      3. 要求负载因子值合法
     *
     * 需要注意的是, HashMap在这里就将initialCapacity转换为了向上取最近的二进制位。
     * 该方法只完成了一些全局变量的赋值动作, 并没有实际初始化容器
     * @param initialCapacity
     * @param loadFactor
     */
    public MyHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * 获取到cap值向上取最近的2进制位, 比如 tableSizeFor(25) -> 32
     *
     * 该算法的思想就是将Capacity的有效二进制位转换为全1, 然后加1取到二进制位
     * 例如(14)2 = 1100, 1100低位全部转换为1, 1100 -> 1111, 1111 + 1 = 100000
     * @param cap
     * @return
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;// cap - 1是为了避免一个二进制数被转换为更大的二进制数
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * 指定初始容量构建HashMap
     */
    public MyHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 创建一个空的HashMap, 采用默认初始化容量16, 以及默认负载因子0.75
     */
    public MyHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Map是否存在包含传入key值的键值对
     *
     * 这里直接看能否按照key获取到指定Node即可
     * @param key
     * @return
     */
    @Override
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     * Map中是否存在包含传入value的键值对
     *
     * 这个方法逻辑简单来说就是：遍历桶数组, 从每个桶的头结点开始遍历链表
     * 如果结点的value = 传入的value值, 那么return true
     * @param value
     * @return
     */
    @Override
    public boolean containsValue(Object value) {
        MyNode<K,V>[] tab;
        V v;
        if ((tab = table) != null && size > 0) {
            for (int i = 0; i < tab.length; ++i) {
                for (MyNode<K,V> e = tab[i]; e != null; e = e.next) {
                    //这里会比较两个value的内存地址 或 通过equals进行比较
                    if ((v = e.value) == value || (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * huo
     * @param key
     * @return
     */
    public V get(Object key) {
        MyHashMap.MyNode<K,V> e = getNode(hash(key), key);
        return e == null ? null : e.value;
    }

    /**
     * 获取到传入的key, hash在HashMap中对应的键值对
     *
     * @param hash
     * @param key
     * @return
     */
    final MyNode<K,V> getNode(int hash, Object key) {
        MyNode<K,V>[] tab;//table数组引用
        MyNode<K,V> e;//指向桶中链表Node的指针
        int n;//table数组长度
        MyNode<K,V> first;//桶上的头结点
        K k;//指向桶中链表Node的key的指针

        //下面这个if语句块会先保证, 桶数组table我们能够成功获取到, 然后通过hash值计算得到的索引也能成功获取到, 然后才是检索逻辑
        if ((tab = table) != null && (n = table.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
            //下面会先对头结点进行判断, 如果头结点就是我们寻找的值, 那么直接返回
            if (first.hash == hash && // 第一步就检查hash值是否相同, 不相同直接退出判断
                    ((k = first.key) == key || (key != null && key.equals(k))))//如果头结点的key是传入key 或者 传入key与k指向结点的key相同, 那么就返回这个结点
                return first;
            //如果头结点没有找到, 那么我们就需要遍历整个链表来寻找
            //原生JDK中, 这里会对红黑树的结点进行特殊的遍历操作, 以获取到target Node
            //这也是为什么我们要将头结点与后续所有结点的判断分开来做, 目的是为了兼容链表与红黑树的遍历
            if ((e = first.next) != null) { // 如果头结点的next结点不为空, 那么将值赋给引用e
                do { // 下面做的就是遍历链表, 寻找到Node.key = 传入key的结点, 直到遍历完整个链表
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) // 与之前相同的判断
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    /**
     * 如果key在Map中存在, 那么更新value; 否则新建一个Node放入Map中
     *
     * 注意这个方法的onlyIfAbsent参数为false, 这就代表会覆盖已经存在的value值
     * @param key
     * @param value
     * @return
     */
    @Override
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * 放置key, value键值对的核心逻辑
     * @param hash
     * @param key
     * @param value
     * @param onlyIfAbsent 如果为true, 那么就不修改已经存在的value值
     * @param evict 如果为false, 说明这个Map处于构建中
     * @return
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        MyNode<K,V>[] tab;
        MyNode<K,V> p;
        int n, i;
        //首先我们确保table能够成功获取到, 如果是第一次执行putVal, 那么会进行初始化table的工作
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        //根据hash值计算下标, 获取到对应的桶, 如果这个桶为空, 那么直接新建一个键值对放进去即可
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = new MyNode(hash, key, value, null);//原生JDK中将新建Node的操作封装到了一个方法中
        else { // 如果这个桶不为空, 那么我们要遍历这个桶, 如果遍历途中有key相同的Node, 那么更新value; 否则在链表末尾追加新的Node
            MyNode<K,V> e;
            K k;
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) { // 如果桶链表的头结点key值与传入的key值相同, 那么先将桶链表头结点的地址赋给指针 e, 会在后续进行value的更新动作
                e = p;
            }
            //这里省略了桶上头结点是TreeNode的逻辑, 这里会按照TreeNode的方式put键值对
//            else if (p instanceof TreeNode)
//                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                //省略掉树化的相关逻辑, 具体请参照JDK1.8的putVal
                for (;;) {
                    if ((e = p.next) == null) { // 遍历到链表末尾时, 追加新的键值对Node
                        p.next = new MyNode<>(hash, key, value, null); // 注意这里 e 是为null的, 我们是通过指针p.next追加新结点, 目的是为了后面的value覆盖逻辑能够成功执行
                        break;
                    }
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) // 若遍历到一个key相同的Node, 那么直接break
                        break;// 此时 e 指向key相同的那个结点
                    p = e;
                }
            }
            //这里会执行value值的更新操作
            if (e != null) {
                V oldValue = e.value;// 获取到Node的value
                if (!onlyIfAbsent || oldValue == null) // 当 onlyIfAbsent 这个参数为false时, 表示无论value是否为空, 都会对value覆盖
                    e.value = value;
                afterNodeAccess(e);// 在成功覆盖之后, 需要进行一些特殊处理, 这里是空方法
                return oldValue;// 成功覆盖之后返回旧值
            }
        }
        //如果是追加Node的操作, 那么我们还需要在追加成功后判断是否大于阈值, 如果超过了还需要执行扩容操作
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);//成功插入之后, 需要执行什么后续操作
        return null;// 追加操作返回null
    }

    /**
     * 初始化桶数组, 或者扩容桶数组
     *
     * 如果桶数组为空, 则根据阈值中的值分配初始容量
     * 若桶数组不为空, 则进行扩容操作
     *
     * @return the table
     */
    final MyNode<K,V>[] resize() {
        MyNode<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length; // 获取到旧的table容量
        int oldThr = threshold; // 获取到旧的阈值
        int newCap, newThr = 0; //新的table容量、新的阈值
        if (oldCap > 0) { // 如果table的旧容量 > 0 那么这是一个扩容操作
            if (oldCap >= MAXIMUM_CAPACITY) { // 这里会先判旧容量是否大于等于 HashMap 的容量最大值
                threshold = Integer.MAX_VALUE;
                return oldTab; //如果大于等于, 那么不执行任何扩容操作, 直接返回即可
            }
            // 观察新的table容量(oldCap*2) 是否超过最大值, 并且是否大于默认的容量
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; //如果都符合要求, 那么新的阈值 = 旧阈值*2
        }
        else if (oldThr > 0) // 新的table容量 = 旧的阈值
            newCap = oldThr;
        else { // 零初始阈值表示使用默认值
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor; //新的阈值 = 新容量 * 负载因子
            //这里会保证新容量以及新阈值都小于 HashMap 指定的最大容量
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ? (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;//将新的阈值赋给全局变量threshold
        MyNode<K,V>[] newTab = (MyNode<K,V>[]) new MyNode[newCap];//根据新的容量创建一个新的table数组
        table = newTab;//将新的table数组赋给全局变量table
        /**
         * 以上, 新的table数组创建以及新的阈值更新操作已经完成了, 这就意味着, 如果resize()是一个初始化table数组的操作, 到这里逻辑就执行结束了
         * 但是, 如果是扩容操作, 我们还需将旧数组中的Node搬迁到新的数组中, 这其实是一个非常耗时的操作
         * 因为新数组的长度改变了, 我们的Node还需要重新通过hash值计算index, 然后放置在新数组中
         *
         * 说句题外话, 这里也是JDK1.8能够优化的地方, JDK1.8中是一次性将元素全部搬迁到了新数组中, 如果旧数组中的结点有很多个, 那么这个操作将会非常耗时
         * 我们可以在扩容操作时, 先申请一个新数组, 但并不慌执行数据搬迁动作. 当有新的数据插入时, 我们将新数据插入到新的table数组中, 并从旧的table数组中拿一个Node放到新table中
         * 总的来说, 就是每插入一个新Node, 就搬迁一个旧Node
         * 那么对于查询操作, 为了兼容新、旧table中的数据, 我们先从新table中检索(因为新的table容量较少, 检索会更快), 如果没有再去旧table中检索.
         * 通过这种均摊的方式, 就避免了一次性扩容耗时过多的问题
         *
         * 下面就是JDK1.8的搬迁操作, 省略了红黑树的逻辑
         */
        if (oldTab != null) { //旧数组不为空, 那么一定是扩容操作
            for (int j = 0; j < oldCap; ++j) { // 这里会遍历旧数组中的所有结点, 然后放入新数组中
                MyNode<K,V> e;//指向桶链表中Node的指针
                if ((e = oldTab[j]) != null) { // 从头结点开始, 观察头结点是否为空, 若为空就进行下一次遍历
                    oldTab[j] = null; // 这边会将旧数组中的对应桶置为null, 以便于扩容后续的GC, 因为我们的指针 e 已经获取到了对应的通链表
                    /**
                     * 下面两个 if else 语句块的意思简单来说如下：
                     * 如果桶上只有一个结点, 那么我们只需要将头结点搬迁到新table即可
                     * 如果不止一个结点, 那么我们将这个链表分化为两个链表搬迁到新数组中
                     */
                    if (e.next == null) // 如果e.next为空, 那么说明这个桶只有一个结点, 我们计算它在新table中的index, 并放入newTable[index]位置即可
                        newTab[e.hash & (newCap - 1)] = e;
                    //下面省略了树的操作, 简单来说就是头结点为树结点时, 那么把这棵树打散成两棵树搬到新桶中
//                    else if (e instanceof TreeNoe)
//                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else {
                        MyNode<K,V> loHead = null, loTail = null; //低位链表头、尾结点
                        MyNode<K,V> hiHead = null, hiTail = null; //高位链表头、尾结点
                        MyNode<K,V> next;//next指针指向引用 e 的next结点
                        do { //遍历链表上的所有结点
                            next = e.next;
                            /**
                             * 这里分化的逻辑是：
                             * 如果当前结点的hash值 & 旧table的长度 == 0, 那么就将其追加在低位链表中
                             * 如果当前结点的hash值 & 旧table的长度 != 0, 那么追加在高位链表中
                             */
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)  //如果低位链表尾结点为空, 说明这个表为空, 我们将e作为头结点即可
                                    loHead = e;
                                else
                                    loTail.next = e; // 否则将 e 追加在尾结点之后
                                loTail = e; // 将 e 作为新的尾结点
                            } else { // 同样的操作也使用在高位链表中
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        /**
                         * 判断高位、低位链表是否为空
                         * 若不为空, 低位链表在新桶中的位置还是与旧桶一样; 高位链表在新桶中的位置刚好实在原位置之上加上旧容量
                         *
                         * 其实这里, 高低位链表在newTab中的下标位置是很有学问的
                         * 我们在将链表划分为高低位时, 是根据 (e.hash & oldCap) 是否等于0 来划分的
                         * 如果一个结点的hash值高位值都是0, 那么它在新数组中的位置和之前的位置是一样的, 这里的高位是从原长度二进制位=1的位置开始计算的, 例如16的二进制=10000, 那么高位就是从第5位开始计算。
                         *
                         * 就比如 e.hash=000011, 它的原table(假设长度为16)中的 index=000011&(16 - 1)=3
                         * 在扩容之后, index=000011&(32-1)=3, 可以发现与原数组的下标值相同
                         *
                         * 而高位为1的, 比如e.hash=010101, 原table中的index=010101&(16-1)=5
                         * 在扩容之后, index=010101&(32-1)=16+5=21， 也就是原数组长度+原下标
                         *
                         * 从这一点上看, 这也是为什么hashMap要求长度为2的幂次方
                         */
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    /**
     * 删除Map中, Node.key = 传入的key 的结点, 并返回这个结点的value
     *
     * 这里调用removeNode时, 传入matchValue为false, 说明当两个key只要地址相同、或equals返回true、或hash值相同, 都会执行删除
     * @param key
     * @return
     */
    @Override
    public V remove(Object key) {
        MyNode<K,V> e = removeNode(hash(key), key, null, false, true);
        return e == null ? null : e.value;
    }

    /**
     * 删除Map中, Node.key=key, Node.value=value的结点, 并返回是否成功删除
     * @param key
     * @param value
     * @return
     */
    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    /**
     *
     * @param hash
     * @param key
     * @param value
     * @param matchValue 如果为true，则仅在 Node.value.equals(value) 时删除
     * @param movable 如果为false，则在删除时不移动其他节点
     * @return
     */
    final MyNode<K,V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        MyNode<K,V>[] tab;
        MyNode<K,V> p;
        int n, index;
        //下面这个逻辑会保证成功获取到table数组, 并获取到hash对应的桶链表
        if ((tab = table) != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null) {
            MyNode<K,V> node = null, e;//node引用指向的是待删除结点、e 指向遍历链表时的当前结点
            K k;
            V v;
            //判断链表头结点的key与传入的key是否相同, 如果相同, 将引用p赋给待删除结点引用node
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            else if ((e = p.next) != null) { //如果头结点不匹配, 那么从第二个结点开始遍历, 寻找待删除结点
                //省略TreeNode逻辑
                do {
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) { // 如果找到了就将当前链表地址引用赋给node, 并退出循环
                        node = e;
                        break;
                    }
                    p = e;
                } while ((e = e.next) != null);
            }
            //移除Node的操作, 这里会先判断是否找到待删除的结点, 然后依据matchValue的逻辑来判断是否删除这个node
            if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
                if (node == p) //头结点的删除逻辑, 直接将桶置为null
                    tab[index] = node.next;
                else //单链表的删除逻辑
                    p.next = node.next;
                afterNodeRemoval(node);//成功删除之后的后续操作
                --size;
                return node;// 成功删除后返回删除结点
            }
        }
        return null;
    }

    /**
     * 清空整个table
     *
     * 这里就是遍历整个table, 然后将桶链表置为null即可 (也要更新size=0)
     */
    @Override
    public void clear() {
        MyNode<K,V>[] tab = table;
        if (tab != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
    }

    /**
     * 获取key对应的Node, 如果成功获取那么返回Node.value, 否则返回默认值
     * @param key
     * @param defaultValue
     * @return
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        MyNode<K,V> e = getNode(hash(key), key);
        return e == null ? defaultValue : e.value;
    }

    /**
     * 执行put操作, 但仅当不存在这个键值对的时候才追加结点, 不会执行覆盖旧value的操作
     *
     * 注意这个方法的onlyIfAbsent参数为true, 这就代表不会覆盖已经存在的value值
     * @param key
     * @param value
     * @return
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }


    /**
     * 替换Node.key = key, Node.value = oldValue 的value值为newValue
     *
     * 整个逻辑也比较简单 (因为复杂的获取Node操作已经封装在getNode中了)
     * @param key
     * @param oldValue
     * @param newValue
     * @return
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        MyNode<K,V> e = getNode(hash(key), key);
        V v;
        if (e != null && ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    /**
     * 这个方法的功能和逻辑与上一个方法差不多, 区别只在于返回值
     * @param key
     * @param value
     * @return
     */
    @Override
    public V replace(K key, V value) {
        MyNode<K,V> e = getNode(hash(key), key);
        if (e != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(MyNode<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(MyNode<K,V> p) { }
}