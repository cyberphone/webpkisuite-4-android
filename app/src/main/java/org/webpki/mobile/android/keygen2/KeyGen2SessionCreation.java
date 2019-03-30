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
package org.webpki.mobile.android.keygen2;

import java.io.IOException;

import java.security.cert.X509Certificate;

import java.util.GregorianCalendar;
import java.util.Iterator;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

import org.webpki.mobile.android.proxy.BaseProxyActivity;
import org.webpki.mobile.android.proxy.InterruptedProtocolException;

import org.webpki.mobile.android.R;

import org.webpki.crypto.MACAlgorithms;
import org.webpki.crypto.SymKeySignerInterface;

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
    private KeyGen2Activity keygen2_activity;

    private int pin_count;

    private class PINDialog {
        private EditText pin1;
        private EditText pin2;
        private TextView pin_err;
        private boolean equal_pins;

        KeyCreationRequestDecoder.UserPINDescriptor upd;

        private void upperCasePIN(EditText pin) {
            InputFilter[] old_filter = pin.getEditableText().getFilters();
            InputFilter[] new_filter = new InputFilter[old_filter.length + 1];
            for (int i = 0; i < old_filter.length; i++) {
                new_filter[i] = old_filter[i];
            }
            new_filter[old_filter.length] = new InputFilter.AllCaps();
            pin.getEditableText().setFilters(new_filter);
        }

        private boolean checkPIN(boolean set_value) {
            String pin = pin1.getText().toString();
            equal_pins = pin1.getText().toString().equals(pin2.getText().toString());
            KeyCreationRequestDecoder.UserPINError res = upd.setPIN(pin, set_value && equal_pins);
            if (res == null) {
                pin_err.setText("");
                return true;
            } else {
                KeyCreationRequestDecoder.PINPolicy pin_policy = upd.getPINPolicy();
                String error = "PIN syntax error";
                if (res.lengthError) {
                    int multiplier = pin_policy.getFormat() == PassphraseFormat.BINARY ? 2 : 1;
                    error = "PINs must be " + (pin_policy.getMinLength() * multiplier) + "-" + (pin_policy.getMaxLength() * multiplier) +
                            (pin_policy.getFormat() == PassphraseFormat.NUMERIC ? " digits" : " characters");
                } else if (res.syntaxError) {
                    switch (pin_policy.getFormat()) {
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
                            error = "PINs must be a mix of A-Z " + (pin_policy.getFormat() == PassphraseFormat.ALPHANUMERIC ?
                                    "0-9" : "a-z 0-9\nand control characters");
                            break;
                    }
                } else if (res.uniqueError) {
                    error = "PINs for " + upd.getAppUsage().getProtocolName() + " and " + res.uniqueErrorAppUsage.getProtocolName() + " must not be equal";
                }
                pin_err.setText(error);
            }
            return false;
        }

        PINDialog(final Iterator<KeyCreationRequestDecoder.UserPINDescriptor> iter) {
            if (iter.hasNext()) {
                keygen2_activity.noMoreWorkToDo();
                upd = iter.next();
                keygen2_activity.setContentView(R.layout.activity_keygen2_pin);

                final Button ok = (Button) keygen2_activity.findViewById(R.id.OKbutton);
                Button cancel = (Button) keygen2_activity.findViewById(R.id.cancelButton);

                pin1 = (EditText) keygen2_activity.findViewById(R.id.editpin1);
                pin2 = (EditText) keygen2_activity.findViewById(R.id.editpin2);
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
                pin_err = (TextView) keygen2_activity.findViewById(R.id.errorPIN);
                TextView set_pin_text = (TextView) keygen2_activity.findViewById(R.id.setPINtext);
                StringBuffer lead_text = new StringBuffer("Set ");
                if (upd.getPINPolicy().getGrouping() == Grouping.SIGNATURE_PLUS_STANDARD) {
                    lead_text.append(upd.getAppUsage() == AppUsage.SIGNATURE ? "signature " : "standard ");
                } else if (upd.getPINPolicy().getGrouping() == Grouping.UNIQUE) {
                    if (upd.getAppUsage() != AppUsage.UNIVERSAL) {
                        lead_text.append(upd.getAppUsage().getProtocolName());
                        lead_text.append(' ');
                    }
                }
                lead_text.append("PIN");
                if (keygen2_activity.key_creation_request.getUserPINDescriptors().size() > 1) {
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
                            if (equal_pins) {
                                new PINDialog(iter);
                            } else {
                                keygen2_activity.showAlert("The retyped PIN doesn't match the original");
                            }
                        } else {
                            keygen2_activity.showAlert("Please correct PIN");
                        }
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        keygen2_activity.conditionalAbort(null);
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
                keygen2_activity.findViewById(R.id.primaryWindow).setVisibility(View.INVISIBLE);
                new KeyGen2KeyCreation(keygen2_activity).execute();
            }
        }
    }

    public void hideSoftKeyBoard() {
        // Check if no view has focus:
        View view = this.keygen2_activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) this.keygen2_activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public KeyGen2SessionCreation(KeyGen2Activity keygen2_activity) {
        this.keygen2_activity = keygen2_activity;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            publishProgress(BaseProxyActivity.PROGRESS_SESSION);

            DeviceInfo device_info = keygen2_activity.sks.getDeviceInfo();
            InvocationResponseEncoder invocation_response = new InvocationResponseEncoder(keygen2_activity.invocation_request);

            if (keygen2_activity.invocation_request.getQueriedCapabilities().contains(KeyGen2URIs.LOGOTYPES.LIST)) {
                BitmapFactory.Options bmo = new BitmapFactory.Options();
                bmo.inScaled = false;
                Bitmap default_icon = BitmapFactory.decodeResource(keygen2_activity.getResources(), R.drawable.certview_logo_na, bmo);
                default_icon.setDensity(Bitmap.DENSITY_NONE);
                invocation_response.addImagePreference(KeyGen2URIs.LOGOTYPES.LIST, "image/png", default_icon.getWidth(), default_icon.getHeight());
            }

            keygen2_activity.postJSONData(keygen2_activity.getTransactionURL(),
                                          invocation_response,
                                          BaseProxyActivity.RedirectPermitted.FORBIDDEN);

            keygen2_activity.prov_init_request = (ProvisioningInitializationRequestDecoder) keygen2_activity.parseJSONResponse();
            GregorianCalendar client_time = new GregorianCalendar();
            ProvisioningSession session =
                    keygen2_activity.sks.createProvisioningSession(keygen2_activity.prov_init_request.getSessionKeyAlgorithm(),
                                                                   keygen2_activity.invocation_request.getPrivacyEnabledFlag(),
                                                                   keygen2_activity.prov_init_request.getServerSessionId(),
                                                                   keygen2_activity.prov_init_request.getServerEphemeralKey(),
                                                                   keygen2_activity.getTransactionURL(), // IssuerURI
                                                                   keygen2_activity.prov_init_request.getKeyManagementKey(),
                                                                   (int) (client_time.getTimeInMillis() / 1000),
                                                                   keygen2_activity.prov_init_request.getSessionLifeTime(),
                                                                   keygen2_activity.prov_init_request.getSessionKeyLimit());

            keygen2_activity.provisioning_handle = session.getProvisioningHandle();

            ProvisioningInitializationResponseEncoder prov_sess_response =
                    new ProvisioningInitializationResponseEncoder(keygen2_activity.prov_init_request,
                                                                  session.getClientEphemeralKey(),
                                                                  session.getClientSessionId(),
                                                                  client_time,
                                                                  session.getAttestation(),
                                                                  keygen2_activity.getServerCertificate(),
                                                                  keygen2_activity.invocation_request.getPrivacyEnabledFlag() ?
                                                                      null : device_info.getCertificatePath());

            prov_sess_response.setResponseSigner(new SymKeySignerInterface() {
                @Override
                public byte[] signData(byte[] data, MACAlgorithms algorithm) throws IOException {
                    return keygen2_activity.sks.signProvisioningSessionData(keygen2_activity.provisioning_handle, data);
                }

                @Override
                public MACAlgorithms getMacAlgorithm() throws IOException {
                    return MACAlgorithms.HMAC_SHA256;
                }
            });

            keygen2_activity.postJSONData(keygen2_activity.getTransactionURL(),
                                          prov_sess_response,
                                          BaseProxyActivity.RedirectPermitted.FORBIDDEN);
            JSONDecoder json_object = keygen2_activity.parseJSONResponse();
            if (json_object instanceof CredentialDiscoveryRequestDecoder) {
                publishProgress(BaseProxyActivity.PROGRESS_LOOKUP);

                CredentialDiscoveryRequestDecoder cred_disc_request = (CredentialDiscoveryRequestDecoder) json_object;
                CredentialDiscoveryResponseEncoder cred_disc_response = new CredentialDiscoveryResponseEncoder(cred_disc_request);
                for (CredentialDiscoveryRequestDecoder.LookupSpecifier ls : cred_disc_request.getLookupSpecifiers()) {
                    CredentialDiscoveryResponseEncoder.LookupResult lr = cred_disc_response.addLookupResult(ls.getID());
                    EnumeratedProvisioningSession eps = new EnumeratedProvisioningSession();
                    while ((eps = keygen2_activity.sks.enumerateProvisioningSessions(eps.getProvisioningHandle(), false)) != null) {
                        if (ls.getKeyManagementKey().equals(eps.getKeyManagementKey()) &&
                                keygen2_activity.invocation_request.getPrivacyEnabledFlag() == eps.getPrivacyEnabled()) {
                            EnumeratedKey ek = new EnumeratedKey();
                            while ((ek = keygen2_activity.sks.enumerateKeys(ek.getKeyHandle())) != null) {
                                if (ek.getProvisioningHandle() == eps.getProvisioningHandle()) {
                                    KeyAttributes ka = keygen2_activity.sks.getKeyAttributes(ek.getKeyHandle());
                                    X509Certificate[] cert_path = ka.getCertificatePath();
                                    if (ls.matches(cert_path)) {
                                        KeyProtectionInfo kpi = keygen2_activity.sks.getKeyProtectionInfo(ek.getKeyHandle());
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
                keygen2_activity.postJSONData(keygen2_activity.getTransactionURL(),
                                              cred_disc_response,
                                              BaseProxyActivity.RedirectPermitted.FORBIDDEN);
                json_object = keygen2_activity.parseJSONResponse();
            }
            keygen2_activity.key_creation_request = (KeyCreationRequestDecoder) json_object;
            return KeyGen2Activity.CONTINUE_EXECUTION;
        } catch (InterruptedProtocolException e) {
            return keygen2_activity.getRedirectURL();
        } catch (Exception e) {
            keygen2_activity.logException(e);
        }
        return null;
    }

    @Override
    public void onProgressUpdate(String... message) {
        keygen2_activity.showHeavyWork(message[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        if (keygen2_activity.userHasAborted()) {
            return;
        }
        if (result != null) {
            if (result.equals(BaseProxyActivity.CONTINUE_EXECUTION)) {
                try {
                    ///////////////////////////////////////////////////////////////////////////
                    // Note: There may be zero PINs but the test in the constructor fixes that
                    ///////////////////////////////////////////////////////////////////////////
                    new PINDialog(keygen2_activity.key_creation_request.getUserPINDescriptors().iterator());
                } catch (Exception e) {
                    keygen2_activity.logException(e);
                    keygen2_activity.showFailLog();
                }
            } else {
                keygen2_activity.launchBrowser(result);
            }
        } else {
            keygen2_activity.showFailLog();
        }
    }
}
