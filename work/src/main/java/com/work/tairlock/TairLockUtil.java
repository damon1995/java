package com.work.tairlock;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.TairManager;
import com.taobao.tair.impl.mc.MultiClusterTairManager;

/**
 * @author damon
 * @date: 2017/8/29.
 */
public class TairLockUtil {

    private static final int DEFAULT_VERSION = 2; // 不能为0或1，其他值都可以：0表示强制更新，如果是1则第二次刚好是2，也不能保证锁
    private static final int EXPIRE_TIME = 6; // 超时时间，单位为秒
    private static final int LOCK_GET_WAIT_TIMES = 5; // wait重试最大次数,间隔时间2的指数级增长
    private static final int LOCK_GET_WAIT_BASE_TIME = 100; // wait重试base时间ms
    private static final int LOCK_GET_WAIT_GAP = 200; // wait重试递增时间ms
    private static final int LOCK_GET_WAIT_TOTAL_TIME = 7000; // wait重试最大时间ms
    private static final int LOCK_GET_MAX_RETRY = 3; // get重试次数
    private static final int LOCK_PUT_MAX_RETRY = 3; // put重试次数

    private static final int NAMESPACE = 694; //应用申请时分配的namespace
    private static TairManager tairManager;//如果项目使用了spring可以采用注入的方式，非Spring可以用静态代码块在类初始化的时候初始化
    private static ThreadLocal<String> currentThreadValue = new InheritableThreadLocal<String>(); // 避免delete了非本线程put的值

    static {
        tairManager = new MultiClusterTairManager();
        //tairManager.setConfigID("mdbcomm-daily");
        //tairManager.setDynamicConfig(true);
        tairManager.init();
    }

    //第一种方式。incr/decr
    public static boolean incr(String key){
        Result<Integer> result = tairManager.incr(NAMESPACE,key,1,0, EXPIRE_TIME, 0,1);
        if (result.isSuccess()){

            return true;
        }
        return false;
    }
    public static boolean decr(String key){
        Result<Integer> result = tairManager.decr(NAMESPACE,key,1,0, EXPIRE_TIME, 0,1);
        if (result.isSuccess()){
            return true;
        }
        return false;
    }

    //第二种方式。get/put
    /**
     * 尝试获取锁，get或put失败都分别进行重试，支持重入
     * @param key
     * @return
     */
    public static boolean tryLock(String key, String reqValue) {
        int retryGet = 0;
        Result<DataEntry> result = null;
        while (retryGet++ < LOCK_GET_MAX_RETRY && isGetFail(result)) // 支持重试
        {
            result = tairManager.get(NAMESPACE, key);
        }

        if (result == null) {
            System.out.println("[TairLock] tryLock, maybe Tair service  is unavailable");
            return false;
        }

        String value = reqValue;
        if (ResultCode.DATANOTEXSITS.equals(result.getRc())) {
            if(value==null){
                value = genValue();
            }
            ResultCode code = null;
            int retryPut = 0;
            while (retryPut++ < LOCK_PUT_MAX_RETRY && isPutFail(code)) // 支持重试
            {
                code = tairManager.put(NAMESPACE, key, value, DEFAULT_VERSION, EXPIRE_TIME);
            }
            if (code != null && code.isSuccess()) {
                currentThreadValue.set(value);
                System.out.println(String.format("[TairLock]tryLock success, key=%s, value=%s",key,value));
                return true;
            }else{
                System.out.println(String.format("[TairLock]tryLock fail,  key=%s, tairLockManager.putValue.ResultCode=%s"
                    , key,code));
                return false;
            }
        }else{
            if(reqValue!=null && result.getValue()!=null && reqValue.equals(result.getValue().getValue())){
                System.out.println(String.format("[TairLock]tryLock true,reenterable lock,  key=%s, reqValue=%s"
                    , key, reqValue));
                return true;
            }

            System.out.println(String.format("[TairLock]tryLock fail,  key=%s, tairLockManager.getValue.getRc()=%s"
                , key,result.getRc()));
            return false;
        }
    }

    /**
     * 不可重入
     * @param key
     * @return
     */
    public static boolean tryLock(String key) {
        return tryLock(key, null);
    }

