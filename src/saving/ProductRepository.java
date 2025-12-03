package saving;

import products.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Керує каталогом продуктів, використовуючи txt файл.
 * Реалізує логіку читання/запису.
 */
public class ProductRepository {
    private static final Logger logger = LogManager.getLogger(ProductRepository.class);

    private List<IProduct> availableProducts;
    private final String FILE_PATH;

    public ProductRepository(String filePath) {
        this.FILE_PATH = filePath;
        this.availableProducts = new ArrayList<>();
        logger.info("ProductRepository ініціалізовано. Шлях до файлу: {}", FILE_PATH);

        try {
            Path path = Paths.get(FILE_PATH);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
                logger.debug("Створено (або підтверджено існування) директорії для файлу: {}", path.getParent());
            }
        } catch (IOException e) {
            System.err.println("Помилка створення папки для " + FILE_PATH + ": " + e.getMessage());
            logger.fatal("Помилка створення папки для {}: {}", FILE_PATH, e.getMessage(), e);
        }
    }

    /**
     * Записує поточний список availableProducts у txt файл.
     * Використовує поліморфний метод product.toTxtLine().
     */
    public void saveToFile() {
        logger.info("Запущено збереження каталогу продуктів ({} шт.).", this.availableProducts.size());
        List<String> lines = new ArrayList<>();

        for (IProduct product : this.availableProducts) {
            lines.add(product.toTxtLine());
            logger.trace("Генерування рядка для збереження: {}", product.getName());
        }

        try {
            Files.write(Paths.get(FILE_PATH), lines, StandardCharsets.UTF_8);
            System.out.println("Каталог збережено у " + FILE_PATH);
            logger.info("Каталог успішно збережено у {}. Рядків: {}", FILE_PATH, lines.size());
        } catch (IOException e) {
            System.err.println("Помилка збереження файлу продуктів: " + e.getMessage());
            logger.error("Помилка збереження файлу продуктів {}: {}", FILE_PATH, e.getMessage(), e);
        }
    }

    /**
     * Читає txt файл, парсить його і заповнює availableProducts.
     */
    public void loadFromFile() {
        logger.info("Початок завантаження каталогу продуктів із {}.", FILE_PATH);
        Path path = Paths.get(FILE_PATH);
        if (!Files.exists(path)) {
            logger.warn("Файл {} не знайдено. Буде створено новий при першому збереженні.", FILE_PATH);
            System.out.println("Файл " + FILE_PATH + " не знайдено, буде створено новий при першому збереженні.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            availableProducts.clear();
            logger.debug("Зчитано {} рядків із файлу.", lines.size());
            int successfullyLoaded = 0;

            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 3) {
                    logger.warn("⚠️ Невірний формат рядка (замало частин). Пропущено: {}", line);
                    continue;
                }

                try {
                    String type = parts[0];
                    String name = parts[1];
                    String caloriesString = parts[2].replace(",", ".");
                    double calories = parseDoubleWithLocaleFix(caloriesString);

                    IProduct product = null;
                    switch (type) {
                        case "RootVegetable":
                            // RootVegetable;назва;калорії;sugarContent;isHard
                            product = new RootVegetable(name, calories, parseDoubleWithLocaleFix(parts[3]), Boolean.parseBoolean(parts[4]));
                            break;
                        case "LeafyVegetable":
                            // LeafyVegetable;назва;калорії;fiberContent
                            product = new LeafyVegetable(name, calories, parseDoubleWithLocaleFix(parts[3]));
                            break;
                        case "FruitingVegetable":
                            // FruitingVegetable;назва;калорії;waterContentPercent
                            product = new FruitingVegetable(name, calories, parseDoubleWithLocaleFix(parts[3]));
                            break;
                        case "TuberVegetable":
                            // TuberVegetable;назва;калорії;starchContent
                            product = new TuberVegetable(name, calories, parseDoubleWithLocaleFix(parts[3]));
                            break;
                        case "Dressing":
                            // Dressing;назва;калорії;baseType
                            product = new Dressing(name, calories, parts[3]);
                            break;
                        case "Topping":
                            // Topping;назва;калорії;isCrunchy
                            product = new Topping(name, calories, Boolean.parseBoolean(parts[3]));
                            break;
                    }
                    if (product != null) {
                        availableProducts.add(product);
                        logger.trace("Завантажено продукт: {} ({})", name, type);
                        successfullyLoaded++;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("Помилка читання рядка (неправильний формат). Пропущено: " + line);
                    logger.warn("Помилка парсингу рядка (неправильний формат). Пропущено: {}. Помилка: {}", line, e.getMessage());
                }
            }
            logger.info("Завантаження завершено. Успішно завантажено {} продуктів.", successfullyLoaded);
        } catch (IOException e) {
            System.err.println("Помилка читання файлу продуктів: " + e.getMessage());
            logger.error("❌ Помилка читання файлу продуктів {}: {}", FILE_PATH, e.getMessage(), e);
        }
    }

    /**
     * Повертає копію списку всіх продуктів.
     */
    public List<IProduct> getAllProducts() {
        logger.debug("Виклик getAllProducts. Повертається {} продуктів.", this.availableProducts.size());
        return new ArrayList<>(this.availableProducts);
    }

    /**
     * Додає продукт і викликає saveToFile().
     */
    public void addProduct(IProduct product) {
        logger.info("Додавання нового продукту до каталогу: {}", product.getName());
        this.availableProducts.add(product);
        saveToFile();
    }

    /**
     * Видаляє продукт і викликає saveToFile().
     */
    public void removeProduct(IProduct product) {
        logger.info("Видалення продукту з каталогу: {}", product.getName());
        this.availableProducts.remove(product);
        saveToFile();
    }

    /**
     * Шукає продукт за назвою (без урахування регістру).
     */
    public Optional<IProduct> getProductByName(String name) {
        logger.debug("Пошук продукту за назвою: '{}'", name);
        return this.availableProducts.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private double parseDoubleWithLocaleFix(String value) throws NumberFormatException {
        if (value == null) {
            logger.error("Спроба парсингу null-рядка.");
            throw new NumberFormatException("Рядок для парсингу є null.");
        }

        return Double.parseDouble(value.replace(",", "."));
    }
}