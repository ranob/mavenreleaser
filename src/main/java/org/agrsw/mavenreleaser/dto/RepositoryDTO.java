package org.agrsw.mavenreleaser.dto;

import org.agrsw.mavenreleaser.util.RepositoryTypeEnum;

public class RepositoryDTO {

    private String userName;
    private String password;
    private String remotePath;
    private String localPath;
    private RepositoryTypeEnum repositoryType;
}
