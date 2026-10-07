package com.somepro.interfaces.rest.collection;

import com.somepro.application.collection.CollectionAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.collection.converter.CollectionVoConverter;
import com.somepro.interfaces.rest.collection.vo.CollectionItemVO;
import com.somepro.interfaces.rest.common.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 催收工作台用户接口层：一屏把快到期、已逾期两拨还在当的票摆清楚。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link CollectionAppService}。
 * 清单上的应还总额与续当新到期日都是只读试算：照现有赎当 / 续当办理规矩当场算，
 * 不入库、不动票和当物状态。
 */
@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    private final CollectionAppService collectionAppService;

    public CollectionController(CollectionAppService collectionAppService) {
        this.collectionAppService = collectionAppService;
    }

    /**
     * 催收工作台清单：只认在当的票，按到期日分两档 ——
     * bucket 不传：两档一起翻（已逾期在前、快到期随后）；
     * bucket=DUE_SOON：只翻快到期（今天起七天内，含今天、含第七天）；
     * bucket=OVERDUE：只翻已逾期（到期日已过今天）。
     * 按到期日从近到远、同一天按票号排序，一页页走；库里没有票时返回空页。
     */
    @GetMapping("/workbench")
    public Mono<Result<PageVO<CollectionItemVO>>> workbench(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String bucket) {
        return collectionAppService.workbench(pageNum, pageSize, bucket)
                .map(CollectionVoConverter::toPageVo)
                .map(Result::ok);
    }
}
