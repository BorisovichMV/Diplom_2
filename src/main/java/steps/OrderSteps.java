package steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entities.Ingredient;
import entities.Order;
import entities.User;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import models.OrderModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static steps.RequestSteps.sendAuthorizedRequest;
import static steps.RequestSteps.sendRequest;

public class OrderSteps {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Step("Выбираем два разных ингредиента")
    public static void chooseIngredients(List<Ingredient> ingredients) {
        Response response = sendRequest("GET", null, "/ingredients", 200);
        List<Ingredient> allIngredients = objectMapper.convertValue(response.jsonPath().getList("data"), new TypeReference<List<Ingredient>>() { });

        int index1 = ThreadLocalRandom.current().nextInt(allIngredients.size());
        int index2;
        do {
            index2 = ThreadLocalRandom.current().nextInt(allIngredients.size());
        } while (index1 == index2);

        ingredients.add(allIngredients.get(index1));
        ingredients.add(allIngredients.get(index2));
    }

    @Step("Получаем ингредиенты")
    public static void fetchIngredients(List<Ingredient> ingredients) {
        Response response = sendRequest("GET", null, "/ingredients", 200);
        List<Ingredient> allIngredients = objectMapper.convertValue(response.jsonPath().getList("data"), new TypeReference<List<Ingredient>>() { });
        ingredients.addAll(allIngredients);
    }

    @Step("Создаём заказы")
    public static void createOrders(User user, List<Ingredient> ingredients, List<Order> orders, Integer ordersCount) {
        for (int i = 0; i < ordersCount; i++) {
            createOrder(user, ingredients, orders);
        }
    }

    @Step("Создаём заказ")
    public static void createOrder(User user, List<Ingredient> ingredients, List<Order> orders) {
        List<Ingredient> orderIngredients = new ArrayList<>();
        int index1 = ThreadLocalRandom.current().nextInt(ingredients.size());
        int index2;
        do {
            index2 = ThreadLocalRandom.current().nextInt(ingredients.size());
        } while (index1 == index2);

        orderIngredients.add(ingredients.get(index1));
        orderIngredients.add(ingredients.get(index2));
        Order order = new Order(orderIngredients);
        sendAuthorizedRequest(user, "POST", OrderModel.fromOrder(order), "/orders", 200);
        orders.add(order);
    }

}
