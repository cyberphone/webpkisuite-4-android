/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
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
package org.webpki.mobile.android.saturn;

import android.annotation.SuppressLint;

import android.content.Context;

import android.content.res.Configuration;

import android.os.Build;
import android.os.Bundle;

import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.View;

import android.view.inputmethod.InputMethodManager;

import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;

import android.webkit.WebView;
import android.widget.Toast;

import org.webpki.mobile.android.R;

import java.io.IOException;

import java.security.PublicKey;

import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymKeySignerInterface;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CryptoRandom;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;

import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.mobile.android.saturn.common.AuthorizationData;
import org.webpki.mobile.android.saturn.common.UserResponseItem;
import org.webpki.mobile.android.saturn.common.NonDirectPayments;
import org.webpki.mobile.android.saturn.common.PaymentRequest;
import org.webpki.mobile.android.saturn.common.WalletRequestDecoder;

import org.webpki.mobile.android.sks.HardwareKeyStore;

import org.webpki.sks.KeyProtectionInfo;
import org.webpki.sks.PassphraseFormat;
import org.webpki.sks.SKSException;

import org.webpki.util.ArrayUtil;
import org.webpki.util.HTMLEncoder;

public class SaturnActivity extends BaseProxyActivity {

    public static final String SATURN = "Saturn";

    static final String HTML_HEADER = "<html><head><style type='text/css'>\n" +
                                      "body {margin:0;font-size:12pt;color:#000000;font-family:Roboto;background-color:white}\n" +
                                      "td.label {text-align:right;padding:3pt 3pt 3pt 0pt}\n" +
                                      "div.balance {display:inline-block;padding:3pt 5pt;border-width:1px;" +
                                      "border-style:solid;border-color:#808080;border-radius:5pt}\n" +
                                      "td.field {min-width:11em;max-width:15em;padding:3pt 6pt 3pt 6pt;border-width:1px;" +
                                      "border-style:solid;border-color:#808080;background-color:#fafafa;overflow:hidden;" +
                                      "white-space:nowrap;box-sizing:border-box}\n" +
                                      "div.cardimage {border-style:groove;border-width:2px;border-color:#c0c0c0;border-radius:12pt;" +
                                      "box-shadow:3pt 3pt 3pt #d0d0d0;background-size:cover;background-repeat:no-repeat}\n" +
                                      "span.marquee {display:inline-block;position:relative;top:1pt;white-space:nowrap;animation-name:rollingtext;" +
                                      "animation-duration:10s;animation-timing-function:linear;" +
                                      "animation-iteration-count:infinite;font-size:10pt}\n" +
                                      "@keyframes rollingtext {0% {opacity:1;text-indent:0em} 33% {opacity:1;text-indent:0em} " +
                                      "75% {opacity:1;text-indent:-30em} 76% {opacity:0;text-indent:-30em} 77% {opacity:0;text-indent:15em} " +
                                      "78% {opacity:1;text-indent:15em} 100% {opacity:1;text-indent:0em}}\n" +
                                      "</style>\n" +
                                      "<script type='text/javascript'>\n" +
                                      "'use strict';\n" +
                                      "function positionElements() {\n";

    String htmlBodyPrefix;

    boolean landscapeMode;

    boolean oldAndroid;

    WalletRequestDecoder walletRequest;

    enum FORM {SIMPLE, COLLECTION, PAYMENTREQUEST}

    FORM currentForm = FORM.SIMPLE;

    Account selectedCard;

    String pin = "";

    UserResponseItem[] challengeResults;

    byte[] privateMessageEncryptionKey;

    String keyboardSvg;

    JSONObjectWriter authorizationData;

    boolean done;

    WebView saturnView;
    int factor;
    DisplayMetrics displayMetrics;

