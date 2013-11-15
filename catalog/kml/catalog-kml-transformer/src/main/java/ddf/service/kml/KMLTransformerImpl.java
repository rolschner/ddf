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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.service.kml.internal.TransformedContentImpl;
import ddf.service.kml.subscription.KmlSubscription;
import ddf.service.kml.subscription.KmlUpdateDeliveryMethod;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.RefreshMode;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;

/**
 * The base Transformer for handling KML requests to take a {@link Metacard} or
 * {@link SourceResponse} and produce a KML representation. This service attempts to first locate a
 * {@link KMLEntryTransformer} for a given {@link Metacard} based on the metadata-content-type. If
 * no {@link KMLEntryTransformer} can be found, the default transformation is performed.
 * 
 * @author Ashraf Barakat, Ian Barnett, Keith C Wire
 * 
 */
public class KMLTransformerImpl implements KMLTransformer, ConfigurationWatcher {

    private static final String UTF_8 = "UTF-8";

    private static final String DEFAULT_INTERVAL_STRING = "5.0";

    private static final String KML_RESPONSE_QUEUE_PREFIX = "Query Results (";

    private static final String NETWORK_LINK_UPDATE_NAME = "Update";

    private static final String XSL_REST_URL_PROPERTY = "resturl";

    private static final String SERVICES_REST = "/services/catalog/";
    
    private static final String QUALIFIER = "qualifier";

    private static final String AMPERSAND = "&";

    private static final String CONTENT_TYPE = "content-type";

    protected static final String KML_MIMETYPE = "application/vnd.google-earth.kml+xml";

    private static final String EQUALS_SIGN = "=";

    private static final String CLOSE_PARENTHESIS = ")";

    private static final String OPEN_PARENTHESIS = "(";

    private static final String KML_ENTRY_TRANSFORMER = ddf.service.kml.KMLEntryTransformer.class
            .getName();

    private static final String BBOX_QUERY_PARAM_KEY = "bbox";

    private static final String TEMPLATE_DIRECTORY = "/templates";

    private static final String TEMPLATE_SUFFIX = ".hbt";

    private static final String DESCRIPTION_TEMPLATE = "description";

    protected BundleContext context;

    private static List<StyleSelector> defaultStyle = new ArrayList<StyleSelector>();

    private Map<String, ServiceRegistration> subscriptionMap;

    private JAXBContext jaxbContext;

    private Marshaller marshaller;

    private Unmarshaller unmarshaller;
            
    private static final Logger LOGGER = LoggerFactory.getLogger(KMLTransformerImpl.class);

    private ClassPathTemplateLoader templateLoader;

    private Map<String, String> platformConfiguration;

    private KmlStyleMap styleMapper;

    public KMLTransformerImpl(BundleContext bundleContext, String defaultStylingName,
            KmlStyleMap mapper) {
        this.subscriptionMap = new HashMap<String, ServiceRegistration>();
        this.context = bundleContext;
        this.styleMapper = mapper;

        URL stylingUrl = context.getBundle().getResource(defaultStylingName);

        try {
            this.jaxbContext = JAXBContext.newInstance(Kml.class);
            this.marshaller = jaxbContext.createMarshaller();
            this.unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB Context and Marshaller.  Setting to null.");
            this.jaxbContext = null;
            this.marshaller = null;
            this.unmarshaller = null;
        }

        try {
            if (unmarshaller != null) {
                LOGGER.debug("Reading in KML Style");
                JAXBElement<Kml> jaxbKmlStyle = this.unmarshaller.unmarshal(new StreamSource(
                        stylingUrl.openStream()), Kml.class);
                Kml kml = jaxbKmlStyle.getValue();
                if (kml.getFeature() != null) {
                    defaultStyle = kml.getFeature().getStyleSelector();
                }
            }
        } catch (JAXBException e) {
            LOGGER.warn("Exception while unmarshalling default style resource.", e);
        } catch (IOException e) {
            LOGGER.warn("Exception while opening default style resource.", e);
        }

        try {
            this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            this.marshaller.setProperty(Marshaller.JAXB_ENCODING, UTF_8);
        } catch (PropertyException e) {
            LOGGER.error("Unable to set properties on JAXB Marshaller: ", e);
        }

        templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix(TEMPLATE_DIRECTORY);
        templateLoader.setSuffix(TEMPLATE_SUFFIX);
    }

