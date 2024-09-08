package org.ar.wallet.controller;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.CollectionInfoDTO;
import org.ar.common.pay.req.CollectionInfoIdReq;
import org.ar.common.pay.req.CollectionInfoListPageReq;
import org.ar.common.pay.req.CollectionInfoReq;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.entity.CollectionInfo;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.mapper.CollectionInfoMapper;
import org.ar.wallet.service.ICollectionInfoService;
import org.ar.wallet.service.IMemberInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

/**
 * @author
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = {"/api/v1/collectionInfo", "/collectionInfo"})
@Api(description = "收款信息控制器")
@Validated
@ApiIgnore
public class CollectionInfoController {

    private final ICollectionInfoService collectionInfoService;
    private final CollectionInfoMapper collectionInfoMapper;
    private final IMemberInfoService iMemberInfoService;


    @PostMapping("/update")
    @ApiOperation(value = "修改会员信息")
    public RestResult<CollectionInfoDTO> update(@RequestBody @ApiParam @Valid CollectionInfoReq collectionInfoReq) {
        CollectionInfo collectionInfo = new CollectionInfo();
        BeanUtils.copyProperties(collectionInfoReq, collectionInfo);
        collectionInfo.setDailyLimitCount(collectionInfoReq.getDailyLimitNumber());
        collectionInfoService.updateById(collectionInfo);
        CollectionInfoDTO collectionInfoDTO = new CollectionInfoDTO();
        BeanUtils.copyProperties(collectionInfo, collectionInfoDTO);
        collectionInfoDTO.setDailyLimitNumber(collectionInfo.getDailyLimitCount());
        return RestResult.ok(collectionInfoDTO);
    }


    @PostMapping("/getInfo")
    @ApiOperation(value = "收款信息")
    public RestResult<List<CollectionInfoDTO>> getInfo(@RequestBody @ApiParam @Valid CollectionInfoIdReq collectionInfoIdReq) {
        List<CollectionInfoDTO> list = collectionInfoService.getListByUid(collectionInfoIdReq);
        return RestResult.ok(list);
    }


    @PostMapping("/delete")
    @ApiOperation(value = "删除")
    public RestResult delete(@RequestBody @ApiParam @Valid CollectionInfoIdReq collectionInfoIdReq) {
        CollectionInfo collectionInfo = new CollectionInfo();
        BeanUtils.copyProperties(collectionInfoIdReq, collectionInfo);
        collectionInfo.setDeleted(1);
        collectionInfoService.updateById(collectionInfo);
        return RestResult.ok("删除成功");
    }


    @PostMapping("/add")
    @ApiOperation(value = "新增")
    public RestResult add(@RequestBody @ApiParam @Valid CollectionInfoReq collectionInfoIdReq) {
        String updateBy = UserContext.getCurrentUserName();
        CollectionInfo collectionInfo = new CollectionInfo();
        MemberInfo memberInfo = iMemberInfoService.getMemberInfoById(collectionInfoIdReq.getMemberId());
        collectionInfo.setCreateBy(updateBy);
        collectionInfo.setUpdateBy(updateBy);
        BeanUtils.copyProperties(collectionInfoIdReq, collectionInfo);
        collectionInfo.setMemberAccount(memberInfo.getMemberAccount());
        collectionInfoMapper.insert(collectionInfo);
        return RestResult.ok();
    }

    @PostMapping("/listPage")
    @ApiOperation(value = "收款列表")
    public RestResult<CollectionInfoDTO> listPage(@RequestBody @ApiParam @Valid CollectionInfoListPageReq req) {
        PageReturn<CollectionInfoDTO> pageCollectionInfo = collectionInfoService.listPage(req);

        return RestResult.page(pageCollectionInfo);
    }
}
