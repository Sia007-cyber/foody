package com.foody.businesses.validation;

import com.foody.common.exception.InvalidRequestException;
import java.util.List;
import java.util.Set;

public final class SupportedIranianCities {
    private static final List<String> VALUES = List.of(
            "تهران", "مشهد", "اصفهان", "شیراز", "تبریز", "کرج", "اهواز", "قم",
            "کرمانشاه", "رشت", "زاهدان", "همدان", "ارومیه", "اردبیل", "بندرعباس",
            "بوشهر", "یزد", "کرمان", "سنندج", "خرم‌آباد", "ساری", "گرگان", "بیرجند",
            "بجنورد", "ایلام", "قزوین", "زنجان", "شهرکرد", "یاسوج", "اراک", "سمنان");
    private static final Set<String> LOOKUP = Set.copyOf(VALUES);

    private SupportedIranianCities() {}

    public static List<String> values() { return VALUES; }

    public static String requireSupported(String value) {
        if (value == null) throw new InvalidRequestException("A supported city is required");
        String normalized = value.trim().replace('ي', 'ی').replace('ك', 'ک').replaceAll("\\s+", " ");
        if (!LOOKUP.contains(normalized)) throw new InvalidRequestException("Unsupported city: " + normalized);
        return normalized;
    }
}
