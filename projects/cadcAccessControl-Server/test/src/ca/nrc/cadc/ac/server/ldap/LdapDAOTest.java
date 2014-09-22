/**
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
 ************************************************************************
 */


package ca.nrc.cadc.ac.server.ldap;

import static ca.nrc.cadc.ac.server.ldap.LdapGroupDAOTest.config;
import static org.junit.Assert.assertTrue;

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.junit.Test;

import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.NumericPrincipal;

import com.unboundid.ldap.sdk.LDAPConnection;

public class LdapDAOTest
{
    static String server = "mach275.cadc.dao.nrc.ca";
    static int port = 389;
    static String adminDN = "uid=webproxy,ou=WebProxy,ou=topologymanagement,o=netscaperoot";
    static String adminPW = "go4it";
    static String usersDN = "ou=Users,ou=ds,dc=canfartest,dc=net";
    static String groupsDN = "ou=Groups,ou=ds,dc=canfartest,dc=net";
    static String adminGroupsDN = "ou=adminGroups,ou=ds,dc=canfartest,dc=net";
    
    LdapConfig config = new LdapConfig(server, port, adminDN, adminPW, usersDN, groupsDN, adminGroupsDN);
    
    @Test
    public void testLdapBindConnection() throws Exception
    {
        //TODO use a test user to test with. To be done when addUser available.
        //LdapUserDAO<X500Principal> userDAO = new LdapUserDAO<X500Principal>();

        // User authenticated with HttpPrincipal
        HttpPrincipal httpPrincipal = new HttpPrincipal("CadcDaoTest1");
        Subject subject = new Subject();

        subject.getPrincipals().add(httpPrincipal);
        
        final LdapDAOTestImpl ldapDao = new LdapDAOTestImpl(config);

        Subject.doAs(subject, new PrivilegedExceptionAction<Object>()
        {
            public Object run() throws Exception
            {
                try
                {
                    LDAPConnection ldapCon = ldapDao.getConnection();
                    assertTrue(ldapCon.isConnected());
                    return null;
                }
                catch (Exception e)
                {
                    throw new Exception("Problems", e);
                }
            }
        });
               
        
        X500Principal subjPrincipal = new X500Principal(
                "cn=cadcdaotest1,ou=cadc,o=hia,c=ca");
        subject = new Subject();
        subject.getPrincipals().add(subjPrincipal);
        
        Subject.doAs(subject, new PrivilegedExceptionAction<Object>()
        {
            public Object run() throws Exception
            {
                try
                {
                    LDAPConnection ldapCon = ldapDao.getConnection();
                    assertTrue(ldapCon.isConnected());
                    return null;
                }
                catch (Exception e)
                {
                    throw new Exception("Problems", e);
                }
            }
        });
        
        
        NumericPrincipal numPrincipal = new NumericPrincipal(1866);       
        subject.getPrincipals().add(numPrincipal);

        Subject.doAs(subject, new PrivilegedExceptionAction<Object>()
        {
            public Object run() throws Exception
            {
                try
                {

                    LDAPConnection ldapCon = ldapDao.getConnection();
                    assertTrue(ldapCon.isConnected());
                    return null;
                }
                catch (Exception e)
                {
                    throw new Exception("Problems", e);
                }
            }
        });

    }
}
