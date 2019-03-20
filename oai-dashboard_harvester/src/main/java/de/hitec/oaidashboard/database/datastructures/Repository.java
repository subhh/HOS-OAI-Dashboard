package de.hitec.oaidashboard.database.datastructures;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "REPOSITORY",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "repository_id" }) })

@NamedQueries({
        @NamedQuery(name="get_active_repositories", query="from Repository where state='ACTIVE'"),
})
public class Repository {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int repository_id;

	@NotNull
	@Size(max=200)
	private String name;

	@Size(max=100)
	private String land;

	@Size(max=100)
	private String bundesland;

	@Size(max=500)
	private String geodaten;

	@Size(max=100)
	private String technische_plattform;

	@Size(max=100)
	private String repo_typ;

	@Size(max=100)
	private String oa_status;

    @NotNull
    @Size(max=400)
	private String harvesting_url;

	private Timestamp first_index_timestamp;

    @Size(max=150)
    private String kontakt;

    @NotNull
    @Size(max=50)
	private String state;

	public Repository() {
		this.state = "ACTIVE";
	}
	
	public Repository(String name, String url) {
		this.name = name;
		this.harvesting_url = url;
		this.state = "ACTIVE";
	}

	public Repository(String name, String url, String mail) {
		this.name = name;
		this.harvesting_url = url;
		this.kontakt = mail;
		this.state = "ACTIVE";
	}

	public int getId() {
		return repository_id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int repository_id) {
		this.repository_id = repository_id;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public String getLand() {
		return land;
	}

	public void setLand( String land ) {
		this.land = land;
	}

	public String getBundesland() {
		return bundesland;
	}

	public void setBundesland( String bundesland ) {
		this.bundesland = bundesland;
	}

	public String getGeodaten() {
		return geodaten;
	}

	public void setGeodaten( String geodaten ) {
		this.land = geodaten;
	}
	
	public String getTechnische_plattform() {
		return technische_plattform;
	}

	public void setTechnische_plattform(String technische_plattform) {
		this.technische_plattform = technische_plattform;
	}
	
	public String getRepo_typ() {
		return repo_typ;
	}

	public void setRepo_typ(String repo_typ) {
		this.repo_typ = repo_typ;
	}
	
	public String getOa_status() {
		return oa_status;
	}

	public void setOa_status(String status ) {
		this.oa_status = status;
	}
	
	public String getHarvesting_url() {
		return harvesting_url;
	}

	public void setHarvesting_url(String harvesting_url) {
		this.harvesting_url = harvesting_url;
	}

	public Timestamp getFirst_index_timestamp() {
		return first_index_timestamp;
	}

	public void setfirstIndexTimestamp( Timestamp firstIndexTimestamp ) {
		this.first_index_timestamp = firstIndexTimestamp;
	}
	public String getKontakt() {
		return kontakt;
	}

	public void setKontakt( String kontakt ) {
		this.kontakt = kontakt;
	}

	public String getState() {
		return state;
	}

	public void setState( String state ) {
		this.state = state;
	}
}
