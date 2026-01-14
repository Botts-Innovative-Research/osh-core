/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.resource;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import org.sensorhub.api.common.IdEncoders;
import org.vast.swe.fast.CotDataReader;
import org.vast.swe.fast.CotDataWriter;
import org.vast.xml.XMLImplFinder;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;


/**
 * <p>
 * Base class for all XML resource formatters
 * </p>
 * 
 * @param <K> Resource Key
 * @param <V> Resource Object
 *
 * @author Ashley Poteau
 * @since Dec 3, 2025
 */
public abstract class ResourceBindingCotXml<K, V> extends ResourceBinding<K, V>
{
    public static final String INVALID_XML_ERROR_MSG = "Invalid XML: ";
    public static final String MISSING_PROP_ERROR_MSG = "Missing property: ";

//    protected final XMLStreamReader xmlReader;
//    protected final XMLStreamWriter xmlWriter;

    protected final CotDataReader cotReader;
    protected final CotDataWriter cotWriter;

    protected boolean isCollection;

    Set<String> excludedProps;
    Set<String> includedProps;

    protected ResourceBindingCotXml(RequestContext ctx, IdEncoders idEncoders, boolean forReading) throws IOException
    {
        super(ctx, idEncoders);

        try
        {
            if (forReading)
            {
                var is = new BufferedInputStream(ctx.getInputStream());
                cotReader = getCotReader(is);
                cotWriter = null;
            }
            else
            {
                var os = ctx.getOutputStream();
                cotWriter = getCotWriter(os);
                cotReader = null;
            }
        }
        catch (Exception e)
        {
            throw new IOException("Error initializing COT XML bindings", e);
        }
    }

    public abstract V deserialize(CotDataReader xmlReader) throws IOException;
    public abstract void serialize(K key, V res, boolean showLinks, CotDataWriter xmlWriter) throws IOException, XMLStreamException;


    protected CotDataReader getCotReader(InputStream is) throws IOException
    {
        var osr = new InputStreamReader(is, StandardCharsets.UTF_8);
        return new CotDataReader(osr);
    }

    protected CotDataWriter getCotWriter(OutputStream os) throws IOException
    {
        try {
            CotDataWriter writer = new CotDataWriter();
            writer.setOutput(os);
            return writer;
        } catch (Exception e) {
            throw new IOException("Error creating COT writer", e);
        }
    }


    @Override
    public V deserialize() throws IOException
    {
        return deserialize(this.cotReader);
    }

    @Override
    public void serialize(K key, V res, boolean showLinks) throws IOException, XMLStreamException {
        serialize(key, res, showLinks, this.cotWriter);
    }

    @Override
    public void startCollection() throws IOException, XMLStreamException {
        isCollection = true;
        if (cotWriter != null) {
            cotWriter.startStream(true);
        }
    }

    @Override
    public void endCollection(Collection<ResourceLink> links) throws IOException, XMLStreamException {
        if (cotWriter != null) {
            cotWriter.endStream();
            cotWriter.flush();
        }
    }

    protected String getItemsPropertyName()
    {
        return "items";
    }

}