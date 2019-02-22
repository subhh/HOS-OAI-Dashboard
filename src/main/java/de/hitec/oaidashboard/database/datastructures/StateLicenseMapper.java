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
@Table(name = "STATELICENSEMAPPER",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "sl_id" }) })

public class StateLicenseMapper {
	private long id;
	private State state; 
	private License license;
	private long recordCount;
	
	public StateLicenseMapper() {}
	public StateLicenseMapper(State state, License license, long rc) {
		this.state = state;
		this.license = license;
		this.recordCount = rc;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "sl_id")
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
	@JoinColumn(name="license_id", referencedColumnName="license_id", updatable=false)
	public License getLicense() {
		return license;
	}

	public void setLicense( License license ) {
		this.license = license;
	}
	
	@Column(name = "record_count", nullable = false)
	public long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount( long rc ) {
		this.recordCount = rc;
	}

}
