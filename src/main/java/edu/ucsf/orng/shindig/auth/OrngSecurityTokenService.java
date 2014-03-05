package edu.ucsf.orng.shindig.auth;

import static org.apache.shindig.auth.AbstractSecurityToken.Keys.APP_URL;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.OWNER;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.VIEWER;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.CONTAINER;
import static org.apache.shindig.auth.AnonymousSecurityToken.ANONYMOUS_ID;

import java.io.BufferedReader;
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
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class OrngSecurityTokenService implements Runnable {

	private static final Logger LOG = Logger.getLogger(OrngSecurityTokenService.class.getName());	
	
	private SecurityTokenCodec securityTokenCodec;
	private final int port;
	
	Map<String, BlobCrypter> crypters = Maps.newHashMap();

	@Inject
	public OrngSecurityTokenService(ContainerConfig config, SecurityTokenCodec codec, @Named("orng.tokenservice.port") int port) {
		this.securityTokenCodec = codec;
		this.port = port;
	    for (String container : config.getContainers()) {
	        String key = config.getString(container, BlobCrypterSecurityTokenCodec.SECURITY_TOKEN_KEY);
	        if (key != null) {
	        	BlobCrypter crypter = new BasicBlobCrypter(key);
	        	crypters.put(container, crypter);
	        }
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

	String convert(String input) throws UnsupportedEncodingException, SecurityTokenException, BlobCrypterException {
	    LOG.log(Level.INFO, "Received " + input + ": length = " + input.length());
		Map<String, String> tokenParameters = getQueryMap(input);
	      SecurityToken token = new BlobCrypterSecurityToken(tokenParameters.get(CONTAINER.getKey()), "*", "0", tokenParameters);
		// we probably will need to add more stuff to this
		//SecurityToken st = securityTokenCodec.createToken(tokenParameters);
		return securityTokenCodec.encodeToken(token);
		// FROM BlobCrypterSecurityTokenCodecTest
//		Map<String, String> values = new HashMap<String, String>();
//		values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
//		values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
//		values.put(Keys.OWNER.getKey(), "owner");
//		values.put(Keys.VIEWER.getKey(), "viewer");
//		values.put(Keys.TRUSTED_JSON.getKey(), "trusted");
//
//		BlobCrypterSecurityToken t = new BlobCrypterSecurityToken("container", null, null, values);
//		String encrypted = t.getContainer() + ":" + getBlobCrypter(getContainerKey("container")).wrap(t.toMap());
	}

	private Map<String, String> getQueryMap(String query) throws UnsupportedEncodingException {
		String[] params = query.split("&");
		Map<String, String> map = Maps.newHashMap();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, 
					OWNER.getKey().equals(name) || VIEWER.getKey().equals(name) || APP_URL.getKey().equals(name) ? URLDecoder.decode(value, "UTF-8") : value);
		}
		return map;
	}

}
