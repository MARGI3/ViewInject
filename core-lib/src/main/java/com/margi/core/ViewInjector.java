package com.margi.core;

/**
 * Created by allen on 8/24/16.
 */
public interface ViewInjector<T> {
    /**
     * 处理绑定操作
     *
     * @param finder 定义一些枚举，表示查找的类型。以及一些查找方法
     * @param target 进行绑定的目标对象
     * @param source 所依附的对象，可能是target本身
     */
    void inject(Finder finder, T target, Object source);
}
