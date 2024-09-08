package org.ar.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ar.common.pay.dto.MemberAuthDTO;
import org.ar.common.pay.dto.MemberInfoDTO;
import org.ar.common.pay.dto.MemberLevelInfoDTO;
import org.ar.common.pay.dto.MerchantActivationInfoDTO;
import org.ar.wallet.entity.MemberInfo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author
 */
@Mapper
public interface MemberInfoMapper extends BaseMapper<MemberInfo> {
    MemberAuthDTO getByUsername(@Param("userName") String userName);

    MemberInfo getMemberInfoById(@Param("mid") String mid);

    int updateBalanceById(@Param("finalAmount") BigDecimal finalAmount, @Param("mid") String mid,@Param("frozenAmountFlag") String frozenAmountFlag,
                           @Param("frozenAmount") BigDecimal frozenAmount);

    /**
     * 查询会员信息 加上排他行锁
     *
     * @param id
     * @return {@link MemberInfo}
     */
    @Select("SELECT * FROM member_info WHERE id = #{id} FOR UPDATE")
    MemberInfo selectMemberInfoForUpdate(Long id);


    /**
     * 查询会员信息 加上排他行锁
     *
     * @param member_id
     * @return {@link MemberInfo}
     */
    @Select("SELECT * FROM member_info WHERE member_id = #{member_id} FOR UPDATE")
    MemberInfo selectMemberInfoByMemberIdForUpdate(String member_id);

    Integer updateByMemberId(@Param("vo") MemberInfo memberInfo);

    List<MerchantActivationInfoDTO> selectMerchantInfoList();

    BigDecimal selectMemberTotalBalance();

    List<MerchantActivationInfoDTO> selectActiveInfoList(@Param("dateStr") String dateStr);

    List<MerchantActivationInfoDTO> selectActiveInfoMonthList(@Param("dateStr") String dateStr);

    List<MemberInfo> selectSumInfo();

    List<MemberInfo> selectSumNumInfo();

    Long selectActiveNum(@Param("startTime") String startTime, @Param("endTime") String endTime);


    Long selectRealNameNum();

    Long selectBuyNum();

    Long selectSellNum();

    Long selectBuyAndSellNum();

    Long selectBuyUsdtNum();

    Long selectBuyDisableFuture();

    Long selectSellDisableFuture();

    List<MemberInfo> selectMemberInfoInfo();


    List<MemberInfo> selectMerchantActiveNum(@Param("startTime") String startTime, @Param("endTime") String endTime);

    List<MemberInfo> selectMerchantRealNameNum();

    List<MemberInfo> selectMerchantBuyNum();

    List<MemberInfo> selectMerchantSellNum();

    List<MemberInfo> getRechargeInfo(@Param("list") List<MemberInfo> list);

    List<MemberInfo> getWithdrawInfo(@Param("list") List<MemberInfo> list);

    void updateRechargeInfo(@Param(value = "list") List<MemberInfo> userIdList);

    void updateWithdrawInfo(@Param("id")Long id, @Param("withdrawNum")Long withdrawNum, @Param("withdrawTotalAmount")BigDecimal withdrawTotalAmount);

    List<MemberInfo> selectMyPage(@Param("page")long page, @Param("size")long size, @Param(value = "userIdList") List<String> userIdList);

    long count(@Param(value = "userIdList") List<String> userIdList);

    List<MemberInfo> selectTaskReward();

    // 更新会员状态信息
    void updateMemberInfoStatus(@Param("id")String id, @Param("status")String status,@Param("buyStatus")String buyStatus, @Param("sellStatus")String sellStatus);


    MemberAuthDTO getByAppUsername(@Param("userName") String userName);

    List<MemberLevelInfoDTO> getLevelNum(@Param("merchantCode")String merchantCode);

    Long selectblackMemberNum();
}
