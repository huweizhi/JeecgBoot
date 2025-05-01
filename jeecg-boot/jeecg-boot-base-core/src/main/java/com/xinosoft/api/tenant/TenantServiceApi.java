package com.xinosoft.api.tenant;

import java.util.List;

public interface TenantServiceApi {
    /**
     * 查询有效的租户
     *
     * @return
     */
    List<SysTenantDTO> queryEffectiveTenant();
}
