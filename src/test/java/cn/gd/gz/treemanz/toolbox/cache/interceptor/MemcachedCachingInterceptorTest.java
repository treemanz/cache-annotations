package cn.gd.gz.treemanz.toolbox.cache.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheKey;
import cn.gd.gz.treemanz.toolbox.cache.annotation.Caching;

/**
 * 未完成
 * 
 * @author Treeman
 */
public class MemcachedCachingInterceptorTest extends TestCase {

    public static class Entity {
        public Integer a;

        public String b;

        /**
         * @return the a
         */
        public Integer getA() {
            return a;
        }

        /**
         * @param a
         *            the a to set
         */
        public void setA(Integer a) {
            this.a = a;
        }

        /**
         * @return the b
         */
        public String getB() {
            return b;
        }

        /**
         * @param b
         *            the b to set
         */
        public void setB(String b) {
            this.b = b;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return a + "," + b;
        }
    }

    public static class MyInterface {
        @Caching(event = Caching.Event.LIST, agentKeyPrefix = "agentKeyPrefix", keyPrefix = "keyPrefix", ttl = 600)
        public List<Entity> getEntityList(@CacheKey
        int lid, @CacheKey
        int type, int beginId) {
            return testMap.get(lid).get(type);
        }
    }

    private static Map<Integer, Map<Integer, List<Entity>>> testMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testMap = new HashMap<Integer, Map<Integer, List<Entity>>>();
        int lidLen = 3;
        int typeLen = 3;
        int entityLen = 10;
        for (int lid = 0; lid < lidLen; lid++) {
            testMap.put(lid, new HashMap<Integer, List<Entity>>());
            for (int type = 0; type < typeLen; type++) {
                List<Entity> entities = new ArrayList<Entity>();
                for (int i = 0; i < entityLen; i++) {
                    Entity entity = new Entity();
                    entity.setA(lid * 10000 + type * 100 + i);
                    entity.setB(lid + "_" + type + "_" + i);
                    entities.add(entity);
                }
                testMap.get(lid).put(lid, entities);
            }
        }
    }

    public void testGetEntityList() {
        MyInterface myInterface = new MyInterface();
        List<Entity> entities = myInterface.getEntityList(1, 1, 0);
        assertNotNull(entities);
        System.out.println(entities.size());
        for (Entity entity: entities) {
            System.out.println(entity);
        }
    }

}
