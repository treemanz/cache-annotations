package cn.gd.gz.treemanz.toolbox.cache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import cn.gd.gz.treemanz.toolbox.cache.Cache;
import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheAidMethod;
import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheInterface;
import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheKey;
import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheObject;
import cn.gd.gz.treemanz.toolbox.cache.annotation.CacheOut;
import cn.gd.gz.treemanz.toolbox.cache.annotation.Caching;
import cn.gd.gz.treemanz.toolbox.cache.exception.CacheException;

public class CachingInterceptor implements MethodInterceptor {

    public void init() {
        
    }

    public void shutdown() {

    }
    
    private static final Logger logger = Logger.getLogger(CachingInterceptor.class);

    protected long DEFAULT_TTL = -1;

    /**
     * @param dEFAULT_TTL
     *            the dEFAULT_TTL to set
     */
    public void setDEFAULT_TTL(long dEFAULT_TTL) {
        DEFAULT_TTL = dEFAULT_TTL;
    }

    protected Cache<String, Object> cache;

    public void setCache(Cache<String, Object> cache) {
        this.cache = cache;
    }

    protected String getCacheKey(String keyPrefix, KeyTuple keyTuple) {
        String[] versions = getCacheVersion(keyPrefix, keyTuple.getMasterKey());
        return keyPrefix + "_" + versions[0] + "_" + keyTuple.getMasterKey() + "_" + versions[1] + "_"
                + keyTuple.getNormalKey();
    }

    protected String getCacheKey(String keyPrefix, String masterKey) {
        String[] versions = getCacheVersion(keyPrefix, masterKey);
        return keyPrefix + "_" + versions[0] + "_" + masterKey + "_" + versions[1] + "_NoParam";
    }

    protected void refreshCacheVersion(String keyPrefix, KeyTuple keyTuple) {
        // System.out.println("****keyPrefix="+keyPrefix+",keyTuple=<"+keyTuple.getMasterKey()+","+keyTuple.getNormalKey()+">");
        if (keyTuple.getMasterKey().equals("NoParam")) {
            setToCache("VER_" + keyPrefix, String.valueOf(System.currentTimeMillis()), -1);
        }
        setToCache("VER_" + keyPrefix + "_" + keyTuple.getMasterKey(), String.valueOf(System.currentTimeMillis()), -1);
    }

    protected String[] getCacheVersion(String keyPrefix, String masterKey) {
        String version0 = (String) getFromCache("VER_" + keyPrefix);
        if (StringUtils.isBlank(version0)) {
            version0 = "NoVer0";
        }
        String version1 = (String) getFromCache("VER_" + keyPrefix + "_" + masterKey);
        if (StringUtils.isBlank(version1)) {
            version1 = "NoVer1";
        }
        return new String[] {
            version0, version1
        };
    }

    protected String getSafeKey(String key) {
        // if (key.contains("@")) {
        // key = key.replaceAll("@", "_at_");
        // }
        return key;
    }

    protected void setToCache(String key, Object value, long ttl) {
        try {
            key = getSafeKey(key);
            if (ttl > 0) {
                cache.set(key, value, ttl);
            } else {
                cache.set(key, value);
            }
        } catch (CacheException e) {
            logger.error("set to cache error,ex=" + e.getMessage(), e);
        }
    }

    protected Object getFromCache(String key) {
        try {
            key = getSafeKey(key);
            return cache.get(key);
        } catch (CacheException e) {
            logger.error("get from cache error,ex=" + e.getMessage(), e);
            return null;
        }
    }

