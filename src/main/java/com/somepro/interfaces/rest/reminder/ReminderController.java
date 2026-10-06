package com.somepro.interfaces.rest.reminder;

import com.somepro.application.reminder.RedemptionReminderAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.reminder.converter.RedemptionReminderVoConverter;
import com.somepro.interfaces.rest.reminder.vo.RedemptionReminderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 待赎提醒清单用户接口层：柜台催收头一道，把快到期 / 已逾期的在当票拉出来挨个打电话。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link RedemptionReminderAppService}。
 * 入参走 query string，便于柜台端直接调用。
 */
@RestController
@RequestMapping("/api/reminder")
public class ReminderController {

    private final RedemptionReminderAppService reminderAppService;

    public ReminderController(RedemptionReminderAppService reminderAppService) {
        this.reminderAppService = reminderAppService;
    }

    /**
     * 待赎提醒清单：只含在当票，按到期日从近到远排（逾期最久的排最前），支持按档位挑、一页页走。
     * bucket 不传两档一起翻；EXPIRING 快到期（今天起七天内，含今天含第七天）；
     * OVERDUE 已逾期（到期日早于今天，另带 overdueDays）。库里一条都没有时返回空页。
     */
    @GetMapping("/list")
    public Mono<Result<PageVO<RedemptionReminderVO>>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String bucket) {
        return reminderAppService.page(pageNum, pageSize, bucket)
                .map(RedemptionReminderVoConverter::toPageVo)
                .map(Result::ok);
    }
}
