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

import java.math.BigDecimal;
import java.util.Vector;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONObjectReader;

public class WalletRequestDecoder extends JSONDecoder implements BaseProperties {

    public static class PaymentMethodDescriptor {
        public String paymentMethod;
        public String payeeAuthorityUrl;
    }

    public Vector<PaymentMethodDescriptor> paymentMethodList = new Vector<>();

    public String noMatchingMethodsUrl;

    public String optionalReceiptUrl;

    public PaymentRequestDecoder paymentRequest;

    private NonDirectPaymentDecoder nonDirectPayment;

    public static String getFormattedMoney(BigDecimal amount, Currencies currency) {
        return "<span class='money'>" +
               currency.amountToDisplayString(amount, true) +
               "</span>";
    }

    public String getFormattedTotal() {
        return (nonDirectPayment == null ||
                nonDirectPayment.getType() == NonDirectPaymentTypes.RESERVATION ?
            "" : "<span class='moneynote'>Upfront fee:</span> ") +
            getFormattedMoney(paymentRequest.getAmount(), paymentRequest.getCurrency()) +
            (nonDirectPayment == null ? "" : "<br>\u200b");
    }

    private String getMarquee() {
        switch (nonDirectPayment.getType()) {
            case RESERVATION:
                switch (nonDirectPayment.getReservationSubType()) {
                    case GAS_STATION:
                        return  "<i>Reserved</i>, actual payment will match fuel quantity";
                    case BOOKING:
                        return  "<i>Booking</i>, see contract for details";
                    case DEPOSIT:
                        return  "<i>Deposit</i>, see contract for details";
                    case CONSUMABLE:
                        return  "<i>Reserved</i>, actual payment will match quantity";
                }
            case RECURRING:
                return nonDirectPayment.getInterval() == RecurringPaymentIntervals.UNSPECIFIED ?
                    "Multiple payment grant" : "<i>Subscription</i>, see contract for details";
        }
        throw new RuntimeException("Unexpected");
    }

    public String getOptionalMarqueeCode() {
        if (nonDirectPayment == null) {
            return "";
        }
        String marquee = getMarquee();
        return
         "document.getElementById('amountfield').innerHTML += \"<span id='marquee1' class='marquee'>" +
            marquee +
            "</span><span id='marquee2' class='marquee'>" +
            marquee +
            "</span>\";\n" +
        "let marquee1 = document.getElementById('marquee1');\n" +
        "let marquee2 = document.getElementById('marquee2');\n" +
        "let marqueeWidth = marquee1.offsetWidth;\n" +
        "let startMarquee = 0;\n" +
        "let delayMarquee = 0;\n" +
        "let distance = Math.floor(document.getElementById('payeefield').offsetWidth / 3);\n" +
        "setInterval(function() {\n" +
          "marquee1.style.left = startMarquee + 'px';\n" +
          "marquee2.style.left = (startMarquee + distance) + 'px';\n" +
          "if (delayMarquee++ > 200) {\n" +
            "if (--startMarquee + marqueeWidth + distance == 0) {\n" +
              "delayMarquee = 0;\n" +
              "startMarquee = 0;\n" +
            "}\n" +
          "}\n" +
        "}, 10);\n";
    }

    public String getLocalSuccessMessage()  {
        return nonDirectPayment != null &&
            nonDirectPayment.getType() == NonDirectPaymentTypes.RESERVATION &&
            nonDirectPayment.getReservationSubType() == ReservationSubTypes.GAS_STATION ?
            "The operation was successful!<p>Now follow the instructions at the pump.</p>"
            :
            "The operation was successful!";
    }

    public String getAmountLabel() {
        return nonDirectPayment == null ||
               nonDirectPayment.getType() == NonDirectPaymentTypes.RESERVATION ?
                                                                       "Total" : "Confirm";
    }

    @Override
    protected void readJSONData(JSONObjectReader rd) {
        JSONArrayReader methodList = rd.getArray(SUPPORTED_PAYMENT_METHODS_JSON);
        do {
            PaymentMethodDescriptor pmd = new PaymentMethodDescriptor();
            JSONObjectReader o = methodList.getObject();
            pmd.paymentMethod = o.getString(PAYMENT_METHOD_JSON);
            pmd.payeeAuthorityUrl = o.getString(PAYEE_AUTHORITY_URL_JSON);
            paymentMethodList.add(pmd);
        } while (methodList.hasMore());
        optionalReceiptUrl = rd.getStringConditional(RECEIPT_URL_JSON);
        paymentRequest = new PaymentRequestDecoder(rd.getObject(PAYMENT_REQUEST_JSON));
        nonDirectPayment = paymentRequest.getNonDirectPayment();
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
