package cn.gd.gz.treemanz.toolbox.cache.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import cn.gd.gz.treemanz.toolbox.cache.TwoTuple;
import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheAidMethod;
import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheKey;
import cn.gd.gz.treemanz.toolbox.cache.annotation.Caching;
import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;
import cn.gd.gz.treemanz.toolbox.cache.memcached.MemcachedCache;

/**
 * @author Treeman
 */
public class MemcachedCachingInterceptor extends CachingInterceptor {

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.interceptor.CachingInterceptor#init()
     */
    @Override
    public void init() {
        super.init();
    }

    /*
     * (non-Javadoc)
     * @see
     * cn.gd.gz.treemanz.toolbox.cache.interceptor.CachingInterceptor#shutdown()
     */
    @Override
    public void shutdown() {
        if (null != executorService) {
            executorService.shutdown();
        }
        super.shutdown();
    }

    private static final Logger logger = Logger
            .getLogger(MemcachedCachingInterceptor.class);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * @return
     */
    private MemcachedCache getMemcachedCache() {
        return (MemcachedCache) cache;
    }

    protected Map<String, Object> getMultiFromCache(String[] keys) {
        try {
            for (int i = 0; i < keys.length; i++) {
                keys[i] = getSafeKey(keys[i]);
            }
            return getMemcachedCache().getMulti(keys);
        } catch (CacheException e) {
            logger.error("getMulti from cache error,ex=" + e.getMessage(), e);
            return null;
        }
    }

    protected TwoTuple<String, Map<String, String>> getCacheVersions(
            String keyPrefix, String[] masterKeys) {
        String version0 = (String) getFromCache("VER_" + keyPrefix);
        if (StringUtils.isBlank(version0)) {
            version0 = "NoVer0";
        }
        // String[] version1CacheKeys = new String[masterKeys.length];
        Map<String, String> version1CacheKeyMasterKeyMap = new HashMap<String, String>();
        for (String masterKey: masterKeys) {
            version1CacheKeyMasterKeyMap.put("VER_" + keyPrefix + "_"
                    + masterKey, masterKey);
        }
        Map<String, Object> version1MapRaw = getMultiFromCache(version1CacheKeyMasterKeyMap
                .keySet().toArray(new String[0]));
        if (null == version1MapRaw) {
            version1MapRaw = new HashMap<String, Object>();
        }
        // System.out.print("####################");
        // for (String version1CacheKey: version1CacheKeyMasterKeyMap.keySet())
        // {
        // System.out.print(version1CacheKey
        // + "("
        // + version1MapRaw.get(version1CacheKey)
        // + "|"
        // + (version1MapRaw.get(version1CacheKey) != null ?
        // version1MapRaw.get(version1CacheKey).getClass()
        // : "") + "),");
        // }
        // System.out.println("\n----version1MapRaw.size()=" +
        // version1MapRaw.size());
        Map<String, String> version1Map = new HashMap<String, String>();
        for (Entry<String, String> entry: version1CacheKeyMasterKeyMap
                .entrySet()) {
            String version1CacheKey = entry.getKey();
            String masterKey = entry.getValue();
            Object obj = version1MapRaw.get(version1CacheKey);
            if (null != obj && obj instanceof String) {
                String version1 = (String) obj;
                if (StringUtils.isBlank(version1)) {
                    version1 = "NoVer1";
                }
                version1Map.put(masterKey, version1);
            }
        }
        // for (String masterKey: version1CacheKeyMasterKeyMap.values()) {
        // System.out.print(masterKey + "(" + version1Map.get(masterKey) +
        // "),");
        // }
        // System.out.println("\n----version1MapRaw.size()=" +
        // version1MapRaw.size());
        return new TwoTuple<String, Map<String, String>>(version0, version1Map);
    }

    protected Map<String, String> getCacheKeyMap(String keyPrefix,
            String[] masterKeys) {
        TwoTuple<String, Map<String, String>> tuple = getCacheVersions(
                keyPrefix, masterKeys);
        Map<String, String> cacheKeyMap = new HashMap<String, String>(
                masterKeys.length);
        for (String masterKey: masterKeys) {
            cacheKeyMap.put(masterKey, keyPrefix + "_" + tuple.getFirst() + "_"
                    + masterKey + "_" + tuple.getSecond().get(masterKey)
                    + "_NoParam");
        }
        return cacheKeyMap;
    }

