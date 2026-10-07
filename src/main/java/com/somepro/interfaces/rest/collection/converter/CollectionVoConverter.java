package com.somepro.interfaces.rest.collection.converter;

import com.somepro.domain.collection.model.CollectionItem;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.collection.vo.CollectionItemVO;
import com.somepro.interfaces.rest.common.vo.PageVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 催收清单领域对象 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 */
public final class CollectionVoConverter {

    private CollectionVoConverter() {
    }

    public static CollectionItemVO toVo(CollectionItem domain) {
        return new CollectionItemVO(
                domain.bucket().code(),
                domain.bucket().label(),
                domain.ticketId(),
                domain.ticketNo(),
                domain.pawnerId(),
                domain.pawnerNo(),
                domain.pawnerName(),
                domain.collateralId(),
                domain.itemNo(),
                domain.itemName(),
                domain.dueDate(),
                domain.daysRemaining(),
                domain.overdueDays(),
                domain.renewCount(),
                domain.redeemTotalAmount(),
                domain.renewNewDueDate());
    }

    public static PageVO<CollectionItemVO> toPageVo(PageResult<CollectionItem> page) {
        List<CollectionItemVO> content = page.content().stream()
                .map(CollectionVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
