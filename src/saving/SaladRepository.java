package saving;

import products.IProduct;
import salad.Salad;
import salad.SaladIngredient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Відповідає за збереження та завантаження рецептів салатів.
 */
public class SaladRepository {
    private static final Logger logger = LogManager.getLogger(SaladRepository.class);

    private final String directoryPath;
    private final ProductRepository productRepository;
    private List<Salad> savedSalads;

    public SaladRepository(String directoryPath, ProductRepository productRepository) {
        this.directoryPath = directoryPath;
        this.productRepository = productRepository;
        this.savedSalads = new ArrayList<>();
        logger.info("SaladRepository ініціалізовано. Шлях до даних: {}", directoryPath);

        try {
            Files.createDirectories(Paths.get(directoryPath));
            logger.debug("Директорія для рецептів '{}' готова.", directoryPath);
        } catch (IOException e) {
            logger.fatal("Помилка створення директорії {}: {}", directoryPath, e.getMessage(), e);
            System.err.println("Помилка створення директорії " + directoryPath + ": " + e.getMessage());
        }
    }

    public void loadAllSalads() {
        logger.info("Початок завантаження всіх рецептів із '{}'.", directoryPath);
        savedSalads.clear();

        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            List<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .collect(Collectors.toList());

            logger.debug("Знайдено {} потенційних файлів рецептів.", files.size());

            for (Path filePath : files) {
                try {
                    String fileName = filePath.getFileName().toString();
                    String saladName = fileName.substring(0, fileName.lastIndexOf('.'));
                    Salad salad = new Salad(saladName);

                    List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
                    logger.debug("Завантаження салату '{}'. Знайдено {} рядків.", saladName, lines.size());
                    for (String line : lines) {
                        if (line == null || line.trim().isEmpty()) {
                            continue;
                        }

                        String[] parts = line.split(";");
                        if (parts.length != 2) {
                            logger.warn("Невірний формат інгредієнта у {}: '{}'. Пропущено.", fileName, line);
                            System.err.println("Невірний формат інгредієнта у " + fileName + ": " + line);
                            continue;
                        }

                        String productName = parts[0];
                        double weight = Double.parseDouble(parts[1]);

                        Optional<IProduct> productOpt = productRepository.getProductByName(productName);
                        if (productOpt.isPresent()) {
                            SaladIngredient ingredient = new SaladIngredient(productOpt.get(), weight);
                            salad.addIngredient(ingredient);
                            logger.trace("Додано інгредієнт: {} ({}г)", productName, weight);
                        } else {
                            System.err.println("Продукт '" + productName + "' не знайдено в каталозі. "
                                    + "Інгредієнт не додано до салату '" + saladName + "'.");
                            logger.error("Продукт '{}' не знайдено в каталозі. Інгредієнт не додано до '{}'.",
                                    productName, saladName);
                        }
                    }

                    savedSalads.add(salad);
                    logger.info("Рецепт '{}' успішно завантажено.", saladName);

                } catch (IOException | NumberFormatException e) {
                    logger.error("Помилка завантаження рецепту {}: {}", filePath.getFileName(), e.getMessage(), e);
                    System.err.println("Помилка завантаження рецепту " + filePath.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Помилка доступу до папки рецептів {}: {}", directoryPath, e.getMessage(), e);
            System.err.println("Помилка доступу до папки рецептів " + directoryPath + ": " + e.getMessage());
        }

        logger.info("Завантаження завершено. Загалом завантажено {} рецептів.", savedSalads.size());
    }

    /**
     * Зберігає один салат у файл (наприклад, "Цезар.txt").
     */
    public void saveSalad(Salad salad) {
        logger.info("Спроба збереження рецепту: '{}'.", salad.getName());
        Path filePath = Paths.get(directoryPath, salad.getName() + ".txt");
        List<String> lines = new ArrayList<>();

        for (SaladIngredient ingredient : salad.getIngredients()) {
            String line = ingredient.getConsumable().getName() + ";" + ingredient.getWeightInGrams();
            lines.add(line);
        }

        try {
            Files.write(filePath, lines, StandardCharsets.UTF_8);
            savedSalads.removeIf(s -> s.getName().equalsIgnoreCase(salad.getName()));
            savedSalads.add(salad);
            logger.info("Салат '{}' успішно збережено у файл {}.", salad.getName(), filePath.getFileName());
        } catch (IOException e) {
            logger.error("Помилка збереження рецепту {}: {}", salad.getName(), e.getMessage(), e);
            System.err.println("Помилка збереження рецепту " + salad.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Видаляє салат (файл з рецептом салату)
     */
    public void deleteSalad(String saladName) {
        logger.info("Спроба видалення рецепту: '{}'.", saladName);
        Path filePath = Paths.get(directoryPath, saladName + ".txt");

        try {
            Files.deleteIfExists(filePath);
            savedSalads.removeIf(s -> s.getName().equalsIgnoreCase(saladName));
            logger.info("Рецепт '{}' успішно видалено з файлової системи.", saladName);
        } catch (IOException e) {
            System.err.println("Помилка видалення рецепту " + saladName + ": " + e.getMessage());
            logger.warn("Рецепт '{}' не знайдено для видалення.", saladName);
        }
    }

    public List<Salad> getAllSalads() {
        logger.debug("Виклик getAllSalads. Повертається {} рецептів.", savedSalads.size());
        return new ArrayList<>(savedSalads);
    }

    public Optional<Salad> getSaladByName(String name) {
        return savedSalads.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}
