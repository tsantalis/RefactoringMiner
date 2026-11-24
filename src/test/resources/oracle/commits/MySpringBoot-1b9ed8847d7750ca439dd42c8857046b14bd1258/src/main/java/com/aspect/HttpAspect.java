package com.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Created by zhuzhengping on 2017/3/19.
 */
@Aspect
@Component
public class HttpAspect {

    @Before("execution(public * com.controller.*(..))")
    public void log(){
        System.out.println("this is the aspect before every requset!!!!");
    }
}
