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

public class ImageGenerator {

    private ImageGenerator() {}

    private static StringBuilder addButtonCore(int outerWidth,
                                               int outerHeight,
                                               int xOffset,
                                               int yOffset) {
        return new StringBuilder("<rect x='")
            .append(xOffset + 6)
            .append("' y='")
            .append(yOffset + 6)
            .append("' width='")
            .append(outerWidth - 12)
            .append("' height='")
            .append(outerHeight - 13)
            .append("' rx='10' stroke-width='2' stroke='")
            .append(Settings.isWhiteTheme() ? "#989898" : "orange")
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

    private static StringBuilder addDigit(boolean visuallyImpaired,
                                          int value,
                                          int width,
                                          int height,
                                          int xOffset,
                                          int yOffset) {
        return new StringBuilder("<a xlink:href=\"javascript:addDigit('")
            .append(value)
            .append("')\">")
            .append(addButtonCore(width, height, xOffset, yOffset))
            .append("<text x='")
            .append(xOffset + (double)(width + 1) / 2)
            .append("' y='")
            .append(yOffset + (visuallyImpaired ? 43 : 33))
            .append("' font-family='Roboto' font-size='")
            .append(visuallyImpaired ? 38 : 26)
            .append("' text-anchor='middle' font-weight='bold'>")
            .append(value)
            .append("</text></a>");
    }

    public static StringBuilder getKeyBoard(boolean visuallyImpaired) {
        int xOffset;
        int yOffset;

        StringBuilder s = new StringBuilder("<svg viewBox='0 0 416 ")
            .append(visuallyImpaired ? 210 : 162)
            .append(
                "' xmlns='http://www.w3.org/2000/svg' " +
                    "xmlns:xlink='http://www.w3.org/1999/xlink'>" +
                "<defs>" +
                    "<filter height='150%' width='150%' y='-25%' x='-25%' id='actorsBlur'>" +
                        "<feGaussianBlur stdDeviation='3'/>" +
                    "</filter>" +
                "</defs>");
/*
   s.append("<rect width='416' height='")
    .append(visuallyImpaired ? 210 : 162)
    .append("' fill='white'/>");

 */

        int width = visuallyImpaired ? 68 : 56;
        int distance = visuallyImpaired ? 87 : 90;
        int height = visuallyImpaired ? 60 : 48;
        int middle = visuallyImpaired ? 76 : 57;
        int last = middle + middle;
        xOffset = 0;
        for (int digit = 0; digit < 10; digit++) {
            if (digit == 4 || digit == 7) {
                xOffset = 0;
            }
            yOffset = digit < 4 ? 0 : digit > 6 ? last : middle;
            s.append(addDigit(visuallyImpaired,
                              digit,
                              width,
                              height,
                              xOffset,
                              yOffset));
            xOffset += distance;
        }

        int deleteSize = visuallyImpaired ? 30 : 20;
        s.append("<a xlink:href=\"javascript:deleteDigit()\">")
            .append(addButtonCore(width, height, distance * 4, 0))
            .append("<svg x='")
            .append(distance * 4  + (width - deleteSize) / 2)
            .append("' y='")
            .append(((height - deleteSize) / 2) - 0.5)
            .append(
                "' width='")
            .append(deleteSize)
            .append("' height='")
            .append(deleteSize)
            .append("' viewBox='0 0 20 20'>" +
                    "<path fill='#be1018' d='m 5,9.9 6.3,9.8 H 7.7 l -7.2,-9.5 V 9.6 l 7.2,-9.4 " +
                    "H 11.3 Z M 13.2,9.9 19.5,19.7 H 15.9 L 8.7,10.2 8.7,9.6 15.9,0.2 h 3.6 z'/>" +
                    "</svg>" +
                    "</a>");

        int validateWidth = visuallyImpaired ? 100 : 80;
        int validateHeight = visuallyImpaired ? 78 : 60;
        int validateSize = visuallyImpaired ? 50 : 30;
        s.append("<a xlink:href=\"javascript:validatePin()\">")
            .append(addButtonCore(validateWidth,
                                  validateHeight,
                                  xOffset = distance * 4 + width - validateWidth,
                                  yOffset = middle * 2 + height - validateHeight))
            .append("<svg x='")
            .append(xOffset + (validateWidth - validateSize) / 2)
            .append("' y='")
            .append(yOffset + (validateHeight - validateSize) / 2)
            .append(
                "' width='")
            .append(validateSize)
            .append("' height='")
            .append(validateSize)
            .append("' viewBox='0 0 30 30'>" +
                    "<path fill='#009900' d='m 0.8,14.2 c 5.4,5.7 8.4,12.4 8.8,13.5 h 2 " +
                    "C 16.6,17.4 22.3,9 29.1,2 h -4 C 18.5,9.5 16.4,12.5 10.6,23 8.8,19.6 " +
                    "7.6,17.8 4.8,14.2 Z'/>" +
                    "</svg>" +
                    "</a>" +

                    "</svg>");
        return s;
    }

    public static StringBuilder getFingerPrintSwitch(boolean visuallyImpaired) {
        String switchColor = Settings.isWhiteTheme() ? "blue" : "white";
        return new StringBuilder("<svg style='height:")
            .append(visuallyImpaired ? 18 : 12)
            .append("pt' viewBox='0 0 100 50' xmlns='http://www.w3.org/2000/svg'>" +
                "<rect stroke='")
            .append(switchColor)
            .append(
                "' stroke-width='3' fill='none' rx='6' y='1.5' x='1.5' height='47' width='97'/>" +
                "<g fill='")
            .append(switchColor)
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
            .append(Settings.isWhiteTheme() ? "blue" : "white")
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

    public static StringBuilder getStylizedCardImage(int widthInPixels, int selectedCard) {
        return new StringBuilder("<svg style='width:")
            .append(widthInPixels)
            .append("px;cursor:pointer;vertical-align:middle' " +
                "viewBox='0 0 320 200' xmlns='http://www.w3.org/2000/svg'>" +
                "<defs>" +
                "<clipPath id='cardClip'>" +
                "<rect rx='15' width='300' height='180' x='0' y='0'/>" +
                "</clipPath>")
            .append(Settings.isWhiteTheme() ?
                "<filter id='dropShaddow'>" +
                    "<feGaussianBlur stdDeviation='2.4'/>" +
                    "</filter>" +
                    "<linearGradient y1='0' x1='0' y2='1' x2='1' id='innerCardBorder'>" +
                    "<stop offset='0' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "<stop offset='0.48' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "<stop offset='0.52' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    "<stop offset='1' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    "</linearGradient>" +
                    "<linearGradient y1='0' x1='0' y2='1' x2='1' id='outerCardBorder'>" +
                    "<stop offset='0' stop-color='#b0b0b0'/>" +
                    "<stop offset='0.48' stop-color='#b0b0b0'/>" +
                    "<stop offset='0.52' stop-color='#808080'/>" +
                    "<stop offset='1' stop-color='#808080'/>" +
                    "</linearGradient>" +
                    "</defs>" +
                    "<rect filter='url(#dropShaddow)' rx='16' " +
                    "width='302' height='182' x='12' y='12' fill='#c0c0c0'/>"
                :
                "<linearGradient y1='0' x1='0' y2='1' x2='1' id='innerCardBorder'>" +
                    "<stop offset='0' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "<stop offset='0.48' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "<stop offset='0.52' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    "<stop offset='1' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    "</linearGradient>" +
                    "<filter id='dropShaddow'>" +
                    "<feGaussianBlur stdDeviation='3.5'/>" +
                    "</filter>" +
                    "</defs>" +
                    "<rect filter='url(#dropShaddow)' rx='16' " +
                    "width='305' height='184' x='7' y='8' fill='white'/>")

            .append(
                "<svg x='10' y='10' clip-path='url(#cardClip)'>" +
                "<image id='cardImage' width='300' height='180' href='/card/")
            .append(selectedCard)
            .append("'/></svg>")

            .append(Settings.isWhiteTheme() ?
                "<rect fill='none' x='11' y='11' width='298' height='178' " +
                    "rx='14.7' stroke='url(#innerCardBorder)' stroke-width='2.7'/>" +
                    "<rect fill='none' x='9.5' y='9.5' width='301' height='181' " +
                    "rx='16' stroke='url(#outerCardBorder)'/>"
                :
                "<rect fill='none' x='11' y='11' width='298' height='178' " +
                    "rx='14.75' stroke='url(#innerCardBorder)' stroke-width='2'/>" +
                    "<rect fill='none' x='8.5' y='8.5' width='303' height='183' " +
                    "rx='17' stroke='#e0e0e0'/>" +
                    "<rect fill='none' x='9.5' y='9.5' width='301' height='181' " +
                    "rx='16' stroke='#162c44'/>")

            .append("</svg>");
    }

    public static StringBuilder getLeftArrow(int arrowWidth, boolean visuallyImpaired) {
        String arrowColor = Settings.isWhiteTheme() ? "blue" : "white";
        return new StringBuilder("<svg style='width:")
            .append(arrowWidth)
            .append("px' viewBox='0 0 110 320' xmlns='http://www.w3.org/2000/svg'>" +
                "<path d='M100 20 L100 300 L10 160 Z' fill='")
            .append(visuallyImpaired ? arrowColor : "none")
            .append("' stroke='")
            .append(arrowColor)
            .append("' stroke-width='10'/></svg>");
    }

    public static StringBuilder getRightArrow(int arrowWidth, boolean visuallyImpaired) {
        String arrowColor = Settings.isWhiteTheme() ? "blue" : "white";
        return new StringBuilder("<svg style='width:")
            .append(arrowWidth)
            .append("px' viewBox='0 0 110 320' xmlns='http://www.w3.org/2000/svg'>" +
                "<path d='M10 20 L10 300 L100 160 Z' fill='")
            .append(visuallyImpaired ? arrowColor : "none")
            .append("' stroke='")
            .append(arrowColor)
            .append("' stroke-width='10'/></svg>");
    }

    public static StringBuilder getSpinner() {
        return new StringBuilder(
                "<svg style='height:1em;animation:spin 2s linear infinite' " +
                "viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'><g stroke='")
            .append(Settings.isWhiteTheme() ? "black" : "yellow")
            .append(
                "' stroke-width='10' stroke-linecap='round'>" +
                "<line x1='95' y1='50' x2='75' y2='50' stroke-opacity='1'/>" +
                "<line x1='88.97' y1='72.5' x2='71.65' y2='62.5' stroke-opacity='0.94'/>" +
                "<line x1='72.5' y1='88.97' x2='62.5' y2='71.65' stroke-opacity='0.88'/>" +
                "<line x1='50' y1='95' x2='50' y2='75' stroke-opacity='0.82'/>" +
                "<line x1='27.5' y1='88.97' x2='37.5' y2='71.65' stroke-opacity='0.76'/>" +
                "<line x1='11.02' y1='72.5' x2='28.34' y2='62.5' stroke-opacity='0.7'/>" +
                "<line x1='5' y1='50' x2='25' y2='50' stroke-opacity='0.64'/>" +
                "<line x1='11.02' y1='27.5' x2='28.34' y2='37.5' stroke-opacity='0.59'/>" +
                "<line x1='27.49' y1='11.02' x2='37.49' y2='28.34' stroke-opacity='0.53'/>" +
                "<line x1='49.99' y1='5' x2='49.99' y2='25' stroke-opacity='0.47'/>" +
                "<line x1='72.49' y1='11.02' x2='62.49' y2='28.34' stroke-opacity='0.41'/>" +
                "<line x1='88.97' y1='27.49' x2='71.65' y2='37.49' stroke-opacity='0.35'/>" +
                "</g>" +
                "</svg>");
    }

    public static StringBuilder getFailedIcon() {
        return new StringBuilder(
                "<svg style='height:1em' " +
                "viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'>" +
                "<circle cx='50' cy='50' fill='#ffff00' r='45' stroke='#ff0000' stroke-width='10'/>" +
                "<path d='m30,70c10,-8.5 30,-8.5 40,0' fill='none' " +
                "stroke='#000000' stroke-linecap='round' stroke-width='10'/>" +
                "<g fill='none' stroke='#007fff' stroke-linecap='round' stroke-width='15'>" +
                "<line x1='30' x2='30' y1='30' y2='40'/>" +
                "<line x1='70' x2='70' y1='30' y2='40'/>" +
                "</g></svg>");
    }
}
