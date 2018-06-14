package org.agrsw.mavenreleaser;

import org.springframework.boot.autoconfigure.*;
import org.slf4j.*;
import org.apache.http.*;
import org.springframework.http.client.support.*;
import org.springframework.util.*;
import org.springframework.http.client.*;
import org.springframework.http.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.*;
import java.util.*;

@SpringBootApplication
public class JiraClient
{
    private static final Logger log;
    private static String apiURL;
    public static String userName;
    public static String password;
    public static String jiraURL;
    public static String hostName;
    public static int port;
    
    static {
        log = LoggerFactory.getLogger((Class)JiraClient.class);
        JiraClient.apiURL = "";
        JiraClient.userName = "XXXX";
        JiraClient.password = "XXXX.";
        JiraClient.jiraURL = "http://192.168.10.21:8080/rest/api/2/";
        JiraClient.hostName = "192.168.10.21";
        JiraClient.port = 8080;
    }
    
      /*public static void main(final String[] args) {
    	   
    	     String jiraUser ="XXXX";
    	    String jiraPassword="XXX.";
    	    JiraClient.setUserName(jiraUser);
    	    JiraClient.setPassword(jiraPassword);    	  
    	    JiraClient.createVersion("MERCURY","borrame36", "version para borrar");
       }
    	   /*        final String desc = "Santander ES Products Layer - Loans Business **  ** *JENKINS:* ** http://192.168.10.4:8082/view/Branches/job/Loans%20Business%201.24.X/ **  ** *SVN BRANCH:* ** http://192.168.10.2/svn/mercury/branches/development/customers/bancosantander/spain/santandercomex/branches/products/loans/business/loans.business-1.24.X **  ** *SVN TAG:* ** http://192.168.10.2/svn/mercury/tags/releases/customers/bancosantander/spain/santandercomex/products/loans/business";
        final Artefact art = getIssueByKey("BANORTE-2966", false);
        createIssue("SANESPBACK", "loans.business-comex-1.24.34", desc, "loans.business-comex", "1.24.33", "1.84.34");
        closeIssue("MERCURY-11660");
        final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
        final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
        final ResponseEntity<Object> obj = (ResponseEntity<Object>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "search?jql=project=MERCURY AND issuetype = \"Artefacto Maven\" AND component=test  AND fixVersion = 1.0.0 AND status in (Open, \"In Progress\", Reopened)", HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
        HashMap map = (HashMap)obj.getBody();
        final ArrayList issues = (ArrayList) map.get("issues");
        final Map issue = (Map) issues.get(0);
        final Map fields = (Map) issue.get("fields");
        final String key = (String) issue.get("key");
        final ResponseEntity<Object> obj2 = (ResponseEntity<Object>)restTemplate.exchange("http://jira.mercury-tfs.com/rest/api/2/issue/MERCURY-11660/transitions", HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
        map = (HashMap)obj2.getBody();
        final ArrayList<Map> transitions = (ArrayList<Map>) map.get("transitions");
        final String json = "{\"fields\": {\"assignee\":{\"name\":\"ernesto.acosta\"}}}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = (HttpEntity<String>)new HttpEntity((Object)json, (MultiValueMap)headers);
        final String jsonTransition = "{\"transition\": {\"id\": \"5\"}}";
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        entity = (HttpEntity<String>)new HttpEntity((Object)jsonTransition, (MultiValueMap)headers);
        final ResponseEntity<String> loginResponse = (ResponseEntity<String>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "issue/MERCURY-11660/transitions", HttpMethod.POST, (HttpEntity)entity, (Class)String.class, new Object[0]);
        final JSONObject json2 = new JSONObject();
        final String jsonCreate = "{\"fields\": {\"project\": {\"key\": \"MERCURY\"}, \"summary\": \"something's wrong\",\"issuetype\": {\"name\": \"Error\"}}}";
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        entity = (HttpEntity<String>)new HttpEntity((Object)jsonCreate, (MultiValueMap)headers);
        restTemplate.exchange("http://jira.mercury-tfs.com/rest/api/2/issue/", HttpMethod.POST, (HttpEntity)entity, (Class)String.class, new Object[0]);
        System.out.println("");
    }*/
    
