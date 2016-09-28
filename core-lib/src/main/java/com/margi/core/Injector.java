package com.margi.core;

import android.app.Activity;
import android.support.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by allen on 8/24/16.
 */
public class Injector {

    static final Map<Class<?>, ViewInjector<Object>> INJECTORS = new LinkedHashMap<>();

    private Injector(){
        throw new AssertionError("No instances");
    }

    /**
     * 绑定Activity
     * @param target 绑定的目标为activity
     */
    public static void inject(@NonNull Activity target) {
        doInject(target,target,Finder.ACTIVITY);
    }

    private static void doInject(@NonNull Object target, @NonNull Object source, @NonNull Finder finder){
        Class<?> targetClass = target.getClass();

        ViewInjector<Object> viewInjector = findViewInjecterForClass(targetClass);

        viewInjector.inject(finder,target,source);

    }

    /**
     * 通过目标class找到对应的ViewInjecter
     *
     * @return
     */
    private static ViewInjector<Object> findViewInjecterForClass(Class<?> cls){
        ViewInjector<Object> viewInjector = INJECTORS.get(cls);
        if (viewInjector != null) {
            return viewInjector;//缓存中已经存在，直接返回
        }

        String className = cls.getName();
        if(className.startsWith("android.") || className.startsWith("java.") ){
            return null;
        }

        try {
            Class<?> viewInjectClass = Class.forName(className + "$$ViewInjector");
            viewInjector = (ViewInjector<Object>) viewInjectClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            //当前类没有找到去父类中查找
            viewInjector = findViewInjecterForClass(cls.getSuperclass());
        } catch (Exception e) {
            throw new RuntimeException("Unable to create view binder for " + className, e);
        }
        INJECTORS.put(cls,viewInjector);
        return viewInjector;
    }

}
