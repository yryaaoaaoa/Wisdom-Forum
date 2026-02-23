package com.yry.blog.myblogcommon.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD}) // 该注解只能用在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时保留，以便反射获取
public @interface RequiresPermission {
    // 注解的属性，用来指定所需的权限，默认是一个空数组
    String[] value() default {};
}