    static class Account {
        PaymentRequest paymentRequest;
        String paymentMethod;
        String accountId;
        String authorityUrl;
        boolean cardFormatAccountId;
        String cardSvgIcon;
        AsymSignatureAlgorithms signatureAlgorithm;
        int signatureKeyHandle;
        DataEncryptionAlgorithms dataEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
        PublicKey encryptionKey;
        String optionalKeyId;
        int optionalBalanceKeyHandle;

        Account(PaymentRequest paymentRequest,
                // The core...
                String paymentMethod,
                String accountId,
                String authorityUrl,
                // Card visuals
                boolean cardFormatAccountId,
                String cardSvgIcon,
                // Signature
                int signatureKeyHandle,
                AsymSignatureAlgorithms signatureAlgorithm,
                // Encryption
                KeyEncryptionAlgorithms keyEncryptionAlgorithm,
                DataEncryptionAlgorithms dataEncryptionAlgorithm,
                PublicKey encryptionKey,
                String optionalKeyId) {
            this.paymentRequest = paymentRequest;
            this.paymentMethod = paymentMethod;
            this.accountId = accountId;
            this.authorityUrl = authorityUrl;
            this.cardFormatAccountId = cardFormatAccountId;
            this.cardSvgIcon = cardSvgIcon;
            this.signatureKeyHandle = signatureKeyHandle;
            this.signatureAlgorithm = signatureAlgorithm;
            this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
            this.dataEncryptionAlgorithm = dataEncryptionAlgorithm;
            this.encryptionKey = encryptionKey;
            this.optionalKeyId = optionalKeyId;
        }
    }

    Vector<Account> accountCollection = new Vector<Account>();

