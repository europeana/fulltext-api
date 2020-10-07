package eu.europeana.fulltext.api.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by luthien on 25/06/2020.
 */
public class LoggableDispatcherServlet extends DispatcherServlet {

    private static final Logger LOG = LogManager.getLogger(LoggableDispatcherServlet.class);
    private static final int BUFFERLENGTH = 5120;

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }
        HandlerExecutionChain handler = null;
        try {
            handler = getHandler(request);
        } catch (Exception e) {
            LOG.error("Exception occurred while retrieving HandlerExecutionChain from HttpServletRequest", e);
        }

        try {
            super.doDispatch(request, response);
        } catch (Exception e) {
            LOG.error("Exception occurred while invoking DispatcherServlet", e);
        } finally {
            logRequest(request, response, handler);

            updateResponse(response);
        }
    }

    private void logRequest(HttpServletRequest requestToCache, HttpServletResponse responseToCache, HandlerExecutionChain handler) {
        LogMessage logMessage = new LogMessage();
        logMessage.setHttpStatus(responseToCache.getStatus());
        logMessage.setHttpMethod(requestToCache.getMethod());
        logMessage.setPath(requestToCache.getRequestURI());
        logMessage.setClientIp(requestToCache.getRemoteAddr());
        logMessage.setJavaMethod(handler.toString());
        logMessage.setResponse(getResponsePayload(responseToCache));
        LOG.info(logMessage);
    }

    private String getResponsePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {

            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, BUFFERLENGTH);
                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                } catch (UnsupportedEncodingException ex) {
                    LOG.error("Unsupported encoding encountered while retrieving response payload from HttpServletResponse", ex);
                }
            }
        }
        return "[unknown]";
    }

    private void updateResponse(HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (responseWrapper != null) {
            responseWrapper.copyBodyToResponse();
        }
    }

    private static class LogMessage{

        private int httpStatus;
        private String responsePayload;
        private String javaMethod;
        private String clientIp;
        private String path;
        private String method;

        public void setHttpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
        }


        public void setResponse(String responsePayload) {
            this.responsePayload = responsePayload;
        }

        public void setJavaMethod(String javaMethod) {
            this.javaMethod = javaMethod;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setHttpMethod(String method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return "{" +
                   "httpStatus=" + httpStatus +
                   ", responsePayload='" + responsePayload + '\'' +
                   ", javaMethod='" + javaMethod + '\'' +
                   ", clientIp='" + clientIp + '\'' +
                   ", path='" + path + '\'' +
                   ", method='" + method + '\'' +
                   '}';
        }
    }

}
