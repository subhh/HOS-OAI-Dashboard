package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RECORD",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "record_id" }) })

public class Record {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "record_id")
	private long id;

	@NotNull
    @Column(name = "identifier")
	private String identifier;

	@ManyToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name="licence_id", referencedColumnName="licence_id", updatable=false)
	private Licence licence;

	@ManyToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name="state_id", referencedColumnName="state_id", updatable=false)
	private HarvestingState state;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RECORDSETMAPPER",
            joinColumns = @JoinColumn(name = "record_id"),
            inverseJoinColumns = @JoinColumn(name = "set_id"))
    private List<OAISet> oaiSets = new ArrayList<>();

    // not managed by Hibernate!
    @Transient
    private List<String> set_specs = new ArrayList<>();

    // not managed by Hibernate!
	@Transient
	private String licence_str;

	public Record() {}
	public Record(String identifier) {
		this.identifier = identifier;
	}

	public long getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long id) {
		this.id = id;
	}

	public String getIdentifier() { return identifier; }

	public void setIdentifier( String identifier ) { this.identifier = identifier; }

	public Licence getLicence() {
		return licence;
	}

	public void setLicense( Licence licence ) {
		this.licence = licence;
	}

	public HarvestingState getState() {
		return state;
	}

	public void setState( HarvestingState state ) {
		this.state = state;
	}

    public List<OAISet> getOaiSets() {
        return oaiSets;
    }

    public void setOaiSets(List<OAISet> oaiSets) {
        this.oaiSets = oaiSets;
    }

    public List<String> getSet_specs() {
        return set_specs;
    }

    public void setSet_specs(List<String> set_specs) {
        this.set_specs = set_specs;
    }

	public String getLicence_str() {
		return licence_str;
	}

	public void setLicence_str(String licence_str) {
		this.licence_str = licence_str;
	}
}
