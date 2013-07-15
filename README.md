dropwizard-authrole
===================

*simple addon for [dropwizard's](http://dropwizard.codahale.com) [dropwizard-auth module](http://dropwizard.codahale.com/manual/auth/) that allows you to specify roles*

Based on the HTTP basic authentication of `dropwizard-auth`, it extends the `Auth` annotation with a `roles` parameter. These will be provided to `BasicCredentialsWithRequiredRoles`, which allows the `Authenticator` to process them in addition to username / password.
And by based I mean copied & extended a tiny little bit!

Usage
-----

Your `Authenticator` class needs to use the `BasicCredentialsWithRequiredRoles`:

    public class SampleAuthenticator implements Authenticator<BasicCredentialsWithRequiredRoles, User> {
        private HashMap<String, String> userDatabase;

        public SampleAuthenticator() {
            userDatabase = new HashMap<>();
            userDatabase.put("foo_user", "ROLE_FOO");
            userDatabase.put("bar_user", "ROLE_BAR");
        }

        @Override
        public Optional<User> authenticate(BasicCredentialsWithRequiredRoles credentials) throws AuthenticationException {
            // credentials.getRoles() contain the requested roles;
            // get the details for the User object and compare if the user specified correct credentials and meets the role needs
            String userRole = userDatabase.get(credentials.getUsername());
            if (userRole != null && Arrays.asList(credentials.getRoles()).contains(userRole))
                return Optional.of(new User(credentials.getUsername()));
            else
                return Optional.absent();
        }
    }

The resource uses the annotation `AuthWithRole` instead of `Auth`:

    @Path("/sample")
    @Produces(MediaType.APPLICATION_JSON)
    public class SampleResource {
        @GET
        public String getSample(@AuthWithRoles(roles = { "ROLE_FOO" }) User user) {
            // only users with ROLE_FOO will get through
            return "foobar";
        }
    }

Instead of using the `BasicAuthProvider`, add the `BasicAuthWithRolesProvider` to your environment:

    public void run(WorklogConfiguration configuration, Environment environment) throws Exception {
        environment.addProvider(new BasicAuthWithRolesProvider<>(new SampleAuthenticator(), "sample realm"));
        environment.addResource(new SampleResource());
    }

Build
-----

This project uses [gradle](http://www.gradle.org) as a build system. Either use:
* `gradle build` and copy the JAR from `build/libs`
* or even better use `gradle install` to utilize the maven plugin and install it into your local maven repository