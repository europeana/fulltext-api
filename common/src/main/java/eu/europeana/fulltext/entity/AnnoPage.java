package eu.europeana.fulltext.entity;

import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by luthien on 31/05/2018.
 * Namespace assumptions (see the FTDefinitions class):
 * IIIF Api base URL: https://iiif.europeana.eu/presentation/
 * Resource base URL: https://www.europeana.eu/api/fulltext/
 */
@Entity(value = "AnnoPage", useDiscriminator = false)
@Indexes(@Index(fields = {@Field("dsId"), @Field("lcId"), @Field("pgId")}, options = @IndexOptions(unique = true)))
public class AnnoPage {

    @Id
    private ObjectId         _id;
    private String           dsId;
    private String           lcId;
    private String           pgId;
    private String           tgtId;
    private List<Annotation> ans;
    private Date             modified;
    private String           lang;
    @Indexed
    private String           source;

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

    public String toString() {
        return "/" + this.dsId + "/" + this.getLcId() + "/" + this.getPgId();
    }
}
