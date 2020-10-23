/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
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
package org.webpki.mobile.android.receipts;

import android.app.Activity;

import android.database.Cursor;

import android.os.Bundle;

import android.util.Base64;

import android.webkit.WebSettings;
import android.webkit.WebView;

import org.webpki.mobile.android.R;

import org.webpki.mobile.android.util.WebViewHtmlLoader;

public class ReceiptViewActivity extends Activity {

    static final String ROW_ID_EXTRA = "rowId";

    private static int LOGOTYPE_AREA = 80;  // We give logotypes the same area to play around in


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_view);
        WebView receiptView = (WebView) findViewById(R.id.receiptView);
        WebSettings webSettings = receiptView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        StringBuilder listText =
            new StringBuilder("<html><body>");

        int rowId = getIntent().getIntExtra(ROW_ID_EXTRA, 1);

        Cursor cursor = Database.getReceipt(this, rowId);
        if (cursor.moveToNext()) {
            listText.append("<script>\n" +
                            "function adjustImage(image) {\n" +
                            "  image.style.width = " +
                               "Math.sqrt((" +
                               LOGOTYPE_AREA +
                               " * image.offsetWidth) / image.offsetHeight) + 'em';\n" +
                               "  image.style.visibility = 'visible';\n" +
                            "}\n"+
                            "</script>" +
                            "<img style='margin:0 auto 0.5em auto;max-width:90%;" +
                                "display:block;visibility:hidden' src='data:")
                    .append(cursor.getString(2))
                    .append(";base64,")
                    .append(Base64.encodeToString(cursor.getBlob(1), Base64.NO_WRAP))
                    .append("' onload=\"adjustImage(this)\">");
        } else {
            listText.append("SHIT");
        }
        cursor.close();

        WebViewHtmlLoader.loadHtml(receiptView, listText.append("</body></html>"));
    }

}
