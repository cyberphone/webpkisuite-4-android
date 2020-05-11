/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2013-2020 WebPKI.org (http://webpki.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.webpki.mobile.android.application;

import android.content.Intent;

import android.net.Uri;

import android.os.AsyncTask;

import android.util.Log;

import com.journeyapps.barcodescanner.CaptureActivity;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.net.HTTPSWrapper;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 * @uathor Anders Rundgren (adapted BarcodScanner from ZXING)
 */
public final class QRIDActivity extends CaptureActivity {
    
    static class QRTrigger extends AsyncTask<Void, Void, Boolean> {
        private String url;
        private QRIDActivity activity;
        Uri proxyUrl;

        public QRTrigger (String url, QRIDActivity activity) {
             this.url = url;
             this.activity = activity;
        }

        @Override
        protected Boolean doInBackground (Void... params) {
            try {
                HTTPSWrapper wrapper = new HTTPSWrapper();
                wrapper.setTimeout(5000);
                wrapper.setRequireSuccess(true);
                wrapper.makeGetRequest(url);
                proxyUrl = Uri.parse(wrapper.getDataUTF8());
                Intent intent = new Intent(activity.getApplicationContext(), BaseProxyActivity.getExecutor(proxyUrl));
                intent.setData(proxyUrl);
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e("QQQ", url, e);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Log.i("QQQ", "OK=" + success);
            if (success) {
                BaseProxyActivity.getExecutor(proxyUrl);
            }
            activity.finish();
        }
    }

    @Override
    protected void webPkiEvent(String url) {
        new QRTrigger(url, this).execute();
    }
}

