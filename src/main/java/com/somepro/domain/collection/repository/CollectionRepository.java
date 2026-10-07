package com.somepro.domain.collection.repository;

import com.somepro.domain.collection.model.CollectionItem;
import com.somepro.domain.collection.model.CollectionQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 催收工作台只读查询端口（领域层定义，基础设施层实现）。
 *
 * 只查不写：实现里不许有任何 insert/update/delete —— 清单上的应还总额、续当新到期日
 * 全是当面试算，翻清单不落库、不动当票与当物状态。
 */
public interface CollectionRepository {

    /**
     * 翻催收工作台：只认在当（ACTIVE、未删除）的票，按到期日对业务日落两档
     * （快到期：今天起七天内含两端；已逾期：到期日早于今天），其余状态与窗口外的票一概不进。
     * 稳定按到期日从近到远、同一天按票号升序分页；库里一条都没有时返回空页，不报错。
     */
    Mono<PageResult<CollectionItem>> pageWorkbench(int pageNum, int pageSize, CollectionQuery query);
}
