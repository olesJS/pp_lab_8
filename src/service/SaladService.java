package service;

import saving.ProductRepository;
import saving.SaladRepository;
import products.IProduct;
import products.Vegetable;
import salad.Salad;
import salad.SaladIngredient;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Отримувач (Receiver) для всієї логіки, пов'язаної з каталогом салатів
 */
public class SaladService {
    private static final Logger logger = LogManager.getLogger(SaladService.class);

    private final SaladRepository saladRepository;
    private final ProductRepository productRepository;
    private final Scanner scanner;

    public SaladService(SaladRepository sRepo, ProductRepository pRepo, Scanner scanner) {
        this.saladRepository = sRepo;
        this.productRepository = pRepo;
        this.scanner = scanner;
        logger.info("SaladService ініціалізовано. Репозиторії: SaladRepo={}, ProductRepo={}",
                sRepo.getClass().getSimpleName(), pRepo.getClass().getSimpleName());
    }

    private double calculateTotalCalories(Salad salad) {
        logger.debug("Розрахунок загальної калорійності для салату '{}'", salad.getName());
        return salad.getIngredients().stream()
                .mapToDouble(SaladIngredient::getTotalCalories)
                .sum();
    }

    private double calculateTotalWeight(Salad salad) {
        logger.debug("Розрахунок загальної ваги для салату '{}'", salad.getName());
        return salad.getIngredients().stream()
                .mapToDouble(SaladIngredient::getWeightInGrams)
                .sum();
    }

    /**
     * Створює новий, порожній салат та зберігає його.
     */
    public void createNewSalad() {
        logger.info("Запущено операцію: Створення нового салату.");
        System.out.print("\n--- Створення салату ---\nВведіть назву нового салату: ");
        String name = scanner.nextLine().trim();

        if (name.isEmpty()) {
            logger.warn("Створення салату скасовано: Назва порожня.");
            System.out.println("Назва не може бути порожньою.");
            return;
        }
        if (saladRepository.getSaladByName(name).isPresent()) {
            logger.warn("Створення салату скасовано: Рецепт '{}' вже існує.", name);
            System.out.println("Рецепт '" + name + "' вже існує.");
            return;
        }

        Salad newSalad = new Salad(name);
        saladRepository.saveSalad(newSalad);

        System.out.println("Салат '" + name + "' створено та збережено.");
        logger.info("Салат '{}' успішно створено та збережено.", name);

        handleEditLoop(newSalad);
    }

    /**
     * Відображає список усіх збережених рецептів.
     */
    public void viewSalads() {
        logger.info("Користувач переглядає список усіх рецептів.");
        List<Salad> salads = saladRepository.getAllSalads();
        if (salads.isEmpty()) {
            logger.info("Список рецептів порожній.");
            System.out.println("Наразі немає збережених рецептів.");
            return;
        }

        logger.info("Знайдено {} рецептів для відображення.", salads.size());
        System.out.println("\n--- Наявні рецепти салатів ---");
        for (int i = 0; i < salads.size(); i++) {
            Salad salad = salads.get(i);
            double cals = calculateTotalCalories(salad);
            System.out.printf("%d. %s (Загалом: %.2f ккал)\n",
                    i + 1, salad.getName(), cals);
        }
    }

    /**
     * Видаляє рецепт за назвою.
     */
    public void removeSalad() {
        logger.info("Запущено операцію: Видалення рецепту.");
        System.out.print("\nВведіть назву салату для видалення: ");
        String name = scanner.nextLine().trim();
        logger.debug("Назва для видалення: '{}'", name);

        if (saladRepository.getSaladByName(name).isPresent()) {
            saladRepository.deleteSalad(name);
            System.out.println("Рецепт '" + name + "' успішно видалено.");
            logger.info("Рецепт '{}' успішно видалено.", name);
        } else {
            System.out.println("Рецепт '" + name + "' не знайдено.");
            logger.error("Помилка видалення: Рецепт '{}' не знайдено.", name);
        }
    }

