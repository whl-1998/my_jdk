package com.whl.thread.blockingQueue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author whl
 * @version V1.0
 * @Title: JDK1.8 阻塞队列 (内部基于数组)
 * @Description:
 *
 * 总的来看, 阻塞队列的入队方法有put(E e)、offer(E e)、offer(E e, long timeout, TimeUnit u)、add(E e)
 * 其中支持阻塞式入队的方法有put(E e)、offer(E e, long timeout, TimeUnit u), 主要通过条件锁 + ReentrantLock的中断式获取锁机制来实现
 * 他们四者的共同点就是, 入队的具体操作都交给了enqueue这个方法完成, 这些方法中主要完成的是获取锁, 空值校验, 唤醒线程等工作
 *
 * 同样的原理, 也使用在出队方法take(), poll(), poll(long timeout, TimeUnit u)、remove(Object o)这几个方法中
 * 其中take()、poll(long timeout, TimeUnit u)支持阻塞式出队
 *
 */
public class MyArrayBlockingQueue<E> implements MyBlokingQueue<E> {
    final Object[] items;//队列保存元素的Object数组

    int takeIndex;//当我们调用出队、或peek 这类方法时, 操作的就是这个指针指向的元素

    int putIndex;//当我们调用入队这类方法时, 操作的就是这个指针指向的元素

    int count;//队列中的元素值

    final ReentrantLock lock;// 锁对象, 保证并发

    private final Condition notEmpty;// Condition对象, 使队列实现阻塞出队的操作

    private final Condition notFull;// Condition对象, 使队列实现阻塞入队的操作

