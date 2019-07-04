package eu.europeana.fulltext.entity;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Transient;

import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 */
@Embedded
public class Annotation {

    private String       anId;
    private char         dcType;
    private String       motiv;
    private String       lang;
    private Integer      from;
    private Integer      to;
    private List<Target> tgs;

    /*
     * These two boolean parameters facilitate processing the Annotation during the loading process, to avoid having to
     * check dcType all the time.
     * isMedia is true in case of annotations for video/audio captioning, transcribing or subtitles, false for fulltext
     * isTopLevel is true if the annotation pertains to the whole Page of media file; false for all other annotations
     */
    @Transient
    private boolean      isMedia;

    @Transient
    private boolean      isTopLevel;

    public Annotation(){}

    public Annotation(String      anId,
                      char      dcType,
                      Integer     from,
                      Integer     to) {
        this.anId   = anId;
        this.dcType = dcType;
        this.from   = from;
        this.to     = to;
    }

    public Annotation(String       anId,
                      char       dcType,
                      Integer      from,
                      Integer      to,
                      List<Target> tgs) {
        this(anId, dcType, from, to);
        this.tgs = tgs;
    }

    public Annotation(String       anId,
                      char       dcType,
                      Integer      from,
                      Integer      to,
                      List<Target> tgs,
                      String       lang) {
        this(anId, dcType, from, to, tgs);
        this.lang = lang;
    }

    public String getAnId() {
        return anId;
    }

    public void setAnId(String anId) {
        this.anId = anId;
    }

    public char getDcType() {
        return dcType;
    }

    public void setDcType(char dcType) {
        this.dcType = dcType;
    }

    public String getMotiv() {
        return motiv;
    }

    public void setMotiv(String motiv) {
        this.motiv = motiv;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public List<Target> getTgs() {
        return tgs;
    }

    public void setTgs(List<Target> tgs) {
        this.tgs = tgs;
    }

    public boolean isMedia() {
        return isMedia;
    }

    public void setMedia(boolean media) {
        isMedia = media;
    }

    public boolean isTopLevel() {
        return isTopLevel;
    }

    public void setTopLevel(boolean topLevel) {
        isTopLevel = topLevel;
    }
}
