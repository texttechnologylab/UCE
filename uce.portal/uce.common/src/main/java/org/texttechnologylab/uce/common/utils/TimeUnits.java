package org.texttechnologylab.uce.common.utils;

import java.sql.Date;

public class TimeUnits {
    public Integer year;
    public String month;
    public String day;
    public Date fullDate;
    public String season;

    public TimeUnits(Integer year, String month, String day, Date fullDate, String season) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.fullDate = fullDate;
        this.season = season;
    }
}
