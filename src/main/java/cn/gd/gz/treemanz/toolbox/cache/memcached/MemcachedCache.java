package cn.gd.gz.treemanz.toolbox.cache.memcached;

import java.util.Map;
import java.util.Set;

import cn.gd.gz.treemanz.toolbox.cache.Cache;
import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;

public interface MemcachedCache extends Cache<String, Object> {

    public Map<String, Object> getMulti(String[] keys) throws CacheException;

    public Object[] getMultiArray(String[] keys) throws CacheException;

    public Object[] getMultiArray(String[] keys, Integer[] hashCodes) throws CacheException;

    public boolean storeCounter(String key, long counter) throws CacheException;

    public long getCounter(String key) throws CacheException;

    public long incr(String key) throws CacheException;

    public long incr(String key, long inc) throws CacheException;

    public long decr(String key) throws CacheException;

    public long decr(String key, long dec) throws CacheException;

    public long addOrIncr(String key) throws CacheException;

    public long addOrIncr(String key, long inc) throws CacheException;

    public long addOrDecr(String key) throws CacheException;

    public long addOrDecr(String key, long dec) throws CacheException;

    public void set(String key, Object value, int hashCode) throws CacheException;

    public void set(String key, Object value, int hashCode, long ttl) throws CacheException;

    public Object get(String key, int hashCode) throws CacheException;

    public boolean delete(String key, int hashCode) throws CacheException;

    public boolean containsKey(String key) throws CacheException;

    public Set<String> keySet() throws CacheException;

}
