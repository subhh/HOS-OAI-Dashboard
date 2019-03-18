package de.hitec.oaidashboard.database.datastructures2;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "METADATAFORMAT",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "format_id" }) })

public class MetadataFormat {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long format_id;

	@NotNull
	@Size(max=100)
	private String prefix;

	@NotNull
	@Size(max=200)
	private String format_schema;

	@NotNull
	@Size(max=200)
	private String namespace;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "state_id")
    private HarvestingState state;

	public MetadataFormat(String prefix, String format_schema, String namespace, HarvestingState state) {
		this.prefix = prefix;
		this.format_schema = format_schema;
		this.namespace = namespace;
		this.state = state;
	}


	public long getId() {
		return format_id;
	}

	// Hibernate insists on having a setter method,
	// but the metadataformat_id is chosen by the database.
	private void setId(long format_id) {
		this.format_id = format_id;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix( String prefix ) {
		this.prefix = prefix;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace( String namespace ) {
		this.namespace = namespace;
	}

    public HarvestingState getState() {
        return state;
    }

    public void setState(HarvestingState state) {
        this.state = state;
    }

    public String getFormat_schema() {
        return format_schema;
    }

    public void setFormat_schema(String format_schema) {
        this.format_schema = format_schema;
    }
}
