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

import java.io.IOException;

import java.net.URL;

import android.os.AsyncTask;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.json.JSONSignatureDecoder;

import org.webpki.json.encryption.DataEncryptionAlgorithms;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

import org.webpki.keygen2.KeyGen2URIs;

import org.webpki.mobile.android.saturn.SaturnActivity.Account;

import org.webpki.mobile.android.saturn.common.BaseProperties;
import org.webpki.mobile.android.saturn.common.ProviderUserResponseDecoder;
import org.webpki.mobile.android.saturn.common.WalletAlertDecoder;
import org.webpki.mobile.android.saturn.common.WalletRequestDecoder;
import org.webpki.mobile.android.saturn.common.WalletSuccessDecoder;

import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.Extension;
import org.webpki.sks.SKSException;
import org.webpki.sks.SecureKeyStore;

public class SaturnProtocolInit extends AsyncTask<Void, String, Boolean> {
    private SaturnActivity saturnActivity;

    public SaturnProtocolInit (SaturnActivity saturnActivity) {
        this.saturnActivity = saturnActivity;
    }

    @Override
    protected Boolean doInBackground (Void... params) {
        try {
            saturnActivity.getProtocolInvocationData();
            saturnActivity.addDecoder(WalletRequestDecoder.class);
            saturnActivity.addDecoder(WalletSuccessDecoder.class);
            saturnActivity.addDecoder(WalletAlertDecoder.class);
            saturnActivity.addDecoder(ProviderUserResponseDecoder.class);
            saturnActivity.walletRequest = (WalletRequestDecoder) saturnActivity.getInitialReguest();
            saturnActivity.setAbortURL(saturnActivity.walletRequest.getAndroidCancelUrl());

            // Enumerate keys but only go for those who are intended for
            // Web Payments (according to our fictitious payment schemes...)
            EnumeratedKey ek = new EnumeratedKey();
            while ((ek = saturnActivity.sks.enumerateKeys(ek.getKeyHandle())) != null) {
                Extension ext = null;
                try {
                    ext = saturnActivity.sks.getExtension(ek.getKeyHandle(),
                                                          BaseProperties.SATURN_WEB_PAY_CONTEXT_URI);
                } catch (SKSException e) {
                    if (e.getError() == SKSException.ERROR_OPTION) {
                        continue;
                    }
                    throw new Exception(e);
                }

                // This key had the attribute signifying that it is a payment credential
                // for the fictitious payment schemes this system is supporting but it
                // might still not match the Payee's list of supported account types.
                collectPotentialCard(ek.getKeyHandle(),
                                     JSONParser.parse(ext.getExtensionData(SecureKeyStore.SUB_TYPE_EXTENSION)),
                                     saturnActivity.walletRequest);
            }
            return true;
        } catch (Exception e) {
            saturnActivity.logException(e);
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (saturnActivity.userHasAborted() || saturnActivity.initWasRejected()) {
            return;
        }
        saturnActivity.noMoreWorkToDo();
        if (success) {
            saturnActivity.setTitle("Requester: " + saturnActivity.getRequestingHost());
            try {
                if (saturnActivity.cardCollection.isEmpty()) {
                    URL url = new URL(saturnActivity.getInitializationURL());
                    String host = url.getHost();
                    if (host.equals("test.webpki.org")) {
                        host = "mobilepki.org";
                    }
                    String modifiedUrl = new URL(url.getProtocol(), host, url.getPort(), "webpay-keyprovider").toExternalForm();
                    saturnActivity.simpleDisplay(
                        "You do not seem to have any payment cards." +
                        "<p>For a selection of test cards, you can enroll such at the Saturn " +
                        "proof-of-concept site: <span style='white-space:nowrap'><a style='font-size:10pt;font-weight:bold;text-decoration:none;color:blue' href='" +
                        modifiedUrl + "' target='_blank'>" + modifiedUrl + "</a>.</span></p>");
                } else if (saturnActivity.cardCollection.size () == 1) {
                    saturnActivity.selectCard("0");
                } else {
                    saturnActivity.showCardCollection();
                }
            } catch (IOException e){
                saturnActivity.logException(e);
                saturnActivity.showFailLog();
            }
        } else {
            saturnActivity.showFailLog();
        }
    }

    void collectPotentialCard(int keyHandle, JSONObjectReader cardProperties, WalletRequestDecoder wrd) throws IOException {
        String paymentMethod = cardProperties.getString(BaseProperties.PAYMENT_METHOD_JSON);
        for (WalletRequestDecoder.PaymentNetwork paymentNetwork : wrd.getPaymentNetworks()) {
            for (String acceptedPaymentMethod : paymentNetwork.getPaymentMethods()) {
                if (paymentMethod.equals(acceptedPaymentMethod)) {
                    Account card =
                        new Account(paymentNetwork.getPaymentRequest(),
                                    paymentMethod,
                                    cardProperties.getString(BaseProperties.ACCOUNT_ID_JSON),
                                    cardProperties.getBoolean(BaseProperties.CARD_FORMAT_ACCOUNT_ID_JSON),
                                    saturnActivity.sks.getExtension(keyHandle, KeyGen2URIs.LOGOTYPES.CARD)
                                        .getExtensionData(SecureKeyStore.SUB_TYPE_LOGOTYPE),
                                    keyHandle,
                                    AsymSignatureAlgorithms
                                        .getAlgorithmFromId(cardProperties.getString(BaseProperties.SIGNATURE_ALGORITHM_JSON),
                                                            AlgorithmPreferences.JOSE),
                                    cardProperties.getString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON));
                    JSONObjectReader encryptionParameters = cardProperties.getObject(BaseProperties.ENCRYPTION_PARAMETERS_JSON);
                    card.optionalKeyId = encryptionParameters.getStringConditional(JSONSignatureDecoder.KEY_ID_JSON);
                    card.keyEncryptionAlgorithm = KeyEncryptionAlgorithms
                        .getAlgorithmFromId(encryptionParameters.getString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON));
                    card.dataEncryptionAlgorithm = DataEncryptionAlgorithms
                        .getAlgorithmFromId(encryptionParameters.getString (BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON));
                    card.keyEncryptionKey = encryptionParameters.getPublicKey(AlgorithmPreferences.JOSE);
    
                    // We found a useful card!
                    saturnActivity.cardCollection.add(card);
                    break;
                }
            }
        }
    }
}
