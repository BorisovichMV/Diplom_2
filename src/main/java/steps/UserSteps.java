package steps;

import entities.User;
import io.qameta.allure.Step;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import models.UserLoginModel;
import models.UserRegistrationModel;

import static steps.RequestSteps.sendAuthorizedRequest;
import static steps.RequestSteps.sendRequest;

public class UserSteps {
    @Step("Запоминаем токены")
    public static void rememberTokens(User user, JsonPath jsonPath) {
        String accessToken = jsonPath.getString("accessToken");
        String refreshToken = jsonPath.getString("refreshToken");
        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
    }

    @Step("Отправляем запрос на создание пользователя")
    public static Response createUser(User user) {
        return createUser(user, false, false, false, false, 200);
    }

    @Step("Отправляем запрос на создание пользователя")
    public static Response createUser(User user, Boolean userExisting, Boolean emptyName, Boolean emptyEmail, Boolean emptyPassword, Integer statusCode) {
        if (userExisting) {
            return sendRequest("POST", UserRegistrationModel.fromUser(user), "/auth/register", statusCode);
        }
        UserRegistrationModel model = UserRegistrationModel.fromUser(user);
        if (emptyName) {
            model.eraseName();
        }
        if (emptyEmail) {
            model.eraseEmail();
        }
        if (emptyPassword) {
            model.erasePassword();
        }
        return sendRequest("POST", model, "/auth/register", statusCode);
    }

    @Step("Удаляем пользователя")
    public static void deleteUser(User user) {
        sendAuthorizedRequest(user, "DELETE", user, "/auth/user", 202);
    }

    @Step("Изменяем данные")
    public static Response changeUserData(User user, Boolean isAuthorizedRequest, Integer statusCode) {
        if (isAuthorizedRequest) {
            return sendAuthorizedRequest(user, "PATCH", UserRegistrationModel.fromUser(user), "/auth/user", statusCode);
        }
        return sendRequest("PATCH", UserRegistrationModel.fromUser(user), "/auth/user", statusCode);
    }
    @Step("Отправляем запрос на вход пользователя")
    public static Response loginUser(User user) {
        return loginUser(user, false, false, 200);
    }

    @Step("Отправляем запрос на вход пользователя")
    public static Response loginUser(User user, Boolean isForgotPassword, Boolean isForgotEmail, Integer statusCode) {
        UserLoginModel model = UserLoginModel.fromUser(user);
        if (isForgotPassword) {
            model.forgotPassword();
        }
        if (isForgotEmail) {
            model.forgotEmail();
        }
        return sendRequest("POST", model, "/auth/login", statusCode);
    }

}
