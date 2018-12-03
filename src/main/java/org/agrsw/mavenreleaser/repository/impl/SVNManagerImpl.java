package org.agrsw.mavenreleaser.repository.impl;

import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.util.Date;

@Component
public class SVNManagerImpl implements VersionControlRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SVNManagerImpl.class);


    @Override
    public boolean commit(File file, RepositoryDTO repositoryDTO, String notCheckToken) throws ReleaserException {

        LOGGER.info("commit SVN artifact in." + repositoryDTO.getRemotePath());

        DAVRepositoryFactory.setup();
        final SVNClientManager manager = SVNClientManager.newInstance();
        try {
            final ISVNAuthenticationManager authManager =
                    SVNWCUtil.createDefaultAuthenticationManager(repositoryDTO.getUserName(), repositoryDTO.getPassword());
            manager.setAuthenticationManager(authManager);
            final SVNCommitClient svnCommitClient = manager.getCommitClient();
            svnCommitClient.doCommit(new File[] { file }, false, "Releaser " + notCheckToken, null, null, false, false, SVNDepth.INFINITY);

            LOGGER.info("commit SVN artifact Ok.");
            return true;
        }
        catch (SVNException e) {
            LOGGER.error("Error commit project." + e);
            throw new ReleaserException(e.getMessage(), e);
        }
        finally {
            manager.dispose();
        }
    }

    public boolean downloadProject(RepositoryDTO repositoryDTO, final File target) throws ReleaserException {
       LOGGER.info("--> Downloading from SVN " + repositoryDTO.getRemotePath());
        DAVRepositoryFactory.setup();
        final SVNClientManager manager = SVNClientManager.newInstance();
        try {

            final ISVNAuthenticationManager authManager =
                    SVNWCUtil.createDefaultAuthenticationManager(repositoryDTO.getUserName(), repositoryDTO.getPassword());
            final SVNUpdateClient client = manager.getUpdateClient();
            manager.setAuthenticationManager(authManager);
            LOGGER.debug("Before checking out " + new Date());
            final SVNURL svnURL = SVNURL.parseURIEncoded(repositoryDTO.getRemotePath());
            client.doCheckout(svnURL, target, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
            LOGGER.debug("Artefact downloaded from SVN");
            return true;
        }
        catch (SVNException e) {
            LOGGER.error("Error checking out project." + e);
            throw new ReleaserException(e.getMessage(), e);
        }
        catch (Exception e2) {
            LOGGER.error("Error checking out project." + e2);
            throw new ReleaserException(e2.getMessage(), e2);
        }
        finally {
            manager.dispose();
        }
    }
}
