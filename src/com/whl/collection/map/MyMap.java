package com.whl.collection.map;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface MyMap<K,V> {
    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    V put(K key, V value);

    V remove(Object key);

//    void putAll(Map<? extends K, ? extends V> m);

    void clear();

//    Set<K> keySet();

//    Collection<V> values();

//    Set<Map.Entry<K, V>> entrySet();

    interface Entry<K,V> {
        K getKey();

        V getValue();

        V setValue(V value);

        boolean equals(Object o);

        int hashCode();
    }

    boolean equals(Object o);

    int hashCode();

    /**
     * Map中如果存在这个key, 那么就返回这个key, 否则返回默认值defaultValue
     * @param key
     * @param defaultValue
     * @return
     */
    default V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v != null || containsKey(key) ? v : defaultValue;
    }

    /**
     * Map中, 如果传入key对应的value为空, 那么添加新的value, 并返回; 否则返回已经存在的value
     * @param key
     * @param value
     * @return
     */
    default V putIfAbsent(K key, V value) {
        V v = get(key);
        if (v == null) {
            v = put(key, value);
        }
        return v;
    }

    /**
     * 移除map中key, value等于传入值的键值对
     *
     * 我们需要在删除之前先判断是否存在这么个键值对
     *
     * 先获取到传入key的当前值curValue
     * 如果这个传入的value与curValue不相等, 或是 "curValue为空且map中不包含key" 那么remove失败
     *
     * 否则执行remove操作
     * @param key
     * @param value
     * @return
     */
    default boolean remove(Object key, Object value) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, value) || (curValue == null && !containsKey(key))) {
            return false;
        }
        remove(key);
        return true;
    }

    /**
     * 替换value操作
     *
     * 与remove同样的判断逻辑, 判断如果不存在符合条件的键值对, return false
     * 然后执行put
     * @param key
     * @param oldValue
     * @param newValue
     * @return
     */
    default boolean replace(K key, V oldValue, V newValue) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, oldValue) || (curValue == null && !containsKey(key))) {
            return false;
        }
        put(key, newValue);
        return true;
    }

    /**
     * 直接替换key的value
     * @param key
     * @param value
     * @return
     */
    default V replace(K key, V value) {
        V curValue = get(key);
        if (curValue != null || containsKey(key)) {
            curValue = put(key, value);
        }
        return curValue;
    }
}
