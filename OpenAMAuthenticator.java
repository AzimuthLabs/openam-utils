
package sg.azlabs.openam.jira.seraph;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import org.apache.log4j.Category;

import com.atlassian.seraph.auth.DefaultAuthenticator;
import com.atlassian.jira.security.login.JiraSeraphAuthenticator;
import com.atlassian.seraph.util.RedirectUtils;

public class OpenAMAuthenticator extends JiraSeraphAuthenticator {

        private final static String OPENAMCOOKIENAME = "iPlanetDirectoryPro";
        private final static String OPENAMSESSVALID  = "valid";
        private final static String OPENAMUID        = "uid";

        private final static long serialVersionUID = 0L;

        private static final Category log = Category.getInstance(OpenAMAuthenticator.class);

        public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
                Principal user = null;

                try {
                        request.getSession(true);
                        log.info("Trying seamless Single Sign-on...");
                        String username = obtainUsername(request);
                        log.info("Got username = " + username);
                        if (username != null) {
                                if (request.getSession() != null && request.getSession().getAttribute(DefaultAuthenticator.LOGGED_IN_KEY) != null) {
                                        log.info("Session found; user already logged in");
                                        user = (Principal) request.getSession().getAttribute(DefaultAuthenticator.LOGGED_IN_KEY);
                                } else {
                                        user = getUser(username);
                                        log.info("Logged in via SSO, with User " + user);
                                        request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, user);
                                        request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);
                                }
                        } else {
                                String redirectUrl = RedirectUtils.getLoginUrl(request);
                                log.info("Username is null; redirecting to " + redirectUrl);
                                // user was not found, or not currently valid
//                              response.sendRedirect(redirectUrl);
                                return null;
                        }
                } catch (Exception e) // catch class cast exceptions
                {
                        log.warn("Exception: " + e, e);
                }
                return user;

        }
   
        private String obtainUsername(HttpServletRequest request) {
                String result = null;

                // Extract TokenID from iPlanetDirectoryPro cookie
                // This method will only be called after OpenAM Login Page authN
                Cookie[] cookies = request.getCookies();
                Cookie   cookie  = null;
                String   tokenID = null;

                if(cookies != null) {
                    for (int i = 0; i < cookies.length; i++) {
                        cookie=cookies[i];
                        if (cookie.getName().equals(OPENAMCOOKIENAME))
                            tokenID = cookie.getValue();
                    }
                }

                try {
                if (tokenID != null) {
                    // Pass TokenID to OpenAM to validate session
                    // OpenAM will return UID as part of the response
                    String url = "https://<am-server-url>/<am-context-uri>/json/sessions/"
                                 + tokenID +"/?_action=validate";
                    HttpPostConnection connection = new HttpPostConnection(url);
                    String response = connection.executeWithResponse();

                    try {
                        JSONObject jsonToken = new JSONObject(response);
                        if (jsonToken != null
                                && !jsonToken.isNull(OPENAMUID)) {
                            result = jsonToken.getString(OPENAMUID);
                            log.info("UID found from cookie: " + result);
                        }
                    } catch (JSONException je) {
                        log.warn("Not in JSON format" + je);
                    }
                } // tokenID

                } catch (IOException ioe) {
                        log.warn("IO Exception while connecting OpenAM " + ioe);
                }
                return result;
        }
}     
