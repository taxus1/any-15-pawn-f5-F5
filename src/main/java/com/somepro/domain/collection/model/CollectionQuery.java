package com.somepro.domain.collection.model;

import java.time.LocalDate;

/**
 * 催收清单翻查条件（不可变值对象）。
 *
 * @param today  业务日（行里时区今天）：快到期 = today～today+7（含今天、含第七天），
 *               已逾期 = dueDate 早于 today
 * @param bucket 档位；null 表示两档一起翻，否则只翻指定一档
 */
public record CollectionQuery(LocalDate today, CollectionBucket bucket) {

    public static CollectionQuery of(LocalDate today, CollectionBucket bucket) {
        return new CollectionQuery(today, bucket);
    }
}
