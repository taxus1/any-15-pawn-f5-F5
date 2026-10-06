package com.somepro.application.reminder;

import com.somepro.common.exception.BizException;
import com.somepro.domain.reminder.model.RedemptionReminder;
import com.somepro.domain.reminder.model.ReminderBucket;
import com.somepro.domain.reminder.repository.RedemptionReminderRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 待赎提醒清单应用服务：编排柜台催收用的那张待赎提醒清单，不写表映射。
 *
 * 基准日统一取服务端行里时区的当天，不接受前端指定 —— 同一张票在同一天谁翻都落在同一档、
 * 报同一个价。出入参用领域对象/基础类型，不认识 PO 与 VO。
 */
@Service
public class RedemptionReminderAppService {

    /** 业务时刻统一按行里所在时区算，避免容器 UTC 下把到期档位算偏一天。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    private final RedemptionReminderRepository reminderRepository;

    public RedemptionReminderAppService(RedemptionReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    /**
     * 翻待赎提醒清单：只含还在当的票，按到期日从近到远排（逾期越久越靠前）。
     *
     * @param pageNum 页码，从 1 起
     * @param pageSize 每页条数
     * @param bucket  档位筛选：null/空 两档一起翻；EXPIRING 只翻快到期（今天起七天内，含今天含第七天）；
     *                OVERDUE 只翻已逾期（到期日早于今天）
     */
    public Mono<PageResult<RedemptionReminder>> page(int pageNum, int pageSize, String bucket) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        ReminderBucket bucketFilter = ReminderBucket.ofCode(blankToNull(bucket));
        LocalDate today = LocalDate.now(BIZ_ZONE);
        return reminderRepository.page(pageNum, pageSize, bucketFilter, today);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
