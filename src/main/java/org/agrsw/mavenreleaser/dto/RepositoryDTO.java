package org.agrsw.mavenreleaser.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.agrsw.mavenreleaser.util.RepositoryTypeEnum;

@Getter
@Setter
@AllArgsConstructor
public class RepositoryDTO {

    private String userName;
    private String password;
    private String remotePath;
    private String localPath;
    private RepositoryTypeEnum repositoryType;
}
