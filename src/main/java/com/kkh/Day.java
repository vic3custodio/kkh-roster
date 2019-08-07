package com.kkh;

public class Day {

    private Integer day = 0;
    private Integer totalNumberOfDoctorOnC = 0;
    private Integer numberOfBlueDoctorOnC = 0;

    public Day(Integer day) {
        this.day = day;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getTotalNumberOfDoctorOnC() {
        return totalNumberOfDoctorOnC;
    }

    public void addTotalNumberOfDoctorOnC() {
        this.totalNumberOfDoctorOnC++;
    }

    public Integer getNumberOfBlueDoctorOnC() {
        return numberOfBlueDoctorOnC;
    }

    public void addNumberOfBlueDoctorOnC() {
        this.numberOfBlueDoctorOnC++;
    }
}
