//package org.ar.pay.util;
//
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import org.ar.common.core.page.PageReturn;
//import org.ar.common.core.result.ResultCode;
//import org.springframework.context.MessageSource;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.io.Serializable;
//import java.util.List;
//
//@RequiredArgsConstructor
//@Component
//public class RestResultI18n<T> implements Serializable {
//
//    private static LocaleMessageUtil localeMessageUtil;
//
//    private String code;
//
//    private T data;
//
//    private String msg;
//
//    private Integer total;
//
//
//    public static <T> org.ar.common.core.result.RestResult<T> ok() {
//        return ok(null);
//    }
//
//
//    public static <T> org.ar.common.core.result.RestResult<T> ok(T data, Long total) {
//        org.ar.common.core.result.RestResult<T> result = new org.ar.common.core.result.RestResult<>();
//
//        result.setCode(ResultCode.SUCCESS.getCode());
//        result.setMsg(getMsgByKey(ResultCode.SUCCESS.getCode()));
//        result.setData(data);
//        result.setTotal(total.intValue());
//        return result;
//    }
//
//
//
//    public static <T> org.ar.common.core.result.RestResult<T> ok(T data) {
//        ResultCode rce = ResultCode.SUCCESS;
//        if (data instanceof Boolean && Boolean.FALSE.equals(data)) {
//            rce = ResultCode.SYSTEM_EXECUTION_ERROR;
//        }
//        return result(rce, data);
//    }
//
//    public static <T> org.ar.common.core.result.RestResult<T> failed() {
//        return result(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), getMsgByKey(ResultCode.SYSTEM_EXECUTION_ERROR.getCode()), null);
//    }
//
//    public static <T> org.ar.common.core.result.RestResult<T> failed(String msg) {
//        return result(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(),msg, null);
//    }
//
//    public static <T> org.ar.common.core.result.RestResult<T> failed(ResultCode resultCode) {
//        return result(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(),getMsgByKey(ResultCode.SYSTEM_EXECUTION_ERROR.getCode()), null);
//    }
//
//
//    public static <T> org.ar.common.core.result.RestResult<T> failedI18(ResultCode resultCode,String key) {
//        return result(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(),resultCode.getMsg(), null);
//    }
//
//
//    private static <T> org.ar.common.core.result.RestResult<T> result(ResultCode resultCode, T data) {
//        return result(resultCode.getCode(), getMsgByKey(resultCode.getCode()), data);
//    }
//
//    private static <T> org.ar.common.core.result.RestResult<T> result(ResultCode resultCode) {
//        return result(resultCode.getCode(), resultCode.getMsg(), null);
//    }
//
//
//    private static <T> org.ar.common.core.result.RestResult<T> result(String code, String msg, T data) {
//        org.ar.common.core.result.RestResult<T> result = new org.ar.common.core.result.RestResult<>();
//        result.setCode(code);
//        result.setData(data);
//        result.setMsg(msg);
//        return result;
//    }
//
//
//    public static <T> org.ar.common.core.result.RestResult page(PageReturn<T> page) {
//        org.ar.common.core.result.RestResult<List<T>> result = new org.ar.common.core.result.RestResult<>();
//        result.setCode(ResultCode.SUCCESS.getCode());
//        result.setData(page.getList());
//        result.setMsg(getMsgByKey(ResultCode.SUCCESS.getCode()));
//        result.setTotal(page.getTotal().intValue());
//        return result;
//    }
//
//    public static String getMsgByKey(String key){
//        I18nUtil  i18nUtil = SpringContextUtil.getBean(I18nUtil.class);
//        String msg = i18nUtil.getMessage(key);
//        return msg;
//    }
//
//
//
//}
