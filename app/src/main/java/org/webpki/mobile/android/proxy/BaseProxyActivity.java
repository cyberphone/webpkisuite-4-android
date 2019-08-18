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

import java.security.cert.X509Certificate;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

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

import org.webpki.mobile.android.saturn.SaturnActivity;

import org.webpki.net.HTTPSWrapper;

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
        executors.put("saturn", SaturnActivity.class);
    }

    public static Class<? extends BaseProxyActivity> getExecutor(Uri url) {
        Class<? extends BaseProxyActivity> executor = executors.get(url.getHost());
        if (executor == null) {
            Log.e("Missing executor", url.getHost());
        }
        return executor;
    }

    private static final String JSON_CONTENT = "application/json";

    private JSONDecoderCache schema_cache;

    private ProgressDialog progress_display;

    private StringBuilder logger = new StringBuilder();

    private HTTPSWrapper https_wrapper;

    public AndroidSKSImplementation sks;

    private String transaction_url;

    private String requesting_host;

    private X509Certificate server_certificate;

    private String redirect_url;

    private boolean user_aborted;

    private boolean init_rejected;

    private Vector<String> cookies = new Vector<String>();

    public Vector<byte[]> protocol_log;

    private byte[] initial_request_object;

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
                        // The user decided that this is not what he/she wants...
                        dialog.cancel();
                        user_aborted = true;
                        abortTearDown();
                        if (cancelUrl == null) {
                            instance.finish();
                        } else {
                            launchBrowser(cancelUrl);
                        }
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
                        user_aborted = true;
                        abortTearDown();
                        if (cancelUrl == null) {
                            instance.finish();
                        } else {
                            launchBrowser(cancelUrl);
                        }
                    }
                });
        alert_dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // The user apparently changed his/her mind and wants to continue...
                dialog.cancel();
                if (message != null && progress_display != null) {
                    progress_display = null;
                    showHeavyWork(message);
                }
            }
        });

        // Create and show alert dialog
        alert_dialog.create().show();
    }

    public String getTransactionURL() {
        return transaction_url;
    }

    public String getRequestingHost() {
        return requesting_host;
    }

    public void showHeavyWork(final String message) {
        if (!user_aborted) {
            if (progress_display == null) {
                progress_display = new ProgressDialog(this);
                progress_display.setMessage(message);
                progress_display.setCanceledOnTouchOutside(false);
                progress_display.setCancelable(false);
                progress_display.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        conditionalAbort(message);
                    }
                });
                progress_display.show();
            } else {
                progress_display.setMessage(message);
            }
        }
    }

    public void noMoreWorkToDo() {
        if (progress_display != null) {
            progress_display.dismiss();
            progress_display = null;
        }
    }

    private void addOptionalCookies(String url) throws IOException {
        for (String cookie : cookies) {
            https_wrapper.setHeader("Cookie", cookie);
        }
    }

    public boolean userHasAborted() {
        return user_aborted;
    }

    public boolean initWasRejected() {
        if (init_rejected) {
            launchBrowser(redirect_url);
        }
        return init_rejected;
    }

    public void initSKS() {
        sks = HardwareKeyStore.createSKS(getProtocolName(), this, false);
    }

    public void closeProxy() {
        if (protocol_log != null) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(getProtocolName(), Context.MODE_PRIVATE));
                oos.writeObject(protocol_log);
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
                                RedirectPermitted redirectPermitted) throws IOException {
        logOK("Writing \"" + json_object.getQualifier() + "\" object to: " + url);
        addOptionalCookies(url);
        https_wrapper.setHeader("Content-Type", JSON_CONTENT);
        byte[] posted_data = json_object.serializeJSONDocument(JSONOutputFormats.PRETTY_PRINT);
        protocol_log.add(posted_data);
        https_wrapper.makePostRequest(url, posted_data);
        if (https_wrapper.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            if ((redirect_url = https_wrapper.getHeaderValue("Location")) == null) {
                throw new IOException("Malformed redirect");
            }
            if (redirectPermitted == RedirectPermitted.FORBIDDEN) {
                throw new IOException("Unexpected redirect: " + getProtocolName());
            }
            return true;
        } else {
            if (https_wrapper.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(https_wrapper.getResponseMessage());
            }
            checkContentType();
            if (redirectPermitted == RedirectPermitted.REQUIRED) {
                throw new IOException("Redirect expected");
            }
            return false;
        }
    }

    private void checkContentType() throws IOException {
        if (!https_wrapper.getContentType().equals(JSON_CONTENT)) {
            throw new IOException("Unexpected content: " + https_wrapper.getContentType());
        }
    }

    public String getRedirectURL() {
        return redirect_url;
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
    protected boolean qrInvoked() {
        return qr_mode;
    }

    boolean pr_mode;
    protected boolean prInvoked() {
        return pr_mode;
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
        schema_cache.addToCache(decoder_class);
        logOK("Added JSON decoder for: " + decoder_class.getName());
    }

    private JSONDecoder parseJSON(byte[] json_data) throws IOException {
        protocol_log.add(json_data);
        JSONDecoder json_object = schema_cache.parse(json_data);
        logOK("Successfully read \"" + json_object.getQualifier() + "\" object");
        return json_object;
    }

    public JSONDecoder parseJSONResponse() throws IOException {
        return parseJSON(https_wrapper.getData());
    }

    public X509Certificate getServerCertificate() {
        return server_certificate;
    }

    public JSONDecoder getInitialRequest() throws IOException {
        return parseJSON(initial_request_object);
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
        protocol_log = new Vector<byte[]>();
        https_wrapper = new HTTPSWrapper();
        initSKS();
        schema_cache = new JSONDecoderCache();
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            throw new IOException("No URI");
        }
        pr_mode = uri.getScheme().startsWith("w3c");
        transaction_url = getQueryParameter(uri, "url");
        String boot_url = getQueryParameter(uri, "init");
        qr_mode = uri.getScheme().startsWith("qr");
        requesting_host = new URL(boot_url).getHost();
        List<String> arg = uri.getQueryParameters("cookie");
        if (!arg.isEmpty()) {
            cookies.add(arg.get(0));
        }
        logOK("Invocation URL=" + transaction_url + ", Cookie: " + (arg.isEmpty() ? "N/A" : cookies.elementAt(0)));
        logOK(uri.toString());
        addOptionalCookies(transaction_url);
        cancelUrl = uri.getQueryParameter("cncl");
        String versionSpan = getQueryParameter(uri, "ver");
        String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        if (versionSpan.substring(0, versionSpan.indexOf('-')).compareTo(version) > 0 ||
            versionSpan.substring(versionSpan.indexOf('-') + 1).compareTo(version) < 0) {
            throw new IOException("App version:" + version + " required:" + versionSpan);
        }
        https_wrapper.makeGetRequest(boot_url);
        if (https_wrapper.getResponseCode() == HttpURLConnection.HTTP_OK) {
            initial_request_object = https_wrapper.getData();
            server_certificate = https_wrapper.getServerCertificates()[0];
            checkContentType();
        } else if (https_wrapper.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            if ((redirect_url = https_wrapper.getHeaderValue("Location")) == null) {
                throw new IOException("Malformed redirect");
            }
            init_rejected = true;
        } else {
            throw new IOException(https_wrapper.getResponseMessage());
        }
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
