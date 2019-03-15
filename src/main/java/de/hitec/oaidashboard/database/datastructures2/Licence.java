package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.*;

@Entity
@Table(name = "LICENCE",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "licence_id" }) })

public class Licence {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "licence_id")
	private int id;

	@Column(name = "licence_name", length = 200, nullable = false)
	private String name;

	@Column(name = "licence_type", length = 200, nullable = false)
	private String type;
	
	public Licence() {}
	public Licence(String name) {
		this.name = name;
		this.type = "UNASSIGNED";
	}

	public int getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType( String type ) {
		this.type = type;
	}
}
