package com.defano.hypertalk.util;

import com.defano.hypertalk.ast.model.enums.ConvertibleDateFormat;
import com.defano.hypertalk.ast.model.Convertible;
import com.defano.hypertalk.ast.model.enums.LengthAdjective;
import com.defano.hypertalk.ast.model.Value;

import java.text.*;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    private DateUtils() {}

    public static Value valueOf(Date d, LengthAdjective format) {
        switch (format) {
            case LONG:
                return new Value(ConvertibleDateFormat.LONG_DATE.dateFormat.format(d));
            case SHORT:
                return new Value(ConvertibleDateFormat.SHORT_DATE.dateFormat.format(d));
            case ABBREVIATED:
                return new Value(ConvertibleDateFormat.ABBREV_DATE.dateFormat.format(d));

            default:
                throw new IllegalArgumentException("Bug! Unimplemented date format.");
        }
    }

    public static Value valueOf(Date d, Convertible format) {
        DateFormat firstFormat = format.first.dateFormat;
        DateFormat secondFormat = format.second == null ? null : format.second.dateFormat;

        if (secondFormat != null) {
            return new Value(firstFormat.format(d) + " " + secondFormat.format(d));
        } else {
            return new Value(firstFormat.format(d));
        }
    }

    public static Date dateOf(Value value, Convertible format) {
        if (format == null) {
            return dateOf(value);
        }

        ParsePosition position = new ParsePosition(0);

        Date firstDate = dateOf(value.toString(), format.first, position);
        if (firstDate == null) {
            return null;
        }

        if (format.second != null) {
            Date secondDate = dateOf(value.toString(), format.second, position);
            return mergeDates(firstDate, secondDate, format.second);
        } else {
            return firstDate;
        }
    }

    private static Date dateOf(Value value, ConvertibleDateFormat format) {
        return dateOf(value.toString(), format, new ParsePosition(0));
    }

    private static Date dateOf(String text, ConvertibleDateFormat format, ParsePosition parsePosition) {
        try {
            switch (format) {
                case SECONDS:
                    return format.dateFormat.parse(text, parsePosition);
                case DATE_ITEMS:
                    return format.dateFormat.parse(text);
                case LONG_DATE:
                    return mergeDates(new Date(), format.dateFormat.parse(text, parsePosition), ConvertibleDateFormat.LONG_DATE);
                case SHORT_DATE:
                    return mergeDates(new Date(), format.dateFormat.parse(text, parsePosition), ConvertibleDateFormat.SHORT_DATE);
                case ABBREV_DATE:
                    return mergeDates(new Date(), format.dateFormat.parse(text, parsePosition), ConvertibleDateFormat.ABBREV_DATE);
                case LONG_TIME:
                    return mergeDates(new Date(), format.dateFormat.parse(text, parsePosition), ConvertibleDateFormat.LONG_TIME);
                case SHORT_TIME:
                    return mergeDates(new Date(), format.dateFormat.parse(text, parsePosition), ConvertibleDateFormat.SHORT_TIME);
            }
        } catch (ParseException e) {
            return null;
        }

        return null;
    }

    public static Date dateOf(Value value) {
        return parseDate(value, ConvertibleDateFormat.values());
    }

    private static Date parseDate(Value value, ConvertibleDateFormat[] formats) {
        for (ConvertibleDateFormat format : formats) {
            Date date = tryParseDate(value, format);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private static Date tryParseDate(Value value, ConvertibleDateFormat format) {
        return dateOf(value, format);
    }


    /**
     * Merges two dates together. Returns a date equal to first but with certain date fields overwritten with values
     * from second based on the given conversion format.
     *
     * @param first
     * @param second
     * @param secondFormat
     * @return
     */
    @SuppressWarnings("deprecation")
    private static Date mergeDates(Date first, Date second, ConvertibleDateFormat secondFormat) {
        if (first == null || second == null) {
            return null;
        }

        if (secondFormat == null) {
            return first;
        }

        Date updated = new Date(first.getTime());
        Calendar cal = Calendar.getInstance();

        switch (secondFormat) {
            case DATE_ITEMS:
            case SECONDS:
                return second;
            case LONG_DATE:
            case SHORT_DATE:
            case ABBREV_DATE:
                updated.setYear(second.getYear());
                updated.setMonth(second.getMonth());
                updated.setDate(second.getDate());
                cal.setTime(updated);
                cal.set(Calendar.HOUR_OF_DAY, second.getHours());
                cal.set(Calendar.MINUTE, second.getMinutes());
                cal.set(Calendar.SECOND, second.getSeconds());
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTime();
            case LONG_TIME:
            case SHORT_TIME:
                cal.setTime(updated);
                cal.set(Calendar.HOUR_OF_DAY, second.getHours());
                cal.set(Calendar.MINUTE, second.getMinutes());
                cal.set(Calendar.SECOND, second.getSeconds());
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTime();
        }

        throw new IllegalStateException("Bug! Unimplemented conversion format: " + secondFormat);
    }

}
