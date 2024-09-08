package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TaskCollectionRecordDTO;
import org.ar.common.pay.req.TaskCollectionRecordReq;
import org.ar.wallet.entity.MemberTaskStatus;
import org.ar.wallet.entity.TaskCollectionRecord;
import org.ar.wallet.req.ClaimTaskRewardReq;
import org.ar.wallet.req.TaskCollectionRecordPageReq;
import org.ar.wallet.vo.PrizeWinnersVo;
import org.ar.wallet.vo.TaskCollectionRecordListVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 会员领取任务记录 服务类
 * </p>
 *
 * @author 
 * @since 2024-03-18
 */
public interface ITaskCollectionRecordService extends IService<TaskCollectionRecord> {

    PageReturn<TaskCollectionRecordDTO> listPage(TaskCollectionRecordReq req);

    /**
     * 前台-分页查询奖励明细
     * @param req
     * @return
     */
    RestResult<PageReturn<TaskCollectionRecordListVo>> getPageList(TaskCollectionRecordPageReq req);


    /**
     * 获取领奖会员列表
     *
     * @return {@link List}<{@link PrizeWinnersVo}>
     */
    List<PrizeWinnersVo> getPrizeWinnersList();

    TaskCollectionRecordDTO getStatisticsData();


    /**
     * 查看会员是否领取过任务奖励
     *
     * @param memberId 会员id
     * @param taskId   任务id
     * @return boolean
     */
    boolean checkTaskCompletedByMember(Long memberId, Long taskId);


    /**
     * 检查会员是否领取过奖励
     *
     * @param memberTaskStatus
     * @return {@link Boolean}
     */
    TaskCollectionRecord hasReceivedReward(MemberTaskStatus memberTaskStatus);



    /**
     * 领取任务奖励
     *
     * @param claimTaskRewardReq
     * @param request
     * @return {@link RestResult}
     */
    RestResult claimTaskReward(ClaimTaskRewardReq claimTaskRewardReq, HttpServletRequest request);

}
