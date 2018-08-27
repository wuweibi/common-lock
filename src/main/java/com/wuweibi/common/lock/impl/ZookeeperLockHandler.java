package com.wuweibi.common.lock.impl;


import com.wuweibi.common.lock.LockHandler;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/** 
 * Zookeeper 分布式锁实现
 * （带有默认前缀）
 *
 * @author marker
 */  
public class ZookeeperLockHandler implements LockHandler {
    // 日志记录
    private static final Logger log = LoggerFactory.getLogger(ZookeeperLockHandler.class);

    // 单个锁有效期
    private static final int DEFAULT_SINGLE_EXPIRE_TIME = 30;

    // 批量锁有效期
    private static final int DEFAULT_BATCH_EXPIRE_TIME = 60;

    // 构造时，注入
    private ZkClient client;



    /**
     * 锁 Key 前缀
     */
    private String prefix = "/lock";



    /**
     * 构造
     * @param client
     */
    public ZookeeperLockHandler(ZkClient client) {
        this.client = client;

        if(!client.exists(prefix)){
            log.debug("lock root path({}) not exits!", prefix);
            client.createPersistent(prefix);
        }
    }



    /** 
     * 获取锁 如果锁可用 立即返回true， 否则返回false，不等待 
     *  
     * @return 
     */
    public boolean tryLock(String key) {  
        return tryLock(key, 0L, null);  
    }  
  
    /** 
     * 锁在给定的等待时间内空闲，则获取锁成功 返回true， 否则返回false 
     *
     * @param key 锁键
     * @param timeout 超时时间
     * @param unit 超时时间单位
     * @return 
     */
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        String lockKey = prefix.concat(keySupport(key));
        log.debug("tryLock key={}", lockKey);
        try {
            // 系统计时器的当前值，以毫微秒为单位。
            long nano = System.nanoTime();  
            do {
                //将 key 的值设为 空 byte  成功 或者 失败 ()
                boolean status = create(lockKey);
                if (status) {
                    log.debug("get lock, key: {} , expire in {} seconds.", lockKey, DEFAULT_SINGLE_EXPIRE_TIME);
                    // 成功获取锁，返回true
                    return Boolean.TRUE;  
                } else { // 存在锁,循环等待锁
                    log.debug("key: {} locked by another business：", lockKey);
                }  
                if (timeout <= 0) {  // 没有设置超时时间，直接退出等待
                    break;  
                }  
                Thread.sleep(30);
            } while ((System.nanoTime() - nano) < unit.toNanos(timeout));  
            return Boolean.FALSE;  
        }  catch (Exception e) {
            log.error("{}", e);
        } finally {

        }  
        return Boolean.FALSE;  
    }  
  
    /** 
     * 如果锁空闲立即返回 获取失败 一直等待 
     */
    public void lock(String key) {
        String lockKey = prefix.concat(keySupport(key));
        try {
            do {
                log.debug("lock key: " + lockKey);
                Boolean status = create(lockKey);
                if (status) {
                    log.debug("get lock, key: {} , expire in {} seconds.", lockKey , DEFAULT_SINGLE_EXPIRE_TIME);
                    return;  
                } else {
                    log.debug("key: {} locked by another business：", lockKey);
                }  
                Thread.sleep(300);  
            } while (true);  
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {

        }  
    }  
  
    /** 
     * 释放锁 
     */
    public void unLock(String key) {
        if(null == key){  key = "null";  }
        List<String> list = new ArrayList<>(1);
        list.add(key);
        unLock(list);  
    }  
  
    /** 
     * 批量获取锁 如果全部获取 立即返回true, 部分获取失败 返回false 
     *  
     * @return 
     */
    public boolean tryLock(List<String> keyList) {  
        return tryLock(keyList, 0L, null);  
    }  
  
    /** 
     * 锁在给定的等待时间内空闲，则获取锁成功 返回true， 否则返回false 
     *  
     * @param timeout 
     * @param unit 
     * @return 
     */
    public boolean tryLock(List<String> keyList, long timeout, TimeUnit unit) {
        try {  
            //需要的锁  
            List<String> needLocking = new CopyOnWriteArrayList<String>();  
            //得到的锁  
            List<String> locked = new CopyOnWriteArrayList<>();

            long nano = System.nanoTime();  
            do {  
                // 构建pipeline，批量提交
                for (String key : keyList) {
                    String lockKey = prefix.concat(keySupport(key));
                    boolean status = create(lockKey);
                    if(status){
                        needLocking.add(lockKey);
                    }
                }
                log.debug("try lock keys: " + needLocking);
                needLocking.removeAll(locked); // 已锁定资源去除  
  
                if (needLocking.size() == 0) { //成功获取全部的锁  
                    return true;  
                } else {  
                    // 部分资源未能锁住  
                    log.debug("keys: {} locked by another business：", needLocking);
                }  
  
                if (timeout == 0) {  
                    break;  
                }  
                Thread.sleep(500);  
            } while ((System.nanoTime() - nano) < unit.toNanos(timeout));  
  
            // 得不到锁，释放锁定的部分对象，并返回失败  
            if (locked.size() > 0) {
                unLock(locked);
            }  
            return false;  
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
        }  
        return true;  
    }  
  
    /** 
     * 批量释放锁 
     */
    public void unLock(List<String> keyList) {
        try {
            for(String path : keyList){
                String lockKey = prefix.concat( keySupport(path));
                client.delete(lockKey);
            }
            log.debug("release lock, keys : {}", keyList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    /**
     * 创建目录
     * @param path 目录
     * @return
     */
    private boolean create(String path){
        String resultPath = "";
        try {
            resultPath = client.create(path,"1", CreateMode.PERSISTENT);
        } catch (Exception e){
            log.debug("path {} exists {}", path, e.getMessage());
        }
        if(resultPath.equals(path)){
            return true;
        }
        return false;
    }


    /**
     * redis key 方式兼容处理
     * @param key key
     * @return
     */
    private String keySupport(String key){
        if(key == null){ return "";}
        return "/".concat(key.replaceAll(":","-"));
    }

}