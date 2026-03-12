/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.obs;

import com.google.common.collect.Sets;
import net.opengis.OgcPropertyList;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.service.consys.obs.ObsHandler.ObsHandlerContextData;
import org.sensorhub.impl.service.consys.resource.*;
import org.sensorhub.utils.SWEDataUtils;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataRecordImpl;
import org.vast.data.XMLEncodingImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.ScalarIndexer;
import org.vast.swe.fast.*;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.xml.DOMHelper;
import net.opengis.swe.v20.DataRecord;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.*;

import static org.sensorhub.impl.service.consys.SWECommonUtils.OM_COMPONENTS_FILTER;


public class ObsBindingCotXml extends ResourceBindingCotXml<BigId, IObsData>
{
    ObsHandlerContextData contextData;
    IObsStore obsStore;
    XmlDataParser resultReader;
    Map<BigId, AbstractDataWriter> resultWriters;
    DOMHelper dom;
    ScalarIndexer timeStampIndexer;
    XMLOutputFactory factory = XMLOutputFactory.newInstance();
    OutputStream os = ctx.getOutputStream();
    protected DataRecord dataRec;

    private static final Set<String> LOCATION_DEFINITIONS = Sets.newHashSet(
            SWEConstants.DEF_SENSOR_LOC,
            SWEConstants.DEF_PLATFORM_LOC,
            SWEConstants.DEF_SAMPLING_LOC,
            GeoPosHelper.DEF_LOCATION
    );

    protected ObsBindingCotXml(RequestContext ctx, IdEncoders idEncoders, boolean forReading, IObsStore obsStore) throws IOException, XMLStreamException {
        super(ctx, idEncoders, forReading);
        this.contextData = (ObsHandlerContextData)ctx.getData();
        this.obsStore = obsStore;

        dom = new DOMHelper();

        if (forReading)
        {
            var inputStream = ctx.getInputStream();
            resultReader = getSweCommonParser(contextData.dsInfo, inputStream);
            resultReader.setRenewDataBlock(true);
            timeStampIndexer = SWEDataUtils.getTimeStampIndexer(contextData.dsInfo.getRecordStructure());

        }
        else
        {
            this.resultWriters = new HashMap<>();

            // init result writer only in case of single datastream
            // otherwise we'll do it later
            if (contextData != null && contextData.dsInfo != null)
            {
                var resultWriter = getSweCommonWriter(contextData.dsInfo, os);
                resultWriters.put(ctx.getParentID(), resultWriter);
            }
        }
    }

    @Override
    public IObsData deserialize(CotDataReader xmlReader) throws IOException {
        var obs = new ObsData.Builder()
                .withDataStream(contextData.dsID);
        var newObs = obs.build();
        return newObs;
    }

