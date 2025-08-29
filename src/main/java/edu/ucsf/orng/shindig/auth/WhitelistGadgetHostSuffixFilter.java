package edu.ucsf.orng.shindig.auth;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Filter implementation class WhitelistGadgetHostSuffixFilter
 */
public class WhitelistGadgetHostSuffixFilter implements Filter {
	
	private String[] allowedGadgetHostSuffixes;

    /**
     * Default constructor. 
     */
    public WhitelistGadgetHostSuffixFilter() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
		// TODO Auto-generated method stub
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String gadgetHost = new URL(request.getParameter("url")).getHost().toLowerCase();
		if (allowedGadgetHostSuffixes.length == 0) {
			chain.doFilter(request, response);
			return;
		}
		for (String allowedHost : allowedGadgetHostSuffixes) {
			if (gadgetHost.endsWith(allowedHost)) {
				chain.doFilter(request, response);
				return;
			}
		}
		if (response instanceof HttpServletResponse) {
			((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN, "Unrecognized gadget not allowed");
		}
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		String allowedStr = fConfig.getInitParameter("AllowedGadgetHostSuffixes");
		allowedGadgetHostSuffixes = allowedStr != null ? allowedStr.split(",") : new String[0];
	}

}
