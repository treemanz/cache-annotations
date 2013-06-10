package cn.gd.gz.treemanz.toolbox.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Caching {

    enum Event {
        NONE, GET, UPDATE, SET_RETURN, DEL_PARAM, GET_MULTI, LIST, DEL_LIST
    }

    enum MinorCache {
        ONLY, DEFAULT, CUSTOM
    }

    /**
     * caching operation :
     * <p>
     * <li>NONE - 不缓存，没什么用</li>
     * <li>GET - get类接口，前环绕，首先访问缓存(keyPrefix + @CacheKey)，若命中不访问方法体</li>
     * <li>UPDATE - After returning advice, update cache with the return object</li>
     * <li>SET_RETURN - get/update(return)类接口，后环绕，对接口返回值缓存</li>
     * <li>DEL_PARAM - update(no return)/delete类接口，返回后删除指定缓存(keyPrefix+CacheKey)
     * </li>
     * <li>GET_MULTI - get[]类接口，前环绕，根据ids来获取对象集合，将对不命中部分id访问方法体(类似GET)</li>
     * <li>LIST - list类接口，前环绕，首先访问缓存(keyPrefix +
     * params)获取ids，若命中则调用GET_MULTI获取对象集合；若不命中则访问方法体</li>
     * <li>DEL_LIST - list类接口，用以删除List的ID缓存</li>
     * 
     * @return
     */
    Event event();

    /**
     * cache key prefix, default ""
     * 
     * @return
     */
    String keyPrefix() default "";

    /**
     * each agent key prefix, default ""
     * <p>
     * used when event=LIST
     * 
     * @return
     */
    String agentKeyPrefix() default "";

    /**
     * time-to-live millisecond
     * <p>
     * Caching.ttl takes precedence over CacheObject.ttl
     * 
     * @return
     */
    long ttl() default -1;

}
