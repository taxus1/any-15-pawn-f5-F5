package com.somepro.infrastructure.persistence.reminder;

import com.github.pagehelper.PageHelper;
import com.somepro.domain.reminder.model.RedemptionReminder;
import com.somepro.domain.reminder.model.ReminderBucket;
import com.somepro.domain.reminder.repository.RedemptionReminderRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.reminder.po.ReminderTicketPO;
import com.somepro.infrastructure.persistence.ticket.converter.PawnTicketPoConverter;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 待赎提醒清单仓储适配器（基础设施层）：MyBatis-Plus 阻塞 JDBC 经 blocking(...) 桥接进响应式链路。
 *
 * SQL 侧只负责把「在当 + 落在档位窗口 + 联出当户当物名称」的行按到期日排好、用 PageHelper 分页；
 * 档位判定、离到期天数、拖欠天数与「假设今天来赎」的应付金额都在领域侧（{@link RedemptionReminder#of}）
 * 用票面上的快照算 —— 取数和分页在同一条查询里串起来，金额不依赖当前费率配置。
 */
@Repository
public class RedemptionReminderRepositoryImpl implements RedemptionReminderRepository {

    private final ReminderQueryMapper reminderQueryMapper;

    public RedemptionReminderRepositoryImpl(ReminderQueryMapper reminderQueryMapper) {
        this.reminderQueryMapper = reminderQueryMapper;
    }

    @Override
    public Mono<PageResult<RedemptionReminder>> page(int pageNum, int pageSize,
                                                     ReminderBucket bucket, LocalDate today) {
        String bucketCode = bucket == null ? null : bucket.code();
        LocalDate windowEnd = today.plusDays(ReminderBucket.EXPIRING_WINDOW_DAYS);
        return this.<PageResult<RedemptionReminder>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ReminderTicketPO> rows =
                        reminderQueryMapper.selectReminderRows(bucketCode, today, windowEnd);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<RedemptionReminder> content = rows.stream()
                        .map(row -> {
                            PawnTicket ticket = PawnTicketPoConverter.toDomain(row);
                            return RedemptionReminder.of(ticket, row.getPawnerName(),
                                    row.getItemName(), today);
                        })
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清，避免污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic
     * （与各仓储适配器同一套约定，顺序不能颠倒）。清单只读不写，取操作人仅为约定统一。
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
