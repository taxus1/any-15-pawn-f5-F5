package com.somepro.domain.collection.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.redeem.model.PawnRedeem;
import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.ticket.model.PawnTicket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 催收工作台一行（不可变领域值对象）：一张还在当、且进入催收到期窗口的当票，
 * 外加柜台接电话最常被问的两句答案的「当面试算」。
 *
 * 两个试算口径与真正去办一次分毫不差 —— 直接调赎当 / 续当聚合的工厂：
 * 1. 今天来赎要还多少（{@link #redeemTotalAmount}）= {@link PawnRedeem#apply} 当场算出的应还总额，
 *    计费天数、日费率（票面月利率+月综合费率快照 ÷30）、四舍五入都走那一处，这里不另立算法；
 * 2. 今天来续之后到哪天（{@link #renewNewDueDate}）= {@link PawnRenew#apply} 当场推出的新到期日，
 *    顺延月数照票面原当期走。
 *
 * 试算只读不写：两个工厂只 new 聚合、不落库，本工作台也绝不调用任何写仓储，
 * 翻多少遍清单都不动票、不动当物、不生成赎当/续当单。
 * 过了到期日的票续当聚合本身会挡，因此已逾期一档的续当新到期日为 null（空着不展示）。
 *
 * @param bucket            落档：快到期 / 已逾期
 * @param ticketId          当票 id
 * @param ticketNo          当票号
 * @param pawnerId          当户 id
 * @param pawnerNo          当户编号
 * @param pawnerName        当户姓名
 * @param collateralId      当物 id
 * @param itemNo            当物编号
 * @param itemName          当物名称
 * @param dueDate           票上现在挂着的到期日期
 * @param daysRemaining     离到期还剩几天 = 到期日 − 今天（今天到期为 0；已逾期为负数）
 * @param overdueDays       已拖了多少天 = 今天 − 到期日；未逾期为 null
 * @param renewCount        这张票办过几回续当（t_pawn_renew 未删除记录数）
 * @param redeemTotalAmount 今天来赎要还的总额（元，两位小数）
 * @param renewNewDueDate   今天来办续当之后的新到期日；已逾期今天续不了，为 null
 */
public record CollectionItem(CollectionBucket bucket,
                             Long ticketId,
                             String ticketNo,
                             Long pawnerId,
                             String pawnerNo,
                             String pawnerName,
                             Long collateralId,
                             String itemNo,
                             String itemName,
                             LocalDate dueDate,
                             int daysRemaining,
                             Integer overdueDays,
                             int renewCount,
                             BigDecimal redeemTotalAmount,
                             LocalDate renewNewDueDate) {

    /**
     * 装配一行催收清单并完成两笔当面试算。
     *
     * @param ticket     库里读出的最新票面（状态、起当日期、到期日期、当期月数、当金、利率费率快照以它为准）
     * @param pawnerNo   当户编号
     * @param pawnerName 当户姓名
     * @param itemNo     当物编号
     * @param itemName   当物名称
     * @param renewCount 该票已办续当回数
     * @param today      清单业务日（行里时区的今天，由应用层统一补，不接受前端指定）
     */
    public static CollectionItem of(PawnTicket ticket, String pawnerNo, String pawnerName,
                                    String itemNo, String itemName, int renewCount, LocalDate today) {
        if (ticket == null || ticket.getId() == null) {
            throw new BizException("催收清单行缺少当票");
        }
        LocalDate dueDate = ticket.getDueDate();
        if (dueDate == null) {
            throw new BizException("当票到期日期缺失，无法进入催收清单：" + ticket.getTicketNo());
        }
        if (today == null) {
            throw new BizException("业务日期缺失，无法生成催收清单");
        }

        LocalDate horizonEnd = today.plusDays(CollectionBucket.DUE_SOON_DAYS);
        CollectionBucket bucket;
        if (!dueDate.isBefore(today)) {
            // 到期日 >= 今天：快到期窗口含今天、含第七天；窗口外的在当票不应被查进来
            if (dueDate.isAfter(horizonEnd)) {
                throw new BizException("到期日尚在七天之外，不进催收清单：" + ticket.getTicketNo());
            }
            bucket = CollectionBucket.DUE_SOON;
        } else {
            bucket = CollectionBucket.OVERDUE;
        }

        long remaining = ChronoUnit.DAYS.between(today, dueDate);
        Integer overdueDays = bucket == CollectionBucket.OVERDUE
                ? (int) ChronoUnit.DAYS.between(dueDate, today)
                : null;

        // 两笔试算都只用「今天」这一天，时分秒不影响按自然日算的结果；用今天零点与办理口径一致。
        LocalDateTime trialAt = today.atStartOfDay();

        // 「我现在来赎要还多少」：晚于到期日来赎也照收、按实际天数算不加罚，规则全在赎当聚合里。
        BigDecimal redeemTotal = PawnRedeem.apply(ticket, trialAt).getTotalAmount();

        // 「我再续一段到哪天」：到期日当天还办得了；过了到期日续当聚合会挡，
        // 已逾期这一栏按行里规矩空着，不试算、不臆造日期。
        LocalDate renewNewDueDate = null;
        if (!today.isAfter(dueDate)) {
            PawnRenew renew = PawnRenew.apply(ticket, trialAt);
            renewNewDueDate = renew.getNewDueDate();
        }

        return new CollectionItem(
                bucket,
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getPawnerId(),
                pawnerNo,
                pawnerName,
                ticket.getCollateralId(),
                itemNo,
                itemName,
                dueDate,
                (int) remaining,
                overdueDays,
                renewCount,
                redeemTotal,
                renewNewDueDate);
    }
}
