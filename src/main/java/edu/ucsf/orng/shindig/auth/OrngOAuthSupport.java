package edu.ucsf.orng.shindig.auth;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;

import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.social.core.oauth.OAuthSecurityToken;
import org.apache.shindig.social.core.oauth2.OAuth2Client;
import org.apache.shindig.social.core.oauth2.OAuth2Client.ClientType;
import org.apache.shindig.social.core.oauth2.OAuth2Code;
import org.apache.shindig.social.core.oauth2.OAuth2DataService;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.ucsf.orng.shindig.spi.OrngDBUtil;

public class OrngOAuthSupport implements OAuthDataStore, OAuth2DataService {

	// This needs to be long enough that an attacker can't guess it.  If the attacker can guess this
	// value before they exceed the maximum number of attempts, they can complete a session fixation
	// attack against a user.
	private static final int CALLBACK_TOKEN_LENGTH = 6;

	// We limit the number of trials before disabling the request token.
	private static final int CALLBACK_TOKEN_ATTEMPTS = 5;

	private static final Logger LOG = Logger.getLogger(OrngOAuthSupport.class.getName());		

	// used to get samplecontainer data from canonicaldb.json
	private final OrngDBUtil dbUtil;
	private final String domain;
	private final OAuthServiceProvider SERVICE_PROVIDER;

	// for OAuth2DataService
	private List<OAuth2Client> clients; // list of clients
	private Map<String, List<OAuth2Code>> authCodes; // authorization codes per client
	private Map<String, List<OAuth2Code>> accessTokens; // access tokens per client

	@Inject
	public OrngOAuthSupport(OrngDBUtil dbUtil, @Named("shindig.oauth.base-url") String baseUrl,
			@Named("orng.systemDomain") String systemDomain) throws MalformedURLException {
		this.dbUtil = dbUtil;
		// grab just the mysite.edu part
		URL sys = new URL(systemDomain);
		String host = sys.getHost();
		while (host.indexOf('.') != host.lastIndexOf('.')) {
			host = host.substring(host.indexOf('.') + 1);
		}
		this.domain = host;
		this.SERVICE_PROVIDER = new OAuthServiceProvider(baseUrl + "/requestToken", baseUrl + "/authorize", baseUrl + "/accessToken");

		this.clients = Lists.newArrayList();
		this.authCodes = Maps.newHashMap();
		this.accessTokens = Maps.newHashMap();
		loadClients();
	}


	// All valid OAuth tokens
	private static Cache<String, OAuthEntry> oauthEntries = CacheBuilder.newBuilder()
			.build();

	// Get the OAuthEntry that corresponds to the oauthToken
	public OAuthEntry getEntry(String oauthToken) {
		Preconditions.checkNotNull(oauthToken);
		return oauthEntries.asMap().get(oauthToken);
	}

	public SecurityToken getSecurityTokenForConsumerRequest(String consumerKey,
			String userId) throws OAuthProblemException {
		String container = "default";
		return new OAuthSecurityToken(userId, null, consumerKey, domain, container, null,
				AuthenticationMode.OAUTH_CONSUMER_REQUEST.name());

	}

	public OAuthConsumer getConsumer(String consumerKey)
			throws OAuthProblemException {
		// consumer key defaults to the URL
		String appId = dbUtil.getAppId(consumerKey);
		String consumerSecret = null;
		if (appId != null) {
			Connection conn = dbUtil.getConnection();
			try {
				String sql = "SELECT [OAuthSecret] from [ORNG.].[Apps] where AppID = " + appId;
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					consumerSecret = rs.getString(1);
				}
			}
			catch (SQLException se) {
				LOG.log(Level.SEVERE, "Error reading OAuthSecret ", se);
			}
			finally {
				try { conn.close(); } catch (SQLException se) {
					LOG.log(Level.SEVERE, "Error closing connection", se);
				}
			}
		}
		if (consumerSecret == null)
			return null;

