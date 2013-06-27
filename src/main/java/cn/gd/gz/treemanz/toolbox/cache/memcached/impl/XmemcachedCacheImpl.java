package cn.gd.gz.treemanz.toolbox.cache.memcached.impl;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;

import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;
import cn.gd.gz.treemanz.toolbox.cache.memcached.XmemcachedCache;
import cn.gd.gz.treemanz.toolbox.cache.vo.DefaultValueCASOperation;

/**
 * @author Treeman
 */
public class XmemcachedCacheImpl implements XmemcachedCache {

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
    public Object get(String key) throws CacheException {
        try {
            return memcachedClient.get(key);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.Cache#set(java.lang.Object,
     * java.lang.Object)
     */
    public void set(String key, Object value) throws CacheException {
        this.set(key, value, -1L);
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.Cache#set(java.lang.Object,
     * java.lang.Object, long)
     */
    public void set(String key, Object value, long ttl) throws CacheException {
        if (0 > ttl) {
            ttl = 0;
        }
        try {
            memcachedClient.set(key, (int) ttl, value);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.Cache#delete(java.lang.Object)
     */
    public boolean delete(String key) throws CacheException {
        try {
            return memcachedClient.delete(key);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#getMulti(java
     * .lang.String[])
     */
    public Map<String, Object> getMulti(String[] keys) throws CacheException {
        try {
            return memcachedClient.get(Arrays.asList(keys));
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#getMultiArray
     * (java.lang.String[])
     */
    public Object[] getMultiArray(String[] keys) throws CacheException {
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
    public Object[] getMultiArray(String[] keys, Integer[] hashCodes) throws CacheException {
        if (null == keys || null == hashCodes || keys.length != hashCodes.length) {
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
    public boolean storeCounter(String key, long counter) throws CacheException {
        try {
            memcachedClient.getCounter(key, counter).get();
            return true;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#getCounter(java
     * .lang.String)
     */
    public long getCounter(String key) throws CacheException {
        try {
            return memcachedClient.getCounter(key).get();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#incr(java.lang
     * .String)
     */
    public long incr(String key) throws CacheException {
        return this.incr(key, 1);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#incr(java.lang
     * .String, long)
     */
    public long incr(String key, long inc) throws CacheException {
        try {
            return memcachedClient.incr(key, inc);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#decr(java.lang
     * .String)
     */
    public long decr(String key) throws CacheException {
        return this.decr(key, -1);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#decr(java.lang
     * .String, long)
     */
    public long decr(String key, long dec) throws CacheException {
        try {
            return memcachedClient.decr(key, dec);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrIncr(java
     * .lang.String)
     */
    public long addOrIncr(String key) throws CacheException {
        return this.incr(key);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrIncr(java
     * .lang.String, long)
     */
    public long addOrIncr(String key, long inc) throws CacheException {
        try {
            return memcachedClient.getCounter(key).addAndGet(inc);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrDecr(java
     * .lang.String)
     */
    public long addOrDecr(String key) throws CacheException {
        return this.decr(key);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#addOrDecr(java
     * .lang.String, long)
     */
    public long addOrDecr(String key, long dec) throws CacheException {
        try {
            return memcachedClient.getCounter(key).addAndGet(dec);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#set(java.lang
     * .String, java.lang.Object, int)
     */
    public void set(String key, Object value, int hashCode) throws CacheException {
        this.set(key + hashCode, value);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#set(java.lang
     * .String, java.lang.Object, int, long)
     */
    public void set(String key, Object value, int hashCode, long ttl) throws CacheException {
        this.set(key + hashCode, value, ttl);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#get(java.lang
     * .String, int)
     */
    public Object get(String key, int hashCode) throws CacheException {
        return this.get(key + hashCode);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#delete(java.lang
     * .String, int)
     */
    public boolean delete(String key, int hashCode) throws CacheException {
        return this.delete(key + hashCode);
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#containsKey(java
     * .lang.String)
     */
    public boolean containsKey(String key) throws CacheException {
        return this.get(key) != null;
    }

    /*
     * (non-Javadoc)
     * @see cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache#keySet()
     */
    public Set<String> keySet() throws CacheException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.XmemcachedCache#cas(java.lang
     * .String, int, cn.gd.gz.treemanz.toolbox.cache.vo.DefaultValueCASOperation)
     */
    public <T> boolean cas(String key, int exp, DefaultValueCASOperation<T> operation) throws CacheException {
        try {
            GetsResponse<T> getsResponse = memcachedClient.gets(key);
            if (null == getsResponse) {
                memcachedClient.add(key, exp, operation.getDefaultValue());
            }
            return memcachedClient.cas(key, exp, operation);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

}
