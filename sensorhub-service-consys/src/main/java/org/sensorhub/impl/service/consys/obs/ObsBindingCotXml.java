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

import com.ctc.wstx.api.WstxOutputProperties;
import com.google.common.collect.Sets;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.service.consys.ResourceParseException;
import org.sensorhub.impl.service.consys.SWECommonUtils;
import org.sensorhub.impl.service.consys.ServiceErrors;
import org.sensorhub.impl.service.consys.obs.ObsHandler.ObsHandlerContextData;
import org.sensorhub.impl.service.consys.resource.*;
import org.sensorhub.utils.SWEDataUtils;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataRecordImpl;
import org.vast.data.XMLEncodingImpl;
import org.vast.swe.BinaryDataWriter;
import org.vast.swe.SWEConstants;
import org.vast.swe.ScalarIndexer;
import org.vast.swe.fast.*;
import org.vast.swe.helper.GeoPosHelper;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.vast.util.ReaderException;
import org.vast.xml.DOMHelper;
import net.opengis.swe.v20.DataRecord;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.vast.swe.helper.CoTHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.sensorhub.impl.service.consys.SWECommonUtils.OM_COMPONENTS_FILTER;


public class ObsBindingCotXml extends ResourceBindingCotXml<BigId, IObsData>
{
    ObsHandlerContextData contextData;
    IObsStore obsStore;
    XmlDataParser resultReader;
    CotDataReader cotReader; // unused...
    CotDataWriter cotWriter = new CotDataWriter();
    Map<BigId, AbstractDataWriter> resultWriters;
    DOMHelper dom;
    ScalarIndexer timeStampIndexer;
    XMLOutputFactory factory = XMLOutputFactory.newInstance();
    OutputStream os = ctx.getOutputStream();
    IFoiStore foiStore;
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

//    @Override
//    public IObsData deserialize(CotDataReader xmlReader) throws IOException {
//        var obs = new ObsData.Builder()
//                .withDataStream(contextData.dsID);
//
//        CoTHelper fac = new CoTHelper();
//
//        String vers = "2.0";
//
//        dataRec = (DataRecord) fac.createRecord()
//                .label("<event>")
//                .addField("version", fac.createCoTVersion())
//                .addField("type", fac.createType())
//                .addField("uid", fac.createUID())
//                .addField("time", fac.createPrecisionTimeStamp())
//                .addField("start-time", fac.createStartTime())
//                .addField("stale-time", fac.createStaleTime())
//                .addField("how", fac.createHow())
//                .label("</event>")
//                .label("<point>")
//                .addField("lat", fac.createLatitude())
//                .addField("lon", fac.createLongitude())
//                .addField("ce", fac.createCE())
//                // hae here?.....
//                .addField("le", fac.createLE())
//                .label("</point>")
//                .label("<detail>")
//                // <track course="0" speed="0"/>
//                .label("<remarks>")
//                .addField("source", fac.createRmkSrc())
//                .addField("time", fac.createPrecisionTimeStamp())
//                .label("</remarks>")
//                .label("</detail>");
//
////        try {
////            while (xmlReader.hasNext()) {
////                xmlReader.next();
////
////                //var propName = reader.nextName();
////
////                if (xmlReader.isStartElement()) {
////                    String propName = xmlReader.getLocalName();
////
////                    if ("phenomenonTime".equals(propName))
////                        obs.withPhenomenonTime(OffsetDateTime.parse(xmlReader.getElementText()).toInstant());
////                    else if ("resultTime".equals(propName))
////                        obs.withResultTime(OffsetDateTime.parse(xmlReader.getElementText()).toInstant());
////                    else if ("foi@id".equals(propName)) {
////                        try {
////                            var foiID = idEncoders.getFoiIdEncoder().decodeID(xmlReader.getElementText());
////                            obs.withFoi(foiID);
////                        } catch (IllegalArgumentException e) {
////                            throw ServiceErrors.badRequest("Invalid FOI ID");
////                        } catch (XMLStreamException e) {
////                            throw new RuntimeException(e);
////                        }
////                    } else if ("result".equals(propName)) {
////                        //var result = xmlReader.next();
////                        obs.withResult((DataBlock) dataRec);
////                    }
////                }
////            }
////        }
////        catch (DateTimeParseException e)
////        {
////            throw new ResourceParseException(INVALID_XML_ERROR_MSG + "Invalid ISO8601 date/time at " + xmlReader.getLocalName());
////        }
////        catch (IllegalStateException e)
////        {
////            throw new ResourceParseException(INVALID_XML_ERROR_MSG + e.getMessage());
////        } catch (XMLStreamException e) {
////            throw new RuntimeException(e);
////        }
//
//        if (contextData.foiId != null && contextData.foiId != BigId.NONE)
//            obs.withFoi(contextData.foiId);
//
//        var newObs = obs.build();
//
//        // set timestamp in result data if present in schema
//        if (timeStampIndexer != null)
//        {
//            var phenomenonTimeIdx = timeStampIndexer.getDataIndex(newObs.getResult());
//            newObs.getResult().setDoubleValue(phenomenonTimeIdx, newObs.getPhenomenonTime().toEpochMilli() / 1000.0);
//        }
//
//        return newObs;
//    }

