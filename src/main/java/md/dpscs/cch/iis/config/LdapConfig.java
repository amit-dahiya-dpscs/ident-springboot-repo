package md.dpscs.cch.iis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;

@Configuration
@Profile("dev") // This class is ONLY active when the 'dev' profile is NOT running
public class LdapConfig {

    @Value("${spring.ldap.urls}") private String ldapUrls;
    @Value("${spring.ldap.base}") private String ldapBase;
    @Value("${spring.ldap.username}") private String ldapUsername;
    @Value("${spring.ldap.password}") private String ldapPassword;
    @Value("${ldap.user.search.base}") private String userSearchBase;
    @Value("${ldap.user.search.filter}") private String userSearchFilter;
    @Value("${ldap.group.search.base}") private String groupSearchBase;
    @Value("${ldap.group.search.filter}") private String groupSearchFilter;

    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrls);
        contextSource.setBase(ldapBase);
        contextSource.setUserDn(ldapUsername);
        contextSource.setPassword(ldapPassword);
        contextSource.setPooled(true);
        return contextSource;
    }

    @Bean
    public LdapUserDetailsService ldapUserDetailsService(LdapContextSource contextSource, LdapAuthoritiesPopulator authoritiesPopulator) {
        // Defines how to find a user in LDAP
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, contextSource);

        // Create the service
        LdapUserDetailsService service = new LdapUserDetailsService(userSearch, authoritiesPopulator);

        // Use our custom mapper to handle user details after they are found
        service.setUserDetailsMapper(new CustomUserDetailsMapper());

        return service;
    }


    @Bean
    public LdapAuthoritiesPopulator ldapAuthoritiesPopulator(LdapContextSource contextSource) {
        // This bean is correct and remains the same
        DefaultLdapAuthoritiesPopulator authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
        authoritiesPopulator.setGroupSearchFilter(groupSearchFilter);
        authoritiesPopulator.setRolePrefix("ROLE_");
        authoritiesPopulator.setConvertToUpperCase(true);
        return authoritiesPopulator;
    }

    /**
     * This bean provides the AuthenticationManager for PRODUCTION profiles.
     * This configuration is also correct and remains the same.
     */
    @Bean
    public AuthenticationManager authenticationManager(LdapContextSource contextSource, LdapAuthoritiesPopulator authoritiesPopulator) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, contextSource));

        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
        provider.setUserDetailsContextMapper(new CustomUserDetailsMapper());

        return new ProviderManager(provider);
    }
}