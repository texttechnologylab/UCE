package org.texttechnologylab.models.corpus;

import org.hibernate.annotations.Type;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "time")
@Typesystem(types = {org.texttechnologylab.annotation.type.Time.class})
public class Time extends UIMAAnnotation implements WikiModel {

    @Column(name = "\"valuee\"", columnDefinition = "TEXT")
    private String value;

    private Integer year;
    private String month;
    private String day;
    private Date date;
    private String season;

    public Time() {
        super(-1, -1);
    }

    public Time(int begin, int end) {
        super(begin, end);
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getWikiId() {
        return "TI" + "-" + this.getId();
    }

    public String getType() {
        return "TIME";
    }
}
