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
/*	private final static String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
			+ "MIIDHDCCAoWgAwIBAgIJAMbTCksqLiWeMA0GCSqGSIb3DQEBBQUAMGgxCzAJBgNV\n"
			+ "BAYTAlVTMQswCQYDVQQIEwJDQTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIG\n"
			+ "A1UEChMLR29vZ2xlIEluYy4xDjAMBgNVBAsTBU9ya3V0MQ4wDAYDVQQDEwVscnlh\n"
			+ "bjAeFw0wODAxMDgxOTE1MjdaFw0wOTAxMDcxOTE1MjdaMGgxCzAJBgNVBAYTAlVT\n"
			+ "MQswCQYDVQQIEwJDQTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIGA1UEChML\n"
			+ "R29vZ2xlIEluYy4xDjAMBgNVBAsTBU9ya3V0MQ4wDAYDVQQDEwVscnlhbjCBnzAN\n"
			+ "BgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAseBXZ4NDhm24nX3sJRiZJhvy9eDZX12G\n"
			+ "j4HWAMmhAcnm2iBgYpAigwhVHtOs+ZIUIdzQHvHeNd0ydc1Jg8e+C+Mlzo38OvaG\n"
			+ "D3qwvzJ0LNn7L80c0XVrvEALdD9zrO+0XSZpTK9PJrl2W59lZlJFUk3pV+jFR8NY\n"
			+ "eB/fto7AVtECAwEAAaOBzTCByjAdBgNVHQ4EFgQUv7TZGZaI+FifzjpTVjtPHSvb\n"
			+ "XqUwgZoGA1UdIwSBkjCBj4AUv7TZGZaI+FifzjpTVjtPHSvbXqWhbKRqMGgxCzAJ\n"
			+ "BgNVBAYTAlVTMQswCQYDVQQIEwJDQTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEU\n"
			+ "MBIGA1UEChMLR29vZ2xlIEluYy4xDjAMBgNVBAsTBU9ya3V0MQ4wDAYDVQQDEwVs\n"
			+ "cnlhboIJAMbTCksqLiWeMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEA\n"
			+ "CETnhlEnCJVDXoEtSSwUBLP/147sqiu9a4TNqchTHJObwTwDPUMaU6XIs2OTMmFu\n"
			+ "GeIYpkHXzTa9Q6IKlc7Bt2xkSeY3siRWCxvZekMxPvv7YTcnaVlZzHrVfAzqNsTG\n"
			+ "P3J//C0j+8JWg6G+zuo5k7pNRKDY76GxxHPYamdLfwk=\n"
			+ "-----END CERTIFICATE-----";*/
	
	/* new one
	private final static String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
			+ "MIICGzCCAYSgAwIBAgIJAOUtSEHMpRkQMA0GCSqGSIb3DQEBBQUAMBQxEjAQBgNV\n"
			+ "BAMTCW15dGVzdGtleTAeFw0xMjA5MTMyMzU2NDRaFw0xMzA5MTMyMzU2NDRaMBQx\n"
			+ "EjAQBgNVBAMTCW15dGVzdGtleTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA\n"
			+ "31Um1xlZNiVNtUTR743vu0mzUGXPK5MENmLfTWAZE/XqU64UTR9muMy3wN1mKjGu\n"
			+ "zxEtn4p+/NsoD11ibWoouT3OMChamxIV9IIoYXkEQGmuphfLqOnjmTDgirHKNz0Y\n"
			+ "lHsHu5w2oYyqmZGBLPz4FxHKb93d4a8tzNOyTc6mQtECAwEAAaN1MHMwHQYDVR0O\n"
			+ "BBYEFCTymvOah2j7ysTqxZrOyRUcoiVTMEQGA1UdIwQ9MDuAFCTymvOah2j7ysTq\n"
			+ "xZrOyRUcoiVToRikFjAUMRIwEAYDVQQDEwlteXRlc3RrZXmCCQDlLUhBzKUZEDAM\n"
			+ "BgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBAHiHzh/+u+TkXhx4G+HGDcX9\n"
			+ "f+1JJDsdRuTG4ZStbSilqgkXC1TJILwDsO8+JVBllw9sXk1pydfZmdK/8UYhP/ls\n"
			+ "TfWcMs4ZxlOkq+v77qpOwat6R1O2hF3RPBmYkB+f3AiIMtnMTB500ZtRyVuPOR60\n"
			+ "chMmTsMh7FlUPUE/IFBS\n"
			+ "-----END CERTIFICATE-----";*/

	private final static String CERTIFICATE = /*"	-----BEGIN RSA PRIVATE KEY-----\n"
			+ "MIICWwIBAAKBgQC0PT3ok8cDMoQxZd1xIhhGCcJDKo5furBPNis/lwEdFitLwLV1\n"
			+ "TzogC3ekqDpEaoMlmQ/+ZipF3g8unY3+WqruPxniRilVORuFScbnYbhwlTnfuKpQ\n"
			+ "yt3p86iMrSUhBEsV35wMPt0GV958AmueBmW/unlvl6tGxr/8nr+mEapizQIDAQAB\n"
			+ "AoGAcapVioD3dqq69zQYbKplyHWLDzSdSP3BBpNQvu+KAj/i2gkT5oEqVN8meq6k\n"
			+ "4FSTlHhsv7DKY/lgdbNiws+HD11+GZXdYWczXaY9en7jH+NNTo7X38VcyOKdeNOl\n"
			+ "3zvA0gjxNUGjNEYINfdOc4hFAO/SQgD04hQMVdDHCf9lIkECQQDhDdWvPPPIZ9k4\n"
			+ "Sb5XTlmogpIae8Hi0vODTvzBeRZgZjrXTwE6CAYui7i6YMmxpAv/yBo5T+d70kL6\n"
			+ "7kurW3jJAkEAzQXfi/EH27rwTexmnqDA39MzudEpGxQN0aTMbfQMUJ7wQTRD/7aA\n"
			+ "brfpYLeAe+ZTgX1N9yJBt27yO/aMvf8f5QJARuC0i5wGqvcJ0lBnQdfLJOb6XJzd\n"
			+ "UzJcvt4BfG1GPtXzchvPpxcf20jlxMz2uJuRq9y5ZZNks/pkXeLusej9AQJAM1lL\n"
			+ "OeNuUmwpj3qr4QLmC6j8BYgLQYruQxmBUfCTvQVxqwMKHNt6o0BQpTaQaXewZngZ\n"
			+ "tNHRn72b0cTYTyW8uQJABMh4+FmT8oqGuehTjIV//neP5Y/9mEkxuyYjRWt1TPTr\n"
			+ "ELaeKkNXQk+txjJkNRsmax7J34R43iT9mvhLL4n7xQ==\n"
			+ "-----END RSA PRIVATE KEY-----\n"
			+ */
			"-----BEGIN CERTIFICATE-----\n"
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
		PrintWriter out = new PrintWriter(System.out); //resp.getWriter();

		try {
			OAuthServiceProvider provider = new OAuthServiceProvider(null,
					null, null);
			OAuthConsumer consumer = new OAuthConsumer(null, "ucsf.edu", null,
					provider);
			consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, CERTIFICATE);

			String method = request.getMethod();
			String requestUrl = getRequestUrl(request);
			List<OAuth.Parameter> requestParameters = getRequestParameters(request);

			OAuthMessage message = new OAuthMessage(method, requestUrl,
					requestParameters);

			OAuthAccessor accessor = new OAuthAccessor(consumer);
			out.println("*** OAuthMessage Params:");
			out.println("URL: " + OAuthServlet.htmlEncode(message.URL));
			for (java.util.Map.Entry param : message.getParameters()) {
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
	 * Constructs and returns the full URL associated with the passed request
	 * object.
	 * 
	 * @param request
	 *            Servlet request object with methods for retrieving the various
	 *            components of the request URL
	 */
	public static String getRequestUrl(HttpServletRequest request) {
		if (true) {
			return request.getRequestURL().toString();
		}
		StringBuilder requestUrl = new StringBuilder();
		String scheme = request.getScheme();
		int port = request.getLocalPort();

		requestUrl.append(scheme);
		requestUrl.append("://");
		requestUrl.append(request.getServerName());

		if ((scheme.equals("http") && port != 80)
				|| (scheme.equals("https") && port != 443)) {
			requestUrl.append(":");
			requestUrl.append(port);
		}

		requestUrl.append(request.getContextPath());
		requestUrl.append(request.getServletPath());

		return requestUrl.toString();
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
			Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) e;

			for (String value : entry.getValue()) {
				parameters.add(new OAuth.Parameter(entry.getKey(), value));
			}
		}

		return parameters;
	}

}
