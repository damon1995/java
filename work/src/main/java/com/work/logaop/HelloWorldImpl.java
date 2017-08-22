package com.work.logaop;

import org.springframework.stereotype.Component;

/**
 * @author damon
 * @date: 2017/8/22.
 */
@Component("helloWorld")
public class HelloWorldImpl implements HelloWorld {
    @Override
    @MonitorLog()
    public void print(Integer age) {
        System.out.println("this is age:" + age);
    }

    @Override
    @MonitorLog()
    public Apple say(String name, Integer userId) {
        System.out.println("this is name:" + name+ " this is userId:"+userId);
        Apple apple =new Apple();
        apple.setId(123);
        return apple;
    }
}