    /**
     * This will return a KML Placemark (i.e. there are no kml tags)
     * {@code 
     * <KML>        ---> not included
     * <Placemark>   ---> What is returned from this method
     * ...          ---> What is returned from this method 
     * </Placemark>  ---> What is returned from this method
     * </KML>       ---> not included
     * }
     * 
     * @param user
     * @param entry
     *            - the {@link Metacard} to be transformed
     * @param arguments
     *            - additional arguments to assist in the transformation
     * @return Placemark - kml object containing transformed content
     * 
     * @throws CatalogTransformerException
     */
    @Override
    public Placemark transformEntry(Subject user, Metacard entry,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        String urlToMetacard = null;

        if (arguments == null) {
            arguments = new HashMap<String, Serializable>();
        }

        String incomingRestUriAbsolutePathString = (String) arguments.get("url");
        if (incomingRestUriAbsolutePathString != null) {
            try {
                URI incomingRestUri = new URI(incomingRestUriAbsolutePathString);
                URI officialRestUri = new URI(incomingRestUri.getScheme(), null,
                        incomingRestUri.getHost(), incomingRestUri.getPort(), SERVICES_REST + "/"
                                + entry.getId(), null, null);
                urlToMetacard = officialRestUri.toString();
            } catch (URISyntaxException e) {
                LOGGER.info("bad url passed in, using request url for kml href.", e);
                urlToMetacard = incomingRestUriAbsolutePathString;
            }
            LOGGER.debug("REST URL: " + urlToMetacard);
        }

        String type = entry.getContentTypeName();
        String qual = "type";
        try {

            LOGGER.debug("Entry with id  {} has come in to be transformed, search for "
                    + "KMLEntryTransformers that handle the given qualified content type: {}:{}"
                    , entry.getId(), qual, type);
            if (this.unmarshaller != null) {
                KMLEntryTransformer kmlET = lookupTransformersForQualifiedContentType(qual, type);
                if (kmlET != null) {

                    // add the rest url argument
                    arguments.put(XSL_REST_URL_PROPERTY, urlToMetacard);

                    String content = kmlET.getKMLContent(entry, arguments);
                    String style = kmlET.getKMLStyle();

                    JAXBElement<Placemark> kmlContentJaxb = this.unmarshaller.unmarshal(
                            new StreamSource(new ByteArrayInputStream(content.getBytes())),
                            Placemark.class);
                    JAXBElement<Style> kmlStyleJaxb = this.unmarshaller.unmarshal(new StreamSource(
                            new ByteArrayInputStream(style.getBytes())), Style.class);
                    Placemark placemark = kmlContentJaxb.getValue();
                    placemark.getStyleSelector().add(kmlStyleJaxb.getValue());

                    return placemark;
                }
            } else {
                LOGGER.warn("Unmarshaller is null. Cannot obtain kml content and kml style from "
                        + "KMLEntryTransformer. Attempting default transformation...");
            }

        } catch (JAXBException e) {
            LOGGER.warn("Could not transform for given content type: " + e.getMessage());
            LOGGER.info("No other transformer can properly perform the transformation, defaulting to common kml transformation.");
        }

        return performDefaultTransformation(entry, incomingRestUriAbsolutePathString);
    }

