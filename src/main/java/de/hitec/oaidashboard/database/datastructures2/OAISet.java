package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "OAISET",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "set_id" }) })

public class OAISet {
    private int id;
    private String name;
    private String spec;

    public OAISet() {}
    public OAISet(String name, String spec) {
        this.name = name;
        this.spec = spec;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "set_id")
    public int getId() {
        return id;
    }

    // Hibernate insists on having a setter method,
    // but the id is chosen by the database.
    private void setId(int id) {
        this.id = id;
    }

    @NotNull
    @Column(name = "set_name", length = 200)
    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @NotNull
    @Column(name = "set_spec", length = 200)
    public String getSpec() {
        return spec;
    }

    public void setSpec( String spec ) {
        this.spec = spec;
    }
}
