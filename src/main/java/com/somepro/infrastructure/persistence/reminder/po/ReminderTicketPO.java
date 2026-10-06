package com.somepro.infrastructure.persistence.reminder.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.somepro.infrastructure.persistence.ticket.po.PawnTicketPO;
import lombok.Getter;
import lombok.Setter;

/**
 * 待赎提醒清单查询行（PO，基础设施层）：以当票列为主体，加两列联表带出的名称。
 *
 * 继承 {@link PawnTicketPO} 复用当票全部列映射；当户姓名 / 当物名称不是 t_pawn_ticket 的列，
 * 标 exist=false 让 MyBatis-Plus 的常规 CRUD 忽略它们（本类只走手写联表 SELECT，不参与写入）。
 * 建表脚本维持现状，本类不做任何建表/改表动作。
 */
@Getter
@Setter
public class ReminderTicketPO extends PawnTicketPO {

    /** 当户姓名（t_pawner.name），联表带出。 */
    @TableField(exist = false)
    private String pawnerName;

    /** 当物名称（t_collateral.item_name），联表带出。 */
    @TableField(exist = false)
    private String itemName;
}
