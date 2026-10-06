package com.somepro.domain.reminder.model;

import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.RedeemQuote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 待赎提醒清单的一行（不可变读模型）：柜台催收头一道用的那张电话清单。
 *
 * 每一行带出：当票号、当户、当物、到期日期、离到期还剩几天；
 * 并按票面上抄下来的月利率 / 月综合费率快照，用今天这个日子算「假设今天就来赎」的应付金额
 * （{@link #feeAmount} 利息与综合费 + {@link #totalAmount} 本金加费用）—— 只认票上快照，
 * 不读现在挂在配置里的数，老票报出去的价钱不会跟着新配置乱跳。
 *
 * 天数字段口径（today = 服务端行里时区的当天）：
 * - {@link #daysRemaining} 到期日 − 今天：快到期档为 0~7（0 即今天到期）；逾期档为负数；
 * - {@link #overdueDays} 仅逾期档有值，拖了多少天（正数）= -daysRemaining，好按拖欠轻重排先后；
 *   快到期档为 null。
 *
 * 排序与分档在取数侧（SQL）按到期日落实，本模型只把已落在窗口内的票组装成行。
 */
public record RedemptionReminder(Long ticketId,
                                 String ticketNo,
                                 Long pawnerId,
                                 String pawnerName,
                                 Long collateralId,
                                 String itemName,
                                 LocalDate dueDate,
                                 ReminderBucket bucket,
                                 int daysRemaining,
                                 Integer overdueDays,
                                 BigDecimal feeAmount,
                                 BigDecimal totalAmount) {

    /**
     * 组装一行提醒。
     *
     * @param ticket     从库里读出的在当当票（当金、起当日期、利率费率快照以它为准）
     * @param pawnerName 当户姓名（t_pawner.name 联表带出）
     * @param itemName   当物名称（t_collateral.item_name 联表带出）
     * @param today      清单基准日（行里时区当天，由应用层补，不接受前端指定）
     */
    public static RedemptionReminder of(PawnTicket ticket, String pawnerName,
                                        String itemName, LocalDate today) {
        ReminderBucket bucket = ReminderBucket.of(ticket.getDueDate(), today);
        int daysRemaining = (int) ChronoUnit.DAYS.between(today, ticket.getDueDate());
        Integer overdueDays = bucket == ReminderBucket.OVERDUE ? -daysRemaining : null;

        // 假设今天就来赎应付的本金加费用：照票面快照算，不读当前费率配置
        RedeemQuote quote = ticket.quoteRedeem(today);

        return new RedemptionReminder(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getPawnerId(),
                pawnerName,
                ticket.getCollateralId(),
                itemName,
                ticket.getDueDate(),
                bucket,
                daysRemaining,
                overdueDays,
                quote.feeAmount(),
                quote.totalAmount());
    }
}
