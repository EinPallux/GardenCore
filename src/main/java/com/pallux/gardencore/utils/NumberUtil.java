package com.pallux.gardencore.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class NumberUtil {

    private static final DecimalFormat FORMATTED = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat MULTIPLIER = new DecimalFormat("x#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private NumberUtil() {}

    public static String formatRaw(double value) {
        return FORMATTED.format(value);
    }

    public static String formatMultiplier(double value) {
        return MULTIPLIER.format(value);
    }

    public static String formatPercent(double value) {
        return FORMATTED.format(value) + "%";
    }
}
