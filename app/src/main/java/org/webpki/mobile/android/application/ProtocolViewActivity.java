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

import java.io.ObjectInputStream;

import java.util.ArrayList;

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.mobile.android.R;

import org.webpki.mobile.android.util.WebViewHtmlLoader;

import android.os.Bundle;

import android.webkit.WebView;

import android.app.Activity;

import android.content.Intent;

public class ProtocolViewActivity extends Activity {

    public static final String LOG_FILE = "logfile";

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protocol_view);
        WebView logView = (WebView) findViewById(R.id.protocolData);
        logView.getSettings().setSupportZoom(true);
        logView.getSettings().setBuiltInZoomControls(true);
        Intent intent = getIntent();
        StringBuilder logMessage = new StringBuilder("<html><body style=\"font-size:8pt;" +
            "font-family:arial,verdana,helvetica;word-break:break-all\">");
        try {
            int count = 0;
            boolean received = true;
            for (byte[] part : (ArrayList<byte[]>) new ObjectInputStream(
                openFileInput(intent.getStringExtra(LOG_FILE))).readObject()) {
                if (count++ > 0) {
                    logMessage.append("<br>&nbsp;<br>");
                }
                logMessage.append(
                    "<table align=\"center\"><tr><td bgcolor=\"#F0F0F0\" align=\"center\" " +
                           "style=\"width:100pt;font-size:8pt;font-family:verdana,arial;" +
                        "border:solid;border-width:1px;padding:4px\">&nbsp;")
                        .append(count)
                        .append(received ? ": Received from Server" : ": Sent to Server")
                        .append("&nbsp;</td></tr></table>");
                received = !received;
                JSONObjectWriter json = new JSONObjectWriter(JSONParser.parse(part));
                logMessage.append(json.serializeToString(JSONOutputFormats.PRETTY_HTML));
            }
        } catch (Exception e) {
            logMessage.append("No log data available");
        }
         WebViewHtmlLoader.loadHtml(logView, logMessage.append("</body></html>"));
    }
}
