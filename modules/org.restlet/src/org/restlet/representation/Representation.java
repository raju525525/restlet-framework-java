/**
 * Copyright 2005-2009 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.representation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Date;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Range;
import org.restlet.data.Tag;
import org.restlet.engine.io.ByteUtils;
import org.restlet.engine.util.DateUtils;

/**
 * Current or intended state of a resource. The content of a representation can
 * be retrieved several times if there is a stable and accessible source, like a
 * local file or a string. When the representation is obtained via a temporary
 * source like a network socket, its content can only be retrieved once. The
 * "transient" and "available" properties are available to help you figure out
 * those aspects at runtime.<br>
 * <br>
 * For performance purpose, it is essential that a minimal overhead occurs upon
 * initialization. The main overhead must only occur during invocation of
 * content processing methods (write, getStream, getChannel and toString).<br>
 * <br>
 * "REST components perform actions on a resource by using a representation to
 * capture the current or intended state of that resource and transferring that
 * representation between components. A representation is a sequence of bytes,
 * plus representation metadata to describe those bytes. Other commonly used but
 * less precise names for a representation include: document, file, and HTTP
 * message entity, instance, or variant." Roy T. Fielding
 * 
 * @see <a href=
 *      "http://roy.gbiv.com/pubs/dissertation/rest_arch_style.htm#sec_5_2_1_2"
 *      >Source dissertation</a>
 * @author Jerome Louvel
 */
public abstract class Representation extends RepresentationInfo {
    /**
     * Indicates that the size of the representation can't be known in advance.
     */
    public static final long UNKNOWN_SIZE = -1L;

    /**
     * Returns a new empty representation with no content.
     * 
     * @return A new empty representation.
     * @deprecated Use {@link EmptyRepresentation} instead.
     */
    @Deprecated
    public static Representation createEmpty() {
        return new EmptyRepresentation();
    }

    /** Indicates if the representation's content is available. */
    private volatile boolean available;

    // [ifndef gwt] member
    /**
     * The representation digest if any.
     */
    private volatile org.restlet.data.Digest digest;

    /** Indicates if the representation is downloadable. */
    private volatile boolean downloadable;

    /**
     * Indicates the suggested download file name for the representation's
     * content.
     */
    private volatile String downloadName;

    /** The expiration date. */
    private volatile Date expirationDate;

    /** Indicates if the representation's content is transient. */
    private volatile boolean isTransient;

    /**
     * Indicates where in the full content the partial content available should
     * be applied.
     */
    private volatile Range range;

    /**
     * The expected size. Dynamic representations can have any size, but
     * sometimes we can know in advance the expected size. If this expected size
     * is specified by the user, it has a higher priority than any size that can
     * be guessed by the representation (like a file size).
     */
    private volatile long size;

    /**
     * Default constructor.
     */
    public Representation() {
        this(null);
    }

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The media type.
     */
    public Representation(MediaType mediaType) {
        super(mediaType);
        this.available = true;
        this.downloadable = false;
        this.downloadName = null;
        this.isTransient = false;
        this.size = UNKNOWN_SIZE;
        this.expirationDate = null;
        // [ifndef gwt]
        this.digest = null;
        this.range = null;
        // [enddef]
    }

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The media type.
     * @param modificationDate
     *            The modification date.
     */
    public Representation(MediaType mediaType, Date modificationDate) {
        this(mediaType, modificationDate, null);
    }

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The media type.
     * @param modificationDate
     *            The modification date.
     * @param tag
     *            The tag.
     */
    public Representation(MediaType mediaType, Date modificationDate, Tag tag) {
        super(mediaType, modificationDate, tag);
    }

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The media type.
     * @param tag
     *            The tag.
     */
    public Representation(MediaType mediaType, Tag tag) {
        this(mediaType, null, tag);
    }

    /**
     * Constructor from a variant.
     * 
     * @param variant
     *            The variant to copy.
     * @param modificationDate
     *            The modification date.
     */
    public Representation(Variant variant, Date modificationDate) {
        this(variant, modificationDate, null);
    }

    /**
     * Constructor from a variant.
     * 
     * @param variant
     *            The variant to copy.
     * @param modificationDate
     *            The modification date.
     * @param tag
     *            The tag.
     */
    public Representation(Variant variant, Date modificationDate, Tag tag) {
        setCharacterSet(variant.getCharacterSet());
        setEncodings(variant.getEncodings());
        setIdentifier(variant.getIdentifier());
        setLanguages(variant.getLanguages());
        setMediaType(variant.getMediaType());
        setModificationDate(modificationDate);
        setTag(tag);
    }