    protected void addNetworkLinkUpdate(Kml kmlResult, URL requestUrl, String subscriptionId,
            double refreshInterval) throws URISyntaxException, MalformedURLException {
        // TODO: make it so it generates the URL dynamically. Needs to include
        // query params from the original request.
        // TODO: use some sort of URI or URL builder to generate the URL to
        // handle encoding of special chars.
        String queryParams = requestUrl.getQuery();
        String[] queryParamsSplit = queryParams.split(BBOX_QUERY_PARAM_KEY + "=");

        String requestPath = requestUrl.getPath();
        String[] requestPathSplit = requestPath.split("query");
        LOGGER.debug("incoming request url for network link update: " + requestUrl);
        LOGGER.debug("bbox query param: " + queryParamsSplit[queryParamsSplit.length - 1]);
        String netLinkUpdatePath = null;

        // This is to handle the case where the path may already have a slash
        // Although some clients can handle multiple slashes (/) in the URL
        // some do not so we will make sure there are not two slashes
        if (requestPathSplit[0].endsWith("/")) {
            netLinkUpdatePath = requestPathSplit[0] + "kml" + "/" + "update";
        } else {
            netLinkUpdatePath = requestPathSplit[0] + "/" + "kml" + "/" + "update";

        }
        String netLinkUpdateQuery = "subscription=" + subscriptionId + "&" + BBOX_QUERY_PARAM_KEY
                + "=" + queryParamsSplit[queryParamsSplit.length - 1];

        URI netLinkUpdateUri = new URI(requestUrl.getProtocol(), null, requestUrl.getHost(),
                requestUrl.getPort(), netLinkUpdatePath, netLinkUpdateQuery, null);
        LOGGER.debug("network link update url: " + netLinkUpdateUri.toURL());

        if (kmlResult.getFeature() instanceof Document) {
            Document doc = (Document) kmlResult.getFeature();
            NetworkLink networkLink = KmlFactory.createNetworkLink();
            doc.addToFeature(networkLink);

            networkLink.setName(NETWORK_LINK_UPDATE_NAME);
            // networkLink.setVisibility(true);

            Link link = KmlFactory.createLink();
            networkLink.setLink(link);

            link.setRefreshMode(RefreshMode.ON_INTERVAL);
            link.setRefreshInterval(refreshInterval);
            link.setHref(netLinkUpdateUri.toURL().toString());
            link.setViewBoundScale(1);
        } else {
            LOGGER.warn("Unable to add network link update becuase the KML was not a Document");
        }
    }

    /**
     * The default Transformation from a {@link Metacard} to a KML {@link Placemark}. Protected to
     * easily allow other default transformations.
     * 
     * @param entry
     *            - the {@link Metacard} to transform.
     * @param urlToMetacard
     * @return
     * @throws TransformerException
     */
    protected Placemark performDefaultTransformation(Metacard entry, String url)
        throws CatalogTransformerException {
        Placemark kmlPlacemark = KmlFactory.createPlacemark();
        kmlPlacemark.setId("Placemark-" + entry.getId());
        kmlPlacemark.setName(entry.getTitle());

        // TODO - Need to understand how this effects the NetworkLink Behavior.
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String effectiveTime = null;
        if (entry.getEffectiveDate() == null) {
            effectiveTime = dateFormat.format(new Date());
        } else {
            effectiveTime = dateFormat.format(entry.getEffectiveDate());
        }
        kmlPlacemark.setTimePrimitive(KmlFactory.createTimeStamp().withWhen(effectiveTime));

        kmlPlacemark.setGeometry(getKmlGeoFromWkt(entry.getLocation()));

        String description = entry.getTitle();
        Handlebars handlebars = new Handlebars(templateLoader);
        handlebars.registerHelpers(new DescriptionTemplateHelper(url, platformConfiguration));
        try {
            Template template = handlebars.compile(DESCRIPTION_TEMPLATE);
            description = template.apply(entry);
            LOGGER.debug(description);
            
        } catch (IOException e) {
            LOGGER.error("Failed to apply description Template", e);
        }
        kmlPlacemark.setDescription(description);

        String styleUrl = styleMapper.getStyleForMetacard(entry);
        if (StringUtils.isNotBlank(styleUrl)) {
            kmlPlacemark.setStyleUrl(styleUrl);
        }

        return kmlPlacemark;
    }

