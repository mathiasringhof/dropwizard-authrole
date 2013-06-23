package eu.ringhof.dropwizard;

import com.yammer.dropwizard.auth.basic.BasicCredentials;

public class BasicCredentialsWithRequiredRoles extends BasicCredentials {
    private final String[] requiredRoles;

    public BasicCredentialsWithRequiredRoles(String username, String password, String[] requiredRoles) {
        super(username, password);
        this.requiredRoles = requiredRoles;
    }

    public String[] getRequiredRoles() {
        return requiredRoles;
    }
}