    @Override
    public void serialize(BigId key, IObsData obs, boolean showLinks, CotDataWriter xmlWriter) throws IOException, XMLStreamException {
        Set<Integer> locationComponents = new HashSet<>();
        var dataStream = this.obsStore.getDataStreams().get(new DataStreamKey(obs.getDataStreamID()));

        OgcPropertyList<DataComponent> recordStruct = null;
        AbstractDataBlock[] resultBlock = null;


        for (int i = 0; i < dataStream.getRecordStructure().getComponentCount(); i++) {
            var component = dataStream.getRecordStructure().getComponent(i);
            if (LOCATION_DEFINITIONS.contains(component.getDefinition())) {
                // This is how we know we have location components in the data structure
                // So we can save this and parse specifically the location components into GeoJSON
                locationComponents.add(i);
            }
        }

        recordStruct = ((DataRecordImpl) dataStream.getRecordStructure()).getFieldList();

        resultBlock = ((DataBlockMixed) obs.getResult()).getUnderlyingObject();

        int i = 0;

        Map<String, String> unmatchedData = new HashMap<>();
        Map<String, String> pointData = new HashMap<>();
        Map<String, String> eventData = new HashMap<>();
        Map<String, String> remarksData = new HashMap<>();

        xmlWriter.writeCotAttribute("version", "2.0");

        for (AbstractDataBlock abstractResultBlock : resultBlock) {
            String value = abstractResultBlock.getStringValue();
            String fieldName = recordStruct.get(i).getName();

            OffsetDateTime timeResult = null;

            switch (fieldName) {
                case "version": // event
                    eventData.put("version", "2.0");
                    break;
                case "type": // event
                    eventData.put("type", value);
                    break;
                case "uuid", "uid": // event
                    eventData.put("uid", value);
                    break;
                case "time": // event
                    timeResult = abstractResultBlock.getDateTime();
                    eventData.put("time", timeResult.toString());
                    break;
                case "start": // event
                    timeResult = abstractResultBlock.getDateTime();
                    eventData.put("start time", timeResult.toString());
                    break;
                case "stale": // event
                    timeResult = abstractResultBlock.getDateTime().plusYears(1);
                    eventData.put("stale time", timeResult.toString());
                    break;
                case "how": // event
                    eventData.put("how", value);
                    break;
                case "ce": // point
                    pointData.put("ce", value);
                    break;
                case "hae": // point
                    pointData.put("hae", value);
                    break;
                case "le": // point
                    pointData.put("le", value);
                    break;
                case "source": // remarks
                    remarksData.put("source", value);
                    break;
                case "location": // point
                    var locationBlockCount = abstractResultBlock.getAtomCount();
                    String lat, lon, alt;
                    if (locationBlockCount == 2) {
                        lat = resultBlock[i].getStringValue(0);
                        lon = resultBlock[i].getStringValue(1);

                        pointData.put("lat", lat);
                        pointData.put("lon", lon);
                        break;
                    } else if (locationBlockCount == 3) {
                        lat = resultBlock[i].getStringValue(0);
                        lon = resultBlock[i].getStringValue(0);
                        alt = resultBlock[i].getStringValue(0);

                        pointData.put("lat", lat);
                        pointData.put("lon", lon);
                        pointData.put("alt", alt);
                        break;
                    } else {
                        break;
                    }
                default:
                    unmatchedData.put(fieldName, value);
            }
            i++;
        }

        eventData.entrySet().forEach(entry -> {
            try {
                xmlWriter.writeCotAttribute(entry.getKey(), entry.getValue());
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });

        cotWriter.writeCotStartElement("detail");
        unmatchedData.entrySet().forEach(entry -> {
            try {
                xmlWriter.writeCotAttribute(entry.getKey(), entry.getValue());
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });

        cotWriter.writeCotStartElement("remarks");
        remarksData.entrySet().forEach(entry -> {
            try {
                xmlWriter.writeCotAttribute(entry.getKey(), entry.getValue());
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        cotWriter.writeCotEndElement();

        cotWriter.writeCotEndElement();

        cotWriter.writeCotStartElement("point");
        pointData.entrySet().forEach(entry -> {
            try {
                xmlWriter.writeCotAttribute(entry.getKey(), entry.getValue());
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        cotWriter.writeCotEndElement();

        cotWriter.endStream();
    }


    protected CotDataWriter getCotWriter(OutputStream os) throws IOException {
        var writer = super.getCotWriter(os);
        writer.setSerializeNulls(true);
        return writer;
    }

    protected AbstractDataWriter getSweCommonWriter(BigId dsID, OutputStream os) throws IOException {
        var dsInfo = obsStore.getDataStreams().get(new DataStreamKey(dsID));

        return getSweCommonWriter(dsInfo, os);
    }

    protected AbstractDataWriter getSweCommonWriter(IDataStreamInfo dsInfo, OutputStream os) throws IOException {
        CotDataWriter dataWriter = new CotDataWriter();

        dataWriter.setDataEncoding(new XMLEncodingImpl());
        dataWriter.setOutput(os);
        dataWriter.setDataComponents(dsInfo.getRecordStructure());

        // filter out components that are already included in O&M
        dataWriter.setDataComponentFilter(OM_COMPONENTS_FILTER);
        return dataWriter;
    }

    protected XmlDataParser getSweCommonParser(IDataStreamInfo dsInfo, InputStream is) throws IOException {
        // create XML SWE parser
        var sweParser = new XmlDataParser();
        sweParser.setDataComponents(dsInfo.getRecordStructure());

        // filter out components that are already included in O&M
        sweParser.setDataComponentFilter(OM_COMPONENTS_FILTER);
        sweParser.setInput(is);
        return sweParser;
    }

    @Override
    public void startCollection() throws XMLStreamException, IOException {
        super.startCollection();
    }

    @Override
    public void endCollection(Collection<ResourceLink> links) throws IOException, XMLStreamException {
        super.endCollection(links);
    }
}
