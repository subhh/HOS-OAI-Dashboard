package de.uni_hamburg.sub.oaidashboard.database.datastructures;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "HARVESTINGSTATE",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "state_id" }) })

@NamedQueries({
        @NamedQuery(name="get_state_at_timepoint", query="from HarvestingState where repository_id = :repo_id " +
                "AND timestamp >= :timepoint_from " +
                "AND timestamp < :timepoint_to"),
})
public class HarvestingState {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	private long state_id;

    @NotNull
	private long record_count;

    @NotNull
	private Timestamp timestamp;

	private long record_count_fulltext;

    private long record_count_oa;

    private Timestamp earliest_record_timestamp;

	private Timestamp latest_record_timestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name="repository_id", referencedColumnName="repository_id", updatable=false)
	private Repository repository;

    @NotNull
    @Enumerated(EnumType.STRING)
    private HarvestingStatus status;

	private Timestamp start_time;

	private Timestamp end_time;

    @Size(max=300)
    private String error_message;

    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
    private Set<LicenceCount> licenceCounts;

    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
    private Set<SetCount> setCounts;

    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
    private Set<MetadataFormat> metadataFormats;

    public HarvestingState() {}
	public HarvestingState(Timestamp timestamp, Repository repo, HarvestingStatus status) {
		this.timestamp = timestamp;
		this.repository = repo;
		this.status = status;
	}

    /**
     * Fixes the Lazy-Initialization-Problem: "org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role"
     * Call after getting a HarvestingState from Database, while still in session.
     */
	public void fixLazyInitialization() {
        getLicenceCounts().size();
        getSetCounts().size();
        getMetadataFormats().size();
    }

	public long getId() {
		return state_id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long state_id) {
		this.state_id = state_id;
	}

	public long getRecord_count() {
		return record_count;
	}

	public void setRecord_count( long rc ) {
		this.record_count = rc;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp( Timestamp timestamp ) {
		this.timestamp = timestamp;
	}

	public long getRecord_count_fulltext() {
		return record_count_fulltext;
	}

	public void setRecord_count_fulltext(long rc ) {
		this.record_count_fulltext = rc;
	}

	public long getRecord_count_oa() {
		return record_count_oa;
	}

	public void setRecord_count_oa(long rc ) {
		this.record_count_oa = rc;
	}

	public Timestamp getEarliest_record_timestamp() {
		return earliest_record_timestamp;
	}

	public void setEarliest_record_timestamp(Timestamp timestamp ) {
		this.earliest_record_timestamp = timestamp;
	}

	public Timestamp getLatest_record_timestamp() {
		return latest_record_timestamp;
	}

	public void setLatest_record_timestamp(Timestamp timestamp ) {
		this.latest_record_timestamp = timestamp;
	}

	public HarvestingStatus getStatus() {
		return status;
	}

	public void setStatus( HarvestingStatus status ) {
		this.status = status;
	}

	public Timestamp getStart_time() {
		return start_time;
	}

	public void setstartTime( Timestamp start ) {
		this.start_time = start;
	}

	public Timestamp getEnd_time() {
		return end_time;
	}

	public void setEnd_time(Timestamp end ) {
		this.end_time = end;
	}

	public String getError_message() {
		return error_message;
	}

	public void setError_message(String error_message) {
		this.error_message = error_message;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository( Repository repo ) {
		this.repository = repo;
	}

	public Set<LicenceCount> getLicenceCounts() {
		return licenceCounts;
	}

	public void setLicenceCounts(Set<LicenceCount> licenceCounts) {
		this.licenceCounts = licenceCounts;
	}

    public Set<SetCount> getSetCounts() {
        return setCounts;
    }

    public void setSetCounts(Set<SetCount> setCounts) {
        this.setCounts = setCounts;
    }

    public Set<MetadataFormat> getMetadataFormats() {
        return metadataFormats;
    }

    public void setMetadataFormats(Set<MetadataFormat> metadataFormats) {
        this.metadataFormats = metadataFormats;
    }
}
