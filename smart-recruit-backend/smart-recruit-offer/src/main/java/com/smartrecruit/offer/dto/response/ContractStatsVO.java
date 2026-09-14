package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合同统计 VO。
 *
 * @since 2026-04-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractStatsVO {
    private long total;
    private long draftCount;
    private long pendingCount;
    private long sentCount;
    private long signedCount;
    private long archivedCount;
    private long effectiveCount;
}
