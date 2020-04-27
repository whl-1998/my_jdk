package com.whl.lang;

/**
 * @author whl
 * @version V1.0
 * @Title:
 * @Description:
 */
public class MyInteger {
    private final int value;//注意这个value是一个final类型, 这里采用了多线程中的不变模式, 因此Integer这个类是保证线程安全的 (不仅如此, JDK中Integer这个类也是final类型的)

    //Integer的数值范围, 和int的最小最大值相同
    public static final int MIN_VALUE = 0x80000000;//-2^31
    public static final int MAX_VALUE = 0x7fffffff;//2^31 - 1

    public MyInteger(int value) {
        this.value = value;
    }

    /**
     * 通过将字符串s转换为Integer类型进行初始化
     *
     * 这里我们直接调用了原生Integer的parseInt, 因为转换的细节相对比较复杂, 个人认为也不是重点
     * @param s
     * @throws NumberFormatException
     */
    public MyInteger(String s) throws NumberFormatException {
        this.value = Integer.parseInt(s);
    }

    /**
     * 包装类缓存, 这里是采用了享元模式的设计模式、单例内部类模式
     */
    private static class MyIntegerCache {
        static final int low = -128;//缓存int值的范围从-128开始
        static final int high = 127;//缓存的范围最大值, 原生JDK中是可以通过JVM参数来设置的, 这里我们直接采用默认值127
        static final MyInteger cache[];//Integer[] 缓存数组

        static {
            cache = new MyInteger[(high - low) + 1];//构建一个长度为127+128+1的Integer数组
            int j = low;
            for(int k = 0; k < cache.length; k++)
                cache[k] = new MyInteger(j++);
        }

        private MyIntegerCache() {}
    }

    /**
     * 获取较大值
     * @param a
     * @param b
     * @return
     */
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    /**
     * 获取较小值
     * @param a
     * @param b
     * @return
     */
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    /**
     * 获取a+b的值
     */
    public static int sum(int a, int b) {
        return a + b;
    }

    /**
     * 比较x与y
     * 如果x < y, 那么 return -1
     * 如果x == y, 那么 return 0
     * 如果x > y, 那么 return 1
     * @param x
     * @param y
     * @return
     */
    public static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    /**
     * 获取int值的包装类
     *
     * 其实, Integer的自动装箱拆箱就是依赖的这个方法：
     * 当一个int值在编译期被转换为Integer时, javac编译器在内部调用了这个valueOf方法, 同样的方式也使用在了intValue()这个方法上用于实现自动拆箱
     * @param i
     * @return
     */
    public static MyInteger valueOf(int i) {
        if (i >= MyIntegerCache.low && i <= MyIntegerCache.high) //如果i的范围为-128~127, 那么从缓存中获取
            return MyIntegerCache.cache[i + (-MyIntegerCache.low)];
        return new MyInteger(i);//否则通过构造方法获取
    }

    /**
     * 获取String转换为int值的包装类
     * @param s
     * @return
     * @throws NumberFormatException
     */
    public static MyInteger valueOf(String s) throws NumberFormatException {
        return MyInteger.valueOf(Integer.parseInt(s));
    }

    /**
     * 获取包装类的Int类型
     * @return
     */
    public int intValue() {
        return value;
    }

    /**
     * 重写equals方法, 比较两个Integer是否相同, 这里通过Integer内部的value进行比较
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        if (obj instanceof MyInteger) {
            return value == ((MyInteger)obj).value;
        }
        return false;
    }

    /**
     * 我们重写了equals, 因此为了保证hashCode的有效性, 我们直接将value作为hashCode, 以保证Integer唯一性的有效
     * @return
     */
    public int hashCode() {
        return value;
    }
}
