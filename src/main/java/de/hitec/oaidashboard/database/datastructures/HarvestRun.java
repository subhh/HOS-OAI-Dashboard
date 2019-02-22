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
@Table(name = "HARVESTRUN",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "run_id" }) })

public class HarvestRun {
	private int id;
	private String stateStatus; 
	private Timestamp startTime;   
	private Timestamp endTime;
	private String errorMessage; 
	private State state;
	
	public HarvestRun() {}
	public HarvestRun(Timestamp start) {
		this.startTime = start;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "run_id")
	public int getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int id) {
		this.id = id;
	}

	@Column(name = "state", length = 50)
	public String getStateStatus() {
		return stateStatus;
	}

	public void setStateStatus( String stateStatus ) {
		this.stateStatus = stateStatus;
	}

	@Column(name = "start_time", nullable = false)
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
	@JoinColumn(name="state_id", referencedColumnName="state_id", updatable=false)
	public State getState() {
		return state;
	}

	public void setState( State state ) {
		this.state = state;
	}
}
