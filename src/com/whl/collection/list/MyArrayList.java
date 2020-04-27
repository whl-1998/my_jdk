package com.whl.collection.list;

import java.util.*;

/**
 * @author whl
 * @version V1.0
 * @Title: 手写JDK1.8 - ArrayList
 * @Description:
 *
 *
 */
public class MyArrayList<E> implements MyList<E> {
    private Object[] elementData;// 用于存储元素
    private static final Object[] EMPTY_ELEMENT_DATA  = {};// 用于构造时, 指定初始容量为0时, 初始化的内部数组

    // 用于构造时, 不指定initialCap时, 将其赋予给elementData, 目的是为了实现lazy-load
    // 只有当真正使用ArrayList时(添加元素), 才会通过默认长度创建Object数组
    private static final Object[] DEFAULT_CAPACITY_EMPTY_ELEMENT_DATA = {};

    private static final int DEFAULT_CAPACITY = 10;
    private int size;

    public MyArrayList() {
        this.elementData = DEFAULT_CAPACITY_EMPTY_ELEMENT_DATA;
    }

    /**
     * 如果容量 > 0, 构建对应长度的Object数组
     * 如果容量 = 0, 返回EMPTY_ELEMENT_DATA
     * 如果容量 < 0, 抛出异常
     * @param initialCap
     */
    public MyArrayList(int initialCap) {
        if (initialCap > 0) {
            elementData = new Object[initialCap];
        } else if (initialCap == 0) {
            elementData = EMPTY_ELEMENT_DATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCap);
        }
    }

    /**
     * 首先我们需要明确, elementData数组的大小是否能够放置新元素
     * 如果容量不足, 那么执行扩容操作
     * @param e
     * @return
     */
    @Override
    public boolean add(E e) {
        ensureCapacityInternal(size + 1);
        elementData[size++] = e;
        return true;
    }

    /**
     * 在指定索引位置添加元素
     * 首先我们必须明确这个索引位于 0 ~ size-1 这个范围内, 这个逻辑其实比看上去要关键很多：
     *      假如我们初始化一个初始容量为5的ArrayList, 然后直接在 index==4 的位置进行add操作, 这样其实是会抛出异常的, ArrayList必须确保elementData上的数据是连续的
     * 其次数组的插入操作涉及到了后续元素的移动, 因此我们还需要通过System.arraycopy()移动后续元素
     * 但是在移动之前, 我们必须确保elementData有足够的空间进行数组移动, 因此同样需要调用ensureCapacityInternal判断数组是否需要扩容
     * @param index
     * @param element
     */
    @Override
    public void add(int index, E element) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException("index out of bounds: " + index);
        ensureCapacityInternal(size + 1);
        System.arraycopy(elementData, index, elementData, index + 1, size - index);
        elementData[index] = element;
        size++;
    }

    /**
     * 原生JDK中, 该方法的这两个判断逻辑是分开写的, 其中的思想应该是为了遵循方法的单一职责原则
     * 这里为了更加易于阅读, 我将这两个校验逻辑合并在一起
     * @param minCapacity
     */
    private void ensureCapacityInternal(int minCapacity) {
        /**
         * 将传入的elementData与minCapacity进行比较, 如果判断elementData是DEFAULT_CAPACITY_EMPTY_ELEMENT_DATA, 也就是无参构造创建的elementData
         * 那么我们将默认初始化长度10与当前所需要的最小长度minCapacity进行比较, 获取其中的较大值返回
         *
         * 这个逻辑其实只有在通过无参构造创建后(或者初始化指定initialCapacity <= DEFAULT_CAPACITY), 第一次执行add()操作时, 才会返回DEFAULT_CAPACITY
         * 初始化过后, elementData的长度一般情况下都已经大于等于DEFAULT_CAPACITY了
         */
        if (elementData == DEFAULT_CAPACITY_EMPTY_ELEMENT_DATA) {
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        /**
         * 该方法会对传入的minCapacity进行一次参数校验, 确保minCapacity是大于elementData的, 从而进行elementData的扩容操作
         */
        if (minCapacity - elementData.length > 0) {
            grow(minCapacity);
        }
    }

    /**
     * 这个方法就是ArrayList内部的扩容操作了, 实现的思想是: 在原数组长度的基础上, 创建一个长度为原长1.5倍的新数组, 并将数组内部的值浅拷贝过去
     * @param minCapacity
     */
    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);// 新数组长度为原长1.5倍
        if (newCapacity - minCapacity < 0) // 这里是为了避免计算获取的新数组长度超过Integer.MAX_VALUE造成数值错误的情况,
            newCapacity = minCapacity;// 其避免的手段是: 不再以1.5倍作为新长度, 而是以传入的minCapacity作为新数组长度构建数组

        /**
         * 确保新数组长度不超过规定ArrayList规定的最大容量, JDK中将这个值(Integer.MAX_VALUE - 8)设置为了常量
         * 原生JDK也是为了满足单一职责原则, 将这个判断逻辑设置为了单独的方法
         */
        if (newCapacity - Integer.MAX_VALUE - 8 > 0) { // 当newCapacity超过了ArrayList的最大容量, 这时我们选择通过minCapacity进行构建新数组
            if (minCapacity < 0) // 当我们传入的minCapacity == (Integer.MAX_VALUE + 1)时, 这个minCapacity就溢出为负数了, 因此JDK在这里选择通过抛出内存溢出异常进行处理
                throw new OutOfMemoryError();
            //如果我们的minCapacity已经大于ArrayList的最大容量, 这时还有唯一一次扩容的机会, 就是将其容量扩容至Integer.MAX_VALUE, 否则按照Integer.MAX_VALUE - 8进行扩容
            newCapacity = (minCapacity > Integer.MAX_VALUE - 8) ? Integer.MAX_VALUE : Integer.MAX_VALUE - 8;
        }
        // 最后就是一次简单的Array.copy()实现一次浅拷贝扩容
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 检索对象的下标, 如果小于0, 说明不存在该元素
     * @param o
     * @return
     */
    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * get操作比较简单, 没什么好说的
     * @param index
     * @return
     */
    @Override
    public E get(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("index out of bounds: " + index);
        }
        return (E) elementData[index];
    }

    /**
     * set操作也同样比较简单, 就是一个元素替换
     * @param index
     * @param element
     * @return index位置上的旧值
     */
    @Override
    public Object set(int index, Object element) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("index out of bounds: " + index);
        }
        E oldVal = (E) elementData[index];
        elementData[index] = element;
       return oldVal;
    }


    /**
     * 删除前, 我们必须对index进行校验, 以确保它是大于index的
     * 你可能会疑惑为什么不判断index < 0 这个逻辑, 因为我们访问数组元素时, 若下标为负数, 那么直接就会抛出异常, 因此也不需要校验了
     * 至于add(index, obj)这个方法为什么要判断, 这是因为这个index如果为负数, 会影响到接下来的扩容判断
     *
     * 首先我们获取待删除元素
     * 然后计算删除元素后是否需要搬迁, 除了删除末尾元素, 其他位置的元素被删除了都会涉及到元素移动的动作, 这个是数组的特性, 也是为了保证元素的连续性
     * 然后我们将末尾元素置为null, 以便于让GC回收无引用指向的对象, 释放内存
     * @param index
     * @return
     */
    @Override
    public E remove(int index) {
        if (index < 0 ||index >= size) {
            throw new IndexOutOfBoundsException("index out of bounds: " + index);
        }
        E oldValue = (E) elementData[index];
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index + 1, elementData, index, numMoved);
        elementData[--size] = null;
        return oldValue;
    }

    /**
     * 删除指定元素的操作其实也差不多, 区别在于: 我们要先获取到元素o的index, 然后再调用之前的remove(index)方法
     * 值得一提的是, 原生JDK为了极致的性能:
     *      省略了方法调用的过程, 重写了一遍for循环
     *      并且针对null值也进行了同样的判断, 目的很明确, 也就是考虑到当我们重写传入对象的equals方法时, 需要通过equals判断删除元素的位置
     *      这里我们搞得就简单点, 当传入null对象直接抛出一个异常,
     * 在删除时通过一个省略了index判断的 void remove(int index) 方法进行删除, 这里就直接省略这种操作了
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("argument not allowed null !");
        }
        return o.equals(remove(indexOf(o)));// 当我们删除执行后的返回值equals待删除元素时, 返回true
    }

    /**
     * 清空容器
     * 只需要将elementData的值设置为null即可, GC会帮我们清理引用这些没有引用指向的对象
     */
    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            elementData[i] = null;
        }
        size = 0;
    }

    /**
     * 原生jdk采用的是 equals 判断, 目的是为了实现重写equals, 实现自定义判断对象相等的逻辑
     * 因此原生jdk中, 对传入对象为null的情况也进行特殊处理 (for循环判断当前元素是否为null)
     * 这里为了方便, 就直接比较内存地址了
     * @param o
     * @return
     */
    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size; i++) {
            if (o == elementData[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 和indexOf逻辑基本一致, 这里也采用同样的实现方式
     * @param o
     * @return
     */
    @Override
    public int lastIndexOf(Object o) {
        for (int i = size - 1; i >= 0; i++) {
            if (o == elementData[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 这个方法原生JDK中是通过一个内部类SubList实现的
     * 为了避免新建ArrayList的开销, 这个方法本质上就是获取原始elementData数组上 [fromIdx, toIdx] 这个区间的值
     *
     * 这里为了简单我们就通过创建新的ArrayList来实现
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        List<E> res = new ArrayList<>();
        for (int i = fromIndex; i <= toIndex; i++) {
            res.add((E) this.elementData[i]);
        }
        return res;
    }

    /**
     * 将ArrayList转换为Object数组
     *
     * 该实现只需要将内部数组elementData转换为数组即可
     * 需要非常非常额外注意的是：Arrays.copyOf() 会创建一个新的数组, 但是新的数组内部的元素的引用地址指向的依旧是 elementData 中原始数据的内存地址
     * 这就意味着 arr[] = Arrays.copyOf(elementData, size) != elementData (两个数组内存地址不相同)
     * 但 arr[i] = elementData[i] (数组中的元素内存地址相同)
     * 总的来说, 这个方法本质上是一个浅拷贝操作
     * @return
     */
    @Override
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    /**
     * 将ArrayList转化为其泛型类型的数组
     *
     * 如果我们的ArrayList为泛型类型User, 那么当我们需要将ArrayList转换为一个User数组, 上面的toArray()就无法满足需求了
     * 因此, 提供这个方法用于将我们的ArrayList转化为泛型指定的数组类型
     * 首先这个方法要求我们传入一个类型为T的数组arr, 我们会在这个数组的基础上, 将ArrayList的值覆盖进去
     * 这里存在如下几种情况：
     * 1. 当数组arr的长度 < ArrayList.size()：
     *          此时会先创建一个新数组, 长度为size, 类型为arr.class, 再执行拷贝动作, 将elementData中元素的内存地址按序覆盖到arr中.
     * 2. 当数组arr的长度 >= ArrayList.size()：
     *          如果arr长度恰好等于size, 那么直接执行拷贝动作即可
     *          如果arr长度 > size, 为了让调用者明确数组arr中被覆盖的区域, ArrayList会将覆盖后index = size的位置为null
     * 由于内部元素是通过Arrays.copyof()以及System.arraycopy()进行复制, 因此这个操作也属于浅拷贝
     * @param arr
     * @param <T>
     * @return
     */
    public <T> T[] toArray(T[] arr) {
        if (arr.length < size) {
            return (T[]) Arrays.copyOf(arr, size, arr.getClass());
        }
        System.arraycopy(elementData, 0, arr, 0, size);
        if (arr.length > size) {
            arr[size] = null;
        }
        return arr;
    }
}
