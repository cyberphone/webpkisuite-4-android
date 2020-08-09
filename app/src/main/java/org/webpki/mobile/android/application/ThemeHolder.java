/*
 *  Copyright 2006-2018 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.mobile.android.application;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ThemeHolder {

    private ThemeHolder() {}

    private static final String THEME_FILE = "Theme";

    static Boolean whiteTheme;

    public static boolean isWhiteTheme(Context caller) {
        if (whiteTheme == null) {
            whiteTheme = false;
            try {
                FileInputStream fis = caller.openFileInput(THEME_FILE);
                whiteTheme = fis.read() == 1;
            } catch (IOException e) {
            }
        }
        return whiteTheme;
    }

    public static void writeTheme(Context caller, boolean whiteTheme) {
        ThemeHolder.whiteTheme = whiteTheme;
        try {
            FileOutputStream fos = caller.openFileOutput(THEME_FILE, Context.MODE_PRIVATE);
            fos.write(whiteTheme ? (byte) 1 : (byte) 0);
            fos.close();
        } catch (IOException e) {
        }
    }

    static String addButtonCore(int outerWidth, int outerHeight, int xOffset, int yOffset) {
        return new StringBuilder("<rect x='")
            .append(xOffset + 6)
            .append("' y='")
            .append(yOffset + 6)
            .append("' width='")
            .append(outerWidth - 12)
            .append("' height='")
            .append(outerHeight - 13)
            .append("' rx='10' ry='10' stroke-width='2' stroke='")
            .append(whiteTheme ? "#989898" : "orange")
            .append("' fill='#a5a5a5'/><rect x='")
            .append(xOffset + 8)
            .append("' y='")
            .append(yOffset + 8)
            .append("' width='")
            .append(outerWidth - 16)
            .append("' height='")
            .append(outerHeight - 17)
            .append("' rx='8' ry='8' fill='#fcfcfc' filter='url(#actorsBlur)'/>").toString();
    }

    static String addDigit(int value, int xOffset, int yOffset) {
        return new StringBuilder("<a xlink:href=\"javascript:addDigit('")
            .append(value)
            .append("')\">")
            .append(addButtonCore(56, 48, xOffset, yOffset))
            .append("<text x='")
            .append(xOffset + 28.5)
            .append("' y='")
            .append(yOffset + 33)
            .append("' font-family='Roboto' font-size='26' " +
                    "text-anchor='middle' font-weight='bold'>")
            .append(value)
            .append("</text></a>").toString();
    }

    public static StringBuilder getFingerPrintSwitch() {
        return new StringBuilder(
                "<svg style='height:12pt' viewBox='0 0 100 50' xmlns='http://www.w3.org/2000/svg'>" +
                "<rect stroke='")
            .append(whiteTheme ? "grey" : "white")
            .append(
                "' stroke-width='3' fill='none' rx='6' y='1.5' x='1.5' height='47' width='97'/>" +
                "<g fill='")
            .append(whiteTheme ? "grey" : "white")
            .append(
                "'>" +
                  "<circle cy='26' r='6' cx='17'/>" +
                  "<circle cy='26' r='6' cx='39'/>" +
                  "<circle cy='26' r='6' cx='61'/>" +
                  "<circle cy='26' r='6' cx='83'/>" +
                "</g></svg>");
    }

    public static StringBuilder getFingerPrintSymbol(String strokeSize,
                                                     String javaScript,
                                                     String displayMode,
                                                     int leftPadding,
                                                     int height) {
        return new StringBuilder(
                "<svg style='height:")
            .append(height)
            .append("pt;padding-left:")
            .append(leftPadding)
            .append("pt;display:")
            .append(displayMode)
            .append(
                "' viewBox='0 0 34 34' xmlns='http://www.w3.org/2000/svg' " +
                "xmlns:xlink='http://www.w3.org/1999/xlink'><g stroke='")
            .append(whiteTheme ? "blue" : "white")
            .append("' stroke-linecap='round' stroke-width='")
            .append(strokeSize)
            .append("' fill='none'>")
            .append(
                "<path d='M 2.3671537,23.829104 C -1.4883611,11.602125 5,6 5,6'/>" +
                "<path d='M 7.9345667,3.9672828 C 17.356614,-2.7198321 31.292018,2.2621923 " +
                    "32.98557,18.040416'/>" +
                "<path d='M 5.9415655,29.97528 C 5.8977627,27.293051 6.4844791,23.889769 " +
                    "5.8351751,21.754189 -0.17364441,1.9910089 28.176603,-3.0493321 " +
                    "29.259392,19.619897 c 0.04341,0.908896 0.02622,5.042147 -0.172847,7.397275'/>" +
                "<path d='M 10.086529,32.043763 C 10.058709,28.609393 10.350804,24.675115 " +
                    "9.3265096,20.828601 8.1249875,16.316549 8.1917409,12.304026 11.968,8.95'/>" +
                "<path d='M 15.239128,8.1283894 C 25.141037,6.8797069 26.493401,18.099933 " +
                    "26.000916,29.985946'/>" +
                "<path d='m 13.941565,32.935003 c -0.05949,-2.650859 0.414679,-7.462289 " +
                    "-0.319662,-11.90003 -4.1477696,-11.0693466 6.512761,-10.232544 " +
                    "7.375509,-4.960144 0.79618,3.902636 1.053467,9.849901 1.053467,9.849901'/>" +
                "<path d='m 16.702356,16.333355 c 1.826717,5.055217 1.210888,16.631062 " +
                    "1.210888,16.631062'/>" +
                "<path d='m 21.944845,28.932917 0.0084,2.044847'/>" +
                "</g>" +
                "<a xlink:href=\"javascript:")
            .append(javaScript)
            .append("\"><rect width='34' height='34' opacity='0'/></a></svg>");
    }

    public static String getKeyBoard() {
        StringBuilder s = new StringBuilder("<svg viewBox='0 0 416 162' " +
                "xmlns='http://www.w3.org/2000/svg' " +
                "xmlns:xlink='http://www.w3.org/1999/xlink'>" +
                "<defs>" +
                "<filter height='150%' width='150%' y='-25%' x='-25%' id='actorsBlur'>" +
                "  <feGaussianBlur stdDeviation='3'/>" +
                "</filter>" +
                "</defs>");

        for (int i = 0, x = 0; i < 10; i++) {
            if (i == 4 || i == 7) {
                x = 0;
            }
            s.append(addDigit(i, x, i < 4 ? 0 : i > 6 ? 114 : 57));
            x += 90;
        }

        s.append("<a xlink:href=\"javascript:deleteDigit()\">")
        .append(addButtonCore(56, 48, (90 * 3) + 90, 0))
        .append("<text x='")
        .append(((90 * 3) + 90)  + 28)
        .append("' y='")
        .append(36.5)
        .append("' font-family='Roboto' font-size='50' " +
                "text-anchor='middle' fill='#be1018'>&#171;</text></a>" +

                "<a xlink:href=\"javascript:validatePin()\">")
        .append(addButtonCore(80, 60, 90 * 3 + 90 + 56 - 80, 102))
        .append("<path fill='#009900' " +
                "d='m 362,131 c 5.4,5.7 8.4,12.4 8.8,13.5 h 2 " +
                "c 5,-10.3 10.7,-18.7 17.5,-25.7 h -4 c -6.6,7.5 " +
                "-8.7,10.5 -14.5,21 -1.8,-3.4 -3,-5.2 -5.8,-8.8 z'/>" +
                "</a></svg>");
        return s.toString();
    }
}
