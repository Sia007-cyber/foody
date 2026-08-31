package com.foody.businesses.entity;

/**
 * Known business types. This is the type-safe Java representation of the
 * {@code business_types} reference table. To add a new type later, add a constant
 * here AND insert a matching row into business_types — no ALTER TABLE on businesses.
 */
public enum BusinessTypeCode {
    CAFE("CAFE"),
    FAST_FOOD("FAST_FOOD");

    private final String code;

    BusinessTypeCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static BusinessTypeCode fromCode(String code) {
        for (BusinessTypeCode t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown business type: " + code);
    }
}