    protected Executor getExecutor() {
        return executorService;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object result = null;

        Caching caching = null;
        Caching.Event event = null;

        if (method.isAnnotationPresent(Caching.class)) {
            caching = method.getAnnotation(Caching.class);
            event = caching.event();
        }

        if (null != caching) {

            switch (event) {
                case GET_MULTI:
                    logger.debug("event_getMulti: "
                            + invocation.getMethod().getName());
                    result = getMulti(caching, invocation);
                    break;
                case LIST:
                    logger.debug("event_list: "
                            + invocation.getMethod().getName());
                    result = list(caching, invocation);
                    break;
                case DEL_LIST:
                    logger.debug("event_delList: "
                            + invocation.getMethod().getName());
                    result = delList(caching, invocation);
                    break;
                default:
                    result = super.invoke(invocation);
            }

        } else {
            result = invocation.proceed();
        }

        return result;
    }

    /**
     * get[]类接口，前环绕，根据ids来获取对象集合，将对不命中部分id访问方法体(类似GET)
     * <p>
     * 规则：所有参数入参，因为getMulti约定所有参数都是id
     * 
     * @param caching
     * @param invocation
     * @return
     * @throws Throwable
     */
    protected Object getMulti(Caching caching, MethodInvocation invocation)
            throws Throwable {

        Object[] args = invocation.getArguments();
        // check args
        if (null == args || args.length == 0) {
            return invocation.proceed();
        }

        String keyPrefix = getKeyPrefix(caching, null);
        long ttl = getTtl(caching, null);
        if (StringUtils.isBlank(keyPrefix)) {
            throw new RuntimeException(
                    "use MemcachedCacheInterceptor.getMulti, but do not set the keyPrefix");
        }
        return getMulti(keyPrefix, args, invocation, ttl);

    }

    @SuppressWarnings("unchecked")
    private Map<Object, ? extends Object> getMulti(String keyPrefix,
            Object[] keySuffixs, MethodInvocation invocation, long ttl)
            throws Throwable {
        if (null == keySuffixs || keySuffixs.length == 0) {
            return new HashMap<Object, Object>(0);
        }
        logger.debug("getMulti-required:" + Arrays.toString(keySuffixs));
        Map<String, Object> masterKeyKeySuffixMap = new HashMap<String, Object>(
                keySuffixs.length);
        for (int i = 0; i < keySuffixs.length; i++) {
            masterKeyKeySuffixMap.put(keySuffixs[i].toString(), keySuffixs[i]);
        }
        Map<String, String> cacheKeyMap = getCacheKeyMap(keyPrefix,
                masterKeyKeySuffixMap.keySet().toArray(new String[0]));

        Map<String, Object> resultMap = getMultiFromCache(cacheKeyMap.values()
                .toArray(new String[0]));
        HashMap<Object, Object> returnMap = new HashMap<Object, Object>();
        List<Object> missList = new ArrayList<Object>();
        for (Entry<String, String> entry: cacheKeyMap.entrySet()) {
            String masterKey = entry.getKey();
            String cacheKey = entry.getValue();
            Object keySuffix = masterKeyKeySuffixMap.get(masterKey);
            Object v = resultMap.get(cacheKey);
            if (null == v) {
                missList.add(keySuffix);
            } else {
                returnMap.put(keySuffix, v);
            }
        }
        logger.debug("getMulti-notfound:" + Arrays.toString(missList.toArray()));
        Map<Long, Object> dbResultMap = (Map<Long, Object>) invocation
                .getMethod().invoke(invocation.getThis(), new Object[] {
                    missList.toArray(new Long[0])
                });

        returnMap.putAll(dbResultMap);
        setMultiAsynchronous(keyPrefix, dbResultMap, ttl);
        return returnMap;
    }

    @SuppressWarnings("rawtypes")
    protected void setMulti(String keyPrefix, Map map, long ttl) {
        for (Object k: map.keySet()) {
            setToCache(getCacheKey(keyPrefix, k.toString()), map.get(k), ttl);
        }
    }

    @SuppressWarnings("rawtypes")
    protected void setMultiAsynchronous(final String keyPrefix, final Map map,
            final long ttl) {
        try {
            getExecutor().execute(new Runnable() {
                public void run() {
                    setMulti(keyPrefix, map, ttl);
                }
            });
        } catch (Exception e) {
            logger.error("asynchronous set multi use thread pool throw. - "
                    + e.getMessage());
        }

    }

    protected void setAsynchronous(final String key, final Object value,
            final long ttl) {
        try {
            getExecutor().execute(new Runnable() {
                public void run() {
                    setToCache(key, value, ttl);
                }
            });
        } catch (Exception e) {
            logger.error("asynchronous set use thread pool throw. - "
                    + e.getMessage());
        }
    }

