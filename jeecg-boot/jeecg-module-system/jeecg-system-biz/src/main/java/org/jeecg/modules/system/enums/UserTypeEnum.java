package org.jeecg.modules.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;

/**
 * 用户类型
 */
@ToString
@AllArgsConstructor
@Getter
public enum UserTypeEnum {

    /**
     *
     */
    后台用户(0),

    /**
     * 供应链客户
     */
    客户(1),

    /**
     * 供应链供应商
     */
    供应商(2),

    /**
     * C端用户
     */
    C端用户(3),

    /**
     * 平台商家
     */
    商家(4);

    private final Integer value;

    private final static HashMap<Integer,UserTypeEnum> NAME_MAP = new HashMap<>();

    static {
        for (UserTypeEnum e : UserTypeEnum.values()) {
           NAME_MAP.put(e.value,e);
        }
    }

    public static String getNameByValue(Integer value){

        if (value == null) {
            return "";
        }

        if (NAME_MAP.containsKey(value)) {
            return NAME_MAP.get(value).name();
        } else {
            return "";
        }

    }

}
