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

import android.os.AsyncTask;

import android.view.Gravity;

import org.webpki.json.JSONParser;

import org.webpki.mobile.android.saturn.common.ReceiptDecoder;

import org.webpki.net.HTTPSWrapper;

import java.io.IOException;

public class ReceiptRequester extends AsyncTask<Void, String, Boolean> {

    SaturnActivity saturnActivity;
    String receiptUrl;
    ReceiptDecoder receipt;

    ReceiptRequester(SaturnActivity saturnActivity) {
        this.saturnActivity = saturnActivity;
        this.receiptUrl = saturnActivity.walletRequest.optionalReceiptUrl;
    }

    @Override
    protected Boolean doInBackground (Void... params) {
        try {
            HTTPSWrapper wrapper = new HTTPSWrapper();
            wrapper.setRequireSuccess(true);
            wrapper.setTimeout(60000);
            wrapper.makeGetRequest(receiptUrl);
            byte[] receiptJson = wrapper.getData();
            receipt = new ReceiptDecoder(JSONParser.parse(receiptJson));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        saturnActivity.toast(success ? "RECEIPT SUCCESS" + receipt.getPayeeCommonName() : "RECEIPT FAIL", Gravity.CENTER_VERTICAL);
    }
}
