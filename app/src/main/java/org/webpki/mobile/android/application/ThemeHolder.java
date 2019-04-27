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
            whiteTheme = true;
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
}