    /**
     * 创建具有给容量和默认访问策略(false)的ArrayBlockingQueue
     * @param capacity
     */
    public MyArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }

    /**
     * 创建具有给容量和指定访问策略的ArrayBlockingQueue
     *
     * 当我们传入fair参数为true时, 那么创建的lock是支持公平锁操作的
     * 这就意味着, 多线程访问阻塞队列时, 必须按照ReentrantLock的公平锁机制进行访问
     * @throws IllegalArgumentException if capacity < 1
     */
    public MyArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull =  lock.newCondition();
    }

    /**
     * 其实在原生JDK中, 我们能够发现这个方法内部是基于AbstractDeque这个抽象类的方法实现的, 采用了模版设计模式
     * 在这里我们就直接将父类的offer()方法搬过来, 以便于阅读
     *
     * 若offer失败抛出异常, 注意这里抛出的异常是Queue full, 这就意味着这个方法并不是一个阻塞添加方法
     * 其实我们深入到offer(E e)中观察之后也能够发现, 这个方法并不支持阻塞入队
     * @param e
     * @return
     */
    @Override
    public boolean add(E e) {
        if (offer(e))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

    /**
     * offer的逻辑其实只包含了一些参数校验以及获取锁的操作
     *
     * 真正执行入队的操作在 enqueue 这个方法中
     *
     * 需要注意的是, 这个方法的实现用是调用lock, 实现的功能是：入队操作之后要么返回true、要么返回false; 如果入队的元素是null, 那么会抛空指针异常
     * 但是这个方法不会抛出被中断的异常 (InterruptedException)
     *
     * 也就是说, 这个方法并不是一个非阻塞方法, 当队列为满时, 直接就会return false, 而不是阻塞等待队列数组有空间执行入队操作
     * @param e
     * @return
     */
    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        //获取到锁
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length)
                return false;
            else {
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 入队逻辑
     * 1. 获取到底层数组items, 并在对应的putIndex位置添加上元素e
     * 2. putIndex+1, 并观察增加之后是否已经 == items的长度, 如果相等, 那么putIndex指针指向数组头部
     * 3. 条件变量notEmpty调用signal方法, 通知阻塞等待 "队列不为空" 条件的线程
     * @param e
     */
    private void enqueue(E e) {
        final Object[] items = this.items;
        items[putIndex] = e;
        if (++putIndex == items.length)
            putIndex = 0;
        count++;
        notEmpty.signal();
    }

    /**
     * put 这个方法也是入队方法, 但与offer(E e)的区别在于它是阻塞入队的
     *
     * 可以看到我们在获取锁时, 是通过 lockInterruptibly 这个方法获取锁
     *
     * 当两个线程通过 lockInterruptibly 这个方法获取锁时, 如果其中一个线程成功获取到了, 另一个线程就只能进入到休眠状态, 直到成功获取锁.
     *      并且这个状态是可以通过 Thread.interrupted() 进行中断的, 因此这个获取锁的方式也称为 "可中断获取锁"
     * 这就意味着, 如果一个线程在执行这个方法, 尝试获取锁时被中断, 那么就会抛出一个InterruptedException异常; 否则就会进入到等待状态, 直到成功获取到锁
     *
     * 在成功获取锁之后, 若队列已满, 让当前线程释放掉锁, 进入到等待状态, 直到条件锁 notFull.signal()被执行或者当前线程被调用interrupted方法被中断
     *
     * 所有条件都满足之后, 执行入队操作 enqueue()
     *
     * @param e
     * @throws InterruptedException
     */
    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length)
                notFull.await();
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阻塞等待指定时间, 若期间满足入队条件, 则执行入队方法; 否则当前线程被中断
     *
     * 1. 先根据时间单位计算出等待时间nanos
     * 2. 中断式获取锁
     * 3. 当队列已满时, 先判断等待时间是否小于0, 如果小于0那么直接就return false表示入队失败
     *              否则调用条件锁 notFull.awaitNanos , 让当前线程释放当前持有锁, 并进入等待状态, 等待nanos ms之后如果仍然不满足入队条件, 那么return false
     *              当满足条件, 执行入队操作
     * @param e
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阻塞式出队
     *
     * 1. 首先中断式获取锁
     * 2. 若队列为空, 那么当前线程释放持有锁, 等待直到被唤醒后执行出队方法
     * @return
     * @throws InterruptedException
     */
    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0)
                notEmpty.await();
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 出队 非阻塞式
     *
     * 1. 获取锁
     * 2. 观察队列是否为空, 若不为空则return出队的元素, 为空则return null
     * @return
     */
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阻塞式出队, 等待指定时间
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除队列中的指定元素
     *
     * 1. 首先获取items数组, 注意这里获取到的items数组有可能与在获取到锁之后的items不同, 因为有可能其他线程执行了出、入队的操作
     * 2. 获取到锁
     * 3. 判断队列中是否存在元素, 若不存在则 return false
     * 4. 若存在元素, 则执行出队逻辑
     *      获取到putIndex、i = takeIndex
     *      从i位置开始遍历, 如果遍历到 items[i].equals(o), 执行删除方法 removeAt(i), 并return true
     *      如果 i == items.length, 那么从头开始遍历, 直到 i == putIndex
     *
     *
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                // 注意这里为什么要获取一个final类型的putIndex, 而不是直接使用全局变量
                // 这是因为putIndex有可能被其他线程改变, 而这里我们不希望它改变
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i])) {
                        removeAt(i);
                        return true;
                    }
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 删除指定位置上的元素
     *
     * 由于这个方法在被调用时, 一定是已经获取到锁的状态(与enqueue、dequeue相同), 因此我们不需要考虑并发问题
     *
     * 1. 获取到items数组
     * 2. 若removeIndex恰好等于takeIndex, 那么直接将 items[takeIndex] 位置上的元素置为null即可, ++takeIndex, 并且当takeIndex==数组长度时, 复位到0
     *          这里原生JDK还有一个更新迭代器状态的操作, 我这边注释掉了
     * 3. 若不等于, 那么从takeIndex开始遍历, 挨个将待删除位置后续的元素往前挪一位, 然后将挪动的末尾元素置为null
     * 4. 成功移除后, 条件锁 NotFull 执行 signal() 方法
     * @param removeIndex
     */
    private void removeAt(int removeIndex) {
        final Object[] items = this.items;
        if (removeIndex == takeIndex) {
            items[takeIndex] = null;
            if (++takeIndex == items.length)
                takeIndex = 0;
            count--;
//            if (itrs != null)
//                itrs.elementDequeued();
        } else {
            final int putIndex = this.putIndex;
            for (int i = removeIndex;;) {
                int next = i + 1;
                if (next == items.length)
                    next = 0;
                //其实这个操作也就是为了保证删除之后, 队列中元素的连续性
                if (next != putIndex) { //将待删除位置后的元素往前挪动一位
                    items[i] = items[next];
                    i = next;
                } else { // 若 next == putIndex, 说明挪动完成, 将挪动后的最后一个元素置为null, 并将putIndex置为i, 以保证下次入队操作从这个位置开始放入, 保证items中元素的连续性
                    items[i] = null;
                    this.putIndex = i;
                    break;
                }
            }
            count--;
//            if (itrs != null)
//                itrs.removedAt(removeIndex);
        }
        notFull.signal();
    }

    /**
     * 出队操作
     *
     * 1. 获取takeIndex上的元素, 并将takeIndex位置置为null
     * 2. takeIndex++, 当==items.length时, 复位到0
     * 3. notFull条件锁唤醒其他线程
     * @return
     */
    private E dequeue() {
        final Object[] items = this.items;
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            takeIndex = 0;
        count--;
//        if (itrs != null)
//            itrs.elementDequeued();
        notFull.signal();
        return x;
    }

    /**
     * 返回当前队列中还剩余多少可放置元素的位置
     * @return
     */
    @Override
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }


    /**
     * 观察该队列是否包含元素 o
     *
     * 1. 获取到数组items与锁
     * 2. 若队列中有值, 则从 i = takeIndex 开始寻找是否存在与 o 相等的元素, 找到就返回true, 直到 i == putIndex
     * @param o
     * @return
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i]))
                        return true;
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
