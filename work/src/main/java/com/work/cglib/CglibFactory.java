package com.work.cglib;

import org.springframework.cglib.proxy.Enhancer;

/**
 * @author damon
 * @date: 2017/8/22.
 */
public class CglibFactory {
    public static Apple getInstance(CglibProxy proxy) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Apple.class);
        //回调方法的参数为代理类对象CglibProxy，最后增强目标类调用的是代理类对象CglibProxy中的intercept方法
        enhancer.setCallback(proxy);
        Apple base = (Apple) enhancer.create();
        return base;
    }
}
