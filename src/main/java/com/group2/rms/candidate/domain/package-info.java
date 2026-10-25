/**
 * Lõi dùng chung của module candidate: entity, repository (mọi câu ghi và các câu đọc từ hai hộp trở lên hoặc module
 * khác dùng), luật trạng thái và quyền xem đơn ({@code rule/}). Không import {@code application}, {@code talentpool}.
 * Phụ trách: dunglt.
 *
 * <p>Hợp đồng với {@code talentpool} (Linh), đổi chữ ký thì báo trước:
 * <ul>
 *   <li>entity {@code Application}, {@code ApplicationReview}, {@code Candidate};</li>
 *   <li>{@code ApplicationRepository}: {@code findDetailById}, {@code findFirstBy…AndApplicationStatus},
 *       {@code existsActiveByCandidateAndJobPosting}, {@code countPendingHrRecommendation},
 *       {@code promoteSuggestionToHmPassed}, {@code transition}; chuỗi {@code FORMAL_FILTER},
 *       {@code HR_RECOMMENDATION}, {@code PENDING_HR_RECOMMENDATION};</li>
 *   <li>{@code ApplicationReviewRepository}: {@code findWithReviewerByApplicationId}, {@code saveAndFlush};
 *       {@code CandidateRepository.findById};</li>
 *   <li>{@code ApplicationAccess.requireView}.</li>
 * </ul>
 */
package com.group2.rms.candidate.domain;
