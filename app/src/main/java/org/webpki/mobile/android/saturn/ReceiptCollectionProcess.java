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

import android.os.SystemClock;

import android.view.Gravity;

import org.webpki.mobile.android.receipts.Database;
import org.webpki.mobile.android.receipts.ReceiptData;

public class ReceiptCollectionProcess extends AsyncTask<Void, String, Boolean> {

    SaturnActivity saturnActivity;
    ReceiptData receiptData;
    long delay = 30000;

    ReceiptCollectionProcess(SaturnActivity saturnActivity) {
        this.saturnActivity = saturnActivity;
        this.receiptData = new ReceiptData(saturnActivity.walletRequest.optionalReceiptUrl);
    }

    @Override
    protected Boolean doInBackground (Void... params) {
        for (int i = 0; i < 13; i++) {
            if (receiptData.tryToPopulate()) {
                return true;
            }
            SystemClock.sleep(delay);
            if (i == 3) {
                delay = delay << 1;
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            Database.AddReceipt(receiptData, saturnActivity);
        }
        // Here there should of course be a more intelligent service for
        // dealing with failed receipts but that is for another day...
        saturnActivity.toast(success ? "RECEIPT RECEIVED" : "RECEIPT FAIL", Gravity.CENTER_VERTICAL);
    }
}
