/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.appengine.tck.transformers;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Map;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MultipleTransformer implements ClassFileTransformer {
    private MatchingClassFileTransformer defaultFileTransformer;
    private Map<String, ClassFileTransformer> transformers;

    public MultipleTransformer(MatchingClassFileTransformer defaultFileTransformer, Map<String, ClassFileTransformer> transformers) {
        this.defaultFileTransformer = defaultFileTransformer;
        this.transformers = transformers;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassFileTransformer transformer = transformers.get(className);
        if (transformer != null) {
            return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        }
        if (defaultFileTransformer != null && defaultFileTransformer.match(className)) {
            return defaultFileTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        }
        return classfileBuffer;
    }
}
