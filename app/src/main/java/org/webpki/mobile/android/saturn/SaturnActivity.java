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
package org.webpki.mobile.android.saturn;

import android.annotation.SuppressLint;

import android.content.Context;

import android.content.Intent;
import android.content.res.Configuration;

import android.hardware.fingerprint.FingerprintManager;

import android.os.AsyncTask;
import android.os.Build;
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

import androidx.core.os.CancellationSignal;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import androidx.webkit.WebViewAssetLoader;

import org.webpki.json.JSONAsymKeySigner;
import org.webpki.mobile.android.R;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.security.PublicKey;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymKeySignerInterface;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CryptoRandom;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;

import org.webpki.mobile.android.application.ThemeHolder;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.mobile.android.saturn.common.AuthorizationDataEncoder;
import org.webpki.mobile.android.saturn.common.ClientPlatform;
import org.webpki.mobile.android.saturn.common.UserResponseItem;
import org.webpki.mobile.android.saturn.common.WalletRequestDecoder;

import org.webpki.mobile.android.sks.HardwareKeyStore;

import org.webpki.net.MobileProxyParameters;

import org.webpki.sks.BiometricProtection;
import org.webpki.sks.KeyProtectionInfo;
import org.webpki.sks.SKSException;

import org.webpki.util.ArrayUtil;
import org.webpki.util.HTMLEncoder;


public class SaturnActivity extends BaseProxyActivity {

    public static final String SATURN = "Saturn";

    static final String SATURN_SOFTWARE = "WebPKI Suite/Saturn";

    static final String SATURN_SETTINGS                   = "satset";

    int lastKeyId;
    static final String SETTINGS_LAST_KEY_ID_JSON         = "lastKey";

    boolean biometricPreferred = true;
    static final String SETTINGS_BIOMETRIC_PREFERRED_JSON = "bioPref";

    static final String BACKGROUND_WH = "#f2f2ff";
    static final String BORDER_WH     = "#8080ff";

    static final String HEADER_FONT_SIZE   = "14pt";

    static final String SPINNER_FIRST = "<svg style='height:1em;animation:spin 2s linear infinite' " +
        "viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'><g stroke='";
    static final String SPINNER_LAST = "' stroke-width='10' stroke-linecap='round'>" +
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
        "</svg>";

    static final String FAILED_ICON = "<svg style='height:1em' " +
        "viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'>" +
        "<circle cx='50' cy='50' fill='#ffff00' r='45' stroke='#ff0000' stroke-width='10'/>" +
        "<path d='m30,70c10,-8.5 30,-8.5 40,0' fill='none' " +
        "stroke='#000000' stroke-linecap='round' stroke-width='10'/>" +
        "<g fill='none' stroke='#007fff' stroke-linecap='round' stroke-width='15'>" +
        "<line x1='30' x2='30' y1='30' y2='40'/>" +
        "<line x1='70' x2='70' y1='30' y2='40'/>" +
        "</g></svg>";

    static final String HTML_HEADER_WHITE =
          "<!DOCTYPE html><html><head><title>Saturn</title><style type='text/css'>\n" +
          "body {margin:0;font-size:12pt;color:#000000;font-family:Roboto;background-color:white}\n" +
          "td.label {text-align:right;padding:3pt 3pt 3pt 0pt}\n" +
          "td.field {min-width:11em;padding:3pt 6pt 3pt 6pt;border-width:1px" +
              ";border-style:solid;border-color:" + BORDER_WH +
              ";background-color:" + BACKGROUND_WH + ";overflow:hidden;" +
          "white-space:nowrap;box-sizing:border-box}\n" +
          "div.balance {margin-top:4pt;padding:2pt 5pt;border-width:1px" +
              ";border-style:solid;border-color:" + BORDER_WH +
              ";border-radius:5pt;background-color:" + BACKGROUND_WH +
              ";display:flex;align-items:center}\n" +
          "div.header {font-size:" + HEADER_FONT_SIZE +
              ";visibility:hidden;position:absolute;width:100%;text-align:center}\n" +
          "div.message {visibility:hidden;position:absolute;box-shadow:3pt 3pt 3pt lightgrey" +
              ";border-width:1px;border-color:grey;border-style:solid;border-radius:10pt" +
              ";left:10pt;right:10pt;background-color:#ffffea;color:black;padding:15pt 10pt}" +
          "span.pinfix {color:" + BACKGROUND_WH + "}\n" +
          "span.money {font-weight:500;letter-spacing:1pt}\n" +
          "span.marquee {color:brown;display:inline-block;position:relative;top:1pt" +
              ";white-space:nowrap;font-size:10pt}\n" +
          "span.moneynote {color:darkblue}\n" +
          "@keyframes spin {100% {transform:rotate(360deg);}}\n" +
          "</style>\n" +
          "<script>\n" +
          "'use strict';\n" +
          "function positionElements() {\n";

