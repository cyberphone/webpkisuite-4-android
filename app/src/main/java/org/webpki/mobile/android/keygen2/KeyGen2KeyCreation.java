/*
 *  Copyright 2013-2020 WebPKI.org (http://webpki.org).
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
package org.webpki.mobile.android.keygen2;

import java.io.IOException;

import java.security.GeneralSecurityException;

import java.util.Arrays;

import android.os.AsyncTask;

import org.webpki.mobile.android.proxy.BaseProxyActivity;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.HashAlgorithms;

import org.webpki.keygen2.KeyCreationRequestDecoder;
import org.webpki.keygen2.KeyCreationResponseEncoder;
import org.webpki.keygen2.ProvisioningFinalizationRequestDecoder;
import org.webpki.keygen2.ProvisioningFinalizationResponseEncoder;

import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.KeyAttributes;
import org.webpki.sks.EnumeratedProvisioningSession;
import org.webpki.sks.KeyData;
import org.webpki.sks.PatternRestriction;

/**
 * This worker class creates keys.
 * If keys are only managed, this class will not be instantiated.
 */
public class KeyGen2KeyCreation extends AsyncTask<Void, String, String> {
    private KeyGen2Activity keyGen2Activity;

    public KeyGen2KeyCreation(KeyGen2Activity keyGen2Activity) {
        this.keyGen2Activity = keyGen2Activity;
    }

