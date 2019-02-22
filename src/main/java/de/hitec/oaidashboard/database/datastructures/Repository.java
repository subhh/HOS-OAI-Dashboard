package de.hitec.oaidashboard.database.datastructures;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "REPOSITORY",
   uniqueConstraints = { @UniqueConstraint(columnNames = { "repository_id" }) })

public class Repository {
	private int id;
	private String name;
	private String land;
	private String bundesland;
	private String geodaten;
	private String technischePlattform;
	private String repoTyp;
	private String oaStatus;
	private String harvestingUrl;
	private Timestamp firstIndexTimestamp;
	private String kontakt;
	private String state;

	public Repository() {
		this.state = "ACTIVE";
	}
	
	public Repository(String name, String url) {
		this.name = name;
		this.harvestingUrl = url;
		this.state = "ACTIVE";
	}

	public Repository(String name, String url, String mail) {
		this.name = name;
		this.harvestingUrl = url;
		this.kontakt = mail;
		this.state = "ACTIVE";
	}

	public boolean updateOnChange(String name, String url, String mail) {
		boolean change = false;
		if (this.name != name)
		{
			this.name = name;
			change = true;
		}
		if (this.harvestingUrl != url)
		{
			this.harvestingUrl = url;
			change = true;
		}
		if (this.kontakt != mail)
		{
			this.kontakt = mail;
			change = true;
		}
		return change;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "repository_id")
	public int getId() {
		return id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int id) {
		this.id = id;
	}

	@Column(name = "name", length = 200, nullable = false)
	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	@Column(name = "land", length = 100)
	public String getLand() {
		return land;
	}

	public void setLand( String land ) {
		this.land = land;
	}

	@Column(name = "bundesland", length = 100)
	public String getBundesland() {
		return bundesland;
	}

	public void setBundesland( String bundesland ) {
		this.bundesland = bundesland;
	}

	@Column(name = "geodaten", length = 500)
	public String getGeodaten() {
		return geodaten;
	}

	public void setGeodaten( String geodaten ) {
		this.land = geodaten;
	}
	
	@Column(name = "technische_plattform", length = 100)
	public String getTechnischePlattform() {
		return technischePlattform;
	}

	public void setTechnischePlattform( String technischePlattform ) {
		this.technischePlattform = technischePlattform;
	}
	
	@Column(name = "repo_typ", length = 100)
	public String getRepoTyp() {
		return repoTyp;
	}

	public void setRepoTyp( String repoTyp ) {
		this.repoTyp = repoTyp;
	}
	
	@Column(name = "oa_status", length = 100)
	public String getOaStatus() {
		return oaStatus;
	}

	public void setOaStatus( String status ) {
		this.oaStatus = status;
	}
	
	@Column(name = "harvesting_url", length = 400, nullable = false)
	public String getHarvestingUrl() {
		return harvestingUrl;
	}

	public void setHarvestingUrl( String harvestingUrl ) {
		this.harvestingUrl = harvestingUrl;
	}

	@Column(name = "first_index_timestamp")
	public Timestamp getFirstIndexTimestamp() {
		return firstIndexTimestamp;
	}

	public void setfirstIndexTimestamp( Timestamp firstIndexTimestamp ) {
		this.firstIndexTimestamp = firstIndexTimestamp;
	}

	@Column(name = "kontakt", length = 150)
	public String getKontakt() {
		return kontakt;
	}

	public void setKontakt( String kontakt ) {
		this.kontakt = kontakt;
	}

	@Column(name = "state", length = 50, nullable = false)
	public String getState() {
		return state;
	}

	public void setState( String state ) {
		this.state = state;
	}
}
