package cn.gd.gz.treemanz.toolbox.cache.simplecache.impl;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SimpleCacheImplTest extends TestCase {

    private static SimpleCacheImpl cache = new SimpleCacheImpl();

    public void testDelete() {
        String key = "xyz";
        String value = "def";
        cache.set(key, value);
        Assert.assertEquals(value, cache.get(key));
        cache.delete(key);
        Assert.assertNull(cache.get(key));
    }

    public void testGet() {
        // the same as testSetStringObject()
    }

    public void testSetStringObjectLong() {
        String key = "abc";
        String value = "def";
        cache.set(key, value, 10);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {}
        Assert.assertEquals(value, cache.get(key));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {}
        Assert.assertNull(cache.get(key));
    }

    public void testSetStringObject() {
        String key = "123";
        String value = "def";
        cache.set(key, value);
        Assert.assertEquals(value, cache.get(key));
    }

}
