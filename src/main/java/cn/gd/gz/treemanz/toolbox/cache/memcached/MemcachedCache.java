package cn.gd.gz.treemanz.toolbox.cache.memcached;

import java.util.Map;
import java.util.Set;

import cn.gd.gz.treemanz.toolbox.cache.Cache;

public interface MemcachedCache extends Cache<String, Object> {

	public Map<String, Object> getMulti(String[] keys);

	public Object[] getMultiArray(String[] keys);

	public Object[] getMultiArray(String[] keys, Integer[] hashCodes);

	public boolean storeCounter(String key, long counter);

	public long getCounter(String key);

	public long incr(String key);

	public long incr(String key, long inc);

	public long decr(String key);

	public long decr(String key, long dec);

	public long addOrIncr(String key);

	public long addOrIncr(String key, long inc);

	public long addOrDecr(String key);

	public long addOrDecr(String key, long dec);

	public void set(String key, Object value, int hashCode);

	public void set(String key, Object value, int hashCode, long ttl);

	public Object get(String key, int hashCode);

	public boolean delete(String key, int hashCode);

	public boolean containsKey(String key);

	public Set<String> keySet();
}
