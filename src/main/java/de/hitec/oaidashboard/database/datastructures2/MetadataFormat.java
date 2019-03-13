package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.*;

@Entity
@Table(name = "METADATAFORMAT",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "metadataformat_id" }) })

public class MetadataFormat {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "metadataformat_id")
	private long metadataformat_id;

	@Column(name = "format_prefix", length = 100, nullable = false)
	private String formatPrefix;

	@Column(name = "format_schema", length = 200, nullable = false)
	private String schema;

	@Column(name = "namespace", length = 200, nullable = false)
	private String namespace;
	
	public MetadataFormat() {}

	public MetadataFormat(String formatPrefix, String schema, String namespace) {
		this.formatPrefix = formatPrefix;
		this.schema = schema;
		this.namespace = namespace;
	}


	public long getMetadataformat_id() {
		return metadataformat_id;
	}

	// Hibernate insists on having a setter method,
	// but the metadataformat_id is chosen by the database.
	private void setMetadataformat_id(long metadataformat_id) {
		this.metadataformat_id = metadataformat_id;
	}

	public String getFormatPrefix() {
		return formatPrefix;
	}

	public void setFormatPrefix( String formatPrefix ) {
		this.formatPrefix = formatPrefix;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema( String schema ) {
		this.schema = schema;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace( String namespace ) {
		this.namespace = namespace;
	}
}
