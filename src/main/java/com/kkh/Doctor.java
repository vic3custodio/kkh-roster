package com.kkh;

import java.util.ArrayList;
import java.util.List;

public class Doctor {

    private String name;
    private String type; // resident or junior
    private String color; // green, blue or white
    private List<Cell> cells = new ArrayList<>();

    private Integer currentTotalNumberOfCs = 0;
    private Integer currentTotalNumberOfLs = 0;

    public Doctor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    public Integer getCurrentTotalNumberOfCs() {
        return currentTotalNumberOfCs;
    }

    public void resetCurrentTotalNumberOfCs() {
        this.currentTotalNumberOfCs = 0;
    }

    public void addCurrentTotalNumberOfCs() {
        this.currentTotalNumberOfCs++;
    }

    public Integer getCurrentTotalNumberOfLs() {
        return currentTotalNumberOfLs;
    }

    public void resetCurrentTotalNumberOfLs() {
        this.currentTotalNumberOfLs = 0;
    }

    public void addCurrentTotalNumberOfLs() {
        this.currentTotalNumberOfLs++;
    }
}
