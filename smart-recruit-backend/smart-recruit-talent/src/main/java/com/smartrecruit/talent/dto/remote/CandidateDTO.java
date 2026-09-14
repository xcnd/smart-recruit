package com.smartrecruit.talent.dto.remote;

import lombok.Data;

import java.util.List;

/**
 * 招聘服务候选人 DTO — 对应 smart-recruit-recruitment 的 CandidateDetailVO 响应。
 *
 * @since 1.0.0
 */
@Data
public class CandidateDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer education;
    private Integer yearsOfExperience;
    private String currentCompany;
    private List<String> skills;
    private Integer source;
    private Integer currentStage;
    private Integer aiMatchScore;
    /** 当前职位（候选人最近任职岗位）。 */
    private String currentPosition;
    private String jobTitle;
    private List<String> tags;
}
