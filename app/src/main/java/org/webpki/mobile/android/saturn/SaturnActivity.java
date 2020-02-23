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

import android.content.Intent;
import android.content.res.Configuration;

import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;

import android.view.Gravity;
import android.view.View;

import android.view.inputmethod.InputMethodManager;

import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.webkit.WebViewAssetLoader;

import org.webpki.mobile.android.R;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.math.BigDecimal;

import java.security.PublicKey;

import java.util.ArrayList;
import java.util.HashMap;

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

import org.webpki.mobile.android.application.ThemeHolder;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.mobile.android.saturn.common.AuthorizationData;
import org.webpki.mobile.android.saturn.common.UserResponseItem;
import org.webpki.mobile.android.saturn.common.WalletRequestDecoder;

import org.webpki.mobile.android.sks.HardwareKeyStore;

import org.webpki.net.MobileProxyParameters;

import org.webpki.sks.KeyProtectionInfo;
import org.webpki.sks.PassphraseFormat;
import org.webpki.sks.SKSException;

import org.webpki.util.ArrayUtil;
import org.webpki.util.HTMLEncoder;


public class SaturnActivity extends BaseProxyActivity {

    public static final String SATURN = "Saturn";

    static final String SATURN_SETTINGS    = "satset";
    static final String LAST_KEY_ID_JSON   = "lastKey";

    static final String BACKGROUND_WH = "#f2f2ff";
    static final String BORDER_WH     = "#8080ff";

    static final String HEADER_FONT_SIZE   = "14pt";

    static final String HTML_HEADER_WHITE = "<html><head><style type='text/css'>\n" +
          "body {margin:0;font-size:12pt;color:#000000;font-family:Roboto;background-color:white}\n" +
          "td.label {text-align:right;padding:3pt 3pt 3pt 0pt}\n" +
          "td.field {min-width:11em;padding:3pt 6pt 3pt 6pt;border-width:1px;" +
          "border-style:solid;border-color:" + BORDER_WH + ";background-color:" + BACKGROUND_WH + ";overflow:hidden;" +
          "white-space:nowrap;box-sizing:border-box}\n" +
          "div.balance {display:inline-block;padding:2pt 5pt;border-width:1px;" +
          "border-style:solid;border-color:" + BORDER_WH + ";border-radius:5pt;background-color:" + BACKGROUND_WH + "}\n" +
          "div.header {font-size:" + HEADER_FONT_SIZE + ";visibility:hidden;position:absolute;width:100%;text-align:center}\n" +
          "div.message {visibility:hidden;position:absolute;box-shadow:3pt 3pt 3pt lightgrey;" +
          "border-width:1px;border-color:grey;border-style:solid;border-radius:10pt;" +
          "left:10pt;right:10pt;background-color:#ffffea;color:black;padding:15pt 10pt}" +
          "span.pinfix {color:" + BACKGROUND_WH + "}\n" +
          "span.money {font-weight:500;letter-spacing:1pt}\n" +
          "span.marquee {color:brown;display:inline-block;position:relative;top:1pt;white-space:nowrap;animation-name:rollingtext;" +
          "animation-duration:10s;animation-timing-function:linear;" +
          "animation-iteration-count:infinite;font-size:10pt}\n" +
          "@keyframes rollingtext {0% {opacity:1;text-indent:0em} 33% {opacity:1;text-indent:0em} " +
          "75% {opacity:1;text-indent:-30em} 76% {opacity:0;text-indent:-30em} 77% {opacity:0;text-indent:15em} " +
          "78% {opacity:1;text-indent:15em} 100% {opacity:1;text-indent:0em}}\n" +
          "</style>\n" +
          "<script>\n" +
          "'use strict';\n" +
          "function positionElements() {\n";

