/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.service.kml;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;

public class TestKmlStyleMapper {

    private static final String DEFAULT_STYLE_URL = "http://example.com/style#myStyle";

    private static BundleContext context;

    @Test
    public void testGetStyleForMetacardStringAttribute() {
        Metacard metacard = new MockMetacard();
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE, DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardBooleanAttribute() {
        MetacardImpl metacard = new MockMetacard(AttributeFormat.BOOLEAN.toString(), true);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.BOOLEAN.toString(), String.valueOf(true),
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardXmlAttribute() {
        Metacard metacard = new MockMetacard();
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(Metacard.METADATA, MockMetacard.DEFAULT_METADATA, DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardGeoAttribute() {
        Metacard metacard = new MockMetacard();
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(Metacard.GEOGRAPHY, MockMetacard.DEFAULT_LOCATION, DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardDateAttribute() {
        Metacard metacard = new MockMetacard();
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = dateFormat.format(MockMetacard.DEFAULT_DATE);
        mapper.addMapEntry(Metacard.EFFECTIVE, date, DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardShortAttribute() {
        Short testShort = Short.valueOf("2");
        Metacard metacard = new MockMetacard(AttributeFormat.SHORT.toString(), testShort);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.SHORT.toString(), String.valueOf(testShort),
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardIntegerAttribute() {
        Integer testInteger = Integer.valueOf("2");
        Metacard metacard = new MockMetacard(AttributeFormat.INTEGER.toString(), testInteger);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.INTEGER.toString(), String.valueOf(testInteger),
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardLongAttribute() {
        Long testLong = Long.valueOf("2000000");
        Metacard metacard = new MockMetacard(AttributeFormat.LONG.toString(), testLong);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.LONG.toString(), String.valueOf(testLong),
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardFloatAttribute() {
        Float testFloat = Float.valueOf("2.0");
        Metacard metacard = new MockMetacard(AttributeFormat.FLOAT.toString(), testFloat);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.FLOAT.toString(), String.valueOf(testFloat),
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardDoubleAttribute() {
        Double testDouble = Double.valueOf("2");
        Metacard metacard = new MockMetacard(AttributeFormat.DOUBLE.toString(), testDouble);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.DOUBLE.toString(), String.valueOf(testDouble),
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
    }

    @Test
    public void testGetStyleForMetacardNoAttributeMatch() {
        MetacardImpl metacard = new MetacardImpl();
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        assertThat(mapper.getStyleForMetacard(metacard), nullValue());
    }

    @Test
    public void testGetStyleForMetacardBinaryNoMatch() {
        Metacard metacard = new MockMetacard(AttributeFormat.BINARY.toString(),
                MockMetacard.DEFAULT_LOCATION);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.BINARY.toString(), MockMetacard.DEFAULT_LOCATION,
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), nullValue());
    }

    @Test
    public void testGetStyleForMetacardObjectNoMatch() {
        Metacard metacard = new MockMetacard(AttributeFormat.OBJECT.toString(),
                MockMetacard.DEFAULT_LOCATION);
        KmlStyleMapper mapper = new KmlStyleMapper(context);
        mapper.addMapEntry(AttributeFormat.OBJECT.toString(), MockMetacard.DEFAULT_LOCATION,
                DEFAULT_STYLE_URL);
        assertThat(mapper.getStyleForMetacard(metacard), nullValue());
    }

}
