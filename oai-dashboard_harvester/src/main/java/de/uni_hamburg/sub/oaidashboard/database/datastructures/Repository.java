package de.uni_hamburg.sub.oaidashboard.database.datastructures;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.builder.StandardToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;

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

	@JsonIgnore
	@NotNull
	@Size(max=250)
	private String initialDirectoryHash;

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
	@Column(unique=true)
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
		this.initialDirectoryHash = createHash(url);
	}

	public Repository(String name, String url, String mail) {
		this.name = name;
		this.harvesting_url = url;
		this.kontakt = mail;
		this.state = "ACTIVE";
		this.initialDirectoryHash = createHash(url);
	}

	public int getId() {
		return repository_id;
	}

	// Hibernate insists on having a setter method,
	// but the id is chosen by the database.
	private void setId(int repository_id) {
		this.repository_id = repository_id;
	}

	public String getInitialDirectoryHash() {
		return initialDirectoryHash;
	}

	// Hibernate insists on having a setter method, but the
	// directoryHash is used as identifier, which is created
	// upon repository creation and *must never* be changed.
	private void setInitialDirectoryHash(String dh) {
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
		this.geodaten = geodaten;
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

	private String createHash(String url) {
		UUID uuid = UUID.randomUUID();
		String hash = null;
		try {
			hash = (String) File.separator 
	    			+ Base64.getUrlEncoder().withoutPadding().encodeToString(
	    					(uuid.toString() + url).getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException ex) {
			System.err.println("Caught Exception: " + ex.getMessage());
		}
		return hash;
	}

	@Override
	public String toString() {
		StandardToStringStyle style = new StandardToStringStyle();
		style.setFieldSeparator(", ");
		style.setUseClassName(false);
		style.setUseIdentityHashCode(false);
		style.setFieldNameValueSeparator(" = ");
		style.setNullText("");
		String a = "'";
		return new ToStringBuilder(this, style)
				.append("ID", repository_id)
				.append("name", a + name + a)
				.append("harvesting_url", a + harvesting_url + a)
				.append("land",  a + land + a)
				.append("bundesland", a + bundesland + a)
				.append("geodaten", a + geodaten + a)
				.append("technische_plattform", a + technische_plattform + a)
				.append("kontakt", a + kontakt + a)
				.append("state", a + state + a)
				.toString().replace("'null'", "''");
	}
}
