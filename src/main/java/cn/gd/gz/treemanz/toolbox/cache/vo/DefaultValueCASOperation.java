package cn.gd.gz.treemanz.toolbox.cache.vo;

import net.rubyeye.xmemcached.CASOperation;

/**
 *
 * @author Treeman
 *
 */
public abstract class DefaultValueCASOperation<T> implements CASOperation<T> {

    private T defaultValue;
    
    /**
     * @return the defaultValue
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    public DefaultValueCASOperation(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    
}