    static final String HTML_HEADER_SPACE = "<html><head><style type='text/css'>\n" +
          "body {margin:0;font-size:12pt;color:white;font-family:Roboto;" +
          "background:linear-gradient(to bottom right, #162c44, #6d7a8e, #162c44);background-attachment:fixed}\n" +
          "td.label {font-weight:500;text-align:right;padding:3pt 3pt 3pt 0pt}\n" +
          "td.field {font-weight:500;min-width:11em;padding:3pt 6pt 3pt 6pt;border-width:1pt;" +
          "border-style:solid;border-color:#b0b0b0;background-color:black;overflow:hidden;" +
          "white-space:nowrap;box-sizing:border-box}\n" +
          "div.balance {font-weight:500;display:inline-block;padding:2pt 5pt;border-width:1pt;" +
          "border-style:solid;border-color:#b0b0b0;border-radius:5pt;background-color:black}\n" +
          "div.header {font-size:" + HEADER_FONT_SIZE + ";visibility:hidden;position:absolute;width:100%;text-align:center}\n" +
          "div.message {visibility:hidden;position:absolute;box-shadow:0pt 0pt 8pt white;" +
          "border-width:1pt;border-color:#162c44;border-style:solid;border-radius:10pt;" +
          "left:10pt;right:10pt;background-color:white;color:black;padding:15pt 10pt}" +
          "span.pinfix {color:black}\n" +
          "span.money {font-weight:500;letter-spacing:1pt}\n" +
          "span.marquee {color:orange;display:inline-block;position:relative;top:1pt;white-space:nowrap;animation-name:rollingtext;" +
          "animation-duration:10s;animation-timing-function:linear;" +
          "animation-iteration-count:infinite;font-size:10pt}\n" +
          "@keyframes rollingtext {0% {opacity:1;text-indent:0em} 33% {opacity:1;text-indent:0em} " +
          "75% {opacity:1;text-indent:-30em} 76% {opacity:0;text-indent:-30em} 77% {opacity:0;text-indent:15em} " +
          "78% {opacity:1;text-indent:15em} 100% {opacity:1;text-indent:0em}}\n" +
          "</style>\n" +
          "<script>\n" +
          "'use strict';\n" +
          "function positionElements() {\n";

    String htmlBodyPrefix;

    boolean landscapeMode;

    WalletRequestDecoder walletRequest;

    enum FORM {SIMPLE, PAYMENTREQUEST}

    FORM currentForm = FORM.SIMPLE;

    int selectedCard;

    int lastKeyId;

    String pin = "";

    UserResponseItem[] challengeResults;

    byte[] privateMessageEncryptionKey;

    JSONObjectWriter authorizationData;

    boolean done;

    static boolean whiteTheme;

    WebView saturnView;
    int factor;
    DisplayMetrics displayMetrics;

    static class Account {
        String paymentMethod;
        String credentialId;
        String accountId;
        String authorityUrl;
        boolean cardFormatAccountId;
        byte[] cardImage;
        AsymSignatureAlgorithms signatureAlgorithm;
        int signatureKeyHandle;
        DataEncryptionAlgorithms dataEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
        PublicKey encryptionKey;
        String optionalKeyId;
        int optionalBalanceKeyHandle;

        BigDecimal tempBalanceFix;

        Account(// The core...
                String paymentMethod,
                String credentialId,
                String accountId,
                String authorityUrl,
                // Card visuals
                boolean cardFormatAccountId,
                byte[] cardImage,
                // Signature
                int signatureKeyHandle,
                AsymSignatureAlgorithms signatureAlgorithm,
                // Encryption
                KeyEncryptionAlgorithms keyEncryptionAlgorithm,
                DataEncryptionAlgorithms dataEncryptionAlgorithm,
                PublicKey encryptionKey,
                String optionalKeyId,
                BigDecimal tempBalanceFix) {
            this.paymentMethod = paymentMethod;
            this.credentialId = credentialId;
            this.accountId = accountId;
            this.authorityUrl = authorityUrl;
            this.cardFormatAccountId = cardFormatAccountId;
            this.cardImage = cardImage;
            this.signatureKeyHandle = signatureKeyHandle;
            this.signatureAlgorithm = signatureAlgorithm;
            this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
            this.dataEncryptionAlgorithm = dataEncryptionAlgorithm;
            this.encryptionKey = encryptionKey;
            this.optionalKeyId = optionalKeyId;

            this.tempBalanceFix = tempBalanceFix;
        }
    }

