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
package org.webpki.mobile.android.saturn.common;

import java.io.IOException;

import java.util.GregorianCalendar;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.AsymKeySignerInterface;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.DataEncryptionAlgorithms;

import org.webpki.util.ISODateTime;

public class AuthorizationDataEncoder implements BaseProperties {

    public static final String SOFTWARE_ID      = "WebPKI.org - Wallet";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(PaymentRequestDecoder paymentRequest,
                                          HashAlgorithms reguestHashAlgorithm,
                                          String payeeAuthorityUrl,
                                          String payeeHost,
                                          String paymentMethodUrl,
                                          String credentialId,
                                          String accountId,
                                          byte[] dataEncryptionKey,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          UserResponseItem[] optionalUserResponseItems,
                                          GregorianCalendar timeStamp,
                                          ClientPlatform clientPlatform,
                                          JSONAsymKeySigner signer) throws IOException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                .setString(JSONCryptoHelper.ALGORITHM_JSON, 
                           reguestHashAlgorithm.getJoseAlgorithmId())
                .setBinary(JSONCryptoHelper.VALUE_JSON, 
                           paymentRequest.getRequestHash(reguestHashAlgorithm)))
            .setString(PAYEE_AUTHORITY_URL_JSON, payeeAuthorityUrl)
            .setString(PAYEE_HOST_JSON, payeeHost)
            .setString(PAYMENT_METHOD_JSON, paymentMethodUrl)
            .setString(CREDENTIAL_ID_JSON, credentialId)
            .setString(ACCOUNT_ID_JSON, accountId)
            .setObject(ENCRYPTION_PARAMETERS_JSON, new JSONObjectWriter()
                .setString(JSONCryptoHelper.ALGORITHM_JSON, dataEncryptionAlgorithm.toString())
                .setBinary(ENCRYPTION_KEY_JSON, dataEncryptionKey));
        if (optionalUserResponseItems != null && optionalUserResponseItems.length > 0) {
            JSONArrayWriter aw = wr.setArray(USER_RESPONSE_ITEMS_JSON);
            for (UserResponseItem challengeResult : optionalUserResponseItems) {
                aw.setObject(challengeResult.writeObject());
            }
        }
        return wr.setDateTime(TIME_STAMP_JSON, timeStamp, ISODateTime.LOCAL_NO_SUBSECONDS)
                 .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
                 .setObject(PLATFORM_JSON, new JSONObjectWriter()
                     .setString(NAME_JSON, clientPlatform.name)
                     .setString(VERSION_JSON, clientPlatform.version)
                     .setString(VENDOR_JSON, clientPlatform.vendor))
                 .setSignature(AUTHORIZATION_SIGNATURE_JSON, signer);
    }

   public static JSONObjectWriter encode(PaymentRequestDecoder paymentRequest,
                                          HashAlgorithms requestHashAlgorithm,
                                          String payeeAuthorityUrl,
                                          String payeeHost,
                                          String paymentMethod,
                                          String credentialId,
                                          String accountId,
                                          byte[] dataEncryptionKey,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          UserResponseItem[] optionalUserResponseItems,
                                          AsymSignatureAlgorithms signatureAlgorithm,
                                          ClientPlatform clientPlatform,
                                          AsymKeySignerInterface signer) throws IOException {
        return encode(paymentRequest,
                      requestHashAlgorithm,
                      payeeAuthorityUrl,
                      payeeHost,
                      paymentMethod,
                      credentialId,
                      accountId,
                      dataEncryptionKey,
                      dataEncryptionAlgorithm,
                      optionalUserResponseItems,
                      new GregorianCalendar(),
                      clientPlatform,
                      new JSONAsymKeySigner(signer).setSignatureAlgorithm(signatureAlgorithm));
    }
}