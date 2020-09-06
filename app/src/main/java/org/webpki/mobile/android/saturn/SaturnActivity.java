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

import org.webpki.mobile.android.R;

import java.io.ByteArrayInputStream;
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

import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;

import org.webpki.mobile.android.application.Settings;
import org.webpki.mobile.android.application.ImageGenerator;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.mobile.android.saturn.common.AuthorizationDataEncoder;
import org.webpki.mobile.android.saturn.common.UserAuthorizationMethods;
import org.webpki.mobile.android.saturn.common.ClientPlatform;
import org.webpki.mobile.android.saturn.common.UserResponseItem;
import org.webpki.mobile.android.saturn.common.WalletRequestDecoder;
import org.webpki.mobile.android.saturn.common.MobileProxyParameters;

import org.webpki.mobile.android.sks.HardwareKeyStore;

import org.webpki.sks.BiometricProtection;
import org.webpki.sks.KeyProtectionInfo;
import org.webpki.sks.SKSException;

import org.webpki.util.ArrayUtil;
import org.webpki.util.HTMLEncoder;


public class SaturnActivity extends BaseProxyActivity {

    public static final String SATURN = "Saturn";

    static final String SATURN_SOFTWARE = "WebPKI Suite/Saturn";

    int lastKeyId;

    static final String BACKGROUND_WH = "#f2f2ff";
    static final String BORDER_WH     = "#8080ff";

    static final String HEADER_FONT_SIZE   = "14pt";

    static final String HTML_HEADER_WHITE =
          "pt;color:black;background-color:white}\n" +
          "td.label {text-align:right;padding:3pt 3pt 3pt 0pt}\n" +
          "td.field {min-width:11em;padding:3pt 6pt 3pt 6pt;border-width:1px" +
              ";border-style:solid;border-color:" + BORDER_WH +
              ";background-color:" + BACKGROUND_WH + ";overflow:hidden;" +
          "white-space:nowrap;box-sizing:border-box}\n" +
          "div.balance {margin-top:4pt;padding:2pt 5pt;border-width:1px" +
              ";border-style:solid;border-color:" + BORDER_WH +
              ";border-radius:5pt;background-color:" + BACKGROUND_WH +
              ";display:flex;align-items:center}\n" +
          "div.message {visibility:hidden;position:absolute;box-shadow:3pt 3pt 3pt lightgrey" +
              ";border-width:1px;border-color:grey;border-style:solid;border-radius:10pt" +
              ";left:10pt;right:10pt;background-color:#ffffea;color:black;padding:15pt 10pt}" +
          "span.pinfix {color:" + BACKGROUND_WH + "}\n" +
          "span.moneynote {color:darkblue}\n" +
          "span.marquee {color:brown;display:inline-block;position:relative;top:1pt" +
              ";white-space:nowrap;font-size:";

    static final String HTML_HEADER_SPACE =
          "pt;color:white" +
               ";background:linear-gradient(to bottom right, #162c44, #6d7a8e, #162c44)" +
               ";background-attachment:fixed}\n" +
          "td.label {font-weight:500;text-align:right;padding:3pt 3pt 3pt 0pt}\n" +
          "td.field {font-weight:500;min-width:11em;padding:3pt 6pt 3pt 6pt;border-width:1pt" +
               ";border-style:solid;border-color:#b0b0b0;background-color:black;overflow:hidden;" +
          "white-space:nowrap;box-sizing:border-box}\n" +
          "div.balance {font-weight:500;margin-top:4pt;padding:2pt 5pt;border-width:1pt" +
              ";border-style:solid;border-color:#b0b0b0;border-radius:5pt" +
              ";background-color:black;display:flex;align-items:center}\n" +
          "div.message {visibility:hidden;position:absolute;box-shadow:0pt 0pt 8pt white" +
              ";border-width:1pt;border-color:#162c44;border-style:solid;border-radius:10pt" +
              ";left:10pt;right:10pt;background-color:white;color:black;padding:15pt 10pt}" +
          "span.pinfix {color:black}\n" +
          "span.moneynote {color:lightblue}\n" +
          "span.marquee {color:orange;display:inline-block;position:relative;top:1pt" +
          ";white-space:nowrap;font-size:";

    String htmlBodyPrefix;

    boolean landscapeMode;

    boolean visuallyImpaired;

    WalletRequestDecoder walletRequest;

    enum FORM {SIMPLE, PAYMENTREQUEST}

    FORM currentForm = FORM.SIMPLE;

    int selectedCard;

    String pin = "";

    UserResponseItem[] challengeResults;

    byte[] privateMessageEncryptionKey;

    JSONObjectWriter authorizationData;

