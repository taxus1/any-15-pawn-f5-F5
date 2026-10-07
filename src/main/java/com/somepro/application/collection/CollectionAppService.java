package com.somepro.application.collection;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collection.model.CollectionBucket;
import com.somepro.domain.collection.model.CollectionItem;
import com.somepro.domain.collection.model.CollectionQuery;
import com.somepro.domain.collection.repository.CollectionRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 催收工作台应用服务：只编排「翻催收清单」一个只读用例，不写表映射。
 *
 * 业务日由服务端按行里时区统一补（不接受前端指定，免得有人拿别的日子来试算），
 * 档位可挑（DUE_SOON 快到期 / OVERDUE 已逾期），不挑就两档一起翻，一页页走。
 * 每行的应还总额、续当新到期日是当着客户面的试算，在领域值对象里照赎当 / 续当
 * 现有办理规矩当场算，全程不调任何写仓储。
 */
@Service
public class CollectionAppService {

    /** 业务日期统一按行里所在时区算，避免容器 UTC 下跨天把档位算偏。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    private final CollectionRepository collectionRepository;

    public CollectionAppService(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    /**
     * 翻催收工作台。
     *
     * @param pageNum 页码，从 1 起
     * @param pageSize 每页条数
     * @param bucket 档位 code；空串/null 表示两档一起翻，非法写法在枚举解析阶段挡回
     */
    public Mono<PageResult<CollectionItem>> workbench(int pageNum, int pageSize, String bucket) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        CollectionBucket bucketEnum = CollectionBucket.ofCode(blankToNull(bucket));
        LocalDate today = LocalDate.now(BIZ_ZONE);
        return collectionRepository.pageWorkbench(pageNum, pageSize, CollectionQuery.of(today, bucketEnum));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
