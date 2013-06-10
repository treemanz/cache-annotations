package cn.gd.gz.treemanz.toolbox.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheObject {

    String prefix() default "";

    /**
     * time-to-live second
     *
     * @return
     */
    long ttl() default -1;

}
