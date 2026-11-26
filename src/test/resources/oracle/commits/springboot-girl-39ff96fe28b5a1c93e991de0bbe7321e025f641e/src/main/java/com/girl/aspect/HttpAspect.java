package com.girl.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Created by corning on 2017/4/20.
 */
@Aspect
@Component
public class HttpAspect {

    @Before(value = "execution(public * com.girl.controller.GirlController.*(..))")
    public void log() {
        System.out.println("log HttpAspect !!!!!!");
    }
}
