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
package ca.nrc.cadc.ac.server.web.users;

import ca.nrc.cadc.ac.UserNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.util.Log4jInit;

import java.io.*;
import java.security.AccessControlException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static org.easymock.EasyMock.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author jburke
 */
public class AbstractUserActionTest
{
    private final static Logger log = Logger.getLogger(AbstractUserActionTest.class);

    @BeforeClass
    public static void setUpClass()
    {
        Log4jInit.setLevel("ca.nrc.cadc.ac", Level.INFO);
    }

    @Test
    public void testDoActionAccessControlException() throws Exception
    {
        String message = "Permission Denied";
        int responseCode = 403;
        Exception e = new AccessControlException("");
        testDoAction(message, responseCode, e);
    }

    @Test
    public void testDoActionIllegalArgumentException() throws Exception
    {
        String message = "message";
        int responseCode = 400;
        Exception e = new IllegalArgumentException("message");
        testDoAction(message, responseCode, e);
    }

    @Test
    public void testDoActionUserNotFoundException() throws Exception
    {
        String message = "User not found: foo";
        int responseCode = 404;
        Exception e = new UserNotFoundException("foo");
        testDoAction(message, responseCode, e);
    }

    @Test
    public void testDoActionUnsupportedOperationException() throws Exception
    {
        String message = "Not yet implemented.";
        int responseCode = 501;
        Exception e = new UnsupportedOperationException();
        testDoAction(message, responseCode, e);
    }

    @Test
    public void testDoActionTransientException() throws Exception
    {
        HttpServletResponse response = createMock(HttpServletResponse.class);
        expect(response.isCommitted()).andReturn(Boolean.FALSE);
        response.setContentType("text/plain");
        expectLastCall().once();
        expect(response.getWriter())
                .andReturn(new PrintWriter(new StringWriter())).once();

        response.setStatus(503);
        expectLastCall().once();
        replay(response);

        UsersActionImpl action = new UsersActionImpl();
        action.setException(new TransientException("foo"));
        action.doAction();
    }

    private void testDoAction(String message, int responseCode, Exception e)
            throws Exception
    {
        HttpServletResponse response =
                createMock(HttpServletResponse.class);
        expect(response.isCommitted()).andReturn(Boolean.FALSE);
        response.setContentType("text/plain");
        expectLastCall().once();
        expect(response.getWriter())
                .andReturn(new PrintWriter(new StringWriter())).once();

        response.setStatus(responseCode);
        expectLastCall().once();
        replay(response);

        UsersActionImpl action = new UsersActionImpl();
        action.setException(e);
        action.doAction();
    }

    public class UsersActionImpl extends AbstractUserAction
    {
        Exception exception;

        public UsersActionImpl()
        {
            super();
        }

        public void doAction() throws Exception
        {
            throw exception;
        }

        public void setException(Exception e)
        {
            this.exception = e;
        }
    }

}