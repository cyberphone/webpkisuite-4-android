/*
 *  Copyright 2006-2024 WebPKI.org (https://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.sks;

public class EnumeratedKey implements Cloneable {

    public static final int INIT_ENUMERATION = 0;

    int keyHandle = INIT_ENUMERATION;

    public int getKeyHandle() {
        return keyHandle;
    }


    int provisioningHandle;

    public int getProvisioningHandle() {
        return provisioningHandle;
    }


    public EnumeratedKey() {
    }


    public EnumeratedKey(int keyHandle,
                         int provisioningHandle) {
        this.keyHandle = keyHandle;
        this.provisioningHandle = provisioningHandle;
    }
    
    @Override
    public EnumeratedKey clone() {
        return new EnumeratedKey(keyHandle, provisioningHandle);
    }
}
