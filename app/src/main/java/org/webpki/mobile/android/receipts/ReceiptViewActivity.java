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

import java.io.IOException;

import java.math.BigDecimal;

import java.text.SimpleDateFormat;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;

import android.database.Cursor;

import android.os.Bundle;

import android.util.Base64;

import android.webkit.WebSettings;
import android.webkit.WebView;

import org.webpki.json.JSONParser;

import org.webpki.mobile.android.R;

import org.webpki.mobile.android.saturn.common.ReceiptDecoder;
import org.webpki.mobile.android.saturn.common.ReceiptLineItem;
import org.webpki.mobile.android.saturn.common.ReceiptShippingRecord;
import org.webpki.mobile.android.saturn.common.ReceiptTaxRecord;

import org.webpki.mobile.android.util.WebViewHtmlLoader;

public class ReceiptViewActivity extends Activity {

    static final String ROW_ID_EXTRA = "rowId";

    private static int LOGOTYPE_AREA = 80;  // We give logotypes the same area to play around in

    private static final String BORDER =
        "border-width:1px;border-style:solid;border-color:#a9a9a9";

    private static final String BOX_SHADOW_OFFSET = "0.3em";

    private static final String BOX_SHADOW = "box-shadow:" +
        BOX_SHADOW_OFFSET + " " +
        BOX_SHADOW_OFFSET + " " +
        BOX_SHADOW_OFFSET + " " +
        "#d0d0d0";

    private static final String HTML_TOP_ELEMENT =
        "<!DOCTYPE html><html><head>" +
        "<meta charset='utf-8'>" +
        "<style type='text/css'>" +
            " .header {font-size:1.6em;padding-bottom:1em}" +
            " .para {padding-bottom:0.4em}" +
            " .tftable {border-collapse:collapse;" + BOX_SHADOW + ";" +
                "margin-bottom:" + BOX_SHADOW_OFFSET + "}" +
            " .tftable td {white-space:nowrap;background-color:#ffffe0;" +
            "padding:0.4em 0.5em;" + BORDER + "}" +
            " .tftable th {white-space:nowrap;padding:0.4em 0.5em;" +
            "background:linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" +
            "text-align:center;" + BORDER +"}" +
            " .tableheader {margin:1.2em 0 0.6em 0}" +
            " .json {word-break:break-all;background-color:#f8f8f8;padding:1em;" +
            BORDER + ";" + BOX_SHADOW + "}" +
            " .icon {margin:0 auto 0.5em auto;max-width:90%;display:block;visibility:hidden}" +
            " body {margin:10pt;font-size:8pt;color:#000000;font-family:Roboto" +
                ";background-color:white}" +
            " code {font-size:9pt}" +
            " @media (max-width:800px) {code {font-size:8pt;}}" +
            " a {color:blue;text-decoration:none}" +
        "</style>" +
        "<script>\n" +
        "'use strict';\n" +
        "function adjustImage(image) {\n" +
        "  image.style.width = " +
           "Math.sqrt((" +
           LOGOTYPE_AREA +
           " * image.offsetWidth) / image.offsetHeight) + 'em';\n" +
           "  image.style.visibility = 'visible';\n" +
        "}\n"+
        "</script>" +
        "</head>" +
        "<body>";

    ReceiptDecoder receiptDecoder;

