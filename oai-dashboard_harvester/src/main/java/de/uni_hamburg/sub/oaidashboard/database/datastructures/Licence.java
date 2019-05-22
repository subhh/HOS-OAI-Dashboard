package de.uni_hamburg.sub.oaidashboard.database.datastructures;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "LICENCE",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "licence_id" }) })

public class Licence {
	private int id;
	private String name;
	private LicenceType licenceType;
	private Timestamp validFrom;
	private Timestamp validUntil;
	
	public Licence() {}
	
	public Licence(String name) {
		this.name = name;
		this.licenceType = LicenceType.UNASSIGNED;
		this.validFrom = new Timestamp(0);
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "licence_id")
	public int getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int id) {
		this.id = id;
	}
	
	@Column(name = "licence_name", length = 200, nullable = false)
	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}
	
    @Enumerated(EnumType.STRING)
	@Column(name = "licence_type", length = 200, nullable = false)
	public LicenceType getType() {
    	return licenceType;
	}

	public void setType( LicenceType licenceType ) {
		this.licenceType = licenceType;
	}
	
	@Column(name = "valid_from")
	public Timestamp getValidFrom() {
		return this.validFrom;
	}
	
	public void setValidFrom(Timestamp validFrom) {
		this.validFrom = validFrom;	
	}
	
	@Column(name = "valid_until")
	public Timestamp getValidUntil() {
		return this.validUntil;
	}
	
	public void setValidUntil(Timestamp validUntil) {
		this.validUntil = validUntil;	
	}
}