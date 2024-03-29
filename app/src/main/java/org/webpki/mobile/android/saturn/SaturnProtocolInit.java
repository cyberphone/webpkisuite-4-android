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
import java.security.GeneralSecurityException;
import java.util.Arrays;

import android.os.AsyncTask;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.keygen2.KeyGen2URIs;

import org.webpki.mobile.android.saturn.common.BaseProperties;
import org.webpki.mobile.android.saturn.common.CardDataDecoder;
import org.webpki.mobile.android.saturn.common.ProviderUserResponseDecoder;
import org.webpki.mobile.android.saturn.common.WalletAlertDecoder;
import org.webpki.mobile.android.saturn.common.WalletRequestDecoder;

import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.Extension;
import org.webpki.sks.KeyAttributes;
import org.webpki.sks.SecureKeyStore;

import org.webpki.util.ArrayUtil;

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
        if (saturnActivity.userHasAborted()) {
            return;
        }
        saturnActivity.noMoreWorkToDo();
        if (success) {
            saturnActivity.setTitle(saturnActivity.localTesting ?
                    (saturnActivity.walletRequest.paymentRequest.getPayeeCommonName()
                        .equals("Planet Gas") ? "planetgas.com" : "spaceshop.com")
                    : saturnActivity.getRequestingHost());
            try {
                if (saturnActivity.accountCollection.isEmpty()) {
                    String noMatchingMethodsUrl = saturnActivity.walletRequest.noMatchingMethodsUrl;
                    saturnActivity.messageDisplay("",
                        "You do not seem to have any matching payment cards." +
                                (noMatchingMethodsUrl == null ? "" :
                        "<div style='padding:10pt 0 2pt 0'>To remedy this situation, <b>" +
                         saturnActivity.getRequestingHost() + "</b> suggests browsing to:" +
                        "</div><div style='word-break:break-all;color:blue' " +
                         "onclick=\"Saturn.launchLink('" +
                        noMatchingMethodsUrl + "')\">" + noMatchingMethodsUrl + "</div>"));
                } else {
                    saturnActivity.showPaymentRequest();;
                }
            } catch (IOException e){
                saturnActivity.logException(e);
                saturnActivity.showFailLog();
            }
        } else {
            saturnActivity.showFailLog();
        }
    }

    EnumeratedKey collectPotentialAccount(EnumeratedKey signatureKey,
                                          CardDataDecoder cardData,
                                          WalletRequestDecoder wrd)
            throws IOException, GeneralSecurityException {
        if (cardData.isRecognized()) {
            String paymentMethod = cardData.getPaymentMethod();
            for (WalletRequestDecoder.PaymentMethodDescriptor acceptedPaymentMethod
                                       : wrd.paymentMethodList) {
                if (paymentMethod.equals(acceptedPaymentMethod.paymentMethod)) {
                    byte[] hash = cardData.getOptionalAccountStatusKeyHash();
                    Integer balanceKeyHandle = null;
                    if (hash != null) {
                        EnumeratedKey ek = (EnumeratedKey)signatureKey.clone();
                        while ((ek = saturnActivity.sks.enumerateKeys(ek.getKeyHandle())) != null) {
                            balanceKeyHandle = ek.getKeyHandle();
                            if (Arrays.equals(hash, HashAlgorithms.SHA256.digest(
                                saturnActivity.sks.getKeyAttributes(balanceKeyHandle)
                                    .getCertificatePath()[0].getPublicKey().getEncoded()))) {
                                break;
                            }
                        }
                        if (balanceKeyHandle == null) {
                            throw new IOException("Missing balanceKey h=" + signatureKey.getKeyHandle());
                        }
                    }
                    Account account = new Account(
                            paymentMethod,
                            acceptedPaymentMethod.payeeAuthorityUrl,
                            cardData.getRequestHashAlgorithm(),
                            cardData.getCredentialId(),
                            cardData.getAccountId(),
                            cardData.getCurrency(),
                            cardData.getAuthorityUrl(),
                            // Card visuals
                            saturnActivity.sks.getExtension(
                                    signatureKey.getKeyHandle(),
                                    KeyGen2URIs.LOGOTYPES.CARD)
                                        .getExtensionData(SecureKeyStore.SUB_TYPE_LOGOTYPE),
                            // Signature
                            signatureKey.getKeyHandle(),
                            cardData.getSignatureAlgorithm(),
                            // Encryption
                            cardData.getKeyEncryptionAlgorithm(),
                            cardData.getContentEncryptionAlgorithm(),
                            cardData.getEncryptionKey(),
                            cardData.getOptionalKeyId(),
                            saturnActivity.sks.getKeyProtectionInfo(
                                    signatureKey.getKeyHandle()).getBiometricProtection(),
                            balanceKeyHandle
                    );

                    // We found an applicable account!
                    if (signatureKey.getKeyHandle() == saturnActivity.lastKeyId) {
                        // Is it our favorite account?
                        saturnActivity.selectedCard = saturnActivity.accountCollection.size();
                    }
                    saturnActivity.accountCollection.add(account);
                    break;
                }
            }
        }
        return signatureKey;
    }
}
