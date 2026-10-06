package com.somepro.infrastructure.persistence.reminder;

import com.somepro.infrastructure.persistence.reminder.po.ReminderTicketPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 待赎提醒清单只读 Mapper（基础设施层）：在当票联当户、当物取清单行，不做任何写入。
 *
 * 口径：
 * - 只认 t_pawn_ticket.status = 'ACTIVE' 且 del_flag=0 的在当票，
 *   已赎 REDEEMED / 已绝当 FORFEITED / 已撤销 CANCELLED / 已删除一概不进；
 * - 当户、当物 INNER JOIN 且两边 del_flag=0，已注销/已销掉的关联行当没看见；
 * - bucket 传 'EXPIRING' 只取到期日落在 [today, windowEnd]（含两端），
 *   传 'OVERDUE' 只取到期日 < today；传 null 两档并集（due_date <= windowEnd，天然含逾期）；
 * - 排序到期日从近到远、同日按当票 id 升序兜底，逾期越久越靠前，分页可重复对号。
 *
 * 分页由 PageHelper 在适配器里包在这条 SELECT 上；阻塞 JDBC，只能在 blocking(...) 桥接里调用。
 */
@Mapper
public interface ReminderQueryMapper {

    @Select("""
            <script>
            SELECT t.id, t.ticket_no, t.pawner_id, t.collateral_id, t.category,
                   t.pawn_amount, t.appraised_value, t.monthly_rate, t.service_rate,
                   t.start_date, t.due_date, t.term_months, t.status,
                   t.del_flag, t.create_by, t.create_time, t.update_by, t.update_time,
                   p.name AS pawner_name, c.item_name AS item_name
            FROM t_pawn_ticket t
            INNER JOIN t_pawner p ON p.id = t.pawner_id AND p.del_flag = 0
            INNER JOIN t_collateral c ON c.id = t.collateral_id AND c.del_flag = 0
            WHERE t.del_flag = 0 AND t.status = 'ACTIVE'
            <choose>
                <when test="bucket != null and bucket == 'EXPIRING'">
                    AND t.due_date &gt;= #{today} AND t.due_date &lt;= #{windowEnd}
                </when>
                <when test="bucket != null and bucket == 'OVERDUE'">
                    AND t.due_date &lt; #{today}
                </when>
                <otherwise>
                    AND t.due_date &lt;= #{windowEnd}
                </otherwise>
            </choose>
            ORDER BY t.due_date ASC, t.id ASC
            </script>
            """)
    List<ReminderTicketPO> selectReminderRows(@Param("bucket") String bucket,
                                              @Param("today") java.time.LocalDate today,
                                              @Param("windowEnd") java.time.LocalDate windowEnd);
}
