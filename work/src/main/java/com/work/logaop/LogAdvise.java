package com.work.logaop;

import java.lang.reflect.Method;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

/**
 * @author damon
 * @date: 2017/8/22.
 */
@Aspect
@Component
public class LogAdvise {

    @Pointcut("@annotation(com.work.logaop.MonitorLog)")
    public void XflushMonitorLog(){
        System.out.println("我是一个切入点");
    }

    @Around("XflushMonitorLog()")
    public Object aroundExec(ProceedingJoinPoint pjp) throws Throwable{
        System.out.println("我是Around");
        //返回被织入增强处理的目标对象
        Object target = pjp.getTarget();
        String className = target.getClass().getName();
        System.out.println("className :" + className);
        //返回目标方法的签名
        MethodSignature ms=(MethodSignature) pjp.getSignature();
        Method method=ms.getMethod();
        //方法名
        String methodName = method.getName();
        System.out.println("methodName :" + methodName);
        //方法参数名
        LocalVariableTableParameterNameDiscoverer u =
            new LocalVariableTableParameterNameDiscoverer();
        String[] parameters = u.getParameterNames(method);
        //方法参数值
        Object[] args = pjp.getArgs();
        for(int i=0;i< parameters.length;i++){
            System.out.println(parameters[i]+":"+args[i]);
        }
        //方法返回值
        Object returnVal = pjp.proceed();
        if (Objects.nonNull(returnVal)){
            System.out.println("return value:"+ returnVal.toString());
        }
        //执行log记录日志
        Logger logger = LoggerFactory.getLogger(className);
        StringBuffer stringBuffer = new StringBuffer();
        for(int i=0;i< parameters.length;i++){
            if (i==parameters.length -1 ){
                stringBuffer.append(parameters[i]).append(":").append(args[i]);
            }else{
                stringBuffer.append(parameters[i]).append(":").append(args[i]).append(",");
            }
        }
        logger.info(methodName+"|cost|true|"+stringBuffer.toString()+"|result:{}",returnVal);
        return returnVal;
    }
}
