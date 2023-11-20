package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */
@Aspect
@Component
@Slf4j  //记录日志
public class AutoFillAspect {
    //定义切入点：哪些类的哪些方法来进行拦截
    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {

    }


    /**
     * 前置通知，在这个部分进行公共字段的赋值
     */
    //定义前置通知，因为要在 update 和 insert 之前为公共字段赋值
    //当匹配上切点表达式的时候就会执行我们这个通知的方法
    @Before("autoFillPointCut")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段的填充...");

        //1、获取到当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //方法签名对象
        AutoFill autoFill= signature.getMethod().getAnnotation(AutoFill.class);  //获得方法上的注解对象
        OperationType operationType = autoFill.value(); // 获得数据库操作类型

        //2、获取到当前被拦截的方法的参数--实体对象
         Object[] args= joinPoint.getArgs();
         if(args == null || args.length == 0) {
             return;
         }

         Object entity = args[0];

        //3、准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //4、根据当前的不同操作类型，为对应的属性通过反射来赋值
        if (operationType == OperationType.INSERT) {
            //为四个公共字段赋值
            try {
                //获得了四个公共字段的set方法
//                Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime",LocalDateTime.class);  为了避免方法拼写错误，所以统一用方法常量，定义在common包
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                //通过反射来为对象属性赋值
                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else if (operationType == OperationType.UPDATE) {
            //为两个公共字段赋值
            try {
                //获得了2个公共字段的set方法
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                //通过反射来为对象属性赋值
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
