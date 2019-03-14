package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "STATESETMAPPER",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "ss_id" }) })

public class StateSetMapper {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "ss_id")
	private long id;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "state_id")
	private HarvestingState state;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "set_id")
	private OAISet set;

	@NotNull
	@Column(name = "record_count")
	private long recordCount;
	
	public StateSetMapper() {}

	public StateSetMapper(HarvestingState state, OAISet set) {
		this.state = state;
		this.set = set;
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

	public OAISet getSet() {
		return set;
	}

	public void setSet( OAISet set ) {
		this.set = set;
	}

	public long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount( long rc ) {
		this.recordCount = rc;
	}
}
