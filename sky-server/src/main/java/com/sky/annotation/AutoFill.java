package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于表示某个方法需要进行公共字段填充处理
 */
@Target(ElementType.METHOD)  //指定注解只能加在方法上面
@Retention(RetentionPolicy.RUNTIME)  //指明注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在，且可以在运行时通过反射获取到。这样的注解可以用来在运行时进行一些特殊的操作，例如动态生成代码、动态代理等

public @interface AutoFill {
    //指定当前数据库的操作的类型:update insert
    OperationType value();
}
