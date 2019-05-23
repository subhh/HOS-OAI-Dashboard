package de.uni_hamburg.sub.oaidashboard.database.datastructures;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


@Entity
@Table(name = "TYPECOUNT",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "dc_type_id" }) })

public class DCTypeCount {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long dc_type_id;

    @NotNull
    @Size(max=200)
    private String dc_type;

    @NotNull
    private Integer record_count;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "state_id")
    private HarvestingState state;

    public DCTypeCount() {

    }

    public DCTypeCount(String dc_type, HarvestingState state) {
        this.dc_type = dc_type;
        this.state = state;
    }

    public long getId() {
        return dc_type_id;
    }

    public void setId(long dc_type_id) {
        this.dc_type_id = dc_type_id;
    }

    public String getDc_Type() {
        return dc_type;
    }

    public void setDc_Type(String dc_type) {
        this.dc_type = dc_type;
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
