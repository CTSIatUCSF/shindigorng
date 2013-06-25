package edu.ucsf.orng.shindig.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.auth.BlobCrypterSecurityTokenCodec;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class OrngCrypterSecurityTokenCodec extends
		BlobCrypterSecurityTokenCodec implements Runnable {

	private static final Logger LOG = Logger.getLogger(OrngCrypterSecurityTokenCodec.class.getName());	
	
	private final int port;

	protected static final String CONTAINER_KEY = "c";
	protected static final String OWNER_KEY = "o";
	protected static final String VIEWER_KEY = "v";
	protected static final String GADGET_KEY = "g";

	@Inject
	public OrngCrypterSecurityTokenCodec(ContainerConfig config,
			@Named("orng.tokenservice.port") int port, @Named("orng.securityTokenKeyFile") String securityTokenKeyFile) {
		super(config);
		this.port = port;
		try {
			for (String container : config.getContainers()) {
				BlobCrypter crypter = loadCrypterFromFile(new File(securityTokenKeyFile));
				crypters.put(container, crypter);
			}
		} catch (IOException e) {
			// Someone specified securityTokenKeyFile, but we couldn't load the
			// key. That merits killing
			// the server.
			throw new RuntimeException(e);
		}
		
		// start listening for connections
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();	
	}

	public void run() {
	      LOG.log(Level.INFO, "Waiting for client message...");
		//
		// The server do a loop here to accept all connection initiated by the
		// client application.
		//

		try {
			acceptConnections();
		} catch (Exception e) {
			e.printStackTrace();
		}

	      LOG.log(Level.INFO, "Shutting down secure token service");
	}

	// Accept connections for current time. Lazy Exception thrown.
	private void acceptConnections() throws Exception {
		// Create a new server socket and set to non blocking mode
		ServerSocketChannel ssc = ServerSocketChannel.open();
		// Bind the server socket to the local host and port

		InetAddress lh = InetAddress.getLocalHost();
		InetSocketAddress isa = new InetSocketAddress(lh, port);
		ssc.socket().bind(isa);
		ssc.configureBlocking(true);

		// Here's where everything happens. The select method will
		// return when any operations registered above have occurred, the
		// thread has been interrupted, etc.
		while (true) {
			final Socket s = ssc.accept().socket();
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {						
						// we expect exceptions here
						BufferedReader in = new BufferedReader(new InputStreamReader(
								s.getInputStream()));
						PrintWriter out = new PrintWriter(s.getOutputStream(), true);
						String input = in.readLine();
						while (input != null) {
							String token = convert(input);
							// Send back the security token
							out.println(token);
							out.flush();
							input = in.readLine();
						}
						in.close();
						out.close();
					} catch (IOException e) {
						LOG.log(Level.INFO, "Socket Exception :" + e.getMessage());
					} catch (Exception e) {
						LOG.log(Level.WARNING, e.getMessage(), e);
					}
				}
			});
			thread.run();
		}
	}

	String convert(String input) throws BlobCrypterException, UnsupportedEncodingException {
	    LOG.log(Level.INFO, "Received " + input + ": length = " + input.length());
		Map<String, String> tokenParams = getQueryMap(input);

		String container = tokenParams.containsKey(CONTAINER_KEY) ? tokenParams
				.get(CONTAINER_KEY) : "default";

		BlobCrypterSecurityToken st = new BlobCrypterSecurityToken(
				crypters.get(container), container, domains.get(container));
		st.setViewerId(tokenParams.get(VIEWER_KEY));
		st.setOwnerId(tokenParams.get(OWNER_KEY));
		st.setAppUrl(tokenParams.get(GADGET_KEY));

		String token = Utf8UrlCoder.encode(st.encrypt());

		// Send back the security token
	      LOG.log(Level.INFO, "Returning " + token);
		return token;
	}

	private Map<String, String> getQueryMap(String query) throws UnsupportedEncodingException {
		String[] params = query.split("&");
		Map<String, String> map = Maps.newHashMap();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, 
					OWNER_KEY.equals(name) || VIEWER_KEY.equals(name) || GADGET_KEY.equals(name) ? URLDecoder.decode(value, "UTF-8") : value);
		}
		return map;
	}

}