    protected boolean deleteFromCache(String key) {
        try {
            key = getSafeKey(key);
            return cache.delete(key);
        } catch (CacheException e) {
            logger.error("delete from cache error,ex=" + e.getMessage(), e);
            return false;
        }
    }

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
                case GET:
                    logger.debug("event_get:" + invocation.getMethod().getName());
                    result = get(caching, invocation);
                    break;
                case UPDATE:
                    logger.debug("event_update:" + invocation.getMethod().getName());
                    result = update(caching, invocation);
                    break;
                case SET_RETURN:
                    logger.debug("event_set_return:" + invocation.getMethod().getName());
                    result = setReturn(caching, invocation);
                    break;
                case DEL_PARAM:
                    logger.debug("event_del:" + invocation.getMethod().getName());
                    result = deleteParam(caching, invocation);
                    break;
                default:
                    result = invocation.proceed();
            }

        } else {
            result = invocation.proceed();
        }

        return result;
    }

    /**
     * get类接口，后环绕，先执行方法体，再对接口返回值使用主键进行缓存
     * <p>
     * 规则：1.获取首个@CacheKey注解的参数作为缓存key；2.若以上规则无法获取，则获取首个Long/Integer/
     * String类型的参数作为缓存key
     * 
     * @param caching
     * @param invocation
     * @return
     * @throws Throwable
     */
    protected Object get(Caching caching, MethodInvocation invocation) throws Throwable {
        KeyTuple keyTuple = getKeyTupleFromArguments(invocation);
        Object value = null;

        Class<?> tarClass = getTarClass(invocation);
        String keyPrefix = getKeyPrefix(caching, tarClass);
        long ttl = getTtl(caching, tarClass);

        String cacheKey = getCacheKey(keyPrefix, keyTuple);

        // System.out.println("========get========");
        // System.out.println("keyPrefix="+ keyPrefix+",keyTuple=" +
        // keyTuple.getMasterKey() + ":" + keyTuple.getNormalKey());
        // System.out.println("===================");

        Object _target = getFromCache(cacheKey);
        if (null != _target) {
            logger.debug("cmd=get,status=hit,key=" + cacheKey);
            getCacheOut(caching, invocation, cacheKey);
            return _target;
        } else {
            value = invocation.proceed();
            if (value != null) {
                logger.debug("cmd=get,status=miss_set,key=" + cacheKey);
                setToCache(cacheKey, value, ttl);
                setCacheOut(caching, invocation, cacheKey, ttl);
            }
        }

        return value;
    }

    /**
     * After returning advice<br>
     * update cache with the return object
     * 
     * @param caching
     * @param invocation
     * @return
     * @throws Throwable
     */
    protected Object update(Caching caching, MethodInvocation invocation) throws Throwable {
        KeyTuple keyTuple = getKeyTupleFromArguments(invocation);
        Object value = invocation.proceed();
        if (value == null)
            return value;

        String keyPrefix = getKeyPrefix(caching, value.getClass());
        Class<?> tarClass = getTarClass(invocation);
        long ttl = getTtl(caching, tarClass);

        String cacheKey = getCacheKey(keyPrefix, keyTuple);

        if (null != value) {
            logger.debug("set cache, key=" + cacheKey + ", ttl=" + ttl);
            setToCache(cacheKey, value, ttl);
        }
        return value;
    }

    /**
     * get/update(return)类接口，后环绕，对接口返回值缓存
     * <p>
     * 规则：1.获取首个@CacheKey注解的参数作为缓存key；2.若以上规则无法获取，则获取首个Long/Integer/
     * String类型的参数作为缓存key
     * <p>
     * PS:缓存对象的主键必须为Integer类型
     * 
     * @param caching
     * @param invocation
     * @return
     * @throws Throwable
     */
    protected Object setReturn(Caching caching, MethodInvocation invocation) throws Throwable {
        String agentKeyPrefix = caching.agentKeyPrefix();
        if (StringUtils.isBlank(agentKeyPrefix)) {
            throw new RuntimeException("use CacheInterceptor.setReturn, but do not set the agentKeyPrefix");
        }

        Class<?> tarClass = getTarClass(invocation);
        String realKeyPrefix = getKeyPrefix(caching, tarClass);

        long ttl = getTtl(caching, null);

        // get key
        KeyTuple agentKeyTuple = getKeyTupleFromArguments(invocation);

        // mockKey
        String agentCacheKey = getCacheKey(agentKeyPrefix, agentKeyTuple);

        // get targetId from cache
        Object _realKey = getFromCache(agentCacheKey);
        if (null != _realKey && _realKey instanceof String) {
            String realKey = (String) _realKey;
            logger.debug("cmd=setReturn,status=hit,mockKey=" + agentCacheKey);
            // find cacheKeyMethod
            Method cacheKeyMethod = null;
            for (Method method: invocation.getThis().getClass().getMethods()) {
                if (method.isAnnotationPresent(CacheAidMethod.class)
                        && method.getAnnotation(CacheAidMethod.class).method()
                                .equals(CacheAidMethod.Method.GetCacheObjectByKey)) {
                    cacheKeyMethod = method;
                    break;
                }
            }
            if (null == cacheKeyMethod) {
                Class<?>[] interfaces = invocation.getThis().getClass().getInterfaces();
                if (null != interfaces && interfaces.length > 0) {
                    for (Class<?> interface1: interfaces) {
                        for (Method method: interface1.getMethods()) {
                            if (method.isAnnotationPresent(CacheAidMethod.class)
                                    && method.getAnnotation(CacheAidMethod.class).method()
                                            .equals(CacheAidMethod.Method.GetCacheObjectByKey)) {
                                cacheKeyMethod = method;
                                break;
                            }
                        }
                    }
                }
            }
            if (null != cacheKeyMethod) {
                // find id in cache or from cacheKeyMethod
                String realCacheKey = getCacheKey(realKeyPrefix, realKey);
                Object value = getFromCache(realCacheKey);
                if (null != value) {
                    logger.debug("cmd=setReturn_real,status=hit,realKey=" + realCacheKey);
                } else {
                    value = cacheKeyMethod.invoke(invocation.getThis(), Integer.parseInt(realCacheKey));
                    if (null != value) {
                        logger.debug("cmd=setReturn_real,status=miss_set,realKey=" + realCacheKey);
                        setToCache(realCacheKey, value, ttl);
                    }
                }
                // done and return
                return value;
            } else {
                logger.debug("use CacheInterceptor.setReturn, but do not set the CacheAidMethod, so re-get the object");
            }
        }

        Object value = invocation.proceed();
        logger.debug("cmd=setReturn,status=miss_set,mockKey=" + agentCacheKey);
        if (null != value) {
            StringBuilder realKeySB = new StringBuilder();
            for (Field field: value.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(CacheKey.class)) {
                    realKeySB.append(BeanUtils.getProperty(value, field.getName()));
                }
            }
            String realKey = realKeySB.toString();
            String realCacheKey = getCacheKey(realKeyPrefix, realKey);
            if (StringUtils.isNotBlank(realCacheKey)) {
                logger.debug("cmd=setReturn_real,status=miss_set,realKey=" + realCacheKey);
                // set realKey-value to cache
                setToCache(realCacheKey, value, ttl);
                // set mockKey-realKey to cache
                setToCache(agentCacheKey, realKey, ttl);
            } else {
                logger.warn("use annotation cache set_return but with no key - " + invocation.getMethod().getName());
            }
        }

        return value;

    }

    /**
     * update(no return)/delete类接口，返回后删除指定缓存(keyPrefix + @CacheKey)
     * <p>
     * 规则：1.获取首个@CacheKey注解的参数作为缓存key；2.若以上规则无法获取，
     * 则获取首个CacheObject的CacheKey作为缓存key；3.
     * 若以上规则无法获取，则获取首个Long/Integer/String类型的参数作为缓存key
     * <p>
     * PS:缓存对象的主键必须为Integer类型
     * 
     * @param caching
     * @param invocation
     * @return
     * @throws Throwable
     */
    protected Object deleteParam(Caching caching, MethodInvocation invocation) throws Throwable {
        KeyTuple keyTuple = getKeyTupleFromArguments(invocation);
        String keyPrefix = caching.keyPrefix();
        String cacheKey = getCacheKey(keyPrefix, keyTuple);

        logger.debug("cmd=deleteParam,status=delete,key=" + cacheKey);
        // if (!deleteFromCache(cacheKey)) {
        // logger.error("delete from memecached failed, key is " + cacheKey);
        // }

        refreshCacheVersion(keyPrefix, keyTuple);

        // System.out.println("========deleteParam========");
        // System.out.println("keyPrefix="+ keyPrefix+",keyTuple=" +
        // keyTuple.getMasterKey() + ":" + keyTuple.getNormalKey());
        // System.out.println("===========================");

        return invocation.proceed();
    }

    /**
     * 通过invocation获取返回类型class
     * 
     * @param invocation
     * @return
     */
    protected Class<?> getTarClass(MethodInvocation invocation) {
        Class<?> tarClass = invocation.getMethod().getReturnType();
        if (tarClass.isAnnotationPresent(CacheInterface.class)) {
            Class<?>[] interfaces = invocation.getThis().getClass().getInterfaces();
            if (null != interfaces && interfaces.length > 0) {
                for (Class<?> interface1: interfaces) {
                    Method[] methods = interface1.getMethods();
                    tarClass = null;
                    for (Method method: methods) {
                        if (method.isAnnotationPresent(CacheAidMethod.class)
                                && method.getAnnotation(CacheAidMethod.class).method()
                                        .equals(CacheAidMethod.Method.GetCacheObjectClass)) {
                            try {
                                tarClass = (Class<?>) method.invoke(invocation.getThis());
                                if (null != tarClass) {
                                    if (tarClass.isAnnotationPresent(CacheObject.class)) {
                                        break;
                                    } else {
                                        tarClass = null;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new RuntimeException("use getTarClass, get error(" + e.getMessage()
                                        + ") when invoke CacheGetCacheObjectClassMethod method");
                            }
                        }
                    }
                    if (null != tarClass) {
                        break;
                    }
                }
            }
            if (null == tarClass) {
                throw new RuntimeException(
                        "use getTarClass, but cannot locate the CacheObject. Reason: 1. the returnType(tarClass) of the method is not a CacheObject; 2. maybe cannot find the CacheGetCacheObjectClassMethod Annotation in the tarClass's methods");
            }
        } else if (!tarClass.isAnnotationPresent(CacheObject.class) && !tarClass.equals(Integer.class)
                && !tarClass.equals(Long.class) && !tarClass.equals(String.class)) {
            throw new RuntimeException(
                    "use getTarClass, but the returnType(tarClass) of the method is not a CacheObject or a CacheInterface or the primitive types");
        }
        return tarClass;
    }

    /**
     * 获取缓存key前缀
     * <p>
     * 在Caching设置的优先级比CacheObject的高
     * 
     * @param caching
     * @param clazz
     * @return
     */
    protected String getKeyPrefix(Caching caching, Class<?> clazz) {
        String keyPrefix;
        if (null == caching || StringUtils.isBlank(caching.keyPrefix())) {
            if (null == clazz) {
                keyPrefix = "";
            } else {
                String _prefix = clazz.getAnnotation(CacheObject.class).prefix();
                keyPrefix = StringUtils.isBlank(_prefix) ? clazz.getName() : _prefix;
            }
        } else {
            keyPrefix = caching.keyPrefix();
        }
        return keyPrefix;
    }

    /**
     * 获取缓存的agentKey前缀
     * <p>
     * 如果不在Caching设置，则读取调用的类与方法名来作为缓存名
     * <p>
     * 不过这样可能会产生同Class的同名方法冲突，所以针对这种情况，请手动设置Caching.agentKeyPrefix
     * 
     * @param caching
     * @param invocation
     * @return
     */
    protected String getAgentKeyPrefix(Caching caching, MethodInvocation invocation) {
        String agentKeyPrefix;
        if (null == caching || StringUtils.isBlank(caching.agentKeyPrefix())) {
            if (null == invocation) {
                agentKeyPrefix = "";
            } else {
                agentKeyPrefix = invocation.getMethod().getDeclaringClass() + "_" + invocation.getMethod().getName();
            }
        } else {
            agentKeyPrefix = caching.agentKeyPrefix();
        }
        return agentKeyPrefix;
    }

    /**
     * 获取超时时间
     * <p>
     * 在Caching设置的优先级比CacheObject的高
     * 
     * @param caching
     * @param clazz
     * @return
     */
    protected long getTtl(Caching caching, Class<?> clazz) {
        long ttl;
        if (null == caching || caching.ttl() < 0) {
            if (null == clazz) {
                ttl = DEFAULT_TTL >= 0 ? DEFAULT_TTL : 0;
            } else {
                long _ttl = clazz.getAnnotation(CacheObject.class).ttl();
                if (_ttl < 0) {
                    ttl = DEFAULT_TTL >= 0 ? DEFAULT_TTL : 0;
                } else {
                    ttl = _ttl;
                }
            }
        } else {
            ttl = caching.ttl();
        }
        return ttl;
    }

    /**
     * 把CacheOut标注的参数的字段值保存到缓存
     * 
     * @param caching
     * @param invocation
     * @param wholeKey
     * @param ttl
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    protected void setCacheOut(Caching caching, MethodInvocation invocation, String wholeKey, long ttl)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object[] args = invocation.getArguments();
        Annotation[][] parametersAnnotations = invocation.getMethod().getParameterAnnotations();
        boolean done = false;
        for (int i = 0; i < args.length && !done; i++) {
            for (Annotation parameterAnnotation: parametersAnnotations[i]) {
                if (done) {
                    break;
                }
                if (parameterAnnotation.annotationType().equals(CacheOut.class)) {
                    for (Field field: args[i].getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(CacheOut.class)) {
                            Object out = BeanUtils.getProperty(args[i], field.getName());
                            setToCache(wholeKey + "_cacheOut", out, ttl);
                            done = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 从缓存读出字段值并设置到CacheOut标注的参数的字段中
     * 
     * @param caching
     * @param invocation
     * @param wholeKey
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void getCacheOut(Caching caching, MethodInvocation invocation, String wholeKey)
            throws IllegalAccessException, InvocationTargetException {
        Object[] args = invocation.getArguments();
        Annotation[][] parametersAnnotations = invocation.getMethod().getParameterAnnotations();
        boolean done = false;
        for (int i = 0; i < args.length && !done; i++) {
            for (Annotation parameterAnnotation: parametersAnnotations[i]) {
                if (done) {
                    break;
                }
                if (parameterAnnotation.annotationType().equals(CacheOut.class)) {
                    for (Field field: args[i].getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(CacheOut.class)) {
                            Object out = getFromCache(wholeKey + "_cacheOut");
                            BeanUtils.setProperty(args[i], field.getName(), out);
                            done = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    public static class KeyTuple {
        public KeyTuple(String masterKey, String normalKey) {
            this.masterKey = masterKey;
            this.normalKey = normalKey;
        }

        private String masterKey;

        private String normalKey;

        /**
         * @return the masterKey
         */
        public String getMasterKey() {
            return masterKey;
        }

        /**
         * @return the normalKey
         */
        public String getNormalKey() {
            return normalKey;
        }
    }

    /**
     * 从参数中获取缓存key
     * <p>
     * 必须添加{@code @CacheKey}注解才算
     * 
     * @param invocation
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    protected KeyTuple getKeyTupleFromArguments(MethodInvocation invocation) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        Object[] args = invocation.getArguments();
        String masterKey = null;
        String normalKey = null;
        if (null == args || 0 == args.length) {
            masterKey = normalKey = "NoParam";
        } else {
            StringBuilder masterKeySB = new StringBuilder();
            StringBuilder normalKeySB = new StringBuilder();
            Annotation[][] parametersAnnotations = invocation.getMethod().getParameterAnnotations();

            int masterKeyCnt = 0;
            int normalKeyCnt = 0;
            for (int i = 0; i < args.length; i++) {
                for (Annotation parameterAnnotation: parametersAnnotations[i]) {
                    if (parameterAnnotation.annotationType().equals(CacheKey.class)) {
                        CacheKey cacheKeyAnnotation = (CacheKey) parameterAnnotation;
                        if (null == args[i]) {
                            if (cacheKeyAnnotation.master()) {
                                masterKeySB.append("null").append("_");
                                masterKeyCnt++;
                            } else {
                                normalKeySB.append("null").append("_");
                                normalKeyCnt++;
                            }
                        } else {
                            Class<?> clazz = args[i].getClass();
                            if (!clazz.isAnnotationPresent(CacheObject.class)
                                    && (clazz.equals(Integer.class) || clazz.equals(Long.class) || clazz
                                            .equals(String.class))) {
                                if (cacheKeyAnnotation.master()) {
                                    masterKeySB.append(args[i].toString()).append("_");
                                    masterKeyCnt++;
                                } else {
                                    normalKeySB.append(args[i].toString()).append("_");
                                    normalKeyCnt++;
                                }
                            } else if (clazz.isAnnotationPresent(CacheObject.class)) {
                                for (Field field: args[i].getClass().getDeclaredFields()) {
                                    if (field.isAnnotationPresent(CacheKey.class)) {
                                        if (cacheKeyAnnotation.master()) {
                                            masterKeySB.append(BeanUtils.getProperty(args[i], field.getName())).append(
                                                    "_");
                                            masterKeyCnt++;
                                        } else {
                                            normalKeySB.append(BeanUtils.getProperty(args[i], field.getName())).append(
                                                    "_");
                                            normalKeyCnt++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (masterKeyCnt > 0) {
                masterKey = masterKeySB.toString();
            } else {
                masterKey = "NoParam";
            }
            if (normalKeyCnt > 0) {
                normalKey = normalKeySB.toString();
            } else {
                normalKey = "NoParam";
            }
        }
        return new KeyTuple(masterKey, normalKey);
    }

}
