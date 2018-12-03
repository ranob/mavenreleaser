package org.agrsw.mavenreleaser.factory;


import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.agrsw.mavenreleaser.util.RepositoryTypeEnum;

public interface RepositoryFactory {

    VersionControlRepository buildRepositoryManager(RepositoryTypeEnum repositoryTypeEnum) throws ReleaserException;
}
