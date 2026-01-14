/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.vast.swe.fast;

import com.ctc.wstx.api.WstxOutputProperties;
import com.google.gson.FormattingStyle;
import com.google.gson.Strictness;
import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.vast.data.AbstractArrayImpl;
import org.vast.data.XMLEncodingImpl;
import org.vast.swe.SWEDataTypeUtils;
import org.vast.util.DateTimeFormat;
import org.vast.util.WriterException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * <p>
 * New implementation of XML data writer with better efficiency since the 
 * write tree is pre-computed during init instead of being re-evaluated
 * while iterating through the component tree.
 * </p>
 *
 * @author Ashley Poteau
 * @since Oct 31, 2025
 */
// xmlstreamwriter
// xmldatawriter?
public class CotDataWriter extends XmlDataWriter {
    static final String COT_ERROR = "Error writing XML stream for ";
    
    protected XMLStreamWriter xmlWriter;
    protected String namespace;
    protected String prefix;
    protected Map<String, XmlDataWriter.IntegerWriter> countWriters = new HashMap<>();
    private Strictness strictness = Strictness.LEGACY_STRICT;
    private boolean serializeNulls = true;
    private FormattingStyle formattingStyle;
    // These fields cache data derived from the formatting style, to avoid having to
    // re-evaluate it every time something is written
    private String formattedColon;
    private String formattedComma;
    private boolean usesEmptyNewlineAndIndent;


    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        xmlWriter.writeStartElement(namespaceURI, localName);
    }

    public void writeCotCharacters(String text) throws XMLStreamException {
        if (xmlWriter == null) {
            throw new IllegalStateException("XMLStreamWriter not initialized. Call setOutput() first.");
        }
        xmlWriter.writeCharacters(text);
    }

    public void writeCotStartElement(String text) throws XMLStreamException {
        if (xmlWriter == null) {
            throw new IllegalStateException("XMLStreamWriter not initialized. Call setOutput() first.");
        }
        xmlWriter.writeStartElement(text);
    }

    public void writeCotAttribute(String var1, String var2) throws XMLStreamException {
        if (xmlWriter == null) {
            throw new IllegalStateException("XMLStreamWriter not initialized. Call setOutput() first.");
        }
        xmlWriter.writeAttribute(var1, var2);
    }

    public final void setStrictness(Strictness strictness) {
        this.strictness = Objects.requireNonNull(strictness);
    }

    public final Strictness getStrictness() {
        return strictness;
    }

    public final void setSerializeNulls(boolean serializeNulls) {

        this.serializeNulls = serializeNulls;
    }

    /**
     * Returns true if object members are serialized when their value is null. This has no impact on
     * array elements. The default is true.
     */
    public final boolean getSerializeNulls() {
        return serializeNulls;
    }

    public final void setFormattingStyle(FormattingStyle formattingStyle) {
        this.formattingStyle = Objects.requireNonNull(formattingStyle);

        this.formattedComma = ",";
        if (this.formattingStyle.usesSpaceAfterSeparators()) {
            this.formattedColon = ": ";

            // Only add space if no newline is written
            if (this.formattingStyle.getNewline().isEmpty()) {
                this.formattedComma = ", ";
            }
        } else {
            this.formattedColon = ":";
        }

        this.usesEmptyNewlineAndIndent =
                this.formattingStyle.getNewline().isEmpty() && this.formattingStyle.getIndent().isEmpty();
    }


    public final void setIndent(String indent) {
        if (indent.isEmpty()) {
            setFormattingStyle(FormattingStyle.COMPACT);
        } else {
            setFormattingStyle(FormattingStyle.PRETTY.withIndent(indent));
        }
    }

}
