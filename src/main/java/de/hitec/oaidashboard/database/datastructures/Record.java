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
@Table(name = "RECORD",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "record_id" }) })

public class Record {
	private long id;
	private License license;
	private State state;
	
	public Record() {}
	public Record(License license, State state) {
		this.license = license;
		this.state = state;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "record_id")
	public long getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long id) {
		this.id = id;
	}

	@ManyToOne(optional = false)
	@JoinColumn(name="license_id", referencedColumnName="license_id", updatable=false)
	public License getLicense() {
		return license;
	}

	public void setLicense( License license ) {
		this.license = license;
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
