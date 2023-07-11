package eu.europeana.fulltext.entity;

import dev.morphia.annotations.*;
import org.bson.types.ObjectId;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 * Namespace assumptions (see the FTDefinitions class):
 * IIIF Api base URL: https://iiif.europeana.eu/presentation/
 * Resource base URL: https://www.europeana.eu/api/fulltext/
 */
@Entity(value = "AnnoPage", useDiscriminator = false)
@Indexes(@Index(fields = {@Field("dsId"), @Field("lcId"), @Field("pgId"), @Field("lang")}, options = @IndexOptions(unique = true)))
public class AnnoPage {

    @Id
    private ObjectId         _id;
    private String           dsId;
    private String           lcId;
    private String           pgId;
    @Indexed
    private String           tgtId;
    private List<Annotation> ans;
    private Date             modified;
    private String           lang;
    @Indexed(options = @IndexOptions(sparse = true))
    private String           source;
    @Indexed
    private Date deleted;

    private boolean translation;

    @Reference
    private Resource res;


    /**
     * Empty constructor required for serialisation
     */
    public AnnoPage() {
        init();
    }

    /**
     * Create a new AnnoPage object using the following parameters:
     *
     * @param dsId  String containing the dataset of this Fulltext SummaryManifest
     * @param lcId  String containing the localId of this Fulltext SummaryManifest
     * @param pgId  String containing the page number of this Fulltext SummaryManifest
     * @param tgtId String containing the target ID of this Fulltext SummaryManifest
     * @param lang  String containing the language code of this Fulltext SummaryManifest
     * @param res   reference to the Resource linked to this Fulltext SummaryManifest
     */
    public AnnoPage(String dsId, String lcId, String pgId, String tgtId, String lang, Resource res) {
        this.dsId = dsId;
        this.lcId = lcId;
        this.pgId = pgId;
        this.tgtId = tgtId;
        this.res = res;
        this.lang = lang;
        init();
    }

    private void init(){
        ans = new ArrayList<>();
        modified = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
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

    public String getPgId() {
        return pgId;
    }

    public void setPgId(String pgId) {
        this.pgId = pgId;
    }

    public Resource getRes() {
        return res;
    }

    public void setRes(Resource res) {
        this.res = res;
    }

    public String getTgtId() {
        return tgtId;
    }

    public void setTgtId(String tgtId) {
        this.tgtId = tgtId;
    }

    public List<Annotation> getAns() {
        return ans;
    }

    public void setAns(List<Annotation> ans) {
        this.ans = ans;
    }

    public Date getModified() {
        return (Date) modified.clone();
    }

    public void setModified(Date modified) {
        this.modified = (Date) modified.clone();
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getDeleted() {
        return deleted;
    }

    public void setDeleted(Date deleted) {
        this.deleted = deleted;
    }

    public boolean isDeprecated(){
        return deleted != null;
    }

    public boolean isActive(){
        return deleted == null;
    }

    public String toString() {
        return "/" + this.dsId + "/" + this.getLcId() + "/" + this.getPgId();
    }

    /**
     * Morphia handles a "save" operation as an update if the mongo _id is set.
     *
     * This method copies the _id value from a source AnnoPage object to this one.
     * @param source source AnnoPage
     */
    public void copyDbIdFrom(@Nullable AnnoPage source){
        if (source != null) {
            _id = source._id;
        }
    }

    public ObjectId getDbId(){
        return _id;
    }

    public boolean isTranslation() {
        return translation;
    }

    public void setTranslation(boolean translation) {
        this.translation = translation;
    }

}