    ArrayList<Account> accountCollection = new ArrayList<Account>();

    byte[] currentHtml;

    static final HashMap<String,String> noCache = new HashMap();

    static {
        noCache.put("Cache-Control", "no-store");
    }

    final WebViewAssetLoader webLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/card/", new WebViewAssetLoader.PathHandler() {
                @Nullable
                @Override
                public WebResourceResponse handle(@NonNull String cardIndex) {
                    Log.i("RRR", cardIndex + " old=" + selectedCard);
                    selectedCard = Integer.parseInt(cardIndex);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saturnView.evaluateJavascript(
                                    "document.getElementById('balance').innerHTML = '" +
                                            getBalance(getSelectedCard()) + "';", null);                        }
                    });
                    return new WebResourceResponse("image/svg+xml",
                                                   "utf-8",
                                                   200,
                                                   "OK",
                                                   noCache,
                                                   new ByteArrayInputStream(
                                                           getSelectedCard().cardImage));
                }
            })
            .addPathHandler("/main/", new WebViewAssetLoader.PathHandler() {
                @Nullable
                @Override
                public WebResourceResponse handle(@NonNull String path) {
                    return new WebResourceResponse("text/html",
                                                   "utf-8",
                                                   new ByteArrayInputStream(currentHtml));
                }
            })
            .build();

    void loadHtml(final String positionScript, final String body) {
        try {
            currentHtml = new StringBuilder(
                    whiteTheme ? HTML_HEADER_WHITE : HTML_HEADER_SPACE)
                    .append(positionScript)
                    .append(htmlBodyPrefix)
                    .append(body)
                    .append("</body></html>").toString().getBytes("utf-8");
         } catch (Exception e) {
            Log.e("HTM", e.getMessage());
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                saturnView.loadUrl("https://appassets.androidplatform.net/main/");
            }
        });
    }

    @JavascriptInterface
    public void launchLink(String url) {
        if (w3cPayInvoked()) {
            launchBrowser(url);
        } else {
            super.launchBrowser(url);
        }
    }

    @Override
    public void launchBrowser(String url) {
        if (qrCodeInvoked()) {
            new QRCancel(this, url).execute();
        } else if (w3cPayInvoked()) {
            Intent result = new Intent();
            Bundle extras = new Bundle();
            extras.putString("methodName", "https://mobilepki.org/w3cpay/method");
            extras.putString("details", "{\"" + MobileProxyParameters.W3CPAY_GOTO_URL + "\": \"" + url + "\"}");
            result.putExtras(extras);
            setResult(RESULT_OK, result);
            closeProxy();
            finish();
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

        case PAYMENTREQUEST:
            try {
                showPaymentRequest();
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
                 "<tr><td style='padding:20pt;font-size:" + HEADER_FONT_SIZE + "'>" +
                 simpleHtml +
                 "</td></tr></table>");
    }

    public void messageDisplay(String js, String message) {
        currentForm = FORM.SIMPLE;
        loadHtml("var message = document.getElementById('message');\n" +
                 "message.style.top = ((Saturn.height() - message.offsetHeight) / 2) + 'px';\n" +
                 "message.style.visibility='visible';\n" +
                 js,
                 "<div id='message' class='message'>" +
                         message +
                 "</div>");
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
        saturnView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                return webLoader.shouldInterceptRequest(request.getUrl());
            }
        });
        displayMetrics = new DisplayMetrics();
        whiteTheme = ThemeHolder.isWhiteTheme(getBaseContext());
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        factor = (int)(displayMetrics.density * 100);
        landscapeMode = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        try {
            FileInputStream fis = openFileInput(SaturnActivity.SATURN_SETTINGS);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            JSONObjectReader settings = JSONParser.parse(buffer);
            lastKeyId = settings.getInt(LAST_KEY_ID_JSON);
        } catch (IOException e) {
            lastKeyId = -1;
        }
        try {

            htmlBodyPrefix = new StringBuilder("}\n" +
                                              "</script>" +
                                              "</head><body onload='positionElements()'>" +
                                              "<svg style='height:")
                .append((int)((Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels)  * 5) / factor))
                .append("px'")
                .append(new String(ArrayUtil.getByteArrayFromInputStream(getResources()
                            .openRawResource(whiteTheme ?
                                 R.raw.saturnlogo_white : R.raw.saturnlogo_space)),"utf-8").substring(4))
                    .toString();
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

    String getBalance(Account account) {
        try {
            return walletRequest.paymentRequest.getCurrency()
                    .amountToDisplayString(account.tempBalanceFix, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String htmlOneCard(int width) {
        int arrowWidth = (width * 4) / factor;
        return new StringBuilder("<table id='card' style='visibility:hidden;position:absolute'>"+
                    "<tr><td id='leftArrow' style='visibility:hidden'>" +
                    "<svg style='width:")
            .append(arrowWidth)
            .append("px' viewBox='0 0 110 320' xmlns='http://www.w3.org/2000/svg'>" +
                    "<path d='M100 20 L100 300 L10 160 Z' fill='none' stroke='")
            .append(whiteTheme ? "black" : "white")
            .append("' stroke-width='10'/>" +
                    "</svg></td></td><td><svg style='width:")
            .append((width * 100) / factor)
            .append("px' ")
            .append(whiteTheme ?
                    "viewBox='0 0 318 190' xmlns='http://www.w3.org/2000/svg'>" +
                    "<defs>" +
                    " <clipPath id='cardClip'>" +
                    "  <rect rx='15' ry='15' height='180' width='300' y='0' x='0'/>" +
                    " </clipPath>" +
                    " <filter id='dropShaddow'>" +
                    "  <feGaussianBlur stdDeviation='2.4'/>" +
                    " </filter>" +
                    " <linearGradient y1='0' x1='0' y2='1' x2='1' id='innerCardBorder'>" +
                    "  <stop offset='0' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "  <stop offset='0.48' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "  <stop offset='0.52' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    "  <stop offset='1' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    " </linearGradient>" +
                    " <linearGradient y1='0' x1='0' y2='1' x2='1' id='outerCardBorder'>" +
                    "  <stop offset='0' stop-color='#b0b0b0'/>" +
                    "  <stop offset='0.48' stop-color='#b0b0b0'/>" +
                    "  <stop offset='0.52' stop-color='#808080'/>" +
                    "  <stop offset='1' stop-color='#808080'/>" +
                    " </linearGradient>" +
                    "</defs>" +
                    "<rect filter='url(#dropShaddow)' rx='16' ry='16' " +
                    "height='182' width='302' y='4' x='12' fill='#c0c0c0'/>" +
                    "<svg x='9' y='1' clip-path='url(#cardClip)'>"
                                       :
                    "viewBox='0 0 318 196' xmlns='http://www.w3.org/2000/svg'>" +
                    "<defs>" +
                    " <clipPath id='cardClip'>" +
                    "  <rect rx='15' ry='15' height='178' width='298' y='0' x='0'/>" +
                    " </clipPath>" +
                    " <linearGradient y1='0' x1='0' y2='1' x2='1' id='innerCardBorder'>" +
                    "  <stop offset='0' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "  <stop offset='0.48' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                    "  <stop offset='0.52' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    "  <stop offset='1' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    " </linearGradient>" +
                    " <filter id='dropShaddow'>" +
                    "  <feGaussianBlur stdDeviation='3.5'/>" +
                    " </filter>" +
                    "</defs>" +
                    "<rect filter='url(#dropShaddow)' rx='16' ry='16' " +
                    "height='181' width='303' y='8.5' x='7.5' fill='white'/>" +
                    "<svg x='10' y='10' clip-path='url(#cardClip)'>")
            .append("<image id='cardImage' width='300' height='180' " +
                    "style='opacity:1;cursor:pointer' href='/card/")
            .append(selectedCard)
            .append("'/></svg>")

            .append(whiteTheme ?
                    "<rect fill='none' x='10' y='2' width='298' height='178' " +
                    "rx='14.7' ry='14.7' stroke='url(#innerCardBorder)' stroke-width='2.7'/>" +
                    "<rect fill='none' x='8.5' y='0.5' width='301' height='181' " +
                    "rx='16' ry='16' stroke='url(#outerCardBorder)'/>"
                             :
                    "<rect fill='none' x='12' y='12' width='294' height='174' " +
                    "rx='15' ry='15' stroke='url(#innerCardBorder)' stroke-width='2'/>" +
                    "<rect fill='none' x='9.5' y='9.5' width='299' height='179' " +
                    "rx='17' ry='17' stroke='#e0e0e0'/>" +
                    "<rect fill='none' x='10.5' y='10.5' width='297' height='177' " +
                    "rx='16' ry='16' stroke='#162c44'/>")

            .append("</svg></td><td id='rightArrow' style='visibility:hidden'>" +
                    "<svg style='width:")
            .append(arrowWidth)
            .append("px' viewBox='0 0 110 320' xmlns='http://www.w3.org/2000/svg'>" +
                    "<path d='M10 20 L10 300 L100 160 Z' fill='none' stroke='")
            .append(whiteTheme ? "black" : "white")
            .append("' stroke-width='10'/>" +
                    "</svg></td></tr><tr><td colspan='3' style='text-align:center'>" +
                    "<div class='balance' onClick=\"Saturn.toast('Not implemented in the demo...')\">" +
                    "Balance: <span id='balance' class='money'>0.00</span>" +
                    "</div></td></tr></table>").toString();
    }

    Account getSelectedCard() {
        return accountCollection.get(selectedCard);
    }

    void showPaymentRequest() throws IOException {
        currentForm = FORM.PAYMENTREQUEST;
        boolean numericPin = sks.getKeyProtectionInfo(
                getSelectedCard().signatureKeyHandle).getPinFormat() == PassphraseFormat.NUMERIC;
        int width = displayMetrics.widthPixels;
        StringBuilder js = new StringBuilder(
            "cardImage = document.getElementById('cardImage');\n" +
            "cardImage.addEventListener('touchstart', beginSwipe, false);\n" +
            "cardImage.addEventListener('touchmove', e => { e.preventDefault() }, false);\n" +
            "cardImage.addEventListener('touchend', endSwipe, false);\n" +
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
                    "var wGutter = Math.floor((Saturn.width() - kbd.offsetWidth - card.offsetWidth) / 3);\n" +
                    "card.style.right = wGutter + 'px';\n" +
                    "card.style.top = (Saturn.height() - card.offsetHeight)/3 + 'px';\n" +
                    "kbd.style.left = wGutter + 'px';\n" +
                    "var hGutter = Math.floor((Saturn.height() - kbd.offsetHeight - paydata.offsetHeight) / 3);\n" +
                    "var kbdTop = Math.floor(Saturn.height() - hGutter - kbd.offsetHeight);\n" +
                    "kbd.style.top = kbdTop + 'px';\n" +
                    "paydata.style.left = wGutter + 'px';\n" +
                    "paydata.style.top = hGutter + 'px';\n"+
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
            js.append(
                "card.style.left = ((Saturn.width() - card.offsetWidth) / 2) + 'px';\n" +
                "var pGutter = ((Saturn.width() - paydata.offsetWidth - payeelabel.offsetWidth) / 2);\n" +
                "if (pGutter < 10) pGutter = 10;\n" +
                "paydata.style.left = pGutter + 'px';\n");
            if (numericPin) {
                js.append(
                    "var kbdTop = Saturn.height() - Math.floor(kbd.offsetHeight * 1.20);\n" +
                    "kbd.style.top = kbdTop + 'px';\n" +
                    "kbd.style.left = ((Saturn.width() - kbd.offsetWidth) / 2) + 'px';\n" +
                    "var gutter = (kbdTop - card.offsetHeight - paydata.offsetHeight) / 7;\n" +
                    "card.style.top = gutter * 3 + 'px';\n" +
                    "paydata.style.top = (5 * gutter + card.offsetHeight) + 'px';\n" +
                    "kbd.style.visibility='visible';\n");
            } else {
                js.append(
                    "var gutter = Math.floor((Saturn.height() - card.offsetHeight - paydata.offsetHeight) / 8);\n" +
                    "card.style.top = (gutter * 3) + 'px';\n" +
                    "paydata.style.top = (gutter * 5 + card.offsetHeight) + 'px';\n");
            }
        }
        if (walletRequest.gasStationPayment) {
            js.append("document.getElementById('amountfield').innerHTML += " +
                    "\"<span class='marquee'><i>Reserved</i>, actual payment will match fuel quantity</span>\";\n");
        }
        js.append(
            "card.style.visibility='visible';\n" +
            "paydata.style.visibility='visible';\n" +
            "setArrows();\n" +
            "}\n" +
            "let swipeStartPosition = null;\n" +

            "function beginSwipe(e) { e.preventDefault(); swipeStartPosition = e.changedTouches[0].clientX };\n" +

            "function endSwipe(e) {\n" +
            "  if (swipeStartPosition || swipeStartPosition === 0) {\n" +
            "    let dx = e.changedTouches[0].clientX - swipeStartPosition;\n" +
            "    swipeStartPosition = null\n" +
            "    if (Math.abs(dx) > 30) {\n" +
            "      if (dx > 0 && cardIndex < numberOfAccountsMinus1) {\n" +
            "        cardIndex++;\n" +
            "      } else if (dx < 0 && cardIndex > 0) {\n" +
            "        cardIndex--;\n" +
            "      } else {\n" +
            "        return;\n" +
            "      }\n" +
            "      cardImage.setAttribute('href', '/card/' + cardIndex);\n" +
            "      setOpacity(0);\n" +
            "      setArrows();\n" +
            "    } else {\n" +
            "      Saturn.toast('Swipe to the left or right to change account/card');\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "function setOpacity(opacity) {\n" +
            "cardImage.style.opacity = opacity;\n" +
            "if (opacity < 0.99) {\n" +
            "opacity += 0.2;\n" +
             "setTimeout(function () {\n" +
             "setOpacity(opacity);\n" +
             "}, 50);\n" +
             "}\n" +
             "}\n" +
            "function setArrows() {\n" +
            "document.getElementById('leftArrow').style.visibility = " +
            "cardIndex == 0 ? 'hidden' : 'visible';\n" +
            "document.getElementById('rightArrow').style.visibility = " +
            "cardIndex == numberOfAccountsMinus1 ? 'hidden' : 'visible';\n" +
            "}\n" +
            "const numberOfAccountsMinus1 = ")
        .append(accountCollection.size() - 1)
        .append(
            ";\n" +
            "var cardIndex = ")
        .append(selectedCard)
        .append(
            ";\n" +
            "var cardImage = null;\n");
        if (numericPin) {
            js.append(
                "var pin = '" + HTMLEncoder.encode(pin) + "';\n" +
                "function showPin() {\n" +
                "var pwd = \"<span style='font-size:10pt;position:relative;top:-1pt'>\";\n" +
                "for (var i = 0; i < pin.length; i++) {\n" +
                "pwd += '\u25cf\u2007';\n" +
                "}\n" +
                "pinfield.innerHTML = pwd + \"</span><span class='pinfix'>K</span>\";\n" +
                "}\n" +
                "function addDigit(digit) {\n" +
                "if (pin.length < 16) {\n" +
                "pinfield.innerHTML = pin.length == 0 ? digit : pinfield.innerHTML.substring(0, pinfield.innerHTML.length - 29)  + digit;\n" +
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
        StringBuilder html = new StringBuilder(
            "<table id='paydata' style='visibility:hidden;position:absolute;z-index:5'>");
        if (!numericPin) {
            html.append("<form onsubmit='return validatePin()'>");
        }
        html.append(
            "<tr><td id='payeelabel' class='label'>Payee</td><td id='payeefield' class='field' onClick=\"Saturn.toast('Name of merchant')\">")
          .append(HTMLEncoder.encode(walletRequest.paymentRequest.getPayeeCommonName()))
          .append("</td></tr>" +
            "<tr><td colspan='2' style='height:5pt'></td></tr>" +
            "<tr><td class='label'>Amount</td><td id='amountfield' " +
            "class='field' onClick=\"Saturn.toast('Amount to pay')\"><span class='money'>")
          .append(walletRequest.paymentRequest.getCurrency().amountToDisplayString(walletRequest.paymentRequest.getAmount(), true))
          .append("</span>")
          .append(walletRequest.gasStationPayment ? "<br>\u200b" : "")
          .append("</td></tr>" +
            "<tr><td colspan='2' style='height:5pt'></td></tr>" +
            "<tr><td class='label'>PIN</td>");

        if (numericPin) {
            html.append("<td id='pinfield' class='field' " +
                        "onClick=\"Saturn.toast('Use the keyboard below...')\"></td></tr>" +
                        "</table>" +
                        "<div id='kbd' style='visibility:hidden;position:absolute;width:")
                .append(landscapeMode ? (width * 50) / factor : (width * 88) / factor)
                .append("px;height:")
                .append(landscapeMode ? (width * ((50 * 162) / 416)) / factor : (width * ((88 * 162) / 416)) / factor)
                .append("'>")
                .append(ThemeHolder.getKeyBoard())
                .append("</div>");
        } else {
            html.append("<td><input id='alphanum' style='font-size:inherit;width:100%' autofocus type='password' size='12' maxlength='16' value='")
                .append(HTMLEncoder.encode(pin))
                .append("'></td></tr>" +
                        "<tr><td></td><td style='padding-top:12pt;text-align:center'>" +
                        "<input type='submit' style='font-size:inherit' value='Validate'></td></tr>" +
                        "</form></table>");
        }

        html.append(htmlOneCard(landscapeMode ? (width * 4) / 11 : (width * 7) / 10));
        loadHtml(js.toString(), html.toString());
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
            ArrayList<UserResponseItem> temp = new ArrayList<UserResponseItem>();
            JSONArrayReader challengeArray = JSONParser.parse(json).getJSONArrayReader();
             do {
                 JSONObjectReader challengeObject = challengeArray.getObject();
                 String id = challengeObject.getProperties()[0];
                 String data = challengeObject.getString(id);
                 if (data.isEmpty()) {
                     toast("Please provide some data");
                     return false;
                 }
                 temp.add(new UserResponseItem(id, data));
            } while (challengeArray.hasMore());
            challengeResults = temp.toArray(new UserResponseItem[0]);
            hideSoftKeyBoard();
            showPaymentRequest();
            paymentEvent();
        } catch (Exception e) {
            unconditionalAbort("Challenge data read failure");
        }
        return false;
    }

    boolean pinBlockCheck() throws SKSException {
        if (sks.getKeyProtectionInfo(getSelectedCard().signatureKeyHandle).isPinBlocked()) {
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
            Account account = getSelectedCard();
            try {
                // User authorizations are always signed by a key that only needs to be
                // understood by the issuing Payment Provider (bank).
                UserResponseItem[] tempChallenge = challengeResults;
                challengeResults = null;
                // The key we use for decrypting private information from our bank
                privateMessageEncryptionKey =
                        CryptoRandom.generateRandom(account.dataEncryptionAlgorithm.getKeyLength());
                // The response
                authorizationData = AuthorizationData.encode(
                    walletRequest.paymentRequest,
                    getRequestingHost(),
                    account.paymentMethod,
                    account.credentialId,
                    account.accountId,
                    privateMessageEncryptionKey,
                    account.dataEncryptionAlgorithm,
                    tempChallenge,
                    account.signatureAlgorithm,
                    new AsymKeySignerInterface () {
                        @Override
                        public PublicKey getPublicKey() throws IOException {
                            return sks.getKeyAttributes(
                                    account.signatureKeyHandle).getCertificatePath()[0].getPublicKey();
                        }
                        @Override
                        public byte[] signData(byte[] data, AsymSignatureAlgorithms algorithm) throws IOException {
                            return sks.signHashedData(account.signatureKeyHandle,
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
                KeyProtectionInfo pi = sks.getKeyProtectionInfo(account.signatureKeyHandle);
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
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
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
            finish();
        } else if (currentForm == SaturnActivity.FORM.SIMPLE) {
            try {
                showPaymentRequest();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            conditionalAbort(null);
        }
    }

    @Override
    protected String getAbortString() {
        return "Do you want to abort the payment process?";
    }
}
