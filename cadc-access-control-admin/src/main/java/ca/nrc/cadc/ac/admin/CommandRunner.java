/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2015.                            (c) 2015.
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
 *
 ************************************************************************
 */

package ca.nrc.cadc.ac.admin;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;

import ca.nrc.cadc.ac.UserNotFoundException;
import ca.nrc.cadc.ac.server.UserPersistence;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.DelegationToken;
import ca.nrc.cadc.auth.PrincipalExtractor;
import ca.nrc.cadc.auth.SSOCookieCredential;
import ca.nrc.cadc.auth.X509CertificateChain;
import ca.nrc.cadc.net.TransientException;


public class CommandRunner
{
    private final static Logger LOGGER = Logger.getLogger(CommandRunner.class);
    private final CmdLineParser commandLineParser;
    private final UserPersistence userPersistence;


    public CommandRunner(final CmdLineParser commandLineParser,
                         final UserPersistence userPersistence)
    {
        this.commandLineParser = commandLineParser;
        this.userPersistence = userPersistence;
    }


    /**
     * Run a suitable action command.
     *
     */
    public void run() throws UserNotFoundException, TransientException
    {
        AbstractCommand command = commandLineParser.getCommand();
        command.setUserPersistence(userPersistence);

        Subject operatorSubject = new Subject();

        if (command instanceof AbstractUserCommand)
        {
            Principal userIDPrincipal = ((AbstractUserCommand) command).getPrincipal();
            operatorSubject.getPrincipals().add(userIDPrincipal);
        }
        else
        {
            // run as the operator using their cert
            Subject subjectFromCert = commandLineParser.getSubjectFromCert();

            if (subjectFromCert == null)
                throw new IllegalArgumentException("Certificate required");

            Set<X500Principal> pSet = subjectFromCert.getPrincipals(X500Principal.class);
            if (pSet.isEmpty())
                throw new IllegalArgumentException("Certificate required");

            operatorSubject.getPrincipals().addAll(subjectFromCert.getPrincipals());
            operatorSubject.getPublicCredentials().addAll(subjectFromCert.getPublicCredentials());
        }

        // run as the user
        AnonPrincipalExtractor principalExtractor = new AnonPrincipalExtractor(operatorSubject);
        Subject subject = AuthenticationUtil.getSubject(principalExtractor);
        LOGGER.debug("running as: " + subject);
        Subject.doAs(subject, command);
    }

    class AnonPrincipalExtractor implements PrincipalExtractor
    {
        Subject s;

        AnonPrincipalExtractor(Subject s)
        {
            this.s = s;
        }
        public Set<Principal> getPrincipals()
        {
            return s.getPrincipals();
        }
        public X509CertificateChain getCertificateChain()
        {
            LOGGER.debug("getCerfiticateChain called");
            for (Object o : s.getPublicCredentials())
            {
                if (o instanceof X509CertificateChain)
                {
                    LOGGER.debug("returning certificate chain.");
                    return (X509CertificateChain) o;
                }
            }
            return null;
        }
        public DelegationToken getDelegationToken()
        {
            return null;
        }
        public SSOCookieCredential getSSOCookieCredential()
        {
            return null;
        }
    }
}
