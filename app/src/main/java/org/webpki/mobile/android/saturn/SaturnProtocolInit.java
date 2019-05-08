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

import org.webpki.keygen2.KeyGen2URIs;

import org.webpki.mobile.android.saturn.SaturnActivity.Account;

import org.webpki.mobile.android.saturn.common.BaseProperties;
import org.webpki.mobile.android.saturn.common.CardDataDecoder;
import org.webpki.mobile.android.saturn.common.ProviderUserResponseDecoder;
import org.webpki.mobile.android.saturn.common.WalletAlertDecoder;
import org.webpki.mobile.android.saturn.common.WalletRequestDecoder;

import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.Extension;
import org.webpki.sks.KeyAttributes;
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
            saturnActivity.addDecoder(WalletAlertDecoder.class);
            saturnActivity.addDecoder(ProviderUserResponseDecoder.class);
            saturnActivity.walletRequest = (WalletRequestDecoder) saturnActivity.getInitialRequest();

            // Enumerate keys but only go for those who are intended for
            // Web Payments (according to our fictitious payment schemes...)
            EnumeratedKey ek = new EnumeratedKey();
            while ((ek = saturnActivity.sks.enumerateKeys(ek.getKeyHandle())) != null) {
                KeyAttributes ka = saturnActivity.sks.getKeyAttributes(ek.getKeyHandle());
                if (!ka.getExtensionTypes().contains(BaseProperties.SATURN_WEB_PAY_CONTEXT_URI)) {
                    continue;
                }
                Extension ext =
                        saturnActivity.sks.getExtension(ek.getKeyHandle(),
                                                        BaseProperties.SATURN_WEB_PAY_CONTEXT_URI);

                // This key had the attribute signifying that it is a payment credential
                // for the fictitious payment schemes this system is supporting but it
                // might still not match the Payee's list of supported account types.
                ek = collectPotentialAccount(ek,
                                             new CardDataDecoder(ext.getExtensionData(
                                                         SecureKeyStore.SUB_TYPE_EXTENSION)),
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
                if (saturnActivity.accountCollection.isEmpty()) {
                    URL url = new URL(saturnActivity.getTransactionURL());
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
                } else if (saturnActivity.accountCollection.size () == 1) {
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

    EnumeratedKey collectPotentialAccount(EnumeratedKey foundKey,
                                          CardDataDecoder cardData,
                                          WalletRequestDecoder wrd) throws IOException {
        String paymentMethod = cardData.getPaymentMethod();
        for (WalletRequestDecoder.PaymentNetwork paymentNetwork : wrd.getPaymentNetworks()) {
            for (String acceptedPaymentMethod : paymentNetwork.getPaymentMethods()) {
                if (paymentMethod.equals(acceptedPaymentMethod)) {
                    Account account = new Account(
                        paymentNetwork.getPaymentRequest(),
                        cardData.getPaymentMethod(),
                        cardData.getAccountId(),
                        cardData.getAuthorityUrl(),
                        // Card visuals
                        true,
                        new String(saturnActivity.sks.getExtension(foundKey.getKeyHandle(),
                                                        KeyGen2URIs.LOGOTYPES.CARD)
                           .getExtensionData(SecureKeyStore.SUB_TYPE_LOGOTYPE), "utf-8"),
                        // Signature
                        foundKey.getKeyHandle(),
                        cardData.getSignatureAlgorithm(),
                        // Encryption
                        cardData.getKeyEncryptionAlgorithm(),
                        cardData.getDataEncryptionAlgorithm(),
                        cardData.getEncryptionKey(),
                        cardData.getOptionalKeyId(),
                        cardData.getTempBalanceFix()
                    );
                    byte[] hash = cardData.getOptionalAccountStatusKeyHash();

                    // We found an applicable account!
                    saturnActivity.accountCollection.add(account);
                    break;
                }
            }
        }
        return foundKey;
    }
}
