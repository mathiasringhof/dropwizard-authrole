package eu.ringhof.dropwizard;

import com.yammer.dropwizard.auth.basic.BasicCredentials;

public class BasicCredentialsWithRequiredRoles extends BasicCredentials {
    private final String[] roles;

    public BasicCredentialsWithRequiredRoles(String username, String password, String[] roles) {
        super(username, password);
        this.roles = roles;
    }

    public String[] getRoles() {
        return roles;
    }
}
