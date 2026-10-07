package com.somepro.infrastructure.persistence.collection;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 催收工作台联表查询的一行投影（基础设施层只读对象，不映射任何表、不被写入）。
 * 列别名即字段名（查询里全部显式起了驼峰别名）。
 */
@Getter
@Setter
public class CollectionRow {

    private Long ticketId;
    private String ticketNo;
    private Long pawnerId;
    private String pawnerNo;
    private String pawnerName;
    private Long collateralId;
    private String itemNo;
    private String itemName;

    /** 以下票面字段用于在领域侧重放赎当 / 续当试算，不外放。 */
    private LocalDate startDate;
    private LocalDate dueDate;
    private Integer termMonths;
    private BigDecimal pawnAmount;
    private BigDecimal monthlyRate;
    private BigDecimal serviceRate;

    /** 该票已办续当回数（未删除的 t_pawn_renew 条数）。 */
    private Long renewCount;
}
