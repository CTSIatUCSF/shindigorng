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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
		// Selector for incoming time requests
		Selector acceptSelector = SelectorProvider.provider().openSelector();

		// Create a new server socket and set to non blocking mode
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		// Bind the server socket to the local host and port

		InetAddress lh = InetAddress.getLocalHost();
		InetSocketAddress isa = new InetSocketAddress(lh, port);
		ssc.socket().bind(isa);

		// Register accepts on the server socket with the selector. This
		// step tells the selector that the socket wants to be put on the
		// ready list when accept operations occur, so allowing multiplexed
		// non-blocking I/O to take place.
		ssc.register(acceptSelector, SelectionKey.OP_ACCEPT);

		// Here's where everything happens. The select method will
		// return when any operations registered above have occurred, the
		// thread has been interrupted, etc.
		while (acceptSelector.select() > 0) {
			// Someone is ready for I/O, get the ready keys
			Set<SelectionKey> readyKeys = acceptSelector.selectedKeys();
			Iterator<SelectionKey> i = readyKeys.iterator();

			// Walk through the ready keys collection and process date requests.
			while (i.hasNext()) {
				SelectionKey sk = i.next();
				i.remove();
				// The key indexes into the selector so you
				// can retrieve the socket that's ready for I/O
				ServerSocketChannel nextReady = (ServerSocketChannel) sk
						.channel();
				// Accept the date request and send back the date string
				Socket s = nextReady.accept().socket();
				BufferedReader in = new BufferedReader(new InputStreamReader(
						s.getInputStream()));
				String input = in.readLine();

				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				try {
					String token = convert(input);
					// Send back the security token
					out.print(token);

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					out.close();
				}
			}
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

	private static Map<String, String> getQueryMap(String query) throws UnsupportedEncodingException {
		String[] params = query.split("&");
		Map<String, String> map = Maps.newHashMap();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, OWNER_KEY.equals(name) || VIEWER_KEY.equals(name) ? URLDecoder.decode(value, "UTF-8") : value);
		}
		return map;
	}

}
