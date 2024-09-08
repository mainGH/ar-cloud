package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.ApiResponse;
import org.ar.common.core.result.KycRestResult;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.PaymentOrderMapper;
import org.ar.wallet.req.KycPartnerReq;
import org.ar.wallet.req.KycSellReq;
import org.ar.wallet.req.LinkKycPartnerReq;
import org.ar.wallet.service.*;
import org.ar.wallet.util.OrderNumberGeneratorUtil;
import org.ar.wallet.util.SpringContextUtil;
import org.ar.wallet.vo.BankKycTransactionVo;
import org.ar.wallet.vo.KycBankResponseVo;
import org.ar.wallet.vo.KycBanksVo;
import org.ar.wallet.vo.KycPartnersVo;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class IKycCenterServiceImpl implements IKycCenterService {

    @Autowired
    private IKycPartnersService kycPartnersService;

    @Autowired
    private IMemberInfoService memberInfoService;

    @Autowired
    private IKycBankService kycBankService;

    @Autowired
    private IKycApprovedOrderService kycApprovedOrderService;

    @Autowired
    private OrderNumberGeneratorUtil orderNumberGenerator;

    @Autowired
    private RedissonUtil redissonUtil;

    @Autowired
    private ISellService sellService;

    /**
     * 获取KYC列表
     *
     * @return {@link KycRestResult}<{@link List}<{@link KycPartnersVo}>>
     */
    @Override
    public KycRestResult<List<KycPartnersVo>> getKycPartners() {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取KYC列表失败: 获取会员信息失败");
            return KycRestResult.failure(ResultCode.RELOGIN);
        }

        //查询银行列表
        List<KycBank> kycBankList = kycBankService.lambdaQuery().eq(KycBank::getDeleted, 0)
                .list();

        List<KycPartners> kycPartners = kycPartnersService.getKycPartners(memberInfo.getId());

        if (kycPartners == null) {
            kycPartners = new ArrayList<>();
        }

        ArrayList<KycPartnersVo> kycPartnersVoList = new ArrayList<>();

        for (KycPartners kycPartner : kycPartners) {
            KycPartnersVo kycPartnersVo = new KycPartnersVo();

            BeanUtils.copyProperties(kycPartner, kycPartnersVo);

            for (KycBank kycBank : kycBankList) {
                if (kycBank.getBankCode().equals(kycPartner.getBankCode())) {
                    //设置银行连接地址
                    kycPartnersVo.setLinkUrl(kycBank.getLinkUrl());

                    //设置银行连接方式
                    kycPartnersVo.setLinkType(kycBank.getLinkType());

                    //设置 图标地址
                    kycPartnersVo.setIconUrl(kycBank.getIconUrl());

                    break;
                }
            }

            if (kycPartnersVo.getLinkStatus() == 1) {
                kycPartnersVo.setRemark("Working");
            } else {
                kycPartnersVo.setRemark("Please relink the tool or modify the upi and relink.");
            }

            kycPartnersVoList.add(kycPartnersVo);
        }

        return KycRestResult.ok(kycPartnersVoList);
    }


    /**
     * 添加 KYC Partner
     *
     * @param kycPartnerReq
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public KycRestResult addKycPartner(KycPartnerReq kycPartnerReq, HttpServletRequest request) {


        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("添加 KYC Partner失败: 获取会员信息失败");
            return KycRestResult.failure(ResultCode.RELOGIN);
        }

        // 查询银行信息
        KycBank kycBank = kycBankService.getBankInfoByBankCode(kycPartnerReq.getBankCode());

        if (kycBank == null) {
            log.error("添加 KYC Partner失败: 获取KYC银行信息失败");
            return KycRestResult.failure(ResultCode.KYC_BANK_NOT_FOUND);
        }

        KycPartners kycPartners = new KycPartners();

        BeanUtils.copyProperties(kycPartnerReq, kycPartners);

        // 会员id
        kycPartners.setMemberId(String.valueOf(memberInfo.getId()));

        // 会员账号
        kycPartners.setMemberAccount(memberInfo.getMemberAccount());

        // 会员手机号
        kycPartners.setMobileNumber(memberInfo.getMobileNumber());

        // 银行名称
        kycPartners.setBankName(kycBank.getBankName());

        // 图标地址
        kycPartners.setIconUrl(kycBank.getIconUrl());

        return kycPartnersService.save(kycPartners) ? KycRestResult.ok() : KycRestResult.failed();
    }


    /**
     * 连接KYC
     *
     * @param linkKycPartnerReq
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public KycRestResult linkKycPartner(LinkKycPartnerReq linkKycPartnerReq, HttpServletRequest request) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("连接KYC失败: 获取会员信息失败");
            return KycRestResult.failure(ResultCode.RELOGIN);
        }

        //查询KYC 判断该KYC是否属于该会员
        KycPartners KycPartner = kycPartnersService.lambdaQuery().eq(KycPartners::getId, linkKycPartnerReq.getId()).eq(KycPartners::getDeleted, 0).one();

        if (KycPartner == null || !KycPartner.getMemberId().equals(String.valueOf(memberInfo.getId()))) {
            log.error("连接KYC失败: KycPartner为null 或 KycPartner 不属于该会员, KycPartner: {}, 会员信息: {}", KycPartner, memberInfo);
            return KycRestResult.failure(ResultCode.DATA_NOT_FOUND);
        }

        //判断如果状态是连接成功, 就不进行连接了
//        if (KycPartner.getLinkStatus() == 1) {
//            log.info("连接KYC 当前状态是已连接, 不进行操作, KycPartner: {}, memberInfo: {}", KycPartner, memberInfo);
//            return KycRestResult.ok();
//        }

        //获取对应的银行实现类
        IAppBankTransaction appBankTransaction = SpringContextUtil.getBean(KycPartner.getBankCode());

        //连接银行
        KycBankResponseVo linkKycPartner = appBankTransaction.linkKycPartner(linkKycPartnerReq.getToken());

        //判断是否连接成功 修改连接状态
        if (linkKycPartner.getStatus()) {
            //连接成功
            log.info("连接KYC银行成功, KycPartner: {}, 会员信息: {}", KycPartner, memberInfo);

            // 创建一个 UpdateWrapper 对象，用于构建更新条件和指定更新字段
            LambdaUpdateWrapper<KycPartners> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(KycPartners::getId, KycPartner.getId())  // 指定更新条件，这里以 id 为条件
                    .set(KycPartners::getToken, linkKycPartnerReq.getToken()) // 指定更新字段
                    .set(KycPartners::getLinkStatus, 1); // 指定更新字段

            // 这里传入的 null 表示不更新实体对象的其他字段
            boolean update = kycPartnersService.update(null, lambdaUpdateWrapper);

            return KycRestResult.ok();
        } else {

            log.error("连接KYC银行失败, KycPartner: {}, 会员信息: {}", KycPartner, memberInfo);

            //将连接状态改为未连接

            // 创建一个 UpdateWrapper 对象，用于构建更新条件和指定更新字段
            LambdaUpdateWrapper<KycPartners> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(KycPartners::getId, KycPartner.getId())  // 指定更新条件，这里以 id 为条件
                    .set(KycPartners::getLinkStatus, 0); // 指定更新字段

            // 这里传入的 null 表示不更新实体对象的其他字段
            boolean update = kycPartnersService.update(null, lambdaUpdateWrapper);

            // 连接失败 将银行返回的信息返回给APP客户端
            return KycRestResult.failure(ResultCode.KYC_CONNECTION_FAILED, linkKycPartner.getMsg());
        }

        //TODO 将连接状态推送给APP端
    }

    /**
     * 获取银行列表
     *
     * @return {@link ApiResponse}
     */
    @Override
    public KycRestResult<List<KycBanksVo>> getBanks() {

        List<KycBank> kycBankList = kycBankService.lambdaQuery().eq(KycBank::getDeleted, 0).list();

        ArrayList<KycBanksVo> kycBanksVoList = new ArrayList<>();

        for (KycBank kycBank : kycBankList) {

            KycBanksVo kycBanksVo = new KycBanksVo();

            BeanUtils.copyProperties(kycBank, kycBanksVo);

            kycBanksVoList.add(kycBanksVo);
        }

        return KycRestResult.ok(kycBanksVoList);
    }


    /**
     * 判断KYC是否在线
     *
     * @param req
     * @return {@link Boolean}
     */