    static final String HTML_HEADER_SPACE =
          "<!DOCTYPE html><html><head><title>Saturn</title><style type='text/css'>\n" +
          "body {margin:0;font-size:12pt;color:white;font-family:Roboto" +
               ";background:linear-gradient(to bottom right, #162c44, #6d7a8e, #162c44)" +
               ";background-attachment:fixed}\n" +
          "td.label {font-weight:500;text-align:right;padding:3pt 3pt 3pt 0pt}\n" +
          "td.field {font-weight:500;min-width:11em;padding:3pt 6pt 3pt 6pt;border-width:1pt" +
               ";border-style:solid;border-color:#b0b0b0;background-color:black;overflow:hidden;" +
          "white-space:nowrap;box-sizing:border-box}\n" +
          "div.balance {font-weight:500;margin-top:4pt;padding:2pt 5pt;border-width:1pt" +
              ";border-style:solid;border-color:#b0b0b0;border-radius:5pt" +
              ";background-color:black;display:flex;align-items:center}\n" +
          "div.header {font-size:" + HEADER_FONT_SIZE +
              ";visibility:hidden;position:absolute;width:100%;text-align:center}\n" +
          "div.message {visibility:hidden;position:absolute;box-shadow:0pt 0pt 8pt white" +
              ";border-width:1pt;border-color:#162c44;border-style:solid;border-radius:10pt" +
              ";left:10pt;right:10pt;background-color:white;color:black;padding:15pt 10pt}" +
          "span.pinfix {color:black}\n" +
          "span.money {font-weight:500;letter-spacing:1pt}\n" +
          "span.marquee {color:orange;display:inline-block;position:relative;top:1pt" +
              ";white-space:nowrap;font-size:10pt}\n" +
          "span.moneynote {color:lightblue}\n" +
          "@keyframes spin {100% {transform:rotate(360deg);}}\n" +
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

    String pin = "";

    UserResponseItem[] challengeResults;

    byte[] privateMessageEncryptionKey;

    JSONObjectWriter authorizationData;

    boolean done;

    static boolean whiteTheme;

    static String webPkiVersion;

    WebView saturnView;
    int factor;
    DisplayMetrics displayMetrics;

    ArrayList<Account> accountCollection = new ArrayList<>();

    byte[] currentHtml;

    static final HashMap<String,String> noCache = new HashMap<>();

    static {
        noCache.put("Cache-Control", "no-store");
    }

