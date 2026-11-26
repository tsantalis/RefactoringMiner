package com.girl.aspect;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Created by corning on 2017/4/20.
 */
@Aspect
@Component
public class HttpAspect {

    @Pointcut(value = "execution(public * com.girl.controller.GirlController.*(..))")
    public void log() {
    }

    @Before("log()")
    public void doBefore() {
        System.out.println("log HttpAspect Before log !!!!!!");
    }

    @After("log()")
    public void doAfter() {
        System.out.println("log HttpAspect After log !!!!!!");
    }

}
