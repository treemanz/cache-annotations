package cn.gd.gz.treemanz.toolbox.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})   
@Retention(RetentionPolicy.RUNTIME)   
public @interface CacheKey {

    /**
     * 是否为master key
     * <p>
     * master key会与keyPrefix一起决定cache version
     * @return
     */
    boolean master() default false;
    
}