    /**
     * Constructor from a variant.
     * 
     * @param variant
     *            The variant to copy.
     * @param tag
     *            The tag.
     */
    public Representation(Variant variant, Tag tag) {
        this(variant, null, tag);
    }

    // [ifndef gwt] method
    /**
     * Check that the digest computed from the representation content and the
     * digest declared by the representation are the same.<br>
     * Since this method relies on the {@link #computeDigest(String)} method,
     * and since this method reads entirely the representation's stream, user
     * must take care of the content of the representation in case the latter is
     * transient.
     * 
     * {@link #isTransient}
     * 
     * @return True if both digests are not null and equals.
     * @deprecated Use {@link Representation#getDigester()} instead.
     */
    @Deprecated
    public boolean checkDigest() {
        return (getDigest() != null && checkDigest(getDigest().getAlgorithm()));
    }

    // [ifndef gwt] method
    /**
     * Check that the digest computed from the representation content and the
     * digest declared by the representation are the same. It also first checks
     * that the algorithms are the same.<br>
     * Since this method relies on the {@link #computeDigest(String)} method,
     * and since this method reads entirely the representation's stream, user
     * must take care of the content of the representation in case the latter is
     * transient.
     * 
     * {@link #isTransient}
     * 
     * @param algorithm
     *            The algorithm used to compute the digest to compare with. See
     *            constant values in {@link org.restlet.data.Digest}.
     * @return True if both digests are not null and equals.
     * @deprecated Use {@link Representation#getDigester()} instead.
     */
    @Deprecated
    public boolean checkDigest(String algorithm) {
        org.restlet.data.Digest digest = getDigest();
        if (digest != null) {
            if (algorithm.equals(digest.getAlgorithm())) {
                return digest.equals(computeDigest(algorithm));
            }
        }
        return false;
    }

