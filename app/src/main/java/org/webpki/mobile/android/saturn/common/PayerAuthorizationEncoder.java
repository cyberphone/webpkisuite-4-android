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
package org.webpki.mobile.android.saturn.common;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.ContentEncryptionAlgorithms;
import org.webpki.crypto.KeyEncryptionAlgorithms;

import org.webpki.json.JSONEncoder;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONAsymKeyEncrypter;

import org.webpki.util.Base64URL;

public class PayerAuthorizationEncoder extends JSONEncoder implements BaseProperties {
    
    private static final long serialVersionUID = 1L;

    String providerAuthorityUrl;

    String paymentMethod;

    JSONObjectWriter encryptedData;

    public PayerAuthorizationEncoder(JSONObjectWriter unencryptedAuthorizationData,
                                     String providerAuthorityUrl,
                                     String paymentMethod,
                                     ContentEncryptionAlgorithms dataEncryptionAlgorithm,
                                     PublicKey keyEncryptionKey,
                                     String optionalKeyId,
                                     KeyEncryptionAlgorithms keyEncryptionAlgorithm)
    throws GeneralSecurityException, IOException {
        this.providerAuthorityUrl = providerAuthorityUrl;
        this.paymentMethod = paymentMethod;
        this.encryptedData =
            JSONObjectWriter.createEncryptionObject(
                    unencryptedAuthorizationData.serializeToBytes(JSONOutputFormats.NORMALIZED),
                    dataEncryptionAlgorithm,
                    new JSONAsymKeyEncrypter(keyEncryptionKey,
                                             keyEncryptionAlgorithm).setKeyId(optionalKeyId));
    }

    @Override
    protected void writeJSONData(JSONObjectWriter wr) throws IOException {
        wr.setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
          .setString(PAYMENT_METHOD_JSON, paymentMethod)
          .setObject(ENCRYPTED_AUTHORIZATION_JSON, encryptedData);
    }

    public String getAuthorizationHash() throws IOException, GeneralSecurityException {
        return Base64URL.encode(
            HashAlgorithms.SHA256.digest(
                encryptedData.serializeToBytes(JSONOutputFormats.CANONICALIZED)));
    }

    @Override
    public String getContext() {
        return SATURN_WEB_PAY_CONTEXT_URI;
    }

    @Override
    public String getQualifier() {
        return Messages.PAYER_AUTHORIZATION.toString();
    }
}
