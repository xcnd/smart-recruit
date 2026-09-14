package com.smartrecruit.offer.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.offer.dto.request.OfferConfirmRequest;
import com.smartrecruit.offer.dto.response.OfferDetailVO;
import com.smartrecruit.offer.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 候选人公开接口（无需登录）。
 *
 * <p>候选人通过邮件中的确认链接访问，无需认证即可查看 Offer 并确认接受或拒绝。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/public/offers")
@RequiredArgsConstructor
@Slf4j
public class CandidateController {

    private final OfferService offerService;

    /**
     * 通过确认 Token 查看 Offer 详情。
     */
    @GetMapping("/{token}")
    public ApiResponse<OfferDetailVO> getByToken(@PathVariable String token) {
        OfferDetailVO vo = offerService.getByConfirmToken(token);
        return ApiResponse.success(vo);
    }

    /**
     * 候选人接受或拒绝 Offer。
     */
    @PostMapping("/{token}/confirm")
    public ApiResponse<Void> confirm(@PathVariable String token,
                                      @RequestBody OfferConfirmRequest request) {
        boolean accept = "accept".equalsIgnoreCase(request.getAction());
        String declineReason = request.getDeclineReason();
        offerService.confirmOffer(token, accept, declineReason);
        return ApiResponse.success();
    }
}
