package org.ar.wallet.config;


import org.ar.common.pay.dto.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.vo.AccountChangeVo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WalletMapStruct {
    List<AccountChangeVo> AccountChangeTransform(List<AccountChange> accountChangeList);


    List<ApplyDistributedDTO> ApplyDistributedTransform(List<ApplyDistributed> accountChangeList);


    List<C2cConfigDTO> C2cConfigTransform(List<C2cConfig> accountChangeList);

    List<CancellationRechargeDTO> CancellationRechargeTransform(List<CancellationRecharge> cancellationRechargeList);

    List<WithdrawalCancellationDTO> WithdrawalCancellationTransform(List<WithdrawalCancellation> cancellationRechargeList);

    List<UsdtConfigDTO> UsdtConfigTransform(List<UsdtConfig> cancellationRechargeList);


    List<MemberGroupListPageDTO> MemberGroupTransform(List<MemberGroup> memberGroupList);


    List<CollectionInfoDTO> collectionInfoTransform(List<CollectionInfo> collectionInfoList);


    List<MemberAccountChangeDTO> memberAccountChangeTransform(List<MemberAccountChange> memberAccountChangeList);

    List<CollectionOrderDTO> collectionOrderTransform(List<CollectionOrder> collectionOrderList);

   // List<CollectionOrderDTO> collectionOrderTransformMap(List<Map<String,Object>> collectionOrderList);


    List<MerchantInfoDTO> merchantInfoTransform(List<MerchantInfo> collectionOrderList);


    List<TradeConfigDTO> TradeConfigTransform(List<TradeConfig> accountChangeList);

    List<TradeConfigSchemeDTO> TradeConfigSchemeTransform(List<TradeConfigScheme> tradeConfigSchemeList);

    List<MatchPoolDTO> matchPoolTransform(List<MatchPool> accountChangeList);


    List<PaymentOrderDTO> paymentOrderTransform(List<PaymentOrder> paymentOrderList);


    List<MatchingOrderDTO> matchingOrderTransform(List<MatchingOrder> paymentOrderList);

    List<WithdrawOrderDTO> withdrawOrderTransform(List<MerchantPaymentOrders> paymentOrderList);

    List<RechargeOrderDTO> rechargeOrderTransform(List<MerchantCollectOrders> records);

    List<MerchantInfoListPageDTO> merchantInfoListTransform(List<MerchantInfo> collectionOrderList);


    List<UsdtBuyOrderDTO> usdtBuyOrderTransform(List<UsdtBuyOrder> collectionOrderList);

    List<UserVerificationCodeslistPageDTO> userVerificationCodesToDto(List<UserVerificationCodes> records);

    List<MemberLoginLogsDTO> memberLoginLogsToDto(List<MemberLoginLogs> records);

    List<MemberOperationLogsDTO> memberOperationLogsDto(List<MemberOperationLogs> records);

    List<TradeIpBlackListPageDTO> tradeIpBlackListToDto(List<TradeIpBlacklist> records);

    List<TaskManagerDTO> taskListToDto(List<TaskManager> record);
    List<TaskCollectionRecordDTO> taskCollectionRecordTransform(List<TaskCollectionRecord> records);

    List<MerchantMemberInfoPageDTO> merchantListPage(List<MemberInfo> records);

    List<MemberBlackDTO> memberBlackToDto(List<MemberBlack> records);

    List<CorrelationMemberDTO> correlationMemberToDto(List<CorrelationMember> records);

    List<MemberInfolistPageDTO> relationMemberToDto(List<MemberInfo> records);

    List<MemberLevelConfigDTO> memberLevelConfigToDto(List<MemberLevelConfig> records);

    List<MemberLevelWelfareConfigDTO> memberLevelWelfareConfigToDto(List<MemberLevelWelfareConfig> records);

    List<MemberLevelChangeDTO> memberLevelChangeToDto(List<MemberLevelChange> result);

    List<MemberInfoDTO> memberInfoToDto(List<MemberInfo> records);
}