    StringBuilder html;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_view);
        WebView receiptView = (WebView) findViewById(R.id.receiptView);
        WebSettings webSettings = receiptView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        html = new StringBuilder(HTML_TOP_ELEMENT);

        int rowId = getIntent().getIntExtra(ROW_ID_EXTRA, 1);

        Cursor cursor = Database.getReceipt(this, rowId);
        if (cursor.moveToNext()) {
            try {
                receiptDecoder = new ReceiptDecoder(JSONParser.parse(cursor.getBlob(0)));
                html.append("<img class='icon' src='data:")
                    .append(cursor.getString(3))
                    .append(";base64,")
                    .append(Base64.encodeToString(cursor.getBlob(2), Base64.NO_WRAP))
                    .append("' onload=\"adjustImage(this)\">");
                buildHtmlReceipt(cursor.getString(1));
            } catch (Exception e) {
                fatalError(e.getMessage());
            }
        } else {
            fatalError("Receipt database error");
        }
        cursor.close();

        WebViewHtmlLoader.loadHtml(receiptView, html.append("</body></html>"));
    }

    private void fatalError(String message) {
        html = new StringBuilder(HTML_TOP_ELEMENT)
            .append("div style='color:red'>Failure:")
            .append(message)
            .append("</div>");
    }

    private static String showLines(String[] description) {
        StringBuilder lines = new StringBuilder();
        boolean next = false;
        for (String line : description) {
            if (next) {
                lines.append("<br>");
            } else {
                next = true;
            }
            lines.append(line);
        }
        return lines.toString();
    }

    static String displayUtcTime(GregorianCalendar dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateTime.getTime());
    }

    static String showMoney(ReceiptDecoder receiptDecoder, BigDecimal amount) throws IOException {
        return receiptDecoder.getCurrency().amountToDisplayString(amount, false);
    }

    static String optional(Object o) {
        return o == null ? "" : o.toString();
    }

    static class HtmlTable {

        static final String RIGHT_ALIGN = "text-align:right";

        StringBuilder html = new StringBuilder();


        HtmlTable(String headerText) {
            html.append("<div style='text-align:center' class='tableheader'>")
                .append(headerText)
                .append("</div>" +
            "<div style='overflow-x:auto'>" +
            "<table style='margin-left:auto;margin-right:auto' class='tftable'><tr>");
        }

        boolean headerMode = true;

        int headerCount;

        int cellCount;

        StringBuilder render() {
            return html.append("</table></div>");
        }

        HtmlTable addHeader(String name) {
            html.append("<th>")
                .append(name)
                .append("</th>");
            headerCount++;
            return this;
        }

        HtmlTable addCell(String data, String style) {
            if (headerMode) {
                headerMode = false;
                html.append("</tr>");
            }
            if (cellCount++ % headerCount == 0) {
                html.append("<tr>");
            }
            html.append(style == null  ? "<td>" : "<td style='" + style + "'>")
                .append(data == null ? "N/A" : data)
                .append("</td>");
            if (cellCount % headerCount == 0) {
                html.append("</tr>");
            }
            return this;
        }

        HtmlTable addCell(String data) {
            return addCell(data, null);
        }
    }

    private void buildHtmlReceipt(String payeeHomePage) throws IOException {
        // Optional Address Information
        if (receiptDecoder.getOptionalPhysicalAddress() != null ||
            receiptDecoder.getOptionalPhoneNumber() != null ||
            receiptDecoder.getOptionalEmailAddress() != null) {
            html.append("<div style='overflow-x:auto'><table style='margin:0.5em auto 0 auto'>");
            if (receiptDecoder.getOptionalPhysicalAddress() != null) {
                html.append("<tr><td>")
                    .append(showLines(receiptDecoder.getOptionalPhysicalAddress()))
                    .append("</td></tr>");
            }
            if (receiptDecoder.getOptionalPhoneNumber() != null ||
                receiptDecoder.getOptionalEmailAddress() != null) {
                html.append("<tr><td>");
                if (receiptDecoder.getOptionalPhoneNumber() != null) {
                    html.append("<i>Phone</i>: ")
                        .append(receiptDecoder.getOptionalPhoneNumber());
                }
                if (receiptDecoder.getOptionalEmailAddress() != null) {
                    if (receiptDecoder.getOptionalPhoneNumber() != null) {
                        html.append("<br>");
                    }
                    html.append("<i>e-mail</i>: ")
                        .append(receiptDecoder.getOptionalEmailAddress());
                }
                html.append("</td></tr>");
            }
            html.append("</table></div>");
        }
        BigDecimal optionalSubtotal = receiptDecoder.getOptionalSubtotal();
        BigDecimal optionalDiscount = receiptDecoder.getOptionalDiscount();
        ReceiptTaxRecord optionalTaxRecord = receiptDecoder.getOptionalTaxRecord();

        // Core Receipt Data
        HtmlTable coreData =
            new HtmlTable("Core Receipt Data")
                .addHeader("Payee Name")
                .addHeader("Total");
        if (optionalSubtotal != null) {
            coreData.addHeader("Subtotal");
        }
        if (optionalDiscount != null) {
            coreData.addHeader("Discount");
        }
        if (optionalTaxRecord != null) {
            coreData.addHeader("Tax");
        }
        coreData.addHeader("Time Stamp")
            .addHeader("Reference Id")
            .addCell("<a href='" +
                payeeHomePage +
                "' target='_blank'>" + receiptDecoder.getPayeeCommonName() + "</a>")
            .addCell(showMoney(receiptDecoder, receiptDecoder.getAmount()),
                HtmlTable.RIGHT_ALIGN);
        if (optionalSubtotal != null) {
            coreData.addCell(showMoney(receiptDecoder, optionalSubtotal),
                HtmlTable.RIGHT_ALIGN);
        }
        if (optionalDiscount != null) {
            coreData.addCell(showMoney(receiptDecoder, optionalDiscount),
                HtmlTable.RIGHT_ALIGN);
        }
        if (optionalTaxRecord != null) {
            coreData.addCell(showMoney(receiptDecoder, optionalTaxRecord.getAmount()) +
                " (" +
                optionalTaxRecord.getPercentage().toPlainString() +
                "%)");
        }
        html.append(coreData.addCell(displayUtcTime(receiptDecoder.getPayeeTimeStamp()))
            .addCell(receiptDecoder.getPayeeReferenceId(), HtmlTable.RIGHT_ALIGN)
            .render());

        // Order Data
        HtmlTable orderData = new HtmlTable("Order Data");
        orderData.addHeader("Description");
        if (receiptDecoder.getOptionalLineItemElements()
            .contains(ReceiptLineItem.OptionalElements.SKU)) {
            orderData.addHeader("SKU");
        }
        orderData.addHeader("Quantity");
        if (receiptDecoder.getOptionalLineItemElements()
            .contains(ReceiptLineItem.OptionalElements.PRICE)) {
            orderData.addHeader("Price");
        }
        if (receiptDecoder.getOptionalLineItemElements()
            .contains(ReceiptLineItem.OptionalElements.SUBTOTAL)) {
            orderData.addHeader("Subtotal");
        }
        if (receiptDecoder.getOptionalLineItemElements()
            .contains(ReceiptLineItem.OptionalElements.DISCOUNT)) {
            orderData.addHeader("Discount");
        }
        for (ReceiptLineItem lineItem : receiptDecoder.getLineItems()) {
            String quantity = lineItem.getQuantity().toPlainString();
            if (lineItem.getOptionalUnit() != null) {
                quantity += " " + lineItem.getOptionalUnit();
            }
            orderData.addCell(showLines(lineItem.getDescription()));
            if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.SKU)) {
                orderData.addCell(optional(lineItem.getOptionalSku()));
            }
            orderData.addCell(quantity, HtmlTable.RIGHT_ALIGN);
            if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.PRICE)) {
                BigDecimal price = lineItem.getOptionalPrice();
                String priceText = "";
                if (price != null) {
                    priceText = showMoney(receiptDecoder, price);
                    if (lineItem.getOptionalUnit() != null) {
                        priceText += "/" + lineItem.getOptionalUnit();
                    }
                }
                orderData.addCell(priceText, HtmlTable.RIGHT_ALIGN);
            }
            if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.SUBTOTAL)) {
                BigDecimal subtotal = lineItem.getOptionalSubtotal();
                orderData.addCell(subtotal == null ? "" : showMoney(receiptDecoder, subtotal),
                    HtmlTable.RIGHT_ALIGN);
            }
            if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.DISCOUNT)) {
                BigDecimal discount = lineItem.getOptionalDiscount();
                orderData.addCell(discount == null ? "" : showMoney(receiptDecoder, discount),
                    HtmlTable.RIGHT_ALIGN + ";color:red");
            }
        }
        html.append(orderData.render());

        // Optional Shipping Record
        ReceiptShippingRecord optionalShippingRecord = receiptDecoder.getOptionalShippingRecord();
        if (optionalShippingRecord != null) {
            html.append(new HtmlTable("Shipping")
                    .addHeader("Description")
                    .addHeader("Cost")
                    .addCell(showLines(optionalShippingRecord.getDescription()))
                    .addCell(showMoney(receiptDecoder, optionalShippingRecord.getAmount()),
                             HtmlTable.RIGHT_ALIGN)
                    .render());
        }

        // Optional Barcode
        if (receiptDecoder.getOptionalBarcode() != null) {
   //         html.append(printBarcode(receiptDecoder.getOptionalBarcode()));
        }

        // Optional Free Text
        if (receiptDecoder.getOptionalFreeText() != null) {
            html.append("<table style='margin:1.5em auto 0 auto'><tr><td>")
                .append(showLines(receiptDecoder.getOptionalFreeText()))
                .append("</td></tr></table>");
        }

        // Payment Details
        html.append(new HtmlTable("Payment Details")
            .addHeader("Provider Name")
            .addHeader("Account Type")
            .addHeader("Account Id")
            .addHeader("Transaction Id")
            .addHeader("Time Stamp")
            .addHeader("Request Id")
            .addCell(receiptDecoder.getProviderCommonName())
            .addCell(receiptDecoder.getPaymentMethodName())
            .addCell(receiptDecoder.getOptionalAccountReference())
            .addCell(receiptDecoder.getProviderReferenceId())
            .addCell(displayUtcTime(receiptDecoder.getProviderTimeStamp()))
            .addCell(receiptDecoder.getPayeeRequestId())
            .render());
    }
}
