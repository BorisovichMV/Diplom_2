import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrderModel {
    private final List<String> ingredients = new ArrayList<>();

    public static OrderModel fromOrder(Order order) {
        OrderModel model = new OrderModel();
        order.getIngredients().forEach(item -> model.ingredients.add(item.get_id()));
        return model;
    }

    public static OrderModel badModel(List<Ingredient> ingredients, Integer ingredientCount) {
        OrderModel model = new OrderModel();
        List<String> rightIngredientHashes = ingredients.stream()
                .map(Ingredient::get_id)
                .collect(Collectors.toList());
        Integer hashLength = rightIngredientHashes.get(0).length();
        while (model.ingredients.size() < ingredientCount) {
            String hash;
            do {
                hash = RandomStringGenerator.generateWrongHash(hashLength);
            } while (rightIngredientHashes.contains(hash));
            model.ingredients.add(hash);
        }
        return model;
    }

    public List<String> getIngredients() {
        return ingredients;
    }
}
