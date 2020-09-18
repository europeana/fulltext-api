package eu.europeana.fulltext.api.web;


import org.apache.commons.lang3.StringUtils;
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
import java.util.Collection;
import java.util.Enumeration;

/**
 * Created by Srishti on 14 September 2020
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
        //Enumeration<String> headers = requestToCache.getHeaderNames();
        for (Enumeration<?> e = requestToCache.getHeaderNames(); e.hasMoreElements();) {
            String nextHeaderName = (String) e.nextElement();
            String headerValue = requestToCache.getHeader(nextHeaderName);
            System.out.println(nextHeaderName + "   " +headerValue);
        }

        Collection<String> list = responseToCache.getHeaderNames();

        for (String s : responseToCache.getHeaderNames()) {
            System.out.println(s + "=== " + responseToCache.getHeader(s));
        }
        logMessage.setApp_guid(requestToCache.getHeader("app_id"));

        logMessage.setApp_name("");
        logMessage.setBytes(String.valueOf(responseToCache.getBufferSize()));
        //logMessage.setClientLocation(new Location()));
        logMessage.setClientIPv4(requestToCache.getRemoteAddr());
        //logMessage.setLocation(new Location());
        logMessage.setGorouterTime("");

        logMessage.setHttpVersion(StringUtils.substringAfter(requestToCache.getProtocol(), "/"));

        logMessage.setInstance(1);
        logMessage.setOriginCode("");
        logMessage.setProcessTime("");
        logMessage.setReferer(requestToCache.getHeader("Referer"));
        logMessage.setHttpStatus(responseToCache.getStatus());

        logMessage.setServerDate(requestToCache.getHeader("Date"));
        logMessage.setServerTime(" ");

        logMessage.setServerTimeZoneOffset("+00:00");
        logMessage.setUrlQuery(requestToCache.getQueryString());
        logMessage.setUrlPath(requestToCache.getPathInfo());
        logMessage.setUserAgent(requestToCache.getHeader("User-Agent"));

        logMessage.setVcapRequestId(requestToCache.getHeader("Vcap-Request-Id"));
        logMessage.setMethod(requestToCache.getMethod());
        logMessage.setxB3parentSpanId(requestToCache.getHeader("x-b3-parentspanid"));
        logMessage.setxB3SpanId(requestToCache.getHeader("x-b3-spanid"));
        logMessage.setxB3TraceId(requestToCache.getHeader("x-b3-traceid"));
        logMessage.setxGlobalTransId(requestToCache.getHeader("x_global_transaction_id"));

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


}

