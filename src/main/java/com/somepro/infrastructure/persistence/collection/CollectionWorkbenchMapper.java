package com.somepro.infrastructure.persistence.collection;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 催收工作台只读 Mapper（基础设施层）：一条联表查询把在当票、当户、当物与续当回数落齐。
 *
 * 口径（与 doc/schema/pawn.sql 的状态约定一致）：
 * - 主表只认 t_pawn_ticket：del_flag = 0 且 status = 'ACTIVE' ——
 *   已赎 / 已绝当 / 已撤销、打了删除标记的票一概不进；
 * - 当户、当物走 INNER JOIN 且同样要 del_flag = 0：底档被删的行不会冒进清单；
 * - 续当回数是 t_pawn_renew 里该票 del_flag = 0 的条数，打了删除标记的续当不算；
 * - 排序按到期日从近到远、同一天按票号升序，分页可重复对号。
 *
 * 本 Mapper 没有任何写方法；阻塞 JDBC，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface CollectionWorkbenchMapper {

    /** 公共主体：在当票 + 当户 + 当物 + 续当回数；档位窗口由各方法在尾部补。 */
    String SELECT_BODY = "SELECT t.id AS ticketId, t.ticket_no AS ticketNo, t.pawner_id AS pawnerId, "
            + "p.pawner_no AS pawnerNo, p.name AS pawnerName, "
            + "t.collateral_id AS collateralId, c.item_no AS itemNo, c.item_name AS itemName, "
            + "t.start_date AS startDate, t.due_date AS dueDate, t.term_months AS termMonths, "
            + "t.pawn_amount AS pawnAmount, t.monthly_rate AS monthlyRate, t.service_rate AS serviceRate, "
            + "(SELECT COUNT(*) FROM t_pawn_renew r WHERE r.ticket_id = t.id AND r.del_flag = 0) AS renewCount "
            + "FROM t_pawn_ticket t "
            + "INNER JOIN t_pawner p ON p.id = t.pawner_id AND p.del_flag = 0 "
            + "INNER JOIN t_collateral c ON c.id = t.collateral_id AND c.del_flag = 0 "
            + "WHERE t.del_flag = 0 AND t.status = 'ACTIVE' ";

    String ORDER_BY = "ORDER BY t.due_date ASC, t.ticket_no ASC";

    /**
     * 快到期：到期日落在 [today, today+7]，含今天、含第七天。
     */
    @Select(SELECT_BODY
            + "AND t.due_date BETWEEN #{today} AND #{horizonEnd} "
            + ORDER_BY)
    List<CollectionRow> selectDueSoon(@Param("today") LocalDate today,
                                      @Param("horizonEnd") LocalDate horizonEnd);

    /**
     * 已逾期：到期日已经过了今天（due_date &lt; today）。
     */
    @Select(SELECT_BODY
            + "AND t.due_date < #{today} "
            + ORDER_BY)
    List<CollectionRow> selectOverdue(@Param("today") LocalDate today);

    /**
     * 两档合翻：到期日不晚于窗口末日（today+7）。更早的已逾期票也在其中 ——
     * 同一排序下已逾期排最前、快到期随后，档位由领域侧按日期落。
     */
    @Select(SELECT_BODY
            + "AND t.due_date <= #{horizonEnd} "
            + ORDER_BY)
    List<CollectionRow> selectWindow(@Param("today") LocalDate today,
                                     @Param("horizonEnd") LocalDate horizonEnd);
}
