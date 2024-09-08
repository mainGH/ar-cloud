package org.ar.wallet.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.dto.MemberLoginLogMessage;
import org.ar.wallet.Enum.MemberTypeEnum;
import org.ar.wallet.entity.MemberLoginLogs;
import org.ar.wallet.entity.MemberOperationLogMessage;
import org.ar.wallet.entity.MemberOperationLogs;
import org.ar.wallet.service.IMemberInfoService;
import org.ar.wallet.service.IMemberLoginLogsService;
import org.ar.wallet.service.IMemberOperationLogsService;
import org.ar.wallet.service.ProcessMemberLogService;
import org.ar.wallet.util.RedisUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessMemberLogServiceImpl implements ProcessMemberLogService {

    private final IMemberLoginLogsService memberLoginLogsService;
    private final IMemberOperationLogsService memberOperationLogsService;
    private final IMemberInfoService memberInfoService;
    private final RedisUtil redisUtil;

    /**
     * 处理会员登录日志记录
     *
     * @param memberLoginLogMessage
     * @return {@link Boolean}
     */
    @Override
    public Boolean processMemberLoginLog(MemberLoginLogMessage memberLoginLogMessage) {

        MemberLoginLogs memberLoginLogs = new MemberLoginLogs();

        BeanUtils.copyProperties(memberLoginLogMessage, memberLoginLogs);

        //判断会员类型
        if (MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberLoginLogs.getMemberType())) {
            //钱包会员
            //判断是否是首次登录

            if (StringUtils.isEmpty(memberLoginLogMessage.getFirstLoginIp())) {

                log.info("会员首次登录, 记录首次登录信息, memberLoginLogMessage: {}", memberLoginLogMessage);

                //首次登录
                memberInfoService.setFirstLoginInfo(memberLoginLogMessage.getMemberId(), memberLoginLogMessage.getIpAddress(), LocalDateTime.now());
            }

            if (!StringUtils.isEmpty(memberLoginLogMessage.getIpAddress())) {
                log.info("会员登录, 更新本次登录IP, memberId: {}", memberLoginLogMessage.getMemberId());
                memberInfoService.updateLastLoginInfo(memberLoginLogMessage.getMemberId(), memberLoginLogMessage.getIpAddress());
            }
        }

        //将会员登录ip写入到redis
        redisUtil.updateMemberLastLoginIp(String.valueOf(memberLoginLogMessage.getMemberId()), memberLoginLogMessage.getIpAddress());

        //将前台会员登录日志 写进数据库
        return memberLoginLogsService.save(memberLoginLogs);
    }


    /**
     * 处理会员操作日志记录
     *
     * @param memberOperationLogMessage
     * @return {@link Boolean}
     */
    @Override
    public Boolean processMemberOperationLog(MemberOperationLogMessage memberOperationLogMessage) {

        MemberOperationLogs memberOperationLogs = new MemberOperationLogs();

        BeanUtils.copyProperties(memberOperationLogMessage, memberOperationLogs);
        // 商户会员登录时获取IP不准, 从操作日志中获取
        if (memberOperationLogMessage.getMemberId() != null && (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberOperationLogMessage.getMemberType())) && !redisUtil.existLastIp(memberOperationLogMessage.getMemberId().toString())) {
            log.info("商户会员操作日志记录, 更新本次操作IP, memberId: {}, ip: {}", memberOperationLogMessage.getMemberId(), memberOperationLogMessage.getIpAddress());
            if (!StringUtils.isEmpty(memberOperationLogMessage.getIpAddress())) {
                memberInfoService.updateLastLoginInfo(memberOperationLogMessage.getMemberId(), memberOperationLogMessage.getIpAddress());
                redisUtil.refreshLastIp(memberOperationLogMessage.getMemberId().toString(), memberOperationLogMessage.getIpAddress());
            }
        }

        //将前台会员操作日志 写进数据库
        return memberOperationLogsService.save(memberOperationLogs);
    }
}
