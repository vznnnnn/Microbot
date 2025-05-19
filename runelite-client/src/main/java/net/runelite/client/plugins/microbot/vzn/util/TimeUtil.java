package net.runelite.client.plugins.microbot.vzn.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final ThreadLocal<StringBuilder> MM_SS_BUILDER = ThreadLocal.withInitial(StringBuilder::new);
    private static final SimpleDateFormat dateTimeFullFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm aa");
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    public static String formatIntoMMSS(int secs) {
        int seconds = secs % 60;
        secs -= seconds;

        long minutesCount = secs / 60L;
        long minutes = minutesCount % 60L;
        minutesCount -= minutes;

        long hours = minutesCount / 60L;

        StringBuilder result = MM_SS_BUILDER.get();
        result.setLength(0);

        if (hours > 0L) {
            if (hours < 10L) {
                result.append("0");
            }
            result.append(hours).append(":");
        }

        if (minutes < 10L) {
            result.append("0");
        }
        result.append(minutes).append(":");

        if (seconds < 10) {
            result.append("0");
        }
        result.append(seconds);

        return result.toString();
    }

    public static String formatIntoDetailedString(int secs) {
        if (secs == 0) {
            return "0 seconds";
        }

        int remainder = secs % 86400;
        int days = secs / 86400;
        int hours = remainder / 3600;
        int minutes = remainder / 60 - hours * 60;
        int seconds = remainder % 3600 - minutes * 60;

        String fDays = days > 0 ? " " + days + " day" + (days > 1 ? "s" : "") : "";
        String fHours = hours > 0 ? " " + hours + " hour" + (hours > 1 ? "s" : "") : "";
        String fMinutes = minutes > 0 ? " " + minutes + " minute" + (minutes > 1 ? "s" : "") : "";
        String fSeconds = seconds > 0 ? " " + seconds + " second" + (seconds > 1 ? "s" : "") : "";

        return (fDays + fHours + fMinutes + fSeconds).trim();
    }

    public static String formatIntoAbbreviatedString(int secs) {
        if (secs == 0) {
            return "0s";
        }

        int remainder = secs % 86400;
        int days = secs / 86400;
        int hours = remainder / 3600;
        int minutes = remainder / 60 - hours * 60;
        int seconds = remainder % 3600 - minutes * 60;

        String fDays = days > 0 ? " " + days + "d" : "";
        String fHours = hours > 0 ? " " + hours + "h" : "";
        String fMinutes = minutes > 0 ? " " + minutes + "m" : "";
        String fSeconds = seconds > 0 ? " " + seconds + "s" : "";

        return (fDays + fHours + fMinutes + fSeconds).trim();
    }

    public static String formatIntoFullCalendarString(Date date) {
        return dateTimeFullFormat.format(date);
    }

    public static String formatIntoCalendarString(Date date) {
        return dateTimeFormat.format(date);
    }

    public static String formatIntoDateString(Date date) {
        return dateFormat.format(date);
    }

    public static int parseTime(String time) {
        if (time.equals("0") || time.isEmpty()) {
            return 0;
        }

        String[] lifeMatch = {"y", "w", "d", "h", "m", "s"};
        int[] lifeInterval = {31_536_000, 604_800, 86_400, 3_600, 60, 1};

        int seconds = -1;
        for (int i = 0; i < lifeMatch.length; i++) {
            Pattern pattern = Pattern.compile("([0-9]+)" + lifeMatch[i]);
            Matcher matcher = pattern.matcher(time);
            while (matcher.find()) {
                if (seconds == -1) {
                    seconds = 0;
                }
                seconds += Integer.parseInt(matcher.group(1)) * lifeInterval[i];
            }
        }

        if (seconds == -1) {
            throw new IllegalArgumentException("Invalid time provided.");
        }

        return seconds;
    }

}
