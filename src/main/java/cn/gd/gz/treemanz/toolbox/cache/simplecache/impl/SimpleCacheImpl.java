package cn.gd.gz.treemanz.toolbox.cache.simplecache.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cn.gd.gz.treemanz.toolbox.cache.simplecache.SimpleCache;

public class SimpleCacheImpl implements SimpleCache {

    private static final Logger logger = Logger.getLogger(SimpleCacheImpl.class);

    /**
     * 具体内容存放的地方
     */
    ConcurrentHashMap<String, Object>[] caches;

    /**
     * 超期信息存储
     */
    ConcurrentHashMap<String, Long> expiryCache;

    /**
     * 清理超期内容的服务
     */
    private ScheduledExecutorService scheduleService;

    /**
     * 清理超期信息的时间间隔，默认1分钟
     */
    private int expiryInterval = 1;

    /**
     * 内部cache的个数，根据key的hash对module取模来定位到具体的某一个内部的Map， 减小阻塞情况发生。
     */
    private int moduleSize = 10;

    public SimpleCacheImpl() {
        init();
    }

    public SimpleCacheImpl(int expiryInterval, int moduleSize) {
        this.expiryInterval = expiryInterval;
        this.moduleSize = moduleSize;
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        caches = new ConcurrentHashMap[moduleSize];

        for (int i = 0; i < moduleSize; i++) {
            caches[i] = new ConcurrentHashMap<String, Object>();
        }

        expiryCache = new ConcurrentHashMap<String, Long>();

        scheduleService = Executors.newScheduledThreadPool(1);

        scheduleService.scheduleAtFixedRate(new CheckExpiredJob(caches, expiryCache), 0, expiryInterval * 60,
                TimeUnit.SECONDS);

        if (logger.isInfoEnabled()) {
            logger.info("DefaultCache CheckService is start!");
        }
    }

    private ConcurrentHashMap<String, Object> getCache(String key) {
        long hashCode = key.hashCode();

        if (hashCode < 0) {
            hashCode = -hashCode;
        }

        int moudleNum = (int) hashCode % moduleSize;

        return caches[moudleNum];
    }

    private void checkExpire(String key) {
        Long expiry = expiryCache.get(key);
        if (expiry != null && expiry != -1 && new Date(expiry).before(new Date())) {
            getCache(key).remove(key);
            expiryCache.remove(key);
        } else if (expiry != null && expiry != -1) {
            logger.debug(new Date(expiry) + " - " + new Date());
        }
    }

    public boolean delete(String key) {
        Object o = getCache(key).remove(key);
        if (null != o) {
            Long l = expiryCache.remove(key);
            logger.debug("delete key : " + key + " - value : " + o.toString() + " - expiry : " + l + " | current : "
                    + System.currentTimeMillis());
        }
        return true;
    }

    public Object get(String key) {
        checkExpire(key);
        return getCache(key).get(key);
    }

    public void set(String key, Object value, long ttl) {
        getCache(key).put(key, value);

        if (ttl == -1 || ttl == 0) {
            expiryCache.put(key, -1L);
        } else {
            expiryCache.put(key, System.currentTimeMillis() + ttl * 1000); // 去掉Calendar的实现
        }
    }

    public void set(String key, Object value) {
        set(key, value, -1);
    }

    class CheckExpiredJob implements Runnable {
        /**
         * 具体内容存放的地方
         */
        ConcurrentHashMap<String, Object>[] caches;

        /**
         * 超期信息存储
         */
        ConcurrentHashMap<String, Long> expiryCache;

        public CheckExpiredJob(ConcurrentHashMap<String, Object>[] caches, ConcurrentHashMap<String, Long> expiryCache) {
            this.caches = caches;
            this.expiryCache = expiryCache;
        }

        public void run() {
            check();
        }

        public void check() {
            try {
                for (ConcurrentHashMap<String, Object> cache: caches) {
                    Iterator<String> keys = cache.keySet().iterator();

                    while (keys.hasNext()) {
                        String key = keys.next();

                        if (expiryCache.get(key) == null) {
                            continue;
                        }

                        long date = expiryCache.get(key);

                        if ((date > 0) && (new Date(date).before(new Date()))) {
                            expiryCache.remove(key);
                            cache.remove(key);
                        }

                    }

                }
            } catch (Exception ex) {
                logger.info("DefaultCache CheckService is start!");
            }
        }

    }
}