    void loadHtml(final String positionScript, final String body) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                saturnView.loadUrl("about:blank");
                try {
                    String html = Base64.encodeToString(new StringBuffer(HTML_HEADER)
                            .append(positionScript)
                            .append(htmlBodyPrefix)
                            .append(body)
                            .append("</body></html>").toString().getBytes("utf-8"), Base64.NO_WRAP);
                    saturnView.loadData(html, "text/html; charset=utf-8", "base64");
                } catch (Exception e) {
                }
            }
        });
    }

    @Override
    public void launchBrowser(String url) {
        if (qrInvoked()) {
            new QRCancel(this, url).execute();
        } else {
            super.launchBrowser(url);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscapeMode = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            landscapeMode = false;
        } else {
            return;
        }
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        switch (currentForm) {
        case COLLECTION:
            showCardCollection();
            break;

        case PAYMENTREQUEST:
            try {
                ShowPaymentRequest();
            } catch (IOException e) {
            }
            break;

        default:
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   saturnView.reload();
                }
            });
        }
    }

    public void simpleDisplay(String simpleHtml) {
        currentForm = FORM.SIMPLE;
        loadHtml("var simple = document.getElementById('simple');\n" +
                 "simple.style.top = ((Saturn.height() - simple.offsetHeight) / 2) + 'px';\n" +
                 "simple.style.left = ((Saturn.width() - simple.offsetWidth) / 2) + 'px';\n" +
                 "simple.style.visibility='visible';\n",
                 "<table id='simple' style='visibility:hidden;position:absolute'>" +
                 "<tr><td style='padding:20pt'>" +
                 simpleHtml +
                 "</td></tr></table>");
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saturn);
        saturnView = (WebView) findViewById(R.id.saturnMain);
        WebSettings webSettings = saturnView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        saturnView.addJavascriptInterface (this, "Saturn");
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        factor = (int)(displayMetrics.density * 100);
        landscapeMode = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        oldAndroid = Build.VERSION.SDK_INT < 21;
        try {
            keyboardSvg = new String(ArrayUtil.getByteArrayFromInputStream(getResources()
                    .openRawResource(R.raw.pinkeyboard)), "utf-8");
            htmlBodyPrefix = new StringBuffer("}\n" +
                                              "</script>" +
                                              "</head><body onload=\"positionElements()\">" +
                                              "<img src='data:image/svg+xml;base64,")
                .append(Base64.encodeToString(ArrayUtil.getByteArrayFromInputStream(getResources()
                                                  .openRawResource(R.raw.saturnlogo)),
                                              Base64.NO_WRAP))
                .append("' style='height:")
                .append((int)((Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels)  * 5) / factor))
                .append("px'>").toString();
            simpleDisplay("Initializing...");
        } catch (Exception e) {
            unconditionalAbort("Saturn didn't initialize!");
            return;
        }

        showHeavyWork(PROGRESS_INITIALIZING);

        // Start of Saturn
        new SaturnProtocolInit(this).execute();
    }

    static String formatAccountId(Account card) {
        return card.cardFormatAccountId ?
            AuthorizationData.formatCardNumber(card.accountId)
                                        :
            card.accountId;
    }

    String htmlOneCard(Account account, int width, String card, String clickOption) {
        return new StringBuffer("<table id='")
            .append(card)
            .append("' style='visibility:hidden;position:absolute'><tr><td id='")
            .append(card)
            .append("image'><svg ")
            .append(clickOption)
            .append(" style=\"width:")
            .append((width * 100) / factor)
            .append("px\" viewBox=\"0 0 318 190\" xmlns=\"http://www.w3.org/2000/svg\">" +
                    "<defs>" +
                    " <clipPath id=\"cardClip\">" +
                    "  <rect rx=\"15\" ry=\"15\" height=\"180\" width=\"300\" y=\"0\" x=\"0\"/>" +
                    " </clipPath>" +
                    " <filter id=\"dropShaddow\">" +
                    "  <feGaussianBlur stdDeviation=\"2.4\"/>" +
                    " </filter>" +
                    " <linearGradient y1=\"0\" x1=\"0\" y2=\"1\" x2=\"1\" id=\"innerCardBorder\">" +
                    "  <stop offset=\"0\" stop-opacity=\"0.6\" stop-color=\"white\"/>" +
                    "  <stop offset=\"0.48\" stop-opacity=\"0.6\" stop-color=\"white\"/>" +
                    "  <stop offset=\"0.52\" stop-opacity=\"0.6\" stop-color=\"#b0b0b0\"/>" +
                    "  <stop offset=\"1\" stop-opacity=\"0.6\" stop-color=\"#b0b0b0\"/>" +
                    " </linearGradient>" +
                    " <linearGradient y1=\"0\" x1=\"0\" y2=\"1\" x2=\"1\" id=\"outerCardBorder\">" +
                    "  <stop offset=\"0\" stop-color=\"#b0b0b0\"/>" +
                    "  <stop offset=\"0.48\" stop-color=\"#b0b0b0\"/>" +
                    "  <stop offset=\"0.52\" stop-color=\"#808080\"/>" +
                    "  <stop offset=\"1\" stop-color=\"#808080\"/>" +
                    " </linearGradient>" +
                    "</defs>" +
                    "<rect filter=\"url(#dropShaddow)\" rx=\"16\" ry=\"16\" " +
                    "height=\"182\" width=\"302\" y=\"4\" x=\"12\" fill=\"#c0c0c0\"/>" +
                    "<svg x=\"9\" y=\"1\" clip-path=\"url(#cardClip)\"")
            .append(account.cardSvgIcon.substring(account.cardSvgIcon.indexOf('>')))
            .append(
                    "<rect x=\"10\" y=\"2\" " +
                    "width=\"298\" height=\"178\" " +
                    "rx=\"14.7\" ry=\"14.7\" " +
                    "fill=\"none\" " +
                    "stroke=\"url(#innerCardBorder)\" stroke-width=\"2.7\"/>" +
                    "<rect x=\"8.5\" y=\"0.5\" " +
                    "width=\"301\" height=\"181\" " +
                    "rx=\"16\" ry=\"16\" fill=\"none\" stroke=\"url(#outerCardBorder)\"/>" +
                    "</svg></td></tr><tr><td style='text-align:center'>" +
                    "<div class='balance'>")
            .append("Balance: \u20ac\u20092304")
            .append("</div></td></tr></table>").toString();
    }

    void ShowPaymentRequest() throws IOException {
        currentForm = FORM.PAYMENTREQUEST;
        boolean numericPin = sks.getKeyProtectionInfo(selectedCard.signatureKeyHandle).getPinFormat() == PassphraseFormat.NUMERIC;
        int width = displayMetrics.widthPixels;
        StringBuffer js = new StringBuffer(
            "var card = document.getElementById('card');\n" +
            "var paydata = document.getElementById('paydata');\n");
        if (numericPin) {
            js.append(
                "var pinfield = document.getElementById('pinfield');\n" +
                "pinfield.style.maxWidth = document.getElementById('amountfield').style.maxWidth = " +
                "document.getElementById('payeefield').offsetWidth + 'px';\n" +
                "var payeelabel = document.getElementById('payeelabel');\n" +
                "var kbd = document.getElementById('kbd');\n" +
                "showPin();\n");
        }
        if (landscapeMode) {
            if (numericPin) {
                js.append(
                    "var gutter = Math.floor((Saturn.width() - kbd.offsetWidth - card.offsetWidth) / 3);\n" +
                    "card.style.right = gutter + 'px';\n" +
                    "card.style.top = gutter + 'px';\n" +
                    "kbd.style.left = gutter + 'px';\n" +
                    "var kbdTop = Math.floor(Saturn.height() - gutter - kbd.offsetHeight);\n" +
                    "kbd.style.top = kbdTop + 'px';\n" +
                    "paydata.style.left = gutter + 'px';\n" +
                    "paydata.style.top = Math.floor(((document.getElementById('cardimage').offsetHeight - paydata.offsetHeight) / 2) + gutter) + 'px';\n"+
                    "kbd.style.visibility='visible';\n");
            } else {
                js.append(
                    "var gutter = Math.floor((Saturn.width() - paydata.offsetWidth - card.offsetWidth) / 3);\n" +
                    "card.style.right = gutter + 'px';\n" +
                    "paydata.style.left = gutter + 'px';\n" +
                    "card.style.top = Math.floor((Saturn.height() - card.offsetHeight) / 2) + 'px';\n" +
                    "paydata.style.top = Math.floor((Saturn.height() - paydata.offsetHeight) / 2) + 'px';\n");
            }
        } else {
            if (numericPin) {
                js.append(
                    "card.style.left = ((Saturn.width() - card.offsetWidth) / 2) + 'px';\n" +
                    "paydata.style.left = ((Saturn.width() - paydata.offsetWidth - payeelabel.offsetWidth) / 2) + 'px';\n" +
                    "var kbdTop = Saturn.height() - Math.floor(kbd.offsetHeight * 1.20);\n" +
                    "kbd.style.top = kbdTop + 'px';\n" +
                    "kbd.style.left = ((Saturn.width() - kbd.offsetWidth) / 2) + 'px';\n" +
                    "var gutter = (kbdTop - card.offsetHeight - paydata.offsetHeight) / 7;\n" +
                    "card.style.top = gutter * 3 + 'px';\n" +
                    "paydata.style.top = (5 * gutter + card.offsetHeight) + 'px';\n" +
                    "kbd.style.visibility='visible';\n");
            } else {
                js.append(
                    "card.style.left = ((Saturn.width() - card.offsetWidth) / 2) + 'px';\n" +
                    "paydata.style.left = ((Saturn.width() - paydata.offsetWidth - payeelabel.offsetWidth) / 2) + 'px';\n" +
                    "var gutter = Math.floor((Saturn.height() - card.offsetHeight - paydata.offsetHeight) / 8);\n" +
                    "card.style.top = (gutter * 3) + 'px';\n" +
                    "paydata.style.top = (gutter * 5 + card.offsetHeight) + 'px';\n");
            }
        }
        if (selectedCard.paymentRequest.getNonDirectPayment() == NonDirectPayments.GAS_STATION) {
            js.append("document.getElementById('amountfield').innerHTML += " +
                      "\"<br><span class='marquee'><i>Reserved</i>, actual payment will match fuel quantity</span>\";\n");
        }

        js.append(
            "card.style.visibility='visible';\n" +
            "paydata.style.visibility='visible';\n" +
            "}\n");
        if (numericPin) {
            js.append(
                "var pin = '" + HTMLEncoder.encode(pin) + "';\n" +
                "function showPin() {\n" +
                "if (pin.length == 0) {\n" +
                "pinfield.innerHTML = \"<span style='color:#a0a0a0'>Please enter PIN</span>\";\n" +
                "} else {\n"+
                "var pwd = \"<span style='font-size:10pt;position:relative;top:-1pt'>\";\n" +
                "for (var i = 0; i < pin.length; i++) {\n" +
                "pwd += '\u25cf\u2009';\n" +
                "}\n" +
                "pinfield.innerHTML = pwd + \"</span><span style='color:white'>K</span>\";\n" +
                "}\n" +
                "}\n" +
                "function addDigit(digit) {\n" +
                "if (pin.length < 16) {\n" +
                "pinfield.innerHTML = pin.length == 0 ? digit : pinfield.innerHTML.substring(0, pinfield.innerHTML.length - 34)  + digit;\n" +
                "pin += digit;\n" +
                "setTimeout(function() {\n" +
                "showPin();\n" +
                "}, 500);\n" +
                "} else {\n" +
                "Saturn.toast('PIN digit ignored');\n" +
                "}\n" +
                "}\n" +
                "function validatePin() {\n" +
                "if (pin.length == 0) {\n" +
                "Saturn.toast('Empty PIN - Ignored');\n" +
                "} else {\n" +
                "Saturn.performPayment(pin);\n" +
                "}\n" +
                "}\n" +
                "function deleteDigit() {\n" +
                "if (pin.length > 0) {\n" +
                "pin = pin.substring(0, pin.length - 1);\n" +
                "showPin();\n" +
                "}\n");
        } else {
            js.append(
                "function validatePin() {\n" +
                "var pin = alphanum.value;\n" +
                "if (pin.length == 0) {\n" +
                "Saturn.toast('Empty PIN - Ignored');\n" +
                "} else {\n" +
                "Saturn.performPayment(pin);\n" +
                "}\n" +
                "return false;\n");
        }
        StringBuffer html = new StringBuffer(
            "<table id='paydata' style='visibility:hidden;position:absolute'>");
        if (!numericPin) {
            html.append("<form onsubmit=\"return validatePin()\">");
        }
        html.append(
            "<tr><td id='payeelabel' class='label'>Payee</td><td id='payeefield' class='field' onClick=\"Saturn.toast('Name of merchant')\">")
          .append(HTMLEncoder.encode(selectedCard.paymentRequest.getPayee().getCommonName()))
          .append("</td></tr>" +
            "<tr><td colspan='2' style='height:5pt'></td></tr>" +
            "<tr><td class='label'>Amount</td><td id='amountfield' class='field' onClick=\"Saturn.toast('Amount to pay')\">")
          .append(selectedCard.paymentRequest.getCurrency().amountToDisplayString(selectedCard.paymentRequest.getAmount(), true))
          .append("</td></tr>" +
            "<tr><td colspan='2' style='height:5pt'></td></tr>" +
            "<tr><td class='label'>PIN</td>");

        if (numericPin) {
            html.append("<td id='pinfield' class='field' style='background-color:white;border-color:#0000ff' " +
                        "onClick=\"Saturn.toast('Use the keyboard below...')\"></td></tr>" +
                        "</table>" +
                        "<div id='kbd' style='visibility:hidden;position:absolute;width:")
                .append(landscapeMode ? (width * 50) / factor : (width * 88) / factor)
                .append("px;height:")
                .append(landscapeMode ? (width * ((50 * 162) / 416)) / factor : (width * ((88 * 162) / 416)) / factor)
                .append("'>")
                .append(keyboardSvg)
                .append("</div>");
        } else {
            html.append("<td><input id='alphanum' style='font-size:inherit;width:100%' autofocus type='password' size='12' maxlength='16' value='")
                .append(HTMLEncoder.encode(pin))
                .append("'></td></tr>" +
                        "<tr><td></td><td style='padding-top:12pt;text-align:center'>" +
                        "<input type='submit' style='font-size:inherit' value='Validate'></td></tr>" +
                        "</form></table>");
        }

        html.append(htmlOneCard(selectedCard, landscapeMode ? (width * 4) / 11 : (width * 4) / 5, "card", " onClick=\"Saturn.toast('The selected card')\""));
        loadHtml(js.toString(), html.toString());
    }

    void showCardCollection() {
        currentForm = FORM.COLLECTION;
        StringBuffer js = new StringBuffer("var header = document.getElementById('header');\n");
        StringBuffer html = 
            new StringBuffer("<div id='header' style='visibility:hidden;position:absolute;width:100%;text-align:center'>Select Payment Card</div>");
        int width = displayMetrics.widthPixels;
        int index = 0;
        for (SaturnActivity.Account account : accountCollection) {
            String card = "card" + String.valueOf(index);
            js.append("var " + card + " = document.getElementById('" + card + "');\n");
            if (index == 0) {
                js.append("var next = ")
                  .append(landscapeMode ? 
                          "(Saturn.height() - " + card + ".offsetHeight) / 2;\n" 
                                        :
                          "(Saturn.height() - Math.floor(" + card + ".offsetHeight * 2.3)) / 2;\n");
                js.append("header.style.top = (next - header.offsetHeight) / 2 + 'px';\n");
            }
            js.append(card + ".style.top = next;\n");
            if (landscapeMode) {
                double left = 1.0 / 11;
                if (index % 2 == 1) {
                    js.append("next += Math.floor(" + card + ".offsetHeight * 1.3);\n");
                    left = 6.0 / 11;
                }
                js.append(card + ".style.left = Math.floor(Saturn.width() * " + String.valueOf(left) + ") + 'px';\n");
            } else {
                js.append(card + ".style.left = ((Saturn.width() - " + card + ".offsetWidth) / 2) + 'px';\n" +
                          "next += Math.floor(" + card + ".offsetHeight * 1.3);\n");
            }
            js.append(card + ".style.visibility = 'visible';\n");
            html.append(htmlOneCard(account,
                                    landscapeMode ? (width * 4) / 11 : (width * 4) / 5,
                                    card,
                                    " onClick=\"Saturn.selectCard('" + (index++) + "')\""));
        }
        js.append("header.style.visibility='visible';\n");
        loadHtml(js.toString(), html.toString());
    }

    @JavascriptInterface
    public void selectCard(String index) throws IOException {
        pin = "";
        selectedCard = accountCollection.elementAt(Integer.parseInt(index));
        ShowPaymentRequest();
    }

    public void hideSoftKeyBoard() {
        // Check if no view has focus:
        View view = getCurrentFocus();
        if (view != null) {  
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @JavascriptInterface
    public boolean getChallengeJSON(String json) {
        try {
            Vector<UserResponseItem> temp = new Vector<UserResponseItem>();
            JSONArrayReader challengeArray = JSONParser.parse(json).getJSONArrayReader();
             do {
                 JSONObjectReader challengeObject = challengeArray.getObject();
                 String id = challengeObject.getProperties()[0];
                 temp.add(new UserResponseItem(id, challengeObject.getString(id)));
            } while (challengeArray.hasMore());
            challengeResults = temp.toArray(new UserResponseItem[0]);
            hideSoftKeyBoard();
            ShowPaymentRequest();
            paymentEvent();
        } catch (Exception e) {
            unconditionalAbort("Challenge data read failure");
        }
        return false;
    }

    boolean pinBlockCheck() throws SKSException {
        if (sks.getKeyProtectionInfo(selectedCard.signatureKeyHandle).isPinBlocked()) {
            unconditionalAbort("Card blocked due to previous PIN errors!");
            return true;
        }
        return false;
    }

    boolean userAuthorizationSucceeded() {
        try {
            if (pinBlockCheck()) {
                return false;
            }
            try {
                // User authorizations are always signed by a key that only needs to be
                // understood by the issuing Payment Provider (bank).
                UserResponseItem[] tempChallenge = challengeResults;
                challengeResults = null;
                // The key we use for decrypting private information from our bank
                privateMessageEncryptionKey = CryptoRandom.generateRandom(selectedCard.dataEncryptionAlgorithm.getKeyLength());
                // The response
                authorizationData = AuthorizationData.encode(
                    selectedCard.paymentRequest,
                    getRequestingHost(),
                    selectedCard.paymentMethod,
                    selectedCard.accountId,
                        privateMessageEncryptionKey,
                    selectedCard.dataEncryptionAlgorithm,
                    tempChallenge,
                    selectedCard.signatureAlgorithm,
                    new AsymKeySignerInterface () {
                        @Override
                        public PublicKey getPublicKey() throws IOException {
                            return sks.getKeyAttributes(selectedCard.signatureKeyHandle).getCertificatePath()[0].getPublicKey();
                        }
                        @Override
                        public byte[] signData(byte[] data, AsymSignatureAlgorithms algorithm) throws IOException {
                            return sks.signHashedData(selectedCard.signatureKeyHandle,
                                                      algorithm.getAlgorithmId (AlgorithmPreferences.SKS),
                                                      null,
                                                      pin.getBytes("UTF-8"),
                                                      algorithm.getDigestAlgorithm().digest(data));
                        }
                    });
                Log.i(SATURN, "Authorization before encryption:\n" + authorizationData);
                return true;
            } catch (SKSException e) {
                if (e.getError() != SKSException.ERROR_AUTHORIZATION) {
                    throw new Exception(e);
                }
                HardwareKeyStore.serializeSKS(SATURN, this);
            }
            if (!pinBlockCheck()) {
                Log.w(SATURN, "Incorrect PIN");
                KeyProtectionInfo pi = sks.getKeyProtectionInfo(selectedCard.signatureKeyHandle);
                showAlert("Incorrect PIN. There are " +
                          (pi.getPinRetryLimit() - pi.getPinErrorCount()) +
                          " tries left.");
            }
            return false;
        } catch (Exception e) {
            unconditionalAbort(e.getMessage());
            return false;  
        }
    }

    void paymentEvent() {
        if (userAuthorizationSucceeded()) {

            showHeavyWork(PROGRESS_PAYMENT);

            // Threaded payment process
            new SaturnProtocolPerform(this).execute();
        }
    }

    @JavascriptInterface
    public void performPayment(String pin) {
        this.pin = pin;
        hideSoftKeyBoard();
        paymentEvent();
    }

    @JavascriptInterface
    public void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void log(String message) {
        Log.i(SATURN, message);
    }
    
    @JavascriptInterface
    public int width() {
        return (saturnView.getWidth() * 100) / factor;
    }

    @JavascriptInterface
    public int height() {
        return (saturnView.getHeight() * 100) / factor;
    }

    @Override
    protected String getProtocolName() {
        return SATURN;
    }

    @Override
    protected void abortTearDown() {
    }

    @Override
    public void onBackPressed() {
        if (done) {
            closeProxy();
            finish ();
        } else {
            if (selectedCard == null) {
                conditionalAbort(null);
            } else if (accountCollection.size() == 1) {
                conditionalAbort(null);
                return;
            }
            selectedCard = null;
            showCardCollection();
        }
    }

    @Override
    protected String getAbortString() {
        return "Do you want to abort the payment process?";
    }
}
