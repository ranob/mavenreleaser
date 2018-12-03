package org.agrsw.mavenreleaser.repository.impl;


import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GitManagerImpl implements VersionControlRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitManagerImpl.class);

    private static final String ADD_ALL_FILES_TO_COMMIT = ".";

    @Override
    public boolean commit(File file, RepositoryDTO repositoryDTO, String notCheckToken) throws ReleaserException{
        try {

            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(repositoryDTO.getUserName(), repositoryDTO.getPassword());
            Git git =  Git.open(new File(file.getPath().replace("pom.xml", "")));

            git.add().addFilepattern(ADD_ALL_FILES_TO_COMMIT).call();

            git.commit()
                    .setAll(true)
                    .setMessage("Versioned files released")
                    .call();

            git.push()
                    .setCredentialsProvider(cp)
                    .call();
           return true;
        } catch (Exception e) {
            LOGGER.error("Error in git commit: ", e.getMessage());
            throw new ReleaserException("Error commit changes into repository: ", e);
        }
    }

    @Override
    public boolean downloadProject(RepositoryDTO repositoryDTO, File target) throws ReleaserException {
        try {
            LOGGER.info("--> Downloading from GIT " + repositoryDTO.getRemotePath());
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(repositoryDTO.getUserName(), repositoryDTO.getPassword());

            Git.cloneRepository()
                    .setCredentialsProvider(cp)
                    .setURI(repositoryDTO.getRemotePath())
                    .setDirectory(new File(target.getPath()))
                    .call();
            LOGGER.info("Artifact downloaded from GIT");
            return true;
        } catch (GitAPIException e) {
            LOGGER.error("Error in git cloning repository: ", e.getMessage());
            throw new ReleaserException("Error cloning repository.", e);
        }
    }
}
