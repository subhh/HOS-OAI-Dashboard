package de.hitec.oaidashboard.database.datastructures;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "SETCOUNT",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "sc_id" }) })

public class SetCount {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long sc_id;

    @NotNull
    @Size(max=200)
    private String set_name;

    @NotNull
    @Size(max=200)
    private String set_spec;

    private int record_count;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "state_id")
    private HarvestingState state;

    public SetCount() {

    }

    public SetCount(String set_name, String set_spec, HarvestingState state) {
        this.set_name = set_name;
        this.set_spec = set_spec;
        this.state = state;
    }

    public long getId() {
        return sc_id;
    }

    public void setId(long sc_id) {
        this.sc_id = sc_id;
    }

    public String getSet_name() {
        return set_name;
    }

    public void setSet_name(String set_name) {
        this.set_name = set_name;
    }

    public String getSet_spec() {
        return set_spec;
    }

    public void setSet_spec(String set_spec) {
        this.set_spec = set_spec;
    }

    public int getRecord_count() {
        return record_count;
    }

    public void setRecord_count(int record_count) {
        this.record_count = record_count;
    }

    public HarvestingState getState() {
        return state;
    }

    public void setState(HarvestingState state) {
        this.state = state;
    }
}
