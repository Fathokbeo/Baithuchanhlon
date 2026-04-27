package main.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class MoneyUtils {
    private static final DecimalFormat DISPLAY = new DecimalFormat("#,##0.00");

    private MoneyUtils() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static String display(BigDecimal amount) {
        return DISPLAY.format(normalize(amount));
    }
}
