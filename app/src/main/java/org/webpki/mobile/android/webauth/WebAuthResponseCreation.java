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
package org.webpki.mobile.android.webauth;

import java.io.IOException;

import java.security.cert.X509Certificate;

import java.util.GregorianCalendar;

import android.os.AsyncTask;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.sks.SKSException;

import org.webpki.webauth.AuthenticationResponseEncoder;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.SignerInterface;

import org.webpki.json.JSONX509Signer;

/**
 * This worker class creates the actual authentication response.
 */
public class WebAuthResponseCreation extends AsyncTask<Void, String, String> {
    private WebAuthActivity webauth_activity;

    private String authorization;

    private int key_handle;

    private String authorization_error;

    public WebAuthResponseCreation(WebAuthActivity webauth_activity, String authorization, int key_handle) {
        this.webauth_activity = webauth_activity;
        this.authorization = authorization;
        this.key_handle = key_handle;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            publishProgress(BaseProxyActivity.PROGRESS_AUTHENTICATING);

            JSONX509Signer signer = new JSONX509Signer(
                    new SignerInterface() {
                        @Override
                        public X509Certificate[] getCertificatePath() throws IOException {
                            X509Certificate[] certificate_path = webauth_activity.sks.getKeyAttributes(key_handle).getCertificatePath();
                            if (webauth_activity.authenticationRequest.wantsExtendedCertPath()) {
                                return certificate_path;
                            }
                            return new X509Certificate[]{certificate_path[0]};
                        }

                        @Override
                        public byte[] signData(byte[] data, AsymSignatureAlgorithms sign_alg) throws IOException {
                            return webauth_activity.sks.signHashedData(key_handle,
                                                                       sign_alg.getAlgorithmId(AlgorithmPreferences.SKS),
                                                                       null,
                                                                       false,
                                                                       authorization.getBytes("UTF-8"),
                                                                       sign_alg.getDigestAlgorithm().digest(data));
                        }
                    });
            signer.setSignatureAlgorithm(webauth_activity.matchingKeys.get(key_handle));
            AuthenticationResponseEncoder authentication_response =
                    new AuthenticationResponseEncoder(signer,
                                                      webauth_activity.authenticationRequest,
                                                      new GregorianCalendar(),
                                                      webauth_activity.getServerCertificate());
            webauth_activity.postJSONData(webauth_activity.getTransactionURL(),
                                          authentication_response,
                                          BaseProxyActivity.RedirectPermitted.REQUIRED);
            return webauth_activity.getRedirectURL();
        } catch (SKSException e) {
            webauth_activity.logException(e);
            if (e.getError() == SKSException.ERROR_AUTHORIZATION) {
                try {
                    authorization_error = webauth_activity.sks.getKeyProtectionInfo(key_handle).isPinBlocked() ?
                            "Too many PIN errors - Blocked" : "Incorrect PIN";
                } catch (SKSException e1) {
                }
            }
        } catch (Exception e) {
            webauth_activity.logException(e);
        }
        return null;
    }

    @Override
    public void onProgressUpdate(String... message) {
        webauth_activity.showHeavyWork(message[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        if (webauth_activity.userHasAborted()) {
            return;
        }
        webauth_activity.noMoreWorkToDo();
        if (result == null) {
            if (authorization_error == null) {
                webauth_activity.showFailLog();
            } else {
                webauth_activity.showAlert(authorization_error);
            }
        } else {
            webauth_activity.launchBrowser(result);
        }
    }
}
