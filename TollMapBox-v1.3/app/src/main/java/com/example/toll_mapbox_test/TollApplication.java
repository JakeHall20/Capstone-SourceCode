package com.example.toll_mapbox_test;

import android.app.Application;

public class TollApplication extends Application {

    public StringBuilder tempStr = new StringBuilder("");
    public String formStr = "Toll ";
    public String whatTolls = "";
    public Double total = 0.0;

    public boolean isFinished;

    public int[] tollArray = new int[] {0,0,0,0};
    {
        tollArray[0] = R.bool.t1;
        tollArray[1] = R.bool.t2;
        tollArray[2] = R.bool.t3;
        tollArray[3] = R.bool.t4;
    }

    String d = "$";
    public int numTolls = 4;

    public String[] toll_cost = new String[] {"$1.00","$1.50","$.75","$2.00"};


}


