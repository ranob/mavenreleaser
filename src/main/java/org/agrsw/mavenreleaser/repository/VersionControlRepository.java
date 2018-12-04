package org.agrsw.mavenreleaser.repository;


import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.exception.ReleaserException;

import java.io.File;

public interface VersionControlRepository {

    boolean commit(File file, RepositoryDTO repositoryDTO, String notCheckToken) throws ReleaserException;

    boolean downloadProject(RepositoryDTO repositoryDTO, final File target) throws ReleaserException;
}
