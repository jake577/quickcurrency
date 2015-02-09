package com.jesttek.quickcurrency;

public class CheckboxListItem {

    public boolean Activated = false;
    public String Text = "";
    public int ID = -1;

    public CheckboxListItem(String text, boolean activated, int id) {
        Activated = activated;
        Text = text;
        ID = id;
    }
}
