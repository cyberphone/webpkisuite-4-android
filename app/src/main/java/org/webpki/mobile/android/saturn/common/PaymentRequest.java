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

import java.math.BigDecimal;

import java.security.PublicKey;

import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class PaymentRequest implements BaseProperties {

    public PaymentRequest(JSONObjectReader rd) throws IOException {
        root = rd;
        payee = new Payee(rd.getObject(PAYEE_JSON));
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        if (rd.hasProperty(NON_DIRECT_PAYMENT_JSON)) {
            nonDirectPayment = NonDirectPayments.fromType(rd.getString(NON_DIRECT_PAYMENT_JSON));
        }
        amount = rd.getBigDecimal(AMOUNT_JSON, currency.getDecimals());
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        expires = rd.getDateTime(EXPIRES_JSON);
        software = new Software(rd);
        publicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        rd.checkForUnread();
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    Payee payee;
    public Payee getPayee() {
        return payee;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }

    NonDirectPayments nonDirectPayment;
    public NonDirectPayments getNonDirectPayment() {
        return nonDirectPayment;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }
    
    GregorianCalendar dateTime;
    public GregorianCalendar getDateTime() {
        return dateTime;
    }

    Software software;
    public Software getSoftware() {
        return software;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }

    JSONObjectReader root;

    public byte[] getRequestHash() throws IOException {
        return RequestHash.getRequestHash(new JSONObjectWriter(root));
    }

    public void consistencyCheck(PaymentRequest paymentRequest) throws IOException {
        if (paymentRequest.currency != currency ||
            !paymentRequest.amount.equals(amount) ||
            paymentRequest.nonDirectPayment != nonDirectPayment ||
            !paymentRequest.referenceId.equals(referenceId) ||
            !paymentRequest.payee.commonName.equals(payee.commonName)) {
            throw new IOException("Inconsistent \"" + PAYMENT_REQUEST_JSON + "\" objects");
        }
    }
}
