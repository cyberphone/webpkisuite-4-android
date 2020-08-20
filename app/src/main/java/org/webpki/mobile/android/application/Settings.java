/*
 *  Copyright 2013-2020 WebPKI.org (http://webpki.org).
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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Settings {

    private Settings() {}

    private static final String SETTINGS_FILE = "settings.json";

    // Set by user patterns
    private static final String LAST_KEY_ID       = "lastKeyId";
    private static final String PREFER_BIOMETRICS = "biometricPreferred";

    // Set by explicit preference dialogs
    private static final String WHITE_THEME       = "whiteTheme";
    private static final String VISUALLY_IMPAIRED = "visuallyImpaired";

    static Context applicationContext;

    static int lastKeyId = -1;

    static boolean whiteTheme;

    static boolean visuallyImpaired;

    static boolean preferBiometrics = true;

    public static void writeTheme(boolean whiteTheme) {
        Settings.whiteTheme = whiteTheme;
        writeAll();
    }

    public static void writeAccessibility(boolean visuallyImpaired) {
        Settings.visuallyImpaired = visuallyImpaired;
        writeAll();
    }

    public static void writeLastKeyId(int lastKeyId) {
        Settings.lastKeyId = lastKeyId;
        writeAll();
    }

    public static int getLastKeyId() {
        return lastKeyId;
    }

    public static boolean isVisuallyImpaired() {
        return visuallyImpaired;
    }

    public static boolean isBiometricsPreferred() {
        return preferBiometrics;
    }

    public static boolean isWhiteTheme() {
        return whiteTheme;
    }

    public static void setBiometricPreferred(boolean preferBiometrics) {
        Settings.preferBiometrics = preferBiometrics;
    }

    private static void writeAll() {
        try {
            FileOutputStream fos =
                applicationContext.openFileOutput(SETTINGS_FILE, Context.MODE_PRIVATE);
            fos.write(new JSONObjectWriter()
                .setInt(LAST_KEY_ID, lastKeyId)
                .setBoolean(PREFER_BIOMETRICS, preferBiometrics)
                .setBoolean(WHITE_THEME, whiteTheme)
                .setBoolean(VISUALLY_IMPAIRED, visuallyImpaired)
                .serializeToBytes(JSONOutputFormats.NORMALIZED));
            fos.close();
         } catch (IOException e) {
        }
    }

    public static void initialize(Context applicationContext) {
        if (Settings.applicationContext != null) {
            return;
        }
        Settings.applicationContext = applicationContext;
        try {
            FileInputStream fis = applicationContext.openFileInput(SETTINGS_FILE);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            JSONObjectReader json = JSONParser.parse(buffer);
            lastKeyId = json.getInt(LAST_KEY_ID);
            preferBiometrics = json.getBoolean(PREFER_BIOMETRICS);
            whiteTheme = json.getBoolean(WHITE_THEME);
            visuallyImpaired = json.getBoolean(VISUALLY_IMPAIRED);
        } catch (IOException e) {
        }
    }
}
