package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "STATE",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "state_id" }) })

public class HarvestingState {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	private long state_id;

    @NotNull
	private long record_count;

    @NotNull
	private Timestamp timestamp;

    @Column(name = "record_count_fulltext")
	private long recordCountFulltext;

    @Column(name = "record_count_oa")
    private long recordCountOA;

    @Column(name = "earliest_record_timestamp")
    private Timestamp earliestRecordTimestamp;

    @Column(name = "latest_record_timestamp")
	private Timestamp latestRecordTimestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name="repository_id", referencedColumnName="repository_id", updatable=false)
	private Repository repository;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "start_time")
	private Timestamp startTime;

    @Column(name = "end_time")
	private Timestamp endTime;

    @Column(name = "error_message", length = 300)
    private String errorMessage;

    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
    private Set<LicenceCount> licenceCounts;

    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
    private Set<SetCount> setCounts;

    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
    private Set<MetadataFormat> metadataFormats;

    public HarvestingState() {}
	public HarvestingState(Timestamp timestamp, Repository repo, String status) {
		this.timestamp = timestamp;
		this.repository = repo;
		this.status = status;
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

	public long getRecordCountFulltext() {
		return recordCountFulltext;
	}

	public void setRecordCountFulltext( long rc ) {
		this.recordCountFulltext = rc;
	}

	public long getRecordCountOA() {
		return recordCountOA;
	}

	public void setRecordCountOA( long rc ) {
		this.recordCountOA = rc;
	}

	public Timestamp getEarliestRecordTimestamp() {
		return earliestRecordTimestamp;
	}

	public void setEarliestRecordTimestamp( Timestamp timestamp ) {
		this.earliestRecordTimestamp = timestamp;
	}

	public Timestamp getLatestRecordTimestamp() {
		return latestRecordTimestamp;
	}

	public void setLatestRecordTimestamp( Timestamp timestamp ) {
		this.latestRecordTimestamp = timestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus( String status ) {
		this.status = status;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setstartTime( Timestamp start ) {
		this.startTime = start;
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp end ) {
		this.endTime = end;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage( String errorMessage ) {
		this.errorMessage = errorMessage;
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