    void balanceRequestExecutor(int cardIndex) {
        new BalanceRequester(this, cardIndex).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    int backgroundBalanceRequests(int potentialCardIndex) {
        if (potentialCardIndex < 0 || potentialCardIndex >= accountCollection.size()) {
            return 0;
        }
        Account account = accountCollection.get(potentialCardIndex);
        if (account.optionalBalanceKeyHandle == null) {
            return 0;
        }
        if (account.balanceRequestIsRunning) {
            // Do not restart balance reequests and count those that are unfinished as running
            return account.balanceRequestIsReady ? 0 : 1;
        }
        balanceRequestExecutor(potentialCardIndex);
        return 1;
    }

    void setBalance(int cardIndex) {
        Account account = accountCollection.get(cardIndex);
        SaturnActivity saturnActivity = this;
        if (cardIndex == selectedCard) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String argument;
                    if (account.optionalBalanceKeyHandle == null) {
                        argument = "N/A";
                    } else if (account.balanceRequestIsRunning) {
                        if (account.balanceRequestIsReady) {
                            argument = account.balance == null ? FAILED_ICON : account.balance;
                        } else {
                            argument = SPINNER_FIRST + (whiteTheme ? "black" : "yellow") + SPINNER_LAST;
                        }
                    } else {
                        balanceRequestExecutor(cardIndex);
                        setBalance(cardIndex);
                        return;
                    }
                    // Permit up to three parallell requests to run at the same time
                    for (int i = 1, q = 0; i < accountCollection.size(); i++) {
                        if ((q += backgroundBalanceRequests(cardIndex + i)) >= 3) {
                            break;
                        }
                        if ((q += backgroundBalanceRequests(cardIndex - i)) >= 3) {
                            break;
                        }
                    }
                    saturnView.evaluateJavascript(
                        "document.getElementById('balance').innerHTML = \"Balance:&nbsp;" +
                            argument + "\";", null);
                }
            });
        }
    }

    final WebViewAssetLoader webLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/card/", new WebViewAssetLoader.PathHandler() {
                @Nullable
                @Override
                public WebResourceResponse handle(@NonNull String cardIndex) {
                    Log.i("RRR", cardIndex + " old=" + selectedCard);
                    setBalance(selectedCard = Integer.parseInt(cardIndex));
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

            FileOutputStream fos = openFileOutput("html.txt", Context.MODE_PRIVATE);
            fos.write(currentHtml);
            fos.close();

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
        loadHtml("let simple = document.getElementById('simple');\n" +
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
        loadHtml("let message = document.getElementById('message');\n" +
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
            lastKeyId = settings.getInt(SETTINGS_LAST_KEY_ID_JSON);
            biometricPreferred = settings.getBoolean(SETTINGS_BIOMETRIC_PREFERRED_JSON);
        } catch (IOException e) {
            lastKeyId = -1;
        }
        try {
            webPkiVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
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
                    "</svg></td><td><svg style='width:")
            .append((width * 100) / factor)
            .append("px;cursor:pointer;vertical-align:middle' " +
                    "viewBox='0 0 320 200' xmlns='http://www.w3.org/2000/svg'>" +
                    "<defs>" +
                    "<clipPath id='cardClip'>" +
                    "<rect rx='15' ry='15' width='300' height='180' x='0' y='0'/>" +
                    "</clipPath>")
            .append(whiteTheme ?
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
                    "<rect filter='url(#dropShaddow)' rx='16' ry='16' " +
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
                    "<rect filter='url(#dropShaddow)' rx='16' ry='16' " +
                    "width='305' height='184' x='7' y='8' fill='white'/>")
            .append("<svg x='10' y='10' clip-path='url(#cardClip)'>" +
                    "<image id='cardImage' width='300' height='180' href='/card/")
            .append(selectedCard)
            .append("'/></svg>")

            .append(whiteTheme ?
                    "<rect fill='none' x='11' y='11' width='298' height='178' " +
                    "rx='14.7' ry='14.7' stroke='url(#innerCardBorder)' stroke-width='2.7'/>" +
                    "<rect fill='none' x='9.5' y='9.5' width='301' height='181' " +
                    "rx='16' ry='16' stroke='url(#outerCardBorder)'/>"
                             :
                    "<rect fill='none' x='11' y='11' width='298' height='178' " +
                    "rx='14.75' ry='14.75' stroke='url(#innerCardBorder)' stroke-width='2'/>" +
                    "<rect fill='none' x='8.5' y='8.5' width='303' height='183' " +
                    "rx='17' ry='17' stroke='#e0e0e0'/>" +
                    "<rect fill='none' x='9.5' y='9.5' width='301' height='181' " +
                    "rx='16' ry='16' stroke='#162c44'/>")

            .append("</svg></td><td id='rightArrow' style='visibility:hidden'>" +
                    "<svg style='width:")
            .append(arrowWidth)
            .append("px' viewBox='0 0 110 320' xmlns='http://www.w3.org/2000/svg'>" +
                    "<path d='M10 20 L10 300 L100 160 Z' fill='none' stroke='")
            .append(whiteTheme ? "black" : "white")
            .append("' stroke-width='10'/>" +
                    "</svg></td></tr><tr><td colspan='3' style='text-align:center'>" +
                    "<div style='display:inline-block'>" +
                    "<div class='balance' id='balance' onClick=\"Saturn.balanceClicked()\">" +
                    "</div></div></td></tr></table>").toString();
    }

    Account getSelectedCard() {
        return accountCollection.get(selectedCard);
    }

    void showPaymentRequest() throws IOException {
        currentForm = FORM.PAYMENTREQUEST;
        int width = displayMetrics.widthPixels;
        StringBuilder js = new StringBuilder(
            "cardImage = document.getElementById('cardImage');\n" +
            "cardImage.addEventListener('touchstart', beginSwipe, false);\n" +
            "cardImage.addEventListener('touchmove', e => { e.preventDefault() }, false);\n" +
            "cardImage.addEventListener('touchend', endSwipe, false);\n" +
            "let card = document.getElementById('card');\n" +
            "paydata = document.getElementById('paydata');\n" +
            "pinfield = document.getElementById('pinfield');\n" +
            "pinRow = document.getElementById('pinRow');\n" +
            "pinfield.style.maxWidth = document.getElementById('amountfield').style.maxWidth = " +
            "document.getElementById('payeefield').offsetWidth + 'px';\n" +
            "let payeelabel = document.getElementById('payeelabel');\n" +
            "kbd = document.getElementById('kbd');\n" +
            "fpFrame = document.getElementById('fpFrame');\n" +
            "showPin();\n");
        if (landscapeMode) {
            js.append(
                "let wGutter = Math.floor((Saturn.width() - kbd.offsetWidth - card.offsetWidth) / 3);\n" +
                "card.style.left = wGutter / 2 + 'px';\n" +
                "card.style.top = (Saturn.height() - card.offsetHeight)/2 + 'px';\n" +
                "let kbdRight = wGutter * 1.5;\n" +
                "kbd.style.right = kbdRight + 'px';\n" +
                "let hGutter = Math.floor((Saturn.height() - kbd.offsetHeight - paydata.offsetHeight) / 3);\n" +
                "let kbdTop = Math.floor(Saturn.height() - hGutter - kbd.offsetHeight);\n" +
                "kbd.style.top = kbdTop + 'px';\n" +
                "let pGutter = ((kbd.offsetWidth - paydata.offsetWidth + payeelabel.offsetWidth " +
                    "- document.getElementById('fpField').offsetWidth) / 2) + wGutter * 1.5;\n" +
                "if (pGutter < 10) pGutter = 10;\n" +
                "paydata.style.right = pGutter + 'px';\n" +
                "paydataTop = hGutter;\n" +
                "let fpFrameTop = (Saturn.height() - pinRow.offsetHeight + paydata.offsetHeight " +
                    "+ paydataTop - fpFrame.offsetHeight) / 2;\n" +
                "fpFrame.style.top = fpFrameTop + 'px';\n" +
                "fpFrame.style.right = ((kbd.offsetWidth - fpFrame.offsetWidth " +
                    "- document.getElementById('pinSwitch').offsetWidth) / 2 " +
                    "+ kbdRight) + 'px';\n");
        } else {
            js.append(
                "card.style.left = ((Saturn.width() - card.offsetWidth) / 2) + 'px';\n" +
                "let pGutter = ((Saturn.width() - paydata.offsetWidth - payeelabel.offsetWidth " +
                    "+ document.getElementById('fpField').offsetWidth) / 2);\n" +
                "if (pGutter < 10) pGutter = 10;\n" +
                "paydata.style.left = pGutter + 'px';\n" +
                "let kbdTop = Saturn.height() - Math.floor(kbd.offsetHeight * 1.20);\n" +
                "kbd.style.top = kbdTop + 'px';\n" +
                "kbd.style.left = ((Saturn.width() - kbd.offsetWidth) / 2) + 'px';\n" +
                "let gutter = (kbdTop - card.offsetHeight - paydata.offsetHeight) / 7;\n" +
                "card.style.top = gutter * 3 + 'px';\n" +
                "paydataTop = 5 * gutter + card.offsetHeight;\n" +
                "let fpFrameTop = (Saturn.height() + paydata.offsetHeight " +
                    "+ paydataTop - fpFrame.offsetHeight) / 2 - pinRow.offsetHeight;\n" +
                "fpFrame.style.top = fpFrameTop + 'px';\n" +
                "fpFrame.style.left = ((Saturn.width() - fpFrame.offsetWidth " +
                    "+ document.getElementById('pinSwitch').offsetWidth) / 2) + 'px';\n");
        }
        js.append (walletRequest.getOptionalMarqueeCode())
          .append(
              "fpFrame.style.maxWidth = (fpFrame.offsetWidth + 20) + 'px';" +
              "card.style.visibility='visible';\n" +
              "paydata.style.visibility='visible';\n" +
              "setAccountSpecificDetails();\n" +
            "}\n" +

            "let swipeStartPosition = null;\n" +

            "function beginSwipe(e) { e.preventDefault(); " +
                  "swipeStartPosition = e.changedTouches[0].clientX };\n" +

            "function endSwipe(e) {\n" +
              "if (swipeStartPosition || swipeStartPosition === 0) {\n" +
                "let dx = e.changedTouches[0].clientX - swipeStartPosition;\n" +
                "swipeStartPosition = null\n" +
                "if (Math.abs(dx) > 30) {\n" +
                  "if (dx > 0 && cardIndex < numberOfAccountsMinus1) {\n" +
                    "cardIndex++;\n" +
                  "} else if (dx < 0 && cardIndex > 0) {\n" +
                    "cardIndex--;\n" +
                  "} else {\n" +
                    "return;\n" +
                  "}\n" +
                  "cardImage.setAttribute('href', '/card/' + cardIndex);\n" +
                  "setOpacity(0);\n" +
                  "setAccountSpecificDetails();\n" +
                "} else {\n" +
                  "Saturn.toast('Swipe to the left or right to change account/card', " +
                      Gravity.BOTTOM + ");\n" +
                "}\n" +
              "}\n" +
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

            "function selectAuthMode(biometricMode) {\n" +
              "Saturn.setAuthPreference(biometricMode);\n" +
              "setAccountSpecificDetails();\n" +
            "}\n" +

            "function setAccountSpecificDetails() {\n" +
              "document.getElementById('leftArrow').style.visibility = " +
              "cardIndex == 0 ? 'hidden' : 'visible';\n" +
              "document.getElementById('rightArrow').style.visibility = " +
              "cardIndex == numberOfAccountsMinus1 ? 'hidden' : 'visible';\n" +
              "let accountProtectionInfo = JSON.parse(Saturn.getAccountProtectionInfo(cardIndex));\n" +
              "if (accountProtectionInfo.useBiometrics) {\n" +
                "paydata.style.top = (paydataTop + pinRow.offsetHeight) + 'px';\n" +
                "document.getElementById('pinSwitch').style.visibility = " +
                    "accountProtectionInfo.supportsPinCodes ? 'visible' : 'hidden';\n" +
                "document.getElementById('fpField').style.visibility = 'hidden';\n" +
                "kbd.style.visibility = 'hidden';\n" +
                "fpFrame.style.visibility = 'visible';\n" +
                "pinRow.style.visibility = 'hidden';\n" +
              "} else {\n" +
                "paydata.style.top = paydataTop + 'px';\n" +
                "document.getElementById('fpField').style.visibility = " +
                    "accountProtectionInfo.supportsBiometric ? 'visible' : 'hidden';\n" +
                "document.getElementById('pinSwitch').style.visibility = 'hidden';\n" +
                "kbd.style.visibility = 'visible';\n" +
                "fpFrame.style.visibility = 'hidden';\n" +
                "pinRow.style.visibility = 'visible';\n" +
              "}\n" +
              "Saturn.setAuthenticationMode(accountProtectionInfo.useBiometrics);\n" +
            "}\n" +

            "const numberOfAccountsMinus1 = ")
        .append(accountCollection.size() - 1)
        .append(
            ";\n" +
            "let cardIndex = ")
        .append(selectedCard)
        .append(
            ";\n" +
            "let cardImage = null;\n" + 
            "let paydataTop;\n" +
            "let paydata;\n" +
            "let pinfield;\n" +
            "let pinRow;\n" +
            "let kbd;\n" +
            "let fpFrame;\n" +
            "let pin = '" + HTMLEncoder.encode(pin) + "';\n" +

            "function showPin() {\n" +
              "let pwd = \"<span style='font-size:10pt;position:relative;top:-1pt'>\";\n" +
              "for (let i = 0; i < pin.length; i++) {\n" +
                "pwd += '\u25cf\u2007';\n" +
              "}\n" +
              "pinfield.innerHTML = pwd + \"</span><span class='pinfix'>K</span>\";\n" +
            "}\n" +

            "function addDigit(digit) {\n" +
              "if (pin.length < 16) {\n" +
                "pinfield.innerHTML = pin.length == 0 ? digit :" +
                    " pinfield.innerHTML.substring(0, pinfield.innerHTML.length - 29)  + digit;\n" +
                "pin += digit;\n" +
                "setTimeout(function() {\n" +
                  "showPin();\n" +
                "}, 500);\n" +
              "} else {\n" +
                "Saturn.toast('PIN digit ignored', " + Gravity.CENTER_VERTICAL + ");\n" +
              "}\n" +
            "}\n" +

            "function validatePin() {\n" +
              "if (pin.length == 0) {\n" +
                "Saturn.toast('Empty PIN - Ignored', " + Gravity.CENTER_VERTICAL + ");\n" +
              "} else {\n" +
                "Saturn.performPinAuthorizedPayment(pin);\n" +
              "}\n" +
            "}\n" +

            "function deleteDigit() {\n" +
              "if (pin.length > 0) {\n" +
                "pin = pin.substring(0, pin.length - 1);\n" +
                "showPin();\n" +
              "}\n");

        StringBuilder html = new StringBuilder(
            "<table id='paydata' style='visibility:hidden;position:absolute;z-index:5'>" +
            "<tr><td id='payeelabel' class='label'>Payee</td><td id='payeefield' " +
                "class='field' onClick=\"Saturn.toast('Name of merchant', " +
                    Gravity.CENTER_VERTICAL + ")\">")
          .append(HTMLEncoder.encode(walletRequest.paymentRequest.getPayeeCommonName()))
          .append("</td><td></td></tr>" +
            "<tr><td colspan='3' style='height:5pt'></td></tr>" +
            "<tr><td class='label'>")
          .append(walletRequest.getAmountLabel())
          .append(
            "</td><td id='amountfield' " +
                "class='field' onClick=\"Saturn.toast('Amount to pay', " +
                    Gravity.CENTER_VERTICAL + ")\">")
          .append(walletRequest.getFormattedTotal())
          .append("</td><td></td></tr>" +
            "<tr><td colspan='3' style='height:5pt'></td></tr>" +
            "<tr id='pinRow'><td class='label'>PIN</td>" +
            "<td id='pinfield' class='field' " +
                "onClick=\"Saturn.toast('Use the keyboard below...', " +
                    Gravity.CENTER_VERTICAL + ")\"></td><td id='fpField'>")
          .append(ThemeHolder.getFingerPrintSymbol("1.8", "selectAuthMode(true)", "block", 7, 18))
          .append(
            "</td></tr>" +
            "</table>" +

            "<table id='fpFrame' style='visibility:hidden;position:absolute'>" +
            "<tr><td class='label'>Authorize&nbsp;Request</td><td></td></tr>" +
            "<tr><td style='text-align:center'>")
          .append(ThemeHolder.getFingerPrintSymbol("1",
                                                   "Saturn.toast('Use the fingerprint sensor', " +
                                                       Gravity.BOTTOM + ")",
                                                   "inline-block", 0, 40))
          .append(
            "</td><td id='pinSwitch' onclick=\"selectAuthMode(false)\">")
          .append(ThemeHolder.getFingerPrintSwitch())
          .append(
            "</td></tr>" +
            "<tr><td id='fpText' colspan='2' style='height:0px'></td></tr>" +
            "</table>" +

            "<div id='kbd' style='visibility:hidden;position:absolute;width:")
          .append(landscapeMode ? (width * 50) / factor : (width * 88) / factor)
          .append("px'>")
          .append(ThemeHolder.getKeyBoard())
          .append("</div>");
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
    public void setAuthPreference(boolean biometricPreferred) {
        this.biometricPreferred = biometricPreferred;
    }

    @JavascriptInterface
    public String getAccountProtectionInfo(int cardIndex) {
        // selectedCard doesn't work here due to threading issues
        boolean supportsBiometric =
            accountCollection.get(cardIndex).biometricProtection != BiometricProtection.NONE;
        boolean supportsPinCodes =
            accountCollection.get(cardIndex).biometricProtection != BiometricProtection.EXCLUSIVE;
        try {
            return new JSONObjectWriter()
                .setBoolean("supportsBiometric", supportsBiometric)
                .setBoolean("supportsPinCodes", supportsPinCodes)
                .setBoolean("useBiometrics", supportsBiometric && biometricPreferred)
                    .toString();
        } catch (IOException e) {
            return null;
        }
    }

    void setFingerPrintError(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                saturnView.evaluateJavascript(
                    "document.getElementById('fpText').innerHTML = '" + message + "';", null);
            }
        });
    }

    CancellationSignal fingerPrintAuthenticationInProgess;

    @JavascriptInterface
    public void setAuthenticationMode(boolean useBiometrics) {
        Log.e("KKK", "UB" + useBiometrics);
        if (fingerPrintAuthenticationInProgess != null) {
            fingerPrintAuthenticationInProgess.cancel();
            fingerPrintAuthenticationInProgess = null;
        }
        if (useBiometrics) {
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(this);
            if (fingerprintManager.hasEnrolledFingerprints()) {
                fingerPrintAuthenticationInProgess = new CancellationSignal();
                Log.e("KKK", "FP SETUP");
                fingerprintManager.authenticate(
                    null,
                    0,
                    fingerPrintAuthenticationInProgess,
                    new FingerprintManagerCompat.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationSucceeded(
                            FingerprintManagerCompat.AuthenticationResult result) {
                            Log.e("KKK", "SUCCESS");
                            performPayment();
                        }

                        @Override
                        public void onAuthenticationError(int errMsgId, CharSequence errString) {
                            Log.e("KKK", "X=" + errString.toString() + " Y=" + errMsgId);
                            if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                                setFingerPrintError("Failed to authenticate, try again later");
                            }
                        }
                    },
                    null);
            } else {
                Log.e("KKK", "NO FP");
                setFingerPrintError("You need to enable the fingerprint reader");
            }
        }
    }

    @JavascriptInterface
    public boolean getChallengeJSON(String json) {
        try {
            ArrayList<UserResponseItem> temp = new ArrayList<>();
            JSONArrayReader challengeArray = JSONParser.parse(json).getJSONArrayReader();
             do {
                 JSONObjectReader challengeObject = challengeArray.getObject();
                 String id = challengeObject.getProperties()[0];
                 String data = challengeObject.getString(id);
                 if (data.isEmpty()) {
                     toast("Please provide some data", Gravity.CENTER_VERTICAL);
                     return false;
                 }
                 temp.add(new UserResponseItem(id, data));
            } while (challengeArray.hasMore());
            challengeResults = temp.toArray(new UserResponseItem[0]);
            hideSoftKeyBoard();
            showPaymentRequest();
            performPayment();
        } catch (Exception e) {
            unconditionalAbort("Challenge data read failure");
        }
        return false;
    }

    boolean pinBlockCheck() throws SKSException {
        if (fingerPrintAuthenticationInProgess == null &&
            sks.getKeyProtectionInfo(getSelectedCard().signatureKeyHandle).isPinBlocked()) {
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
                authorizationData = AuthorizationDataEncoder.encode(
                    walletRequest.paymentRequest,
                    account.requestHashAlgorithm,
                    account.payeeAuthorityUrl,
                    getRequestingHost(),
                    account.paymentMethod,
                    account.credentialId,
                    account.accountId,
                    privateMessageEncryptionKey,
                    account.dataEncryptionAlgorithm,
                    tempChallenge,
                    new GregorianCalendar(),
                    SATURN_SOFTWARE,
                    webPkiVersion,
                    new ClientPlatform("Android", Build.VERSION.RELEASE, Build.MANUFACTURER),
                    new JSONAsymKeySigner(new AsymKeySignerInterface() {
                        @Override
                        public PublicKey getPublicKey() throws IOException {
                            return sks.getKeyAttributes(
                                    account.signatureKeyHandle).getCertificatePath()[0].getPublicKey();
                        }
                        @Override
                        public byte[] signData(byte[] data, AsymSignatureAlgorithms algorithm) throws IOException {
                            return sks.signHashedData(account.signatureKeyHandle,
                                                      algorithm.getAlgorithmId(AlgorithmPreferences.SKS),
                                                      null,
                                                      fingerPrintAuthenticationInProgess != null,
                                                      fingerPrintAuthenticationInProgess == null ?
                                                          pin.getBytes("UTF-8") : null,
                                                      algorithm.getDigestAlgorithm().digest(data));
                        }
                    }).setSignatureAlgorithm(account.signatureAlgorithm)
                      .setOutputPublicKeyInfo(account.optionalKeyId == null)
                      .setKeyId(account.optionalKeyId));
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

    void performPayment() {
        if (userAuthorizationSucceeded()) {

            showHeavyWork(PROGRESS_PAYMENT);

            // Threaded payment process
            new SaturnProtocolPerform(this).execute();
        }
    }

    @JavascriptInterface
    public void performPinAuthorizedPayment(String pin) {
        this.pin = pin;
        hideSoftKeyBoard();
        performPayment();
    }

    @JavascriptInterface
    public void balanceClicked() {
        Account account = getSelectedCard();
        String message;
        if (account.optionalBalanceKeyHandle == null) {
            toast("This service does not support account balances!", Gravity.BOTTOM);
        } else if (account.balanceRequestIsReady) {
            account.balanceRequestIsReady = false;
            toast(account.balance == null ?
    "Failed, but we give it another try!" : "Retrieving the latest balance...", Gravity.BOTTOM);
            account.balance = null;
            account.balanceRequestIsRunning = false;
            setBalance(selectedCard);
        } else {
            toast("Please wait for the result!", Gravity.BOTTOM);
        }
    }

    @JavascriptInterface
    public void toast(String message, int gravity) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(gravity, 0, 0);
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
