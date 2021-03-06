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

import android.content.Intent;
import android.database.Cursor;

import android.os.Bundle;

import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.webpki.mobile.android.R;

import org.webpki.mobile.android.application.Settings;

import org.webpki.mobile.android.util.WebViewHtmlLoader;

public class ReceiptListActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_list);
        WebView receiptList = (WebView) findViewById(R.id.receiptList);
        WebSettings webSettings = receiptList.getSettings();
        webSettings.setJavaScriptEnabled(true);
        receiptList.addJavascriptInterface (this, "Saturn");
        Settings.initialize(getApplicationContext());

        StringBuilder listText =
            new StringBuilder("<html><body>"+
                "<div>Dear tester, here there should be a receipt search dialog " +
                "but it won't be available until the next release&nbsp;&#x1f60f;</div>" +
                "<div style='margin-top:0.5em'>Click on a table entry to view the receipt.</div>" +
                "<div style='overflow-x:auto'>" +
                "<table style='margin:1em auto 0 auto' cellspacing='5' cellpadding='5'>" +
                "<tr style='text-align:center'><th>Received</th><th>Merchant</th><th>Total</th></tr>");

        Cursor cursor = Database.getReceiptSelection(this);
        while(cursor.moveToNext()) {
            listText.append("<tr style='background-color:#e0e0e8' onclick='Saturn.selectReceipt(")
                    .append(cursor.getInt(0))
                    .append(")'>");
            for (int i = 1 ; i < cursor.getColumnCount(); i++) {
                listText.append(i == 3 ? "<td style='text-align:right'>" : "<td>")
                        .append(cursor.getString(i))
                        .append("</td>");
            }
            listText.append("</tr>");
        }
        cursor.close();

        WebViewHtmlLoader.loadHtml(receiptList, listText.append("</div></table></body></html>"));
    }

    @JavascriptInterface
    public void selectReceipt(int rowId) {
        Intent intent = new Intent(this, ReceiptViewActivity.class);
        intent.putExtra(ReceiptViewActivity.ROW_ID_EXTRA, rowId);
        startActivity(intent);
    }
}
