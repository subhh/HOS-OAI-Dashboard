package de.hitec.oaidashboard.database.datastructures2;


import javax.persistence.*;

@Entity
@Table(name = "STATELICENCEMAPPER",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "sl_id" }) })

public class StateLicenceMapper {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "sl_id")
	private long id;

	@ManyToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name="state_id", referencedColumnName="state_id", updatable=false)
	private HarvestingState state;

	@ManyToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name="licence_id", referencedColumnName="licence_id", updatable=false)
	private Licence licence;

	@Column(name = "record_count", nullable = false)
	private long recordCount;

	public StateLicenceMapper() {}
	public StateLicenceMapper(HarvestingState state, Licence licence) {
		this.state = state;
		this.licence = licence;
	}

	public long getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long id) {
		this.id = id;
	}

	public HarvestingState getState() {
		return state;
	}

	public void setState( HarvestingState state ) {
		this.state = state;
	}

	public Licence getLicence() {
		return licence;
	}

	public void setLicence( Licence licence ) {
		this.licence = licence;
	}
	
	public long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount( long rc ) {
		this.recordCount = rc;
	}

}
