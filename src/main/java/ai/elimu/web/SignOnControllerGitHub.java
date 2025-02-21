package ai.elimu.web;

import java.io.IOException;
import java.util.Calendar;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import ai.elimu.dao.ContributorDao;
import ai.elimu.model.contributor.Contributor;
import ai.elimu.model.v2.enums.Environment;
import ai.elimu.model.enums.Role;
import ai.elimu.util.ConfigHelper;
import ai.elimu.web.context.EnvironmentContextLoaderListener;
import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.ServiceBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * See https://github.com/organizations/elimu-ai/settings/applications and 
 * https://developer.github.com/v3/oauth/#web-application-flow
 */
@Controller
public class SignOnControllerGitHub {
    
    private static final String PROTECTED_RESOURCE_URL = "https://api.github.com/user";

    private OAuth20Service oAuth20Service;
    
    private final String secretState = "secret_" + new Random().nextInt(999_999);
    
    private Logger logger = LogManager.getLogger();
    
    @Autowired
    private ContributorDao contributorDao;
    
    @RequestMapping("/sign-on/github")
    public String handleAuthorization(HttpServletRequest request) throws IOException {
        logger.info("handleAuthorization");
        
        String clientId = "75ab65504795daf525f5";
        String clientSecret = "4f6eba014e102f0ed48334de77dffc12c4d1f1d6";
        String baseUrl = "http://localhost:8080/webapp";
        if (EnvironmentContextLoaderListener.env == Environment.TEST) {
            clientId = "57aad0f85f09ef18d8e6";
            clientSecret = ConfigHelper.getProperty("github.api.secret");
            baseUrl = "http://" + request.getServerName();
        } else if (EnvironmentContextLoaderListener.env == Environment.PROD) {
            clientId = "7018e4e57438eb0191a7";
            clientSecret = ConfigHelper.getProperty("github.api.secret");
            baseUrl = "http://" + request.getServerName();
        }

        oAuth20Service = new ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .defaultScope("user:email read:user") // See https://docs.github.com/en/free-pro-team@latest/developers/apps/scopes-for-oauth-apps
                .callback(baseUrl + "/sign-on/github/callback")
                .build(GitHubApi.instance());

        logger.info("Fetching the Authorization URL...");
        String authorizationUrl = oAuth20Service.createAuthorizationUrlBuilder()
                .state(secretState)
                .build();
        logger.info("Got the Authorization URL!");
		
        return "redirect:" + authorizationUrl;
    }
    
    @RequestMapping(value="/sign-on/github/callback", method=RequestMethod.GET)
    public String handleCallback(HttpServletRequest request, Model model) {
        logger.info("handleCallback");
        
        String state = request.getParameter("state");
        logger.debug("state: " + state);
        if (!secretState.equals(state)) {
            return "redirect:/sign-on?error=state_mismatch";
        } else {
            String code = request.getParameter("code");
            logger.debug("verifierParam: " + code);
            
            String responseBody = null;
            
            logger.info("Trading the Authorization Code for an Access Token...");
            try {
                OAuth2AccessToken accessToken = oAuth20Service.getAccessToken(code);
                logger.debug("accessToken: " + accessToken);
                logger.info("Got the Access Token!");

                // Access the protected resource
                OAuthRequest oAuthRequest = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
                oAuth20Service.signRequest(accessToken, oAuthRequest);
                Response response = oAuth20Service.execute(oAuthRequest);
                responseBody = response.getBody();
                logger.info("response.getCode(): " + response.getCode());
                logger.info("response.getBody(): " + responseBody);
            } catch (IOException | InterruptedException | ExecutionException ex) {
                logger.error(ex);
                return "redirect:/sign-on?error=" + ex.getMessage();
            }
            
            JSONObject jsonObject = new JSONObject(responseBody);
            logger.info("jsonObject: " + jsonObject);
            
            Contributor contributor = new Contributor();
            if (jsonObject.has("email") && !jsonObject.isNull("email")) {
                contributor.setEmail(jsonObject.getString("email"));
            }
            if (jsonObject.has("login")) {
                contributor.setUsernameGitHub(jsonObject.getString("login"));
            }
            if (jsonObject.has("id")) {
                Long idAsLong = jsonObject.getLong("id");
                String id = String.valueOf(idAsLong);
                contributor.setProviderIdGitHub(id);
            }
            if (jsonObject.has("avatar_url")) {
                contributor.setImageUrl(jsonObject.getString("avatar_url"));
            }
            if (jsonObject.has("name") && !jsonObject.isNull("name")) {
                String name = jsonObject.getString("name");
                String[] nameParts = name.split(" ");
                String firstName = nameParts[0];
                logger.info("firstName: " + firstName);
                contributor.setFirstName(firstName);
                if (nameParts.length > 1) {
                    String lastName = nameParts[nameParts.length - 1];
                    logger.info("lastName: " + lastName);
                    contributor.setLastName(lastName);
                }
            }

            // Look for existing Contributor with matching e-mail address
            Contributor existingContributor = contributorDao.read(contributor.getEmail());
            if (existingContributor == null) {
                // Look for existing Contributor with matching GitHub id
                existingContributor = contributorDao.readByProviderIdGitHub(contributor.getProviderIdGitHub());
            }
            logger.info("existingContributor: " + existingContributor);
            if (existingContributor == null) {
                // Store new Contributor in database
                contributor.setRegistrationTime(Calendar.getInstance());
                contributor.setRoles(new HashSet<>(Arrays.asList(Role.CONTRIBUTOR)));
                if (contributor.getEmail() == null) {
                    // Ask the Contributor to add her e-mail manually
                    request.getSession().setAttribute("contributor", contributor);
                    new CustomAuthenticationManager().authenticateUser(contributor);
                    return "redirect:/content/contributor/add-email";
                }
                contributorDao.create(contributor);
            } else {
                // Contributor already exists in database
                // Update existing contributor with latest values fetched from provider
                if (StringUtils.isNotBlank(contributor.getUsernameGitHub())) {
                    existingContributor.setUsernameGitHub(contributor.getUsernameGitHub());
                }
                if (StringUtils.isNotBlank(contributor.getProviderIdGitHub())) {
                    existingContributor.setProviderIdGitHub(contributor.getProviderIdGitHub());
                }
                if (StringUtils.isNotBlank(contributor.getImageUrl())) {
                    existingContributor.setImageUrl(contributor.getImageUrl());
                }
                if (StringUtils.isNotBlank(contributor.getFirstName())) {
                    existingContributor.setFirstName(contributor.getFirstName());
                }
                if (StringUtils.isNotBlank(contributor.getLastName())) {
                    existingContributor.setLastName(contributor.getLastName());
                }
                contributorDao.update(existingContributor);
                
                contributor = existingContributor;
            }

            // Authenticate
            new CustomAuthenticationManager().authenticateUser(contributor);

            // Add Contributor object to session
            request.getSession().setAttribute("contributor", contributor);
            
            return "redirect:/content";
        }
    }
}
