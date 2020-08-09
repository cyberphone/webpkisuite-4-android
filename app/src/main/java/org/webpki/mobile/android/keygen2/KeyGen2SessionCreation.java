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
package org.webpki.mobile.android.keygen2;

import java.security.cert.X509Certificate;

import java.util.GregorianCalendar;
import java.util.Iterator;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;

import android.view.KeyEvent;
import android.view.View;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.TextView.OnEditorActionListener;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.mobile.android.R;

import org.webpki.json.JSONDecoder;

import org.webpki.keygen2.CredentialDiscoveryRequestDecoder;
import org.webpki.keygen2.CredentialDiscoveryResponseEncoder;
import org.webpki.keygen2.KeyCreationRequestDecoder;
import org.webpki.keygen2.KeyGen2URIs;
import org.webpki.keygen2.InvocationResponseEncoder;
import org.webpki.keygen2.ProvisioningInitializationRequestDecoder;
import org.webpki.keygen2.ProvisioningInitializationResponseEncoder;

import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.EnumeratedProvisioningSession;
import org.webpki.sks.KeyAttributes;
import org.webpki.sks.AppUsage;
import org.webpki.sks.Grouping;
import org.webpki.sks.KeyProtectionInfo;
import org.webpki.sks.PassphraseFormat;
import org.webpki.sks.ProvisioningSession;
import org.webpki.sks.DeviceInfo;

/**
 * This worker class creates the SKS/KeyGen2 SessionKey.
 * Optionally credentials are looked-up and PINs are set.
 */
public class KeyGen2SessionCreation extends AsyncTask<Void, String, String> {
    private KeyGen2Activity keyGen2Activity;

    private int pin_count;

    private class PINDialog {
        private EditText pin1;
        private EditText pin2;
        private TextView pinErrView;
        private boolean equalPins;

        KeyCreationRequestDecoder.UserPINDescriptor upd;

        private void upperCasePIN(EditText pin) {
            InputFilter[] oldFilter = pin.getEditableText().getFilters();
            InputFilter[] newFilter = new InputFilter[oldFilter.length + 1];
            for (int i = 0; i < oldFilter.length; i++) {
                newFilter[i] = oldFilter[i];
            }
            newFilter[oldFilter.length] = new InputFilter.AllCaps();
            pin.getEditableText().setFilters(newFilter);
        }

        private boolean checkPIN(boolean set_value) {
            String pin = pin1.getText().toString();
            equalPins = pin1.getText().toString().equals(pin2.getText().toString());
            KeyCreationRequestDecoder.UserPINError res = upd.setPIN(pin, set_value && equalPins);
            if (res == null) {
                pinErrView.setText("");
                return true;
            } else {
                KeyCreationRequestDecoder.PINPolicy pinPolicy = upd.getPINPolicy();
                String error = "PIN syntax error";
                if (res.lengthError) {
                    int multiplier = pinPolicy.getFormat() == PassphraseFormat.BINARY ? 2 : 1;
                    error = "PINs must be " + (pinPolicy.getMinLength() * multiplier) + "-" + (pinPolicy.getMaxLength() * multiplier) +
                            (pinPolicy.getFormat() == PassphraseFormat.NUMERIC ? " digits" : " characters");
                } else if (res.syntaxError) {
                    switch (pinPolicy.getFormat()) {
                        case NUMERIC:
                            error = "PINs must only contain 0-9";
                            break;

                        case ALPHANUMERIC:
                            error = "PINs must only contain 0-9 A-Z";
                            break;

                        case BINARY:
                            error = "PINs must be a hexadecimal string";
                            break;

                        default:
                            break;
                    }
                } else if (res.patternError != null) {
                    switch (res.patternError) {
                        case SEQUENCE:
                            error = "PINs must not be a sequence";
                            break;

                        case TWO_IN_A_ROW:
                            error = "PINs must not contain two equal\ncharacters in a row";
                            break;

                        case THREE_IN_A_ROW:
                            error = "PINs must not contain three equal\ncharacters in a row";
                            break;

                        case REPEATED:
                            error = "PINs must not contain the same\ncharacters twice";
                            break;

                        case MISSING_GROUP:
                            error = "PINs must be a mix of A-Z " + (pinPolicy.getFormat() == PassphraseFormat.ALPHANUMERIC ?
                                    "0-9" : "a-z 0-9\nand control characters");
                            break;
                    }
                } else if (res.uniqueError) {
                    error = "PINs for " + upd.getAppUsage().getProtocolName() + " and " + res.uniqueErrorAppUsage.getProtocolName() + " must not be equal";
                }
                pinErrView.setText(error);
            }
            return false;
        }

