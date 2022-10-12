package eu.europeana.iiif;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

public class AcceptUtilsTest {

    // Test accept headers
    @Test
    public void testAcceptHeaderV3() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"http://iiif.io/api/presentation/3/context.json\"");
        String version = AcceptUtils.getRequestVersion(request, null);
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_3, version);
    }

    @Test
    public void testAcceptHeaderV2() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"http://iiif.io/api/presentation/2/context.json\"");
        String version = AcceptUtils.getRequestVersion(request, null);
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_2, version);
    }

    /**
     * Officially only http://iiif.io/api/presentation/3/context.json should be supported, but to avoid confusion we also
     * support https://iiif.io/api/presentation/3/context.json
     */
    @Test
    public void testAcceptHeaderHttps() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"https://iiif.io/api/presentation/3/context.json\"");
        String version = AcceptUtils.getRequestVersion(request, null);
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_3, version);
    }

    @Test
    public void testAcceptHeaderHttpsWithInvalidVersionInUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"https://iiif.io/api/presentation/5/context.json\"");
        // invalid accept header should return null, as the version passed in url is 5
        Assertions.assertNull(AcceptUtils.getRequestVersion(request, null));
    }

    @Test
    public void testAcceptHeaderInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"invalid\"");
        // invalid accept header should return null
        Assertions.assertNull(AcceptUtils.getRequestVersion(request, null));
    }

    // Test combinations of accept header and format parameter
    @Test
    public void testAcceptHeaderInvalidFormatValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"invalid\"");
        // invalid accept header should return null. Should not process the format value
        Assertions.assertNull(AcceptUtils.getRequestVersion(request, "2"));
    }

    @Test
    public void testAcceptHeaderInvalidFormatInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"invalid\"");
        // invalid accept header should return null. Should not process the format value
        Assertions.assertNull(AcceptUtils.getRequestVersion(request, "6"));
    }

    @Test
    public void testAcceptHeaderValidFormatInValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/json;profile=\"https://iiif.io/api/presentation/2/context.json\"");
        // valid accept header should return the value. Should not process the format value
        String version = AcceptUtils.getRequestVersion(request, "6");
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_2, version);
    }

    @Test
    public void testAcceptHeaderWithoutProfileFormatValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/ld+json");
        // valid accept header but without profile value. Should process the format value if present, or else return default '2'
        String version = AcceptUtils.getRequestVersion(request, "2");
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_2, version);
    }

    @Test
    public void testAcceptHeaderNoProfileNoFormat() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AcceptUtils.ACCEPT, "application/ld+json");
        // valid accept header but without profile value. Should process the format value if present, or else return default '2'
        String version = AcceptUtils.getRequestVersion(request, null);
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_2, version);
    }

    // Test only format values
    @Test
    public void testFormat3Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String version = AcceptUtils.getRequestVersion(request, "3");
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_3, version);
    }

    @Test
    public void testFormat2Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String version = AcceptUtils.getRequestVersion(request, "2");
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_2, version);
    }

    @Test
    public void testFormatInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertNull(AcceptUtils.getRequestVersion(request, "6"));
    }

    @Test
    public void testNoAcceptNoFormat() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String version = AcceptUtils.getRequestVersion(request, null);
        Assertions.assertEquals(AcceptUtils.REQUEST_VERSION_2, version);
    }

    // content type tests
    @Test
    public void testAddContentTypeToResponseHeader_version3Json() {
        HttpHeaders headers = new HttpHeaders();
        AcceptUtils.addContentTypeToResponseHeader(headers, AcceptUtils.REQUEST_VERSION_3, true);
        Assertions.assertEquals(IIIFDefinitions.MEDIA_TYPE_IIIF_JSON_V3, headers.getContentType().toString());
    }

    @Test
    public void testAddContentTypeToResponseHeader_version3JsonLd() {
        HttpHeaders headers = new HttpHeaders();
        AcceptUtils.addContentTypeToResponseHeader(headers, AcceptUtils.REQUEST_VERSION_3, false);
        Assertions.assertEquals(IIIFDefinitions.MEDIA_TYPE_IIIF_JSONLD_V3, headers.getContentType().toString());
    }

    @Test
    public void testAddContentTypeToResponseHeader_version2Json() {
        HttpHeaders headers = new HttpHeaders();
        AcceptUtils.addContentTypeToResponseHeader(headers, AcceptUtils.REQUEST_VERSION_2, true);
        Assertions.assertEquals(IIIFDefinitions.MEDIA_TYPE_IIIF_JSON_V2, headers.getContentType().toString());
    }

    @Test
    public void testAddContentTypeToResponseHeader_version2JsonLd() {
        HttpHeaders headers = new HttpHeaders();
        AcceptUtils.addContentTypeToResponseHeader(headers, AcceptUtils.REQUEST_VERSION_2, false);
        Assertions.assertEquals(IIIFDefinitions.MEDIA_TYPE_IIIF_JSONLD_V2, headers.getContentType().toString());
    }
}
