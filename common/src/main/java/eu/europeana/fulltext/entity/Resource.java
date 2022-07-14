package eu.europeana.fulltext.entity;

import dev.morphia.annotations.*;

/** Created by luthien on 31/05/2018. */
@Entity(value = "Resource", useDiscriminator = false)
@Indexes({
  @Index(
      fields = {@Field("dsId"), @Field("lcId"), @Field("pgId"), @Field("lang")},
      options = @IndexOptions(unique = true)),
    // only index contributed Resources
  @Index(
      fields = {@Field("contributed")},
      options = @IndexOptions(partialFilter = "{contributed: {$eq: true}}"))
})
public class Resource {

    @Id
    private String id;    // custom Mongo ID
    private String dsId;  // IIIF_API_BASE_URL/{dsId}/      /annopage/
    private String lcId;  // IIIF_API_BASE_URL/      /{lcId}/annopage/
    private String lang;
    private String value;
    private String pgId;

    private String source;
    private String rights;
    private boolean contributed;
    private boolean translation;

    // temp field added for migration; will be removed afterwards
    @Transient
    private String oldDbId;

    /**
     * Empty constructor required for serialisation
     */
    public Resource() {
    }

    public Resource(String id, String lang, String value, String rights) {
        this.id     = id;
        this.lang   = lang;
        this.value  = value;
        this.rights = rights;
    }

    public Resource(String id, String lang, String value, String rights, String dsId, String lcId) {
        this(id, lang, value, rights);
        this.dsId = dsId;
        this.lcId = lcId;
    }

    public Resource(String id, String lang, String value, String rights, String dsId, String lcId, String source) {
        this(id, lang, value, rights, dsId, lcId);
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getDsId() {
        return dsId;
    }

    public void setDsId(String dsId) {
        this.dsId = dsId;
    }

    public String getLcId() {
        return lcId;
    }

    public void setLcId(String lcId) {
        this.lcId = lcId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRights() { return rights; }

    public void setRights(String rights) { this.rights = rights; }

    public boolean isContributed() {
        return contributed;
    }

    public void setContributed(boolean contributed) {
        this.contributed = contributed;
    }

    public String getOldDbId() {
        return oldDbId;
    }

    public void setOldDbId(String oldDbId) {
        this.oldDbId = oldDbId;
    }

    public String getPgId() {
        return pgId;
    }

    public void setPgId(String pgId) {
        this.pgId = pgId;
    }

    public boolean isTranslation() {
        return translation;
    }

    public void setTranslation(boolean translation) {
        this.translation = translation;
    }
}
