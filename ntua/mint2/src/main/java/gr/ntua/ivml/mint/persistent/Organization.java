package gr.ntua.ivml.mint.persistent;

import gr.ntua.ivml.mint.Publication;
import gr.ntua.ivml.mint.db.DB;
import gr.ntua.ivml.mint.util.Config;
import gr.ntua.ivml.mint.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

public class Organization implements SecurityEnabled {
	
	public static final Logger log = Logger.getLogger(Organization.class);
	
	public long dbID;
	String originalName;
	String englishName;
	
	String shortName;
	String description;

	String urlPattern;
	String address;
	String country;
	String type;
	Organization parentalOrganization;
	User primaryContact;
	boolean publishAllowed = false;
	
	List<Organization> dependantOrganizations = new ArrayList<Organization>();
	List<User> users = new ArrayList<User>();
	List<DataUpload> dataUploads = new ArrayList<DataUpload>();
	
	// transient 
	
	Publication publication;
	//
	// useful functions
	//
	
	public Publication getPublication() {
		if( publication != null ) return publication;
		if( publishAllowed ) {
			String pubClass = Config.get( "publication.implementation" );
			try {
				Class<?> pubStartClass = this.getClass().getClassLoader().loadClass( pubClass );
				// make an object
					Constructor<?> firstChoice = pubStartClass.getConstructor(Organization.class);
					publication = (Publication) firstChoice.newInstance( this );
			} catch( Exception e ) {
				if( StringUtils.empty(pubClass)) {
					log.info( "No Publication configured in 'publication.implementation'");
				} else {
					log.info( "Could not instantiate " + pubClass );
				}
				publication = new Publication( this );
			}
			log.debug( "Use "+ publication.getClass().getCanonicalName() + " for Publication." );
		} else {
			publication = new Publication( this );
		}
		return publication;
	}
	
	// convenience 
	public List<Dataset> getPublishedDatasets() {
		return DB.getDatasetDAO().findPublishedByOrganization(this);
	}
	
	// temporary getter setter for old attribute
	public String getName() {
		return getEnglishName();
	}
	
	public void setName( String name ) {
		setEnglishName(name);
	}
	
	public String getShortName() {
		return shortName;
	}
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public List<User> getUsers() {
		return users;
	}
	public void setUsers(List<User> users) {
		this.users = users;
	}
	public List<Organization> getDependantOrganizations() {
		return dependantOrganizations;
	}
	public void setDependantOrganizations(List<Organization> dependantOrganizations) {
		this.dependantOrganizations = dependantOrganizations;
	}
	public long getDbID() {
		return dbID;
	}
	public void setDbID(long dbID) {
		this.dbID = dbID;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Organization getParentalOrganization() {
		return parentalOrganization;
	}
	public void setParentalOrganization(Organization parentalOrganization) {
		this.parentalOrganization = parentalOrganization;
	}
	public User getPrimaryContact() {
		return primaryContact;
	}
	public void setPrimaryContact(User primaryContact) {
		this.primaryContact = primaryContact;
	}


	public String getOriginalName() {
		return originalName;
	}
	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}
	public String getEnglishName() {
		return englishName;
	}
	public void setEnglishName(String englishName) {
		this.englishName = englishName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getUrlPattern() {
		return urlPattern;
	}

	public void setUrlPattern(String urlPattern) {
		this.urlPattern = urlPattern; 
	}

	public boolean isPublishAllowed() {
		return publishAllowed;
	}

	public void setPublishAllowed(boolean publishAllowed) {
		this.publishAllowed = publishAllowed;
	}

	public List<DataUpload> getDataUploads() {
		return dataUploads;
	}

	public  List<User> getUploaders() {
		return DB.getDataUploadDAO().getUploaders(this);
	}
	
	public void setDataUploads(List<DataUpload> dataUploads) {
		this.dataUploads = dataUploads;
	}

	/**
	 * Return all the dependent organizations all the levels down.
	 * @return
	 */
	public List<Organization> getDependantRecursive() {
		Map<Long, Organization> m = new HashMap<Long,Organization>();
		List<Organization> toDo = new ArrayList<Organization>();
		toDo.addAll(getDependantOrganizations());
		
		while( !toDo.isEmpty()) {
			Organization o = toDo.remove(0);
			if( ! m.containsKey(o.getDbID())) {
				m.put( o.getDbID(), o);
				toDo.addAll( o.getDependantOrganizations());
			}
		}
		toDo.clear();
		toDo.addAll(m.values());
		return toDo;
	}
	
	/**
	 * if find, just look for one
	 * @param find
	 * @return
	 */
	private List<User> directAdmins( boolean find ) {
		List<User> res = new ArrayList<User>();
		for( User u: getUsers() )
			if( u.getMintRole().equalsIgnoreCase("admin")) {
				res.add( u );
				if( find ) break;
			}
		return res;
	}
	
	/**
	 * if find, just look for one
	 * @param find
	 * @return
	 */
	private void adminsRecursive( boolean find, List<User> result ) {
		result.addAll( directAdmins( find ));
		if( !result.isEmpty() && find ) return;
		Organization parent = getParentalOrganization();
		if( parent != null )
			parent.adminsRecursive(find, result);
	}
	
	/**
	 * Counts all admins in this organizations and all parents
	 * @return
	 */
	public int getAdmincount() {
		List<User> admins = new ArrayList<User>();
		adminsRecursive(false, admins);
		return admins.size();
	}
	
	/**
	 * Find one admin in this organization or any parent
	 * @return
	 */
	public User findAdmin(){
		List<User> admins = new ArrayList<User>();
		adminsRecursive(true, admins);
		if( admins.isEmpty() ) return null;
		else return admins.get(0);
	}
	
	/**
	 * Returns all admins in this and parent organizations
	 * @return
	 */
	public List<User> getAllAdmins() {
		List<User> admins = new ArrayList<User>();
		adminsRecursive(false, admins);
		return admins;
	}
	
	public List<Mapping> getAllMappings() {
		return DB.getMappingDAO().findByOrganization(this);
	}
	
	public JSONObject toJSON() {
		JSONObject res = new JSONObject()
			.element( "dbID", getDbID())
			.element( "address", getAddress())
			.element( "country", getCountry())
			.element( "englishName" , getEnglishName())
			.element( "description", getDescription())
			.element( "publishAllowed", isPublishAllowed())
			.element( "originalName", getOriginalName())
			.element( "shortName", getShortName())
			.element( "type", getType())
			.element( "urlPattern", getUrlPattern());
		if( getParentalOrganization() != null )
			res.element( "parentalOrganization", new JSONObject()
				.element( "dbID", getParentalOrganization().getDbID()));
		if( getPrimaryContact() != null ) 
			res.element( "primaryContact", new JSONObject() 
				.element( "dbID", getPrimaryContact().getDbID()));
		return res;
	}
}
