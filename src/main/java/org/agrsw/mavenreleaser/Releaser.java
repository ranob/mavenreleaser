package org.agrsw.mavenreleaser;

import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.factory.RepositoryFactory;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.agrsw.mavenreleaser.util.RepositoryTypeEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.beans.factory.annotation.*;
import org.slf4j.*;
import org.codehaus.plexus.util.xml.pull.*;
import org.apache.commons.cli.*;
import org.springframework.boot.*;
import org.tmatesoft.svn.core.io.*;
import org.tmatesoft.svn.core.auth.*;
import org.apache.maven.shared.invoker.*;
import org.jfrog.artifactory.client.model.*;
import org.jfrog.artifactory.client.*;
import org.apache.maven.model.*;
import org.tmatesoft.svn.core.internal.io.dav.*;
import org.tmatesoft.svn.core.wc.*;



import org.tmatesoft.svn.core.*;
import org.apache.maven.model.io.xpp3.*;
import org.codehaus.plexus.util.*;
import java.io.*;
import java.io.File;
import java.util.*;

@SpringBootApplication()
@EnableAutoConfiguration(exclude={EmbeddedServletContainerAutoConfiguration.class,WebMvcAutoConfiguration.class})
public class Releaser implements CommandLineRunner
{

    @Value("${maven.home}")
    private String mavenHomeProperty;
 //   @Value("${notcheck.token}")
    private String notcheckTokenPropery;
    private static String mavenHome;
    private static String notCheckToken;
    private static final Logger log;
    private static Map<String, String> artefacts;
    private static Map<String, String> artefactsAlreadyReleased;
    private static Map<String, String> artefactsNotInArtifactory;
    private static Map<String, Artefact> jirasNotReleased;
    private static Map<String, Artefact> jirasReleased;
    private static String username;
    private static String password;
    private static String url;
    private static String artefactName;
    private static String tempDir;
    private static String action;
    private static String repositoryType;
    private static boolean jiraIntegration;
    private static String repoURL;
    private JiraClient jiraClient;
    public static String jiraUser ="";
    public static String jiraPassword="";
    public static RepositoryDTO repositoryDTO;

    @Autowired
    private RepositoryFactory repositoryFactory;

    private static VersionControlRepository versionControlRepository;

    static {
        Releaser.mavenHome = "";
        log = LoggerFactory.getLogger((Class)Releaser.class);
        Releaser.artefacts = new HashMap<String, String>();
        Releaser.artefactsAlreadyReleased = new HashMap<String, String>();
        Releaser.artefactsNotInArtifactory = new HashMap<String, String>();
        Releaser.jirasNotReleased = new HashMap<String, Artefact>();
        Releaser.jirasReleased = new HashMap<String, Artefact>();
        Releaser.username = "";
        Releaser.password = "";
        Releaser.url = "";
        Releaser.artefactName = "";
        Releaser.tempDir = "/tmp/svn/";
        Releaser.action = "";
        Releaser.jiraIntegration = false;
        //Releaser.repoURL = "http://192.168.10.2/svn/mercury/";
        //Releaser.repoURL = "http://192.168.1.17/svn/myrepo/";
        Releaser.repoURL = "http://192.168.10.2/svn/mercury/";
    }
    
    public Releaser() {
        this.mavenHomeProperty = "";
    }
    
    public static String getUsername() {
        return Releaser.username;
    }
    
