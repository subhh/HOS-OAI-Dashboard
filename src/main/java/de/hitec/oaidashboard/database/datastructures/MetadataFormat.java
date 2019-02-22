package de.hitec.oaidashboard.database.datastructures;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "METADATAFORMAT",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "format_id" }) })

public class MetadataFormat {
	private int id;
	private String formatPrefix; 
	private String schema;   
	private String namespace;
	
	public MetadataFormat() {}
	public MetadataFormat(String formatPrefix, String schema, String namespace) {
		this.formatPrefix = formatPrefix;
		this.schema = schema;
		this.namespace = namespace;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "format_id")
	public int getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int id) {
		this.id = id;
	}

	@Column(name = "format_prefix", length = 100, nullable = false)
	public String getFormatPrefix() {
		return formatPrefix;
	}

	public void setFormatPrefix( String formatPrefix ) {
		this.formatPrefix = formatPrefix;
	}

	@Column(name = "format_schema", length = 200, nullable = false)
	public String getSchema() {
		return schema;
	}

	public void setSchema( String schema ) {
		this.schema = schema;
	}

	@Column(name = "namespace", length = 200, nullable = false)
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace( String namespace ) {
		this.namespace = namespace;
	}
}
