package com.work.cglib;

/**
 * @author damon
 * @date: 2017/8/22.
 */
public class CglibTest {
    public static void main(String[] args){
        CglibProxy proxy = new CglibProxy();
        // base为生成的增强过的目标类
        Apple apple = CglibFactory.getInstance(proxy);
        apple.test();
    }
}
