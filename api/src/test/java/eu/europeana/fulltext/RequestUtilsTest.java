package eu.europeana.fulltext;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.mock.web.MockHttpServletRequest;

public class RequestUtilsTest {

    // accept header Test
    @Test
    public void testGetRequestVersion_AcceptHeader3Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/json;profile=\"https://iiif.io/api/presentation/3/context.json\"");
        String version = RequestUtils.getRequestVersion(request, null);

        checkVersion(RequestUtils.REQUEST_VERSION_3, version);
    }

    @Test
    public void testGetRequestVersion_AcceptHeader2Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/json;profile=\"https://iiif.io/api/presentation/2/context.json\"");
        String version = RequestUtils.getRequestVersion(request, null);

        checkVersion(RequestUtils.REQUEST_VERSION_2, version);
    }

    @Test
    public void testGetRequestVersion_AcceptHeaderInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/json;profile=\"invalid\"");

        // invalid accept header should return null
        Assertions.assertNull(RequestUtils.getRequestVersion(request, null));
    }

    @Test
    public void testGetRequestVersion_AcceptHeaderFormatBothInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/json;profile=\"invalid\"");

        // invalid accept header should return null. Should not process the format value
        Assertions.assertNull(RequestUtils.getRequestVersion(request, "6"));
    }

    @Test
    public void testGetRequestVersion_AcceptHeaderInvalidFormatValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/json;profile=\"invalid\"");

        // invalid accept header should return null. Should not process the format value
        Assertions.assertNull(RequestUtils.getRequestVersion(request, "2"));
    }

    @Test
    public void testGetRequestVersion_AcceptHeaderValidFormatInValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/json;profile=\"https://iiif.io/api/presentation/2/context.json\"");

        // valid accept header should return the value. Should not process the format value
        String version = RequestUtils.getRequestVersion(request, "6");
        checkVersion(RequestUtils.REQUEST_VERSION_2, version);
    }

    @Test
    public void testGetRequestVersion_AcceptHeaderWithoutProfileFormatValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/ld+json");

        // valid accept header but without profile value. Should process the format value if present, or else return default '2'
        String version = RequestUtils.getRequestVersion(request, "2");
        checkVersion(RequestUtils.REQUEST_VERSION_2, version);
    }

    @Test
    public void testGetRequestVersion_AcceptHeaderWithoutProfileFormatNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestUtils.ACCEPT, "application/ld+json");

        // valid accept header but without profile value. Should process the format value if present, or else return default '2'
        String version = RequestUtils.getRequestVersion(request, null);
        checkVersion(RequestUtils.REQUEST_VERSION_2, version);
    }

    // format value test
    @Test
    public void testGetRequestVersion_Format3Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String version = RequestUtils.getRequestVersion(request, "3");

        checkVersion(RequestUtils.REQUEST_VERSION_3, version);
    }

    @Test
    public void testGetRequestVersion_Format2Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String version = RequestUtils.getRequestVersion(request, "2");

        checkVersion(RequestUtils.REQUEST_VERSION_2, version);
    }

    @Test
    public void testGetRequestVersion_FormatInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertNull(RequestUtils.getRequestVersion(request, "6"));
    }

    @Test
    public void testGetRequestVersion_DefaultFormat() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String version = RequestUtils.getRequestVersion(request, null);

        checkVersion(RequestUtils.REQUEST_VERSION_2, version);
    }

    private void checkVersion(String expectedVersion, String result) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedVersion, result);
    }
}
