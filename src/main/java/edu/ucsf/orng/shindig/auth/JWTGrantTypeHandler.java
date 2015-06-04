package edu.ucsf.orng.shindig.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;
import org.apache.shindig.gadgets.oauth2.handler.GrantRequestHandler;
import org.json.JSONException;
import org.json.JSONObject;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

public class JWTGrantTypeHandler implements GrantRequestHandler {

	private static final Logger LOG = Logger.getLogger(JWTGrantTypeHandler.class.getName());	
	
	public HttpRequest getAuthorizationRequest(OAuth2Accessor accessor,
			String completeAuthorizationUrl) throws OAuth2RequestException {
	    final HttpRequest request = new HttpRequest(Uri.parse(completeAuthorizationUrl));
	    request.setMethod("POST");
	    request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
	    request.setSecurityToken(new AnonymousSecurityToken("", 0L, accessor.getGadgetUri()));
	    try {
		    String body = "grant_type=" + URLEncoder.encode(getGrantType(), "UTF-8")
		    		+ "&assertion=" + getAssertion(accessor);
		    
		    request.setPostBody(body.getBytes("UTF8"));
	    }
	    catch (Exception e) {
	    	throw new OAuth2RequestException(OAuth2Error.UNKNOWN_PROBLEM, 
	    			"Could not create request for " + accessor, e);
	    }
		return request;
	}
	
	private static String getAssertion(OAuth2Accessor accessor) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException, JSONException, JOSEException {		
		String secret = new String(accessor.getClientSecret(), "UTF8");
        String subsecret = secret.replace("-----BEGIN PRIVATE KEY-----", "").
			replace("-----END PRIVATE KEY-----", "").
			replace("\n", "");
        LOG.info(subsecret);
		KeySpec spec = new PKCS8EncodedKeySpec(Base64.decodeBase64(subsecret));
        KeyFactory kf = KeyFactory.getInstance("RSA");
		JWSSigner signer = new RSASSASigner((RSAPrivateKey) kf.generatePrivate(spec));

		// Prepare JWS object with simple string as payload
		JWSHeader header = new JWSHeader(JWSAlgorithm.RS256, JOSEObjectType.JWT, null, null, null, null, null, null, null, null, null, null, null);
		JWSObject jwsObject = new JWSObject(header, new Payload(getJWTClaim(accessor)));
		
		// Compute the RSA signature
		jwsObject.sign(signer);
		
		return jwsObject.serialize();
	}
	
	private static String getJWTClaim(OAuth2Accessor accessor) throws JSONException, UnsupportedEncodingException {
		JSONObject retval = new JSONObject();
		retval.put("iss", accessor.getClientId());
		retval.put("scope", accessor.getScope());
		retval.put("aud", accessor.getTokenUrl());
		retval.put("exp", System.currentTimeMillis()/1000 + 30*60); // add 30 minutes in seconds
		retval.put("iat", System.currentTimeMillis()/1000);
		LOG.log(Level.INFO, "JWTClaim = " + retval.toString());
		return retval.toString();
	}
	
	public String getCompleteUrl(OAuth2Accessor accessor)
			throws OAuth2RequestException {
		return accessor.getTokenUrl();
	}

	public String getGrantType() {
		// From https://developers.google.com/identity/protocols/OAuth2ServiceAccount
		return "urn:ietf:params:oauth:grant-type:jwt-bearer";
	}

	public boolean isAuthorizationEndpointResponse() {
		return false;
	}

	public boolean isRedirectRequired() {
		return false;
	}

	public boolean isTokenEndpointResponse() {
		return true;
	}
	
