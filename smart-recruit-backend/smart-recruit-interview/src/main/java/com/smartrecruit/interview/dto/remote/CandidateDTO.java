package com.smartrecruit.interview.dto.remote;

import lombok.Data;

import java.time.LocalDate;

/**
 * 招聘服务候选人 DTO - 对应 smart-recruit-recruitment 的 CandidateDetailVO 响应。
 *
 * @since 1.0.0
 */
@Data
public class CandidateDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer gender;
    private LocalDate birthDate;
    private Integer education;
    private String school;
    private String major;
    private String city;
    private String currentCompany;
    private String currentPosition;
    private Integer yearsOfExperience;
}
