package de.uni_hamburg.sub.oaidashboard.database.datastructures;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


@Entity
@Table(name = "FORMATCOUNT",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "dc_format_id" }) })

public class DCFormatCount {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long dc_format_id;

    @NotNull
    @Size(max=200)
    private String dc_format;

    @NotNull
    private Integer record_count;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "state_id")
    private HarvestingState state;

    public DCFormatCount() {

    }

    public DCFormatCount(String dc_format, HarvestingState state) {
        this.dc_format = dc_format;
        this.state = state;
    }

    public long getId() {
        return dc_format_id;
    }

    public void setId(long dc_format_id) {
        this.dc_format_id = dc_format_id;
    }

    public String getDc_Format() {
        return dc_format;
    }

    public void setDc_Format(String dc_format) {
        this.dc_format = dc_format;
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

