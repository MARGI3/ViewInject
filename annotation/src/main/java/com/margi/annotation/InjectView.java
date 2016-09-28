package com.margi.annotation;

import android.support.annotation.IdRes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by maji on 16/8/10.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface InjectView {
    @IdRes int[] value();
}
