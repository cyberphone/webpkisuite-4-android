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
        return new StringBuilder("<rect x=\"")
            .append(xOffset)
            .append("\" y=\"")
            .append(yOffset)
            .append("\" width=\"")
            .append(outerWidth)
            .append("\" height=\"")
            .append(outerHeight)
            .append("\" opacity=\"0\" fill=\"black\"/>" +
                    "<rect x=\"")
            .append(xOffset + 6)
            .append("\" y=\"")
            .append(yOffset + 6)
            .append("\" width=\"")
            .append(outerWidth - 12)
            .append("\" height=\"")
            .append(outerHeight - 13)
            .append("\" rx=\"10\" ry=\"10\" stroke-width=\"2\" stroke=\"")
            .append(whiteTheme ? "#989898" : "orange")
            .append("\" fill=\"#a5a5a5\"/>" +
                    "<rect x=\"")
            .append(xOffset + 8)
            .append("\" y=\"")
            .append(yOffset + 8)
            .append("\" width=\"")
            .append(outerWidth - 16)
            .append("\" height=\"")
            .append(outerHeight - 17)
            .append("\" rx=\"8\" ry=\"8\" fill=\"#fcfcfc\" filter=\"url(#actorsBlur)\"/>").toString();
    }

    static String addDigit(int value, int xOffset, int yOffset) {
        return new StringBuilder("<a xlink:href=\"javascript:addDigit('")
            .append(value)
            .append("')\">")
            .append(addButtonCore(56, 48, xOffset, yOffset))
            .append("<text x=\"")
            .append(xOffset + 28.5)
            .append("\" y=\"")
            .append(yOffset + 33)
            .append("\" font-family=\"Roboto\" font-size=\"26\" " +
                    "text-anchor=\"middle\" font-weight=\"bold\">")
            .append(value)
            .append("</text></a>").toString();
    }

    public static String getKeyBoard() {
        StringBuilder s = new StringBuilder("<svg viewBox=\"0 0 416 162\" " +
                "xmlns=\"http://www.w3.org/2000/svg\" " +
                "xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
                "<defs>" +
                "<filter height=\"150%\" width=\"150%\" y=\"-25%\" x=\"-25%\" id=\"actorsBlur\">" +
                "  <feGaussianBlur stdDeviation=\"3\"/>" +
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
        .append("<text x=\"")
        .append(((90 * 3) + 90)  + 28)
        .append("\" y=\"")
        .append(36.5)
        .append("\" font-family=\"Roboto\" font-size=\"50\" " +
                "text-anchor=\"middle\" fill=\"#be1018\">&#171;</text></a>" +

                "<a xlink:href=\"javascript:validatePin()\">")
        .append(addButtonCore(80, 60, 90 * 3 + 90 + 56 - 80, 102))
        .append("<path fill=\"#009900\" " +
                "d=\"m 364,131 c 4.5,4.9 7.2,9.7 8.9,13.5 h 2.1 " +
                "c 5,-10.3 10.7,-18.7 17.5,-25.7 h -4 " +
                "c -6.6,7.5 -8.7,10.5 -14.5,21 -1.8,-3.4 -3.1,-5.2 -5.8,-8.8 h -4 z\"/>" +
                "</a></svg>");
        return s.toString();
    }
}
