package ru.mirea.project.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormatter {
    private MoneyFormatter() {
    }

    public static String format(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setParseBigDecimal(true);
        return format.format(value);
    }
}