package com.somepro.infrastructure.persistence.collection;

import com.github.pagehelper.PageHelper;
import com.somepro.domain.collection.model.CollectionBucket;
import com.somepro.domain.collection.model.CollectionItem;
import com.somepro.domain.collection.model.CollectionQuery;
import com.somepro.domain.collection.repository.CollectionRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.TicketStatus;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 催收工作台仓储适配器（基础设施层）：只读联表查询经 blocking(...) 桥接进响应式链路。
 *
 * 这是全模块唯一碰库的地方，且只 SELECT：试算需要的票面快照（起当/到期日期、当期月数、
 * 当金、月利率与月综合费率）原样读出后在领域侧重放 {@code PawnRedeem.apply} /
 * {@code PawnRenew.apply}，不 insert / update 任何表，票与当物状态在翻清单期间原封不动。
 */
@Repository
public class CollectionRepositoryImpl implements CollectionRepository {

    private final CollectionWorkbenchMapper workbenchMapper;

    public CollectionRepositoryImpl(CollectionWorkbenchMapper workbenchMapper) {
        this.workbenchMapper = workbenchMapper;
    }

    @Override
    public Mono<PageResult<CollectionItem>> pageWorkbench(int pageNum, int pageSize, CollectionQuery query) {
        return this.<PageResult<CollectionItem>>blocking(() -> {
            LocalDate today = query.today();
            LocalDate horizonEnd = today.plusDays(CollectionBucket.DUE_SOON_DAYS);
            CollectionBucket bucket = query.bucket();
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<CollectionRow> rows;
                if (bucket == CollectionBucket.DUE_SOON) {
                    rows = workbenchMapper.selectDueSoon(today, horizonEnd);
                } else if (bucket == CollectionBucket.OVERDUE) {
                    rows = workbenchMapper.selectOverdue(today);
                } else {
                    rows = workbenchMapper.selectWindow(today, horizonEnd);
                }
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<CollectionItem> content = rows.stream()
                        .map(row -> toItem(row, today))
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清，避免污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    /**
     * 把联表投影重放回领域：票面字段重建成在当票聚合（试算只认它的快照与日期），
     * 档位划分与两笔试算都在 {@link CollectionItem#of} 内，与赎当 / 续当办理同一套规矩。
     */
    private static CollectionItem toItem(CollectionRow row, LocalDate today) {
        PawnTicket ticket = new PawnTicket();
        ticket.setId(row.getTicketId());
        ticket.setTicketNo(row.getTicketNo());
        ticket.setPawnerId(row.getPawnerId());
        ticket.setCollateralId(row.getCollateralId());
        ticket.setPawnAmount(row.getPawnAmount());
        ticket.setMonthlyRate(row.getMonthlyRate());
        ticket.setServiceRate(row.getServiceRate());
        ticket.setStartDate(row.getStartDate());
        ticket.setDueDate(row.getDueDate());
        ticket.setTermMonths(row.getTermMonths());
        // SQL 已限定只查 ACTIVE：重放的票就是在当票，两笔试算的状态门禁自然过得去
        ticket.setStatus(TicketStatus.ACTIVE);

        int renewCount = row.getRenewCount() == null ? 0 : row.getRenewCount().intValue();
        return CollectionItem.of(ticket, row.getPawnerNo(), row.getPawnerName(),
                row.getItemNo(), row.getItemName(), renewCount, today);
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic
     * （查询不写审计，取操作人仅为与各业务模块同一套约定，顺序不能颠倒）。
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