    private void postProvisioning(ProvisioningFinalizationRequestDecoder.PostOperation postOperation,
                                  int handle)
            throws IOException, GeneralSecurityException {
        EnumeratedProvisioningSession oldProvisioningSession = new EnumeratedProvisioningSession();
        while (true) {
            if ((oldProvisioningSession = keyGen2Activity.sks
                    .enumerateProvisioningSessions(oldProvisioningSession
                            .getProvisioningHandle(), false)) == null) {
                throw new IOException("Old provisioning session not found:" +
                        postOperation.getClientSessionId() +
                        "/" +
                        postOperation.getServerSessionId());
            }
            if (oldProvisioningSession.getClientSessionId().equals(postOperation.getClientSessionId()) &&
                oldProvisioningSession.getServerSessionId().equals(postOperation.getServerSessionId())) {
                break;
            }
        }
        EnumeratedKey ek = new EnumeratedKey();
        while (true) {
            if ((ek = keyGen2Activity.sks.enumerateKeys(ek.getKeyHandle())) == null) {
                throw new IOException("Old key not found");
            }
            if (ek.getProvisioningHandle() == oldProvisioningSession.getProvisioningHandle()) {
                KeyAttributes ka = keyGen2Activity.sks.getKeyAttributes(ek.getKeyHandle());
                if (Arrays.equals(HashAlgorithms.SHA256.digest(ka.getCertificatePath()[0].getEncoded()),
                        postOperation.getCertificateFingerprint())) {
                    switch (postOperation.getPostOperation()) {
                        case ProvisioningFinalizationRequestDecoder.PostOperation.CLONE_KEY_PROTECTION:
                            keyGen2Activity.sks.postCloneKeyProtection(handle,
                                                                       ek.getKeyHandle(),
                                                                       postOperation.getAuthorization(),
                                                                       postOperation.getMac());
                            break;

                        case ProvisioningFinalizationRequestDecoder.PostOperation.UPDATE_KEY:
                            keyGen2Activity.sks.postUpdateKey(handle,
                                                              ek.getKeyHandle(),
                                                              postOperation.getAuthorization(),
                                                              postOperation.getMac());
                            break;

                        case ProvisioningFinalizationRequestDecoder.PostOperation.UNLOCK_KEY:
                            keyGen2Activity.sks.postUnlockKey(handle,
                                                              ek.getKeyHandle(),
                                                              postOperation.getAuthorization(),
                                                              postOperation.getMac());
                            break;

                        default:
                            keyGen2Activity.sks.postDeleteKey(handle,
                                                              ek.getKeyHandle(),
                                                              postOperation.getAuthorization(),
                                                              postOperation.getMac());
                    }
                    return;
                }
            }
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            publishProgress(BaseProxyActivity.PROGRESS_KEYGEN);

            KeyCreationResponseEncoder keyCreationResponse = new KeyCreationResponseEncoder(keyGen2Activity.keyCreationRequest);

            int pinPolicyHandle = 0;
            int pukPolicyHandle = 0;
            for (KeyCreationRequestDecoder.KeyObject key : keyGen2Activity.keyCreationRequest.getKeyObjects()) {
                if (key.getPINPolicy() == null) {
                    pinPolicyHandle = 0;
                    pukPolicyHandle = 0;
                } else {
                    if (key.isStartOfPINPolicy()) {
                        if (key.isStartOfPUKPolicy()) {
                            KeyCreationRequestDecoder.PUKPolicy pukPolicy = key.getPINPolicy().getPUKPolicy();
                            pukPolicyHandle = keyGen2Activity.sks.createPukPolicy(keyGen2Activity.provisioningHandle,
                                                                                  pukPolicy.getID(),
                                                                                  pukPolicy.getEncryptedValue(),
                                                                                  pukPolicy.getFormat().getSksValue(),
                                                                                  pukPolicy.getRetryLimit(),
                                                                                  pukPolicy.getMac());
                        }
                        KeyCreationRequestDecoder.PINPolicy pinPolicy = key.getPINPolicy();
                        pinPolicyHandle = keyGen2Activity.sks.createPinPolicy(keyGen2Activity.provisioningHandle,
                                                                              pinPolicy.getID(),
                                                                              pukPolicyHandle,
                                                                              pinPolicy.getUserDefinedFlag(),
                                                                              pinPolicy.getUserModifiableFlag(),
                                                                              pinPolicy.getFormat().getSksValue(),
                                                                              pinPolicy.getRetryLimit(),
                                                                              pinPolicy.getGrouping().getSksValue(),
                                                                              PatternRestriction.getSksValue(pinPolicy.getPatternRestrictions()),
                                                                              pinPolicy.getMinLength(),
                                                                              pinPolicy.getMaxLength(),
                                                                              pinPolicy.getInputMethod().getSksValue(),
                                                                              pinPolicy.getMac());
                    }
                }
                KeyData keyData = keyGen2Activity.sks.createKeyEntry(keyGen2Activity.provisioningHandle,
                                                                     key.getID(),
                                                                     keyGen2Activity.keyCreationRequest.getKeyEntryAlgorithm(),
                                                                     key.getServerSeed(),
                                                                     key.isDevicePINProtected(),
                                                                     pinPolicyHandle,
                                                                     key.getSKSPINValue(),
                                                                     key.getEnablePINCachingFlag(),
                                                                     key.getBiometricProtection().getSksValue(),
                                                                     key.getExportProtection().getSksValue(),
                                                                     key.getDeleteProtection().getSksValue(),
                                                                     key.getAppUsage().getSksValue(),
                                                                     key.getFriendlyName(),
                                                                     key.getKeySpecifier().getKeyAlgorithm().getAlgorithmId(AlgorithmPreferences.SKS),
                                                                     key.getKeySpecifier().getKeyParameters(),
                                                                     key.getEndorsedAlgorithms(),
                                                                     key.getMac());
                keyCreationResponse.addPublicKey(keyData.getPublicKey(), keyData.getAttestation(), key.getID());
            }

            keyGen2Activity.postJSONData(keyGen2Activity.getTransactionURL(),
                                         keyCreationResponse,
                                         BaseProxyActivity.RedirectPermitted.FORBIDDEN);

            publishProgress(BaseProxyActivity.PROGRESS_DEPLOY_CERTS);

            ProvisioningFinalizationRequestDecoder provFinalRequest = (ProvisioningFinalizationRequestDecoder) keyGen2Activity.parseJSONResponse();

            //======================================================================//
            // Note: we could have used the saved provisioningHandle but that
            // would not work for certifications that are delayed. The following
            // code is working for fully interactive and delayed scenarios by
            // using SKS as state-holder
            //======================================================================//
            EnumeratedProvisioningSession eps = new EnumeratedProvisioningSession();
            while (true) {
                if ((eps = keyGen2Activity.sks.enumerateProvisioningSessions(eps.getProvisioningHandle(), true)) == null) {
                    throw new IOException("Provisioning session not found:" +
                            provFinalRequest.getClientSessionId() +
                            "/" +
                            provFinalRequest.getServerSessionId());
                }
                if (eps.getClientSessionId().equals(provFinalRequest.getClientSessionId()) &&
                    eps.getServerSessionId().equals(provFinalRequest.getServerSessionId())) {
                    break;
                }
            }

            //======================================================================//
            // Final check, do these keys match the request?
            //======================================================================//
            for (ProvisioningFinalizationRequestDecoder.IssuedCredential key : provFinalRequest.getIssuedCredentials()) {
                int keyHandle = keyGen2Activity.sks.getKeyHandle(eps.getProvisioningHandle(),
                                                                 key.getId());
                keyGen2Activity.sks.setCertificatePath(keyHandle,
                                                       key.getCertificatePath(),
                                                       key.getMac());

                //======================================================================//
                // There may be a symmetric key
                //======================================================================//
                if (key.getOptionalSymmetricKey() != null) {
                    keyGen2Activity.sks.importSymmetricKey(keyHandle,
                                                           key.getOptionalSymmetricKey(),
                                                           key.getSymmetricKeyMac());
                }

                //======================================================================//
                // There may be a private key
                //======================================================================//
                if (key.getOptionalPrivateKey() != null) {
                    keyGen2Activity.sks.importPrivateKey(keyHandle,
                                                         key.getOptionalPrivateKey(),
                                                         key.getPrivateKeyMac());
                }

                //======================================================================//
                // There may be extensions
                //======================================================================//
                for (ProvisioningFinalizationRequestDecoder.Extension extension : key.getExtensions()) {
                    keyGen2Activity.sks.addExtension(keyHandle,
                                                     extension.getExtensionType(),
                                                     extension.getSubType(),
                                                     extension.getQualifier(),
                                                     extension.getExtensionData(),
                                                     extension.getMac());
                }

                //======================================================================//
                // There may be an postUpdateKey or postCloneKeyProtection
                //======================================================================//
                ProvisioningFinalizationRequestDecoder.PostOperation postOperation = key.getPostOperation();
                if (postOperation != null) {
                    postProvisioning(postOperation, keyHandle);
                }
            }

            //======================================================================//
            // There may be any number of postUnlockKey
            //======================================================================//
            for (ProvisioningFinalizationRequestDecoder.PostOperation postUnlock
                    : provFinalRequest.getPostUnlockKeys()) {
                postProvisioning(postUnlock, eps.getProvisioningHandle());
            }

            //======================================================================//
            // There may be any number of postDeleteKey
            //======================================================================//
            for (ProvisioningFinalizationRequestDecoder.PostOperation postDelete
                    : provFinalRequest.getPostDeleteKeys()) {
                postProvisioning(postDelete, eps.getProvisioningHandle());
            }

            publishProgress(BaseProxyActivity.PROGRESS_FINAL);

            //======================================================================//
            // Create final and attested message
            //======================================================================//
            keyGen2Activity.postJSONData(keyGen2Activity.getTransactionURL(),
                    new ProvisioningFinalizationResponseEncoder(provFinalRequest,
                            keyGen2Activity.sks.closeProvisioningSession(eps.getProvisioningHandle(),
                                                                         provFinalRequest.getCloseSessionNonce(),
                                                                         provFinalRequest.getCloseSessionMac())),
                    BaseProxyActivity.RedirectPermitted.REQUIRED);
            return keyGen2Activity.getRedirectURL();
        } catch (Exception e) {
            keyGen2Activity.logException(e);
        }
        return null;
    }

    @Override
    public void onProgressUpdate(String... message) {
        keyGen2Activity.showHeavyWork(message[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        if (keyGen2Activity.userHasAborted()) {
            return;
        }
        if (result != null) {
            keyGen2Activity.launchBrowser(result);
        } else {
            keyGen2Activity.showFailLog();
        }
    }
}
