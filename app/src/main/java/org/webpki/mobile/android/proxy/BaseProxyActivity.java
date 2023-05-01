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
package org.webpki.mobile.android.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import java.net.HttpURLConnection;
import java.net.URL;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;

import android.util.Log;

import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONEncoder;
import org.webpki.json.JSONOutputFormats;

import org.webpki.mobile.android.keygen2.KeyGen2Activity;

import org.webpki.mobile.android.saturn.SaturnActivity;

import org.webpki.mobile.android.webauth.WebAuthActivity;

import org.webpki.net.HTTPSWrapper;

import org.webpki.mobile.android.saturn.common.MobileProxyParameters;

import org.webpki.mobile.android.sks.AndroidSKSImplementation;
import org.webpki.mobile.android.sks.HardwareKeyStore;

/**
 * Class for taking care of "webpkiproxy://" JSON protocol handlers
 */
public abstract class BaseProxyActivity extends Activity {
    //////////////////////
    // Progress messages
    //////////////////////
    public enum RedirectPermitted {FORBIDDEN, OPTIONAL, REQUIRED};

    public static final String PROGRESS_INITIALIZING   = "Initializing...";
    public static final String PROGRESS_SESSION        = "Creating session...";
    public static final String PROGRESS_KEYGEN         = "Generating keys...";
    public static final String PROGRESS_LOOKUP         = "Credential lookup...";
    public static final String PROGRESS_DEPLOY_CERTS   = "Receiving credentials...";
    public static final String PROGRESS_FINAL          = "Finish message...";
    public static final String PROGRESS_PAYMENT        = "Payment processing...";
    public static final String PROGRESS_AUTHENTICATING = "Authenticating...";

    public static final String CONTINUE_EXECUTION      = "CONTINUE_EXECUTION";  // Return constant to AsyncTasks

    static LinkedHashMap<String,Class<? extends BaseProxyActivity>> executors =
            new LinkedHashMap<String,Class<? extends BaseProxyActivity>>();

    static {
        executors.put(MobileProxyParameters.HOST_SATURN, SaturnActivity.class);
        executors.put(MobileProxyParameters.HOST_KEYGEN2, KeyGen2Activity.class);
        executors.put(MobileProxyParameters.HOST_MOBILEID, WebAuthActivity.class);
    }

    public static Class<? extends BaseProxyActivity> getExecutor(Uri url) {
        Class<? extends BaseProxyActivity> executor = executors.get(url.getHost());
        if (executor == null) {
            Log.e("Missing executor", url.getHost());
        }
        return executor;
    }

    private static final String JSON_CONTENT = "application/json";

    private JSONDecoderCache schemaCache;

    private ProgressDialog progressDisplay;

    private StringBuilder logger = new StringBuilder();

    private HTTPSWrapper httpsWrapper;

    public AndroidSKSImplementation sks;

    private String transactionUrl;

    private String requestingHost;

    private X509Certificate serverCertificate;

    private String redirectUrl;

    private boolean userAborted;

    private ArrayList<String> cookies = new ArrayList<String>();

    public ArrayList<byte[]> protocolLog;

    private byte[] initialRequestObject;

    //TODO check this a bit more
    public boolean localTesting;

    protected abstract String getProtocolName();

    protected abstract void abortTearDown();

    protected abstract String getAbortString();

