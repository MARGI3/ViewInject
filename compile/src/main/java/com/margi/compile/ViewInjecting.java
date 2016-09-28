package com.margi.compile;

/**
 * Created by allen on 9/6/16.
 *
 * A field or method view injecting.
 */
public interface ViewInjecting {
    /**
     * A description of the binding in human readable form (e.g., "field 'foo'").
     * 对于使用注解绑定对象的描述信息
     */
    String getDescription();
}
