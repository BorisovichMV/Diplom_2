import java.security.SecureRandom;

public class RandomStringGenerator {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String AZBUKA = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЮЯабвгдежзийклмнопрстуфхцчшщъыьэюя";
    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomString(int length, String alphabet) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(alphabet.length());
            sb.append(alphabet.charAt(index));
        }
        return sb.toString();
    }

    public static String generateEmail() {
        return generateRandomString(5, ALPHABET) + generateRandomString(1, "_.-") + generateRandomString(5, ALPHABET + DIGITS) + "@yandex.ru";
    }

    public static String generatePassword() {
        return generateRandomString(10, ALPHABET + DIGITS);
    }

    public static String generateUsername() {
        return generateRandomString(7, ALPHABET);
    }

    public static String generateWrongHash(Integer hashLength) {
        return generateRandomString(hashLength, ALPHABET + DIGITS).toLowerCase();
    }
}