    @Override
    public void run(final String... args) {
        Releaser.log.debug("Start Releasing..");
        Releaser.mavenHome = this.mavenHomeProperty;
        Releaser.notCheckToken = Releaser.getToken();
        Releaser.log.debug("Maven Home: " + Releaser.mavenHome);
        Releaser.log.debug("NotCheck Token: " + Releaser.notCheckToken);
        final Options options = new Options();
        final Option userNameOption = Option.builder().argName("username").hasArg(true).longOpt("username").required(true).build();
        final Option urlOption = Option.builder().argName("url").hasArg(true).longOpt("url").required(true).build();
        final Option artefactOption = Option.builder().argName("artefactName").hasArg(true).longOpt("artefactName").required(true).build();
        final Option actionOption = Option.builder().argName("action").hasArg(true).longOpt("action").required(true).build();
        final Option jiraOption = Option.builder().argName("jira").hasArg(true).longOpt("jira").required(false).build();
        final Option repositoryType = Option.builder().argName("repositoryType").hasArg(true).longOpt("repositoryType").required(true).build();
        final CommandLineParser parser = (CommandLineParser)new DefaultParser();
        options.addOption(userNameOption);
        options.addOption(urlOption);
        options.addOption(artefactOption);
        options.addOption(actionOption);
        options.addOption(jiraOption);
        options.addOption(repositoryType);
        try {
            final CommandLine cmd = parser.parse(options, args);
            Releaser.username = (String)cmd.getParsedOptionValue("username");
            Releaser.artefactName = (String)cmd.getParsedOptionValue("artefactName");
            Releaser.url = cmd.getOptionValue("url");
            Releaser.action = cmd.getOptionValue("action");
            Releaser.repositoryType = cmd.getOptionValue("repositoryType");

            final RepositoryTypeEnum repositoryTypeEnum = RepositoryTypeEnum.valueOf(Releaser.repositoryType.toUpperCase());

            final String localRepositoryPath = buildLocalRepositoryPath(repositoryTypeEnum);

             versionControlRepository = repositoryFactory.buildRepositoryManager(repositoryTypeEnum);

            if (cmd.getParsedOptionValue("jira") != null) {
                final String jiraOpt = (String)cmd.getParsedOptionValue("jira");
                if (jiraOpt.equals("true") || jiraOpt.equals("false")) {
                    Releaser.jiraIntegration = new Boolean(jiraOpt);
                }
                else {
                    Releaser.log.error("Invalid jira option. It must be true or false and it was: " + jiraOpt);
                }
            }
            Releaser.log.debug(cmd.toString());
            final Console cnsl = System.console();
            if (cnsl != null) {
                final char[] passwordChar = cnsl.readPassword("Password: ", new Object[0]);
                Releaser.password = String.copyValueOf(passwordChar);
            }
            else {
                Releaser.password = getLineFromConsole("Type the password for " + Releaser.username);
            }
            repositoryDTO = new RepositoryDTO(Releaser.username, Releaser.password, Releaser.url, localRepositoryPath, repositoryTypeEnum);
        }
        catch (ParseException e1) {
            e1.printStackTrace();
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar nombrejar.jar", options);
            System.exit(-1);
        } catch (ReleaserException e) {
            System.exit(-1);
        }
        JiraClient.setUserName(jiraUser);
        JiraClient.setPassword(jiraPassword);  
        try {
            if (Releaser.action.equals("release")) {
                this.jiraClient = new JiraClient();
                              
                doRelease(repositoryDTO, String.valueOf(Releaser.artefactName) + "-" + System.currentTimeMillis());
                Releaser.log.info("######################## Artefactos encontrados:  ###################");
                Collection<String> values = Releaser.artefacts.keySet();
                for (final String artefact : values) {
                    Releaser.log.info(artefact);
                }
                Releaser.log.info("######################## Artefactos que ya estaban releseados:  ###################");
                values = Releaser.artefactsAlreadyReleased.keySet();
                for (final String artefact : values) {
                    Releaser.log.info(artefact);
                }
                Releaser.log.info("######################## Artefactos que no est\u00e1n en Artifactory (hacer clean deploy):  ###################");
                values = Releaser.artefactsNotInArtifactory.keySet();
                for (final String artefact : values) {
                    Releaser.log.info(artefact);
                }
                Releaser.log.info("######################## Artefactos jiras que se procesaron correctamente  ###################");
                Collection<Artefact> jiras = Releaser.jirasReleased.values();
                for (final Artefact issue : jiras) {
                    Releaser.log.info(String.valueOf(issue.getGroupId()) + "-" + issue.getArtefactId() + "-" + issue.getJiraIssue());
                }
                Releaser.log.info("######################## Artefactos jiras que NO se procesaron correctamente  ###################");
                jiras = Releaser.jirasNotReleased.values();
                for (final Artefact issue : jiras) {
                    Releaser.log.info(String.valueOf(issue.getGroupId()) + "-" + issue.getArtefactId());
                }
            }
            else if (Releaser.action.equals("prepare")) {
                doPrepare(repositoryDTO, String.valueOf(Releaser.artefactName) + "-" + System.currentTimeMillis());
                Releaser.log.info("Artefactos encontrados: ");
                final Collection<String> values = Releaser.artefacts.keySet();
                for (final String artefact : values) {
                    Releaser.log.info(artefact);
                }
            }
            else if (Releaser.action.equals("sources")) {
                doSources(repositoryDTO, String.valueOf(Releaser.artefactName) + "-" + System.currentTimeMillis());
                Releaser.log.info("Artefactos encontrados: ");
                final Collection<String> values = Releaser.artefacts.keySet();
                for (final String artefact : values) {
                    Releaser.log.info(artefact);
                }
            }
        }
        catch (FileNotFoundException e2) {
            e2.printStackTrace();
        }
        catch (IOException e3) {
            e3.printStackTrace();
        }
        catch (XmlPullParserException e4) {
            e4.printStackTrace();
        }
        catch (MavenInvocationException e5) {
            e5.printStackTrace();
        } catch (ReleaserException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(final String[] args) throws MavenInvocationException, FileNotFoundException, IOException, XmlPullParserException {    	
    	//JiraClient.createIssue("PRUEB","Summary 1", "Descripcion 1", "padre", "1.0.0", "1.1.0");
    	SpringApplication.run((Object)Releaser.class, args);
    }
    
    private static String getLineFromConsole(final String message) {
        String line = "";
        Releaser.log.info(message);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            line = reader.readLine();
        }
        catch (IOException e) {
            Releaser.log.error(e.toString());
        }
        return line;
    }
    
    private static boolean checkIfPathExist(final String path) {
        final SVNClientManager manager = SVNClientManager.newInstance();
        boolean exists = false;
        try {
            SVNRepository repository = null;
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(Releaser.repoURL));
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(Releaser.username, Releaser.password);
            repository.setAuthenticationManager(authManager);
            final SVNNodeKind nodeKind = repository.checkPath(path, -1L);
            if (nodeKind == SVNNodeKind.NONE) {
                System.err.println("There is no entry at '" + path + "'.");             
            }
            else if (nodeKind == SVNNodeKind.FILE) {
            	exists=true;
            }
            Releaser.log.debug("");
        } catch (Exception e) {
            Releaser.log.debug("");
        }
        return exists;
    }
    
