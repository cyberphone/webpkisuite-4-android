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

import org.webpki.mobile.android.database.Database;
import org.webpki.mobile.android.database.ReceiptData;

public class ReceiptCollectionProcess extends AsyncTask<Void, String, Boolean> {

    SaturnActivity saturnActivity;
    ReceiptData receiptData;

    ReceiptCollectionProcess(SaturnActivity saturnActivity) {
        this.saturnActivity = saturnActivity;
        this.receiptData = new ReceiptData(saturnActivity.walletRequest.optionalReceiptUrl);
    }

    @Override
    protected Boolean doInBackground (Void... params) {
        return receiptData.tryToPopulate();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        String res = "RECEIPT SUCCESS";
        if (success) {
            Database.AddReceipt(receiptData, saturnActivity);
        }
        saturnActivity.toast(success ? res : "RECEIPT FAIL", Gravity.CENTER_VERTICAL);
    }
}