    private Geometry getKmlGeoFromWkt(final String wkt) throws CatalogTransformerException {
        if (StringUtils.isBlank(wkt)) {
            throw new CatalogTransformerException(
                    "WKT was null or empty. Unable to preform KML Transform on Metacard.");
        }

        com.vividsolutions.jts.geom.Geometry geo = readGeoFromWkt(wkt);
        Geometry kmlGeo = createKmlGeo(geo);
        if (!Point.class.getSimpleName().equals(geo.getGeometryType())) {
            kmlGeo = addPointToKmlGeo(kmlGeo, geo.getCoordinate());
        }
        return kmlGeo;
    }
    
    private Geometry createKmlGeo(com.vividsolutions.jts.geom.Geometry geo)
        throws CatalogTransformerException {
        Geometry kmlGeo = null;
        if (Point.class.getSimpleName().equals(geo.getGeometryType())) {
            Point jtsPoint = (Point) geo;
            kmlGeo = KmlFactory.createPoint().addToCoordinates(jtsPoint.getX(), jtsPoint.getY());

        } else if (LineString.class.getSimpleName().equals(geo.getGeometryType())) {
            LineString jtsLS = (LineString) geo;
            de.micromata.opengis.kml.v_2_2_0.LineString kmlLS = KmlFactory.createLineString();
            List<Coordinate> kmlCoords = kmlLS.createAndSetCoordinates();
            for (com.vividsolutions.jts.geom.Coordinate coord : jtsLS.getCoordinates()) {
                kmlCoords.add(new Coordinate(coord.x, coord.y));
            }
            kmlGeo = kmlLS;
        } else if (Polygon.class.getSimpleName().equals(geo.getGeometryType())) {
            Polygon jtsPoly = (Polygon) geo;
            de.micromata.opengis.kml.v_2_2_0.Polygon kmlPoly = KmlFactory.createPolygon();
            List<Coordinate> kmlCoords = kmlPoly.createAndSetOuterBoundaryIs()
                    .createAndSetLinearRing().createAndSetCoordinates();
            for (com.vividsolutions.jts.geom.Coordinate coord : jtsPoly.getCoordinates()) {
                kmlCoords.add(new Coordinate(coord.x, coord.y));
            }
            kmlGeo = kmlPoly;
        } else if (geo instanceof GeometryCollection) {
            List<Geometry> geos = new ArrayList<Geometry>();
            for (int xx = 0; xx < geo.getNumGeometries(); xx++) {
                geos.add(createKmlGeo(geo.getGeometryN(xx)));
            }
            kmlGeo = KmlFactory.createMultiGeometry().withGeometry(geos);
        } else {
            throw new CatalogTransformerException("Unknown / Unsupported Geometry Type '"
                    + geo.getGeometryType() + "'. Unale to preform KML Transform.");
        }
        return kmlGeo;
    }

    private com.vividsolutions.jts.geom.Geometry readGeoFromWkt(final String wkt)
        throws CatalogTransformerException {
        WKTReader reader = new WKTReader();
        try {
            return reader.read(wkt);
        } catch (ParseException e) {
            throw new CatalogTransformerException("Unable to parse WKT to Geometry.", e);

        }
    }

    private Geometry addPointToKmlGeo(Geometry kmlGeo, com.vividsolutions.jts.geom.Coordinate vertex) {
        de.micromata.opengis.kml.v_2_2_0.Point kmlPoint = KmlFactory.createPoint()
                .addToCoordinates(vertex.x, vertex.y);
        return KmlFactory.createMultiGeometry().addToGeometry(kmlPoint).addToGeometry(kmlGeo);
    }

