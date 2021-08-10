package eu.europeana.fulltext.api.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgAnnopage")
@Table(name = "annopage")
public class PgAnnopage {

    @Transient
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annopage_id_gen")
    @SequenceGenerator(name="annopage_id_gen", sequenceName = "annopage_id_seq", allocationSize=50)
    private Long id;

    @NotNull
    private Integer dataset;

    @NotNull
    private String localdoc;

    @NotNull
    @Column(name = "page")
    private int page;

    @Column(name = "target_url")
    private String targetUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "res_id", referencedColumnName = "id")
    private PgResource pgResource;

    @OneToMany(
            mappedBy = "pgAnnopage",
            cascade = CascadeType.ALL
    )
    private List<PgAnnotation> pgAnnotations = new ArrayList<>();

    @Column(name = "date_modified")
    private Date dateModified;

    /**
     * creates an Annopage object
     * @param dataset       dataset identifier
     * @param localdoc      local doc identifier
     * @param page          page number
     * @param targetUrl     base URL of annotation Targets for this Annopage
     * @param dateModified  date modified
     */
    public PgAnnopage(Integer dataset, String localdoc, int page, String targetUrl, Date dateModified) {
        this.dataset = dataset;
        this.localdoc = localdoc;
        this.page = page;
        this.targetUrl = targetUrl;
        this.dateModified = dateModified;
    }

    /**
     * creates an Annopage object plus related resource
     * @param dataset       dataset identifier
     * @param localdoc      local doc identifier
     * @param page          page number
     * @param targetUrl     base URL of annotation Targets for this Annopage
     * @param pgResource    resource object
     * @param dateModified  date modified
     */
    public PgAnnopage(Integer dataset, String localdoc, int page, String targetUrl, PgResource pgResource, Date dateModified) {
        this(dataset, localdoc, page, targetUrl, dateModified);
        this.pgResource = pgResource;
    }

    /**
     * creates an Annopage object plus related resource and Annotations
     * @param dataset       dataset identifier
     * @param localdoc      local doc identifier
     * @param page          page number
     * @param targetUrl     base URL of annotation Targets for this Annopage
     * @param pgResource    resource object
     * @param dateModified  date modified
     * @param pgAnnotations List of annotations
     */
    public PgAnnopage(Integer dataset, String localdoc, int page, String targetUrl, PgResource pgResource, Date dateModified, List<PgAnnotation> pgAnnotations) {
        this(dataset, localdoc, page, targetUrl, pgResource, dateModified);
        this.pgAnnotations = pgAnnotations;
    }

    public PgAnnopage(){ }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDataset() {
        return dataset;
    }

    public void setDataset(Integer dataset) {
        this.dataset = dataset;
    }

    public String getLocaldoc() {
        return localdoc;
    }

    public void setLocaldoc(String localdoc) {
        this.localdoc = localdoc;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public PgResource getPgResource() {
        return pgResource;
    }

    public void setPgResource(PgResource pgResource) {
        this.pgResource = pgResource;
    }

    public List<PgAnnotation> getPgAnnotations() {
        return pgAnnotations;
    }

    public void setPgAnnotations(List<PgAnnotation> pgAnnotations) {
        this.pgAnnotations = pgAnnotations;
    }

    public void addPgAnnotation(PgAnnotation pgAnnotation){
        pgAnnotations.add(pgAnnotation);
        pgAnnotation.setPgAnnopage(this);
    }

    public void removePgAnnotation(PgAnnotation pgAnnotation){
        pgAnnotations.remove(pgAnnotation);
        pgAnnotation.setPgAnnopage(null);
    }

    public Date getDateModified() {
        return dateModified;
    }

    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    @Override
    public String toString() {
        return "PgAnnopage{"
               + "dataset="
               + dataset
               + ", localdoc="
               + localdoc
               + ", page="
               + page
               + ", targetUrl="
               + targetUrl
               + ", language="
               + pgResource.getLanguage()
               + ", dateModified="
               + dateFormat.format(dateModified)
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgAnnopage that = (PgAnnopage) o;
        return dataset.equals(that.dataset)
               && localdoc.equalsIgnoreCase(that.localdoc)
               && getPage() == that.getPage()
               && targetUrl.equalsIgnoreCase(that.targetUrl)
               && getPgResource().getLanguage().equalsIgnoreCase(that.getPgResource().getLanguage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataset, localdoc, getPage(), getTargetUrl(), getPgResource().getLanguage());
    }
}
