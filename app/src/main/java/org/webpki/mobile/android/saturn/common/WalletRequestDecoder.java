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

import java.util.Vector;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONObjectReader;

public class WalletRequestDecoder extends JSONDecoder implements BaseProperties {

    private static final long serialVersionUID = 1L;

    public static class PaymentMethodDescriptor {
        public String paymentMethod;
        public HashAlgorithms keyHashAlgorithm;
        public byte[] keyHashValue;
    }

    public Vector<PaymentMethodDescriptor> paymentMethodList = new Vector<>();

    public String noMatchingMethodsUrl;

    public boolean gasStationPayment;

    public PaymentRequest paymentRequest;

    @Override
    protected void readJSONData(JSONObjectReader rd) throws IOException {
        JSONArrayReader methodList = rd.getArray(SUPPORTED_PAYMENT_METHODS_JSON);
        do {
            PaymentMethodDescriptor pmd = new PaymentMethodDescriptor();
            JSONObjectReader o = methodList.getObject();
            pmd.paymentMethod = o.getString(PAYMENT_METHOD_JSON);
            o = o.getObject(KEY_HASH_JSON);
            pmd.keyHashAlgorithm =
                CryptoUtils.getHashAlgorithm(o, JSONCryptoHelper.ALGORITHM_JSON);
            pmd.keyHashValue = o.getBinary(JSONCryptoHelper.VALUE_JSON);
            paymentMethodList.add(pmd);
        } while (methodList.hasMore());
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        gasStationPayment = paymentRequest.getNonDirectPayment() == NonDirectPayments.GAS_STATION;
        noMatchingMethodsUrl = rd.getStringConditional(NO_MATCHING_METHODS_URL_JSON);
    }

    @Override
    public String getContext() {
        return SATURN_WEB_PAY_CONTEXT_URI;
    }

    @Override
    public String getQualifier() {
        return Messages.PAYMENT_CLIENT_REQUEST.toString ();
    }
}
