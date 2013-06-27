package cn.gd.gz.treemanz.toolbox.cache.memcached.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.gd.gz.treemanz.toolbox.cache.Cache;
import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;
import cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache;

// 去掉了对MemcachedCacheImpl的继承关系
public class MockMemcachedCacheImpl implements MemcachedCache {

    protected Cache<String, Object> localCache;

    public void setLocalCache(Cache<String, Object> localCache) {
        this.localCache = localCache;
    }

    public void setCache(Cache<String, Object> cache) {
        this.localCache = cache;
    }

    public boolean delete(String key) throws CacheException {
        return localCache.delete(key);
    }

    public Object get(String key) throws CacheException {
        return localCache.get(key);
    }

    public void set(String key, Object value, long ttl) throws CacheException {
        localCache.set(key, value, ttl);
    }

    public void set(String key, Object value) throws CacheException {
        localCache.set(key, value, -1);
    }

    public long addOrDecr(String key, long dec) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value - dec;
            localCache.set(key, value);
        } else {
            localCache.set(key, dec);
            value = dec;
        }
        return value;
    }

    public long addOrDecr(String key) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value - 1;
            localCache.set(key, value);
        } else {
            localCache.set(key, 1L);
            value = 1L;
        }
        return value;
    }

    public long addOrIncr(String key, long inc) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value + inc;
            localCache.set(key, value);
        } else {
            localCache.set(key, inc);
            value = inc;
        }
        return value;
    }

    public long addOrIncr(String key) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value + 1;
            localCache.set(key, value);
        } else {
            localCache.set(key, 1L);
            value = 1L;
        }
        return value;
    }

    public boolean containsKey(String key) throws CacheException {
        return localCache.get(key) != null;
    }

    public long decr(String key, long dec) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value - dec;
            localCache.set(key, value);
        } else {
            value = -1L;
        }
        return value;
    }

    public long decr(String key) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value - 1;
            localCache.set(key, value);
        } else {
            value = -1L;
        }
        return value;
    }

    public boolean delete(String key, int hashCode) throws CacheException {
        return localCache.delete(key);
    }

    public Object get(String key, int hashCode) throws CacheException {
        return localCache.get(key + hashCode);
    }

    public long getCounter(String key) throws CacheException {
        Object value = localCache.get(key);
        return null == value ? -1L : (Long) value;
    }

    public Map<String, Object> getMulti(String[] keys) throws CacheException {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String key: keys) {
            map.put(key, localCache.get(key));
        }
        return map;
    }

    public Object[] getMultiArray(String[] keys, Integer[] hashCodes) throws CacheException {
        Object[] values = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = localCache.get(keys[i] + hashCodes[i]);
        }
        return values;
    }

    public Object[] getMultiArray(String[] keys) throws CacheException {
        Object[] values = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = localCache.get(keys[i]);
        }
        return values;
    }

    public long incr(String key, long inc) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value + inc;
            localCache.set(key, value);
        } else {
            value = -1L;
        }
        return value;
    }

    public long incr(String key) throws CacheException {
        Long value = (Long) localCache.get(key);
        if (null != value) {
            value = value + 1;
            localCache.set(key, value);
        } else {
            value = -1L;
        }
        return value;
    }

    public Set<String> keySet() throws CacheException {
        throw new UnsupportedOperationException();
    }

    public void set(String key, Object value, int hashCode, long ttl) throws CacheException {
        localCache.set(key + hashCode, value, ttl);
    }

    public void set(String key, Object value, int hashCode) throws CacheException {
        localCache.set(key + hashCode, value);
    }

    public boolean storeCounter(String key, long counter) throws CacheException {
        localCache.set(key, counter);
        return true;
    }

}
