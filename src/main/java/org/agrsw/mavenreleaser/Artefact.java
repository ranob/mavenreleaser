package org.agrsw.mavenreleaser;

import java.util.*;

public class Artefact
{
    private String artefactId;
    private String groupId;
    private String version;
    private String scmURL;
    private String jiraIssue;
    private String description;
    private List<Artefact> issueslinked;
    
    public Artefact() {
        this.issueslinked = new ArrayList<Artefact>();
    }
    
    public String getArtefactId() {
        return this.artefactId;
    }
    
    public void setArtefactId(final String artefactId) {
        this.artefactId = artefactId;
    }
    
    public String getGroupId() {
        return this.groupId;
    }
    
    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }
    
    public String getVersion() {
        return this.version;
    }
    
    public void setVersion(final String version) {
        this.version = version;
    }
    
    public String getScmURL() {
        return this.scmURL;
    }
    
    public void setScmURL(final String scmURL) {
        this.scmURL = scmURL;
    }
    
    @Override
    public String toString() {
        return "Artefact [artefactId=" + this.artefactId + ", groupId=" + this.groupId + ", version=" + this.version + ", jiraIssue=" + this.jiraIssue + ", issueslinked=" + this.issueslinked + "]";
    }
    
    public String getJiraIssue() {
        return this.jiraIssue;
    }
    
    public void setJiraIssue(final String jiraIssue) {
        this.jiraIssue = jiraIssue;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(final String description) {
        this.description = description;
    }
    
    public List<Artefact> getIssueslinked() {
        return this.issueslinked;
    }
    
    public void setIssueslinked(final List<Artefact> issueslinked) {
        this.issueslinked = issueslinked;
    }
    
    public boolean containsLinkedIssue(final String key) {
        boolean found = false;
        for (final Artefact art : this.issueslinked) {
            if (art.getJiraIssue().equals(key)) {
                found = true;
                break;
            }
        }
        return found;
    }
}
