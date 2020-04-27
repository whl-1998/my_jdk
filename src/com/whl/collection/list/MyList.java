package com.whl.collection.list;

import java.util.*;

/**
 * 鉴于本人比较菜, 关于Collection相关的方法以及SubList内部类相关的逻辑都没有进行相关的分析与实现.
 * 除此以外, 迭代器相关的操作也没有实现
 * 但麻雀虽小, 五脏俱全, 比较常用的方法都进行详细分析和实现了, 如有bug请联系本人qq 313576743
 * @param <E>
 */
public interface MyList<E> {
    void add(int index, E element);

    boolean add(E e);

    E remove(int index);

    boolean remove(Object o);

    void clear();

    E get(int index);

    boolean contains(Object o);

    int indexOf(Object o);

    int lastIndexOf(Object o);

    Object[] toArray();

    <T> T[] toArray(T[] a);

    E set(int index, E element);

    int size();

    boolean isEmpty();

    List<E> subList(int fromIndex, int toIndex);
}
