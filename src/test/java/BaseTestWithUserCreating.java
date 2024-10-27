import io.restassured.response.Response;
import org.junit.Before;

import static steps.UserSteps.createUser;
import static steps.UserSteps.rememberTokens;

public class BaseTestWithUserCreating extends BaseTest {
    @Before
    public void setup() {
        super.setup();
        Response response = createUser(this.user);
        rememberTokens(this.user, response.jsonPath());
        this.createdUsers.add(this.user);
    }
}
