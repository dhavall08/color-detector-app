package com.dr.colordetector;

import android.content.Context;
import android.graphics.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

public final class Colors {
    private ArrayList<String> colorsNames = new ArrayList<>();
    private ArrayList<String> colorsSimpleNames = new ArrayList<>();
    private ArrayList<Integer> colorsArray = new ArrayList<Integer>();
    int d;
    String e;
    String f;

    Colors(Context context) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open("colors2.txt")));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    StringTokenizer stringTokenizer = new StringTokenizer(readLine, ",");
                    int parseColor = Color.parseColor("#" + stringTokenizer.nextToken());
                    String nextToken = stringTokenizer.nextToken();
                    String nextToken2 = stringTokenizer.nextToken();
                    this.colorsArray.add(parseColor);
                    this.colorsNames.add(nextToken);
                    this.colorsSimpleNames.add(nextToken2);
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getColorsNames() {
        return colorsNames;
    }

    public ArrayList<String> getColorsSimpleNames() {
        return colorsSimpleNames;
    }

    public ArrayList<Integer> getColorsArray() {
        return colorsArray;
    }

}