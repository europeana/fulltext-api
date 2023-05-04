package eu.europeana.fulltext.resize;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.edm.media.ImageDimension;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 29 Nov 2018
 */
public class IIIFImageInfoSupport implements ResponseHandler<ImageDimension> {
    private static final Logger LOG = LogManager.getLogger(IIIFImageInfoSupport.class);

    private final ObjectMapper _mapper = new ObjectMapper();
    private final CloseableHttpClient _client;

    public IIIFImageInfoSupport() {
        _client = HttpClientBuilder.create().build();
    }

    public void finalize() {
        try {
            _client.close();
        } catch (IOException e) {
            LOG.error("Error when finalizing http client", e);
        }
    }

    public ImageDimension getImageSize(String imageURL) {
        String url = getInfoURL(imageURL);
        return (url == null ? null : getImageDimension(url));
    }

    private ImageDimension getImageDimension(String url) {
        HttpGet m = new HttpGet(url);
        try {
            return _client.execute(m, this);
        } catch (IOException e) {
            LOG.error("Error getting image dimension for: " + url, e);
            return null;
        } finally {
            m.releaseConnection();
        }

    }

    private String getInfoURL(String imageURL) {
        int l = imageURL.length();
        for (int i = 0; i <= 3; i++) {
            l = imageURL.lastIndexOf('/', l - 1);
            if (l < 0) {
                return null;
            }
        }
        return (imageURL.substring(0, l) + "/info.json");
    }

    public ImageDimension handleResponse(HttpResponse rsp)
            throws IOException {
        StatusLine status = rsp.getStatusLine();
        int code = status.getStatusCode();
        if (code != 200) {
            throw new HttpResponseException(code, status.toString());
        }

        Map map = _mapper.readValue(rsp.getEntity().getContent(), HashMap.class);
        if (map == null || map.isEmpty()) {
            return null;
        }

        return new ImageDimension((Integer) map.get("width")
                , (Integer) map.get("height"));
    }

    /*
    public boolean retryRequest(IOException e, int count, HttpContext context)
    {
        if (count >= 5) {
            // Do not retry if over max retry count
            return false;
        }
        if (exception instanceof InterruptedIOException) {
            // Timeout
            return false;
        }
        if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
        }
        if (exception instanceof SSLException) {
            // SSL handshake exception
            return false;
        }
        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();
        boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
        if (idempotent) {
            // Retry if the request is considered idempotent
            return true;
        }
        return false;
    }*/
}
