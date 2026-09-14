package com.smartrecruit.offer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.exception.ValidationException;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.UserContextUtil;
import com.smartrecruit.offer.dto.remote.NotificationRequest;
import com.smartrecruit.offer.dto.remote.WorkbenchTaskRequest;
import com.smartrecruit.offer.dto.response.ContractDetailVO;
import com.smartrecruit.offer.dto.response.ContractSignRecordVO;
import com.smartrecruit.offer.dto.response.ContractStatsVO;
import com.smartrecruit.offer.dto.response.ContractVO;
import com.smartrecruit.offer.entity.Contract;
import com.smartrecruit.offer.entity.ContractSignRecord;
import com.smartrecruit.offer.entity.Offer;
import com.smartrecruit.offer.repository.ContractMapper;
import com.smartrecruit.offer.repository.ContractSignRecordMapper;
import com.smartrecruit.offer.repository.OfferMapper;
import com.smartrecruit.offer.feign.RecruitmentClient;
import com.smartrecruit.offer.feign.SystemClient;
import com.smartrecruit.offer.service.ContractService;
import com.smartrecruit.offer.service.OfferMailService;
import com.smartrecruit.offer.service.RemoteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 合同管理服务实现。
 *
 * @since 2026-04-09
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    /** 合同状态。 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_SENT = 3;
    public static final int STATUS_SIGNED = 4;
    public static final int STATUS_REJECTED = 5;
    public static final int STATUS_ARCHIVED = 6;
    public static final int STATUS_EFFECTIVE = 7;
    public static final int STATUS_VOID = 8;

    private static final String COMPANY_NAME = "SmartRecruit 科技有限公司";

    private final ContractMapper contractMapper;
    private final ContractSignRecordMapper signRecordMapper;
    private final OfferMapper offerMapper;
    private final SystemClient systemClient;
    private final RecruitmentClient recruitmentClient;
    private final OfferMailService offerMailService;
    private final RemoteConfigService remoteConfigService;

    /** 分页查询合同。 */
    @Override
    public PageResult<ContractVO> pageQuery(Integer page, Integer size, String keyword,
                                            Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Contract::getContractNo, kw)
                    .or().like(Contract::getCandidateName, kw)
                    .or().like(Contract::getJobTitle, kw));
        }
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(Contract::getCreateTime,
                    DateUtils.parseDate(startDate.trim()).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(Contract::getCreateTime,
                    DateUtils.parseDate(endDate.trim()).atTime(23, 59, 59));
        }
        wrapper.orderByDesc(Contract::getCreateTime).orderByDesc(Contract::getId);

        IPage<Contract> result = contractMapper.selectPage(
                new Page<>(page == null ? 1 : page, size == null ? 20 : size), wrapper);
        List<ContractVO> records = result.getRecords().stream()
                .map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getSize(),
                result.getCurrent(), result.getPages());
    }

    /** 合同统计。 */
    @Override
    public ContractStatsVO getStats() {
        return ContractStatsVO.builder()
                .total(countByStatus(null))
                .draftCount(countByStatus(STATUS_DRAFT))
                .pendingCount(countByStatus(STATUS_PENDING))
                .sentCount(countByStatus(STATUS_SENT))
                .signedCount(countByStatus(STATUS_SIGNED))
                .archivedCount(countByStatus(STATUS_ARCHIVED))
                .effectiveCount(countByStatus(STATUS_EFFECTIVE))
                .build();
    }

    /** 合同详情。 */
    @Override
    public ContractDetailVO getDetail(Long id) {
        Contract contract = requireContract(id);
        return toDetailVO(contract);
    }

    /** 通过签署 Token 获取合同。 */
    @Override
    public ContractDetailVO getBySignToken(String token) {
        Contract contract = contractMapper.selectOne(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getSignToken, token));
        if (contract == null) {
            throw new ResourceNotFoundException("合同签署链接无效或已失效");
        }
        return toDetailVO(contract);
    }

    /** 从 Offer 创建合同。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractVO createFromOffer(Long offerId, String operator) {
        Offer offer = offerMapper.selectById(offerId);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer 不存在: id=" + offerId);
        }

        LocalDateTime now = DateUtils.now();
        Contract contract = new Contract();
        contract.setOfferId(offer.getId());
        contract.setCandidateId(offer.getCandidateId());
        contract.setCandidateName(offer.getCandidateName());
        contract.setCandidateEmail(offer.getCandidateEmail());
        contract.setJobTitle(offer.getPositionTitle());
        contract.setDepartmentName(offer.getDepartmentName());
        contract.setOfferNo(offer.getOfferNo());
        contract.setTotalPackage(extractTotalPackage(offer));
        contract.setContent(buildContent(contract, offer));
        contract.setStatus(STATUS_DRAFT);
        LocalDate validFrom = offer.getExpectedOnboardDate() != null
                ? offer.getExpectedOnboardDate() : DateUtils.today();
        contract.setValidFrom(validFrom);
        contract.setValidUntil(validFrom.plusYears(3));
        contract.setCreateBy(operator);
        contract.setCreateUserId(currentUserId());
        contract.setCreateTime(now);
        contract.setUpdateUserId(currentUserId());
        contract.setUpdateTime(now);
        contract.setDeleted(0);
        // 并发创建时可能撞号：唯一键冲突则重新生成编号后重试（最多 5 次）
        for (int attempt = 0; ; attempt++) {
            contract.setContractNo(generateContractNo());
            try {
                contractMapper.insert(contract);
                break;
            } catch (DuplicateKeyException e) {
                if (attempt >= 4) {
                    throw e;
                }
                log.warn("合同编号冲突，重新生成: attempt={}", attempt + 1);
            }
        }

        log.info("合同创建成功: contractNo={}, offerId={}, operator={}",
                contract.getContractNo(), offerId, operator);
        return toVO(contract);
    }

    /** 提交审批。 */
    @Override
    public void submitApproval(Long id, String operator) {
        Contract contract = requireContract(id);
        requireStatus(contract, STATUS_DRAFT, "仅草稿合同可提交审批");
        updateStatus(contract, STATUS_PENDING, operator);
        notifyApprovers(contract,
                "合同待审批 - " + personLabel(contract),
                "候选人 " + personLabel(contract) + " 的合同「" + contract.getContractNo()
                        + "」已提交审批，请及时处理。",
                NotificationConstants.BIZ_CONTRACT_APPROVAL_REQUEST);
        createTask(0L, contract,
                "审批合同 - " + contract.getCandidateName(),
                "候选人 " + contract.getCandidateName() + " 的合同「" + contract.getContractNo()
                        + "」已提交审批，请尽快审批。",
                1, 2, 2);
    }

    /** 审批通过。 */
    @Override
    public void approve(Long id, String operator) {
        Contract contract = requireContract(id);
        requireStatus(contract, STATUS_PENDING, "仅待审批合同可审批");
        updateStatus(contract, STATUS_APPROVED, operator);
        Long creatorId = creatorId(contract);
        String title = "合同审批通过 - " + personLabel(contract);
        String content = "候选人 " + personLabel(contract) + " 的合同「" + contract.getContractNo()
                + "」已审批通过，请发送候选人签署。";
        if (creatorId != null) {
            notifyUser(creatorId, title, content,
                    NotificationConstants.BIZ_CONTRACT_APPROVAL_COMPLETE, contract.getId());
            createTask(creatorId, contract,
                    "发送合同签署 - " + contract.getCandidateName(),
                    "合同「" + contract.getContractNo() + "」已审批通过，请发送给候选人 "
                            + contract.getCandidateName() + " 签署。",
                    5, 1, 3);
        } else {
            notifyApprovers(contract, title, content,
                    NotificationConstants.BIZ_CONTRACT_APPROVAL_COMPLETE);
            createTask(0L, contract,
                    "发送合同签署 - " + contract.getCandidateName(),
                    "合同「" + contract.getContractNo() + "」已审批通过，请发送给候选人 "
                            + contract.getCandidateName() + " 签署。",
                    5, 1, 3);
        }
    }

    /** 发送给候选人签署。 */
    @Override
    public void send(Long id, String operator) {
        Contract contract = requireContract(id);
        requireStatus(contract, STATUS_APPROVED, "仅已审批合同可发送");
        contract.setSignToken(UUID.randomUUID().toString().replace("-", "") + DateUtils.now().getNano());
        updateStatus(contract, STATUS_SENT, operator);
        // 发送前用最新公司信息与签订日期刷新合同正文（兼容此前生成的历史合同）
        if (contract.getOfferId() != null) {
            Offer offer = offerMapper.selectById(contract.getOfferId());
            if (offer != null) {
                contract.setContent(buildContent(contract, offer));
                contract.setUpdateTime(DateUtils.now());
                contract.setUpdateBy(operator);
                contract.setUpdateUserId(currentUserId());
                contractMapper.updateById(contract);
            }
        }
        // 异步发送签署邮件到候选人邮箱，接口立即返回，不阻塞用户操作
        CompletableFuture.runAsync(() -> offerMailService.sendContractForSigning(contract))
                .exceptionally(e -> {
                    log.warn("合同邮件异步发送异常（可忽略）: contractNo={}, error={}",
                            contract.getContractNo(), e.getMessage());
                    return null;
                });
    }

    /** HR 签署。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hrSign(Long id, String signerName, String operator) {
        Contract contract = requireContract(id);
        int status = contract.getStatus() == null ? STATUS_DRAFT : contract.getStatus();
        if (status != STATUS_APPROVED && status != STATUS_SENT && status != STATUS_SIGNED) {
            throw new ValidationException("当前状态不可 HR 签署");
        }
        contract.setSignedByHr(signerName);
        contract.setUpdateBy(operator);
        contract.setUpdateUserId(currentUserId());
        contract.setUpdateTime(DateUtils.now());
        if (status == STATUS_SIGNED) {
            // 候选人已签署，HR 签署后合同生效
            contract.setStatus(STATUS_EFFECTIVE);
            contract.setSignTime(DateUtils.now());
        } else if (contract.getStatus() == STATUS_APPROVED) {
            contract.setStatus(STATUS_SENT);
        } else {
            // 已发送且候选人已签署：同步为已签署；候选人未签署则仅记录 HR 签署
            if (contract.getSignedByCandidate() != null) {
                contract.setStatus(STATUS_SIGNED);
                contract.setSignTime(DateUtils.now());
            }
        }
        contractMapper.updateById(contract);
        insertSignRecord(contract.getId(), 0, null, signerName, 1, null, "HR 签署");
    }

    /** 候选人签署/拒绝。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signByCandidate(String token, String name, boolean accept, String remark,
                                String signature, String idCard, String phone,
                                String address, String ip) {
        Contract contract = contractMapper.selectOne(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getSignToken, token));
        if (contract == null) {
            throw new ResourceNotFoundException("合同签署链接无效或已失效");
        }
        if (contract.getStatus() != STATUS_SENT) {
            throw new ValidationException("合同当前状态不可签署，请刷新后重试");
        }
        if (accept) {
            contract.setSignedByCandidate(name);
            contract.setCandidateSignature(signature);
            contract.setCandidateIdCard(idCard);
            contract.setCandidatePhone(phone);
            contract.setCandidateAddress(address);
            contract.setStatus(STATUS_SIGNED);
            contract.setSignTime(DateUtils.now());
            contract.setUpdateTime(DateUtils.now());
            contractMapper.updateById(contract);
            insertSignRecord(contract.getId(), 1, null, name, 1, ip, "候选人确认签署");
            notifyApprovers(contract,
                    "候选人已签署合同 - " + personLabel(contract),
                    "候选人 " + personLabel(contract) + " 已确认签署合同「" + contract.getContractNo()
                            + "」，请及时归档。",
                    NotificationConstants.BIZ_CONTRACT_SIGNED);
            createTask(0L, contract,
                    "合同已签署 - " + contract.getCandidateName(),
                    "候选人 " + contract.getCandidateName() + " 已签署合同「" + contract.getContractNo()
                            + "」，请归档。",
                    5, 1, 1);
        } else {
            contract.setStatus(STATUS_REJECTED);
            contract.setRejectReason(remark);
            contract.setUpdateTime(DateUtils.now());
            contractMapper.updateById(contract);
            insertSignRecord(contract.getId(), 1, null, name, 2, ip,
                    remark == null ? "候选人拒绝签署" : remark);
            notifyApprovers(contract,
                    "候选人拒绝签署合同 - " + personLabel(contract),
                    "候选人 " + personLabel(contract) + " 拒绝签署合同「" + contract.getContractNo()
                            + "」" + (remark != null ? "，原因：" + remark : "") + "，请及时处理。",
                    NotificationConstants.BIZ_CONTRACT_REJECTED);
            createTask(0L, contract,
                    "合同被拒绝 - " + contract.getCandidateName(),
                    "候选人 " + contract.getCandidateName() + " 拒绝签署合同「" + contract.getContractNo()
                            + "」，请跟进处理。",
                    5, 2, 2);
        }
        log.info("候选人签署处理完成: contractNo={}, accept={}, ip={}",
                contract.getContractNo(), accept, ip);
    }

    /** 合同生效（仅已签署合同可生效）。 */
    @Override
    public void makeEffective(Long id, String operator) {
        Contract contract = requireContract(id);
        requireStatus(contract, STATUS_SIGNED, "仅已签署合同可生效");
        updateStatus(contract, STATUS_EFFECTIVE, operator);
        log.info("合同已生效: contractNo={}, operator={}", contract.getContractNo(), operator);
    }

    /** 合同作废（仅已签署/生效中合同可作废）。 */
    @Override
    public void voidContract(Long id, String reason, String operator) {
        Contract contract = requireContract(id);
        if (contract.getStatus() != STATUS_SIGNED && contract.getStatus() != STATUS_EFFECTIVE) {
            throw new ValidationException("仅已签署或生效中的合同可作废");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("作废原因不能为空");
        }
        contract.setVoidReason(reason.trim());
        updateStatus(contract, STATUS_VOID, operator);
        log.info("合同已作废: contractNo={}, operator={}, reason={}",
                contract.getContractNo(), operator, reason);
    }

    /** 删除（仅草稿/已拒绝）。 */
    @Override
    public void delete(Long id, String operator) {
        Contract contract = requireContract(id);
        if (contract.getStatus() != STATUS_DRAFT && contract.getStatus() != STATUS_REJECTED) {
            throw new ValidationException("仅草稿或已拒绝的合同可删除");
        }
        contract.setUpdateBy(operator);
        contract.setUpdateUserId(currentUserId());
        contract.setUpdateTime(DateUtils.now());
        contractMapper.deleteById(contract.getId());
        log.info("合同已删除: contractNo={}, operator={}", contract.getContractNo(), operator);
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    private Contract requireContract(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new ResourceNotFoundException("合同不存在: id=" + id);
        }
        return contract;
    }

    private void requireStatus(Contract contract, int expected, String message) {
        if (contract.getStatus() == null || contract.getStatus() != expected) {
            throw new ValidationException(message);
        }
    }

    private void updateStatus(Contract contract, int status, String operator) {
        contract.setStatus(status);
        contract.setUpdateBy(operator);
        contract.setUpdateUserId(currentUserId());
        contract.setUpdateTime(DateUtils.now());
        contractMapper.updateById(contract);
    }

    /** 当前登录用户 ID（内部调用或未登录时为 null）。 */
    private Long currentUserId() {
        return UserContextUtil.getCurrentUserId();
    }

    private void insertSignRecord(Long contractId, int signerType, Long signerId,
                                  String signerName, int action, String ip, String remark) {
        ContractSignRecord record = new ContractSignRecord();
        record.setContractId(contractId);
        record.setSignerType(signerType);
        record.setSignerId(signerId);
        record.setSignerName(signerName);
        record.setAction(action);
        record.setSignTime(DateUtils.now());
        record.setSignIp(ip);
        record.setRemark(remark);
        signRecordMapper.insert(record);
    }

    private long countByStatus(Integer status) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        }
        Long count = contractMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    private String generateContractNo() {
        int year = DateUtils.now().getYear();
        Long maxSeq = contractMapper.selectMaxContractSeq(year);
        int next = (maxSeq == null ? 0 : maxSeq.intValue()) + 1;
        return "CT-" + year + "-" + String.format("%04d", next);
    }

    /** 从 Offer 薪资结构中提取年薪总包。 */
    private BigDecimal extractTotalPackage(Offer offer) {
        if (offer.getSalaryStructure() instanceof Map<?, ?> structure
                && structure.get("totalPackage") instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return null;
    }

    /** 根据 Offer 信息构建正式规范的录用合同正文。 */
    private String buildContent(Contract contract, Offer offer) {
        Map<?, ?> salary = offer.getSalaryStructure() instanceof Map<?, ?> s ? s : Map.of();
        BigDecimal baseSalary = num(salary.get("baseSalary"));
        int bonusMonths = intOf(salary.get("bonusMonths"));
        BigDecimal stockOptions = num(salary.get("stockOptions"));
        BigDecimal signOnBonus = num(salary.get("signOnBonus"));
        BigDecimal totalPackage = contract.getTotalPackage() != null
                ? contract.getTotalPackage() : num(salary.get("totalPackage"));
        LocalDate validFrom = contract.getValidFrom() != null
                ? contract.getValidFrom() : DateUtils.today();
        LocalDate validUntil = contract.getValidUntil() != null
                ? contract.getValidUntil() : validFrom.plusYears(3);
        String durationYears = String.valueOf(
                Math.max(1, Period.between(validFrom, validUntil).getYears()));
        int probationMonths = offer.getProbationMonths() != null ? offer.getProbationMonths() : 0;
        String probationMonthsText = probationMonths > 0 ? probationMonths + " 个月" : "按公司制度执行";
        String probationRatio = offer.getProbationSalaryRatio() != null
                ? offer.getProbationSalaryRatio().multiply(BigDecimal.valueOf(100))
                        .stripTrailingZeros().toPlainString() + "%"
                : "80%";
        BigDecimal probationSalary = offer.getProbationSalaryRatio() != null
                ? baseSalary.multiply(offer.getProbationSalaryRatio()) : baseSalary;

        // 甲方信息：从系统配置自动带出，缺失时用占位线
        String companyName = remoteConfigService.getString(
                ConfigKeys.COMPANY_NAME, COMPANY_NAME);
        String creditCode = remoteConfigService.getString(ConfigKeys.COMPANY_CREDIT_CODE, "");
        String address = remoteConfigService.getString(ConfigKeys.COMPANY_ADDRESS, "");
        String legalRep = remoteConfigService.getString(ConfigKeys.COMPANY_LEGAL_REPRESENTATIVE, "");

        return "甲方（用人单位）：" + companyName + "\n"
                + "统一社会信用代码：" + (creditCode.isBlank() ? "________________" : creditCode) + "\n"
                + "住所：" + (address.isBlank() ? "________________" : address) + "\n"
                + "法定代表人：" + (legalRep.isBlank() ? "________________" : legalRep) + "\n\n"
                + "乙方（劳动者）：" + nvl(contract.getCandidateName()) + "\n"
                + "身份证号码：________________\n"
                + "联系电话：________________\n"
                + "通讯地址：________________\n\n"
                + "鉴于乙方有意入职甲方工作，甲方同意录用乙方。依据《中华人民共和国劳动法》"
                + "《中华人民共和国劳动合同法》等法律法规，甲乙双方本着平等自愿、协商一致的原则"
                + "订立本合同，共同遵照执行。\n\n"
                + "第一条　合同期限与试用期\n"
                + "1.1 本合同为固定期限劳动合同，合同期限自 " + nvl(validFrom)
                + " 起至 " + nvl(validUntil) + " 止，共计 " + durationYears + " 年。\n"
                + "1.2 试用期为 " + probationMonthsText + "。试用期内，如乙方不符合录用条件，"
                + "甲方可依法解除本合同；乙方亦可提前三日书面通知甲方解除本合同。\n"
                + "1.3 乙方入职日期以甲方出具的《录用通知书》载明日期为准，预计为 "
                + nvl(offer.getExpectedOnboardDate()) + "。\n\n"
                + "第二条　工作内容与工作地点\n"
                + "2.1 甲方聘用乙方担任 " + nvl(contract.getJobTitle()) + " 岗位"
                + (offer.getLevel() != null ? "（职级 " + offer.getLevel() + "）" : "")
                + "，隶属于 " + nvl(contract.getDepartmentName())
                + "，具体职责以甲方《岗位说明书》及双方另行约定为准。\n"
                + "2.2 乙方应勤勉尽责，遵守甲方各项规章制度，并服从甲方合理的岗位调配与工作安排。\n"
                + "2.3 乙方工作地点为甲方注册地或业务所在地；甲方因经营需要调整乙方工作地点的，"
                + "应与乙方协商一致。\n\n"
                + "第三条　工作时间与休息休假\n"
                + "3.1 甲方实行标准工时制，每日工作时间不超过八小时，平均每周不超过四十小时。\n"
                + "3.2 甲方确需安排乙方加班的，应依法支付加班工资或安排补休。\n"
                + "3.3 乙方依法享有法定节假日、带薪年休假、婚丧假、产假等休息休假权利。\n\n"
                + "第四条　劳动报酬\n"
                + "4.1 乙方试用期税前月工资为人民币 " + money(probationSalary)
                + " 元；转正后税前月工资为人民币 " + money(baseSalary)
                + " 元（含基本工资、岗位工资及绩效工资基数）。\n"
                + "4.2 甲方根据公司经营状况及乙方年度绩效考核结果发放年终奖金，"
                + "奖金基数为 " + bonusMonths + " 个月月薪，具体以甲方薪酬制度为准。\n"
                + "4.3 甲方可向乙方提供股权/期权激励（折合价值约 " + money(stockOptions)
                + " 元）及签约奖金（" + money(signOnBonus) + " 元），"
                + "具体以甲方另行出具的激励文件为准。\n"
                + "4.4 乙方年度总薪酬（含月工资、年终奖金、股权/期权及签约奖金等）预计为人民币 "
                + money(totalPackage) + " 元。\n"
                + "4.5 甲方于每月固定发薪日通过银行转账足额支付乙方工资，"
                + "并依法代扣代缴个人所得税及社会保险、住房公积金个人缴费部分。\n\n"
                + "第五条　社会保险与福利\n"
                + "5.1 甲方依法为乙方缴纳基本养老、医疗、失业、工伤、生育保险及住房公积金。\n"
                + "5.2 甲方提供补充商业保险、年度体检、带薪休假等福利，具体以甲方福利制度为准。\n\n"
                + "第六条　劳动保护、劳动条件与职业危害防护\n"
                + "6.1 甲方为乙方提供符合国家标准的劳动安全卫生条件和必要的劳动保护用品。\n"
                + "6.2 乙方在劳动过程中应严格遵守安全操作规程；甲方对乙方进行必要的安全生产教育和培训。\n\n"
                + "第七条　保密义务\n"
                + "7.1 乙方在任职期间及离职后，应对其在甲方知悉的包括但不限于技术资料、源代码、"
                + "算法模型、经营信息、客户信息等商业秘密承担保密义务，未经甲方书面同意，"
                + "不得向任何第三方披露、使用或许可他人使用。\n"
                + "7.2 乙方应妥善保管甲方提供的涉密载体与系统账号权限，不得用于与工作无关的用途。\n\n"
                + "第八条　知识产权\n"
                + "8.1 乙方在履行职务过程中单独或共同完成的、与工作内容相关的职务作品、"
                + "发明创造、技术成果等，其知识产权（包括但不限于著作权、专利权、软件著作权）"
                + "归甲方所有。\n"
                + "8.2 乙方应按照甲方要求及时披露相关成果，并协助甲方办理权利登记与申请事宜。\n\n"
                + "第九条　竞业限制\n"
                + "如甲方与乙方另行签订《竞业限制协议》，双方应按协议约定履行；"
                + "竞业限制经济补偿按法律规定及协议执行。\n\n"
                + "第十条　规章制度与劳动纪律\n"
                + "10.1 甲方依法制定并公示的各项规章制度（包括但不限于《员工手册》"
                + "《信息安全管理制度》）为本合同附件，乙方应自觉遵守。\n"
                + "10.2 乙方违反规章制度或劳动纪律的，甲方有权依法依规处理。\n\n"
                + "第十一条　合同变更、解除与终止\n"
                + "11.1 经双方协商一致，可以书面形式变更或解除本合同。\n"
                + "11.2 本合同的解除与终止依照《中华人民共和国劳动合同法》第四章的规定执行；"
                + "甲方依法应向乙方支付经济补偿的，应足额支付。\n\n"
                + "第十二条　违约责任\n"
                + "任何一方违反本合同约定给对方造成损失的，应依法承担赔偿责任；"
                + "法律另有规定的，从其规定。\n\n"
                + "第十三条　争议处理\n"
                + "因本合同发生的劳动争议，双方应先行协商；协商不成的，"
                + "可依法向甲方所在地劳动争议仲裁委员会申请仲裁，对仲裁裁决不服的，"
                + "可依法向人民法院提起诉讼。\n\n"
                + "第十四条　其他约定\n"
                + "14.1 本合同未尽事宜，按国家有关法律法规及甲方规章制度执行，"
                + "或由双方另行签订补充协议。\n"
                + "14.2 乙方确认本合同载明的通讯地址、联系电话为有效联系方式；"
                + "甲方按该地址送达相关文书的，视为有效送达，联系方式变更的，"
                + "乙方应及时书面通知甲方。\n"
                + "14.3 本合同一式两份，甲乙双方各执一份，经双方签字（盖章）后生效，"
                + "具有同等法律效力。";
    }

    private static BigDecimal num(Object value) {
        return value instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    }

    private static int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    /** 金额格式化：千分位。 */
    private static String money(BigDecimal value) {
        if (value == null || value.signum() == 0) {
            return "0";
        }
        return String.format("%,.0f", value);
    }

    private static String nvl(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private ContractVO toVO(Contract c) {
        return ContractVO.builder()
                .id(c.getId())
                .contractNo(c.getContractNo())
                .offerId(c.getOfferId())
                .candidateId(c.getCandidateId())
                .candidateName(c.getCandidateName())
                .candidateEmail(c.getCandidateEmail())
                .jobTitle(c.getJobTitle())
                .departmentName(c.getDepartmentName())
                .offerNo(c.getOfferNo())
                .totalPackage(c.getTotalPackage())
                .status(c.getStatus())
                .statusLabel(statusLabel(c.getStatus()))
                .signedByHr(c.getSignedByHr())
                .signedByCandidate(c.getSignedByCandidate())
                .voidReason(c.getVoidReason())
                .signTime(c.getSignTime())
                .validFrom(c.getValidFrom())
                .validUntil(c.getValidUntil())
                .createBy(c.getCreateBy())
                .createTime(c.getCreateTime())
                .remark(c.getRemark())
                .build();
    }

    /** 人员展示标签：姓名（部门 · 岗位）。 */
    private String personLabel(Contract contract) {
        String dept = contract.getDepartmentName() != null && !contract.getDepartmentName().isBlank()
                ? contract.getDepartmentName() : null;
        String job = contract.getJobTitle() != null && !contract.getJobTitle().isBlank()
                ? contract.getJobTitle() : null;
        if (dept != null && job != null) {
            return contract.getCandidateName() + "（" + dept + " · " + job + "）";
        }
        if (dept != null) {
            return contract.getCandidateName() + "（" + dept + "）";
        }
        if (job != null) {
            return contract.getCandidateName() + "（" + job + "）";
        }
        return contract.getCandidateName();
    }

    private ContractDetailVO toDetailVO(Contract c) {
        List<ContractSignRecordVO> records = signRecordMapper.selectList(
                        new LambdaQueryWrapper<ContractSignRecord>()
                                .eq(ContractSignRecord::getContractId, c.getId())
                                .orderByAsc(ContractSignRecord::getSignTime))
                .stream().map(this::toSignVO).toList();
        return ContractDetailVO.builder()
                .id(c.getId())
                .contractNo(c.getContractNo())
                .offerId(c.getOfferId())
                .candidateId(c.getCandidateId())
                .candidateName(c.getCandidateName())
                .candidateEmail(c.getCandidateEmail())
                .jobTitle(c.getJobTitle())
                .departmentName(c.getDepartmentName())
                .offerNo(c.getOfferNo())
                .totalPackage(c.getTotalPackage())
                .content(c.getContent())
                .status(c.getStatus())
                .statusLabel(statusLabel(c.getStatus()))
                .signedByHr(c.getSignedByHr())
                .signedByCandidate(c.getSignedByCandidate())
                .candidateSignature(c.getCandidateSignature())
                .candidateIdCard(c.getCandidateIdCard())
                .candidatePhone(c.getCandidatePhone())
                .candidateAddress(c.getCandidateAddress())
                .signTime(c.getSignTime())
                .rejectReason(c.getRejectReason())
                .voidReason(c.getVoidReason())
                .validFrom(c.getValidFrom())
                .validUntil(c.getValidUntil())
                .remark(c.getRemark())
                .createBy(c.getCreateBy())
                .createTime(c.getCreateTime())
                .signRecords(records)
                .build();
    }

    private ContractSignRecordVO toSignVO(ContractSignRecord r) {
        return ContractSignRecordVO.builder()
                .id(r.getId())
                .contractId(r.getContractId())
                .signerType(r.getSignerType())
                .signerTypeLabel(r.getSignerType() != null && r.getSignerType() == 1 ? "候选人" : "HR")
                .signerName(r.getSignerName())
                .action(r.getAction())
                .actionLabel(r.getAction() != null && r.getAction() == 2 ? "拒绝" : "签署")
                .signTime(r.getSignTime())
                .signIp(r.getSignIp())
                .remark(r.getRemark())
                .build();
    }

    private static String statusLabel(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PENDING -> "待审批";
            case STATUS_APPROVED -> "已审批";
            case STATUS_SENT -> "待签署";
            case STATUS_SIGNED -> "已签署";
            case STATUS_REJECTED -> "已拒绝";
            case STATUS_ARCHIVED -> "已归档";
            case STATUS_EFFECTIVE -> "生效中";
            case STATUS_VOID -> "已作废";
            default -> "未知";
        };
    }

    // ================================================================
    // 消息通知与待办（best-effort，失败不影响主流程）
    // ================================================================

    /** 解析合同创建人用户 ID（createBy 存用户 ID；兼容旧数据用户名）。 */
    private Long creatorId(Contract contract) {
        if (contract.getCreateUserId() != null && contract.getCreateUserId() > 0) {
            return contract.getCreateUserId();
        }
        if (contract.getCreateBy() == null || contract.getCreateBy().isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(contract.getCreateBy());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            try {
                ApiResponse<Long> resp = systemClient.getUserIdByUsername(contract.getCreateBy());
                return resp != null && resp.data() != null ? resp.data() : null;
            } catch (Exception ex) {
                log.warn("解析合同创建人失败: createBy={}, error={}",
                        contract.getCreateBy(), ex.getMessage());
                return null;
            }
        }
    }

    /** 通知单个用户（跳转到合同详情）。 */
    private void notifyUser(Long userId, String title, String content, String businessType, Long contractId) {
        if (userId == null) {
            return;
        }
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setTitle(title);
            request.setContent(content);
            request.setType(NotificationConstants.TYPE_CONTRACT);
            request.setBusinessType(businessType);
            request.setBusinessId(contractId);
            request.setActionUrl("/contracts/" + contractId);
            systemClient.sendNotification(request);
        } catch (Exception e) {
            log.warn("合同通知发送失败（可忽略）: userId={}, title={}, error={}",
                    userId, title, e.getMessage());
        }
    }

    /** 批量通知（设置 userIds 字段）。 */
    private void notifyUsers(List<Long> userIds, String title, String content,
                             String businessType, Long contractId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserIds(userIds);
            request.setTitle(title);
            request.setContent(content);
            request.setType(NotificationConstants.TYPE_CONTRACT);
            request.setBusinessType(businessType);
            request.setBusinessId(contractId);
            request.setActionUrl("/contracts/" + contractId);
            systemClient.sendNotification(request);
        } catch (Exception e) {
            log.warn("合同通知批量发送失败（可忽略）: title={}, error={}", title, e.getMessage());
        }
    }

    /** 通知所有管理员（合同审批人）。 */
    private void notifyApprovers(Contract contract, String title, String content, String businessType) {
        try {
            ApiResponse<List<Long>> resp = systemClient.getAdminIds();
            List<Long> admins = resp != null ? resp.data() : null;
            if (admins == null || admins.isEmpty()) {
                log.debug("无管理员可通知，跳过: contractNo={}", contract.getContractNo());
                return;
            }
            notifyUsers(admins, title, content, businessType, contract.getId());
        } catch (Exception e) {
            log.warn("查询管理员失败（可忽略）: error={}", e.getMessage());
        }
    }

    /** 创建工作台待办任务（招聘服务）。 */
    private void createTask(Long userId, Contract contract, String title, String description,
                            int taskType, int priority, int daysUntilDue) {
        try {
            WorkbenchTaskRequest task = new WorkbenchTaskRequest();
            task.setUserId(userId != null ? userId : 0L);
            task.setTitle(title);
            task.setDescription(description);
            task.setType(taskType);
            task.setPriority(priority);
            task.setRelatedType("CONTRACT");
            task.setRelatedId(contract.getId());
            task.setCandidateName(contract.getCandidateName());
            if (daysUntilDue > 0) {
                task.setDueDate(DateUtils.now().plusDays(daysUntilDue));
            }
            recruitmentClient.createTask(task);
            log.info("合同待办已创建: contractNo={}, title={}", contract.getContractNo(), title);
        } catch (Exception e) {
            log.warn("合同待办创建失败（可忽略）: contractNo={}, error={}",
                    contract.getContractNo(), e.getMessage());
        }
    }
}
