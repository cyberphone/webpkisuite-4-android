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

import java.math.BigDecimal;

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class PaymentRequest implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Payee";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(String payeeCommonName,
                                          String payeeHomePage,
                                          BigDecimal amount,
                                          Currencies currency,
                                          NonDirectPayments optionalNonDirectPayment,
                                          String referenceId,
                                          GregorianCalendar timeStamp,
                                          GregorianCalendar expires) throws IOException {
        return new JSONObjectWriter()
            .setObject(PAYEE_JSON, new JSONObjectWriter()
                                   .setString(COMMON_NAME_JSON, payeeCommonName)
                                   .setString(HOME_PAGE_JSON, payeeHomePage))
            .setMoney(AMOUNT_JSON, amount, currency.getDecimals())
            .setString(CURRENCY_JSON, currency.toString())
            .setDynamic((wr) -> optionalNonDirectPayment == null ?
                    wr : wr.setString(NON_DIRECT_PAYMENT_JSON, optionalNonDirectPayment.toString()))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, timeStamp, ISODateTime.UTC_NO_SUBSECONDS)
            .setDateTime(EXPIRES_JSON, expires, ISODateTime.UTC_NO_SUBSECONDS)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION));
    }

    public PaymentRequest(JSONObjectReader rd) throws IOException {
        root = rd;
        JSONObjectReader payee = rd.getObject(PAYEE_JSON);
        payeeCommonName = payee.getString(COMMON_NAME_JSON);
        payeeHomePage = payee.getString(HOME_PAGE_JSON);
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        if (rd.hasProperty(NON_DIRECT_PAYMENT_JSON)) {
            nonDirectPayment = NonDirectPayments.fromType(rd.getString(NON_DIRECT_PAYMENT_JSON));
        }
        amount = rd.getMoney(AMOUNT_JSON, currency.getDecimals());
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        expires = rd.getDateTime(EXPIRES_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        rd.checkForUnread();
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    String payeeCommonName;
    public String getPayeeCommonName() {
        return payeeCommonName;
    }

    String payeeHomePage;
    public String getPayeeHomePage() {
        return payeeHomePage;
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

    JSONObjectReader root;

    public byte[] getRequestHash() throws IOException {
        return RequestHash.getRequestHash(new JSONObjectWriter(root));
    }
}