    // [ifndef gwt] method
    /**
     * Compute the representation digest according to the given algorithm.<br>
     * Since this method reads entirely the representation's stream, user must
     * take care of the content of the representation in case the latter is
     * transient.
     * 
     * {@link #isTransient}
     * 
     * @param algorithm
     *            The algorithm used to compute the digest. See constant values
     *            in {@link org.restlet.data.Digest}.
     * @return The computed digest or null if the digest cannot be computed.
     * @deprecated Use {@link Representation#getDigester()} instead.
     */
    @Deprecated
    public org.restlet.data.Digest computeDigest(String algorithm) {
        org.restlet.data.Digest result = null;

        if (isAvailable()) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest
                        .getInstance(algorithm);
                java.security.DigestInputStream dis = new java.security.DigestInputStream(
                        getStream(), md);
                org.restlet.engine.io.ByteUtils.exhaust(dis);
                result = new org.restlet.data.Digest(algorithm, md.digest());
            } catch (java.security.NoSuchAlgorithmException e) {
                Context.getCurrentLogger().log(Level.WARNING,
                        "Unable to check the digest of the representation.", e);
            } catch (IOException e) {
                Context.getCurrentLogger().log(Level.WARNING,
                        "Unable to check the digest of the representation.", e);
            }
        }

        return result;
    }

    /**
     * Exhaust the content of the representation by reading it and silently
     * discarding anything read.
     * 
     * @return The number of bytes consumed or -1 if unknown.
     */
    public long exhaust() throws IOException {
        long result = -1L;
        // [ifndef gwt]
        if (isAvailable()) {
            result = ByteUtils.exhaust(getStream());
        }
        // [enddef]

        return result;
    }

    /**
     * Returns the size effectively available. This returns the same value as
     * {@link #getSize()} if no range is defined, otherwise it returns the size
     * of the range using {@link Range#getSize()}.
     * 
     * @return The available size.
     */
    public long getAvailableSize() {
        // [ifndef gwt]
        if (getRange() == null) {
            return getSize();
        } else if (getRange().getSize() != Range.SIZE_MAX) {
            return getRange().getSize();
        } else if (getSize() != Representation.UNKNOWN_SIZE) {
            if (getRange().getIndex() != Range.INDEX_LAST) {
                return getSize() - getRange().getIndex();
            }
            return getSize();
        }

        return Representation.UNKNOWN_SIZE;
        // [enddef]
        // [ifdef gwt] line uncomment
        // return getSize();
    }

    // [ifndef gwt] member
    /**
     * Returns a channel with the representation's content.<br>
     * If it is supported by a file, a read-only instance of FileChannel is
     * returned.<br>
     * This method is ensured to return a fresh channel for each invocation
     * unless it is a transient representation, in which case null is returned.
     * 
     * @return A channel with the representation's content.
     * @throws IOException
     */
    public abstract java.nio.channels.ReadableByteChannel getChannel()
            throws IOException;

    // [ifndef gwt] method
    /**
     * Returns the representation digest if any.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-MD5" header.
     * 
     * @return The representation digest or null.
     */
    public org.restlet.data.Digest getDigest() {
        return this.digest;
    }

    // [ifndef gwt] method
    /**
     * Return a Digester representation that wraps the current representation
     * and helps computing its digest value. By default, the instance relies on
     * the {@link org.restlet.data.Digest#ALGORITHM_MD5} digest algorithm.
     * 
     * @return A Digester representation that wraps the current representation.
     */
    public DigesterRepresentation getDigester() {
        DigesterRepresentation result = null;

        try {
            result = new DigesterRepresentation(this);
        } catch (java.security.NoSuchAlgorithmException e) {
            Context.getCurrentLogger().log(Level.WARNING,
                    "Unable to get the digester representation", e);
        }

        return result;
    }

    // [ifndef gwt] method
    /**
     * Return a Digester representation that wraps the current representation
     * and helps computing its digest value according to the given algorithm.
     * 
     * @param algorithm
     *            The digest algorithm
     * 
     * @return A Digester representation that wraps the current representation.
     */
    public DigesterRepresentation getDigester(String algorithm) {
        DigesterRepresentation result = null;

        try {
            result = new DigesterRepresentation(this, algorithm);
        } catch (java.security.NoSuchAlgorithmException e) {
            Context.getCurrentLogger().log(Level.WARNING,
                    "Unable to get the digester representation", e);
        }

        return result;
    }

    /**
     * Returns the suggested download file name for this representation. This is
     * mainly used to suggest to the client a local name for a downloaded
     * representation. Note that in order for this property to be sent from
     * servers to clients, you also need to call
     * {@link #setDownloadable(boolean)} with a 'true' value.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-Disposition" header with this value:
     * "attachment; filename=<downloadName>".
     * 
     * @return The suggested file name for this representation.
     */
    public String getDownloadName() {
        return this.downloadName;
    }

    /**
     * Returns the future date when this representation expire. If this
     * information is not known, returns null.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Expires" header.
     * 
     * @return The expiration date.
     */
    public Date getExpirationDate() {
        return this.expirationDate;
    }

    /**
     * Returns the range where in the full content the partial content available
     * should be applied.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-Range" header.
     * 
     * @return The content range or null if the full content is available.
     */
    public Range getRange() {
        return this.range;
    }

    /**
     * Returns a characters reader with the representation's content. This
     * method is ensured to return a fresh reader for each invocation unless it
     * is a transient representation, in which case null is returned. If the
     * representation has no character set defined, the system's default one
     * will be used.
     * 
     * @return A reader with the representation's content.
     * @throws IOException
     */
    public abstract Reader getReader() throws IOException;

    /**
     * Returns the size in bytes if known, UNKNOWN_SIZE (-1) otherwise.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-Length" header.
     * 
     * @return The size in bytes if known, UNKNOWN_SIZE (-1) otherwise.
     */
    public long getSize() {
        return this.size;
    }

    /**
     * Returns a stream with the representation's content. This method is
     * ensured to return a fresh stream for each invocation unless it is a
     * transient representation, in which case null is returned.
     * 
     * @return A stream with the representation's content.
     * @throws IOException
     */
    public abstract InputStream getStream() throws IOException;

    // [ifndef gwt] method
    /**
     * Converts the representation to a string value. Be careful when using this
     * method as the conversion of large content to a string fully stored in
     * memory can result in OutOfMemoryErrors being thrown.
     * 
     * @return The representation as a string value.
     */
    public String getText() throws IOException {
        String result = null;

        if (isAvailable()) {
            final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            write(baos);

            if (getCharacterSet() != null) {
                result = baos.toString(getCharacterSet().getName());
            } else {
                result = baos.toString();
            }
        }

        return result;
    }

    // [ifdef gwt] method uncomment
    // /**
    // * Converts the representation to a string value. Be careful when using
    // * this method as the conversion of large content to a string fully
    // * stored in memory can result in OutOfMemoryErrors being thrown.
    // *
    // * @return The representation as a string value.
    // */
    // public abstract String getText() throws IOException;

    /**
     * Indicates if some fresh content is available, without having to actually
     * call one of the content manipulation method like getStream() that would
     * actually consume it. This is especially useful for transient
     * representation whose content can only be accessed once and also when the
     * size of the representation is not known in advance.
     * 
     * @return True if some fresh content is available.
     */
    public boolean isAvailable() {
        return (getSize() != 0) && this.available;
    }

    /**
     * Indicates if the representation is downloadable which means that it can
     * be obtained via a download dialog box.
     * 
     * @return True if the representation's content is downloadable.
     */
    public boolean isDownloadable() {
        return this.downloadable;
    }

    /**
     * Indicates if the representation's content is transient, which means that
     * it can be obtained only once. This is often the case with representations
     * transmitted via network sockets for example. In such case, if you need to
     * read the content several times, you need to cache it first, for example
     * into memory or into a file.
     * 
     * @return True if the representation's content is transient.
     */
    public boolean isTransient() {
        return this.isTransient;
    }

    /**
     * Releases the representation's content and all associated objects like
     * sockets, channels or files. If the representation is transient and hasn't
     * been read yet, all the remaining content will be discarded, any open
     * socket, channel, file or similar source of content will be immediately
     * closed. The representation is also no more available.
     */
    public void release() {
        this.available = false;
    }

    /**
     * Indicates if some fresh content is available.
     * 
     * @param available
     *            True if some fresh content is available.
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    // [ifndef gwt] method
    /**
     * Sets the representation digest.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-MD5" header.
     * 
     * @param digest
     *            The representation digest.
     */
    public void setDigest(org.restlet.data.Digest digest) {
        this.digest = digest;
    }

    /**
     * Indicates if the representation is downloadable which means that it can
     * be obtained via a download dialog box.
     * 
     * @param downloadable
     *            True if the representation's content is downloadable.
     */
    public void setDownloadable(boolean downloadable) {
        this.downloadable = downloadable;
    }

    /**
     * Set the suggested download file name for this representation. Note that
     * in order for this property to be sent from servers to clients, you also
     * need to call {@link #setDownloadable(boolean)} with a 'true' value.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-Disposition" header with this value:
     * "attachment; filename=<downloadName>".
     * 
     * @param fileName
     *            The suggested file name.
     */
    public void setDownloadName(String fileName) {
        this.downloadName = fileName;
    }

    /**
     * Sets the future date when this representation expire. If this information
     * is not known, pass null.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Expires" header.
     * 
     * @param expirationDate
     *            The expiration date.
     */
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = DateUtils.unmodifiable(expirationDate);
    }

    /**
     * Sets the range where in the full content the partial content available
     * should be applied.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-Range" header.
     * 
     * @param range
     *            The content range.
     */
    public void setRange(Range range) {
        this.range = range;
    }

    /**
     * Sets the expected size in bytes if known, -1 otherwise.<br>
     * <br>
     * Note that when used with HTTP connectors, this property maps to the
     * "Content-Length" header.
     * 
     * @param expectedSize
     *            The expected size in bytes if known, -1 otherwise.
     */
    public void setSize(long expectedSize) {
        this.size = expectedSize;
    }

    /**
     * Indicates if the representation's content is transient.
     * 
     * @param isTransient
     *            True if the representation's content is transient.
     */
    public void setTransient(boolean isTransient) {
        this.isTransient = isTransient;
    }

    // [ifndef gwt] member
    /**
     * Writes the representation to a byte stream. This method is ensured to
     * write the full content for each invocation unless it is a transient
     * representation, in which case an exception is thrown.<br>
     * <br>
     * Note that the class implementing this method shouldn't flush or close the
     * given {@link OutputStream} after writing to it as this will be handled by
     * the Restlet connectors automatically.
     * 
     * @param outputStream
     *            The output stream.
     * @throws IOException
     */
    public abstract void write(OutputStream outputStream) throws IOException;

    // [ifndef gwt] member
    /**
     * Writes the representation to a byte channel. This method is ensured to
     * write the full content for each invocation unless it is a transient
     * representation, in which case an exception is thrown.
     * 
     * @param writableChannel
     *            A writable byte channel.
     * @throws IOException
     */
    public abstract void write(
            java.nio.channels.WritableByteChannel writableChannel)
            throws IOException;

    // [ifndef gwt] member
    /**
     * Writes the representation to a characters writer. This method is ensured
     * to write the full content for each invocation unless it is a transient
     * representation, in which case an exception is thrown.<br>
     * <br>
     * Note that the class implementing this method shouldn't flush or close the
     * given {@link java.io.Writer} after writing to it as this will be handled
     * by the Restlet connectors automatically.
     * 
     * @param writer
     *            The characters writer.
     * @throws IOException
     */
    public abstract void write(java.io.Writer writer) throws IOException;

}
