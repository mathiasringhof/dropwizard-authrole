package eu.ringhof.dropwizard;

import com.google.common.base.Optional;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.auth.AuthenticationException;
import com.yammer.dropwizard.auth.Authenticator;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;

/**
 * A Jersey provider for Basic HTTP authentication with roles.
 *
 * @param <T>    the principal type.
 */
public class BasicAuthWithRolesProvider<T> implements InjectableProvider<AuthWithRoles, Parameter> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthWithRolesProvider.class);

    private static class BasicAuthWithRolesInjectable<T> extends AbstractHttpContextInjectable<T> {
        private static final String PREFIX = "Basic";
        private static final String HEADER_NAME = "WWW-Authenticate";
        private static final String HEADER_VALUE = PREFIX + " realm=\"%s\"";

        private final Authenticator<BasicCredentialsWithRequiredRoles, T> authenticator;
        private final String realm;
        private final boolean required;
        private final String[] roles;

        private BasicAuthWithRolesInjectable(Authenticator<BasicCredentialsWithRequiredRoles, T> authenticator, String realm, boolean required, String[] roles) {
            this.authenticator = authenticator;
            this.realm = realm;
            this.required = required;
            this.roles = roles;
        }

        @Override
        public T getValue(HttpContext c) {
            final String header = c.getRequest().getHeaderValue(HttpHeaders.AUTHORIZATION);
            try {
                if (header != null) {
                    final int space = header.indexOf(' ');
                    if (space > 0) {
                        final String method = header.substring(0, space);
                        if (PREFIX.equalsIgnoreCase(method)) {
                            final String decoded = B64Code.decode(header.substring(space + 1),
                                    StringUtil.__ISO_8859_1);
                            final int i = decoded.indexOf(':');
                            if (i > 0) {
                                final String username = decoded.substring(0, i);
                                final String password = decoded.substring(i + 1);
                                final BasicCredentialsWithRequiredRoles credentials = new BasicCredentialsWithRequiredRoles(username,
                                        password, roles);
                                final Optional<T> result = authenticator.authenticate(credentials);
                                if (result.isPresent()) {
                                    return result.get();
                                }
                            }
                        }
                    }
                }
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                LOGGER.debug("Error decoding credentials", e);
            } catch (AuthenticationException e) {
                LOGGER.warn("Error authenticating credentials", e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }

            if (required) {
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                        .header(HEADER_NAME,
                                String.format(HEADER_VALUE,
                                        realm))
                        .entity("Credentials are required to access this resource.")
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .build());
            }
            return null;
        }
    }

    private final Authenticator<BasicCredentialsWithRequiredRoles, T> authenticator;
    private final String realm;

    /**
     * Creates a new BasicAuthProvider with the given {@link com.yammer.dropwizard.auth.Authenticator} and realm.
     *
     * @param authenticator    the authenticator which will take the {@link com.yammer.dropwizard.auth.basic.BasicCredentials} and
     *                         convert them into instances of {@code T}
     * @param realm            the name of the authentication realm
     */
    public BasicAuthWithRolesProvider(Authenticator<BasicCredentialsWithRequiredRoles, T> authenticator, String realm) {
        this.authenticator = authenticator;
        this.realm = realm;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable<?> getInjectable(ComponentContext ic,
                                       AuthWithRoles a,
                                       Parameter c) {
        return new BasicAuthWithRolesInjectable<>(authenticator, realm, a.required(), a.roles());
    }
}
