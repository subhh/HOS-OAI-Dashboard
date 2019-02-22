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
@Table(name = "STATEFORMATMAPPER",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "sf_id" }) })

public class StateFormatMapper {
	private long id;
	private State state; 
	private MetadataFormat metadataFormat;
	
	public StateFormatMapper() {}
	public StateFormatMapper(State state, MetadataFormat format) {
		this.state = state;
		this.metadataFormat = format;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "sf_id")
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
	@JoinColumn(name="format_id", referencedColumnName="format_id", updatable=false)
	public MetadataFormat getMetadataFormat() {
		return metadataFormat;
	}

	public void setMetadataFormat( MetadataFormat format ) {
		this.metadataFormat = format;
	}	
}
