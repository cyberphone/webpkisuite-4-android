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
package org.webpki.mobile.android.receipts;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.mobile.android.saturn.common.BaseProperties;
import org.webpki.mobile.android.saturn.common.ReceiptDecoder;

import org.webpki.net.HTTPSWrapper;

import java.util.GregorianCalendar;

public class ReceiptData {

    HTTPSWrapper wrapper;

    String receiptUrl;
    byte[] jsonReceipt;
    String amount;
    String currency;
    String commonName;
    String homePage;
    byte[] logotype;
    byte[] logotypeHash;
    String mimeType;
    long timeStamp;

    public ReceiptData(String receiptUrl) {
        this.receiptUrl = receiptUrl;
        wrapper = new HTTPSWrapper();
        wrapper.setRequireSuccess(true);
        wrapper.setTimeout(60000);
    }

    public boolean tryToPopulate() {
        try {
            wrapper.makeGetRequest(receiptUrl);
            jsonReceipt = wrapper.getData();
            ReceiptDecoder receipt = new ReceiptDecoder(JSONParser.parse(jsonReceipt));
            amount = receipt.getAmount().toPlainString();
            currency = receipt.getCurrency().toString();
            commonName = receipt.getPayeeCommonName();
            // Unix "epoch" time is in seconds but compensated for time-zone
            long epoch = receipt.getPayeeTimeStamp().getTimeInMillis();
            epoch += new GregorianCalendar().getTimeZone().getOffset(epoch);
            timeStamp = (epoch + 500) / 1000;
            wrapper.makeGetRequest(receipt.getPayeeAuthorityUrl());
            JSONObjectReader authorityObject = JSONParser.parse(wrapper.getData());
            // We don't need the full-blown decoder here since we only access a
            // couple of items that we also hope will never change
            homePage = authorityObject.getString(BaseProperties.HOME_PAGE_JSON);
            // Retrieve the actual logotype binary and its mime type
            wrapper.makeGetRequest(authorityObject.getString(BaseProperties.LOGOTYPE_URL_JSON));
            logotype = wrapper.getData();
            mimeType = wrapper.getContentType();
            // The primary key to the logotype data
            logotypeHash = HashAlgorithms.SHA256.digest(logotype);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
