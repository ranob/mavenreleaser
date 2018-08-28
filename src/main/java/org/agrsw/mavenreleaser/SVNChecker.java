package org.agrsw.mavenreleaser;

import org.agrsw.mavenreleaser.Artefact;
import org.agrsw.mavenreleaser.Releaser;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.*;

public class SVNChecker
{
    private static final Logger log;
    private Releaser releaser;
    private String repositoryURL;
    private static int ERROR_WRONG_PARAMETERS;
    private static int ERROR_COMMIT_MESSAGE_FORMAT;
    private static int ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST;
    private static int ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE;
    private static int ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE;
    @Value("${notcheck.token}")
    private String notcheckTokenProperty;
    private static String notcheckToken ="#NOTCHECK";
    //java -cp mavenreleaser-3.0.0-SNAPSHOT.jar -Dloader.main=org.agrsw.mavenreleaser.SVNChecker    org.springframework.boot.loader.PropertiesLauncher
    static {
        log = LoggerFactory.getLogger((Class)SVNChecker.class);
        SVNChecker.ERROR_WRONG_PARAMETERS = 1;
        SVNChecker.ERROR_COMMIT_MESSAGE_FORMAT = 2;
        SVNChecker.ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST = 3;
        SVNChecker.ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE = 4;
        SVNChecker.ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE = 5;
    }
    
    public SVNChecker() {
        this.repositoryURL = "http://192.168.10.2/svn/mercury";

		notcheckTokenProperty = Releaser.getToken();
		
    }
    
    public Artefact getArtefactOfFile(final String file) {
        final Artefact artefact = null;
        if (file != null) {
            final String[] splits = file.split("/src/main");
            SVNChecker.log.debug(splits.toString());
        }
        return artefact;
    }
    
    public static void main(final String[] args) {
        String[] svnFiles = null;
        String commitMessage = null;
         
        if (args!=null){
        	for (int i=0;i<args.length;i++){
        		SVNChecker.log.info("ARGS[" + i +"] " + args[i]);
        	}
        	SVNChecker.log.info("ARGS: " + args.toString());
        }
        if (args.length != 3) {
            SVNChecker.log.error("ERROR_WRONG_PARAMETERS");
            System.exit(SVNChecker.ERROR_WRONG_PARAMETERS);
        }
        else {
            SVNChecker.log.info("SVN Files: " + args[0]);
            SVNChecker.log.info("Commit Message: " + args[1]);
            SVNChecker.log.info("Usuario: " + args[2]);
            svnFiles = args[0].split(";");
            commitMessage = args[1];
        }
        
      
        
       // boolean isAllowedUser = isUserAllowed(args[2]);
       // if (!isAllowedUser){
       // 	System.exit(0);
       // }
        

        
        final String[] projects = { "MERCURY", "BANORTE", "PRUEB", "SANESPBACK", "SANMEXICO","LIBERBANK", "SANGER", "SANCHILE", "TARIFARIO", "SANESP","SANCHILEBK","SANESPBCK2"};
        String issueKey = null;
        final SVNChecker fm = new SVNChecker();
        
        SVNChecker.notcheckToken = fm.notcheckTokenProperty;
        SVNChecker.log.info("Not Check Toker: " + notcheckToken);
        
        boolean keyFound = false;
        SVNChecker.log.info("Check if the commit message contains de jira issue key");
        for (int i = 0; i < projects.length; ++i) {
            issueKey = fm.checkCommitMessage(commitMessage, projects[i]);
            SVNChecker.log.info("issueKey: " + issueKey);
            if (issueKey != null) {
                keyFound = true;
                break;
            }
        }
        if (keyFound) {
            SVNChecker.log.info("jira issue key found");
            SVNChecker.log.info("Before call checkCommit");
            JiraClient.userName = Releaser.jiraUser;
            JiraClient.password = Releaser.jiraPassword;
            Releaser.setUsername(Releaser.jiraUser);
            Releaser.setPassword(Releaser.jiraPassword);
            
            final int result = Releaser.checkCommit(svnFiles, issueKey);
            SVNChecker.log.info("After call checkCommit. Result: " + result);
            switch (result) {
                case 0: {
                    System.exit(0);
                }
                case 1: {
                    System.exit(SVNChecker.ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST);
                }
                case 2: {
                    System.exit(SVNChecker.ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE);
                }
                case 3: {
                    System.exit(SVNChecker.ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE);
                    break;
                }
            }
        }
        else {
            SVNChecker.log.info("jira issue key not found. Checking if it`s a maven release plugin commit");
           
            issueKey = fm.checkCommitMessageWithOutNumber(commitMessage, "maven-release-plugin");
            if (issueKey==null){
            	issueKey = fm.checkCommitMessageWithOutNumber(commitMessage,SVNChecker.notcheckToken );            	
            }
            if (issueKey == null) {
                SVNChecker.log.error("The commit Message has not the correct format: ERROR_COMMIT_MESSAGE_FORMAT");
                System.err.println("The commit Message has not the correct format: ERROR_COMMIT_MESSAGE_FORMAT");     
                System.exit(SVNChecker.ERROR_COMMIT_MESSAGE_FORMAT);
            }
            else {
                SVNChecker.log.info("The commit Message has the correct format");
                System.exit(0);
            }
        }
    }
    
    public String checkCommitMessage(final String commitMessage, final String projectName) {
        String key = null;
        final Pattern p = Pattern.compile(".*(" + projectName + "-[\\d]+).*", 2);
        final Matcher m = p.matcher(commitMessage);
        final boolean matches = m.find();
        if (matches) {
            SVNChecker.log.debug(m.group(1));
            key = m.group(1);
        }
        return key;
    }
    
    public String checkCommitMessageWithOutNumber(final String commitMessage, final String projectName) {
        String key = null;
        final Pattern p = Pattern.compile(".*(" + projectName + ").*", Pattern.CASE_INSENSITIVE);
        
        final Matcher m = p.matcher(commitMessage);
        final boolean matches = m.find();
        if (matches) {
            SVNChecker.log.debug(m.group(1));
            key = m.group(1);
        }
        return key;
    }
    
    public Releaser getReleaser() {
        return this.releaser;
    }
    
    public void setReleaser(final Releaser releaser) {
        this.releaser = releaser;
    }
    
    private static boolean isUserAllowed(String user){
    	boolean isAllowed = false;
    	SVNChecker.log.debug("->  + isUserAllowed " + user);
    	if (user.equals("alberto.garcia") || user.equals("alfonso.adiego") || user.equals("carlos.palacios")){
    		isAllowed = true;
    	}
    	SVNChecker.log.debug("<-  + isUserAllowed " + isAllowed);
    	return isAllowed;
    }
}
