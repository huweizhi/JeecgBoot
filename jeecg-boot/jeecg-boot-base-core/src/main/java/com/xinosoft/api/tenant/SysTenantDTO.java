package com.xinosoft.api.tenant;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户信息
 * @author: jeecg-boot
 */
@Data
public class SysTenantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 编码
     */
    private Integer id;
    
    /**
     * 名称
     */
    private String name;

    /**
     * 开始时间
     */
    private Date beginDate;

    /**
     * 结束时间
     */
    private Date endDate;

    /**
     * 状态 1正常 0冻结
     */
    private Integer status;

    /**
     * 二级域名(暂时无用,预留字段)
     */
    private String secondaryDomain;

    /**
     * 数据库名称
     */
    private String dbName;
    
}
