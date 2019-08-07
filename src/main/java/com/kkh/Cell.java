package com.kkh;

public class Cell {

    private String value;

    public Boolean isC() {
        if (value != null) {
            return value.indexOf("C") > -1;
        }
        else {
            return false;
        }
    }

    public Boolean isL() {
        if (value != null) {
            return value.indexOf("L") > -1;
        }
        else {
            return false;
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
