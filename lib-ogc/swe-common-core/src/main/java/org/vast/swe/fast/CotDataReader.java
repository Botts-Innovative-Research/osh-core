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
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * New implementation of XML data writer with better efficiency since the 
 * write tree is pre-computed during init instead of being re-evaluated
 * while iterating through the component tree.
 * </p>
 *
 * @author Ashley Poteau
 * @since Dec 2, 2025
 */

// xmlstreamreader
// or xmldataparser?
public class CotDataReader extends XmlDataParser {
    static final String COT_ERROR = "Error writing XML stream for ";
    private final Reader in;

    protected XMLStreamReader xmlReader;
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

    public CotDataReader(Reader in) {
        this.in = Objects.requireNonNull(in, "in == null");
    }


}
