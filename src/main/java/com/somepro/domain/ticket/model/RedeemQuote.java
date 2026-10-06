package com.somepro.domain.ticket.model;

import java.math.BigDecimal;

/**
 * 赎当报价（不可变值对象）：假设在某个日子来赎，照票面上的快照当场算出的应付金额。
 *
 * 清单与赎当结算共用同一套口径，才不会出现「清单上报一个价、真来赎又是一个价」：
 * - usedDays   计费天数 = 结算日 − 起当日期的自然日数，不足一天按一天算（至少 1 天）；
 * - feeAmount  利息与综合费合计 = 当金 ×（月利率快照 + 月综合费率快照）÷ 30 × 计费天数，两位小数四舍五入；
 * - totalAmount 应还总额 = 当金 + 费用，两位小数。
 *
 * 利率费率一律用票面上开票当下抄下来的快照，不读现在挂在配置表里的数 ——
 * 不然老票报出去的价钱会跟着新配置乱跳。
 */
public record RedeemQuote(int usedDays, BigDecimal feeAmount, BigDecimal totalAmount) {
}
