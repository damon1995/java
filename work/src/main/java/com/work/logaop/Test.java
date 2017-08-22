package com.work.logaop;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author damon
 * @date: 2017/8/22.
 */
public class Test {


    public static void  main(String args[]){
        ApplicationContext ac = new ClassPathXmlApplicationContext("application-context.xml");
        HelloWorld helloWorld = (HelloWorld)ac.getBean("helloWorld");
        helloWorld.print(1);
        helloWorld.say("test",2);
    }
}