    public static Artefact getIssueKey(final String project, final String component, final String version) {
        String key = null;
        Artefact artefact = null;
        String description = null;
        JiraClient.log.info("->getIssueKey: " + project + "-" + component + "-" + version);
        try {
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final String queryString = String.valueOf(JiraClient.jiraURL) + "search?jql=project=" + project + " AND issuetype = \"Artefacto Maven\" AND component=" + component + " AND fixVersion = " + version + " AND status in (Open, \"In Progress\", Reopened)";
            JiraClient.log.debug(queryString);
            final ResponseEntity<Object> obj = (ResponseEntity<Object>)restTemplate.exchange(queryString, HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
            if (obj != null) {
                if (obj.getStatusCode().is2xxSuccessful()) {
                    final HashMap map = (HashMap)obj.getBody();
                    Map fields = null;
                    final ArrayList issues = (ArrayList) map.get("issues");
                    if (issues != null && issues.size() > 0) {
                        final Map issue = (Map) issues.get(0);
                        key = (String) issue.get("key");
                        fields = (Map) issue.get("fields");
                        description = (String) fields.get("description");
                        artefact = new Artefact();
                        artefact.setJiraIssue(key);
                        artefact.setDescription(description);
                    }
                    else {
                        JiraClient.log.warn("Issue Not Found");
                    }
                }
                else {
                    JiraClient.log.error("Error in request to jira: " + obj.getStatusCodeValue() + ":" + obj.getBody());
                }
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                JiraClient.log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                JiraClient.log.error(e.toString());
            }
        }
        JiraClient.log.info("<-getIssueKey: " + project + "-" + component + "-" + version);
        return artefact;
    }
    
    public static Artefact getIssueByKey(String key, final boolean unresolved) {
        Artefact artefact = null;
        String description = null;
        JiraClient.log.info("->getIssueByKey: " + key);
        try {
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            String queryString = String.valueOf(JiraClient.jiraURL) + "search?jql=key=" + key;
            if (unresolved) {
                queryString = String.valueOf(queryString) + " and  resolution = Unresolved";
            }
            JiraClient.log.debug(queryString);
            final ResponseEntity<Object> obj = (ResponseEntity<Object>)restTemplate.exchange(queryString, HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
            if (obj != null) {
                if (obj.getStatusCode().is2xxSuccessful()) {
                    final HashMap map = (HashMap)obj.getBody();
                    Map fields = null;
                    List<Map> issuelinks = null;
                    final ArrayList issues = (ArrayList) map.get("issues");
                    if (issues != null && issues.size() > 0) {
                        final Map issue = (Map) issues.get(0);
                        key = (String) issue.get("key");
                        fields = (Map) issue.get("fields");
                        description = (String) fields.get("description");
                        artefact = new Artefact();
                        artefact.setJiraIssue(key);
                        artefact.setDescription(description);
                        issuelinks = (List) fields.get("issuelinks");
                        if (issuelinks != null && issuelinks.size() > 0) {
                            for (final Map<String,Map> issueMap : issuelinks) {
                                final Artefact issueLinked = new Artefact();
                                if ((issueMap.get("outwardIssue") != null) && (issueMap.get("outwardIssue").get("key") != null)) {
                                    issueLinked.setJiraIssue((String)issueMap.get("outwardIssue").get("key"));
                                    artefact.getIssueslinked().add(issueLinked);
                                }
                                else if (issueMap.get("inwardIssue") != null && issueMap.get("inwardIssue").get("key") != null) {
                                    issueLinked.setJiraIssue((String)issueMap.get("inwardIssue").get("key"));
                                    artefact.getIssueslinked().add(issueLinked);
                                }
                                else {
                                    JiraClient.log.error("Cannot get issue key from " + issueMap.toString());
                                }
                            }
                        }
                    }
                    else {
                        JiraClient.log.warn("Issue Not Found");
                    }
                }
                else {
                    JiraClient.log.error("Error in request to jira: " + obj.getStatusCodeValue() + ":" + obj.getBody());
                }
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                JiraClient.log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                JiraClient.log.error(e.toString());
            }
        }
        JiraClient.log.info("<-getIssueByKey: " + key);
        return artefact;
    }
    
    public static String closeIssue(final String key) {
        String result = key;
        JiraClient.log.info("->closeIssue: " + key);
        try {
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final String queryString = String.valueOf(JiraClient.jiraURL) + "issue/" + key + "/transitions";
            JiraClient.log.debug(queryString);
            final ResponseEntity<Object> obj2 = (ResponseEntity<Object>)restTemplate.exchange(queryString, HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
            final Map map = (HashMap)obj2.getBody();
            final ArrayList<Map> transitions = (ArrayList<Map>)map.get("transitions");
            String name = "";
            String actionId = null;
            for (final Map transition : transitions) {
                name = (String) transition.get("name");
                if (name.contains("Cerrar") || name.contains("Close")) {
                    actionId = (String) transition.get("id");
                }
            }
            if (actionId != null) {
                final String jsonTransition = "{\"transition\": {\"id\": \"" + actionId + "\"}}";
                HttpHeaders headers = new HttpHeaders();
                headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = (HttpEntity<String>)new HttpEntity((Object)jsonTransition, (MultiValueMap)headers);
                entity = (HttpEntity<String>)new HttpEntity((Object)jsonTransition, (MultiValueMap)headers);
                final ResponseEntity<String> response = (ResponseEntity<String>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "issue/" + key + "/transitions", HttpMethod.POST, (HttpEntity)entity, (Class)String.class, new Object[0]);
                if (response != null) {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        JiraClient.log.info("Tarea cerrada correctamente: " + key);
                    }
                }
                else {
                    result = null;
                    JiraClient.log.error("Error al cerrar la tarea: " + response.getStatusCodeValue() + ":" + (String)response.getBody());
                }
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                JiraClient.log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                JiraClient.log.error(e.toString());
            }
        }
        JiraClient.log.info("<-closeIssue: " + key);
        return result;
    }
    
    public static String createIssue(final String project, final String summary, String descripcion, final String component, final String currentVersion, final String fixVersion) {
    	String logMessage = "createIssue:";
    	if (project!=null){
    		logMessage += " project: "+ project;
    	} 
    	if (summary!=null){
    		logMessage += " summary: " + summary;
    	}
    	if (descripcion!=null){
    		logMessage += " descripcion: "+ descripcion;
    	} else {
    		descripcion = "";
    	}
    	if (component!=null){
    		logMessage += " component: " + component;
    	}
    	if (currentVersion!=null){
    		logMessage += " currentVersion: " + currentVersion;
    	}
    	if (fixVersion!=null){
    		logMessage += " fixVersion: "+ fixVersion;
    	}
        JiraClient.log.info("-> " + logMessage);
        String key = null;
        
        
        try {
            final String fixDescripcion = fixCarrierReturn(descripcion);
            final String jsonCreate = "{\"fields\": {\"project\":{\"key\": \"" + project + "\"},\"summary\": \"" + summary + "\", \"fixVersions\" : [  {\"name\" : \"" + fixVersion + "\"}],\"versions\" : [  {\"name\" : \"" + currentVersion + "\"}],\"description\": \"" + fixDescripcion + "\", \"components\": [{\"name\":\"" + component + "\"}],\"issuetype\": {\"name\": \"Artefacto Maven\"}}}";
            HttpHeaders headers = new HttpHeaders();
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);           
            final HttpEntity<String> entity = (HttpEntity<String>)new HttpEntity((Object)jsonCreate, (MultiValueMap)headers);
            JiraClient.log.info(jsonCreate);
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final ResponseEntity<Object> response = (ResponseEntity<Object>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "issue/", HttpMethod.POST, (HttpEntity)entity, (Class)Object.class, new Object[0]);
            if (response != null) {
                if (response.getStatusCode().is2xxSuccessful()) {
                    final Map map = (HashMap)response.getBody();
                    key = (String) map.get("key");
                    JiraClient.log.info("Tarea creada correctamente: " + key);
                }
            }
            else {
                JiraClient.log.error("Error al cerrar la tarea: " + response.getStatusCodeValue() + ":" + response.getBody());
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                JiraClient.log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                JiraClient.log.error(e.toString());
            }
        }
        JiraClient.log.info("<- " + logMessage);
        return key;
    }
    
    public static String createVersion(final String project, final String version, String descripcion) {
    	String logMessage = "createVersion";
    	if (project!=null){
    		logMessage += " project: "+ project;
    	} 
    	
    	if (project!=null){
    		logMessage += " version: "+ version;
    	} 
    	
    	if (descripcion!=null){
    		logMessage += " descripcion: "+ descripcion;
    	} else {
    		descripcion = "";
    	}
    	    
        JiraClient.log.info("-> " + logMessage);
        String key = null;
        
        
        try {
            final String fixDescripcion = fixCarrierReturn(descripcion);
            final String jsonCreate = "{\"description\":\""  + descripcion + "\",\"name\":\"" + version + "\",\"project\":\"" + project +  "\"}";            
            HttpHeaders headers = new HttpHeaders();
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);           
            final HttpEntity<String> entity = (HttpEntity<String>)new HttpEntity((Object)jsonCreate, (MultiValueMap)headers);
            JiraClient.log.info(jsonCreate);
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final ResponseEntity<Object> response = (ResponseEntity<Object>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "version/", HttpMethod.POST, (HttpEntity)entity, (Class)Object.class, new Object[0]);
            if (response != null) {
                if (response.getStatusCode().is2xxSuccessful()) {
                    final Map map = (HashMap)response.getBody();
                    key = (String) map.get("name");
                    JiraClient.log.info("Version creada correctamente: " + key);
                }
            }
            else {
                JiraClient.log.error("Error al cerrar la tarea: " + response.getStatusCodeValue() + ":" + response.getBody());
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                JiraClient.log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                JiraClient.log.error(e.toString());
            }
        }
        JiraClient.log.info("<- " + logMessage);
        return key;
    }
    
    
    public static String getUserName() {
        return JiraClient.userName;
    }
    
    public static void setUserName(final String userName) {
        JiraClient.userName = userName;
    }
    
    public static String getPassword() {
        return JiraClient.password;
    }
    
    public static void setPassword(final String password) {
        JiraClient.password = password;
    }
    
    private static String fixCarrierReturn(final String text) {
        String text2 = text.replaceAll("\r", " ** ");
        text2 = text2.replaceAll("\n", " ** ");
        return text2;
    }
}
