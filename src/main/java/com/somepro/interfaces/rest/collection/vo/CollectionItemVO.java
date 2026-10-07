package com.somepro.interfaces.rest.collection.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 催收工作台一行（对外 VO，不可变 record）。
 *
 * 摆清楚两拨票（bucket：DUE_SOON 快到期 / OVERDUE 已逾期）与柜台接电话最常被问的两笔数：
 * redeemTotalAmount「今天来赎要还多少」、renewNewDueDate「今天来续之后到哪天」
 * （已逾期今天续不了，该字段为 null，按全局 non_null 不输出）。
 * overdueDays 只在已逾期档有值；刻意不带 delFlag / 审计字段等内部列。
 */
public record CollectionItemVO(String bucket,
                               String bucketLabel,
                               Long ticketId,
                               String ticketNo,
                               Long pawnerId,
                               String pawnerNo,
                               String pawnerName,
                               Long collateralId,
                               String itemNo,
                               String itemName,
                               LocalDate dueDate,
                               Integer daysRemaining,
                               Integer overdueDays,
                               Integer renewCount,
                               BigDecimal redeemTotalAmount,
                               LocalDate renewNewDueDate) implements Serializable {
}
