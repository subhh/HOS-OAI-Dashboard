package de.hitec.oaidashboard.database.datastructures;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "LICENSE",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "license_id" }) })

public class License {
	private int id;
	private String name;
	private String type;
	
	public License() {}
	public License(String name) {
		this.name = name;
		this.type = "UNASSIGNED";
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "license_id")
	public int getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int id) {
		this.id = id;
	}
	
	@Column(name = "license_name", length = 200, nullable = false)
	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	@Column(name = "license_type", length = 200, nullable = false)
	public String getType() {
		return type;
	}

	public void setType( String type ) {
		this.type = type;
	}
}
