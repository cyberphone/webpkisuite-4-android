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
package org.webpki.mobile.android.saturn;

import android.content.Context;

import android.os.AsyncTask;

import java.io.FileOutputStream;
import java.io.IOException;

import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.mobile.android.saturn.common.BaseProperties;
import org.webpki.mobile.android.saturn.common.EncryptedMessage;
import org.webpki.mobile.android.saturn.common.UserChallengeItem;
import org.webpki.mobile.android.saturn.common.PayerAuthorizationEncoder;
import org.webpki.mobile.android.saturn.common.ProviderUserResponseDecoder;
import org.webpki.mobile.android.saturn.common.WalletAlertDecoder;

import org.webpki.util.HTMLEncoder;

public class SaturnProtocolPerform extends AsyncTask<Void, String, Boolean> {
    private SaturnActivity saturnActivity;

    public SaturnProtocolPerform (SaturnActivity saturnActivity) {
        this.saturnActivity = saturnActivity;
    }

    EncryptedMessage encryptedMessage;
    
    String merchantHtmlAlert;

    @Override
    protected Boolean doInBackground (Void... params) {
        try {
            // Since user authorizations are pushed through the Payees they must be encrypted in order
            // to not leak user information to Payees.  Only the proper Payment Provider can decrypt
            // and process user authorizations.
            Account account = saturnActivity.getSelectedCard();
            if (!saturnActivity.postJSONData(
                saturnActivity.getTransactionURL(),
                new PayerAuthorizationEncoder(saturnActivity.authorizationData,
                                              account.authorityUrl,
                                              account.paymentMethod,
                                              account.dataEncryptionAlgorithm,
                                              account.encryptionKey,
                                              account.optionalKeyId,
                                              account.keyEncryptionAlgorithm),
                BaseProxyActivity.RedirectPermitted.OPTIONAL)) {
                JSONDecoder returnMessage = saturnActivity.parseJSONResponse();
                if (returnMessage instanceof ProviderUserResponseDecoder) {
                    encryptedMessage =
                        ((ProviderUserResponseDecoder)returnMessage)
                            .getEncryptedMessage(saturnActivity.privateMessageEncryptionKey,
                                                 account.dataEncryptionAlgorithm);
                } else {
                    merchantHtmlAlert = ((WalletAlertDecoder)returnMessage).getText();
                }
                return true;
            }
        } catch (Exception e) {
            saturnActivity.logException(e);
            return null;
        }
        return false;
    }

    StringBuilder header(String party, String message) {
//TODO "message" MUST be checked for valid subset of HTML
        return new StringBuilder("<div style='text-align:center'>Message from <i>")
            .append(HTMLEncoder.encode(party))
            .append("</i></div><div style='padding-top:15pt'>")
            .append(message)
            .append("</div>");
    }

    @Override
    protected void onPostExecute(Boolean alertUser) {
        if (saturnActivity.userHasAborted()) {
            return;
        }
        saturnActivity.noMoreWorkToDo();
        if (alertUser == null) {
            saturnActivity.showFailLog();
        } else if (alertUser) {
            StringBuilder html = new StringBuilder();
            StringBuilder js = new StringBuilder();

            if (merchantHtmlAlert == null) {
                html.append(header(encryptedMessage.getRequester(), encryptedMessage.getText()));
                if (encryptedMessage.getOptionalUserChallengeItems() != null) {
                    js.append("}\n" +
                              "function getChallengeData() {\n" +
                              "  var data = [];\n");
                    for (UserChallengeItem challengeField : encryptedMessage.getOptionalUserChallengeItems()) {
                        js.append("  data.push({'")
                          .append(challengeField.getName())
                          .append("': document.getElementById('")
                          .append(challengeField.getName())
                          .append("').value});\n");
                    }
                    js.append("  return JSON.stringify(data);\n");
                    html.append("<form onsubmit=\"return Saturn.getChallengeJSON(getChallengeData())\">");
                    String autofocus = "autofocus ";
                    for (UserChallengeItem challengeField : encryptedMessage.getOptionalUserChallengeItems()) {
                        html.append("<div style='padding-top:15pt'>");
                        if (challengeField.getOptionalLabel() != null) {
                            html.append(challengeField.getOptionalLabel())
                                .append(":<br>");
                        }
                        html.append("<input style='font-size:inherit' ")
                            .append(autofocus)
                            .append("type='")
                            .append(challengeField.getType() == UserChallengeItem.TYPE.ALPHANUMERIC_SECRET ||
                                    challengeField.getType() == UserChallengeItem.TYPE.NUMERIC_SECRET ?
                                "password" : "text")
                            .append("' id='")
                            .append(challengeField.getName())
                            .append("' size='20'></div>");
                        autofocus = "";
                    }
                    html.append("<div style='text-align:center;padding-top:15pt'>" +
                                "<input type='submit' style='font-size:inherit;text-align:center;padding:3pt 10pt;border-width:1px;" +
                                "border-style:solid;border-color:#a0a0a0;box-shadow:3pt 3pt 3pt #d0d0d0;" +
                                "background:linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" +
                                "border-radius:3pt;margin-left:auto;margin-right:auto' value='Submit'></div>" +
                                "</form>");
                }
             } else {
                 html.append(header(saturnActivity.walletRequest.paymentRequest.getPayeeCommonName(),
                                    merchantHtmlAlert));
            }
            saturnActivity.currentForm = SaturnActivity.FORM.SIMPLE;
            saturnActivity.messageDisplay(js.toString(), html.toString());
       } else {
            try {
                FileOutputStream fos =
                        saturnActivity.openFileOutput(SaturnActivity.SATURN_SETTINGS,
                                                      Context.MODE_PRIVATE);
                JSONObjectWriter settings = new JSONObjectWriter();
                settings.setInt(SaturnActivity.LAST_KEY_ID_JSON,
                                saturnActivity.getSelectedCard().signatureKeyHandle);
                fos.write(settings.serializeToBytes(JSONOutputFormats.NORMALIZED));
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String url = saturnActivity.getRedirectURL();
            if (url.equals(BaseProperties.SATURN_LOCAL_SUCCESS_URI)) {
                saturnActivity.done = true;
                saturnActivity.simpleDisplay(saturnActivity.walletRequest.getLocalSuccessMessage());
                saturnActivity.closeProxy();
             } else {
                saturnActivity.launchBrowser(url);
            }
        }
    }
}
