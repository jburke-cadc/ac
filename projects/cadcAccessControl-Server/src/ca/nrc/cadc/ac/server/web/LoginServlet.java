/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2014.                            (c) 2014.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  $Revision: 4 $
 *
 ************************************************************************
 */
package ca.nrc.cadc.ac.server.web;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ca.nrc.cadc.ac.Group;
import ca.nrc.cadc.ac.Role;
import ca.nrc.cadc.ac.UserNotFoundException;
import ca.nrc.cadc.ac.server.GroupDetailSelector;
import ca.nrc.cadc.ac.server.UserPersistence;
import ca.nrc.cadc.ac.server.ldap.LdapGroupPersistence;
import ca.nrc.cadc.ac.server.ldap.LdapUserPersistence;
import ca.nrc.cadc.auth.AuthenticatorImpl;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.SSOCookieManager;
import ca.nrc.cadc.log.ServletLogInfo;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.util.StringUtil;

@SuppressWarnings("serial")
public class LoginServlet extends HttpServlet
{
    private static final Logger log = Logger.getLogger(LoginServlet.class);
    private static final String CONTENT_TYPE = "text/plain";
    // " as " - delimiter use for proxy user authentication
    public static final String PROXY_USER_DELIM = "\\s[aA][sS]\\s";
    String proxyGroup; // only users in this group can impersonate other users
    String nonImpersonGroup; // users in this group cannot be impersonated
    
    private static final String PROXY_ACCESS = "Proxy user access: ";
    
    
    @Override
    public void init(final ServletConfig config) throws ServletException
    {
        super.init(config);

        try
        {
            this.proxyGroup = config.getInitParameter(
                    LoginServlet.class.getName() + ".proxyGroup");
            log.info("proxyGroup: " + proxyGroup);
            this.nonImpersonGroup = config.getInitParameter(
                    LoginServlet.class.getName() + ".nonImpersonGroup");
            log.info("nonImpersonGroup: " + nonImpersonGroup);
        }
        catch(Exception ex)
        {
            log.error("failed to init: " + ex);
        }
    }
    /**
     * Attempt to login for userid/password.
     */
	@SuppressWarnings("rawtypes")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        long start = System.currentTimeMillis();
        ServletLogInfo logInfo = new ServletLogInfo(request);
        try
        {
            log.info(logInfo.start());
            String userID = request.getParameter("username").trim();
            String proxyUser = null;
            String[] fields = userID.split(PROXY_USER_DELIM);
            if (fields.length == 2 )
            {
                proxyUser = fields[0].trim();
                userID = fields[1].trim();
                checkCanImpersonate(userID, proxyUser);
            }
            String password = request.getParameter("password");
            UserPersistence up = new LdapUserPersistence();
            if (StringUtil.hasText(userID))
            {
                if (StringUtil.hasText(password))
                {
                    if ((StringUtil.hasText(proxyUser) && 
                            up.doLogin(proxyUser, password)) ||
                        (!StringUtil.hasText(proxyUser) &&
                                up.doLogin(userID, password)))   
                    {
	            	    String token = 
	            	            new SSOCookieManager().generate(
	            	                    new HttpPrincipal(userID, proxyUser));
	            	    response.setContentType(CONTENT_TYPE);
	            	    response.setContentLength(token.length());
	            	    response.getWriter().write(token);
                	}
                }
                else
                {
                	throw new IllegalArgumentException("Missing password");
                }
            }
            else
            {
            	throw new IllegalArgumentException("Missing userid");
            }
        }
        catch (IllegalArgumentException e)
        {
            String msg = e.getMessage();
            if (msg.startsWith(PROXY_ACCESS))
            {
                log.warn(msg, e);
            }
            else
            {
                log.debug(msg, e);
            }
            logInfo.setMessage(msg);
    	    response.setContentType(CONTENT_TYPE);
            response.getWriter().write(msg);
            response.setStatus(400);
        }
        catch (AccessControlException e)
        {
            log.debug(e.getMessage(), e);
            String message = e.getMessage();
            logInfo.setMessage(message);
    	    response.setContentType(CONTENT_TYPE);
            response.getWriter().write(message);
            response.setStatus(401);
        }
        catch (Throwable t)
        {
            String message = "Internal Server Error: " + t.getMessage();
            log.error(message, t);
            logInfo.setSuccess(false);
            logInfo.setMessage(message);
    	    response.setContentType(CONTENT_TYPE);
            response.getWriter().write(message);
            response.setStatus(500);
        }
        finally
        {
            logInfo.setElapsedTime(System.currentTimeMillis() - start);
            log.info(logInfo.end());
        }
    }
	
	/**
	 * Checks if user can impersonate another user
	 */
    protected void checkCanImpersonate(final String userID, final String proxyUser)
            throws AccessControlException, UserNotFoundException,
            TransientException, Throwable
    {
        final LdapGroupPersistence<HttpPrincipal> gp = 
                getLdapGroupPersistence();
        
        AuthenticatorImpl ai = new AuthenticatorImpl();
        Subject proxySubject = new Subject();
        proxySubject.getPrincipals().add(new HttpPrincipal(proxyUser));
        ai.augmentSubject(proxySubject);
        try
        {
            Subject.doAs(proxySubject, new PrivilegedExceptionAction<Object>()
            {
                @Override
                public Object run() throws Exception
                {
                    
                    if (gp.getGroups(new HttpPrincipal(proxyUser), Role.MEMBER,
                            proxyGroup).size() == 0)
                    {
                        throw new AccessControlException(PROXY_ACCESS
                                + proxyUser + " as " + userID
                                + " failed - not allowed to impersonate ("
                                + proxyUser + " not in " + proxyGroup
                                + " group)");
                    }
                    return null;
                }
            });

            Subject userSubject = new Subject();
            userSubject.getPrincipals().add(new HttpPrincipal(userID));
            ai.augmentSubject(userSubject);
            Subject.doAs(userSubject, new PrivilegedExceptionAction<Object>()
            {
                @Override
                public Object run() throws Exception
                {
                    if (gp.getGroups(new HttpPrincipal(userID), Role.MEMBER,
                            nonImpersonGroup).size() != 0)
                    {
                        throw new AccessControlException(PROXY_ACCESS
                                + proxyUser + " as " + userID
                                + " failed - non impersonable (" + userID
                                + " in " + nonImpersonGroup + " group)");
                    }
                    return null;
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            Throwable cause = e.getCause();
            if (cause != null)
            {
                throw cause;
            }
            Exception exception = e.getException();
            if (exception != null)
            {
                throw exception;
            }
            throw e;
        }
    }
    
    protected LdapGroupPersistence<HttpPrincipal> getLdapGroupPersistence()
    {
        LdapGroupPersistence<HttpPrincipal> gp = new LdapGroupPersistence<HttpPrincipal>();
        gp.setDetailSelector(new GroupDetailSelector()
        {
            @Override
            public boolean isDetailedSearch(Group g, Role r)
            {
                return false;
            }
        });
        return gp;
    }
}
