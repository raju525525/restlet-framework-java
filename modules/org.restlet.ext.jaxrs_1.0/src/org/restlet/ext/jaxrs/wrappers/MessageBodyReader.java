/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */
package org.restlet.ext.jaxrs.wrappers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.core.MultivaluedMap;

import org.restlet.data.MediaType;

/**
 * Class to wrap a {@link javax.ws.rs.ext.MessageBodyWriter}
 * 
 * @author Stephan Koops
 */
@SuppressWarnings("unchecked")
public class MessageBodyReader {

    private javax.ws.rs.ext.MessageBodyReader reader;

    private List<org.restlet.data.MediaType> consumedMimes;

    /**
     * Construct a wrapper or a {@link javax.ws.rs.ext.MessageBodyReader}
     * 
     * @param reader
     *                the JAX-RS {@link javax.ws.rs.ext.MessageBodyReader} to
     *                wrap.
     */
    public MessageBodyReader(javax.ws.rs.ext.MessageBodyReader<?> reader) {
        if (reader == null)
            throw new IllegalArgumentException(
                    "The MessageBodyReader must not be null");
        this.reader = reader;
    }

    /**
     * 
     * @param type
     * @param genericType
     * @param annotations
     * @return
     * @see javax.ws.rs.ext.MessageBodyReader#isReadable(Class, Type,
     *      Annotation[])
     */
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations) {
        return reader.isReadable(type, genericType, annotations);
    }

    /**
     * 
     * @param type
     * @param genericType
     *                The generic {@link Type} to convert to.
     * @param mediaType
     * @param annotations
     *                the annotations of the artefact to convert to
     * @param httpHeaders
     * @param entityStream
     * @return
     * @throws IOException
     * @see javax.ws.rs.ext.MessageBodyReader#readFrom(Class, Type,
     *      javax.ws.rs.core.MediaType, Annotation[], MultivaluedMap,
     *      InputStream)
     */
    public Object readFrom(Class type, Type genericType,
            javax.ws.rs.core.MediaType mediaType, Annotation[] annotations,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException {
        return this.reader.readFrom(type, genericType, mediaType, annotations,
                httpHeaders, entityStream);
    }

    /**
     * Returns the list of produced {@link MediaType}s of the wrapped
     * {@link javax.ws.rs.ext.MessageBodyWriter}.
     * 
     * @return List of produced {@link MediaType}s.
     */
    public List<MediaType> getConsumedMimes() {
        if (consumedMimes == null) {
            ConsumeMime pm = reader.getClass().getAnnotation(ConsumeMime.class);
            if (pm != null)
                this.consumedMimes = ResourceMethod.convertToMediaTypes(pm
                        .value());
            else
                this.consumedMimes = Collections.singletonList(MediaType.ALL);
        }
        return consumedMimes;
    }

    /**
     * Checks, if this MessageBodyReader supports the given MediaType.
     * 
     * @param mediaType
     * @return
     */
    public boolean supports(MediaType mediaType) {
        for (MediaType cm : getConsumedMimes()) {
            if (cm.isCompatible(mediaType))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.reader.getClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MessageBodyReader))
            return false;
        return this.reader.equals(((MessageBodyReader) o).reader);
    }

    @Override
    public int hashCode() {
        return this.reader.hashCode();
    }
}