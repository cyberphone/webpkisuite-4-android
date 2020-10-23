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

import org.webpki.json.JSONParser;

import org.webpki.mobile.android.saturn.common.BaseProperties;
import org.webpki.mobile.android.saturn.common.ReceiptDecoder;

import org.webpki.net.HTTPSWrapper;

public class ReceiptData {

    HTTPSWrapper wrapper;

    String receiptUrl;
    byte[] jsonReceipt;
    String amount;
    String currency;
    String commonName;
    byte[] logotype;
    byte[] logotypeHash;
    String mimeType;
    long payeeTimeStamp;

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
            payeeTimeStamp = (receipt.getPayeeTimeStamp().getTimeInMillis() + 500) / 1000;
            String authorityUrl = receipt.getPayeeAuthorityUrl();
            wrapper.makeGetRequest(authorityUrl);
            String logotypeUrl =
                JSONParser.parse(wrapper.getData()).getString(BaseProperties.LOGOTYPE_URL_JSON);
            wrapper.makeGetRequest(logotypeUrl);
            logotype = wrapper.getData();
            mimeType = wrapper.getContentType();
            logotypeHash = HashAlgorithms.SHA256.digest(logotype);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
