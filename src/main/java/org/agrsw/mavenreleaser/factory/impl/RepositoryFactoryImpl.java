package org.agrsw.mavenreleaser.factory.impl;


import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.factory.RepositoryFactory;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.agrsw.mavenreleaser.repository.impl.GitManagerImpl;
import org.agrsw.mavenreleaser.repository.impl.SVNManagerImpl;
import org.agrsw.mavenreleaser.util.RepositoryTypeEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class RepositoryFactoryImpl implements RepositoryFactory {

    private static final Map<RepositoryTypeEnum, VersionControlRepository> REPOSITORY_MAP = new HashMap<>();

   static {
       REPOSITORY_MAP.put(RepositoryTypeEnum.SVN, new SVNManagerImpl());
       REPOSITORY_MAP.put(RepositoryTypeEnum.GIT, new GitManagerImpl());
    }

    @Override
    public VersionControlRepository buildRepositoryManager(RepositoryTypeEnum repositoryTypeEnum) throws ReleaserException {

        if(Objects.isNull(REPOSITORY_MAP.get(repositoryTypeEnum))){
            throw new ReleaserException("Repository not found");
        }

        return REPOSITORY_MAP.get(repositoryTypeEnum);
    }
}
