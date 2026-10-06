package com.somepro.interfaces.rest.reminder.converter;

import com.somepro.domain.reminder.model.RedemptionReminder;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.reminder.vo.RedemptionReminderVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 待赎提醒读模型 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 */
public final class RedemptionReminderVoConverter {

    private RedemptionReminderVoConverter() {
    }

    public static RedemptionReminderVO toVo(RedemptionReminder domain) {
        return new RedemptionReminderVO(
                domain.ticketId(),
                domain.ticketNo(),
                domain.pawnerId(),
                domain.pawnerName(),
                domain.collateralId(),
                domain.itemName(),
                domain.dueDate(),
                domain.bucket() == null ? null : domain.bucket().code(),
                domain.daysRemaining(),
                domain.overdueDays(),
                domain.feeAmount(),
                domain.totalAmount());
    }

    public static PageVO<RedemptionReminderVO> toPageVo(PageResult<RedemptionReminder> page) {
        List<RedemptionReminderVO> content = page.content().stream()
                .map(RedemptionReminderVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
