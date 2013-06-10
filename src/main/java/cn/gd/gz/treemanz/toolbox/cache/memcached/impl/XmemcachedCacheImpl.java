/**
 * @(#)XmemcachedCacheImpl.java, 2012-3-12. Copyright 2012 Netease, Inc. All
 *                               rights reserved. NETEASE
 *                               PROPRIETARY/CONFIDENTIAL. Use is subject to
 *                               license terms.
 */
package cn.gd.gz.treemanz.toolbox.cache.memcached.impl;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

import cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache;

/**
 * @author Treeman
 */
public class XmemcachedCacheImpl implements MemcachedCache {

    private static Logger logger = Logger.getLogger(XmemcachedCacheImpl.class);

    private MemcachedClient memcachedClient = null;

    /**
     * @param memcachedClient
     *            the memcachedClient to set
     */
    public void setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }
    
    public void setCache(MemcachedClient cache) {
        this.memcachedClient = cache;
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.Cache#get(java.lang.Object)
     */
    public Object get(String key) {
        try {
            return memcachedClient.get(key);
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.Cache#set(java.lang.Object,
     * java.lang.Object)
     */
    public void set(String key, Object value) {
        this.set(key, value, -1L);
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.Cache#set(java.lang.Object,
     * java.lang.Object, long)
     */
    public void set(String key, Object value, long ttl) {
        if (0 > ttl) {
            ttl = 0;
        }
        try {
            memcachedClient.set(key, (int) ttl, value);
        } catch (TimeoutException e) {
            e.printStackTrace();
            logger.error("set not success", e);
            throw new RuntimeException("set not success", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("set not success", e);
            throw new RuntimeException("set not success", e);
        } catch (MemcachedException e) {
            e.printStackTrace();
            logger.error("set not success", e);
            throw new RuntimeException("set not success", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.Cache#delete(java.lang.Object)
     */
    public boolean delete(String key) {
        try {
            return memcachedClient.delete(key);
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#getMulti(java
     * .lang.String[])
     */
    public Map<String, Object> getMulti(String[] keys) {
        try {
            return memcachedClient.get(Arrays.asList(keys));
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#getMultiArray
     * (java.lang.String[])
     */
    public Object[] getMultiArray(String[] keys) {
        Map<String, Object> map = this.getMulti(keys);
        if (null != map) {
            return map.values().toArray();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#getMultiArray
     * (java.lang.String[], java.lang.Integer[])
     */
    public Object[] getMultiArray(String[] keys, Integer[] hashCodes) {
        if (null == keys || null == hashCodes
                || keys.length != hashCodes.length) {
            throw new InvalidParameterException();
        }
        String[] newKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            newKeys[i] = keys[i] + hashCodes[i];
        }
        return getMultiArray(newKeys);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#storeCounter(
     * java.lang.String, long)
     */
    public boolean storeCounter(String key, long counter) {
        try {
            memcachedClient.getCounter(key, counter).get();
            return true;
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#getCounter(java
     * .lang.String)
     */
    public long getCounter(String key) {
        try {
            return memcachedClient.getCounter(key).get();
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#incr(java.lang
     * .String)
     */
    public long incr(String key) {
        return this.incr(key, 1);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#incr(java.lang
     * .String, long)
     */
    public long incr(String key, long inc) {
        try {
            return memcachedClient.incr(key, inc);
        } catch (TimeoutException e) {
            e.printStackTrace();
            logger.error("incr not success", e);
            throw new RuntimeException("incr not success", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("incr not success", e);
            throw new RuntimeException("incr not success", e);
        } catch (MemcachedException e) {
            e.printStackTrace();
            logger.error("incr not success", e);
            throw new RuntimeException("incr not success", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#decr(java.lang
     * .String)
     */
    public long decr(String key) {
        return this.decr(key, -1);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#decr(java.lang
     * .String, long)
     */
    public long decr(String key, long dec) {
        try {
            return memcachedClient.decr(key, dec);
        } catch (TimeoutException e) {
            e.printStackTrace();
            logger.error("decr not success", e);
            throw new RuntimeException("decr not success", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("decr not success", e);
            throw new RuntimeException("decr not success", e);
        } catch (MemcachedException e) {
            e.printStackTrace();
            logger.error("decr not success", e);
            throw new RuntimeException("decr not success", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrIncr(java
     * .lang.String)
     */
    public long addOrIncr(String key) {
        return this.incr(key);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrIncr(java
     * .lang.String, long)
     */
    public long addOrIncr(String key, long inc) {
        try {
            return memcachedClient.getCounter(key).addAndGet(inc);
        } catch (MemcachedException e) {
            e.printStackTrace();
            logger.error("addOrIncr not success", e);
            throw new RuntimeException("addOrIncr not success", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("addOrIncr not success", e);
            throw new RuntimeException("addOrIncr not success", e);
        } catch (TimeoutException e) {
            e.printStackTrace();
            logger.error("addOrIncr not success", e);
            throw new RuntimeException("addOrIncr not success", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrDecr(java
     * .lang.String)
     */
    public long addOrDecr(String key) {
        return this.decr(key);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrDecr(java
     * .lang.String, long)
     */
    public long addOrDecr(String key, long dec) {
        try {
            return memcachedClient.getCounter(key).addAndGet(dec);
        } catch (MemcachedException e) {
            e.printStackTrace();
            logger.error("addOrDecr not success", e);
            throw new RuntimeException("addOrDecr not success", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("addOrDecr not success", e);
            throw new RuntimeException("addOrDecr not success", e);
        } catch (TimeoutException e) {
            e.printStackTrace();
            logger.error("addOrDecr not success", e);
            throw new RuntimeException("addOrDecr not success", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#set(java.lang
     * .String, java.lang.Object, int)
     */
    public void set(String key, Object value, int hashCode) {
        this.set(key + hashCode, value);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#set(java.lang
     * .String, java.lang.Object, int, long)
     */
    public void set(String key, Object value, int hashCode, long ttl) {
        this.set(key + hashCode, value, ttl);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#get(java.lang
     * .String, int)
     */
    public Object get(String key, int hashCode) {
        return this.get(key + hashCode);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#delete(java.lang
     * .String, int)
     */
    public boolean delete(String key, int hashCode) {
        return this.delete(key + hashCode);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#containsKey(java
     * .lang.String)
     */
    public boolean containsKey(String key) {
        return this.get(key) != null;
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#keySet()
     */
    public Set<String> keySet() {
        throw new UnsupportedOperationException();
    }

}
