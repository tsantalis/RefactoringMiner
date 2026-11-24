package com.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by zhuzhengping on 2017/3/19.
 */
@Aspect
@Component
public class HttpAspect {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpAspect.class);

    @Pointcut("execution(public * com.controller.*(..))")
    public void log(){
    }

    @Before("log()")
    public void before(JoinPoint joinPoint){
//        System.out.println("this is the aspect before every requset!!!!");
        LOGGER.info("this is the aspect before every requset!!!!");

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        //url
        LOGGER.info("url={}",request.getRequestURL());
        //method
        LOGGER.info("method={}",request.getMethod());
        //ip
        LOGGER.info("id={}",request.getRemoteAddr());
        //class_method
        LOGGER.info("class_method={}",joinPoint.getSignature().getDeclaringTypeName() + "," + joinPoint.getSignature().getName());
        //args[]
        LOGGER.info("args={}",joinPoint.getArgs());
    }

    @After("log()")
    public void after(){
        LOGGER.info("this is the aspect after every response!!!");
    }

    @AfterReturning(pointcut = "log()",returning = "object")
    public void doAfterReturing(Object object){
        LOGGER.info("response={}",object);
    }
}
