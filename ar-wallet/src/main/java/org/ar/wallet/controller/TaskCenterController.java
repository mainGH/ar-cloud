package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.wallet.Enum.MemberOperationModuleEnum;
import org.ar.wallet.annotation.LogMemberOperation;
import org.ar.wallet.req.ClaimTaskRewardReq;
import org.ar.wallet.service.ITaskCenterService;
import org.ar.wallet.service.ITaskCollectionRecordService;
import org.ar.wallet.service.ITaskRulesContentService;
import org.ar.wallet.vo.TaskCenterVo;
import org.ar.wallet.vo.TaskRuleDetailsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/taskCenter")
@Api(description = "前台-任务中心控制器")
@Validated
public class TaskCenterController {

    @Autowired
    private ITaskRulesContentService taskRulesContentService;

    @Autowired
    private ITaskCollectionRecordService taskCollectionRecordService;


    @Autowired
    private ITaskCenterService taskCenterService;


    //获取任务中心页面数据
    @GetMapping("/fetchTaskCenterDetails")
    @ApiOperation(value = "前台-获取任务中心页面数据")
    public RestResult<TaskCenterVo> fetchTaskCenterDetails() {
        //获取任务规则详情
        return taskCenterService.fetchTaskCenterDetails();
    }


    //获取任务规则详情
    @GetMapping("/getTaskRuleDetails")
    @ApiOperation(value = "前台-获取任务规则详情")
    public RestResult<TaskRuleDetailsVo> getTaskRuleDetails() {
        //获取任务规则详情
        return taskRulesContentService.getTaskRuleDetails();
    }


    //领取任务奖励
    @PostMapping("/claimTaskReward")
    @ApiOperation(value = "前台-领取活动任务奖励")
    @LogMemberOperation(value = MemberOperationModuleEnum.CLAIM_ACTIVITY_TASK_REWARD)
    public RestResult claimTaskReward(@RequestBody @ApiParam @Valid ClaimTaskRewardReq claimTaskRewardReq, HttpServletRequest request) {
        //领取任务奖励
        return taskCollectionRecordService.claimTaskReward(claimTaskRewardReq, request);
    }
}
