package com.xinosoft.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 推荐人类型
 */
@ToString
@AllArgsConstructor
@Getter
public enum UserTypeEnum {

    /**
     *
     */
    后台用户(0),
    商户(2),
    客户(3);

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

    public static List<DictOption> listDictOption() {
        List<DictOption> dictOptionList = new ArrayList<>();
        NAME_MAP.forEach((key, value1) -> dictOptionList.add(new DictOption(key, value1.name())));
        return dictOptionList.stream().sorted(Comparator.comparingInt(DictOption::getValue)).collect(Collectors.toList());
    }


    private static class DictOption {

        DictOption(Integer value, String label) {
            this.value = value;
            this.label = label;
        }

        private Integer value;

        private String label;

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

    }

}
