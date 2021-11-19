package net.minecraft.stats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.SystemUtils;

public interface ICounter {
    DecimalFormat DECIMAL_FORMAT = SystemUtils.make(new DecimalFormat("########0.00"), (decimalFormat) -> {
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    });
    ICounter DEFAULT = NumberFormat.getIntegerInstance(Locale.US)::format;
    ICounter DIVIDE_BY_TEN = (i) -> {
        return DECIMAL_FORMAT.format((double)i * 0.1D);
    };
    ICounter DISTANCE = (i) -> {
        double d = (double)i / 100.0D;
        double e = d / 1000.0D;
        if (e > 0.5D) {
            return DECIMAL_FORMAT.format(e) + " km";
        } else {
            return d > 0.5D ? DECIMAL_FORMAT.format(d) + " m" : i + " cm";
        }
    };
    ICounter TIME = (i) -> {
        double d = (double)i / 20.0D;
        double e = d / 60.0D;
        double f = e / 60.0D;
        double g = f / 24.0D;
        double h = g / 365.0D;
        if (h > 0.5D) {
            return DECIMAL_FORMAT.format(h) + " y";
        } else if (g > 0.5D) {
            return DECIMAL_FORMAT.format(g) + " d";
        } else if (f > 0.5D) {
            return DECIMAL_FORMAT.format(f) + " h";
        } else {
            return e > 0.5D ? DECIMAL_FORMAT.format(e) + " m" : d + " s";
        }
    };

    String format(int i);
}