    public void unconditionalAbort(final String message) {
        final BaseProxyActivity instance = this;
        AlertDialog.Builder alert_dialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Something went terribly wrong...
                        dialog.cancel();
                        userAborted = true;
                        abortTearDown();
                        launchBrowser(cancelUrl);
                    }
                });
        // Create and show alert dialog
        alert_dialog.create().show();
    }

    public void conditionalAbort(final String message) {
        final BaseProxyActivity instance = this;
        AlertDialog.Builder alert_dialog = new AlertDialog.Builder(this)
                .setMessage(getAbortString())
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // The user decided that this is not what he/she wants...
                        dialog.cancel();
                        userAborted = true;
                        abortTearDown();
                        launchBrowser(cancelUrl);
                    }
                });
        alert_dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // The user apparently changed his/her mind and wants to continue...
                dialog.cancel();
                if (message != null && progressDisplay != null) {
                    progressDisplay = null;
                    showHeavyWork(message);
                }
            }
        });
        // Create and show alert dialog
        alert_dialog.create().show();
    }

    public String getTransactionURL() {
        return transactionUrl;
    }

    public String getRequestingHost() {
        return requestingHost;
    }

    public void showHeavyWork(final String message) {
        if (!userAborted) {
            if (progressDisplay == null) {
                progressDisplay = new ProgressDialog(this);
                progressDisplay.setMessage(message);
                progressDisplay.setCanceledOnTouchOutside(false);
                progressDisplay.setCancelable(false);
                progressDisplay.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        conditionalAbort(message);
                    }
                });
                progressDisplay.show();
            } else {
                progressDisplay.setMessage(message);
            }
        }
    }

    public void noMoreWorkToDo() {
        if (progressDisplay != null) {
            progressDisplay.dismiss();
            progressDisplay = null;
        }
    }

    private void addOptionalCookies(String url) throws IOException {
        for (String cookie : cookies) {
            httpsWrapper.setHeader("Cookie", cookie);
        }
    }

    public boolean userHasAborted() {
        return userAborted;
    }

    public void initSKS() {
        sks = HardwareKeyStore.createSKS(getProtocolName(), this, false);
    }

    public void closeProxy() {
        if (protocolLog != null) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(getProtocolName(), Context.MODE_PRIVATE));
                oos.writeObject(protocolLog);
                oos.close();
                Log.i(getProtocolName(), "Wrote protocol log");
            } catch (Exception e) {
                Log.e(getProtocolName(), "Couldn't write protocol log");
            }
        }
        HardwareKeyStore.serializeSKS(getProtocolName(), this);
    }

    public void launchBrowser(String url) {
        noMoreWorkToDo();
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
        startActivity(intent);
        closeProxy();
        finish();
    }

    public boolean postJSONData(String url,
                                JSONEncoder json_object,
                                RedirectPermitted redirectPermitted)
            throws IOException, GeneralSecurityException {
        logOK("Writing \"" + json_object.getQualifier() + "\" object to: " + url);
        addOptionalCookies(url);
        httpsWrapper.setHeader("Content-Type", JSON_CONTENT);
        byte[] posted_data = json_object.serializeJSONDocument(JSONOutputFormats.PRETTY_PRINT);
        protocolLog.add(posted_data);
        httpsWrapper.makePostRequest(url, posted_data);
        if (httpsWrapper.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            if ((redirectUrl = httpsWrapper.getHeaderValue("Location")) == null) {
                throw new IOException("Malformed redirect");
            }
            if (redirectPermitted == RedirectPermitted.FORBIDDEN) {
                throw new IOException("Unexpected redirect: " + getProtocolName());
            }
            return true;
        } else {
            if (httpsWrapper.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(httpsWrapper.getResponseMessage());
            }
            checkContentType();
            if (redirectPermitted == RedirectPermitted.REQUIRED) {
                throw new IOException("Redirect expected");
            }
            return false;
        }
    }

    private void checkContentType() throws IOException {
        if (!httpsWrapper.getContentType().equals(JSON_CONTENT)) {
            throw new IOException("Unexpected content: " + httpsWrapper.getContentType());
        }
    }

    public String getRedirectURL() {
        return redirectUrl;
    }

    public void logOK(String message) {
        logger.append(message).append('\n');
    }

    public void logException(Exception e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter printer_writer = new PrintWriter(baos);
        e.printStackTrace(printer_writer);
        printer_writer.flush();
        try {
            logger.append("<font color=\"red\">").append(baos.toString("UTF-8")).append("</font>");
        } catch (IOException e1) {
        }
    }

    boolean qr_mode;
    protected boolean qrCodeInvoked() {
        return qr_mode;
    }

    boolean w3cpay_mode;
    protected boolean w3cPayInvoked() {
        return w3cpay_mode;
    }

    public void showFailLog() {
        noMoreWorkToDo();
        Intent intent = new Intent(this, FailLoggerActivity.class);
        intent.putExtra(FailLoggerActivity.LOG_MESSAGE, logger.toString());
        startActivity(intent);
        closeProxy();
        finish();
    }

    public void addDecoder(Class<? extends JSONDecoder> decoder_class) throws IOException {
        schemaCache.addToCache(decoder_class);
        logOK("Added JSON decoder for: " + decoder_class.getName());
    }

    private JSONDecoder parseJSON(byte[] json_data) throws IOException, GeneralSecurityException {
        protocolLog.add(json_data);
        JSONDecoder json_object = schemaCache.parse(json_data);
        logOK("Successfully read \"" + json_object.getQualifier() + "\" object");
        return json_object;
    }

    public JSONDecoder parseJSONResponse() throws IOException, GeneralSecurityException {
        return parseJSON(httpsWrapper.getData());
    }

    public X509Certificate getServerCertificate() {
        return serverCertificate;
    }

    public JSONDecoder getInitialRequest() throws IOException, GeneralSecurityException {
        return parseJSON(initialRequestObject);
    }

    String cancelUrl;

    private String getQueryParameter(Uri uri, String name) throws IOException {
        String value = uri.getQueryParameter(name);
        if (value == null) {
            throw new IOException("Missing: " + name);
        }
        return value;
    }
    
    public void getProtocolInvocationData() throws Exception {
        logOK(getProtocolName() + " protocol run: " + new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss").format(new Date()));
        protocolLog = new ArrayList<byte[]>();
        httpsWrapper = new HTTPSWrapper();
        initSKS();
        schemaCache = new JSONDecoderCache();
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            throw new IOException("No URI");
        }
        transactionUrl = getQueryParameter(uri, MobileProxyParameters.PUP_MAIN_URL);
        String boot_url = getQueryParameter(uri, MobileProxyParameters.PUP_INIT_URL);
        cancelUrl = getQueryParameter(uri, MobileProxyParameters.PUP_CANCEL_URL);
        qr_mode = uri.getScheme().equals(MobileProxyParameters.SCHEME_QRCODE);
        w3cpay_mode = uri.getScheme().equals(MobileProxyParameters.SCHEME_W3CPAY);
        requestingHost = new URL(boot_url).getHost();
        List<String> arg = uri.getQueryParameters(MobileProxyParameters.PUP_COOKIE);
        if (!arg.isEmpty()) {
            cookies.add(arg.get(0));
        }
        logOK("Invocation URL=" + transactionUrl + ", Cookie: " + (arg.isEmpty() ? "N/A" : cookies.get(0)));
        logOK(uri.toString());
        addOptionalCookies(transactionUrl);
        String versionSpan = getQueryParameter(uri, MobileProxyParameters.PUP_VERSIONS);
        String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        if (versionSpan.substring(0, versionSpan.indexOf('-')).compareTo(version) > 0 ||
            versionSpan.substring(versionSpan.indexOf('-') + 1).compareTo(version) < 0) {
            throw new IOException("\n\nActual app version:" + version +
                                  "\nRequired version:" + versionSpan + "\n");
        }
//TODO a new error handling please...
        httpsWrapper.setFollowRedirects(true);
        httpsWrapper.setRequireSuccess(true);
        httpsWrapper.makeGetRequest(boot_url);
        initialRequestObject = httpsWrapper.getData();
        serverCertificate = httpsWrapper.getServerCertificates()[0];
        localTesting = serverCertificate.getIssuerX500Principal()
            .getName().equals("CN=WebPKI.org TLS Root CA");
        byte[] eeCert = intent.getByteArrayExtra("eeCert");
        if (eeCert != null && !Arrays.equals(eeCert, serverCertificate.getEncoded())) {
            throw new IOException("Certificate mismatch");
        }
        checkContentType();
        httpsWrapper.setFollowRedirects(false);
        httpsWrapper.setRequireSuccess(false);
    }

    public void showAlert(String message) {
        AlertDialog.Builder alert_dialog = new AlertDialog.Builder(this)
                .setMessage(message).setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Close the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // Create and show alert dialog
        alert_dialog.create().show();
    }
}
