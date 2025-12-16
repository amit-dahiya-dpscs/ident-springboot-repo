package md.dpscs.cch.iis.dto;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import java.util.stream.Collectors;

@Data
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private String username;
    private Collection<String> authorities;

    /**
     * Constructor that accepts the generic GrantedAuthority collection from Spring Security.
     * @param token The JWT token.
     * @param username The authenticated user's name.
     * @param authorities The collection of roles/authorities.
     */
    public AuthResponse(String token, String username, Collection<? extends GrantedAuthority> authorities) {
        this.token = token;
        this.username = username;

        // This stream correctly converts the Collection<? extends GrantedAuthority>
        // into a List<String> which matches the type of our 'authorities' field.
        if (authorities != null) {
            this.authorities = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }
    }
}