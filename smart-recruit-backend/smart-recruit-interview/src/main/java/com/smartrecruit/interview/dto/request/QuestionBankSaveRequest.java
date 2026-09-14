package com.smartrecruit.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试题库-套题保存请求（创建/更新共用）。
 *
 * @since 2026-04-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankSaveRequest {

    @NotBlank(message = "套题名称不能为空")
    private String bankName;

    @NotNull(message = "请选择部门")
    private Long departmentId;

    private String departmentName;

    private Long jobPositionId;

    @NotBlank(message = "职位名称不能为空")
    private String jobTitle;

    /** 套题类型：0=技术面,1=项目面,2=行为/HR面,3=综合面。 */
    private Integer questionType = 0;

    /** 难度：1=简单,2=中等,3=困难。 */
    private Integer difficulty = 2;

    private String description;

    /** 状态：0=草稿,1=启用,2=停用。 */
    private Integer status = 1;

    /** 题目列表。 */
    private List<ItemRequest> items = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {

        /** 题型：0=单选,1=多选,2=问答。 */
        private Integer questionType = 0;

        @NotBlank(message = "题目内容不能为空")
        private String question;

        /** 选项列表（问答题可为空）。 */
        private List<String> options;

        private String answer;

        private String explanation;

        /** 难度：1=简单,2=中等,3=困难。 */
        private Integer difficulty = 2;
    }
}
