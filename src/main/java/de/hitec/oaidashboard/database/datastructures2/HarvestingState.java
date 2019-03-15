package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "STATE",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "state_id" }) })

public class HarvestingState {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "state_id")
	private long id;

    @Column(name = "record_count", nullable = false)
	private long recordCount;

    @Column(name = "timestamp", nullable = false)
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

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "STATEFORMATMAPPER",
            joinColumns = @JoinColumn(name = "state_id"),
            inverseJoinColumns = @JoinColumn(name = "metadataformat_id"))
    //@ElementCollection(targetClass = MetadataFormat.class)
	private List<MetadataFormat> metadataFormats = new ArrayList<>();

	@OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
	private Set<StateSetMapper> stateSetMappers;

	@OneToMany(mappedBy = "state", cascade = CascadeType.ALL)
	private Set<StateLicenceMapper> stateLicenceMappers;

	public HarvestingState() {}
	public HarvestingState(Timestamp timestamp, Repository repo, String status) {
		this.timestamp = timestamp;
		this.repository = repo;
		this.status = status;
	}

	public long getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long id) {
		this.id = id;
	}


	public long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount( long rc ) {
		this.recordCount = rc;
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

	public void setendTime( Timestamp end ) {
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

    public List<MetadataFormat> getMetadataFormats() {
        return metadataFormats;
    }

    public void setMetadataFormats(List<MetadataFormat> metadataFormats) {
        this.metadataFormats = metadataFormats;
    }

    public void addMetadataFormat(MetadataFormat metadataFormat){
	    this.getMetadataFormats().add(metadataFormat);
    }

	public Set<StateSetMapper> getStateSetMappers() {
		return stateSetMappers;
	}

	public void setStateSetMappers(Set<StateSetMapper> stateSetMappers) {
		this.stateSetMappers = stateSetMappers;
	}

	public Set<StateLicenceMapper> getStateLicenceMappers() {
		return stateLicenceMappers;
	}

	public void setStateLicenceMappers(Set<StateLicenceMapper> stateLicenceMappers) {
		this.stateLicenceMappers = stateLicenceMappers;
	}
}
