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
public class CotDataWriter implements XMLStreamWriter {
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

    @Override
    public void writeStartElement(String s) throws XMLStreamException {

    }

    @Override
    public void writeStartElement(String s, String s1) throws XMLStreamException {

    }

    @Override
    public void writeStartElement(String s, String s1, String s2) throws XMLStreamException {

    }

    @Override
    public void writeEmptyElement(String s, String s1) throws XMLStreamException {

    }

    @Override
    public void writeEmptyElement(String s, String s1, String s2) throws XMLStreamException {

    }

    @Override
    public void writeEmptyElement(String s) throws XMLStreamException {

    }

    @Override
    public void writeEndElement() throws XMLStreamException {

    }

    @Override
    public void writeEndDocument() throws XMLStreamException {

    }

    @Override
    public void close() throws XMLStreamException {

    }

    @Override
    public void flush() throws XMLStreamException {

    }

    @Override
    public void writeAttribute(String s, String s1) throws XMLStreamException {

    }

    @Override
    public void writeAttribute(String s, String s1, String s2, String s3) throws XMLStreamException {

    }

    @Override
    public void writeAttribute(String s, String s1, String s2) throws XMLStreamException {

    }

    @Override
    public void writeNamespace(String s, String s1) throws XMLStreamException {

    }

    @Override
    public void writeDefaultNamespace(String s) throws XMLStreamException {

    }

    @Override
    public void writeComment(String s) throws XMLStreamException {

    }

    @Override
    public void writeProcessingInstruction(String s) throws XMLStreamException {

    }

    @Override
    public void writeProcessingInstruction(String s, String s1) throws XMLStreamException {

    }

    @Override
    public void writeCData(String s) throws XMLStreamException {

    }

    @Override
    public void writeDTD(String s) throws XMLStreamException {

    }

    @Override
    public void writeEntityRef(String s) throws XMLStreamException {

    }

    @Override
    public void writeStartDocument() throws XMLStreamException {

    }

    @Override
    public void writeStartDocument(String s) throws XMLStreamException {

    }

    @Override
    public void writeStartDocument(String s, String s1) throws XMLStreamException {

    }

    @Override
    public void writeCharacters(String s) throws XMLStreamException {

    }

    @Override
    public void writeCharacters(char[] chars, int i, int i1) throws XMLStreamException {

    }

    @Override
    public String getPrefix(String s) throws XMLStreamException {
        return "";
    }

    @Override
    public void setPrefix(String s, String s1) throws XMLStreamException {

    }

    @Override
    public void setDefaultNamespace(String s) throws XMLStreamException {

    }

    @Override
    public void setNamespaceContext(NamespaceContext namespaceContext) throws XMLStreamException {

    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;
    }

    @Override
    public Object getProperty(String s) throws IllegalArgumentException {
        return null;
    }

}
