package com.foody.common.validation;

/** Iranian national-ID format and checksum validation. */
public final class IranianNationalId {

    private IranianNationalId() {
    }

    public static boolean isValid(String value) {
        if (value == null || !value.matches("\\d{10}") || value.matches("(\\d)\\1{9}")) {
            return false;
        }

        int sum = 0;
        for (int index = 0; index < 9; index++) {
            sum += (value.charAt(index) - '0') * (10 - index);
        }
        int remainder = sum % 11;
        int checkDigit = value.charAt(9) - '0';
        return remainder < 2 ? checkDigit == remainder : checkDigit == 11 - remainder;
    }
}