    boolean done;

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
                            argument = account.balance == null ?
                                ImageGenerator.getFailedIcon().toString() : account.balance;
                        } else {
                            argument = ImageGenerator.getSpinner().toString();
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
                        "setBalance(\"Balance:&nbsp;" + argument + "\");", null);
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

    void loadHtml(final String positionScript, final StringBuilder body) {
        try {
            currentHtml = new StringBuilder(
                    "<!DOCTYPE html><html><head><title>Saturn</title><style type='text/css'>\n" +
                    "body {margin:0;font-family:Roboto;font-size:")
                .append(visuallyImpaired ? 18 : 12)
                .append(Settings.isWhiteTheme() ? HTML_HEADER_WHITE : HTML_HEADER_SPACE)
                .append(visuallyImpaired ? 15 : 10)
                .append("pt}\n" +
                  "div.header {font-size:" + HEADER_FONT_SIZE +
                      ";visibility:hidden;position:absolute;width:100%;text-align:center}\n" +
                  "span.money {font-weight:500;letter-spacing:1pt}\n" +
                  "@keyframes spin {100% {transform:rotate(360deg);}}\n" +
                  "</style>\n" +
                  "<script>\n" +
                  "'use strict';\n" +
                  "function positionElements() {\n")
                .append(positionScript)
                .append(htmlBodyPrefix)
                .append(body)
                .append("</body></html>").toString().getBytes("utf-8");
/*
            FileOutputStream fos = openFileOutput("html.txt", Context.MODE_PRIVATE);
            fos.write(currentHtml);
            fos.close();
*/
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
                 new StringBuilder (
                     "<table id='simple' style='visibility:hidden;position:absolute'>" +
                     "<tr><td style='padding:20pt;font-size:" + HEADER_FONT_SIZE + "'>")
                     .append(simpleHtml)
                     .append("</td></tr></table>"));
    }

    public void messageDisplay(String js, String message) {
        currentForm = FORM.SIMPLE;
        loadHtml("let message = document.getElementById('message');\n" +
                 "message.style.top = ((Saturn.height() - message.offsetHeight) / 2) + 'px';\n" +
                 "message.style.visibility='visible';\n" +
                 js,
                 new StringBuilder(
                        "<div id='message' class='message'>")
                     .append(message)
                     .append("</div>"));
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
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        factor = (int)(displayMetrics.density * 100);
        landscapeMode = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        Settings.initialize(getApplicationContext());
        lastKeyId = Settings.getLastKeyId();
        try {
            webPkiVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            htmlBodyPrefix = new StringBuilder("}\n" +
                                              "</script>" +
                                              "</head><body onload='positionElements()'>" +
                                              "<svg style='height:")
                .append((int)((Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels)  * 5) / factor))
                .append("px'")
                .append(new String(ArrayUtil.getByteArrayFromInputStream(getResources()
                            .openRawResource(Settings.isWhiteTheme() ?
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

    StringBuilder htmlOneCard(int width) {
        int arrowWidth = (width * 4) / factor;
        return new StringBuilder(
                "<table id='card' style='visibility:hidden;position:absolute'>"+

                "<tr><td id='leftArrow' style='visibility:hidden'>")
            .append(ImageGenerator.getLeftArrow(arrowWidth, visuallyImpaired))
            .append("</td><td>")
            .append(ImageGenerator.getStylizedCardImage((width * 100) / factor, selectedCard))
            .append("</td><td id='rightArrow' style='visibility:hidden'>")
            .append(ImageGenerator.getRightArrow(arrowWidth, visuallyImpaired))
            .append(
                "</td></tr>")

            .append(visuallyImpaired ? "" :
                "<tr><td colspan='3' style='text-align:center'>" +
                    "<div style='display:inline-block'>" +
                    "<div class='balance' id='balance' onClick=\"Saturn.balanceClicked()\">" +
                    "</div></div>" +
                "</td></tr>")

            .append(
                "</table>");
    }

    Account getSelectedCard() {
        return accountCollection.get(selectedCard);
    }

    void showPaymentRequest() throws IOException {
        visuallyImpaired = Settings.isVisuallyImpaired() && !landscapeMode;
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
                "let pGutter = ((kbd.offsetWidth - paydata.offsetWidth + payeelabel.offsetWidth)" +
                    " / 2) + wGutter * 1.5;\n" +
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
                "let pGutter = ((Saturn.width() - paydata.offsetWidth - payeelabel.offsetWidth)" +
                    " / 2);\n" +
                "if (pGutter < 10) pGutter = 10;\n" +
                "paydata.style.left = pGutter + 'px';\n" +
                "let kbdTop = Saturn.height() - Math.floor(kbd.offsetHeight * 1.20);\n" +
                "kbd.style.top = kbdTop + 'px';\n" +
                "kbd.style.left = ((Saturn.width() - kbd.offsetWidth) / 2) + 'px';\n" +
                "let gutter = (kbdTop - card.offsetHeight - paydata.offsetHeight) / 7;\n" +
                "card.style.top = gutter * 3 + 'px';\n" +
                "paydataTop = 5 * gutter + card.offsetHeight;\n" +
                // Fix
                "let dist1 = kbdTop - paydataTop - paydata.offsetHeight;\n" +
                "let dist2 = Saturn.height() - kbdTop - kbd.offsetHeight;\n" +
                "if (dist1 < dist2) kbd.style.top = (paydataTop + paydata.offsetHeight + (dist1 + dist2) / 2) + 'px';\n" +
                // End fix
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

            "function setBalance(argument) {\n" +
            "  let e = document.getElementById('balance');\n" +
            "  if (e) e.innerHTML = argument;\n" +
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
              "let pwd = \"<span style='position:relative;font-size:")
        .append(visuallyImpaired ? "15pt;top;-2pt" : "10pt;top:-1pt")
        .append("'>\";\n" +
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
            "<tr><td id='payeelabel' class='label'>Payee</td><td colspan='2' id='payeefield' " +
                "class='field' onClick=\"Saturn.toast('Name of merchant', " +
                    Gravity.CENTER_VERTICAL + ")\">")
          .append(HTMLEncoder.encode(walletRequest.paymentRequest.getPayeeCommonName()))
          .append("</td></tr>" +
            "<tr><td colspan='3' style='height:5pt'></td></tr>" +
            "<tr><td class='label'>")
          .append(walletRequest.getAmountLabel())
          .append(
            "</td><td colspan='2' id='amountfield' " +
                "class='field' onClick=\"Saturn.toast('Amount to pay', " +
                    Gravity.CENTER_VERTICAL + ")\">")
          .append(walletRequest.getFormattedTotal())
          .append("</td></tr>" +
            "<tr><td colspan='3' style='height:5pt'></td></tr>" +
            "<tr id='pinRow'><td class='label'>PIN</td>" +
            "<td id='pinfield' class='field' style='min-width:unset' " +
                "onClick=\"Saturn.toast('Use the keyboard below...', " +
                    Gravity.CENTER_VERTICAL + ")\"></td><td id='fpField' style='width:10px'>")
          .append(ImageGenerator.getFingerPrintSymbol("1",
                                                      "selectAuthMode(true)",
                                                      "block",
                                                      7,
                                                      visuallyImpaired ? 27 : 18))
          .append(
            "</td></tr>" +
            "</table>" +

            "<table id='fpFrame' style='visibility:hidden;position:absolute'>" +
            "<tr><td class='label'>Authorize&nbsp;Request</td><td></td></tr>" +
            "<tr><td style='text-align:center'>")
          .append(ImageGenerator.getFingerPrintSymbol("0.6",
                                                      "Saturn.toast('Use the fingerprint sensor', " +
                                                          Gravity.BOTTOM + ")",
                                                      "inline-block",
                                                      0,
                                                      visuallyImpaired ? 60 : 40))
          .append(
            "</td><td id='pinSwitch' onclick=\"selectAuthMode(false)\">")
          .append(ImageGenerator.getFingerPrintSwitch(visuallyImpaired))
          .append(
            "</td></tr>" +
            "<tr><td id='fpText' colspan='2' style='height:0px'></td></tr>" +
            "</table>" +

            "<div id='kbd' style='visibility:hidden;position:absolute;width:")
          .append(landscapeMode ? (width * 50) / factor : (width * 88) / factor)
          .append("px'>")
          .append(ImageGenerator.getKeyBoard(visuallyImpaired))
          .append("</div>")
          .append(htmlOneCard(landscapeMode ? (width * 4) / 11 :
              (width * (visuallyImpaired ? 9 : 7)) / 10));
        loadHtml(js.toString(), html);
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
        Settings.setBiometricPreferred(biometricPreferred);
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
                .setBoolean("useBiometrics",
                            supportsBiometric && Settings.isBiometricsPreferred())
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
        if (fingerPrintAuthenticationInProgess != null) {
            fingerPrintAuthenticationInProgess.cancel();
            fingerPrintAuthenticationInProgess = null;
        }
        if (useBiometrics) {
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(this);
            if (fingerprintManager.hasEnrolledFingerprints()) {
                fingerPrintAuthenticationInProgess = new CancellationSignal();
                fingerprintManager.authenticate(
                    null,
                    0,
                    fingerPrintAuthenticationInProgess,
                    new FingerprintManagerCompat.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationSucceeded(
                            FingerprintManagerCompat.AuthenticationResult result) {
                            performPayment();
                        }

                        @Override
                        public void onAuthenticationError(int errMsgId, CharSequence errString) {
                            if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                                setFingerPrintError("Failed to authenticate, try again later");
                            }
                        }
                    },
                    null);
            } else {
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
                    fingerPrintAuthenticationInProgess == null ?
                        UserAuthorizationMethods.PIN : UserAuthorizationMethods.FINGER_PRINT,
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