    private static void doRelease(final RepositoryDTO repositoryDTO, final String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = String.valueOf(Releaser.tempDir) + artefactName;
        Releaser.log.info("--> #################### Release Started for Artefact " + artefactName);
        versionControlRepository.downloadProject(repositoryDTO, new File(path));
        final String artefactInfo = getArtifactInfo(String.valueOf(path) + "/pom.xml");
        Releaser.log.info("Artefact Info :" + artefactInfo);
        processPOM(String.valueOf(path) + "/pom.xml", repositoryDTO);
        mavenInvoker(String.valueOf(path) + "/pom.xml");
        Releaser.log.info("<-- #################### Release Finished for Artefact " + artefactName);
    }
    
    private static void doPrepare(final RepositoryDTO repositoryDTO, final String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = String.valueOf(Releaser.tempDir) + artefactName;
        Releaser.log.info("--> ######## Prepare Started for Artefact " + artefactName);
        versionControlRepository.downloadProject(repositoryDTO, new File(path));
        processPOM2(String.valueOf(path) + "/pom.xml");
        Releaser.log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
    }
    
    private static void doSources(final RepositoryDTO repositoryDTO, final String artefactName) throws FileNotFoundException, IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = String.valueOf(Releaser.tempDir) + artefactName;
        Releaser.log.info("--> ######## Prepare Started for Artefact " + artefactName);
        versionControlRepository.downloadProject(repositoryDTO, new File(path));
        processPOM3(String.valueOf(path) + "/pom.xml");
        Releaser.log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
    }
    
    private static void mavenInvoker(final String pom) throws MavenInvocationException {
        final InvocationRequest request = (InvocationRequest)new DefaultInvocationRequest();
        Releaser.log.debug("Before calling getArtefact");
        final Artefact artefact = getArtefactFromFile(pom);
        
        Releaser.log.info("Current Version : " + artefact.getVersion());
        final String autoVersion = getNextVersion(artefact.getVersion(), artefact.getScmURL());
        String nextVersion = getLineFromConsole("Type the new version (" + autoVersion + "): ");
        if (nextVersion.equals("")) {
            nextVersion = autoVersion;
        }
        if (!nextVersion.endsWith("-SNAPSHOT")) {
            Releaser.log.warn("Next Version has not -SNAPSHOT SUFFIX. Adding...");
            nextVersion = String.valueOf(nextVersion) + "-SNAPSHOT";
        }
        request.setPomFile(new File(pom));
        final List<String> goals = new ArrayList<String>();
        goals.add("release:prepare");
        goals.add("release:perform");
        request.setGoals((List)goals);
        final Properties properties = new Properties();
        
        properties.put("username", Releaser.username);
        properties.put("password", Releaser.password);
        properties.put("arguments", "-DskipTests -Dmaven.javadoc.skip=true ");
        properties.put("developmentVersion", nextVersion);
        request.setProperties(properties);
        final Invoker invoker = (Invoker)new DefaultInvoker();
        invoker.setInputStream(System.in);
        invoker.setMavenHome(new File(Releaser.mavenHome));
        final InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }
        String version = artefact.getVersion();
        if (version.endsWith("-SNAPSHOT")) {
            final int snapshotPosition = version.indexOf("-SNAPSHOT");
            version = version.substring(0, snapshotPosition);
        }
        if (nextVersion.endsWith("-SNAPSHOT")) {
            final int snapshotPosition = nextVersion.indexOf("-SNAPSHOT");
            nextVersion = nextVersion.substring(0, snapshotPosition);
        }
        final String project = getProject(artefact.getGroupId());
        if (!project.equals("")) {
            final Artefact arti = JiraClient.getIssueKey(project, artefact.getArtefactId(), version);
            if (arti == null) {
                Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                Releaser.log.error("Cannot find jira issue for artefact: " + artefact);
            }
            else {
            	JiraClient.createVersion(project, nextVersion, nextVersion);            	
                final String newIssue = JiraClient.createIssue(project, String.valueOf(artefact.getArtefactId()) + "-" + nextVersion, arti.getDescription(), artefact.getArtefactId(), version, nextVersion);
                if (newIssue == null) {
                    Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                    Releaser.log.error("Cannot create jira issue for artefact: " + artefact);
                }
                else {
                    final String oldIssue = JiraClient.closeIssue(arti.getJiraIssue());
                    if (oldIssue == null) {
                        Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                        Releaser.log.error("Cannot close jira issue for artefact: " + artefact);
                    }
                    else {
                        artefact.setJiraIssue(newIssue);
                        Releaser.jirasReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                        Releaser.log.error("jira issue released: " + artefact);
                    }
                }
            }
        }
        else {
            Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
            Releaser.log.error("Cannot determinate the project for artifact: " + artefact);
        }
    }
    
    private static String getProject(final String groupId) {
        String project = "";
        if (groupId != null && !groupId.equals("")) {
            if (groupId.startsWith("com.mercurytfs.mercury.core") || groupId.startsWith("com.mercurytfs.mercury.config") || groupId.startsWith("com.mercurytfs.mercury.products") ||  
                groupId.startsWith("com.mercurytfs.mercury.web") || groupId.startsWith("com.mercurytfs.mercury.modules") || groupId.startsWith("com.mercurytfs.mercury.init")  || 
                groupId.startsWith("com.mercurytfs.mercury.scripts") || groupId.startsWith("com.mercurytfs.mercury.integration")
                || groupId.startsWith("com.mercurytfs.mercury.cloud") ) {
                project = "MERCURY";
            }
            else if (groupId.startsWith("com.santander.comex") || groupId.startsWith("com.mercurytfs.mercury.customers.bancosantander.spain.santandercomex")) {
                project = "SANESPBACK";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.liberbank")) {
                project = "LIBERBANK";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.prueba")) {
                project = "PRUEB";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.santander.mexico")) {
                project = "SANMEXICO";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.santander.spain")) {
                project = "SANESP";
            }
            
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.santander.chile.products") 
            		|| groupId.startsWith("com.mercurytfs.mercury.customers.santander.chile.common")
            		|| groupId.startsWith("com.mercurytfs.mercury.customers.santander.chile.modules")
            		|| groupId.startsWith("com.mercurytfs.mercury.customers.santander.chile.web")
            		|| groupId.startsWith("com.mercurytfs.mercury.customers.santander.chile.config")
            		
            		) {
                project = "SANCHILE";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.santander.chile.tarifario")) {
                project = "TARIFARIO";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.banorte")) {
                project = "BANORTE";
            } else if (groupId.startsWith("com.mercurytfs.mercury.customers.santander.germany")){
            	project = "SANGER";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.santander.chile.back")){
            	project = "SANCHILEBK";
            }
            else if (groupId.startsWith("com.mercurytfs.mercury.customers.bancosantander.spain.cloud")){
            	project = "SANESPBCK2";
            }
            
            
            
        }
        return project;
    }
    
    private static Model getArtifactFromArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        Releaser.log.info("-->Searching artifact in artifactory");
        Model model = null;
        final Artifactory artifactory = ArtifactoryClient.create("http://192.168.10.2:8081/artifactory/", Releaser.username, Releaser.password);
        final String repoSnapshot1 = "libs-snapshot-local";
        final String repoSnapshot2 = "libs-snapshot-santander";
        final String repoRelease1 = "libs-release-santander";
        final String repoRelease2 = "libs-release-local";
        String repo1;
        String repo2;
        if (release) {
            repo1 = repoRelease1;
            repo2 = repoRelease2;
        }
        else {
            repo1 = repoSnapshot1;
            repo2 = repoSnapshot2;
        }
        final List<RepoPath> results = (List<RepoPath>)artifactory.searches().artifactsByGavc().groupId(groupId).artifactId(artifactId).version(version).repositories(new String[] { repo1, repo2 }).doSearch();
        String itemPath = "";
        InputStream iStream = null;
        if (results != null) {
            for (final RepoPath searchItem : results) {
                itemPath = searchItem.getItemPath();
                if (itemPath.endsWith(".pom")) {
                    Releaser.log.debug("Pom found");
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                    final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
                    model = mavenreader.read(iStream);
                }
            }
        }
        if (model == null) {
            Releaser.log.debug("Pom not found in artifactory");
        }
        Releaser.log.info("<--Searching artifact in artifactory");
        return model;
    }
    
    private static InputStream getArtifactSourceArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        Releaser.log.info("-->Searching artifact in artifactory");
        final Artifactory artifactory = ArtifactoryClient.create("http://192.168.10.2:8081/artifactory/", Releaser.username, Releaser.password);
        final String repoSnapshot1 = "libs-snapshot-local";
        final String repoSnapshot2 = "libs-snapshot-santander";
        final String repoRelease1 = "libs-release-santander";
        final String repoRelease2 = "libs-release-local";
        String repo1;
        String repo2;
        if (release) {
            repo1 = repoRelease1;
            repo2 = repoRelease2;
        }
        else {
            repo1 = repoSnapshot1;
            repo2 = repoSnapshot2;
        }
        final List<RepoPath> results = (List<RepoPath>)artifactory.searches().artifactsByGavc().groupId(groupId).artifactId(artifactId).version(version).repositories(new String[] { repo1, repo2 }).doSearch();
        String itemPath = "";
        InputStream iStream = null;
        if (results != null) {
            for (final RepoPath searchItem : results) {
                itemPath = searchItem.getItemPath();
                if (itemPath.endsWith("sources.jar") || itemPath.endsWith(".war")) {
                    Releaser.log.debug("Source found");
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                }
            }
        }
        if (iStream == null) {
            Releaser.log.debug("Source not found in artifactory");
        }
        Releaser.log.info("<--Searching artifact in artifactory");
        return iStream;
    }
    
    private static void saveToFile(final InputStream is, final String name) {
        final File targetFile = new File(name);
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(targetFile);
            final byte[] buffer = new byte[is.available()];
            is.read(buffer);
            outStream.write(buffer);
        }
        catch (FileNotFoundException e) {
            Releaser.log.error(e.toString());
        }
        catch (IOException e2) {
            Releaser.log.error(e2.toString());
        }
        finally {
            if (outStream != null) {
                try {
                    outStream.close();
                }
                catch (IOException e3) {
                    Releaser.log.error(e3.toString());
                }
            }
        }
        if (outStream != null) {
            try {
                outStream.close();
            }
            catch (IOException e3) {
                Releaser.log.error(e3.toString());
            }
        }
    }
    
    private static void processPOM(final String file, RepositoryDTO repositoryDTO) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        Releaser.log.info("-->Processing Pom " + file);
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read((Reader)new FileReader(pomfile));
        if (model.getVersion().indexOf("-SNAPSHOT") == -1) {
            Releaser.log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return;
        }
        final List<Dependency> deps = (List<Dependency>)model.getDependencies();
        Releaser.log.info("Processing dependencies...");
        String artefact = "";
        for (final Dependency d : deps) {
            artefact = String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion();
            Releaser.log.debug(artefact);
            if (d.getVersion().endsWith("SNAPSHOT")) {
                Releaser.log.debug("The artefact is in SNAPSHOT, processing...");
                Releaser.log.debug("Check in artifactoy is the release version already exists");
                final Model releasePom = getArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")), true);
                if (releasePom == null) {
                    Releaser.log.info("Artifact release not found at artifactory");
                    final Model pom = getArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion(), false);
                    if (pom != null) {
                        final String svnURL = pom.getScm().getDeveloperConnection();
                        final String url = svnURL.substring(svnURL.indexOf("http"));

                        RepositoryDTO repositoryDependencyDTO = new RepositoryDTO(Releaser.username, Releaser.password, url, Releaser.tempDir, null);

                        doRelease(repositoryDependencyDTO, String.valueOf(d.getArtifactId()) + System.currentTimeMillis());
                        d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")));
                        if (!Releaser.artefacts.containsKey(artefact)) {
                            Releaser.artefacts.put(artefact, artefact);
                        }
                        else {
                            Releaser.log.warn("Artefact is already in the map " + artefact);
                        }
                    }
                    else {
                        Releaser.log.error("Artifact not found at repository");
                        if (!Releaser.artefactsNotInArtifactory.containsKey(artefact)) {
                            Releaser.artefactsNotInArtifactory.put(artefact, artefact);
                        }
                        else {
                            Releaser.log.warn("Artefact is already in the map " + artefact);
                        }
                    }
                }
                else {
                    Releaser.log.debug("The artifact is in snapshot in the pom.xml but is already released");
                    d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")));
                    if (!Releaser.artefactsAlreadyReleased.containsKey(artefact)) {
                        Releaser.artefactsAlreadyReleased.put(artefact, artefact);
                    }
                    else {
                        Releaser.log.warn("Artefact is already in the map " + artefact);
                    }
                }
            }
            else {
                Releaser.log.debug("The artifact is a release, skiping");
            }
        }
        writeModel(pomfile, model);

        versionControlRepository.commit(pomfile, repositoryDTO, notCheckToken);
        Releaser.log.info("<--Processing Pom " + file);
    }
    
    private static void processPOM2(final String file) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        Releaser.log.debug("Processin Pom " + file);
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read((Reader)new FileReader(pomfile));
        if (model.getVersion().indexOf("-SNAPSHOT") == -1) {
            Releaser.log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return;
        }
        final List<Dependency> deps = (List<Dependency>)model.getDependencies();
        Releaser.log.debug("Processing dependencies...");
        String artefact = "";
        for (final Dependency d : deps) {
            artefact = String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion();
            Releaser.log.debug(String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion());
            if (d.getVersion().endsWith("SNAPSHOT")) {
                Releaser.log.debug("The artefact is in SNAPSHOT, processing...");
                Releaser.log.debug("Check in artifactoy is the release version already exists");
                final Model releasePom = getArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")), true);
                if (releasePom != null) {
                    continue;
                }
                Releaser.log.debug("Artifact release not found at artifactory");
                if (!Releaser.artefacts.containsKey(artefact)) {
                    Releaser.artefacts.put(artefact, artefact);
                    final Model pom = getArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion(), false);
                    if (pom != null) {
                        final String svnURL = pom.getScm().getDeveloperConnection();
                        final String url = svnURL.substring(svnURL.indexOf("http"));
                        // TODO leo bernal
                        RepositoryDTO repositoryDTO = new RepositoryDTO(Releaser.username, Releaser.password, url, Releaser.tempDir, null);

                        doPrepare(repositoryDTO, String.valueOf(d.getArtifactId()) + System.currentTimeMillis());
                        d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")));
                    }
                    else {
                        Releaser.log.debug("Artifact not found at repository");
                    }
                }
                else {
                    Releaser.log.warn("Artefact is already in the map " + artefact);
                }
                
               
            }
            else {
                Releaser.log.debug("The artifact is a release, skiping");
            }
        }
    }
    
    private static void processPOM3(final String file) throws FileNotFoundException, IOException, XmlPullParserException, MavenInvocationException {
        Releaser.log.debug("Processin Pom " + file);
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read((Reader)new FileReader(pomfile));
        final List<Dependency> deps = (List<Dependency>)model.getDependencies();
        Releaser.log.debug("Processing dependencies...");
        String artefact = "";
        for (final Dependency d : deps) {
            artefact = String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion();
            Releaser.log.debug(String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion());
            if (d.getGroupId().startsWith("com.mercury")) {
                final InputStream is = getArtifactSourceArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion(), true);
                if (is == null) {
                    continue;
                }
                Releaser.log.debug("Artifact source  found at artifactory");
                if (!Releaser.artefacts.containsKey(artefact)) {
                    Releaser.artefacts.put(artefact, artefact);
                    saveToFile(is, "/tmp/sources/" + d.getGroupId() + "-" + d.getArtifactId() + "-" + d.getVersion() + ".jar");
                }
                else {
                    Releaser.log.warn("Artefact is already in the map " + artefact);
                }
            }
            else {
                Releaser.log.debug("The artifact does not belong to mercury");
            }
        }
    }
    
    private static boolean downloadProject(final String url, final File target) {
        Releaser.log.info("--> Downloading from SVN " + url);
        DAVRepositoryFactory.setup();
        final SVNClientManager manager = SVNClientManager.newInstance();
        try {
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(Releaser.username, Releaser.password);
            final SVNUpdateClient client = manager.getUpdateClient();
            manager.setAuthenticationManager(authManager);
            Releaser.log.debug("Before checking out " + new Date());
            final SVNURL svnURL = SVNURL.parseURIEncoded(url);
            client.doCheckout(svnURL, target, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
            Releaser.log.debug("After checking out " + new Date());
            Releaser.log.debug("Artefact downloaded from SVN");
            return true;
        }
        catch (SVNException e) {
            Releaser.log.error("Error checking out project." + e);
        }
        catch (Exception e2) {
            Releaser.log.debug("Error checking out project." + e2);
        }
        finally {
            manager.dispose();
        }
        Releaser.log.info("<-- Downloading from SVN " + url);
        return false;
    }
    
    private static void createDirectory(final String path) {
        DAVRepositoryFactory.setup();
        final SVNClientManager manager = SVNClientManager.newInstance();
        try {
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(Releaser.username, Releaser.password);
            manager.setAuthenticationManager(authManager);
            final SVNCommitClient svnCommitClient = manager.getCommitClient();
            final SVNURL svnURL = SVNURL.parseURIEncoded(path);
            final SVNURL[] urls = { svnURL };
            svnCommitClient.doMkDir(urls, "Creating Directory");
        }
        catch (Exception e) {
            Releaser.log.debug(e.toString());
        }
    }
    
    private static boolean commit(final File file) {
        DAVRepositoryFactory.setup();
        final SVNClientManager manager = SVNClientManager.newInstance();
        try {
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(Releaser.username, Releaser.password);
            manager.setAuthenticationManager(authManager);
            final SVNCommitClient svnCommitClient = manager.getCommitClient();
            svnCommitClient.doCommit(new File[] { file }, false, "Releaser " + Releaser.notCheckToken, (SVNProperties)null, (String[])null, false, false, SVNDepth.INFINITY);
        }
        catch (SVNException e) {
            Releaser.log.debug("Error checking out project." + e);
            return false;
        }
        finally {
            manager.dispose();
        }
        manager.dispose();
        return false;
    }
    
    private static String getFilefromSVN(final String repositoryURL, String filePath) {
        SVNRepository repository = null;
        final String file = null;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(repositoryURL));
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(Releaser.username, Releaser.password);
            repository.setAuthenticationManager(authManager);
            final ByteArrayOutputStream os = new ByteArrayOutputStream();

            final long num = repository.getFile(filePath, -1L, (SVNProperties)null, (OutputStream)os);
            final String aString = new String(os.toByteArray(), "UTF-8");
            return aString;
        }
        catch (Exception e) {
            Releaser.log.debug("Error getting file." + e);
            return file;
        }
    }
    
    public static List<Artefact> getArtefactOfFile(final String repositoryURL, String[] files, final String jiraIssue) {

        List<Artefact> artefacts = new ArrayList<>();

        for(String file: files) {
            if (file != null) {
                final String[] splits = file.split("/src/main");
                Releaser.log.debug(splits.toString());
                String url = getPomURL(file);
                if ((url != null) && (splits.length > 0)) {
                    final String pom = getFilefromSVN(repositoryURL, url);
                    final Artefact artefact = getArtefactFromString(pom);
                    Artefact jiraArtefactOfFile = JiraClient.getIssueKey(getProject(artefact.getGroupId()), artefact.getArtefactId(), artefact.getVersion().substring(0, artefact.getVersion().indexOf("-SNAPSHOT")));
                    if(Objects.nonNull(jiraArtefactOfFile)) {
                        artefacts.add(jiraArtefactOfFile);
                    }
                }
            }
        }
        return artefacts;
    }
    
    private static String getPomURL(String file){
    	log.debug("getPomURL->" + file);
    	String[] splits = null;
    	String url = null;
    	if (file != null) {
    		if (file.contains("/src/main")){    			
    			splits = file.split("/src/main");
    			if (splits.length>0){
    				url = String.valueOf(splits[0]) + "/pom.xml";
    			}
    		} else if (file.contains("/src/resources")){
    			splits = file.split("/src/resources");
    			if (splits.length>0){
    				url = String.valueOf(splits[0]) + "/pom.xml";
    			}
    			
    		} else if (file.contains("pom.xml")){
    				url = file;
    		} else {
    			int position = file.length();
    			boolean existPom = false;
    			String tentativePom = file;
    			
    			while (!existPom && position>-1){
    				
    				position = tentativePom.lastIndexOf("/");
    				if (position>-1){
	    				tentativePom = file.substring(0,position);
	    				log.debug(tentativePom + "/pom.xml");
	    				existPom = checkIfPathExist(tentativePom + "/pom.xml");
	    				if (existPom){
	    					url = tentativePom + "/pom.xml";
	    				} else {
	    					log.debug("Pom does not exist");
	    				}
    				}
    			}
    		}
    				           
    	}
    	    	
    	log.debug("getPomURL<-");
    	return url;
    	    	
    }
    
    public static int checkCommit(final String[] svnFiles, final String issueKey) {
        int result = 0;
        Releaser.log.info("Get the jira issue by key: " + issueKey);
        String message = new String();
        final Artefact jiraIssueArtefact = JiraClient.getIssueByKey(issueKey, true);
        if (jiraIssueArtefact == null) {
        	message = "There is not a Jira Issue in open status for the key " + issueKey;
        	System.err.println(message);
        	Releaser.log.info(message);
        	 
            result = 3;
        }
        else {
            List<Artefact> jiraArtefactOfFile = getArtefactOfFile(Releaser.repoURL, svnFiles, issueKey);
                if (CollectionUtils.isEmpty(jiraArtefactOfFile )) {
                	message = "There is not a Jira Artefact for " + jiraArtefactOfFile;
                	System.err.println(message);
                	Releaser.log.info(message);
                    result = 1;
                }
                else {
                    StringBuilder notLinkedArtifacts = new StringBuilder("\n");
                    for (Artefact artefact : jiraArtefactOfFile){
                        if (!jiraIssueArtefact.containsLinkedIssue(artefact.getJiraIssue())) {
                            notLinkedArtifacts.append(artefact.getJiraIssue() + "\n");
                        }
                    }

                    if(StringUtils.isNotBlank(notLinkedArtifacts)){
                        result = 2;
                        message = "The issue  " + jiraIssueArtefact.getJiraIssue() + " has not linked the artifacts: " + notLinkedArtifacts;
                        System.err.println(message.toString());
                        Releaser.log.info(message.toString());
                    }
                }
        }

        return result;
    }
    
    public static void writeModel(final File pomFile, final Model model) throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter(pomFile);
            final MavenXpp3Writer pomWriter = new MavenXpp3Writer();
            pomWriter.write(writer, model);
        }
        finally {
            IOUtil.close(writer);
        }
        IOUtil.close(writer);
    }
    
    private static String getArtifactInfo(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String artefactInfo = "";
        try {
            final Model model = mavenreader.read((Reader)new FileReader(pomfile));
            artefactInfo = String.valueOf(model.getGroupId()) + "." + model.getArtifactId() + "-" + model.getVersion();
        }
        catch (IOException | XmlPullParserException ex2) {
            
            Releaser.log.error(ex2.toString());
        }
        return artefactInfo;
    }
    
    private static String getArtefactVersion(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String version = "";
        try {
            final Model model = mavenreader.read((Reader)new FileReader(pomfile));
            version = model.getVersion();
        }
        catch (IOException | XmlPullParserException ex2) {            
            Releaser.log.error(ex2.toString());
        }
        return version;
    }
    
    private static Artefact getArtefactFromFile(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final Artefact artefact = new Artefact();
        try {
        	File pomfile = new File(file);
        	
        	InputStreamReader fis = new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8);
            final Model model = mavenreader.read(fis);
            artefact.setArtefactId(model.getArtifactId());
            artefact.setGroupId(model.getGroupId());
            artefact.setVersion(model.getVersion());
            if (model.getScm()!=null){
            	artefact.setScmURL(model.getScm().getDeveloperConnection());
            }
        }
        catch (IOException | XmlPullParserException ex2) {
        	 Releaser.log.error(ex2.toString());
        }
        return artefact;
    }
    
    private static Artefact getArtefactFromString(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final Artefact artefact = new Artefact();
        try {
            final InputStream stream = new ByteArrayInputStream(file.getBytes("UTF-8"));
            final Model model = mavenreader.read(stream);
            artefact.setArtefactId(model.getArtifactId());
            artefact.setGroupId(model.getGroupId());
            artefact.setVersion(model.getVersion());
            if (model.getScm()!=null){
            	artefact.setScmURL(model.getScm().getDeveloperConnection());
            }
        }
        catch (IOException | XmlPullParserException ex2) {
        	 Releaser.log.error(ex2.toString());
        }
        return artefact;
    }
    
    private static String getArtefactSCMURL(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String url = "";
        try {
            final Model model = mavenreader.read((Reader)new FileReader(pomfile));
            url = model.getScm().getDeveloperConnection();
        }
        catch (IOException | XmlPullParserException ex2) {
        	 Releaser.log.error(ex2.toString());
        }
        return url;
    }
    

    private static String getNextVersion(String version, String branchName) {
       log.debug("-->getNextVersion");
       String nextVersion = "";
       log.debug("Current Version: " + version);
       log.debug("BranchName: " + branchName);
       try {
         if (version.endsWith("-SNAPSHOT")) {
           int snapshotPosition = version.indexOf("-SNAPSHOT");
           version = version.substring(0, snapshotPosition);
         }
         if (branchName.endsWith("-SNAPSHOT")) {
           int snapshotPosition = branchName.indexOf("-SNAPSHOT");
           branchName = branchName.substring(0, snapshotPosition);
         }
         branchName = new StringBuilder(branchName).reverse().toString();
         int index = branchName.indexOf("-");
         if (index == -1) {
            nextVersion = incrementMiddle(version);
         } else {
           branchName = branchName.substring(0, index);
           branchName = new StringBuilder(branchName).reverse().toString();
           int position = branchName.toUpperCase().indexOf("X");
           if (position > -1) {
             if (position == 2) {
               int position2 = version.indexOf(".", position + 1);
               Integer num = Integer.valueOf(version.substring(position, position2));
               num = Integer.valueOf(num.intValue() + 1);
               nextVersion = String.valueOf(version.substring(0, position)) + num;
               nextVersion = String.valueOf(nextVersion) + version.substring(position2, version.length());
             }
             if ((position == 4) || (position == 5)) {
               int position2 = version.indexOf(".", position);
               position = (position2>-1)?position2+1:position;
               Integer num = Integer.valueOf(version.substring(position, version.length()));
               num = Integer.valueOf(num.intValue() + 1);
               nextVersion = String.valueOf(version.substring(0, position)) + num;
             }
           }
         }
       }
       catch (Exception e) {
         log.error(e.toString());
         log.info("The Next Version could not be discover automatically");
         nextVersion = "";
       }
       if (!nextVersion.equals("")) {
         nextVersion = String.valueOf(nextVersion) + "-SNAPSHOT";
       }

       log.debug("New Version " + nextVersion);
       log.debug("<--getNextVersion");
       return nextVersion;
}

    
    private static String incrementMiddle(final String version) {
        Releaser.log.debug("-->getNextVersion");
        String newVersion = "";
        try {
            final int position = version.indexOf(".");
            final int position2 = version.indexOf(".", position + 1);
            if (position == 1) {
                Integer num = Integer.valueOf(version.substring(position + 1, position2));
                ++num;
                newVersion = String.valueOf(version.substring(0, position)) + "." + num;
                newVersion = String.valueOf(newVersion) + version.substring(position2, version.length());
            }
        }
        catch (Exception e) {
            Releaser.log.error("Error incrementing version of: " + version);
        }
        Releaser.log.debug("<--getNextVersion");
        return newVersion;
    }
    
    public static void setUsername(final String username) {
        Releaser.username = username;
    }
    
    public static String getPassword() {
        return Releaser.password;
    }
    
    public static void setPassword(final String password) {
        Releaser.password = password;
    }
    
    public static String getToken(){
    		String notcheckTokenProperty = null;
    	    Properties prop = new Properties();
	        try {
				prop.load(Releaser.class.getClassLoader().getResourceAsStream("config.properties"));
				notcheckTokenProperty = prop.getProperty("notchecktoken");
			} catch (IOException e) {
				log.error(e.toString());
			}	        
    		return notcheckTokenProperty;
    	
    }

    private String buildLocalRepositoryPath(RepositoryTypeEnum repositoryTypeEnum){
        return Releaser.tempDir = "/tmp/" + repositoryTypeEnum.getRepositoryType() + "/";
    }
}
