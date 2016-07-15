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
		
		String secret = "-----BEGIN PRIVATE KEY-----\nMIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDZcS7OYlkEwWIu\nvLFf4qeKGPZuq3zbhLHc/0oogBlckwJ+7XzTGcU9tUEPiWx+lgb7V389bVoMWxYw\nhvxwX/7fKG2URRb8A22IxnlapejIOfX0nsvNJBq0d1X5O8M4RW8Dd1sD5wCedNx+\n/HCM98SZ96QG3jrQPvN6VnBTNv3GNk94B07q7eabGPZxh3Rz9gAAyE5FVQUo0T/t\nnUB6QfoxFuclS6jwO7wW5eupXk4tfQyrViwvOKUikgxncjb18uON8HdBbjA9HLW5\nC7+FqQpbb/XSQIWM4bMVGIu2tHorKykueO3JCbrUbndG5EIJd4E02t3IyzJkfDxz\nMcJDfFPXAgMBAAECggEAGQy+9avC0ZdttjCqxn8YCgLCMRG0ep2Y1/rZEj1frpiJ\nCxJU9QTdAjTlX+LXCuZBu6bi07LLUu0Ta1fBsTh95juEFDa5ZSMH9V/YDydZ3+c9\nfIbmt6VXJj8xOls2LD9jgKS5aYOtQJP56u8uEC9jCNHMpbXoVKFL72YR3qRRLApT\nLS3eFjhLq2/cPaivs4nVrGnN3UsO54pd9gcemO9PxedlIk8E94rfuQaySeGrVx0P\n+gUi7i+AixgtrQHrbzc2uGMsGPKw81xt8S4WQUk5BUw1LeONeTk7wju3oIo7drPa\nZJTIs83cWJj2unFJfK0btrk/J17u+sXuurr/Gkl8kQKBgQD+c+ZJO3NecuXE5dWv\nxbdL46E1qXqfd6BqS3sgYX7hIX0d7BtMagzrr/pqXAq8ul8eWVDV+4H2F0SBW2kb\niO7YFZvbLWZE9DiWbuhsmwLuJh0mD/GbsnoKUwQLwuZsMP4I1m0jMyMenDQC3Jhu\n9A0wyZJQCIoR0/GzNWUH50kTXQKBgQDaw6t05f0UIqe1u4pBC4F1t1AdUZCbuFYD\nh8jcNmZwbsXUjvgZknZzf9yKkrPBb5OwGQTUbYt2D0eMomTpv0C9s0PX0Z0dVIIn\nX25HNLhXh0UHbnbyGablmxq+VUJbQRDPfoZbSCqOKp76kpPYk+/lRxsN5kugZ6+U\naIg9rGekwwKBgQDijXDZfTxQOL7JW9WxhbmYsRjE/Zv3RfynB4OeJhBhBH8w2BrK\nI5ZTdsDgWAkbVlkAXYeyrhdddAQDpM7lvDrkXAED2d03wbZsgl0g7bdjML49OG7n\nMLamYJm2mxar98cHOVu8vngjdfJ9Jcn26AaijKZf6ep8yxdld0H4En6m5QKBgQC9\nmnNjKOZ06ihTIU0NXQyCWzPrhUMBQ/4Ap1IWUmvsrIV32cX8W+2f40ThMY7sa2kk\najq+ZodF9q47nJA761AQWmvFhN5YimMX/uUUgAP5kLrqWvpqkIMPY6QFLN4LO1R3\nkiCewuhnOVd6s0nCn01/eWZgYKZRSyxpQ0Q5t9BIgwKBgQDR3B8QNKv9YagsxvOf\nR7fs4DJ6Zq/3NZ0vcxudVp+Ia7rFqJtl2D+3xL2XU3f0rzkFgljLPehnXBABHFZC\nojbu9JFi5kwh/A0YW2lmnlEafclQc0iGDeizl4f2M7qFPMuB82fkKMcVH742iZqZ\nyF///boU+nEt6O5xIuATpJkK+Q\u003d\u003d\n-----END PRIVATE KEY-----\n";
		//String secret = "-----BEGIN PRIVATE KEY-----\nMIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ1oa+Nk9NqCjZ1E\nVBMKYxXrWvxXe8B+ZalKd696GQlFy06CaykzVkzPvKMl77cfC4UY9NvvuL4/H2QC\nwDPxxRf0znAgbWMTUKewvUM6lak6Re0DOfIoCM0Dy/YYHdjiHaylY2tM4VxGSGs/\nn9O4UaeABsDAOvaO4E86xwMc9rmNAgMBAAECgYAXRW3V44IToAOBwa1QV1PI1M/R\nyLlB/y3Wdmz1Y266IThqdfuTzsQufPPdhulGwG86kTL6JRnB/qEMbx+tTkweAC6O\nxjWsprXDjut2GoTvBafWEEmgUT4qZaHCoK3IBBFSw+lQJT3WqUJ6trudDTBTNlSw\n4iz9CMyeOb+cIlcgwQJBAM+p0JrOQD66FXYuCS/jh0JCNXk2qv0bptboG80ZYLmC\n1W7K2SNqzdkis7UjjegCW9dDlC282yJOxBKlHC8xTH0CQQDCC//PD+hZWKh1MFG+\nGSm/ZYLZ10vZCF99SYRAmLQIwfnERGPNujLR1jb3n/lhg3fEI0pn53isBCkLEqjr\nGH5RAkBddYi70y1YzbjQ+kEKO3VpXZDhX7gut54rxESW1uAQPZC/Dy4QYYYJPjPw\nxvKbw0wAIpryxrc8xoQ5+/MmTiKlAkBlfi0wOhvHD44crUcz7KlfSFLmaatsOurm\n9trLhpMzuXFP7I1e/zKxeh+J6Qxgqoir9+Fk8za0kgB9oCblwAvhAkAZE3aEB3Nj\nGA83DPDRTshRjJZgZhz0oFui9hYDOGJEwUZzdVNYPOa12GyRGa/Aj2aLDpcaD/Yu\nucgzoB4lyQku\n-----END PRIVATE KEY-----\n";
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