		// null below is for the callbackUrl, which we don't have in the db
		OAuthConsumer consumer = new OAuthConsumer(null, consumerKey, consumerSecret, SERVICE_PROVIDER);
		return consumer;
	}

	// Generate a valid requestToken for the given consumerKey
	public OAuthEntry generateRequestToken(String consumerKey,
			String oauthVersion, String signedCallbackUrl)
					throws OAuthProblemException {
		OAuthEntry entry = new OAuthEntry();
		entry.setAppId(consumerKey);
		entry.setConsumerKey(consumerKey);
		entry.setDomain(domain);
		entry.setContainer("default");

		entry.setToken(UUID.randomUUID().toString());
		entry.setTokenSecret(UUID.randomUUID().toString());

		entry.setType(OAuthEntry.Type.REQUEST);
		entry.setIssueTime(new Date());
		entry.setOauthVersion(oauthVersion);
		if (signedCallbackUrl != null) {
			entry.setCallbackUrlSigned(true);
			entry.setCallbackUrl(signedCallbackUrl);
		}

		oauthEntries.put(entry.getToken(), entry);
		return entry;
	}

	// Turns the request token into an access token
	public OAuthEntry convertToAccessToken(OAuthEntry entry) {
		Preconditions.checkNotNull(entry);
		Preconditions.checkState(entry.getType() == OAuthEntry.Type.REQUEST, "Token must be a request token");

		OAuthEntry accessEntry = new OAuthEntry(entry);

		accessEntry.setToken(UUID.randomUUID().toString());
		accessEntry.setTokenSecret(UUID.randomUUID().toString());

		accessEntry.setType(OAuthEntry.Type.ACCESS);
		accessEntry.setIssueTime(new Date());

		oauthEntries.invalidate(entry.getToken());
		oauthEntries.put(accessEntry.getToken(), accessEntry);

		return accessEntry;
	}

	// Authorize the request token for the given user id
	public void authorizeToken(OAuthEntry entry, String userId) {
		Preconditions.checkNotNull(entry);
		entry.setAuthorized(true);
		entry.setUserId(Preconditions.checkNotNull(userId));
		if (entry.isCallbackUrlSigned()) {
			entry.setCallbackToken(Crypto.getRandomDigits(CALLBACK_TOKEN_LENGTH));
		}
	}

	public void disableToken(OAuthEntry entry) {
		Preconditions.checkNotNull(entry);
		entry.setCallbackTokenAttempts(entry.getCallbackTokenAttempts() + 1);
		if (!entry.isCallbackUrlSigned() || entry.getCallbackTokenAttempts() >= CALLBACK_TOKEN_ATTEMPTS) {
			entry.setType(OAuthEntry.Type.DISABLED);
		}

		oauthEntries.put(entry.getToken(), entry);
	}

	public void removeToken(OAuthEntry entry) {
		Preconditions.checkNotNull(entry);

		oauthEntries.invalidate(entry.getToken());
	}

	public OAuth2Client getClient(String clientId) {
		for (OAuth2Client client : clients) {
			if (client.getId().equals(clientId)) {
				return client;
			}
		}
		return null;
	}

	public OAuth2Code getAuthorizationCode(String clientId, String authCode) {
		if (authCodes.containsKey(clientId)) {
			List<OAuth2Code> codes = authCodes.get(clientId);
			for (OAuth2Code code : codes) {
				if (code.getValue().equals(authCode)) {
					return code;
				}
			}
		}
		return null;
	}

	public void registerAuthorizationCode(String clientId, OAuth2Code authCode) {
		if (authCodes.containsKey(clientId)) {
			authCodes.get(clientId).add(authCode);
		} else {
			List<OAuth2Code> list = Lists.newArrayList();
			list.add(authCode);
			authCodes.put(clientId, list);
		}
	}

	public void unregisterAuthorizationCode(String clientId, String authCode) {
		if (authCodes.containsKey(clientId)) {
			List<OAuth2Code> codes = authCodes.get(clientId);
			for (OAuth2Code code : codes) {
				if (code.getValue().equals(authCode)) {
					codes.remove(code);
					return;
				}
			}
		}
		throw new RuntimeException("signature not found"); // TODO (Eric): handle error
	}

	public OAuth2Code getAccessToken(String accessToken) {
		for (String clientId : accessTokens.keySet()) {
			List<OAuth2Code> tokens = accessTokens.get(clientId);
			for (OAuth2Code token : tokens) {
				if (token.getValue().equals(accessToken)) {
					return token;
				}
			}
		}
		return null;
	}

	public void registerAccessToken(String clientId, OAuth2Code accessToken) {
		if (accessTokens.containsKey(clientId)) {
			accessTokens.get(clientId).add(accessToken);
		} else {
			List<OAuth2Code> list = Lists.newArrayList();
			list.add(accessToken);
			accessTokens.put(clientId, list);
		}
	}

	public void unregisterAccessToken(String clientId, String accessToken) {
		if (accessTokens.containsKey(clientId)) {
			List<OAuth2Code> tokens = accessTokens.get(clientId);
			for (OAuth2Code token : tokens) {
				if (token.getValue().equals(accessToken)) {
					tokens.remove(token);
					return;
				}
			}
		}
		throw new RuntimeException("access token not found"); // TODO (Eric): handle error
	}

	public OAuth2Code getRefreshToken(String refreshToken) {
		throw new RuntimeException("not yet implemented");
	}

	public void registerRefreshToken(String clientId, OAuth2Code refreshToken) {
		throw new RuntimeException("not yet implemented");
	}

	public void unregisterRefreshToken(String clientId, String refreshToken) {
		throw new RuntimeException("not yet implemented");
	}

	private void loadClients() {
		Connection conn = dbUtil.getConnection();
		try {
			String sql = "SELECT [AppID], [Name] from [ORNG.].[Apps] where OAuthSecret is not null";
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				OAuth2Client client = new OAuth2Client();
				client.setId(rs.getString(1));
				client.setTitle(rs.getString(2));
				// have no idea what this means
				client.setType(ClientType.PUBLIC);
				clients.add(client);
			}
		}
		catch (SQLException se) {
			LOG.log(Level.SEVERE, "Error reading OAuthClients ", se);
		}
		finally {
			try { conn.close(); } catch (SQLException se) {
				LOG.log(Level.SEVERE, "Error closing connection", se);
			}
		}
	}
}
