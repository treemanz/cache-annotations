package cn.gd.gz.treemanz.toolbox.cache.memcached;

import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;
import cn.gd.gz.treemanz.toolbox.cache.vo.DefaultValueCASOperation;

public interface XmemcachedCache extends MemcachedCache {

    public <T> boolean cas(final String key,  int exp, final DefaultValueCASOperation<T> operation) throws CacheException;

}
