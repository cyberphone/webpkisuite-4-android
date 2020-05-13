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

import android.util.Log;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymKeySignerInterface;
import org.webpki.crypto.AsymSignatureAlgorithms;

import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.mobile.android.saturn.common.BalanceRequestEncoder;
import org.webpki.mobile.android.saturn.common.BalanceResponseDecoder;
import org.webpki.mobile.android.saturn.common.BaseProperties;
import org.webpki.mobile.android.saturn.common.KnownExtensions;

import org.webpki.net.HTTPSWrapper;

import java.io.IOException;
import java.security.PublicKey;
import java.util.GregorianCalendar;

public class BalanceRequester extends AsyncTask<Void, String, Boolean> {
    private SaturnActivity saturnActivity;
    private Account account;
    String balance;

    public BalanceRequester (SaturnActivity saturnActivity, Account account) {
        this.saturnActivity = saturnActivity;
        this.account = account;
    }

    @Override
    protected Boolean doInBackground (Void... params) {
        try {
            HTTPSWrapper wrapper = new HTTPSWrapper();
            wrapper.setRequireSuccess(true);
            wrapper.setTimeout(60000);
            wrapper.makeGetRequest(account.authorityUrl);
            String balanceUrl = JSONParser.parse(wrapper.getData())
                .getObject(BaseProperties.EXTENSIONS_JSON)
                    .getString(KnownExtensions.BALANCE_REQUEST);
            wrapper.setHeader("Content-Type", BaseProperties.JSON_CONTENT_TYPE);
            byte[] json = BalanceRequestEncoder.encode(balanceUrl,
                                                       account.currency,
                                                       account.accountId,
                                                       account.credentialId,
                                                       new GregorianCalendar(),
                                                       "Saturn",
                                                       "the best",
                new JSONAsymKeySigner(new AsymKeySignerInterface() {
                    @Override
                    public PublicKey getPublicKey() throws IOException {
                        return saturnActivity.sks.getKeyAttributes(
                            account.optionalBalanceKeyHandle).getCertificatePath()[0].getPublicKey();
                    }
                    @Override
                    public byte[] signData(byte[] data, AsymSignatureAlgorithms algorithm)
                    throws IOException {
                        return saturnActivity.sks.signHashedData(
                            account.optionalBalanceKeyHandle,
                            algorithm.getAlgorithmId(AlgorithmPreferences.SKS),
                            null,
                            false,
                            null,
                            algorithm.getDigestAlgorithm().digest(data));
                    }
                }).setSignatureAlgorithm(account.signatureAlgorithm))
                    .serializeToBytes(JSONOutputFormats.NORMALIZED);
            wrapper.makePostRequest(balanceUrl, json);
            balance = new BalanceResponseDecoder(JSONParser.parse(wrapper.getData())).getAmount().toPlainString();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        Log.i("KLM", success.toString() + " " + this.balance);
    }
}
