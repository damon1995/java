package com.work.proxy;

import java.lang.reflect.Proxy;

/**
 * @author damon
 * @date: 2017/8/22.
 */
public class ProxyTest {
    public static void main(String[] args){
        Fruit fruit =new Apple();
        MyInvocationHandler handler = new MyInvocationHandler(fruit);
        Fruit fruitProxy = (Fruit)Proxy.newProxyInstance(fruit.getClass().getClassLoader()
            ,fruit.getClass().getInterfaces(),handler);
        fruitProxy.test();
    }
}
