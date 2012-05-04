package edu.ucsf.profiles.shindig.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.social.opensocial.model.Account;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Drinker;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.LookingFor;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.NetworkPresence;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;

public class ProfilesPerson implements Person {
	  protected String aboutMe;
	  protected List <Account> accounts;
	  protected List<String> activities;
	  protected List<Address> addresses;
	  protected Integer age;
	  protected Map<String, ? extends Object> appData;
	  protected Date birthday;
	  protected BodyType bodyType;
	  protected List<String> books;
	  protected List<String> cars;
	  protected String children;
	  protected Address currentLocation;
	  private String displayName;
	  protected Enum<Drinker> drinker;
	  protected List<ListField> emails;
	  protected String ethnicity;
	  protected String fashion;
	  protected List<String> food;
	  protected Gender gender;
	  protected String happiestWhen;
	  protected Boolean hasApp;
	  protected List<String> heroes;
	  protected String humor;
	  protected String id;
	  protected List<ListField> ims;
	  protected List<String> interests;
	  protected String jobInterests;
	  protected List<String> languagesSpoken;
	  protected Date updated;
	  protected String livingArrangement;
	  protected List<Enum<LookingFor>> lookingFor;
	  protected List<String> movies;
	  protected List<String> music;
	  protected Name name;
	  protected Enum<NetworkPresence> networkPresence;
	  protected String nickname;
	  protected List<Organization> organizations;
	  protected String pets;
	  protected List<ListField> phoneNumbers;
	  protected List<ListField> photos;
	  protected String politicalViews;
	  protected String preferredUsername;
	  protected Url profileSong;
	  protected Url profileVideo;
	  protected String profileUrl;
	  protected List<String> quotes;
	  protected String relationshipStatus;
	  protected String religion;
	  protected String romance;
	  protected String scaredOf;
	  protected String sexualOrientation;
	  protected Enum<Smoker> smoker;
	  protected List<String> sports;
	  protected String status;
	  protected List<String> tags;
	  protected Long utcOffset;
	  protected String thumbnailUrl;
	  protected List<String> turnOffs;
	  protected List<String> turnOns;
	  protected List<String> tvShows;
	  protected List<Url> urls;
	  	  
	  // Note: Not in the opensocial js person object directly
	  private boolean isOwner = false;
	  private boolean isViewer = false;

	public String getAboutMe() {
		return aboutMe;
	}

	public void setAboutMe(String aboutMe) {
		this.aboutMe = aboutMe;
	}

	public List<String> getActivities() {
		return activities;
	}