//    @Override
//    public Boolean effective(AppToken req) {
//        AppToken appToken = appTokenService.lambdaQuery().eq(AppToken::getMemberId, req.getMemberId()).one();
//        Map<String, String> mapHeader = new HashMap<>();
//        mapHeader.put("Cookie", appToken.getToken());
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("direction", "DEBIT");
//        String a = RequestUtil.getForAppJson("https://www.freecharge.in/thv/moneydirection", jsonObject, mapHeader);
//        JSONObject jsonFreechargeObject = JSONObject.parseObject(a);
//        JSONArray dataArry = jsonFreechargeObject.getJSONArray("data");
//        if (dataArry.size() >= 1) {
//            return true;
//        }
//        return false;
//    }


    /**
     * 开始卖出
     *
     * @param kycSellReq
     * @param request
     * @return {@link KycRestResult}
     */
    @Override
    public KycRestResult startSell(KycSellReq kycSellReq, HttpServletRequest request) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("KYC 开始卖出处理失败: 获取会员信息失败");
            return KycRestResult.failure(ResultCode.RELOGIN);
        }

        //查询KYC 判断该KYC是否属于该会员
        KycPartners KycPartner = kycPartnersService.lambdaQuery().eq(KycPartners::getId, kycSellReq.getId()).eq(KycPartners::getDeleted, 0).one();

        if (KycPartner == null || !KycPartner.getMemberId().equals(String.valueOf(memberInfo.getId()))) {
            log.error("KYC 开始卖出处理失败: KycPartner为null 或 KycPartner 不属于该会员, KycPartner: {}, 会员信息: {}", KycPartner, memberInfo);
            return KycRestResult.failure(ResultCode.DATA_NOT_FOUND);
        }

        // 创建一个 UpdateWrapper 对象，用于构建更新条件和指定更新字段
        LambdaUpdateWrapper<KycPartners> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(KycPartners::getId, KycPartner.getId())  // 指定更新条件，这里以 id 为条件
                .set(KycPartners::getSellStatus, 1); // 指定只更新 sellStatus 字段

        // 使用服务层的 update 方法，传入 null 和 updateWrapper
        // 这里传入的 null 表示不更新实体对象的其他字段
        boolean update = kycPartnersService.update(null, lambdaUpdateWrapper);

        return update ? KycRestResult.ok() : KycRestResult.failed();

    }


    /**
     * 停止卖出
     *
     * @param kycSellReq
     * @param request
     * @return {@link KycRestResult}
     */
    @Override
    public KycRestResult stopSell(KycSellReq kycSellReq, HttpServletRequest request) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("KYC 停止卖出处理失败: 获取会员信息失败");
            return KycRestResult.failure(ResultCode.RELOGIN);
        }

        //查询KYC 判断该KYC是否属于该会员
        KycPartners KycPartner = kycPartnersService.lambdaQuery().eq(KycPartners::getId, kycSellReq.getId()).eq(KycPartners::getDeleted, 0).one();

        if (KycPartner == null || !KycPartner.getMemberId().equals(String.valueOf(memberInfo.getId()))) {
            log.error("KYC 停止卖出处理失败: KycPartner为null 或 KycPartner 不属于该会员, KycPartner: {}, 会员信息: {}", KycPartner, memberInfo);
            return KycRestResult.failure(ResultCode.DATA_NOT_FOUND);
        }

        // 创建一个 UpdateWrapper 对象，用于构建更新条件和指定更新字段
        LambdaUpdateWrapper<KycPartners> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(KycPartners::getId, KycPartner.getId())  // 指定更新条件，这里以 id 为条件
                .set(KycPartners::getSellStatus, 0); // 指定只更新 sellStatus 字段

        // 使用服务层的 update 方法，传入 null 和 updateWrapper
        // 这里传入的 null 表示不更新实体对象的其他字段
        boolean update = kycPartnersService.update(null, lambdaUpdateWrapper);

        return update ? KycRestResult.ok() : KycRestResult.failed();
    }


    /**
     * 通过 KYC 验证完成订单
     *
     * @param kycTransactionMessage
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean finalizeOrderWithKYCVerification(KycTransactionMessage kycTransactionMessage) {

        //分布式锁key ar-wallet-finalizeOrderWithKYCVerification+卖出订单号
        String key = "ar-wallet-finalizeOrderWithKYCVerification" + kycTransactionMessage.getSellerOrderId();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {


                //根据 收款 UPI 查 KYC信息 (卖出状态开启 连接状态开启)
                KycPartners kycPartners = kycPartnersService.getKYCPartnersByUpiId(kycTransactionMessage.getRecipientUPI());

                if (kycPartners == null) {
                    log.error("通过 KYC 验证完成订单 处理失败, KYC不存在或状态未开启或未连接银行, kycTransactionMessage: {}, KYC信息: {}", kycTransactionMessage, kycPartners);
                    return false;
                }

                //根据 KYC 获取对应的实体类
                IAppBankTransaction appBankTransaction = SpringContextUtil.getBean(kycPartners.getBankCode());

                if (appBankTransaction == null) {
                    log.error("通过 KYC 验证完成订单 处理失败, 根据 KYC 获取对应的实体类失败, kycTransactionMessage: {}, KYC信息: {}", kycTransactionMessage, kycPartners);
                    return false;
                }


                //获取银行交易记录
                List<BankKycTransactionVo> kycBankTransactions = appBankTransaction.getKYCBankTransactions(kycPartners.getToken());

                BankKycTransactionVo bankKycTransactionVo = findTransactionByUTR(kycBankTransactions, kycTransactionMessage.getTransactionUTR());

                if (bankKycTransactionVo == null) {
                    //没有该笔UTR的交易记录
                    log.error("通过 KYC 验证完成订单 处理失败, 没有该笔UTR的交易记录, kycTransactionMessage: {}, KYC信息: {}, kycBankTransactions: {}", kycTransactionMessage, kycPartners, kycBankTransactions);
                    return false;
                }

                //核对交易记录 校验金额 交易类型 交易状态
                if (!verifyTransaction(bankKycTransactionVo, kycTransactionMessage, kycPartners)) {
                    log.error("通过 KYC 验证完成订单 处理失败, 核对交易记录不通过, 卖出订单号: {}, kycTransactionMessage: {}, KYC信息: {}, kycBankTransactions: {}", kycTransactionMessage.getSellerOrderId(), kycTransactionMessage, kycPartners, kycBankTransactions);
                    return false;
                }

                //校验通过

                // 根据卖出订单号 查询KYC交易订单 查看该订单是否已被处理过
                if (kycApprovedOrderService.checkKycTransactionExistsBySellOrderId(kycTransactionMessage.getSellerOrderId())) {
                    //该笔订单已经被处理过
                    log.error("通过 KYC 验证完成订单 处理失败, 该订单已被处理过, 卖出订单号: {}, kycTransactionMessage: {}, KYC信息: {}, kycBankTransactions: {}", kycTransactionMessage.getSellerOrderId(), kycTransactionMessage, kycPartners, kycBankTransactions);
                    return false;
                }

                //将该笔订单保存到 通过 KYC 验证完成的订单表里面
                KycApprovedOrder kycApprovedOrder = new KycApprovedOrder();


                // 买入订单号
                kycApprovedOrder.setBuyerOrderId(kycTransactionMessage.getBuyerOrderId());

                // 卖出订单号
                kycApprovedOrder.setSellerOrderId(kycTransactionMessage.getSellerOrderId());

                // 买入会员id
                kycApprovedOrder.setBuyerMemberId(String.valueOf(kycTransactionMessage.getBuyerMemberId()));

                // 卖出会员id
                kycApprovedOrder.setSellerMemberId(String.valueOf(kycTransactionMessage.getSellerMemberId()));

                // 收款人 UPI
                kycApprovedOrder.setRecipientUpi(bankKycTransactionVo.getRecipientUPI());

                // 付款人 UPI
                kycApprovedOrder.setPayerUpi(bankKycTransactionVo.getPayerUPI());

                // 金额
                kycApprovedOrder.setAmount(bankKycTransactionVo.getAmount());

                // utr
                kycApprovedOrder.setUtr(bankKycTransactionVo.getUTR());

                // 交易状态, 1: 表示成功
                kycApprovedOrder.setTransactionStatus(bankKycTransactionVo.getOrderStatus());

                // 交易类型, 1: 收入, 2: 支出
                kycApprovedOrder.setTransactionType(bankKycTransactionVo.getMode());

                // 银行交易时间
                kycApprovedOrder.setBankTransactionTime(bankKycTransactionVo.getCreateTime());

                // 钱包交易时间
                kycApprovedOrder.setWalletTransactionTime(kycTransactionMessage.getTransactionTime());

                // kycId
                kycApprovedOrder.setKycId(kycPartners.getId());

                // 银行编码
                kycApprovedOrder.setBankCode(kycPartners.getBankCode());

                // 收款人账户
                kycApprovedOrder.setRecipientAccount(kycPartners.getAccount());

                // 收款人姓名
                kycApprovedOrder.setRecipientName(kycPartners.getName());

                // KYC订单号
                kycApprovedOrder.setOrderId(orderNumberGenerator.generateOrderNo("KYC"));

                kycApprovedOrderService.save(kycApprovedOrder);

                //自动完成订单
                RestResult restResult = sellService.transactionSuccessHandler(kycTransactionMessage.getSellerOrderId(), kycTransactionMessage.getSellerMemberId(), null, null, "3", null);
                if (!restResult.getCode().equals("1")) {
                    log.error("通过 KYC 验证完成订单 处理失败, 自动完成订单处理失败, 卖出订单号: {}, kycTransactionMessage: {}, KYC信息: {}, kycBankTransactions: {}", kycTransactionMessage.getSellerOrderId(), kycTransactionMessage, kycPartners, kycBankTransactions);
                    //自动完成订单处理失败 抛出异常进行回滚
                    throw new RuntimeException("通过 KYC 验证完成订单 处理失败: 自动完成订单处理失败");
                }

                log.info("通过 KYC 验证完成订单 处理成功, 卖出订单号: {}, kycTransactionMessage: {}, bankKycTransactionVo: {}", kycTransactionMessage.getSellerOrderId(), kycTransactionMessage, bankKycTransactionVo);
                return true;
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("通过 KYC 验证完成订单 处理失败, 卖出订单号: {} kycTransactionMessage: {} e: {}", kycTransactionMessage.getSellerOrderId(), kycTransactionMessage, e.getMessage());
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return false;
    }

    private BankKycTransactionVo findTransactionByUTR(List<BankKycTransactionVo> transactions, String utr) {
        for (BankKycTransactionVo transaction : transactions) {
            if (transaction.getUTR().equals(utr)) {
                return transaction;
            }
        }
        return null;
    }


    /**
     * 校验金额 交易类型 交易状态
     *
     * @param transaction
     * @param message
     * @return boolean
     */
    private boolean verifyTransaction(BankKycTransactionVo transaction, KycTransactionMessage message, KycPartners kycPartners) {

        //校验 收款人 UPI_ID是否一致
        if (!message.getRecipientUPI().equals(kycPartners.getUpiId())) {
            log.error("通过 KYC 验证完成订单 处理失败, 收款人 UPI_ID不一致, 卖出订单号: {}, kycTransactionMessage: {}, KYC信息: {}, transaction: {}", message.getSellerOrderId(), message, kycPartners, transaction);
            return false;
        }

        if (transaction.getAmount().compareTo(message.getAmount()) != 0) {
            log.error("通过 KYC 验证完成订单 处理失败, 订单金额不匹配, 卖出订单号: {}, kycTransactionMessage: {}, KYC信息: {}, transaction: {}", message.getSellerOrderId(), message, kycPartners, transaction);
            return false;
        }
        if (!"1".equals(transaction.getMode())) {
            log.error("通过 KYC 验证完成订单 处理失败, 交易类型不是收入, 卖出订单号: {}, kycTransactionMessage: {}, KYC信息: {}, transaction: {}", message.getSellerOrderId(), message, kycPartners, transaction);
            return false;
        }
        if (!"1".equals(transaction.getOrderStatus())) {
            log.error("通过 KYC 验证完成订单 处理失败, 交易状态不是成功, 卖出订单号: {}, kycTransactionMessage: {}, KYC信息: {}, transaction: {}", message.getSellerOrderId(), message, kycPartners, transaction);
            return false;
        }

        return true;
    }


}
