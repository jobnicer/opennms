package org.opennms.netmgt.jasper.measurement;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Verifies that the {@link MeasurementApiConnector} connects accordingly to the OpenNMS Measurement API and may
 * deal with OpenNMS specifics.
 */
public class MeasurementApiConnectorIT {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementApiConnectorTest.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            new WireMockConfiguration()
                    .port(9999)
                    .httpsPort(9443)
                    .keystorePath(System.getProperty("javax.net.ssl.keyStore"))
                    .keystorePassword(System.getProperty("javax.net.ssl.keyStorePassword")));

    @BeforeClass
    public static void beforeClass() {
        String[] keys = new String[]{
                "ssl.debug",
                "javax.net.debug",
                "javax.net.ssl.keyStore",
                "javax.net.ssl.keyStorePassword",
                "javax.net.ssl.trustStore",
                "javax.net.ssl.trustStorePassword"};
        for (String eachKey : keys) {
            String value = eachKey.toLowerCase().contains("password") ? "*****" : System.getProperty(eachKey);
            LOG.debug("{} = {}", eachKey, value);
        }
    }

    @Before
    public void before() {
        // OK Requests
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/opennms/rest/measurements"))
                .withHeader("Accept", WireMock.equalTo("application/xml"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<response>Some content</response>")));

        // Forward Requests
        WireMock.stubFor(WireMock.post(WireMock.urlMatching("/opennms/rest/forward/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(302)));

        // 500 Requests
        WireMock.stubFor(WireMock.post(WireMock.urlMatching("/opennms/rest/bad/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withBody("This did not work as you might have expected, ugh?")));

        // Timeout
        WireMock.stubFor(WireMock.post(WireMock.urlMatching("/opennms/rest/measurements/timeout"))
                .willReturn(WireMock.aResponse().withFixedDelay(5000)));

        // Everything else is automatically bound to a 404
    }

    @Test
    public void test200() throws IOException {
        Result result = new MeasurementApiConnector().execute(false, "http://localhost:9999/opennms/rest/measurements", null, null, "<dummy request>");
        Assert.assertTrue(result.wasSuccessful());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteStreams.copy(result.getInputStream(), outputStream);
        Assert.assertEquals("<response>Some content</response>", outputStream.toString());

        verifyWiremock();
    }

    @Test
    public void test404() throws IOException {
        Result result = new MeasurementApiConnector().execute(false, "http://localhost:9999/opennms/rest/doesNotExist", null, null, "<dummy request>");
        Assert.assertFalse(result.wasSuccessful());
        Assert.assertEquals(404, result.getResponseCode());
        Assert.assertEquals("Not Found", result.getResponseMessage());

        verifyWiremock("/opennms/rest/doesNotExist");
    }

    /**
     * OpenNMS sometimes forwards to the index.jsp if not logged in, which would result in a success of the request
     * in general. We do not want that. This test checks that a 302 is not automatically forwarded.
     */
    @Test
    public void test302() throws IOException {
        Result result = new MeasurementApiConnector().execute(false, "http://localhost:9999/opennms/rest/forward/me", null, null, "<dummy request>");
        Assert.assertFalse(result.wasSuccessful());
        Assert.assertTrue(result.wasRedirection());
        Assert.assertEquals(302, result.getResponseCode());
        Assert.assertNull(result.getInputStream());
        Assert.assertNull(result.getErrorStream());

        verifyWiremock("/opennms/rest/forward/me");
    }

    @Test
    public void test500() throws IOException {
        Result result = new MeasurementApiConnector().execute(false, "http://localhost:9999/opennms/rest/bad/request", null, null, "<dummy request>");
        Assert.assertFalse(result.wasSuccessful());
        Assert.assertFalse(result.wasRedirection());
        Assert.assertEquals(500, result.getResponseCode());
        Assert.assertNull(result.getInputStream());
        Assert.assertNotNull(result.getErrorStream());

        verifyWiremock("/opennms/rest/bad/request");
    }

    @Test
    public void testAuthentication() throws IOException {
        Result result = new MeasurementApiConnector().execute(false, "http://localhost:9999/opennms/rest/measurements", "admin", "admin", "<dummy request>");
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertFalse(result.wasRedirection());
        Assert.assertEquals(200, result.getResponseCode());
        Assert.assertNotNull(result.getInputStream());
        Assert.assertNull(result.getErrorStream());

        RequestPatternBuilder requestPatternBuilder = createDefaultRequestPatternBuilder("/opennms/rest/measurements");
        requestPatternBuilder.withHeader("Authorization", WireMock.matching("Basic .*"));

        verifyWiremock(requestPatternBuilder);
    }

    // We connect to 127.0.0.2 and expect a timeout. We also verify that the timeout is
    // as defined (including a tolerance)
    @Test(expected=SocketTimeoutException.class)
    public void testTimeout() throws IOException {
        long start = System.currentTimeMillis();
        try {
            new MeasurementApiConnector().execute(false, "http://127.0.0.2/opennms/rest/measurements/timeout", null, null, "<dummy request>");
        } catch (SocketTimeoutException ex) {
            long diff = System.currentTimeMillis() - start;
            long offset = 500; // ms
            Assert.assertEquals(Double.valueOf(MeasurementApiConnector.CONNECT_TIMEOUT), (double) diff, (double) offset);
            throw ex;
        }
    }

    // Verifies that a https call can be made
    @Test
    public void testHttpsOk() throws IOException {
        Result result = new MeasurementApiConnector().execute(true, "https://localhost:9443/opennms/rest/measurements", null, null, "<dummy request>");
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertTrue(result.wasSecureConnection());
        Assert.assertNotNull(result.getInputStream());
        Assert.assertNull(result.getErrorStream());

        verifyWiremock();
    }

    // Verifies that a https call cannot be made to an unknown server (certificate)
    @Test(expected=SSLHandshakeException.class)
    public void testHttpsUnknown() throws IOException {
        new MeasurementApiConnector().execute(true, "https://127.0.0.1:9443/opennms/rest/measurements", null, null, "<dummy request>");
    }

    // Verifies that even if useSSL = false, when connecting to a valid https url the connection is secure
    @Test
    public void testHttpsUrlButUseSslNotSet() throws IOException {
        // TODO MVR check for warn log message
        Result result = new MeasurementApiConnector().execute(false, "https://localhost:9443/opennms/rest/measurements", null, null, "<dummy request>");
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertTrue(result.wasSecureConnection());
        Assert.assertNotNull(result.getInputStream());
        Assert.assertNull(result.getErrorStream());
    }

    // Verifies that if SSL is enabled and we connect to a HTTP connection, a SSLException is thrown by our client.
    @Test(expected=SSLException.class)
    public void testHttpUrlButUseSslSet() throws IOException {
        new MeasurementApiConnector().execute(true, "http://localhost:9999/opennms/rest/measurements", null, null, "<dummy request>");
    }

    // We do not need this test, but I leave it for now
    @Test(expected=SSLException.class)
    public void testConectToHttpPortUsingHttpsProtocol() throws IOException {
        new MeasurementApiConnector().execute(true, "https://localhost:9999/opennms/rest/measurements", null, null, "<dummy request>");
    }

    // Verify that an empty URL throws an expected IllegalArgumentException
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyUrl() throws IOException {
        new MeasurementApiConnector().execute(false, "", null, null, null);
    }

    // Verify that a null URL throws an expected IllegalArgumentException
    @Test(expected=IllegalArgumentException.class)
    public void testNullUrl() throws IOException {
        new MeasurementApiConnector().execute(false, null, null, null, null);
    }

    // Verify that a null query throws an expected IllegalArgumentException
    @Test(expected=IllegalArgumentException.class)
    public void testNullQuery() throws IOException {
        new MeasurementApiConnector().execute(false, "http://localhost", null, null, null);
    }

    // Verify that an empty query throws an expected IllegalArgumentException
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyQuery() throws IOException {
        new MeasurementApiConnector().execute(false, "http://localhost", null, null, "");
    }

    private void verifyWiremock() {
        verifyWiremock("/opennms/rest/measurements");
    }

    private void verifyWiremock(String url) {
        verifyWiremock(createDefaultRequestPatternBuilder(url));
    }

    private void verifyWiremock(RequestPatternBuilder builder) {
        WireMock.verify(builder);
    }

    private RequestPatternBuilder createDefaultRequestPatternBuilder(String url) {
        return WireMock.postRequestedFor(WireMock.urlMatching(url))
                .withRequestBody(WireMock.matching("<dummy request>"))
                .withHeader("Content-Type", WireMock.equalTo("application/xml"))
                .withHeader("Accept-Charset", WireMock.equalTo("UTF-8"));
    }
}