	public void setActivities(List<String> activities) {
		this.activities = activities;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public BodyType getBodyType() {
		return bodyType;
	}

	public void setBodyType(BodyType bodyType) {
		this.bodyType = bodyType;
	}

	public List<String> getBooks() {
		return books;
	}

	public void setBooks(List<String> books) {
		this.books = books;
	}

	public List<String> getCars() {
		return cars;
	}

	public void setCars(List<String> cars) {
		this.cars = cars;
	}

	public Address getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Address currentLocation) {
		this.currentLocation = currentLocation;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Enum<Drinker> getDrinker() {
		return drinker;
	}

	public void setDrinker(Enum<Drinker> drinker) {
		this.drinker = drinker;
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public List<ListField> getEmails() {
		return emails;
	}

	public void setEmails(List<ListField> emails) {
		this.emails = emails;
	}

	public String getEthnicity() {
		return ethnicity;
	}

	public void setEthnicity(String ethnicity) {
		this.ethnicity = ethnicity;
	}

	public String getFashion() {
		return fashion;
	}

	public void setFashion(String fashion) {
		this.fashion = fashion;
	}

	public List<String> getFood() {
		return food;
	}

	public void setFood(List<String> food) {
		this.food = food;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getHappiestWhen() {
		return happiestWhen;
	}

	public void setHappiestWhen(String happiestWhen) {
		this.happiestWhen = happiestWhen;
	}

	public Boolean getHasApp() {
		return hasApp;
	}

	public void setHasApp(Boolean hasApp) {
		this.hasApp = hasApp;
	}

	public List<String> getHeroes() {
		return heroes;
	}

	public void setHeroes(List<String> heroes) {
		this.heroes = heroes;
	}

	public String getHumor() {
		return humor;
	}

	public void setHumor(String humor) {
		this.humor = humor;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ListField> getIms() {
		return ims;
	}

	public void setIms(List<ListField> ims) {
		this.ims = ims;
	}

	public List<String> getInterests() {
		return interests;
	}

	public void setInterests(List<String> interests) {
		this.interests = interests;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getPets() {
		return pets;
	}

	public void setPets(String pets) {
		this.pets = pets;
	}

	public Url getProfileSong() {
		return profileSong;
	}

	public void setProfileSong(Url profileSong) {
		this.profileSong = profileSong;
	}

	public Url getProfileVideo() {
		return profileVideo;
	}

	public void setProfileVideo(Url profileVideo) {
		this.profileVideo = profileVideo;
	}

	public Long getUtcOffset() {
		return utcOffset;
	}

	public void setUtcOffset(Long utcOffset) {
		this.utcOffset = utcOffset;
	}

	public List<String> getMovies() {
		return movies;
	}

	public void setMovies(List<String> movies) {
		this.movies = movies;
	}

	public List<String> getQuotes() {
		return quotes;
	}

	public void setQuotes(List<String> quotes) {
		this.quotes = quotes;
	}

	public String getRomance() {
		return romance;
	}

	public void setRomance(String romance) {
		this.romance = romance;
	}

	public List<String> getTurnOffs() {
		return turnOffs;
	}

	public void setTurnOffs(List<String> turnOffs) {
		this.turnOffs = turnOffs;
	}

	public List<String> getTurnOns() {
		return turnOns;
	}

	public void setTurnOns(List<String> turnOns) {
		this.turnOns = turnOns;
	}

	public String getLivingArrangement() {
		return livingArrangement;
	}

	public void setLivingArrangement(String livingArrangement) {
		this.livingArrangement = livingArrangement;
	}

	public List<Enum<LookingFor>> getLookingFor() {
		return lookingFor;
	}

	public void setLookingFor(List<Enum<LookingFor>> lookingFor) {
		this.lookingFor = lookingFor;
	}

	public List<String> getMusic() {
		return music;
	}

	public void setMusic(List<String> music) {
		this.music = music;
	}

	public List<ListField> getPhotos() {
		return photos;
	}

	public void setPhotos(List<ListField> photos) {
		this.photos = photos;
	}

	public String getPoliticalViews() {
		return politicalViews;
	}

	public void setPoliticalViews(String politicalViews) {
		this.politicalViews = politicalViews;
	}

	public String getPreferredUsername() {
		return preferredUsername;
	}

	public void setPreferredUsername(String preferredUsername) {
		this.preferredUsername = preferredUsername;
	}

	public String getRelationshipStatus() {
		return relationshipStatus;
	}

	public void setRelationshipStatus(String relationshipStatus) {
		this.relationshipStatus = relationshipStatus;
	}

	public String getReligion() {
		return religion;
	}

	public void setReligion(String religion) {
		this.religion = religion;
	}

	public String getScaredOf() {
		return scaredOf;
	}

	public void setScaredOf(String scaredOf) {
		this.scaredOf = scaredOf;
	}

	public String getSexualOrientation() {
		return sexualOrientation;
	}

	public void setSexualOrientation(String sexualOrientation) {
		this.sexualOrientation = sexualOrientation;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public List<Organization> getOrganizations() {
		return organizations;
	}

	public void setOrganizations(List<Organization> organizations) {
		this.organizations = organizations;
	}

	public Enum<Smoker> getSmoker() {
		return smoker;
	}

	public void setSmoker(Enum<Smoker> smoker) {
		this.smoker = smoker;
	}

	public List<String> getSports() {
		return sports;
	}

	public void setSports(List<String> sports) {
		this.sports = sports;
	}

	public List<String> getTvShows() {
		return tvShows;
	}

	public void setTvShows(List<String> tvShows) {
		this.tvShows = tvShows;
	}

	public List<Url> getUrls() {
		return urls;
	}

	public void setUrls(List<Url> urls) {
		this.urls = urls;
	}

	public String getJobInterests() {
		return jobInterests;
	}

	public void setJobInterests(String jobInterests) {
		this.jobInterests = jobInterests;
	}

	public List<String> getLanguagesSpoken() {
		return languagesSpoken;
	}

	public void setLanguagesSpoken(List<String> languagesSpoken) {
		this.languagesSpoken = languagesSpoken;
	}

	public List<ListField> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(List<ListField> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean getIsOwner() {
		return isOwner;
	}

	public void setIsOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}

	public boolean getIsViewer() {
		return isViewer;
	}

	public void setIsViewer(boolean isViewer) {
		this.isViewer = isViewer;
	}

	public String getProfileUrl() {
		return profileUrl;
	}

	public void setProfileUrl(String profileUrl) {
		this.profileUrl = profileUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getChildren() {
		return children;
	}

	public void setChildren(String children) {
		this.children = children;
	}

	public Enum<NetworkPresence> getNetworkPresence() {
		return networkPresence;
	}

	public void setNetworkPresence(Enum<NetworkPresence> networkPresence) {
		this.networkPresence = networkPresence;
	}

	public Map<String, ? extends Object> getAppData() {
		return appData;
	}

	public void setAppData(Map<String, ? extends Object> appData) {
		this.appData = appData;
	}

}
