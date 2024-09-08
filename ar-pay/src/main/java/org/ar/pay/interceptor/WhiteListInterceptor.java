package org.ar.pay.interceptor;

import lombok.RequiredArgsConstructor;
import org.ar.common.core.result.RestResult;
import org.ar.pay.service.IMerchantRoleService;
import org.ar.pay.service.impl.MerchantInfoServiceImpl;
import org.ar.pay.service.impl.MerchantRoleServiceImpl;
import org.h2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;


@Slf4j
public class WhiteListInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Object  code = request.getParameter("code");
        if(code==null) {
            returnJson(response, JSON.toJSONString(RestResult.failed("商户code为空")));
            return false;
        }

        MerchantInfoServiceImpl merchantInfoServiceImpl = null;
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null ||  ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (ipAddress.equals("127.0.0.1")) {
                    // 根据网卡取本机配置的IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    ipAddress = inet.getHostAddress();
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > 15) {
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            ipAddress="";
        }
        log.info("机主的ip是"+ipAddress);
        WebApplicationContext cxt = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        if(cxt != null && cxt.getBean(MerchantInfoServiceImpl.class) != null ) {
            merchantInfoServiceImpl =cxt.getBean(MerchantInfoServiceImpl.class);
        }
        if(merchantInfoServiceImpl.getIp(String.valueOf(code),ipAddress)){
            return true;
        }else{
            //RestResult.failed("ip不在白名单之内");
            returnJson(response, JSON.toJSONString(RestResult.failed("ip不在白名单之内")));
            return false;
        }
    }


    private void returnJson(HttpServletResponse response, String result) {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            writer = response.getWriter();
            writer.print(result);
        } catch (IOException e) {
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
