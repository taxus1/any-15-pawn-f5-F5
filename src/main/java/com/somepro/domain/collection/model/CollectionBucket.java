package com.somepro.domain.collection.model;

import com.somepro.common.exception.BizException;

/**
 * 催收工作台档位（纯领域枚举，不依赖任何框架）。
 * 清单只收还在当（ACTIVE）的票，按票上现在挂着的到期日期对今天落档：
 * <ul>
 *   <li>{@link #DUE_SOON} 快到期：到期日落在「今天起往后七天」以内，含今天、含第七天；</li>
 *   <li>{@link #OVERDUE} 已逾期：到期日已经过了今天（到期日早于今天）。</li>
 * </ul>
 * 到期日在七天之后的在当票两档都不进，今天不催。
 */
public enum CollectionBucket {

    DUE_SOON("DUE_SOON", "快到期"),
    OVERDUE("OVERDUE", "已逾期");

    /** 快到期窗口：从今天起往后多少天（含今天、含第 N 天）。 */
    public static final int DUE_SOON_DAYS = 7;

    private final String code;
    private final String label;

    CollectionBucket(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /**
     * 由外部传入值解析枚举：只认上述两个 code（大小写敏感）。
     * 传 null 返回 null（翻清单时表示两档都要）；传空串或其它写法都算非法入参，直接挡回。
     */
    public static CollectionBucket ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (CollectionBucket bucket : values()) {
            if (bucket.code.equals(trimmed)) {
                return bucket;
            }
        }
        throw new BizException("催收档位只支持 DUE_SOON（快到期）/ OVERDUE（已逾期）：" + code);
    }
}
