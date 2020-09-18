package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.logs.Location;

/**
 * Created by Srishti on 14 September 2020
 */
public class LogMessage {

        private String app_guid;
        private String app_name;
        private String bytes;
        private Location clientLocation;
        private String clientIPv4;
        private Location location;

        private String gorouterTime;

        private String httpVersion;
        private int instance;
        private String ipV4;
        private String originCode;
        private String processTime;
        private String referer;
        private int httpStatus;
        private String serverDate;
        private String serverTime;
        private String serverTimeZoneOffset;
        private String type;
        private String urlQuery;
        private String urlPath;
        private String userAgent;
        private String vcapRequestId;
        private String method;
        private String xB3parentSpanId;
        private String xB3SpanId;
        private String xB3TraceId;
        private String xGlobalTransId;

    public void setApp_guid(String app_guid) {
        this.app_guid = app_guid;
    }

    public void setApp_name(String app_name) {
        this.app_name = app_name;
    }

    public void setBytes(String bytes) {
        this.bytes = bytes;
    }

    public void setClientLocation(Location clientLocation) {
        this.clientLocation = clientLocation;
    }

    public void setClientIPv4(String clientIPv4) {
        this.clientIPv4 = clientIPv4;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setGorouterTime(String gorouterTime) {
        this.gorouterTime = gorouterTime;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public void setIpV4(String ipV4) {
        this.ipV4 = ipV4;
    }

    public void setOriginCode(String originCode) {
        this.originCode = originCode;
    }

    public void setProcessTime(String processTime) {
        this.processTime = processTime;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setServerDate(String serverDate) {
        this.serverDate = serverDate;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public void setServerTimeZoneOffset(String serverTimeZoneOffset) {
        this.serverTimeZoneOffset = serverTimeZoneOffset;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUrlQuery(String urlQuery) {
        this.urlQuery = urlQuery;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setVcapRequestId(String vcapRequestId) {
        this.vcapRequestId = vcapRequestId;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setxB3parentSpanId(String xB3parentSpanId) {
        this.xB3parentSpanId = xB3parentSpanId;
    }

    public void setxB3SpanId(String xB3SpanId) {
        this.xB3SpanId = xB3SpanId;
    }

    public void setxB3TraceId(String xB3TraceId) {
        this.xB3TraceId = xB3TraceId;
    }

    public void setxGlobalTransId(String xGlobalTransId) {
        this.xGlobalTransId = xGlobalTransId;
    }

    @Override
    public String toString() {
        return "LogMessage{" + "\n" +
                "app_guid='" + app_guid + '\'' + "\n" +
                ", app_name='" + app_name + '\'' +"\n" +
                ", bytes='" + bytes + '\'' +"\n" +
                ", clientLocation=" + clientLocation +"\n" +
                ", clientIPv4='" + clientIPv4 + '\'' +"\n" +
                ", location=" + location +"\n" +
                ", gorouterTime='" + gorouterTime + '\'' +"\n" +
                ", httpVersion='" + httpVersion + '\'' +"\n" +
                ", instance=" + instance +"\n" +
                ", ipV4='" + ipV4 + '\'' +"\n" +
                ", originCode='" + originCode + '\'' +"\n" +
                ", processTime='" + processTime + '\'' +"\n" +
                ", referer='" + referer + '\'' +"\n" +
                ", httpStatus=" + httpStatus +"\n" +
                ", serverDate=" + serverDate +"\n" +
                ", serverTime='" + serverTime + '\'' +"\n" +
                ", serverTimeZoneOffset='" + serverTimeZoneOffset + '\'' +"\n" +
                ", type='" + type + '\'' +"\n" +
                ", urlQuery='" + urlQuery + '\'' +"\n" +
                ", urlPath='" + urlPath + '\'' +"\n" +
                ", userAgent='" + userAgent + '\'' +"\n" +
                ", vcapRequestId='" + vcapRequestId + '\'' +"\n" +
                ", method='" + method + '\'' +"\n" +
                ", xB3parentSpanId='" + xB3parentSpanId + '\'' +"\n" +
                ", xB3SpanId='" + xB3SpanId + '\'' +"\n" +
                ", xB3TraceId='" + xB3TraceId + '\'' +"\n" +
                ", global trabs id ='" + xGlobalTransId + '\'' +"\n" +
                '}';
    }
}

