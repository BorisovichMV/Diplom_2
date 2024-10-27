import entities.User;
import helpers.RandomStringGenerator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import steps.UserSteps;

import java.util.ArrayList;
import java.util.List;

import static steps.UserSteps.createUser;
import static steps.UserSteps.rememberTokens;

public class BaseTest {
    User user;
    final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site/api";
    }

    @Before
    public void setup() {
        String email = RandomStringGenerator.generateEmail();
        String password = RandomStringGenerator.generatePassword();
        String username = RandomStringGenerator.generateUsername();
        this.user = new User(email, password, username);
    }

    @After
    public void tearDown() {
        this.createdUsers.forEach(UserSteps::deleteUser);
    }

}
