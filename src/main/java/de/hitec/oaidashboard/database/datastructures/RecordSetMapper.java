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
@Table(name = "RECORDSETMAPPER",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "rs_id" }) })

public class RecordSetMapper {
	private long id;
	private Record record; 
	private String setSpec = null;
	private Set set = null;
	
	public RecordSetMapper() {}

	public RecordSetMapper(Record record, Set set) {
		this.record = record;
		this.set = set;
	}

	public RecordSetMapper(Record record, String setSpec) {
		this.record = record;
		this.setSpec = setSpec;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "rs_id")
	public long getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(long id) {
		this.id = id;
	}

	@ManyToOne(optional = false)
	@JoinColumn(name="record_id", referencedColumnName="record_id", updatable=false)
	public Record getRecord() {
		return record;
	}

	public void setRecord( Record record ) {
		this.record = record;
	}

	@ManyToOne(optional = true)
	@JoinColumn(name="set_id", referencedColumnName="set_id", updatable=false)
	public Set getSet() {
		return set;
	}

	public void setSet( Set set ) {
		this.set = set;
	}
	
	@Column(name = "set_spec", length = 200, nullable = true)
	public String getSetSpec() {
		return setSpec;
	}

	public void setSetSpec( String setSpec ) {
		this.setSpec = setSpec;
	}
}
