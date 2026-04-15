/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.utils;

import net.opengis.swe.v20.*;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ProtoWriter {

    private final StringBuilder sb;
    private int indentLevel = 0;
    // Protobuf fields MUST have a unique tag number starting at 1
    private int fieldTagCounter = 1;

    public ProtoWriter() {
        this.sb = new StringBuilder();
    }

    public void writeProto(String systemName, IDataStreamInfo dsInfo) throws IOException {
        startDocument();
        buildProtoForComponent(dsInfo.getRecordStructure());
        endDocument(systemName, dsInfo.getOutputName());
    }

    private void startDocument() {
        sb.append("syntax = \"proto3\";\n\n");
        sb.append("message Record {\n");
        indentLevel++;
    }

    private void buildProtoForComponent(DataComponent component) {
        String name = cleanName(component.getName());

        if (component instanceof DataChoice choice) {
            writeLine("oneof " + name + " {");
            indentLevel++;
            int tempTag = 1;
            for (DataComponent item : choice.getItemList()) {
                writeField(mapType(item), item.getName(), tempTag++);
            }
            indentLevel--;
            writeLine("}");
        }
        else if (component instanceof Category || component instanceof CategoryRange) {
            writeField("string", name, fieldTagCounter++);
        }
        else if (component.getComponentCount() > 0) {

            writeLine("message " + capitalize(name) + " {");
            indentLevel++;
            int savedCounter = fieldTagCounter;
            fieldTagCounter = 1;
            for (int i = 0; i < component.getComponentCount(); i++) {
                buildProtoForComponent(component.getComponent(i));
            }
            indentLevel--;
            writeLine("}");
            writeField(capitalize(name), name, savedCounter);
            fieldTagCounter = savedCounter + 1;
        }
        else {
            // Simple scalars
            writeField(mapType(component), name, fieldTagCounter++);
        }
    }

    private String mapType(DataComponent comp) {
        if (comp instanceof Time) return "uint64";
        if (comp instanceof ScalarComponent scalar) {
            return switch (scalar.getDataType()) {
                case BOOLEAN -> "bool";
                case DOUBLE, FLOAT -> "double";
                case INT, SHORT, BYTE -> "int32";
                case LONG -> "int64";
                default -> "string";
            };
        }
        return "string";
    }

    private void writeField(String type, String name, int tag) {
        writeLine(type + " " + cleanName(name) + " = " + tag + ";");
    }

    private void writeLine(String text) {
        sb.append("  ".repeat(indentLevel)).append(text).append("\n");
    }

    private String cleanName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void endDocument(String systemName, String streamName) throws IOException {
        indentLevel--;
        sb.append("}\n");

        systemName = systemName.replace(' ', '_');
        File file = new File("./schemas/" + systemName + "_" + streamName + ".proto");
        file.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(sb.toString());
        }
    }
}