    /**
     * Детальний перегляд складу рецепту.
     */
    public void viewSaladRecipe() {
        logger.info("Запущено операцію: Детальний перегляд рецепту.");
        System.out.print("\nВведіть назву салату для детального перегляду: ");
        String name = scanner.nextLine().trim();
        logger.debug("Назва рецепту для перегляду: '{}'", name);

        Optional<Salad> saladOpt = saladRepository.getSaladByName(name);

        if (saladOpt.isPresent()) {
            Salad salad = saladOpt.get();
            double totalCals = calculateTotalCalories(salad);
            double totalWeight = calculateTotalWeight(salad);

            logger.info("Відкриття рецепту '{}'. Загальна вага: {} г, Калорійність: {} ккал",
                    name, String.format("%.2f", totalWeight), String.format("%.2f", totalCals));

            System.out.println("\n--- Рецепт: " + salad.getName().toUpperCase() + " ---");
            System.out.println("Загальна калорійність: " + String.format("%.2f ккал", totalCals));
            System.out.println("Вага порції салату: " + String.format("%.2f г", totalWeight));
            System.out.println("Інгредієнти:");

            if (salad.getIngredients().isEmpty()) {
                System.out.println(" (Салат порожній)");
                return;
            }

            for (SaladIngredient ing : salad.getIngredients()) {
                System.out.printf(" - %s (%.1f г) | Базова ккал/100г: %.1f | Ккал у порції: %.2f\n",
                        ing.getConsumable().getName(),
                        ing.getWeightInGrams(),
                        ing.getConsumable().getCaloriesPer100g(),
                        ing.getTotalCalories());

                Optional<String> tip = ing.getConsumable().getCookingTip();

                if (tip.isPresent()) {
                    System.out.println("    Порада: " + tip.get());
                }
            }
        } else {
            System.out.println("Рецепт '" + name + "' не знайдено.");
            logger.error("Помилка перегляду: Рецепт '{}' не знайдено.", name);
        }
    }

    /**
     * ДОПОМІЖНИЙ МЕТОД: Головний цикл додавання/видалення.
     * Працює з об'єктом Salad, переданим як аргумент.
     */
    private void handleEditLoop(Salad salad) {
        boolean editing = true;

        while (editing) {
            System.out.println("\n--- Редагування: " + salad.getName().toUpperCase() + " ---");

            System.out.println("1. Додати інгредієнт");
            System.out.println("2. Видалити інгредієнт");
            System.out.println("0. Зберегти зміни та вийти");
            System.out.print("Ваш вибір: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    handleAddIngredient(salad);
                    break;
                case "2":
                    handleRemoveIngredient(salad);
                    break;
                case "0":
                    saladRepository.saveSalad(salad);
                    logger.info("Зміни в рецепті '{}' збережено.", salad.getName());
                    System.out.println("Зміни в рецепті '" + salad.getName() + "' збережено.");
                    editing = false;
                    break;
                default:
                    logger.warn("Невірний вибір дії редагування: {}", choice);
                    System.out.println("Невірний вибір.");
            }
        }
    }

    public void sortIngredients() {
        logger.info("Запущено операцію: Сортування інгредієнтів.");
        System.out.print("\nВведіть назву салату для сортування інгредієнтів: ");
        String name = scanner.nextLine().trim();
        Optional<Salad> saladOpt = saladRepository.getSaladByName(name);

        if (saladOpt.isEmpty()) {
            logger.error("Сортування скасовано: Рецепт '{}' не знайдено.", name);
            System.out.println("Рецепт '" + name + "' не знайдено.");
            return;
        }

        Salad salad = saladOpt.get();

        System.out.println("Сортувати за: 1. Назва | 2. Вага | 3. Ккал у порції");
        System.out.print("Ваш вибір: ");
        String choice = scanner.nextLine();
        logger.debug("Критерій сортування: {}", choice);

        Comparator<SaladIngredient> comparator;

        switch (choice) {
            case "1":
                comparator = Comparator.comparing(ing -> ing.getConsumable().getName());
                break;
            case "2":
                comparator = Comparator.comparingDouble(SaladIngredient::getWeightInGrams);
                break;
            case "3":
                comparator = Comparator.comparingDouble(SaladIngredient::getTotalCalories);
                break;
            default:
                logger.warn("Невірний критерій сортування: {}", choice);
                System.out.println("Невірний критерій сортування.");
                return;
        }

        List<SaladIngredient> sortedList = new ArrayList<>(salad.getIngredients());
        sortedList.sort(comparator);

        logger.info("Інгредієнти салату '{}' відсортовано за критерієм {}.", name, choice);
        System.out.println("\n--- Салат: " + salad.getName().toUpperCase() + " (Відсортовано) ---");
        sortedList.forEach(ing -> System.out.printf(" - %s (%.2f г) | Ккал: %.2f\n",
                ing.getConsumable().getName(), ing.getWeightInGrams(), ing.getTotalCalories()));
    }

    private void handleAddIngredient(Salad salad) {
        logger.info("Додавання інгредієнта до салату '{}'.", salad.getName());
        System.out.print("Введіть назву продукту: ");
        String productName = scanner.nextLine().trim();

        Optional<IProduct> productOpt = productRepository.getProductByName(productName);

        if (productOpt.isEmpty()) {
            logger.warn("Продукт '{}' не знайдено в каталозі.", productName);
            System.out.println("Продукт '" + productName + "' не знайдено в каталозі.");
            return;
        }

        try {
            System.out.print("Введіть вагу в грамах (напр., 150): ");
            double weight = Double.parseDouble(scanner.nextLine());
            logger.debug("Введено вагу: {} г", weight);

            SaladIngredient ingredient = new SaladIngredient(productOpt.get(), weight);
            salad.addIngredient(ingredient);
            logger.info("Інгредієнт '{}' ({}г) успішно додано до салату '{}'.", productName, weight, salad.getName());
            System.out.println(productName + " (" + weight + "г) додано до салату.");

        } catch (NumberFormatException e) {
            logger.warn("Помилка введення ваги: '{}' має бути числом.", e.getMessage());
            System.out.println("Помилка: Вага має бути числом.");
        } catch (Exception e) {
            logger.error("Критична помилка при додаванні інгредієнта: {}", e.getMessage(), e);
        }
    }

