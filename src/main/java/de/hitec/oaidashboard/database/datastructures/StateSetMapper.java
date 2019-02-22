package de.hitec.oaidashboard.database.datastructures;

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
@Table(name = "STATESETMAPPER",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "ss_id" }) })

public class StateSetMapper {
	private long id;
	private State state; 
	private Set set;
	private long recordCount;
	
	public StateSetMapper() {}

	public StateSetMapper(State state, Set set) {
		this.state = state;
		this.set = set;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "ss_id")
	public long getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long id) {
		this.id = id;
	}

	@ManyToOne(optional = false)
	@JoinColumn(name="state_id", referencedColumnName="state_id", updatable=false)
	public State getState() {
		return state;
	}

	public void setState( State state ) {
		this.state = state;
	}

	@ManyToOne(optional = false)
	@JoinColumn(name="set_id", referencedColumnName="set_id", updatable=false)
	public Set getSet() {
		return set;
	}

	public void setSet( Set set ) {
		this.set = set;
	}
	
	@Column(name = "record_count", nullable = false)
	public long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount( long rc ) {
		this.recordCount = rc;
	}

}