        PINDialog(final Iterator<KeyCreationRequestDecoder.UserPINDescriptor> iter) {
            if (iter.hasNext()) {
                keyGen2Activity.noMoreWorkToDo();
                upd = iter.next();
                keyGen2Activity.setContentView(R.layout.activity_keygen2_pin);

                final Button ok = (Button) keyGen2Activity.findViewById(R.id.OKbutton);
                Button cancel = (Button) keyGen2Activity.findViewById(R.id.cancelButton);

                pin1 = (EditText) keyGen2Activity.findViewById(R.id.editpin1);
                pin2 = (EditText) keyGen2Activity.findViewById(R.id.editpin2);
                if (upd.getPINPolicy().getFormat() != PassphraseFormat.NUMERIC) {
                    pin1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    pin2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                if (upd.getPINPolicy().getFormat() == PassphraseFormat.ALPHANUMERIC) {
                    upperCasePIN(pin1);
                    upperCasePIN(pin2);
                }
                pin1.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_NEXT);
                pin2.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_GO);
                pinErrView = (TextView) keyGen2Activity.findViewById(R.id.errorPIN);
                TextView set_pin_text = (TextView) keyGen2Activity.findViewById(R.id.setPINtext);
                StringBuilder lead_text = new StringBuilder("Set ");
                if (upd.getPINPolicy().getGrouping() == Grouping.SIGNATURE_PLUS_STANDARD) {
                    lead_text.append(upd.getAppUsage() == AppUsage.SIGNATURE ? "signature " : "standard ");
                } else if (upd.getPINPolicy().getGrouping() == Grouping.UNIQUE) {
                    if (upd.getAppUsage() != AppUsage.UNIVERSAL) {
                        lead_text.append(upd.getAppUsage().getProtocolName());
                        lead_text.append(' ');
                    }
                }
                lead_text.append("PIN");
                if (keyGen2Activity.keyCreationRequest.getUserPINDescriptors().size() > 1) {
                    lead_text.append(" #").append(++pin_count);
                }
                set_pin_text.setText(lead_text);
                checkPIN(false);
                pin1.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        checkPIN(false);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }
                });
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (checkPIN(true)) {
                            if (equalPins) {
                                new PINDialog(iter);
                            } else {
                                keyGen2Activity.showAlert("The retyped PIN doesn't match the original");
                            }
                        } else {
                            keyGen2Activity.showAlert("Please correct PIN");
                        }
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        keyGen2Activity.conditionalAbort(null);
                    }
                });
                pin2.setOnEditorActionListener(new OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                            ok.performClick();
                        }
                        return false;
                    }
                });
            } else {
                hideSoftKeyBoard();
                keyGen2Activity.findViewById(R.id.primaryWindow).setVisibility(View.INVISIBLE);
                new KeyGen2KeyCreation(keyGen2Activity).execute();
            }
        }
    }

    public void hideSoftKeyBoard() {
        // Check if no view has focus:
        View view = this.keyGen2Activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) this.keyGen2Activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public KeyGen2SessionCreation(KeyGen2Activity keygen2_activity) {
        this.keyGen2Activity = keygen2_activity;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            publishProgress(BaseProxyActivity.PROGRESS_SESSION);

            DeviceInfo device_info = keyGen2Activity.sks.getDeviceInfo();
            InvocationResponseEncoder invocation_response = new InvocationResponseEncoder(keyGen2Activity.invocationRequest);

            if (keyGen2Activity.invocationRequest.getQueriedCapabilities().contains(KeyGen2URIs.LOGOTYPES.LIST)) {
                BitmapFactory.Options bmo = new BitmapFactory.Options();
                bmo.inScaled = false;
                Bitmap default_icon = BitmapFactory.decodeResource(keyGen2Activity.getResources(), R.drawable.certview_logo_na, bmo);
                default_icon.setDensity(Bitmap.DENSITY_NONE);
                invocation_response.addImagePreference(KeyGen2URIs.LOGOTYPES.LIST, "image/png", default_icon.getWidth(), default_icon.getHeight());
            }

            if (keyGen2Activity.invocationRequest.getQueriedCapabilities().contains(KeyGen2URIs.CLIENT_FEATURES.BIOMETRIC_SUPPORT)) {
                if (FingerprintManagerCompat.from(keyGen2Activity).isHardwareDetected()) {
                    invocation_response.addSupportedFeature(KeyGen2URIs.CLIENT_FEATURES.BIOMETRIC_SUPPORT);
                }
            }

            keyGen2Activity.postJSONData(keyGen2Activity.getTransactionURL(),
                                          invocation_response,
                                          BaseProxyActivity.RedirectPermitted.FORBIDDEN);

            keyGen2Activity.provInitRequest = (ProvisioningInitializationRequestDecoder) keyGen2Activity.parseJSONResponse();
            GregorianCalendar client_time = new GregorianCalendar();
            ProvisioningSession session =
                    keyGen2Activity.sks.createProvisioningSession(keyGen2Activity.provInitRequest.getSessionKeyAlgorithm(),
                                                                  keyGen2Activity.invocationRequest.getPrivacyEnabledFlag(),
                                                                  keyGen2Activity.provInitRequest.getServerSessionId(),
                                                                  keyGen2Activity.provInitRequest.getServerEphemeralKey(),
                                                                  keyGen2Activity.getTransactionURL(), // IssuerURI
                                                                  keyGen2Activity.provInitRequest.getKeyManagementKey(),
                                                                  (int) (client_time.getTimeInMillis() / 1000),
                                                                  keyGen2Activity.provInitRequest.getSessionLifeTime(),
                                                                  keyGen2Activity.provInitRequest.getSessionKeyLimit(),
                                                                  keyGen2Activity.getServerCertificate().getEncoded());

            keyGen2Activity.provisioningHandle = session.getProvisioningHandle();

            ProvisioningInitializationResponseEncoder prov_sess_response =
                    new ProvisioningInitializationResponseEncoder(keyGen2Activity.provInitRequest,
                                                                  session.getClientEphemeralKey(),
                                                                  session.getClientSessionId(),
                                                                  client_time,
                                                                  session.getAttestation(),
                                                                  keyGen2Activity.invocationRequest.getPrivacyEnabledFlag() ?
                                                                      null : device_info.getCertificatePath());

            keyGen2Activity.postJSONData(keyGen2Activity.getTransactionURL(),
                                          prov_sess_response,
                                          BaseProxyActivity.RedirectPermitted.FORBIDDEN);
            JSONDecoder jsonObject = keyGen2Activity.parseJSONResponse();
            if (jsonObject instanceof CredentialDiscoveryRequestDecoder) {
                publishProgress(BaseProxyActivity.PROGRESS_LOOKUP);

                CredentialDiscoveryRequestDecoder cred_disc_request = (CredentialDiscoveryRequestDecoder) jsonObject;
                CredentialDiscoveryResponseEncoder credDiscResponse = new CredentialDiscoveryResponseEncoder(cred_disc_request);
                for (CredentialDiscoveryRequestDecoder.LookupSpecifier ls : cred_disc_request.getLookupSpecifiers()) {
                    CredentialDiscoveryResponseEncoder.LookupResult lr = credDiscResponse.addLookupResult(ls.getID());
                    EnumeratedProvisioningSession eps = new EnumeratedProvisioningSession();
                    while ((eps = keyGen2Activity.sks.enumerateProvisioningSessions(eps.getProvisioningHandle(), false)) != null) {
                        if (ls.getKeyManagementKey().equals(eps.getKeyManagementKey()) &&
                                keyGen2Activity.invocationRequest.getPrivacyEnabledFlag() == eps.getPrivacyEnabled()) {
                            EnumeratedKey ek = new EnumeratedKey();
                            while ((ek = keyGen2Activity.sks.enumerateKeys(ek.getKeyHandle())) != null) {
                                if (ek.getProvisioningHandle() == eps.getProvisioningHandle()) {
                                    KeyAttributes ka = keyGen2Activity.sks.getKeyAttributes(ek.getKeyHandle());
                                    X509Certificate[] cert_path = ka.getCertificatePath();
                                    if (ls.matches(cert_path)) {
                                        KeyProtectionInfo kpi = keyGen2Activity.sks.getKeyProtectionInfo(ek.getKeyHandle());
                                        if ((ls.getGrouping() == null || ls.getGrouping() == kpi.getPinGrouping()) &&
                                            (ls.getAppUsage() == null || ls.getAppUsage() == ka.getAppUsage())) {
                                            lr.addMatchingCredential(cert_path,
                                                                     eps.getClientSessionId(),
                                                                     eps.getServerSessionId(),
                                                                     kpi.isPinBlocked());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                keyGen2Activity.postJSONData(keyGen2Activity.getTransactionURL(),
                                              credDiscResponse,
                                              BaseProxyActivity.RedirectPermitted.FORBIDDEN);
                jsonObject = keyGen2Activity.parseJSONResponse();
            }
            keyGen2Activity.keyCreationRequest = (KeyCreationRequestDecoder) jsonObject;
            return KeyGen2Activity.CONTINUE_EXECUTION;
        } catch (Exception e) {
            keyGen2Activity.logException(e);
        }
        return null;
    }

    @Override
    public void onProgressUpdate(String... message) {
        keyGen2Activity.showHeavyWork(message[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        if (keyGen2Activity.userHasAborted()) {
            return;
        }
        if (result != null) {
            if (result.equals(BaseProxyActivity.CONTINUE_EXECUTION)) {
                try {
                    ///////////////////////////////////////////////////////////////////////////
                    // Note: There may be zero PINs but the test in the constructor fixes that
                    ///////////////////////////////////////////////////////////////////////////
                    new PINDialog(keyGen2Activity.keyCreationRequest.getUserPINDescriptors().iterator());
                } catch (Exception e) {
                    keyGen2Activity.logException(e);
                    keyGen2Activity.showFailLog();
                }
            } else {
                keyGen2Activity.launchBrowser(result);
            }
        } else {
            keyGen2Activity.showFailLog();
        }
    }
}
