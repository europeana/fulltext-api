package eu.europeana.fulltext.api.model;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by luthien on 07/04/2021.
 */
public class AnnoPageInfo implements Serializable {
    private static final long serialVersionUID = -8052995235828716772L;

    private String    dataSetId;
    private String                          localId;
    private HashMap<String, List<LangPage>> pages;

    public AnnoPageInfo(String dataSetId, String localId){
        this.dataSetId = dataSetId;
        this.localId = localId;
        this.pages = new HashMap<>();
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public void addPage(String pageUrl, String pageId, String lang){
        List<LangPage> langList = new ArrayList<>();
        langList.add(new LangPage(pageUrl, lang, true));
        pages.put(pageId, langList);
    }

    public void addLangToPage(String pageUrl, String pageId, String lang){
        pages.get(pageId).add(new LangPage(pageUrl, lang, false));
    }

    public class LangPage {
        String pageUrl;
        boolean orig;
        String lang;

        public LangPage(String pageUrl, String lang, boolean orig){
            this.pageUrl = pageUrl;
            this.lang = lang;
            this.orig = orig;
        }

        public String getPageUrl() {
            return pageUrl;
        }

        public void setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
        }

        public boolean isOrig() {
            return orig;
        }

        public void setOrig(boolean orig) {
            this.orig = orig;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }
    }

}