    @Override
    public void serialize(BigId key, IObsData obs, boolean showLinks, CotDataWriter xmlWriter) throws IOException, XMLStreamException {
        Set<Integer> locationComponents = new HashSet<>();
        var dataStream = this.obsStore.getDataStreams().get(new DataStreamKey(obs.getDataStreamID()));

        for (int i = 0; i < dataStream.getRecordStructure().getComponentCount(); i++) {
            var component = dataStream.getRecordStructure().getComponent(i);
            if (LOCATION_DEFINITIONS.contains(component.getDefinition())) {
                // This is how we know we have location components in the data structure
                // So we can save this and parse specifically the location components into GeoJSON
                locationComponents.add(i);
            }
        }

        var obsId = idEncoders.getObsIdEncoder().encodeID(key);

        cotWriter.setOutput(os);

        cotWriter.writeStartElement("event");
        cotWriter.writeCotAttribute("version", "2.0");
        cotWriter.writeCotAttribute("uid", obsId);

        double longitude = 0;
        double latitude = 0;

        for (int index : locationComponents) {
            var locationDataBlock = ((DataBlockMixed) obs.getResult()).getUnderlyingObject()[index];
            // You'll still need to check if these are real values and not null
            latitude = locationDataBlock.getDoubleValue(0);
            longitude = locationDataBlock.getDoubleValue(1);
        }

        var recordStruct = ((DataRecordImpl) dataStream.getRecordStructure()).getFieldList();

        var resultBlock = ((DataBlockMixed) obs.getResult()).getUnderlyingObject();

        cotWriter.writeCotStartElement("event");

        int i = 0;

        for (AbstractDataBlock abstractResultBlock : resultBlock) {
            String value = abstractResultBlock.getStringValue();
            String fieldName = recordStruct.get(i).getName();

            OffsetDateTime timeResult = null;

            switch (fieldName) {
                case "version":
                    cotWriter.writeCotAttribute("version", value);
                    break;
                case "type":
                    cotWriter.writeCotAttribute("type", value);
                    break;
                case "uid":
                    cotWriter.writeCotAttribute("uid", value);
                    break;
                case "time":
                    timeResult = abstractResultBlock.getDateTime();
                    cotWriter.writeCotAttribute("time", timeResult.toString());
                    break;
                case "start":
                    timeResult = abstractResultBlock.getDateTime();
                    cotWriter.writeCotAttribute("start time", timeResult.toString());
                    break;
                case "stale": // how would i do the plus 1 year here
                    timeResult = abstractResultBlock.getDateTime();
                    cotWriter.writeCotAttribute("stale time", timeResult.toString());
                    break;
                case "how":
                    cotWriter.writeCotAttribute("how", value);
                    break;
                case "lat":
                    cotWriter.writeCotAttribute("lon", value);
                    break;
                case "ce":
                    cotWriter.writeCotAttribute("ce", value);
                    break;
                case "le":
                    cotWriter.writeCotAttribute("le", value);
                    break;
                case "source":
                    cotWriter.writeCotAttribute("source", value);
                    break;
            }
        }

//        if (!(longitude == 0.0 && latitude == 0.0)) {
//
//        }

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
//        if (!SWECommonUtils.allowNonBinaryFormat(dsInfo.getRecordStructure(), dsInfo.getRecordEncoding()))
//            return new BinaryDataWriter(); // i see i cant return BinaryDataWriter because this method and the one before are now
                                           // AbstractDataWriter instead of DataStreamWriter, but idk what else to return...
                                           // the auto fix just creates more errors and issues. i dont wanna meddle too much in other files
                                           // not too sure what this does at all tbh

////        if (dsInfo.getRecordEncoding() instanceof TextEncodingImpl) {
////            throw new IOException("Text encoding not supported for application/cot+xml");
////        }

        // create cot/xml writer
        //XmlDataWriter dataWriter = new XmlDataWriter(); //wraps writer. when you return writer,
        // youre passing in a writer that knows how to write the specific xml data it needs.
        //CotWriter responsible for writing cot spec compliant info

        CotDataWriter dataWriter = new CotDataWriter();
        //xmlcotwriter extends xmldatawriter. new class. same with xmlcotparser and reader. convert it back into a swecommondata record that can be put in osh

        dataWriter.setDataEncoding(new XMLEncodingImpl());
        dataWriter.setOutput(os);
        dataWriter.setDataComponents(dsInfo.getRecordStructure());
        //cotWriter.setDataEncoding(dsInfo.getRecordEncoding());

        // filter out components that are already included in O&M
        dataWriter.setDataComponentFilter(OM_COMPONENTS_FILTER);
        return dataWriter;
    }

    protected XmlDataParser getSweCommonParser(IDataStreamInfo dsInfo, InputStream is) throws IOException {
        //should be cotxmlparser... soon
        // do i have to make a parser actually.....?

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
        cotWriter.close();
        cotWriter.flush();
    }
}