    private KMLEntryTransformer lookupTransformersForQualifiedContentType(String qualifier,
            String contentType) throws CatalogTransformerException {
        try {

            ServiceReference[] refs = context.getServiceReferences(KML_ENTRY_TRANSFORMER,
                    OPEN_PARENTHESIS + AMPERSAND + OPEN_PARENTHESIS + QUALIFIER + EQUALS_SIGN
                            + qualifier + CLOSE_PARENTHESIS + OPEN_PARENTHESIS + CONTENT_TYPE
                            + EQUALS_SIGN + contentType + CLOSE_PARENTHESIS + CLOSE_PARENTHESIS);

            if (refs == null || refs.length == 0) {
                return null;
            } else {
                return (KMLEntryTransformer) context.getService(refs[0]);
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid transformer shortName");
        }
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> configuration) {
        platformConfiguration = configuration;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
        throws CatalogTransformerException {
        try {
            Placemark placemark = transformEntry(null, metacard, arguments);
            if (placemark.getStyleSelector().isEmpty()
                    && StringUtils.isBlank(placemark.getStyleUrl())) {
                placemark.getStyleSelector().addAll(defaultStyle);
            }
            Kml kml = KmlFactory.createKml().withFeature(placemark);

            String transformedKmlString = marshalKml(kml);

            // logger.debug("transformed kml metacard: " + transformedKmlString);
            InputStream kmlInputStream = new ByteArrayInputStream(transformedKmlString.getBytes());

            return new TransformedContentImpl(kmlInputStream, KMLTransformerImpl.KML_MIMETYPE);
        } catch (Exception e) {
            LOGGER.error("Error transforming metacard to KML." + e.getMessage());
            throw new CatalogTransformerException("Error transforming metacard to KML.", e);
        }
    }

    @Override
    public BinaryContent transform(SourceResponse upstreamResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        LOGGER.trace("ENTERING: ResponseQueue transform");
        if (arguments == null) {
            LOGGER.debug("Null arguments, unable to complete transform");
            throw new CatalogTransformerException("Unable to complete transform without arguments");
        }
        // Get the Subscription ID
        Object id = arguments.get(Constants.SUBSCRIPTION_KEY);
        String subscriptionId = null;
        if (id != null) {
            subscriptionId = (String) id;
        }
        String docId = subscriptionId;
        if (docId == null) {
            LOGGER.debug("Document id was null, generating new Document id.");
            docId = UUID.randomUUID().toString();
        }
        LOGGER.debug("subscription id: " + subscriptionId);

        String restUriAbsolutePath = (String) arguments.get("url");
        LOGGER.debug("rest string url arg: " + restUriAbsolutePath);

        // Transform Metacards to KML
        Document kmlDoc = KmlFactory.createDocument();
        boolean needDefaultStyle = false;
        for (Result result : upstreamResponse.getResults()) {
            Placemark placemark = transformEntry(null, result.getMetacard(), arguments);
            if (placemark.getStyleSelector().isEmpty()
                    && StringUtils.isEmpty(placemark.getStyleUrl())) {
                placemark.setStyleUrl("#default");
                needDefaultStyle = true;
            }
            kmlDoc.getFeature().add(placemark);
        }
        
        if (needDefaultStyle) {
            kmlDoc.getStyleSelector().addAll(defaultStyle);
        }

        Kml kmlResult = encloseKml(kmlDoc, "Subscription-" + docId,
                KML_RESPONSE_QUEUE_PREFIX + kmlDoc.getFeature().size() + CLOSE_PARENTHESIS);

        if (subscriptionId != null && !subscriptionId.isEmpty()) {
            try {
                // add network link update to results
                URL url = new URL(restUriAbsolutePath);
                // arguments.get(key)
                double intervalDouble = parseInterval(arguments);
                addNetworkLinkUpdate(kmlResult, url, subscriptionId, intervalDouble);

                ServiceRegistration svcReg = this.subscriptionMap.get(subscriptionId);
                if (svcReg != null) {
                    LOGGER.debug("Found existing subscription with ID: " + subscriptionId
                            + " Delete existing subscription and create new one.");
                    try {
                        svcReg.unregister();
                    } catch (IllegalStateException e) {
                        LOGGER.info("Attempted to unregister subscription, but subscription was already unregistered.");
                    }
                }

                // create and register new subscription
                LOGGER.debug("creating new kml subscription with id: " + subscriptionId);
                QueryRequest queryRequest = upstreamResponse.getRequest();
                Query query = null;
                if (queryRequest != null) {
                    query = queryRequest.getQuery();
                } else {
                    LOGGER.warn("QueryRequest was null, unable to create query for the subscription");
                }
                LOGGER.debug("query used as the subscription: " + query);
                KmlSubscription sub = createKmlSubscription(subscriptionId, query);
                Hashtable<String, String> properties = new Hashtable<String, String>();
                properties.put("subscription-id", subscriptionId);
                String className = KmlSubscription.class.getInterfaces()[0].getName();
                ServiceRegistration newSvcRegistration = this.context.registerService(className,
                        sub, properties);
                this.subscriptionMap.put(subscriptionId, newSvcRegistration);
            } catch (Exception e) {
                LOGGER.warn(
                        "Unable to obtain subscription URL, returning KML results without any updates: ",
                        e);

            }
        }

        String transformedKml = marshalKml(kmlResult);

        // logger.debug("transformed kml: " + transformedKml);

        InputStream kmlInputStream = new ByteArrayInputStream(transformedKml.getBytes());
        LOGGER.trace("EXITING: ResponseQueue transform");
        return new TransformedContentImpl(kmlInputStream, KMLTransformerImpl.KML_MIMETYPE);
    }

    /**
     * parses the interval from the arguments
     * 
     * @param arguments
     *            - the arguments that may contain the interval string
     * @return a double value for the interval
     */
    private double parseInterval(Map<String, Serializable> arguments) {

        String interval = DEFAULT_INTERVAL_STRING;
        Object intervalArg = arguments.get("interval");
        if (intervalArg != null && intervalArg instanceof String) {
            interval = (String) intervalArg;
        }

        return Double.parseDouble(interval);

    }

    private KmlSubscription createKmlSubscription(String subscriptionId, Query query) {
        LOGGER.trace("ENTERING: createKmlSubscription");
        LOGGER.trace("EXITING: createKmlSubscription");
        return new KmlSubscription(subscriptionId, new KmlUpdateDeliveryMethod(), query);
    }

    /**
     * Encapsulate the kml content (placemarks, etc.) with a style in a KML Document element If
     * either content or style are null, they will be in the resulting Document
     * 
     * @param kml
     * @param style
     * @param documentId
     *            which should be the metacard id
     * @return KML DocumentType element with style and content
     */
    public static Document encloseDoc(Placemark placemark, Style style, String documentId,
            String docName) throws IllegalArgumentException {
        Document document = KmlFactory.createDocument();
        document.setId(documentId);
        document.setOpen(true);
        document.setName(docName);

        if (style != null) {
            document.getStyleSelector().add(style);
        }
        if (placemark != null) {
            document.getFeature().add(placemark);
        }

        return document;
    }

    /**
     * Wrap KML document with the opening and closing kml tags
     * 
     * @param document
     * @param folderId
     *            which should be the subscription id if it exists
     * @return completed KML
     */
    public static Kml encloseKml(Document doc, String docId, String docName) {
        Kml kml = KmlFactory.createKml();
        if (doc != null) {
            kml.setFeature(doc);
            doc.setId(docId); // Id should be subscription id
            doc.setName(docName);
            doc.setOpen(true);
        }
        return kml;
    }

    private String marshalKml(Kml kmlResult) {

        String kmlResultString = null;
        StringWriter writer = new StringWriter();

        try {
            marshaller.marshal(kmlResult, writer);
        } catch (JAXBException e) {
            LOGGER.warn("Failed to marshal KML: ", e);
        }

        kmlResultString = writer.toString();

        return kmlResultString;
    }
}
