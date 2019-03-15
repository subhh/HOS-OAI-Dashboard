package de.hitec.oaidashboard.database.datastructures2;

import de.hitec.oaidashboard.database.datastructures.State;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "OAISET",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "set_id" }) })

public class OAISet {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "set_id")
    private int id;

    @NotNull
    @Column(name = "set_name", length = 200)
    private String name;

    @NotNull
    @Column(name = "set_spec", length = 200)
    private String spec;

    @OneToMany(mappedBy = "set", cascade = CascadeType.ALL)
    private Set<StateSetMapper> stateSetMappers = new HashSet<>();

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RECORDSETMAPPER",
            joinColumns = @JoinColumn(name = "set_id"),
            inverseJoinColumns = @JoinColumn(name = "record_id"))
    private List<Record> records = new ArrayList<>();

    public OAISet() {}
    public OAISet(String name, String spec) {
        this.name = name;
        this.spec = spec;
    }

    public int getId() {
        return id;
    }

    // Hibernate insists on having a setter method,
    // but the id is chosen by the database.
    private void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec( String spec ) {
        this.spec = spec;
    }

    public Set<StateSetMapper> getStateSetMappers() {
        return stateSetMappers;
    }

    public void setStateSetMappers(Set<StateSetMapper> stateSetMappers) {
        this.stateSetMappers = stateSetMappers;
    }

    public void addStateSetMapper(StateSetMapper stateSetMapper) {
        this.stateSetMappers.add(stateSetMapper);
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }
}
