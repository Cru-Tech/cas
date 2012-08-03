/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.services.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.web.support.ArgumentExtractor;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.theme.AbstractThemeResolver;

/**
 * ThemeResolver to determine the theme for CAS based on the the host header or
 * on the service specified.  Includes the mobile-browser-detector that is part
 * of ServiceThemeResolver.
 * 
 * Themes are now resolved in the following way, with higher precedence first:
 * 1) The "theme" attribute stored in the session, if it exists and is not blank.
 * 2) The theme configured in the service registry, if the service is registered.
 * 3) A theme mapped from the host header, if the host header is matched to
 *    something configured in the hostHeaderThemes property.
 * 
 * The "isMobile" and "browserType" request attributes are still set by interrogating
 * the "User-Agent" header and matching the "mobileUserAgentPatterns" configuration.
 * 
 * The "theme" request attribute is still set based on the stored theme.
 * 
 * The hostHeaderThemes property configuration looks like this:
 * <property name="hostHeaderThemes">
 *   <map>
 *     <entry key="cas.domain1.com" value="domain1theme" />
 *     <entry key="cas.domain2.org" value="domain2theme" />
 *   </map>
 * </property>
 * 
 * @author Scott Battaglia
 * @author Nathan Kopp
 * @version $Revision$ $Date$
 * @since 3.0
 */
public final class ServiceThemeResolver extends AbstractThemeResolver
{

    /** The ServiceRegistry to look up the service. */
    private ServicesManager servicesManager;

    private List<ArgumentExtractor> argumentExtractors;

    private Map<Pattern, String> mobileUserAgentPatterns = new HashMap<Pattern, String>();
    private Map<String, String> hostHeaderThemes = new HashMap<String, String>();

    public String resolveThemeName(final HttpServletRequest request)
    {
        String theme = getDefaultThemeName();

        // override with host-header-based theme
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null)
        {
            String hostHeaderTheme = hostHeaderThemes.get(hostHeader.toLowerCase());
            if (hostHeaderTheme != null)
            {
                theme = hostHeaderTheme;
            }
        }

        // override with service-based theme
        if (this.servicesManager != null)
        {
            final Service service = WebUtils.getService(this.argumentExtractors, request);
            final RegisteredService rService = this.servicesManager.findServiceBy(service);

            if (rService != null && StringUtils.hasText(rService.getTheme())) theme = rService.getTheme();
        }

        // override with session-based theme
        String sessionTheme = (String)request.getSession(true).getAttribute("theme");
        if (StringUtils.hasText(sessionTheme))
        {
            theme = sessionTheme;
        }
        
        // detect mobile browser types using User-Agent header - store info as
        // request attributes
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null)
        {
            for (final Map.Entry<Pattern, String> entry : this.mobileUserAgentPatterns.entrySet())
            {
                if (entry.getKey().matcher(userAgent).matches())
                {
                    request.setAttribute("isMobile", "true");
                    request.setAttribute("browserType", entry.getValue());
                    break;
                }
            }
        }

        // store the theme as a request attribute
        request.setAttribute("theme", theme);
        return theme;
    }

    public void setThemeName(final HttpServletRequest request, final HttpServletResponse response,
                             final String themeName)
    {
        if(themeName==null || "theme".equals(themeName))
        {
            if(request.getSession(false)!=null)request.getSession(false).removeAttribute("theme");
        }
        request.getSession(true).setAttribute("theme", themeName);
    }

    public void setServicesManager(final ServicesManager servicesManager)
    {
        this.servicesManager = servicesManager;
    }

    public void setArgumentExtractors(final List<ArgumentExtractor> argumentExtractors)
    {
        this.argumentExtractors = argumentExtractors;
    }

    /**
     * Sets the map of mobile browsers. This sets a flag on the request called
     * "isMobile" and also provides the custom flag called browserType which can
     * be mapped into the theme.
     * <p>
     * Themes that understand isMobile should provide an alternative stylesheet.
     * 
     * @param mobileOverrides
     *            the list of mobile browsers.
     */
    public void setMobileBrowsers(final Map<String, String> mobileOverrides)
    {
        // initialize the overrides variable to an empty map
        this.mobileUserAgentPatterns = new HashMap<Pattern, String>();

        for (final Map.Entry<String, String> entry : mobileOverrides.entrySet())
        {
            this.mobileUserAgentPatterns.put(Pattern.compile(entry.getKey()), entry.getValue());
        }
    }

    public void setHostHeaderThemes(final Map<String, String> hostHeaderOverrides)
    {
        // initialize the overrides variable to an empty map
        this.hostHeaderThemes = new HashMap<String, String>();

        for (final Map.Entry<String, String> entry : hostHeaderOverrides.entrySet())
        {
            this.hostHeaderThemes.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }
}
