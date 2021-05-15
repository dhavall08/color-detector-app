package com.dr.colordetector;

import android.graphics.Color;
import android.util.Log;

class MatchColor {
    static String getColorName(Colors colors, int r, int g, int b) {
        String name = "";

        if (r < 30 && g < 30 && b < 30) {
            name = "Black";
        } else if (r > 235 && g > 235 && b > 235) {
            name = "White";
        } else if (r > 125 && g < 30 && b < 30) {
            name = "Red";
        } else if (g > 70 && r < 30 && b < 30) {
            name = "Green";
        } else if (b > 70 && g < 30 && r < 30) {
            name = "Blue";
        } else if ((r > b - 40) && b > 100 && g < 30 && r > 140) {
            name = "Pink";
        } else if (((r - g) < 40) && g > 100 && b < 30 && r > 140) {
            name = "Yellow";
        } else if (Math.abs(r - g) < 40 && g > 170 && r < 30 && b > 170) {
            name = "Cyan";
        } else if (g + 50 < r && g > 60 && r > 150 && b < 60) {
            name = "Orange";
        } else if ((b > r - 40) && b > 140 && g < 30 && r > 100) {
            name = "Purple";
        } else if (Math.abs(r - b) < 15 && Math.abs(g - b) < 15 && Math.abs(r - g) < 15) {
            name = "Grey";
        } else {
            name = findColor(colors, r, g, b);
        }
        return name;
    }

    private static String findColor(Colors colors, int c1, int c2, int c3) {
        String realName = "";
        String normalName = "";
        float[] fArr = new float[]{c1, c2, c3};
        float[] fArr2 = new float[]{0.0f, 0.0f, 0.0f};
        double d = 9.99999999E8d;
        for (int i2 = 0; i2 < colors.getColorsArray().size(); i2++) {
            int intValue = colors.getColorsArray().get(i2);
            fArr2[0] = Color.red(intValue);
            fArr2[1] = Color.green(intValue);
            fArr2[2] = Color.blue(intValue);
            double sqrt = Math.sqrt((Math.pow((double) (Color.red(intValue) - c1), 2.0d)
                    + Math.pow((double) (Color.green(intValue) - c2), 2.0d))
                    + Math.pow((double) (Color.blue(intValue) - c3), 2.0d))
                    + (
                    Math.sqrt((Math.pow((double) (fArr[0] - fArr2[0]), 2.0d)
                            + Math.pow((double) (fArr[1] - fArr2[1]), 2.0d))
                            + Math.pow((double) (fArr[2] - fArr2[2]), 2.0d)) * 2.0d);
            if (sqrt < d) {
                realName = colors.getColorsNames().get(i2);
                normalName = colors.getColorsSimpleNames().get(i2);
                d = sqrt;
            }
        }

        Log.d("MatchColor","Color fetch from file. Real name:"+realName);
        return realName;
    }

}