package com.smartrecruit.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.UserContextUtil;
import com.smartrecruit.interview.dto.request.QuestionBankSaveRequest;
import com.smartrecruit.interview.dto.response.QuestionBankItemVO;
import com.smartrecruit.interview.dto.response.QuestionBankVO;
import com.smartrecruit.interview.entity.QuestionBank;
import com.smartrecruit.interview.entity.QuestionBankItem;
import com.smartrecruit.interview.repository.QuestionBankItemMapper;
import com.smartrecruit.interview.repository.QuestionBankMapper;
import com.smartrecruit.interview.service.InterviewQuestionBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 面试题库服务实现。
 *
 * @since 2026-04-10
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewQuestionBankServiceImpl implements InterviewQuestionBankService {

    private final QuestionBankMapper bankMapper;
    private final QuestionBankItemMapper itemMapper;
    private final ObjectMapper objectMapper;

    /** 分页查询套题。 */
    @Override
    public PageResult<QuestionBankVO> pageQuery(Page<?> page, Map<String, Object> params) {
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBank::getDeleted, 0);
        if (params.get("departmentId") != null && !String.valueOf(params.get("departmentId")).isBlank()) {
            wrapper.eq(QuestionBank::getDepartmentId, Long.valueOf(String.valueOf(params.get("departmentId"))));
        }
        if (params.get("jobTitle") != null && !String.valueOf(params.get("jobTitle")).isBlank()) {
            wrapper.like(QuestionBank::getJobTitle, String.valueOf(params.get("jobTitle")));
        }
        if (params.get("status") != null && !String.valueOf(params.get("status")).isBlank()) {
            wrapper.eq(QuestionBank::getStatus, Integer.valueOf(String.valueOf(params.get("status"))));
        }
        if (params.get("questionType") != null && !String.valueOf(params.get("questionType")).isBlank()) {
            wrapper.eq(QuestionBank::getQuestionType,
                    Integer.valueOf(String.valueOf(params.get("questionType"))));
        }
        if (params.get("keyword") != null && !String.valueOf(params.get("keyword")).isBlank()) {
            String keyword = String.valueOf(params.get("keyword"));
            wrapper.and(w -> w.like(QuestionBank::getBankName, keyword)
                    .or().like(QuestionBank::getJobTitle, keyword));
        }
        if (params.get("startDate") != null && !String.valueOf(params.get("startDate")).isBlank()) {
            wrapper.ge(QuestionBank::getCreateTime, String.valueOf(params.get("startDate")));
        }
        if (params.get("endDate") != null && !String.valueOf(params.get("endDate")).isBlank()) {
            wrapper.le(QuestionBank::getCreateTime,
                    String.valueOf(params.get("endDate")) + " 23:59:59");
        }
        wrapper.orderByDesc(QuestionBank::getId);

        Page<QuestionBank> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<QuestionBank> result = bankMapper.selectPage(mpPage, wrapper);
        List<QuestionBankVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(vos, result.getTotal(), result.getSize(),
                result.getCurrent(), result.getPages());
    }

    /** 查询套题详情（含题目）。 */
    @Override
    public QuestionBankVO getById(Long id) {
        QuestionBank bank = requireBank(id);
        List<QuestionBankItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<QuestionBankItem>()
                        .eq(QuestionBankItem::getDeleted, 0)
                        .eq(QuestionBankItem::getBankId, id)
                        .orderByAsc(QuestionBankItem::getSortOrder));
        QuestionBankVO vo = toVO(bank);
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    /** 新建套题。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionBankVO create(QuestionBankSaveRequest request) {
        QuestionBank bank = new QuestionBank();
        applyBankFields(bank, request);
        bank.setQuestionCount(request.getItems() == null ? 0 : request.getItems().size());
        bank.setCreateTime(DateUtils.now());
        bank.setUpdateTime(DateUtils.now());
        bank.setCreateBy(currentUser());
        bank.setUpdateBy(currentUser());
        bank.setCreateUserId(currentUserId());
        bank.setUpdateUserId(currentUserId());
        bankMapper.insert(bank);
        saveItems(bank.getId(), request.getItems());
        log.info("面试题库套题已创建: id={}, bankName={}", bank.getId(), bank.getBankName());
        return getById(bank.getId());
    }

    /** 更新套题（题目整体替换）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionBankVO update(Long id, QuestionBankSaveRequest request) {
        QuestionBank bank = requireBank(id);
        applyBankFields(bank, request);
        bank.setQuestionCount(request.getItems() == null ? 0 : request.getItems().size());
        bank.setUpdateTime(DateUtils.now());
        bank.setUpdateBy(currentUser());
        bank.setUpdateUserId(currentUserId());
        bankMapper.updateById(bank);
        // 题目整体替换
        itemMapper.delete(new LambdaQueryWrapper<QuestionBankItem>()
                .eq(QuestionBankItem::getBankId, id));
        saveItems(id, request.getItems());
        log.info("面试题库套题已更新: id={}, bankName={}", id, bank.getBankName());
        return getById(id);
    }

    /** 删除套题（逻辑删除套题与题目）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireBank(id);
        bankMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<QuestionBankItem>()
                .eq(QuestionBankItem::getBankId, id));
        log.info("面试题库套题已删除: id={}", id);
    }

    // ==================== 私有方法 ====================

    private QuestionBank requireBank(Long id) {
        QuestionBank bank = bankMapper.selectById(id);
        if (bank == null || bank.getDeleted() != null && bank.getDeleted() == 1) {
            throw new ResourceNotFoundException("面试题库套题不存在: id=" + id);
        }
        return bank;
    }

    private void applyBankFields(QuestionBank bank, QuestionBankSaveRequest request) {
        bank.setBankName(request.getBankName());
        bank.setDepartmentId(request.getDepartmentId());
        bank.setDepartmentName(request.getDepartmentName());
        bank.setJobPositionId(request.getJobPositionId());
        bank.setJobTitle(request.getJobTitle());
        bank.setQuestionType(request.getQuestionType() == null ? 0 : request.getQuestionType());
        bank.setDifficulty(request.getDifficulty() == null ? 2 : request.getDifficulty());
        bank.setDescription(request.getDescription());
        bank.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private void saveItems(Long bankId, List<QuestionBankSaveRequest.ItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<QuestionBankItem> items = new ArrayList<>();
        int sort = 0;
        for (QuestionBankSaveRequest.ItemRequest req : requests) {
            QuestionBankItem item = new QuestionBankItem();
            item.setBankId(bankId);
            item.setQuestionType(req.getQuestionType() == null ? 0 : req.getQuestionType());
            item.setQuestion(req.getQuestion());
            item.setOptions(serializeOptions(req.getOptions()));
            item.setAnswer(req.getAnswer());
            item.setExplanation(req.getExplanation());
            item.setDifficulty(req.getDifficulty() == null ? 2 : req.getDifficulty());
            item.setSortOrder(sort++);
            item.setCreateTime(DateUtils.now());
            item.setUpdateTime(DateUtils.now());
            item.setCreateUserId(currentUserId());
            item.setCreateBy(currentUser());
            item.setUpdateUserId(currentUserId());
            item.setUpdateBy(currentUser());
            items.add(item);
        }
        for (QuestionBankItem item : items) {
            itemMapper.insert(item);
        }
    }

    private QuestionBankVO toVO(QuestionBank bank) {
        return QuestionBankVO.builder()
                .id(bank.getId())
                .bankName(bank.getBankName())
                .departmentId(bank.getDepartmentId())
                .departmentName(bank.getDepartmentName())
                .jobPositionId(bank.getJobPositionId())
                .jobTitle(bank.getJobTitle())
                .questionType(bank.getQuestionType())
                .difficulty(bank.getDifficulty())
                .description(bank.getDescription())
                .questionCount(bank.getQuestionCount())
                .status(bank.getStatus())
                .createTime(bank.getCreateTime())
                .createBy(bank.getCreateBy())
                .build();
    }

    private QuestionBankItemVO toItemVO(QuestionBankItem item) {
        return QuestionBankItemVO.builder()
                .id(item.getId())
                .questionType(item.getQuestionType())
                .question(item.getQuestion())
                .options(parseOptions(item.getOptions()))
                .answer(formatPoints(item.getAnswer()))
                .explanation(formatPoints(item.getExplanation()))
                .difficulty(item.getDifficulty())
                .sortOrder(item.getSortOrder())
                .build();
    }

    /** 选项列表 → JSON 数组文本。 */
    private String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            log.warn("题目选项序列化失败: error={}", e.getMessage());
            return null;
        }
    }

    /** JSON 数组文本 → 选项列表（解析失败返回空列表，不阻断展示）。 */
    private List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("题目选项解析失败: error={}", e.getMessage());
            return List.of();
        }
    }

    /** 编号要点（"；1." / "；2、" 等）转为换行，便于阅读。 */
    private String formatPoints(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text.replaceAll("；\\s*(\\d+)[.、]", "\n$1.");
    }

    private String currentUser() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String username = attrs.getRequest().getHeader("X-Username");
                return username == null || username.isBlank() ? null : username;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 当前登录用户 ID（网关注入 X-User-Id 请求头）。 */
    private Long currentUserId() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return UserContextUtil.getUserIdFromHeader(attrs.getRequest());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
