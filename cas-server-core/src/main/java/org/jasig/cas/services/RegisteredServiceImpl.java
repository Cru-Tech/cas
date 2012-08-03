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
package org.jasig.cas.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import org.jasig.cas.authentication.principal.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Mutable registered service that uses Various types of patterns for service matching.
 * 
 * The default, for backwards compatibility, is Ant-style matching.
 * 
 * To use another form of pattern matchibng, simply prefix the serviceId with one of
 * the following:
 * 
 *   regex: match using a regular expression
 *   domain: a single domain - any path or protocol will match
 *   domainlist: a comma-separated list of domains
 * 
 * Note that the matching algorithms probably should be made pluggable.  However, I do
 * not recommend using the inheritance model adopted for RegexRegisteredService, since
 * that model doesn't allow much runtime flexibility.  Instead, only the algorithm itself
 * should be pluggable, probably configured using a key/value map that ties prefix strings
 * with java classes that implement a particular matching method.  Note, though, that we cached
 * the Pattern object for the regex version here (to improve performance). That might
 * be more difficult (but certainly not impossible) with pluggable algorithms.  Also note
 * that it would be great for the algorithm identifier to be stored in a separate field.
 * I'd recommend reusing the expression_type database column that is currently used for the
 * inheritance descriminator.  However, there's currently nothing in the UI to easiy set
 * or change that value, so I'm sticking with the prefix for now.
 * 
 * @author Scott Battaglia
 * @author Marvin S. Addison
 * @author Nathan Kopp
 * @version $Revision$ $Date$
 * @since 3.1
 */
@Entity
@DiscriminatorValue("ant")
public class RegisteredServiceImpl extends AbstractRegisteredService {

    /** Unique Id for serialization. */
    private static final long serialVersionUID = -5906102762271197627L;

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();
    
    @Transient private Pattern serviceIdPattern = null;

    public void setServiceId(final String id) {
        this.serviceId = id;
    }

    public boolean matches(final Service service) {
        if(service==null) return false;
        if(this.serviceId==null) return false;
        if(this.serviceId.startsWith("regex:"))
        {
            if(serviceIdPattern==null) serviceIdPattern = Pattern.compile(this.serviceId.substring(6));
            return serviceIdPattern.matcher(service.getId().toLowerCase()).matches();
        }
        else if(this.serviceId.startsWith("domain:"))
        {
            String domain = this.serviceId.substring(7).toLowerCase();
            return checkDomainMatch(service, domain);
        }
        else if(this.serviceId.startsWith("domainlist:"))
        {
            String domainList = this.serviceId.substring(11).toLowerCase();
            for(String domain : domainList.split(","))
            {
                if (checkDomainMatch(service, domain)) return true;
            }
            return false;
        }
        else
        {
            return PATH_MATCHER.match(this.serviceId.toLowerCase(), service.getId().toLowerCase());
        }
    }
    
    private boolean checkDomainMatch(final Service service, String domain)
    {
        try
        {
            domain = domain.trim();
            if(domain.startsWith(".")) domain = domain.substring(1);
            URL url = new URL(service.getId().toLowerCase());
            return url.getHost().equals(domain) || url.getHost().endsWith("."+domain);
        }
        catch (MalformedURLException e)
        {
            return false;
        }
    }

    

    protected AbstractRegisteredService newInstance() {
        return new RegisteredServiceImpl();
    }
}
