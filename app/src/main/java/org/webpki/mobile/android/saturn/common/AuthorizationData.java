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
package org.webpki.mobile.android.saturn.common;

import java.io.IOException;

import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.AsymKeySignerInterface;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.json.encryption.DataEncryptionAlgorithms;

public class AuthorizationData implements BaseProperties {

    public static final String SOFTWARE_ID      = "WebPKI.org - Android Wallet";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          String paymentMethod,
                                          String accountId,
                                          byte[] dataEncryptionKey,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          UserResponseItem[] optionalUserResponseItems,
                                          GregorianCalendar timeStamp,
                                          JSONAsymKeySigner signer) throws IOException {
        JSONObjectWriter wr = new JSONObjectWriter()
                .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                        .setString(JSONSignatureDecoder.ALGORITHM_JSON, RequestHash.JOSE_SHA_256_ALG_ID)
                        .setBinary(JSONSignatureDecoder.VALUE_JSON, paymentRequest.getRequestHash()))
                .setString(DOMAIN_NAME_JSON, domainName)
                .setString(PAYMENT_METHOD_JSON, paymentMethod)
                .setString(ACCOUNT_ID_JSON, accountId)
                .setObject(ENCRYPTION_PARAMETERS_JSON,
                        new JSONObjectWriter()
                                .setString(JSONSignatureDecoder.ALGORITHM_JSON, dataEncryptionAlgorithm.toString())
                                .setBinary(KEY_JSON, dataEncryptionKey));
        if (optionalUserResponseItems != null && optionalUserResponseItems.length > 0) {
            JSONArrayWriter aw = wr.setArray(USER_RESPONSE_ITEMS_JSON);
            for (UserResponseItem challengeResult : optionalUserResponseItems) {
                aw.setObject(challengeResult.writeObject());
            }
        }
        return wr.setDateTime(TIME_STAMP_JSON, timeStamp, false)
                .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
                .setSignature (signer);
    }

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          String paymentMethod,
                                          String accountId,
                                          byte[] dataEncryptionKey,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          UserResponseItem[] optionalUserResponseItems,
                                          AsymSignatureAlgorithms signatureAlgorithm,
                                          AsymKeySignerInterface signer) throws IOException {
        return encode(paymentRequest,
                domainName,
                paymentMethod,
                accountId,
                dataEncryptionKey,
                dataEncryptionAlgorithm,
                optionalUserResponseItems,
                new GregorianCalendar(),
                new JSONAsymKeySigner(signer)
                        .setSignatureAlgorithm(signatureAlgorithm)
                        .setAlgorithmPreferences(AlgorithmPreferences.JOSE));
    }

    public static String formatCardNumber(String accountId) {
        StringBuffer s = new StringBuffer();
        int q = 0;
        for (char c : accountId.toCharArray()) {
            if (q != 0 && q % 4 == 0) {
                s.append(' ');
            }
            s.append(c);
            q++;
        }
        return s.toString();
    }
 }
