package com.whl.collection.map;



/**
 * @author whl
 * @version V1.0
 * @Title:
 * @Description:
 *
 * LinkedHashMap继承自HashMap, 那么它是具有HashMap的大部分特性的, 比如键值对存储, 以及HashMap中非常高效的一些Hash算法和扩容策略等
 * 但不同在于, LinkedHashMap在基于HashMap的基础上, 将所有的结点通过一个双向链表串起来, 实现了有序的特性, 这个特性其实主要就是靠HashMap中的三个空方法：
 *     afterNodeAccess、afterNodeInsertion、afterNodeRemoval 这几个方法实现的
 *
 * 相比起HashMap, 优点就是支持有序, 缺点就是占内存要更大, 且每次对结点操作之后都要进行维护, 相对耗时
 */
public class MyLinkedHashMap<K,V> extends MyHashMap<K,V> implements MyMap<K,V> {
    /**
     * LinkedHashMap的Node, 或者应该叫Entry, 是包含前序、后继指针的, 这就意味着它在HashMap的基础上, 采用了双端链表的实现
     */
    static class MyEntry<K,V> extends MyHashMap.MyNode<K,V> {
        MyEntry<K,V> before; //前序指针
        MyEntry<K,V> after; // 后继指针

        MyEntry(int hash, K key, V value, MyNode<K,V> next) {
            super(hash, key, value, next);
        }
    }

    MyEntry<K,V> head;//双端链表的头结点

    MyEntry<K,V> tail;//双端链表的尾结点

    final boolean accessOrder; //指定排序方式：true表示按访问顺序排序, false表示按插入顺序排序

    /**
     * 默认构造, 与HashMap的基本相同, 指定默认排序顺序为false, 按插入顺序排序
     */
    public MyLinkedHashMap() {
        super();
        accessOrder = false;
    }

    /**
     * 其实也就是HashMap的构造方法
     */
    public MyLinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    public MyLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    /**
     * 指定排序策略, 以及初始容量, 负载因子, 构建Map
     * @param initialCapacity
     * @param loadFactor
     * @param accessOrder
     */
    public MyLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }


    /**
     * 将Entry p追加到双端链表末尾
     *
     * 逻辑上就是双端链表的插入操作
     * @param p
     */
    private void linkNodeLast(MyEntry<K,V> p) {
        MyEntry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }

    /**
     * 新建一个结点p, 并追加到双端链表末尾, 并返回p
     * @param hash
     * @param key
     * @param value
     * @param e
     * @return
     */
    MyNode<K,V> newNode(int hash, K key, V value, MyNode<K,V> e) {
        MyEntry<K,V> p = new MyEntry<>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    /**
     * 这个方法其实是HashMap中用于去树化的方法, 当时我省略掉了这个方法
     *
     * 重写之后的逻辑就是：将结点p转换为Entry, 然后替换它在链表中的位置
     * @param p
     * @param next
     * @return
     */
    MyNode<K,V> replacementNode(MyNode<K,V> p, MyNode<K,V> next) {
        MyEntry<K,V> q = (MyEntry<K,V>)p;
        MyEntry<K,V> t = new MyEntry<>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    /**
     * 用结点dst替换结点src在链表中的位置
     * @param src
     * @param dst
     */
    private void transferLinks(MyEntry<K,V> src, MyEntry<K,V> dst) {
        MyEntry<K,V> b = dst.before = src.before; //替换之后, dst结点的前序结点
        MyEntry<K,V> a = dst.after = src.after; // 替换之后, dst结点的后继结点
        if (b == null) // 如果前序结点为空, 说明dst已经是头结点了
            head = dst;
        else // 否则前序结点的Next指针指向dst
            b.after = dst;
        if (a == null) // 如果后继结点为空, 说明dst已经是后继结点
            tail = dst;
        else // 否则后继结点的next指针指向dst
            a.before = dst;
    }

    /**
     * 因为HashMap链表时单向链表, 因此在删除之后, 并不需要进行后置处理
     *
     * 而基于双端链表实现的LinkedHashMap还需要将删除结点的前序、后置结点引用链都置为null, 简单来说就是双端链表删除的那一套操作
     * @param e
     */
    void afterNodeRemoval(MyNode<K,V> e) { // unlink
        MyEntry<K,V> p = (MyEntry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }

    /**
     * 回调移除最早放入Map的对象
     * @param evict
     */
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        MyEntry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    protected boolean removeEldestEntry(MyMap.Entry<K,V> eldest) {
        return false;
    }

    /**
     * accessOrder为true时，且访问节点不等于尾节点时，该方法才有意义。通过before、after重定向，将新访问节点链接为链表尾节点。
     * @param e
     */
    void afterNodeAccess(MyNode<K,V> e) { // move node to last
        MyEntry<K,V> last;
        if (accessOrder && (last = tail) != e) {
            MyEntry<K,V> p = (MyEntry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
        }
    }
}
