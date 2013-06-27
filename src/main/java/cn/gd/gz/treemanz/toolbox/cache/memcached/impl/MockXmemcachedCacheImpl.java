package cn.gd.gz.treemanz.toolbox.cache.memcached.impl;

import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;
import cn.gd.gz.treemanz.toolbox.cache.memcached.XmemcachedCache;
import cn.gd.gz.treemanz.toolbox.cache.vo.DefaultValueCASOperation;

/**
 * @author Treeman
 */
public class MockXmemcachedCacheImpl extends MockMemcachedCacheImpl implements XmemcachedCache {

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.memcached.XmemcachedCache#cas(java.lang
     * .String, int cn.gd.gz.treemanz.toolbox.cache.vo.DefaultValueCASOperation)
     */
    @SuppressWarnings("unchecked")
    public <T> boolean cas(String key, int exp, DefaultValueCASOperation<T> operation) throws CacheException {
        T currentValue = (T) get(key);
        T obj = operation.getNewValue(0, currentValue);
        if (null != obj) {
            this.set(key, obj, (long) exp);
        }
        return true;
    }

}
