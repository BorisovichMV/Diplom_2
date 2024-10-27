package steps;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import entities.User;

public class RequestSteps {

    @Step("Отправляем запрос")
    public static Response sendRequest(String method, Object obj, String uri, Integer statusCode) {
        if (method.equals("DELETE") || method.equals("GET")) {
            return RestAssured.given()
                    .request(method, uri)
                    .then()
                    .log().body()
                    .statusCode(statusCode)
                    .extract().response();
        }
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .body(obj)
                .log().body()
                .when()
                .request(method, uri)
                .then()
                .log().body()
                .statusCode(statusCode)
                .extract().response();
    }

    @Step("Отправляем запрос с авторизацией")
    public static Response sendAuthorizedRequest(User user, String method, Object obj, String uri, Integer statusCode) {
        if (method.equals("DELETE") || method.equals("GET")) {
            return RestAssured.given()
                    .header("Authorization", user.getAccessToken())
                    .when()
                    .request(method, uri)
                    .then()
                    .log().body()
                    .statusCode(statusCode)
                    .extract().response();
        }
        return RestAssured.given()
                .header("Authorization", user.getAccessToken())
                .header("Content-Type", "application/json")
                .body(obj)
                .log().body()
                .when()
                .request(method, uri)
                .then()
                .log().body()
                .statusCode(statusCode)
                .extract().response();
    }
}
