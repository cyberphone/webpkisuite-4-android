package org.webpki.mobile.android.receipts;

import android.app.Activity;

import android.database.Cursor;

import android.os.Bundle;

import android.webkit.WebView;

import org.webpki.mobile.android.R;

import org.webpki.mobile.android.util.WebViewHtmlLoader;

public class ReceiptListActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_list);
        WebView receiptListView = (WebView) findViewById(R.id.receiptList);
        StringBuilder listText =
            new StringBuilder("<html><body><table cellspacing=\"5\" cellpadding=\"5\">");

        Cursor cursor = Database.getReceiptSelection(this);
        while(cursor.moveToNext()) {
            listText.append("<tr>");
            for (int i = 1 ; i < cursor.getColumnCount(); i++) {
                listText.append("<td>").append(cursor.getString(i)).append("</td>");
            }
            listText.append("</tr>");
        }
        WebViewHtmlLoader.loadHtml(receiptListView, listText.append("</body></body></html>"));
    }
}
