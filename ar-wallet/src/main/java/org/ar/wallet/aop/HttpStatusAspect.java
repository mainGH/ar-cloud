package org.ar.wallet.aop;

import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * @author admin
 * @date 2024/5/1 11:44
 */
@Aspect
@Component
public class HttpStatusAspect {


    @AfterReturning(pointcut = "execution(* org.ar.wallet.controller.*.*(..))", returning = "result")
    public void httpsCheck(JoinPoint joinPoint, Object result) throws Throwable {
        if(result instanceof RestResult){
            if (((RestResult<?>) result).getCode().equals(ResultCode.RELOGIN.getCode())) {
                HttpServletResponse response = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
                if(response != null){
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                }
            }
        }
    }
}