	public static void main(String[] args) {
		
		String secret = "-----BEGIN PRIVATE KEY-----\nMIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ1oa+Nk9NqCjZ1E\nVBMKYxXrWvxXe8B+ZalKd696GQlFy06CaykzVkzPvKMl77cfC4UY9NvvuL4/H2QC\nwDPxxRf0znAgbWMTUKewvUM6lak6Re0DOfIoCM0Dy/YYHdjiHaylY2tM4VxGSGs/\nn9O4UaeABsDAOvaO4E86xwMc9rmNAgMBAAECgYAXRW3V44IToAOBwa1QV1PI1M/R\nyLlB/y3Wdmz1Y266IThqdfuTzsQufPPdhulGwG86kTL6JRnB/qEMbx+tTkweAC6O\nxjWsprXDjut2GoTvBafWEEmgUT4qZaHCoK3IBBFSw+lQJT3WqUJ6trudDTBTNlSw\n4iz9CMyeOb+cIlcgwQJBAM+p0JrOQD66FXYuCS/jh0JCNXk2qv0bptboG80ZYLmC\n1W7K2SNqzdkis7UjjegCW9dDlC282yJOxBKlHC8xTH0CQQDCC//PD+hZWKh1MFG+\nGSm/ZYLZ10vZCF99SYRAmLQIwfnERGPNujLR1jb3n/lhg3fEI0pn53isBCkLEqjr\nGH5RAkBddYi70y1YzbjQ+kEKO3VpXZDhX7gut54rxESW1uAQPZC/Dy4QYYYJPjPw\nxvKbw0wAIpryxrc8xoQ5+/MmTiKlAkBlfi0wOhvHD44crUcz7KlfSFLmaatsOurm\n9trLhpMzuXFP7I1e/zKxeh+J6Qxgqoir9+Fk8za0kgB9oCblwAvhAkAZE3aEB3Nj\nGA83DPDRTshRjJZgZhz0oFui9hYDOGJEwUZzdVNYPOa12GyRGa/Aj2aLDpcaD/Yu\nucgzoB4lyQku\n-----END PRIVATE KEY-----\n";
		//String subsecret = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ1oa+Nk9NqCjZ1E\nVBMKYxXrWvxXe8B+ZalKd696GQlFy06CaykzVkzPvKMl77cfC4UY9NvvuL4/H2QCwDPxxRf0znAgbWMTUKewvUM6lak6Re0DOfIoCM0Dy/YYHdjiHaylY2tM4VxGSGs/\nn9O4UaeABsDAOvaO4E86xwMc9rmNAgMBAAECgYAXRW3V44IToAOBwa1QV1PI1M/RyLlB/y3Wdmz1Y266IThqdfuTzsQufPPdhulGwG86kTL6JRnB/qEMbx+tTkweAC6O\nxjWsprXDjut2GoTvBafWEEmgUT4qZaHCoK3IBBFSw+lQJT3WqUJ6trudDTBTNlSw4iz9CMyeOb+cIlcgwQJBAM+p0JrOQD66FXYuCS/jh0JCNXk2qv0bptboG80ZYLmC\n1W7K2SNqzdkis7UjjegCW9dDlC282yJOxBKlHC8xTH0CQQDCC//PD+hZWKh1MFG+GSm/ZYLZ10vZCF99SYRAmLQIwfnERGPNujLR1jb3n/lhg3fEI0pn53isBCkLEqjr\nGH5RAkBddYi70y1YzbjQ+kEKO3VpXZDhX7gut54rxESW1uAQPZC/Dy4QYYYJPjPwxvKbw0wAIpryxrc8xoQ5+/MmTiKlAkBlfi0wOhvHD44crUcz7KlfSFLmaatsOurm\n9trLhpMzuXFP7I1e/zKxeh+J6Qxgqoir9+Fk8za0kgB9oCblwAvhAkAZE3aEB3NjGA83DPDRTshRjJZgZhz0oFui9hYDOGJEwUZzdVNYPOa12GyRGa/Aj2aLDpcaD/Yu\nucgzoB4lyQku";
        String subsecret = secret.replace("-----BEGIN PRIVATE KEY-----", "").
        		replace("-----END PRIVATE KEY-----", "").
        		replace("\n", "");
		try {
	        KeySpec spec = new PKCS8EncodedKeySpec(Base64.decodeBase64(subsecret));
	        KeyFactory kf = KeyFactory.getInstance("RSA");
			JWSSigner signer = new RSASSASigner((RSAPrivateKey) kf.generatePrivate(spec));

			// Prepare JWS object with simple string as payload
			JWSHeader header = new JWSHeader(JWSAlgorithm.RS256, JOSEObjectType.JWT, null, null, null, null, null, null, null, null, null, null, null);
			JWSObject jwsObject = new JWSObject(header, new Payload("In RSA we trust!"));

			// Compute the RSA signature
			jwsObject.sign(signer);

			// To serialize to compact form, produces something like
			// eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
			// mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
			// maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
			// -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
			String s = jwsObject.serialize();
		    // Do something useful with your JWS
		    System.out.println(s);
		    System.out.println(header);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


}