    private void handleRemoveIngredient(Salad salad) {
        logger.info("Видалення інгредієнта із салату '{}'.", salad.getName());
        System.out.println("\n--- Наявні інгредієнти ---");
        salad.getIngredients().forEach(ing -> System.out.println(" > " + ing.getConsumable().getName()));

        System.out.print("Введіть назву інгредієнта для видалення: ");
        String productName = scanner.nextLine().trim();
        logger.debug("Назва інгредієнта для видалення: '{}'", productName);

        Optional<SaladIngredient> ingredientToRemoveOpt = salad.getIngredients().stream()
                .filter(ing -> ing.getConsumable().getName().equalsIgnoreCase(productName))
                .findFirst();

        if (ingredientToRemoveOpt.isPresent()) {
            salad.removeIngredient(ingredientToRemoveOpt.get());
            logger.info("Інгредієнт '{}' успішно видалено із салату '{}'.", productName, salad.getName());
            System.out.println(productName + " видалено з рецепту.");
        } else {
            logger.warn("Спроба видалення неіснуючого інгредієнта: '{}' у салаті '{}'.", productName, salad.getName());
            System.out.println("Інгредієнт '" + productName + "' не знайдено в цьому салаті.");
        }
    }

    /**
     * Сортує загальний список салатів за їх калорійністю.
     */
    public void sortSaladsByCalories() {
        logger.info("Запущено операцію: Сортування всіх салатів за калорійністю.");
        List<Salad> salads = saladRepository.getAllSalads();
        if (salads.isEmpty()) {
            System.out.println("Немає рецептів для сортування.");
            return;
        }

        salads.sort(Comparator.comparingDouble(this::calculateTotalCalories));

        logger.info("Загальний список салатів відсортовано ({} шт.).", salads.size());
        System.out.println("\n--- Список салатів (Відсортовано за ккал) ---");
        for (int i = 0; i < salads.size(); i++) {
            Salad salad = salads.get(i);
            double cals = calculateTotalCalories(salad);
            System.out.printf("%d. %s (Загалом: %.2f ккал)\n", i + 1, salad.getName(), cals);
        }
    }

    /**
     * Фільтрує овочі у складі салату за заданим діапазоном базової калорійності.
     */
    public void findVegetablesByCalories() {
        logger.info("Запущено операцію: Фільтрація овочів у салаті за калорійністю.");
        System.out.print("\nВведіть назву салату для пошуку: ");
        String name = scanner.nextLine().trim();
        Optional<Salad> saladOpt = saladRepository.getSaladByName(name);

        if (saladOpt.isEmpty()) {
            logger.error("Фільтрація скасована: Рецепт '{}' не знайдено.", name);
            System.out.println("Рецепт '" + name + "' не знайдено.");
            return;
        }

        Salad salad = saladOpt.get();

        try {
            System.out.print("Введіть мінімальну калорійність (на 100г): ");
            double minCal = Double.parseDouble(scanner.nextLine());
            System.out.print("Введіть мінімальну калорійність (на 100г): ");
            double maxCal = Double.parseDouble(scanner.nextLine());

            List<SaladIngredient> results = salad.getIngredients().stream()
                    .filter(ing -> ing.getConsumable() instanceof Vegetable)
                    .filter(ing -> {
                        double baseCals = ing.getConsumable().getCaloriesPer100g();
                        return baseCals >= minCal && baseCals <= maxCal;
                    })
                    .collect(Collectors.toList());

            System.out.println("\n--- Результати пошуку в '" + salad.getName().toUpperCase() + "' ---");
            logger.info("Фільтрація завершена. Знайдено {} овочів у діапазоні {}-{} ккал.", results.size(), minCal, maxCal);
            if (results.isEmpty()) {
                System.out.println("Овочів у діапазоні " + minCal + "-" + maxCal + " ккал не знайдено.");
            } else {
                results.forEach(ing -> System.out.printf(" - %s | Базова ккал/100г: %.1f\n",
                        ing.getConsumable().getName(), ing.getConsumable().getCaloriesPer100g()));
            }
        } catch (NumberFormatException e) {
            logger.warn("Помилка введення: Межі калорійності мають бути числами.", e);
            System.out.println("Помилка введення: Межі калорійності мають бути числами.");
        }
    }
}