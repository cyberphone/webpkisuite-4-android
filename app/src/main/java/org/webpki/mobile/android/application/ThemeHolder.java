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

    public static boolean visuallyImpaired;

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

    static StringBuilder addButtonCore(int outerWidth, int outerHeight, int xOffset, int yOffset) {
        return new StringBuilder("<rect x='")
            .append(xOffset + 6)
            .append("' y='")
            .append(yOffset + 6)
            .append("' width='")
            .append(outerWidth - 12)
            .append("' height='")
            .append(outerHeight - 13)
            .append("' rx='10' stroke-width='2' stroke='")
            .append(whiteTheme ? "#989898" : "orange")
            .append("' fill='#a5a5a5'/><rect x='")
            .append(xOffset + 8)
            .append("' y='")
            .append(yOffset + 8)
            .append("' width='")
            .append(outerWidth - 16)
            .append("' height='")
            .append(outerHeight - 17)
            .append("' rx='8' fill='#fcfcfc' filter='url(#actorsBlur)'/>");
    }

    static StringBuilder addDigit(int value, int xOffset, int yOffset) {
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
            .append("</text></a>");
    }

    public static StringBuilder getKeyBoard() {
        int xOffset;
        int yOffset;

        StringBuilder s = new StringBuilder("<svg viewBox='0 0 416 162' " +
            "xmlns='http://www.w3.org/2000/svg' " +
            "xmlns:xlink='http://www.w3.org/1999/xlink'>" +
            "<defs>" +
              "<filter height='150%' width='150%' y='-25%' x='-25%' id='actorsBlur'>" +
                "<feGaussianBlur stdDeviation='3'/>" +
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
            .append("<svg x='")
            .append(((90 * 3) + 90)  + 18)
            .append("' y='")
            .append(14)
            .append(
                "' width='20' height='20' viewBox='0 0 20 20'>" +
                    "<path fill='#be1018' d='m 5,9.9 6.3,9.8 H 7.7 l -7.2,-9.5 V 9.6 l 7.2,-9.4 " +
                    "H 11.3 Z M 13.2,9.9 19.5,19.7 H 15.9 L 8.7,10.2 8.7,9.6 15.9,0.2 h 3.6 z'/>" +
                    "</svg>" +
                    "</a>" +

                    "<a xlink:href=\"javascript:validatePin()\">")
            .append(addButtonCore(80, 60, xOffset = 90 * 3 + 90 + 56 - 80, yOffset = 102))
            .append("<svg x='")
            .append(xOffset + 25)
            .append("' y='")
            .append(yOffset + 15)
            .append(
                "' width='30' height='30' viewBox='0 0 30 30'>" +
                    "<path fill='#009900' d='m 0.8,14.2 c 5.4,5.7 8.4,12.4 8.8,13.5 h 2 " +
                    "C 16.6,17.4 22.3,9 29.1,2 h -4 C 18.5,9.5 16.4,12.5 10.6,23 8.8,19.6 " +
                    "7.6,17.8 4.8,14.2 Z'/>" +
                    "</svg>" +
                    "</a>" +

                    "</svg>");
        return s;
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
                "' viewBox='0 0 18 20' xmlns='http://www.w3.org/2000/svg' " +
                "xmlns:xlink='http://www.w3.org/1999/xlink'><g stroke='")
            .append(whiteTheme ? "blue" : "white")
            .append("' stroke-linecap='round' stroke-width='")
            .append(strokeSize)
            .append("' fill='none'>")
            .append(
                "<path d='m 3.2250671,1.9837657 c 3.1417184,-1.8867533 7.9238359," +
                    "-2.0434273 11.5815359,0'/>" +
                "<path d='M 0.5,7.246675 C 4.3588413,1.4874579 13.658833,1.5817769 " +
                    "17.495814,7.2108188'/>" +
                "<path d='M 1.970102,16.481672 C -1.7916136,7.2417778 9.8090522,1.4363037 " +
                    "15.176462,8.2520265 c 1.465529,1.8609795 2.695267,6.1462945 " +
                    "-0.770891,6.8124565 -1.6153,0.310445 -2.622651,-1.097225 " +
                    "-2.802546,-2.332088 -0.185372,-1.27246 -0.996557,-2.8067915 " +
                    "-3.0332813,-2.512194 -3.9497585,0.571306 -2.2510633,8.04581 " +
                    "3.3325123,9.237531'/>" +
                "<path d='M 6.7389694,19.298419 C 0.3713563,12.527176 5.4657885,6.2690732 " +
                    "10.815088,8.0873415 c 1.459183,0.495987 3.338154,2.0774515 " +
                    "3.346104,4.6135465'/>" +
                "<path d='M 15.04059,17.316012 C 11.229809,18.072804 8.9429657,15.238722 " +
                    "8.9261197,12.653027'/>" +
                "</g>" +
                "<a xlink:href=\"javascript:")
            .append(javaScript)
            .append(
                "\"><rect width='18' height='20' opacity='0'/>" +
                "</a>" +
                "</svg>");
    }
}
