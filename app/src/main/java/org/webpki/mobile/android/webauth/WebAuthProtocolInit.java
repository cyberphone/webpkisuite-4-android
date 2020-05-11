/*
 *  Copyright 2013-2020 WebPKI.org (http://webpki.org).
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

import android.os.AsyncTask;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import android.util.Log;

import android.view.KeyEvent;
import android.view.View;

import android.view.inputmethod.EditorInfo;

import java.io.IOException;

import java.security.cert.X509Certificate;

import java.security.interfaces.RSAPublicKey;

import org.webpki.mobile.android.R;

import org.webpki.sks.AppUsage;
import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.KeyAttributes;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CertificateFilter;
import org.webpki.crypto.KeyContainerTypes;

import org.webpki.webauth.AuthenticationRequestDecoder;

import org.webpki.mobile.android.sks.AndroidSKSImplementation;
import org.webpki.mobile.android.sks.HardwareKeyStore;

import org.webpki.mobile.android.util.CredentialListDataFactory;

public class WebAuthProtocolInit extends AsyncTask<Void, String, Boolean> {
    private WebAuthActivity webAuthActivity;

    AndroidSKSImplementation sks;

    public WebAuthProtocolInit(WebAuthActivity webAuthActivity) {
        this.webAuthActivity = webAuthActivity;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            webAuthActivity.getProtocolInvocationData();
            webAuthActivity.addDecoder(AuthenticationRequestDecoder.class);
            webAuthActivity.authenticationRequest = (AuthenticationRequestDecoder) webAuthActivity.getInitialRequest();
            EnumeratedKey ek = new EnumeratedKey();
            sks = HardwareKeyStore.createSKS(WebAuthActivity.WEBAUTH, webAuthActivity, false);

            ////////////////////////////////////////////////////////////////////////////////////
            // Maybe the requester wants better protected keys...
            ////////////////////////////////////////////////////////////////////////////////////
            if (webAuthActivity.authenticationRequest.getOptionalKeyContainerList() != null &&
                    !webAuthActivity.authenticationRequest.getOptionalKeyContainerList().contains(KeyContainerTypes.SOFTWARE)) {
                throw new IOException("The requester asked for another key container type: " +
                        webAuthActivity.authenticationRequest.getOptionalKeyContainerList().toString());
            }

            ////////////////////////////////////////////////////////////////////////////////////
            // Passed that hurdle, now check keys for compliance...
            ////////////////////////////////////////////////////////////////////////////////////
            while ((ek = sks.enumerateKeys(ek.getKeyHandle())) != null) {
                Log.i(WebAuthActivity.WEBAUTH, "KeyHandle=" + ek.getKeyHandle());
                KeyAttributes ka = sks.getKeyAttributes(ek.getKeyHandle());

                ////////////////////////////////////////////////////////////////////////////////////
                // All keys are NOT usable (or intended) for PKI-based authentication...
                ////////////////////////////////////////////////////////////////////////////////////
                if (ka.isSymmetricKey() ||
                        (ka.getAppUsage() != AppUsage.AUTHENTICATION && ka.getAppUsage() != AppUsage.UNIVERSAL)) {
                    continue;
                }
                X509Certificate[] cert_path = ka.getCertificatePath();
                boolean did_it = false;
                boolean rsa_flag = cert_path[0].getPublicKey() instanceof RSAPublicKey;
                AsymSignatureAlgorithms signature_algorithm = null;
                for (AsymSignatureAlgorithms sig_alg : webAuthActivity.authenticationRequest.getSignatureAlgorithms()) {
                    if (rsa_flag == sig_alg.isRsa() && HardwareKeyStore.isSupported(sig_alg.getAlgorithmId(AlgorithmPreferences.SKS))) {
                        signature_algorithm = sig_alg;
                        did_it = true;
                        break;
                    }
                }
                if (!did_it) {
                    continue;
                }
                if (webAuthActivity.authenticationRequest.getCertificateFilters().length > 0) {
                    did_it = false;
                    ////////////////////////////////////////////////////////////////////////////////////
                    // The requester wants to discriminate keys further...
                    ////////////////////////////////////////////////////////////////////////////////////
                    for (CertificateFilter cf : webAuthActivity.authenticationRequest.getCertificateFilters()) {
                        if (cf.matches(cert_path)) {
                            did_it = true;
                            break;
                        }
                    }
                    if (!did_it) {
                        continue;
                    }
                }
                webAuthActivity.matchingKeys.put(ek.getKeyHandle(), signature_algorithm);
            }
            return true;
        } catch (Exception e) {
            webAuthActivity.logException(e);
        }
        return false;
    }

    int firstKey() {
        return webAuthActivity.matchingKeys.keySet().iterator().next();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (webAuthActivity.userHasAborted()) {
            return;
        }
        webAuthActivity.noMoreWorkToDo();
        if (success) {
            ///////////////////////////////////////////////////////////////////////////////////
            // Successfully received the request, now show the domain name of the requester
            ///////////////////////////////////////////////////////////////////////////////////
            ((TextView) webAuthActivity.findViewById(R.id.partyInfo)).setText(webAuthActivity.getRequestingHost());
            ((TextView) webAuthActivity.findViewById(R.id.partyInfo)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(webAuthActivity, "Requesting Party Properties - Not yet implemented!", Toast.LENGTH_LONG).show();
                }
            });

            final Button ok = (Button) webAuthActivity.findViewById(R.id.OKbutton);
            final Button cancel = (Button) webAuthActivity.findViewById(R.id.cancelButton);
            ok.requestFocus();
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    webAuthActivity.logOK("The user hit OK");

                    ///////////////////////////////////////////////////////////////////////////////////
                    // We have no keys at all or no keys that matches the filter criterions, abort
                    ///////////////////////////////////////////////////////////////////////////////////
                    if (webAuthActivity.matchingKeys.isEmpty()) {
                        webAuthActivity.showAlert("You have no matching credentials");
                        return;
                    }
                    try {
                        ///////////////////////////////////////////////////////////////////////////////////
                        // Seem we got something here to authenticate with!
                        ///////////////////////////////////////////////////////////////////////////////////
                        if (((CheckBox) webAuthActivity.findViewById(R.id.grantCheckBox)).isChecked()) {
                            sks.setGrant(firstKey(), webAuthActivity.getRequestingHost(), true);
                        }
                        webAuthActivity.setContentView(R.layout.activity_webauth_pin);
                        ((LinearLayout) webAuthActivity.findViewById(R.id.credential_element)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(webAuthActivity, "Credential Properties - Not yet implemented!", Toast.LENGTH_LONG).show();
                            }
                        });
                        CredentialListDataFactory credential_data_factory = new CredentialListDataFactory(webAuthActivity, sks);
                        ((ImageView) webAuthActivity.findViewById(R.id.auth_cred_logo)).setImageBitmap(credential_data_factory.getListIcon(firstKey()));
                        String friendly_name = credential_data_factory.getFriendlyName(firstKey());
                        ((TextView) webAuthActivity.findViewById(R.id.auth_cred_domain)).setText(friendly_name == null ? credential_data_factory.getDomain(firstKey()) : friendly_name);
                        if (android.os.Build.VERSION.SDK_INT < 16) {
                            webAuthActivity.findViewById(R.id.pinWindow).setVisibility(View.GONE);
                            webAuthActivity.findViewById(R.id.pinWindow).setVisibility(View.VISIBLE);
                        }
                        final Button ok = (Button) webAuthActivity.findViewById(R.id.OKbutton);
                        Button cancel = (Button) webAuthActivity.findViewById(R.id.cancelButton);
                        final EditText pin = (EditText) webAuthActivity.findViewById(R.id.editpin1);
                        pin.setSelected(true);
                        pin.requestFocus();
                        ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new WebAuthResponseCreation(webAuthActivity,
                                                            pin.getText().toString(),
                                                            firstKey()).execute();
                            }
                        });
                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                webAuthActivity.conditionalAbort(null);
                            }
                        });
                        pin.setOnEditorActionListener(new OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                                    ok.performClick();
                                }
                                return false;
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    webAuthActivity.conditionalAbort(null);
                }
            });
            try {
                if (!webAuthActivity.matchingKeys.isEmpty() &&
                        sks.isGranted(firstKey(), webAuthActivity.getRequestingHost())) {
                    ((CheckBox) webAuthActivity.findViewById(R.id.grantCheckBox)).setChecked(true);
                    ok.performClick();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            webAuthActivity.showFailLog();
        }
    }
}