    /**
     * list类接口，前环绕，首先访问缓存(keyPrefix +
     * params)获取ids，若命中则调用GET_MULTI获取对象集合；若不命中则访问方法体
     * <p>
     * 规则：
     * <p>
     * 1.@CacheKey标注的入参（可多个）； <br>
     * 2.如果没有标注CacheKey，则表示全部符合条件的属性作为缓存key（①CacheObject注解的CacheKey字段；②Long/
     * Integer/String参数）
     * <p>
     * PS:缓存对象的主键必须为Integer类型
     * 
     * @param caching
     * @param invocation
     * @return
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    protected Object list(Caching caching, MethodInvocation invocation)
            throws Throwable {

        long t1 = System.currentTimeMillis();

        // get listKeyPrefix
        String listKeyPrefix = caching.agentKeyPrefix();
        if (StringUtils.isBlank(listKeyPrefix)) {
            throw new RuntimeException(
                    "use MemcachedCacheInterceptor.list, but do not set the agentKeyPrefix");
        }

        // get eachKeyPrefix
        String eachKeyPrefix = caching.keyPrefix();
        if (StringUtils.isBlank(eachKeyPrefix)) {
            throw new RuntimeException(
                    "use MemcachedCacheInterceptor.list, but do not set the keyPrefix");
        }

        long ttl = getTtl(caching, null);

        long t2 = System.currentTimeMillis();
        logger.debug("[opt:list-2,key=" + listKeyPrefix + ",used:" + (t2 - t1)
                + "]");

        // get key
        KeyTuple listKeyTuple = getKeyTupleFromArguments(invocation);

        // get listkey
        String listCachekey = getCacheKey(listKeyPrefix, listKeyTuple);

        long t3 = System.currentTimeMillis();
        logger.debug("[opt:list-3,key=" + listKeyPrefix + ",used:" + (t3 - t2)
                + "]");

        // get targetIds from cache
        Object _eachIds = getFromCache(listCachekey);

        long t4 = System.currentTimeMillis();
        logger.debug("[opt:list-4,key=" + listKeyPrefix + ",used:" + (t4 - t3)
                + ",_eachIds=" + _eachIds + "]");

        // System.out.println("===listKeyPrefix=" + listKeyPrefix +
        // ",eachKeyPrefix=" + eachKeyPrefix + ",ttl=" + ttl
        // + ",listKeyTuple=<" + listKeyTuple.getMasterKey() + "," +
        // listKeyTuple.getNormalKey()
        // + ">,listCachekey=" + listCachekey + ",_eachIds=" + _eachIds);

        List<String> eachIds = null;
        if (null != _eachIds && _eachIds instanceof List) {
            logger.debug("cmd=list,status=hit,listkey=" + listCachekey);
            eachIds = (List<String>) _eachIds;

            getCacheOut(caching, invocation, listCachekey);

            long t5 = System.currentTimeMillis();
            logger.debug("[opt:list-5,key=" + listKeyPrefix + ",used:"
                    + (t5 - t4) + "]");

            if (eachIds.isEmpty()) {
                return new ArrayList<Object>();
            }

            // find id in cache or from cacheKeyMethod
            List<Object> returnObjectList = new ArrayList<Object>();
            List<String> eachMasterKeys = new ArrayList<String>();
            for (String eachId: eachIds) {
                eachMasterKeys.add(eachId + "_");
            }
            Map<String, String> eachCacheKeyMap = getCacheKeyMap(eachKeyPrefix,
                    eachMasterKeys.toArray(new String[0]));

            long t6 = System.currentTimeMillis();
            logger.debug("[opt:list-6,key=" + listKeyPrefix + ",used:"
                    + (t6 - t5) + "]");

            Map<String, Object> multiObjectMap = getMultiFromCache(eachCacheKeyMap
                    .values().toArray(new String[0]));

            long t7 = System.currentTimeMillis();
            logger.debug("[opt:list-7,key=" + listKeyPrefix + ",used:"
                    + (t7 - t6) + "]");

            // System.out.print("----");
            // for (String eachMasterKey: eachMasterKeys) {
            // System.out.print(eachMasterKey + "(" +
            // eachCacheKeyMap.get(eachMasterKey) + "),");
            // }
            // System.out.println("\n----multiObjectMap.size()=" +
            // multiObjectMap.size());

            List<String> leftEachMasterKeys = null;
            if (null != multiObjectMap && multiObjectMap.size() > 0) {
                // 把getMulti得到的object添加到returnObjectList中
                // returnObjectList.addAll(multiObjectMap.values());
                // 获取getMulti找不到的剩余eachId
                leftEachMasterKeys = new ArrayList<String>();
                for (String eachMasterKey: eachMasterKeys) {
                    String eachCacheKey = eachCacheKeyMap.get(eachMasterKey);
                    if (!multiObjectMap.containsKey(eachCacheKey)
                            || null == multiObjectMap.get(eachCacheKey)) {
                        leftEachMasterKeys.add(eachMasterKey);
                    }
                }
            } else {
                leftEachMasterKeys = eachMasterKeys;
            }

            long t8 = System.currentTimeMillis();
            logger.debug("[opt:list-8,key=" + listKeyPrefix + ",used:"
                    + (t8 - t7) + ",eq="
                    + (multiObjectMap.size() == eachCacheKeyMap.size()) + "|"
                    + multiObjectMap.size() + "|" + eachCacheKeyMap.size()
                    + "]");

            if (multiObjectMap.size() == eachCacheKeyMap.size()) { // 如果列表完整，直接返回
                for (String eachMasterKey: eachMasterKeys) {
                    String eachCacheKey = eachCacheKeyMap.get(eachMasterKey);
                    returnObjectList.add(multiObjectMap.get(eachCacheKey));
                }
                return returnObjectList;
            }

            // 重新获取已失效的对象
            // find cacheKeyMethod
            Method cacheKeyMethod = null;
            for (Method method: invocation.getThis().getClass().getMethods()) {
                if (method.isAnnotationPresent(CacheAidMethod.class)) {
                    CacheAidMethod cacheAidMethod = method
                            .getAnnotation(CacheAidMethod.class);
                    if (cacheAidMethod.method().equals(
                            CacheAidMethod.Method.GetCacheObjectByKey)
                            && cacheAidMethod.keyPrefix().equals(eachKeyPrefix)) {
                        cacheKeyMethod = method;
                        break;
                    }
                }
            }
            if (null == cacheKeyMethod) {
                Class<?>[] interfaces = invocation.getThis().getClass()
                        .getInterfaces();
                if (null != interfaces && interfaces.length > 0) {
                    for (Class<?> interface1: interfaces) {
                        for (Method method: interface1.getMethods()) {
                            if (method
                                    .isAnnotationPresent(CacheAidMethod.class)) {
                                CacheAidMethod cacheAidMethod = method
                                        .getAnnotation(CacheAidMethod.class);
                                if (cacheAidMethod
                                        .method()
                                        .equals(CacheAidMethod.Method.GetCacheObjectByKey)
                                        && cacheAidMethod.keyPrefix().equals(
                                                eachKeyPrefix)) {
                                    cacheKeyMethod = method;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            long t9 = System.currentTimeMillis();
            logger.debug("[opt:list-9,key=" + listKeyPrefix + ",used:"
                    + (t9 - t8) + ",cacheKeyMethod_exists="
                    + (null != cacheKeyMethod) + "]");

            if (null != cacheKeyMethod) {
                // 使用getSingle获取getMulti取不到的object
                for (String eachMasterKey: leftEachMasterKeys) {
                    String eachCacheKey = eachCacheKeyMap.get(eachMasterKey);
                    Object eachObject = getFromCache(eachCacheKey);
                    if (null != eachObject) {
                        logger.debug("cmd=list_each,status=hit,eachKey="
                                + eachCacheKey);
                        // returnObjectList.add(eachObject);
                        multiObjectMap.put(eachCacheKey, eachObject);
                    } else {
                        String eachId = eachMasterKey.split("_")[0];
                        eachObject = cacheKeyMethod.invoke(
                                invocation.getThis(), Integer.parseInt(eachId));
                        if (null != eachObject) {
                            logger.debug("cmd=list_each,status=miss_set,eachKey="
                                    + eachCacheKey);
                            // returnObjectList.add(eachObject);
                            multiObjectMap.put(eachCacheKey, eachObject);
                            setToCache(eachCacheKey, eachObject, ttl);
                        }
                    }
                }
                // done and return
                for (String eachMasterKey: eachMasterKeys) {
                    String eachCacheKey = eachCacheKeyMap.get(eachMasterKey);
                    returnObjectList.add(multiObjectMap.get(eachCacheKey));
                }

                long t10 = System.currentTimeMillis();
                logger.debug("[opt:list-10,key=" + listKeyPrefix + ",used:"
                        + (t10 - t9) + "]");

                return returnObjectList;
            } else {
                logger.debug("use MemcachedCacheInterceptor.list, but do not set the CacheAidMethod, so re-get the total list");
            }
        }

        long t11 = System.currentTimeMillis();
        logger.debug("[opt:list-11-4,key=" + listKeyPrefix + ",used:"
                + (t11 - t4) + "]");

        Object value = invocation.proceed();

        long t12 = System.currentTimeMillis();
        logger.debug("[opt:list-12,key=" + listKeyPrefix + ",used:"
                + (t12 - t11) + "]");

        if (value != null) {
            logger.debug("cmd=list,status=miss_set,listkey=" + listCachekey);
            // set list to cache
            if (value instanceof List) {
                List<Object> eachObjectList = (List<Object>) value;
                Map<String, Object> eachMasterIdObjectMap = new HashMap<String, Object>();
                eachIds = new ArrayList<String>();
                // set eachkey-eachObject to cache
                for (Object eachObject: eachObjectList) {
                    String eachId = null;
                    for (Field field: eachObject.getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(CacheKey.class)) {
                            eachId = BeanUtils.getProperty(eachObject,
                                    field.getName());
                            break;
                        }
                    }
                    if (StringUtils.isNotBlank(eachId)) {
                        eachMasterIdObjectMap.put(eachId + "_", eachObject);
                        eachIds.add(eachId);
                    }
                }

                Map<String, String> eachCacheKeyMap = getCacheKeyMap(
                        eachKeyPrefix,
                        eachMasterIdObjectMap.keySet().toArray(new String[0]));

                long t13 = System.currentTimeMillis();
                logger.debug("[opt:list-13,key=" + listKeyPrefix + ",used:"
                        + (t13 - t12) + "]");

                for (Entry<String, String> entry: eachCacheKeyMap.entrySet()) {
                    String eachMasterId = entry.getKey();
                    String eachCachekey = entry.getValue();
                    logger.debug("cmd=list_each,status=miss_set,eachkey="
                            + eachCachekey);
                    setToCache(eachCachekey,
                            eachMasterIdObjectMap.get(eachMasterId), ttl);
                }

                long t14 = System.currentTimeMillis();
                logger.debug("[opt:list-14,key="
                        + listKeyPrefix
                        + ",used:"
                        + (t14 - t13)
                        + ",eq="
                        + (eachMasterIdObjectMap.size() == eachObjectList
                                .size()) + "|" + eachMasterIdObjectMap.size()
                        + "|" + eachObjectList.size() + "]");

                if (eachMasterIdObjectMap.size() == eachObjectList.size()) {
                    // set listkey-eachIds to cache
                    setToCache(listCachekey, eachIds, ttl);
                    setCacheOut(caching, invocation, listCachekey, ttl);
                }

                long t15 = System.currentTimeMillis();
                logger.debug("[opt:list-15,key=" + listKeyPrefix + ",used:"
                        + (t15 - t14) + "]");
            }
        }

        long t16 = System.currentTimeMillis();
        logger.debug("[opt:list-16-12,key=" + listKeyPrefix + ",used:"
                + (t16 - t12) + "]");

        return value;

    }

    /**
     * 删除List缓存
     * <p>
     * 参数说明参见 {@link MemcachedCachingInterceptor#list}
     * 
     * @param caching
     * @param invocation
     * @return
     * @throws Throwable
     */
    protected Object delList(Caching caching, MethodInvocation invocation)
            throws Throwable {

        // get listKeyPrefixs
        String listKeyPrefixs = caching.agentKeyPrefix();
        if (StringUtils.isBlank(listKeyPrefixs)) {
            throw new RuntimeException(
                    "use MemcachedCacheInterceptor.list, but do not set the agentKeyPrefix");
        }
        String[] listKeyPrefixArray = listKeyPrefixs.split("\\|");
        for (String listKeyPrefix: listKeyPrefixArray) {
            // get key
            KeyTuple listKeyTuple = getKeyTupleFromArguments(invocation);

            // get listkey
            String listCachekey = getCacheKey(listKeyPrefix, listKeyTuple);
            logger.debug("cmd=deleteList,status=delete,key=" + listCachekey);
            // if (!deleteFromCache(listCachekey)) {
            // logger.error("delete from memecached failed, key is " +
            // listCachekey);
            // }

            refreshCacheVersion(listKeyPrefix, listKeyTuple);
        }

        return invocation.proceed();

    }

}
