package com.work.redislock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author damon
 * @date: 2017/8/24.
 */
//Redis 工具类
public class RedisUtil {

    private int EXPIRE_TIME = 10;
    private int ACQUIRE_LOCK_MAX_ATTEMPTS = 10;

    public Boolean acquireLock(String key, Jedis jedis, int depth){
        //set if not exist 如果设置成功表示已经获得锁，否则没有获取锁
        long setnx = jedis.setnx(key, String.valueOf(System.currentTimeMillis()));
        if (setnx == 1L){
            //说明客户端已经获得锁
            System.out.println("key:" + key + " is acquired" + " now get the key " + Thread.currentThread().getName());
            return true;
        } else {
            //key被其他客户端加锁
            String keyTimestamp = jedis.get(key);
            if (keyTimestamp == null) {
                //如果该值已经被清空，就尝试去重新获取
                if (depth == ACQUIRE_LOCK_MAX_ATTEMPTS) {
                    //如果尝试次数超过10次，则不再尝试，直接返回false
                    System.out.println("key:" + key + " acquire fail" + " keyTimestamp is null, attempt =10 "+ Thread.currentThread().getName());
                    return false;
                }
                return acquireLock(key, jedis, depth + 1);
            }
            long intervalTime = System.currentTimeMillis() - Long.valueOf(keyTimestamp);
            if (intervalTime < EXPIRE_TIME){
                //锁并没有超时，返回false
                System.out.println("key:" + key + " acquire fail" + " other client get the key "+ Thread.currentThread().getName());
                return false;
            }else{
                //锁已经超时，尝试getset操作，设置当前时间戳
                String getSetTimestamp = jedis.getSet(key,String.valueOf(System.currentTimeMillis()));
                if (getSetTimestamp == null) {
                    //考虑非常特殊的情况，有人释放了锁执行del操作 此时getset拿到的是null,说明已经获得了锁
                    System.out.println("key:" + key + " acquire success" + " getset return null, before client is time out "+ Thread.currentThread().getName());
                    return true;
                }
                if (!getSetTimestamp.equals(keyTimestamp)) {
                    //在设置时，说明该锁已经被其他客户端占用
                    System.out.println("key:" + key + " acquire fail" + " other client get the key, before client is time out "+ Thread.currentThread().getName());
                    return false;
                }else {
                    //锁已更新，可以正常返回
                    System.out.println("key:" + key + " acquire success" + " before client is time out "+ Thread.currentThread().getName());
                    return true;
                }
            }

            }

    }

    public boolean releaseLock(String key, Jedis jedis) {
        boolean result = jedis.del(key) == 1L;
        System.out.println("key:" + key + " is released!");
        return result;
    }

    public static class MyThreadFactory implements ThreadFactory {
        public static AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            count.getAndIncrement();
            Thread thread = new Thread(r);
            thread.setName("Thread-lock-test "+count);
            return thread;
        }
    }

    public static void main(String args[]){

        String redisAddress = "localhost";
        int redisPort = 6379;
        int redisTimeout = 2000;

        JedisPool pool = new JedisPool(new JedisPoolConfig(), redisAddress, redisPort, redisTimeout);
        String key = "apple";
        Runnable runnable = () ->{
             RedisUtil redisUtil = new RedisUtil();
             Jedis jedis = pool.getResource();
             redisUtil.acquireLock(key, jedis, 0);
             try {
                Thread.sleep(100);
             }catch (Exception e){
                e.printStackTrace();
             }
             redisUtil.releaseLock(key, jedis);
        };
        MyThreadFactory myThreadFactory = new MyThreadFactory();
        ExecutorService services = Executors.newFixedThreadPool(50);
        for (int i = 0;i < 30 ;i++) {
            services.execute(myThreadFactory.newThread(runnable));
        }
        }
    }