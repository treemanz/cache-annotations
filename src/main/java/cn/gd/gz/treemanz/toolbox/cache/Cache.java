package cn.gd.gz.treemanz.toolbox.cache;

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
    public V get(K key);

    /**
     * set into cache
     *
     * @param key
     * @param value
     */
    public void set(K key, V value);

    /**
     * set into cache
     *
     * @param key
     * @param value
     * @param ttl
     *            : time to live
     */
    public void set(K key, V value, long ttl);

    /**
     * delete from cache
     *
     * @param key
     * @return
     */
    public boolean delete(K key);

}
