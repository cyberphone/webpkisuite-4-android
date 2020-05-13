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

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.HashAlgorithms;
import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.mobile.android.saturn.common.Currencies;

import java.math.BigDecimal;

import java.security.PublicKey;

public class Account {
    String paymentMethod;
    String payeeAuthorityUrl;
    HashAlgorithms requestHashAlgorithm;
    String credentialId;
    String accountId;
    Currencies currency;
    String authorityUrl;
    byte[] cardImage;
    AsymSignatureAlgorithms signatureAlgorithm;
    int signatureKeyHandle;
    DataEncryptionAlgorithms dataEncryptionAlgorithm;
    KeyEncryptionAlgorithms keyEncryptionAlgorithm;
    PublicKey encryptionKey;
    String optionalKeyId;
    Integer optionalBalanceKeyHandle;

    // External state variables
    boolean balanceRequestIsRunning;
    boolean balanceRequestIsReady;
    public String balance; // Money string or null if not ready or if failed

    Account(// The core...
            String paymentMethod,
            String payeeAuthorityUrl,
            HashAlgorithms requestHashAlgorithm,
            String credentialId,
            String accountId,
            Currencies currency,
            String authorityUrl,
            // Card visuals
            byte[] cardImage,
            // Signature
            int signatureKeyHandle,
            AsymSignatureAlgorithms signatureAlgorithm,
            // Encryption
            KeyEncryptionAlgorithms keyEncryptionAlgorithm,
            DataEncryptionAlgorithms dataEncryptionAlgorithm,
            PublicKey encryptionKey,
            // Not used in the current server PoC
            String optionalKeyId,
            // Non-null if the account supports balance requests
            Integer optionalBalanceKeyHandle) {
        this.paymentMethod = paymentMethod;
        this.payeeAuthorityUrl = payeeAuthorityUrl;
        this.requestHashAlgorithm = requestHashAlgorithm;
        this.credentialId = credentialId;
        this.accountId = accountId;
        this.currency = currency;
        this.authorityUrl = authorityUrl;
        this.cardImage = cardImage;
        this.signatureKeyHandle = signatureKeyHandle;
        this.signatureAlgorithm = signatureAlgorithm;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.dataEncryptionAlgorithm = dataEncryptionAlgorithm;
        this.encryptionKey = encryptionKey;
        this.optionalKeyId = optionalKeyId;
        this.optionalBalanceKeyHandle = optionalBalanceKeyHandle;
    }
}
