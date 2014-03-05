package edu.ucsf.orng.shindig.auth;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;
import net.oauth.signature.RSA_SHA1;

import java.util.ArrayList;
import java.io.IOException;
import java.util.Map;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SampleSignedRequestServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Certificate created by UCSF, set as default OAuth key in Shindig
	private final static String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
			+ "MIICGzCCAYSgAwIBAgIJAOlUynqbuhNfMA0GCSqGSIb3DQEBBQUAMBQxEjAQBgNV\n"
			+ "BAMTCW15dGVzdGtleTAeFw0xMjA5MTcxOTA1NTJaFw0xMzA5MTcxOTA1NTJaMBQx\n"
			+ "EjAQBgNVBAMTCW15dGVzdGtleTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA\n"
			+ "tD096JPHAzKEMWXdcSIYRgnCQyqOX7qwTzYrP5cBHRYrS8C1dU86IAt3pKg6RGqD\n"
			+ "JZkP/mYqRd4PLp2N/lqq7j8Z4kYpVTkbhUnG52G4cJU537iqUMrd6fOojK0lIQRL\n"
			+ "Fd+cDD7dBlfefAJrngZlv7p5b5erRsa//J6/phGqYs0CAwEAAaN1MHMwHQYDVR0O\n"
			+ "BBYEFMh+qA2rO9hsGTyjNpEKeK9Oao6NMEQGA1UdIwQ9MDuAFMh+qA2rO9hsGTyj\n"
			+ "NpEKeK9Oao6NoRikFjAUMRIwEAYDVQQDEwlteXRlc3RrZXmCCQDpVMp6m7oTXzAM\n"
			+ "BgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBAFiadsLpjJMSXbffBefld+VQ\n"
			+ "nZSStHd2bfhc3Aebz8WmpvBCn9ECneGGWNyJviPjkHD+cpqOWIQawYfiTgQv65ju\n"
			+ "AxyqScCgfRSfe8ytflFkuj8VZ49K0uW/nJdCo33GwWgi01XkfTSZzQgVGWZJMbJ7\n"
			+ "B8ruk8tHWMJ8GpETdDLo\n"
			+ "-----END CERTIFICATE-----\n";
	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		verifyFetch(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		verifyFetch(req, resp);
	}

	private void verifyFetch(HttpServletRequest request,
			HttpServletResponse resp) throws IOException, ServletException {
		resp.setContentType("text/html; charset=UTF-8");
		PrintWriter out = new PrintWriter(System.out); 

		try {
			OAuthServiceProvider provider = new OAuthServiceProvider(null,
					null, null);

			OAuthConsumer consumer = null;
			
			if (OAuth.RSA_SHA1.equals(request.getParameter("oauth_signature_method"))) {
				consumer = new OAuthConsumer(null, "localhost", null, provider);
				consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, CERTIFICATE);
				
			}
			else if (OAuth.HMAC_SHA1.equals(request.getParameter("oauth_signature_method"))) {
				consumer = new OAuthConsumer(null, "UCSF", "secretBetweenUCSFandVendor", provider);							
			}
			else {
				throw new ServletException("Unrecognized oauth_signature_method :" + request.getParameter("oauth_signature_method"));
			}

			String method = request.getMethod();
			String requestUrl = request.getRequestURL().toString();
			List<OAuth.Parameter> requestParameters = getRequestParameters(request);

			OAuthMessage message = new OAuthMessage(method, requestUrl,
					requestParameters);

			OAuthAccessor accessor = new OAuthAccessor(consumer);
			out.println("*** OAuthMessage Params:");
			out.println("URL: " + OAuthServlet.htmlEncode(message.URL));
			for (java.util.Map.Entry<String, String> param : message.getParameters()) {
				String key = param.getKey().toString();
				String value = param.getValue().toString();
				out.println();
				out.println("Param Name-->" + OAuthServlet.htmlEncode(key));
				out.println("Value-->" + OAuthServlet.htmlEncode(value));
			}
			out.println();
			out.println(" VALIDATING SIGNATURE ");
			out.println();
			OAuthValidator validator = new SimpleOAuthValidator();
			validator.validateMessage(message, accessor);
			out.println("REQUEST STATUS::OK");
			out.println();
		} catch (OAuthProblemException ope) {
			out.println();
			out.println("OAuthProblemException-->"
					+ OAuthServlet.htmlEncode(ope.getProblem()));
		} catch (Exception e) {
			out.println(e);
			System.out.println(e);
			throw new ServletException(e);
		} finally {
			out.flush();
		}
	}

	/**
	 * Constructs and returns a List of OAuth.Parameter objects, one per
	 * parameter in the passed request.
	 * 
	 * @param request
	 *            Servlet request object with methods for retrieving the full
	 *            set of parameters passed with the request
	 */
	public static List<OAuth.Parameter> getRequestParameters(
			HttpServletRequest request) {

		List<OAuth.Parameter> parameters = new ArrayList<OAuth.Parameter>();

		for (Object e : request.getParameterMap().entrySet()) {
			@SuppressWarnings("unchecked")
			Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) e;

			for (String value : entry.getValue()) {
				parameters.add(new OAuth.Parameter(entry.getKey(), value));
			}
		}

		return parameters;
	}

}
