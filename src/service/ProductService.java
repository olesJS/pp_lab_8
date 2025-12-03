package service;

import saving.ProductRepository;
import products.*;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Отримувач (Receiver) для всієї логіки, пов'язаної з каталогом продуктів
 */
public class ProductService {
    private static final Logger logger = LogManager.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final Scanner scanner;

    public ProductService(ProductRepository repo, Scanner scanner) {
        this.productRepository = repo;
        this.scanner = scanner;

        logger.info("ProductService ініціалізовано. Репозиторій: {}", repo.getClass().getSimpleName());
    }

    private <T extends IProduct> List<T> filterProductsByType(Class<T> type) {
        return productRepository.getAllProducts().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    // Функціонал додавання

    public void addNewVegetable() {
        logger.info("Запущено операцію: Додати новий овоч.");

        System.out.println("\n--- Додати новий ОВОЧ ---");
        System.out.print("Назва: ");
        String name = scanner.nextLine();

        logger.debug("Введена назва овоча: {}", name);

        try {
            System.out.print("Калорійність (на 100г): ");
            double calories = Double.parseDouble(scanner.nextLine());
            logger.debug("Введена калорійність: {}", calories);

            System.out.println("\nОберіть тип овоча:");
            System.out.println("1. Коренеплід (RootVegetable)");
            System.out.println("2. Листовий (LeafyVegetable)");
            System.out.println("3. Плодовий (FruitingVegetable)");
            System.out.println("4. Бульбоплід (TuberVegetable)");
            System.out.print("Ваш вибір: ");
            String choice = scanner.nextLine();
            logger.debug("Обраний тип овоча: {}", choice);

            IProduct newProduct = null;

            switch (choice) {
                case "1": // RootVegetable (sugarContent, isHard)
                    System.out.print("Вміст цукру (г/100г): ");
                    double sugar = Double.parseDouble(scanner.nextLine());
                    System.out.print("Твердий овоч (true/false): ");
                    boolean isHard = Boolean.parseBoolean(scanner.nextLine());
                    newProduct = new RootVegetable(name, calories, sugar, isHard);
                    break;

                case "2": // LeafyVegetable (fiberContent)
                    System.out.print("Вміст клітковини (г/100г): ");
                    double fiber = Double.parseDouble(scanner.nextLine());
                    newProduct = new LeafyVegetable(name, calories, fiber);
                    break;

                case "3": // FruitingVegetable (waterContentPercent, needsSeedRemoval logic)
                    System.out.print("Вміст води (%): ");
                    double water = Double.parseDouble(scanner.nextLine());
                    newProduct = new FruitingVegetable(name, calories, water);
                    break;

                case "4": // TuberVegetable (starchContent, mustBeCooked)
                    System.out.print("Вміст крохмалю (г/100г): ");
                    double starch = Double.parseDouble(scanner.nextLine());
                    newProduct = new TuberVegetable(name, calories, starch);
                    break;

                default:
                    System.out.println("Помилка: Невірний вибір типу овоча.");
                    logger.warn("Користувач ввів невірний тип овоча: {}", choice);
                    return;
            }

            // 3. ДОДАВАННЯ В РЕПОЗИТОРІЙ
            if (newProduct != null) {
                productRepository.addProduct(newProduct);
                logger.info("Успішно додано продукт: {} ({}) з калорійністю: {}",
                        name, newProduct.getClass().getSimpleName(), calories);
                System.out.println("Овоч '" + name + "' (" + newProduct.getClass().getSimpleName() + ") успішно додано та збережено.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Помилка введення: Калорійність, цукор, клітковина або крохмаль мають бути числами.");

            logger.warn("Помилка введення числових даних під час додавання овоча: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Непередбачена критична помилка при додаванні овоча: {}", e.getMessage(), e);
        }
    }

    public void addNewDressing() {
        logger.info("Запущено операцію: Додати нову заправку.");
        System.out.println("\n--- Додати нову ЗАПРАВКУ ---");
        System.out.print("Назва: ");
        String name = scanner.nextLine();

        try {
            System.out.print("Калорійність (на 100г): ");
            double calories = Double.parseDouble(scanner.nextLine());
            System.out.print("Тип основи (Олійна, Вершкова, Кисла): ");
            String baseType = scanner.nextLine();
            logger.debug("Введено: Ккал={}, Тип={}", calories, baseType);

            IProduct newProduct = new Dressing(name, calories, baseType);

            productRepository.addProduct(newProduct);
            System.out.println("Заправка '" + name + "' успішно додана та збережена.");
            logger.info("Успішно додано заправку: {} ({})", name, baseType);
        } catch (NumberFormatException e) {
            System.out.println("Помилка введення: Калорійність має бути числом.");
            logger.warn("Помилка введення числових даних під час додавання заправки: {}", e.getMessage());
        }
    }

    public void addNewTopping() {
        logger.info("Запущено операцію: Додати новий топінг.");
        System.out.println("\n--- Додати новий ТОПІНГ ---");
        System.out.print("Назва: ");
        String name = scanner.nextLine();

        try {
            System.out.print("Калорійність (на 100г): ");
            double calories = Double.parseDouble(scanner.nextLine());
            System.out.print("Хрусткий (true/false): ");
            boolean isCrunchy = Boolean.parseBoolean(scanner.nextLine());
            logger.debug("Введено: Ккал={}", calories);

            IProduct newProduct = new Topping(name, calories, isCrunchy);

            productRepository.addProduct(newProduct);
            System.out.println("Топінг '" + name + "' успішно додано та збережено.");
            logger.info("Успішно додано топінг: {})", name);
        } catch (NumberFormatException e) {
            System.out.println("Помилка введення: Калорійність має бути числом.");
            logger.warn("Помилка введення числових даних під час додавання топінгу: {}", e.getMessage());
        }
    }

    // Функціонал перегляду

    public void viewVegetables() {
        logger.info("Користувач переглядає каталог овочів.");
        List<Vegetable> products = filterProductsByType(Vegetable.class);
        if (products.isEmpty()) {
            System.out.println("Каталог овочів порожній.");
            logger.info("Каталог овочів порожній.");
            return;
        }

        logger.debug("Знайдено {} овочів для відображення.", products.size());
        System.out.println("\nНаявні овочі");
        products.forEach(p -> System.out.println(String.format("%s | %s | %.2f ккал",
                p.getName(), p.getUkrName(), p.getCaloriesPer100g())));
    }

    public void viewDressings() {
        logger.info("Користувач переглядає каталог заправок.");
        List<Dressing> products = filterProductsByType(Dressing.class);
        if (products.isEmpty()) {
            System.out.println("Каталог заправок порожній.");
            logger.info("Каталог заправок порожній.");
            return;
        }

        logger.debug("Знайдено {} заправок для відображення.", products.size());
        System.out.println("\nНаявні заправки");
        products.forEach(p -> System.out.println(String.format("%s | %.2f ккал",
                p.getName(), p.getCaloriesPer100g())));
    }

    public void viewToppings() {
        logger.info("Користувач переглядає каталог топінгів.");
        List<Topping> products = filterProductsByType(Topping.class);
        if (products.isEmpty()) {
            System.out.println("Каталог топінгів порожній.");
            logger.info("Каталог топінгів порожній.");
            return;
        }

        logger.debug("Знайдено {} топінгів для відображення.", products.size());
        System.out.println("\nНаявні додатки:");
        products.forEach(p -> System.out.println(String.format("%s | %.2f ккал",
                p.getName(), p.getCaloriesPer100g())));
    }

    // Функціонал видалення

    public void removeVegetable() {
        logger.info("Запущено операцію: Видалити овоч.");
        viewVegetables();

        System.out.print("\nВведіть назву овоча для видалення: ");
        String nameToRemove = scanner.nextLine();
        logger.debug("Введена назва для видалення: {}", nameToRemove);

        Optional<IProduct> productOpt = productRepository.getProductByName(nameToRemove);

        if (productOpt.isPresent() && productOpt.get() instanceof Vegetable) {
            productRepository.removeProduct(productOpt.get());
            System.out.println("Овоч '" + nameToRemove + "' успішно видалено. Збережено.");
            logger.info("Овоч '{}' успішно видалено.", nameToRemove);
        } else {
            logger.error("Помилка видалення: Овоч з назвою '{}' не знайдено.", nameToRemove);
            System.out.println("Помилка: Овоч з назвою '" + nameToRemove + "' не знайдено.");
        }
    }

    public void removeDressing() {
        logger.info("Запущено операцію: Видалити заправку.");
        viewDressings();

        System.out.print("\nВведіть назву заправки для видалення: ");
        String nameToRemove = scanner.nextLine();
        logger.debug("Введена назва для видалення: {}", nameToRemove);

        Optional<IProduct> productOpt = productRepository.getProductByName(nameToRemove);

        if (productOpt.isPresent() && productOpt.get() instanceof Dressing) {
            productRepository.removeProduct(productOpt.get());
            System.out.println("Заправку '" + nameToRemove + "' успішно видалено. Збережено.");
            logger.info("Заправку '{}' успішно видалено.", nameToRemove);
        } else {
            System.out.println("Помилка: Заправку з назвою '" + nameToRemove + "' не знайдено.");
            logger.error("Помилка видалення: Заправку з назвою '{}' не знайдено.", nameToRemove);
        }
    }

    public void removeTopping() {
        logger.info("Запущено операцію: Видалити топінг.");
        viewToppings();

        System.out.print("\nВведіть назву топінгу для видалення: ");
        String nameToRemove = scanner.nextLine();
        logger.debug("Введена назва для видалення: {}", nameToRemove);

        Optional<IProduct> productOpt = productRepository.getProductByName(nameToRemove);

        if (productOpt.isPresent() && productOpt.get() instanceof Topping) {
            productRepository.removeProduct(productOpt.get());
            System.out.println("Топінг '" + nameToRemove + "' успішно видалено. Збережено.");
            logger.info("Топінг '{}' успішно видалено.", nameToRemove);
        } else {
            System.out.println("Помилка: Топінг з назвою '" + nameToRemove + "' не знайдено.");
            logger.error("Помилка видалення: Топінг з назвою '{}' не знайдено.", nameToRemove);
        }
    }
}