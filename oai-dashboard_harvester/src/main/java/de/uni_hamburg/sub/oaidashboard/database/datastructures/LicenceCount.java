package de.uni_hamburg.sub.oaidashboard.database.datastructures;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "LICENCECOUNT",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "lc_id" }) })

public class LicenceCount {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long lc_id;

    @NotNull
    @Size(max=200)
    private String licence_name;

    @NotNull
    @Enumerated(EnumType.STRING)
    private LicenceType licence_type;

    @NotNull
    private Integer record_count;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "state_id")
    private HarvestingState state;

    public LicenceCount() {}

    public LicenceCount(String licence_name, HarvestingState state, LicenceType lType) {
        this.licence_name = licence_name;
        this.licence_type = lType;
        this.state = state;
    }

    public long getId() {
        return lc_id;
    }

    public void setId(long lc_id) {
        this.lc_id = lc_id;
    }

    public String getLicence_name() {
        return licence_name;
    }

    public void setLicence_name(String licence_name) {
        this.licence_name = licence_name;
    }

    public LicenceType getLicence_type() {
        return licence_type;
    }

    public void setLicence_type(LicenceType licence_type) {
        this.licence_type = licence_type;
    }

    public Integer getRecord_count() {
        return record_count;
    }

    public void setRecord_count(Integer record_count) {
        this.record_count = record_count;
    }

    public HarvestingState getState() {
        return state;
    }

    public void setState(HarvestingState state) {
        this.state = state;
    }
}

