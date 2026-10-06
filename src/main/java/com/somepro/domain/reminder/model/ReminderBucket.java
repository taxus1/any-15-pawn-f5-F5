package com.somepro.domain.reminder.model;

import com.somepro.common.exception.BizException;

import java.time.LocalDate;

/**
 * 待赎提醒分档（纯领域枚举，不依赖任何框架）。清单只认还在当（ACTIVE）的票，按到期日相对今天的位置分两档：
 * <ul>
 *   <li>{@link #EXPIRING} 快到期：到期日落在 [今天, 今天+7]，含今天、含第七天；</li>
 *   <li>{@link #OVERDUE} 已逾期：到期日已经过了今天（&lt; 今天）。</li>
 * </ul>
 * 今天之前到期的只走逾期档，不会混进快到期档重复出现；到期日恰好压在今天算快到期，别漏。
 */
public enum ReminderBucket {

    EXPIRING("EXPIRING", "快到期"),
    OVERDUE("OVERDUE", "已逾期");

    /** 快到期窗口天数：从今天起算往后数七天（含今天、含第七天）。 */
    public static final int EXPIRING_WINDOW_DAYS = 7;

    private final String code;
    private final String label;

    ReminderBucket(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** 按到期日与今天的相对位置归桶：今天及未来七天内快到期，早于今天已逾期。 */
    public static ReminderBucket of(LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) {
            return OVERDUE;
        }
        if (!dueDate.isAfter(today.plusDays(EXPIRING_WINDOW_DAYS))) {
            return EXPIRING;
        }
        throw new BizException("到期日超出待赎提醒窗口，不应进入清单");
    }

    /**
     * 由外部传入值解析枚举：只认 EXPIRING / OVERDUE（大小写敏感）。
     * 传 null 返回 null（翻清单时表示不按档位筛、两档一起翻）；传空串或其它写法都算非法入参，挡回。
     */
    public static ReminderBucket ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (ReminderBucket bucket : values()) {
            if (bucket.code.equals(trimmed)) {
                return bucket;
            }
        }
        throw new BizException("档位只支持 EXPIRING 快到期 / OVERDUE 已逾期：" + code);
    }
}
