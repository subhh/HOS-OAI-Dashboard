package de.hitec.oaidashboard.database.datastructures;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;

@Entity
@Table(name = "STATE",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "state_id" }) })

public class State {
	private long id;
	private long recordCount;
	private Timestamp timestamp;
	private long recordCountFulltext;
	private long recordCountOA;
	private Timestamp earliestRecordTimestamp;   
	private Timestamp latestRecordTimestamp;   
	private Repository repository;
	private String status;
	private Timestamp startTime;   
	private Timestamp endTime;
	private String errorMessage; 

	public State() {}
	public State(Timestamp timestamp, Repository repo, String status) {
		this.timestamp = timestamp;
		this.repository = repo;
		this.status = status;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "state_id")
	public long getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long id) {
		this.id = id;
	}

	@Column(name = "record_count", nullable = false)
	public long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount( long rc ) {
		this.recordCount = rc;
	}

	@Column(name = "timestamp", nullable = false)
	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp( Timestamp timestamp ) {
		this.timestamp = timestamp;
	}

	@Column(name = "record_count_fulltext")
	public long getRecordCountFulltext() {
		return recordCountFulltext;
	}

	public void setRecordCountFulltext( long rc ) {
		this.recordCountFulltext = rc;
	}

	@Column(name = "record_count_oa")
	public long getRecordCountOA() {
		return recordCountOA;
	}

	public void setRecordCountOA( long rc ) {
		this.recordCountOA = rc;
	}

	@Column(name = "earliest_record_timestamp")
	public Timestamp getEarliestRecordTimestamp() {
		return earliestRecordTimestamp;
	}

	public void setEarliestRecordTimestamp( Timestamp timestamp ) {
		this.earliestRecordTimestamp = timestamp;
	}

	@Column(name = "latest_record_timestamp")
	public Timestamp getLatestRecordTimestamp() {
		return latestRecordTimestamp;
	}

	public void setLatestRecordTimestamp( Timestamp timestamp ) {
		this.latestRecordTimestamp = timestamp;
	}
	
	@Column(name = "status", length = 50, nullable = false)
	public String getStatus() {
		return status;
	}

	public void setStatus( String status ) {
		this.status = status;
	}
	
	@Column(name = "start_time")
	public Timestamp getStartTime() {
		return startTime;
	}

	public void setstartTime( Timestamp start ) {
		this.startTime = start;
	}

	@Column(name = "end_time")
	public Timestamp getEndTime() {
		return endTime;
	}

	public void setendTime( Timestamp end ) {
		this.endTime = end;
	}

	@Column(name = "error_message", length = 300)
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage( String errorMessage ) {
		this.errorMessage = errorMessage;
	}

	@ManyToOne(optional = false)
	@JoinColumn(name="repository_id", referencedColumnName="repository_id", updatable=false)
	public Repository getRepository() {
		return repository;
	}

	public void setRepository( Repository repo ) {
		this.repository = repo;
	}

}
