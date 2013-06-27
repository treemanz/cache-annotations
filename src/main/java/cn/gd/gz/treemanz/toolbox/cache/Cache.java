package cn.gd.gz.treemanz.toolbox.cache;

import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;

/**
 * generic cache interface
 *
 * @author lzj
 * @param <K>
 * @param <V>
 */
public interface Cache<K, V> {

    /**
     * get from cache
     *
     * @param key
     * @return
     */
    public V get(K key) throws CacheException;

    /**
     * set into cache
     *
     * @param key
     * @param value
     */
    public void set(K key, V value) throws CacheException;

    /**
     * set into cache
     *
     * @param key
     * @param value
     * @param ttl
     *            : time to live
     */
    public void set(K key, V value, long ttl) throws CacheException;

    /**
     * delete from cache
     *
     * @param key
     * @return
     */
    public boolean delete(K key) throws CacheException;

}
