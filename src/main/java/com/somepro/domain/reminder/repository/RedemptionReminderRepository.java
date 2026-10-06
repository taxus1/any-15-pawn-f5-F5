package com.somepro.domain.reminder.repository;

import com.somepro.domain.reminder.model.RedemptionReminder;
import com.somepro.domain.reminder.model.ReminderBucket;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 待赎提醒清单的仓储端口（领域层定义，基础设施层实现）。
 *
 * 取数口径在基础设施层用 SQL 落实：
 * - 只认还在当（ACTIVE）的票，已赎 / 已绝当 / 已撤销 / 已删除一概不进；
 * - bucket 为 null 时两档一起翻（到期日 ≤ 窗口上界）；指定档位只翻那档，不重复出现；
 * - 按到期日从近到远排（逾期越久越靠前），到期日相同再按当票 id 升序兜底，分页可重复对号；
 * - 当户、当物名称联表带出，已删除（del_flag=1）的不出现。
 *
 * 取数与分页在同一条查询里由 PageHelper 串起来；天数、拖欠天数与应付金额在领域侧按基准日算。
 */
public interface RedemptionReminderRepository {

    /**
     * 翻待赎提醒清单。
     *
     * @param pageNum 页码，从 1 起
     * @param pageSize 每页条数
     * @param bucket  档位：null 两档都翻，EXPIRING 只翻快到期，OVERDUE 只翻已逾期
     * @param today   清单基准日（行里时区当天）；快到期窗口上界 = today + 7
     */
    Mono<PageResult<RedemptionReminder>> page(int pageNum, int pageSize,
                                              ReminderBucket bucket, LocalDate today);
}
