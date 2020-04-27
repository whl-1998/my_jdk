package com.whl.collection.list;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author whl
 * @version V1.0
 * @Title: 手写JDK1.8 - LinkedList
 * @Description:
 */
public class MyLinkedList<E> implements MyList<E> {
    private static class MyNode<E> {
        E item;
        MyNode<E> next;
        MyNode<E> prev;

        MyNode(MyNode<E> prev, E element, MyNode<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    private MyNode<E> first;// 头结点指针
    private MyNode<E> last;// 尾结点指针
    private int size;

    /**
     * 逻辑相对比较简单, 先判断index是否合法, 然后判断插入的位置是否是链表末尾, 如果是, 那么调用linkLast(); 如果不是, 那么插入到index位置结点的前面
     * @param index
     * @param element
     */
    @Override
    public void add(int index, E element) {
        if (!(index >= 0 && index <= size)) {
            throw new IndexOutOfBoundsException("index illegal: " + index);
        }
        if (index == size) {
            linkLast(element);
        } else {
            linkBefore(element, node(index));
        }
    }

    /**
     * 链表的插入操作, 相对比较简单这里略过, 主要的逻辑其实包含在linkLast方法中
     * @param e
     * @return
     */
    @Override
    public boolean add(E e) {
        linkLast(e);//在链表末尾追加一个结点
        return true;
    }


    /**
     * 检索index位置上的结点
     *
     * 这里最简单的方式就是从头到尾遍历index次, 时间复杂度为O(n)
     * 那么JDK处理的就相对巧妙很多：首先判断index位置是否大于链表size/2, 如果大于那么从后往前遍历index次; 如果小于从前往后遍历size-1-index次
     * 这样就节省了相当多的遍历时间
     * @param index
     * @return
     */
    MyNode<E> node(int index) {
        if (index < (size >> 1)) {
            MyNode<E> res = first;
            for (int i = 0; i < index; i++) {
                res = res.next;
            }
            return res;
        } else {
            MyNode<E> res = last;
            for (int i = size - 1; i > index; i--) {
                res = res.prev;
            }
            return res;
        }
    }

    /**
     * 链表末尾追加结点
     *
     * 总体逻辑就是：
     *   1：获取到尾结点lastNode
     *   2：对元素e封装为新结点newNode, 并在构造时指定其前序结点为尾结点lastNode
     *   3：将newNode置为新的尾结点
     *   4：如果lastNode为空, 这说明我们的链表为空, 此时需要将头结点first也指向newNode; 否则lastNode.next指向newNode
     * @param e
     */
    private void linkLast(E e) {
        final MyNode<E> lastNode = last;
        final MyNode<E> newNode = new MyNode<>(lastNode, e, null);
        last = newNode;
        if (lastNode == null) {
            first = newNode;
        } else {
            lastNode.next = newNode;
        }
        size++;
    }

    /**
     * 指定结点的位置前插入一个结点
     *  1. 首先获取到指定结点succ的前序结点pred
     *  2. 然后创建新结点newNode, 并指定prev指针指向pred, next指针指向succ
     *  3. succ结点的prev指针也指向newNode
     *  4. 此时我们需要判断pred这个结点是否为空, 如果为空那么说明succ结点一定是头结点, 那么此时我们需要将newNode设置为新的头结点
     *  5. 若不是, 那么pred.next指向newNode即可
     *
     * @param e
     * @param succ
     */
    void linkBefore(E e, MyNode<E> succ) {
        // assert succ != null;
        final MyNode<E> pred = succ.prev;
        final MyNode<E> newNode = new MyNode<>(pred, e, succ);
        succ.prev = newNode;
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        size++;
    }

    /**
     * 这个方法是重写了Deque接口中的remove抽象方法
     *
     * 该方法会移除链表的头结点
     * @return
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * 移除链表头结点
     *  1. 获取到头结点
     *  2. 如果头结点为空, 这里会抛出一个异常
     *  3. 然后就是移除头结点的操作了, JDK中为了保证方法的单一职责原则, 将这部分的逻辑封装在了unLinkFirst方法中, 这里为了便于阅读直接放上来
     * @return
     */
    private E removeFirst() {
        MyNode<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        /**
         * 以下是unLinkFirst的逻辑
         */
        final E element = f.item;
        final MyNode<E> next = f.next;//获取到头结点的next结点
        f.item = null;
        f.next = null; // help GC, 注意此时头结点还被next结点的prev指针所引用
        first = next;// 将next设置为新的头结点
        if (next == null)// 如果next结点为空, 那么说明链表在删除之前只存在一个结点, 这个结点分别担任了头、尾结点的职责
            last = null;// 于是我们也需要将last结点置为null
        else
            next.prev = null;// 如果next结点不为空, 那么我们需要将next.prev的引用也置为null, 保证GC能够回收到无引用指向的原头结点
            size--;
        return element;
    }

    /**
     * 删除指定位置上的结点
     *
     * 1. 首先依旧是index非法判断
     * 2. 然后获取到待删除结点, 移除其前序结点, 后序结点的互相引用, 这部分的逻辑JDK也封装在unlink方法中
     * @param index
     * @return
     */
    @Override
    public E remove(int index) {
        if (!(index >= 0 && index < size)) {
            throw new IndexOutOfBoundsException("index illegal: " + index);
        }
        MyNode<E> removeNode = node(index);
        return unlink(removeNode);

    }

    /**
     * 移除removeNode
     * @param removeNode
     * @return
     */
    private E unlink(MyNode<E> removeNode) {
        final E element = removeNode.item;
        final MyNode<E> next = removeNode.next;
        final MyNode<E> prev = removeNode.prev;
        if (prev == null) { // 若待删除结点的前序结点为空, 那么删除之后next结点就成为了新的头结点
            first = next;
        } else { // 否则前序结点.next指向待删除结点的后序结点
            prev.next = next;
            removeNode.prev = null; // 移除待删除结点的前序指针
        }
        if (next == null) { // 待删除结点的后序结点也要进行同样的逻辑
            last = prev;
        } else {
            next.prev = prev;
            removeNode.next = null;
        }
        removeNode.item = null; // helpGC
        size--;
        return element;
    }

    /**
     * 这个方法中, 为了保证删除的obj能够支持通过equals判断是否相等, 因此也是针对item为null的结点进行了特殊的处理
     * 这里为了方便直接判断内存地址了
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        for (MyNode<E> f = first; f != null; f = f.next) {
            if (f.item == o) {
                unlink(f);
                return true;
            }
        }
        return false;
    }

    /**
     * 删除所有的结点
     *
     * 这里的逻辑就是从头结点开始, 依次移除所有的引用指针
     */
    @Override
    public void clear() {
        for (MyNode<E> x = first; x != null; ) {
            MyNode<E> next = x.next;
            x.item = null;// 当前结点数据指针置为null
            x.next = null;// 当前结点后续指针置为null
            x.prev = null;// 当前结点前续指针置为null
            x = next;
        }
        first = last = null; // 头尾指针置为null
        size = 0;
    }

    @Override
    public E get(int index) {
        if (!(index >= 0 && index < size)) {
            throw new IndexOutOfBoundsException("index illegal: " + index);
        }
        return node(index).item;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    /**
     * 原生JDK依旧是为了保证equals为了正常执行, 对null值进行特殊处理
     * @param o
     * @return
     */
    @Override
    public int indexOf(Object o) {
        int index = 0;
        for (MyNode<E> f = first; f.next != null; f = f.next) {
            if (f.item == o) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * 逻辑参照indexOf
     * @param o
     * @return
     */
    @Override
    public int lastIndexOf(Object o) {
        int index = size - 1;
        for (MyNode<E> f = last; f.prev != null; f = f.prev) {
            if (f.item == o) {
                return index;
            }
            index--;
        }
        return -1;
    }

    /**
     * 修改index位置上的Node.item
     * 1. index非法校验
     * 2. 先获取到index位置上的Node
     * 3. 然后修改, 返回旧值
     * @param index
     * @param element
     * @return
     */
    @Override
    public E set(int index, E element) {
        if (!(index >= 0 && index < size)) {
            throw new IndexOutOfBoundsException("index illegal: " + index);
        }
        MyNode<E> x = node(index);
        E val = x.item;
        x.item = element;
        return val;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 返回一个包含了从fromIndex到toIndex中的结点的链表
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        LinkedList<E> linkedList = new LinkedList<>();
        MyNode<E> f = node(fromIndex);
        int i = fromIndex;
        while (i <= toIndex) {
            linkedList.add(f.item);
            f = f.next;
            i++;
        }
        return linkedList;
    }

    /**
     * 将链表转换为Object数组
     * 1. 首先创建一个size大小的Object数组
     * 2. 依次遍历链表, 将Node中的值放入链表
     * @return
     */
    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (MyNode<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    /**
     * 将链表转换为指定泛型的数组, 需要注意的是, 如果传入数组 a 的长度足够大, 那么在将链表中的值覆盖到数组中之后, 还会将size位置置为null,
     *  以便于让使用者看到数组 a 中值被覆盖的区域
     * 1. 如果传入的数组a足够大，那么直接存储链表中的元素; 否则，将为a这个引用分配一个具有相同运行时类型的新数组
     * 2. 遍历链表, 将值按顺序覆盖到数组中
     * @param a
     * @param <T>
     * @return
     */
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (MyNode<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        if (a.length > size)
            a[size] = null;
        return a;
    }
}
