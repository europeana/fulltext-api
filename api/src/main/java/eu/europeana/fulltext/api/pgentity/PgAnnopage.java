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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", referencedColumnName = "id")
    private PgDataset pgDataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_id", referencedColumnName = "id")
    private PgLocaldoc pgLocaldoc;

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
     * @param pgDataset     dataset identifier
     * @param pgLocaldoc    local doc identifier
     * @param page          page number
     */
    public PgAnnopage(PgDataset pgDataset, PgLocaldoc pgLocaldoc, int page) {
        this.pgDataset = pgDataset;
        this.pgLocaldoc = pgLocaldoc;
        this.page = page;
    }

    public PgAnnopage(PgDataset pgDataset, PgLocaldoc pgLocaldoc, String page) {
        this.pgDataset = pgDataset;
        this.pgLocaldoc = pgLocaldoc;
        this.page = Integer.parseInt(page);
    }

    /**
     * creates an Annopage object plus related resource
     * @param pgDataset     dataset identifier
     * @param pgLocaldoc    local doc identifier
     * @param page          page number
     * @param pgResource    resource object
     */
    public PgAnnopage(PgDataset pgDataset, PgLocaldoc pgLocaldoc, int page, PgResource pgResource) {
        this(pgDataset, pgLocaldoc, page);
        this.pgResource = pgResource;
    }

    public PgAnnopage(){ }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PgDataset getPgDataset() {
        return pgDataset;
    }

    public void setPgDataset(PgDataset pgDataset) {
        this.pgDataset = pgDataset;
    }

    public PgLocaldoc getPgLocaldoc() {
        return pgLocaldoc;
    }

    public void setPgLocaldoc(PgLocaldoc pgLocaldoc) {
        this.pgLocaldoc = pgLocaldoc;
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

    public Date getDateModified() {
        return dateModified;
    }

    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    @Override
    public String toString() {
        return "PgAnnopage{"
               + "pgDataset="
               + pgDataset.getValue()
               + ", pgLocaldoc="
               + pgLocaldoc.getValue()
               + ", page="
               + page
               + ", language="
               + pgResource.getPgLanguage().getValue()
               + ", dateModified="
               + dateFormat.format(dateModified)
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgAnnopage that = (PgAnnopage) o;
        return getPage() == that.getPage()
               && getPgDataset().getId().equals(that.getPgDataset().getId())
               && getPgLocaldoc().getId().equals(that.getPgLocaldoc().getId())
               && getPgResource().getPgLanguage().getValue().equals(that.getPgResource().getPgLanguage().getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPgDataset().getId(), getPgLocaldoc().getId(), getPage(), getPgResource().getPgLanguage().getValue());
    }
}