    /**
     * 自旋 wait重试获取锁,与unlock配对使用
     * @param key
     * @return
     */
    public static boolean tryLockWait(String key) {
        key = key.intern();//用常量池，确保获取的是同一个监视器

        boolean rs = tryLock(key);
        if(rs){
            System.out.println(String.format("[TairLock]tryLockWait success, key=%s",key));
            return rs;
        }else{
            System.out.println(String.format("[TairLock]tryLockWait, key=%s",key));
            long alreadyWaitSec = 0;
            synchronized (key) {
                for(int i=0;i<LOCK_GET_WAIT_TIMES;i++){
                    if (alreadyWaitSec > LOCK_GET_WAIT_TOTAL_TIME) {
                        System.out.println(String.format("[TairLock]tryLockWait 超过循环等待最大时间=%s(ms)，跳出循环，不再等待"
                            ,LOCK_GET_WAIT_TOTAL_TIME));
                        return false;
                    }
                    long waitFor = LOCK_GET_WAIT_BASE_TIME +
                        Math.round(Math.pow(2, i) * LOCK_GET_WAIT_GAP);
                    if (alreadyWaitSec + waitFor > LOCK_GET_WAIT_TOTAL_TIME) {// 累加后超过最大等待时间
                        System.out.println(String.format("[TairLock]tryLockWait Wait %d Sec For Continue (the last time), Now Already Wait For %d ms.",
                            LOCK_GET_WAIT_TOTAL_TIME - alreadyWaitSec, alreadyWaitSec));
                        try {
                            key.wait(LOCK_GET_WAIT_TOTAL_TIME - alreadyWaitSec);
                        } catch (InterruptedException e) {
                            System.out.println("[TairLock] tryLockWait fail InterruptedException");
                            return false;
                        }
                    } else {
                        System.out.println(String.format("[TairLock]tryLockWait Wait %d ms For Continue, Now Already Wait For %d ms."
                            , waitFor, alreadyWaitSec));
                        try {
                            key.wait(waitFor);
                        } catch (InterruptedException e) {
                            System.out.println("[TairLock] tryLockWait fail InterruptedException");
                            return false;
                        }
                    }

                    alreadyWaitSec += waitFor;
                    //wait后重试
                    rs = tryLock(key);
                    if(rs){
                        try {//唤醒其他线程
                            key.notifyAll();
                        } catch (Exception e) {
                            System.out.println("[TairLock] unlock notifyAll fail");
                        }

                        System.out.println(String.format("[TairLock]tryLockWait success, key=%s, alreadyWaitSec=%s"
                            ,key,alreadyWaitSec));
                        return rs;
                    }
                }

                System.out.println(String.format("[TairLock]tryLockWait failxceed max retry times=%s, key=%s"
                    ,LOCK_GET_WAIT_TIMES, key));
                return rs;
            }
        }
    }


    /**
     * 判断是否是当前线程持有的锁，释放锁
     */
    public static boolean unlock(String key) {
        Result<DataEntry> result = tairManager.get(NAMESPACE,key);
        String threadValue = currentThreadValue.get();
        if (threadValue != null && result != null && result.getValue()!=null) {
            String value = (String)result.getValue().getValue();
            if(threadValue.equals(value)){
                ResultCode rc = tairManager.invalid(NAMESPACE,key);
                if (rc!=null && rc.isSuccess()) {
                    System.out.println(String.format("[TairLock]unlock success, key=%s, threadValue=%s"
                        ,key,threadValue));
                    currentThreadValue.remove();
                    return true;
                }else{
                    System.out.println(String.format("[TairLock]unlock failed, tairLockManager.invalidValue fail, key=%s, ResultCode=%s"
                        ,key, rc));
                    return false;
                }
            }else{
                System.out.println(String.format("[TairLock]unlock failed,value is not equal threadValue, key=%s, threadValue=%s, value=%s"
                    ,key,threadValue, value));
                return false;
            }
        }
        System.out.println(String.format("[TairLock]unlock failed, key=%s, threadValue=%s, result=%s"
            ,key,threadValue, result));
        return false;
    }

    /**
     * @param result
     * @return
     */
    private static boolean isGetFail(Result<DataEntry> result) {
        return result == null || ResultCode.CONNERROR.equals(result.getRc())
            || ResultCode.TIMEOUT.equals(result.getRc())
            || ResultCode.UNKNOW.equals(result.getRc());
    }

    /**
     * @param code
     * @return
     */
    private static boolean isPutFail(ResultCode code) {
        return code == null || ResultCode.CONNERROR.equals(code)
            || ResultCode.TIMEOUT.equals(code)
            || ResultCode.UNKNOW.equals(code);
    }

    /**
     * 用于防止被其他线程非法unlock
     * @return
     */
    private static String genValue() {
        String hostname = "UNKNOW_HOST";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.out.println("[TairLock]unknown Host: " + e.getMessage());
        }
        return hostname + "__" + Thread.currentThread().getName() + "__"
            + UUID.randomUUID();
    }


    public static void main(String[] args) throws Exception {
        String key = "test1";
        //    	Result<Integer> rs = tairManager.incr(NAMESPACE, key, 1, 0, 20, 0, 1);
        //    	System.out.println("rs1: "+rs);
        //    	 rs = tairManager.incr(NAMESPACE, key, 1, 0, 20, 0, 1);
        //    	System.out.println("rs2: "+rs);
        //    	 rs = tairManager.decr(NAMESPACE, key, 1, 0, 20, 0, 1);
        //     	System.out.println("rs3: "+rs);
        //     	rs = tairManager.decr(NAMESPACE, key, 1, 0, 20, 0, 1);
        //     	System.out.println("rs4: "+rs);
        //     	rs = tairManager.incr(NAMESPACE, key, 1, 0, 1, 0, 1);
        //     	System.out.println("rs5: "+rs);
        //    	Thread.currentThread().sleep(1201);
        //     	rs = tairManager.incr(NAMESPACE, key, 1, 0, 1, 0, 1);
        //     	System.out.println("rs6: "+rs);

        boolean rs = tryLockWait(key);
        System.out.println("[TairLock] tryLockWait result:"+rs);
        rs = tryLockWait(key);
        System.out.println("[TairLock] tryLockWait result:"+rs);


        //重入测试
        System.out.println("\n[TairLock] 重入测试");
        key = "test2";
        boolean rs2 = tryLock(key,Thread.currentThread().getName());
        System.out.println("[TairLock] tryLock result:"+rs2);
        rs2 = tryLock(key,Thread.currentThread().getName());
        System.out.println("[TairLock] tryLock result:"+rs2);

    }

}
