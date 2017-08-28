package com.work.springboot;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author damon
 * @date: 2017/8/22.
 */
@RequestMapping("/hello")
@RestController
public class HelloWordController {
    @RequestMapping(value = "/hello",method = RequestMethod.GET,produces = {MediaType.APPLICATION_JSON_VALUE})
    public String helloSpringBoot(){
        return "Hello SpringBoot!";
    }

}
