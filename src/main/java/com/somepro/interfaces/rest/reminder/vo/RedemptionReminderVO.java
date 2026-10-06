package com.somepro.interfaces.rest.reminder.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 待赎提醒清单一行（对外 VO，不可变 record）。
 *
 * 柜台照着这一行就能打电话、报数：当票号 / 当户 / 当物 / 到期日期 / 离到期还剩几天；
 * bucket 标明快到期还是已逾期，逾期行另有 overdueDays 标拖欠天数（快到期行为 null），
 * 好按拖欠轻重排催收先后；feeAmount 是利息与综合费合计、totalAmount 是假设今天就来赎的本金加费用，
 * 都照票面快照按今天算定。
 */
public record RedemptionReminderVO(Long ticketId,
                                   String ticketNo,
                                   Long pawnerId,
                                   String pawnerName,
                                   Long collateralId,
                                   String itemName,
                                   LocalDate dueDate,
                                   String bucket,
                                   Integer daysRemaining,
                                   Integer overdueDays,
                                   BigDecimal feeAmount,
                                   BigDecimal totalAmount) implements Serializable {
}
