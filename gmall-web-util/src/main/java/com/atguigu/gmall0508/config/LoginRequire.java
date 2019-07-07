package com.atguigu.gmall0508.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequire {
    // 给默认值boolean autoRedirect() default true;
    boolean autoRedirect() default true;
}
