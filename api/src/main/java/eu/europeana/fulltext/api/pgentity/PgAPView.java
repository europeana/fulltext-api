package eu.europeana.fulltext.api.pgentity;

import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Entity object for Annopage view (includes language and resource)
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgAPView")
@Immutable
@Table(name = "v_annopages")
public class PgAPView {

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

    @OneToMany(
            mappedBy = "pgAnnopage",
            cascade = CascadeType.ALL
    )
    private List<PgAnnotation> pgAnnotations = new ArrayList<>();

    @NotNull
    private String language;

    @NotNull
    private String rights;

    @Column(name = "original")
    private boolean original;

    @NotNull
    @Column(name = "value")
    private String value;

    @NotNull
    @Column(name = "source")
    private String source;

    @Column(name = "date_modified")
    private Date dateModified;

    @Column(name = "target_url")
    private String targetUrl;

    @Column(name = "res_id")
    private Long resId;

    public PgAPView(){ }

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public boolean isOriginal() {
        return original;
    }

    public void setOriginal(boolean original) {
        this.original = original;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getResId() {
        return resId;
    }

    public void setResId(Long resId) {
        this.resId = resId;
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
               + getLanguage()
               + ", rights="
               + getRights()
               + ", original="
               + original
               + ", value="
               + value
               + ", source="
               + source
               + ", dateModified="
               + dateFormat.format(dateModified)
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgAPView that = (PgAPView) o;
        return dataset.equals(that.dataset)
               && localdoc.equalsIgnoreCase(that.getLocaldoc())
               && page == that.getPage()
               && targetUrl.equalsIgnoreCase(that.getTargetUrl())
               && language.equalsIgnoreCase(that.getLanguage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataset, localdoc, getPage(), targetUrl, language);
    }
}
