package tn.esprit.notaryproject.Services;

import jakarta.annotation.PostConstruct;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.notaryproject.Entities.Article;
import tn.esprit.notaryproject.Repository.ArticleRepo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service

public class ArticleService implements ArticleInterface {
    @Autowired
    ArticleRepo articleRepo;
    private Tesseract tesseract;

    @Value("${tesseract.data.path:src/main/resources/TestData}")
    private String tessdataPath;

    public ArticleService() {
        this.tesseract = new Tesseract();
    }

    @Override
    public Article addArticle(Article article) {
        return articleRepo.save(article);
    }

    @Override
    public Article updateArticle(Article article) {
        if(article.getIdArticle()==null || !articleRepo.existsById(article.getIdArticle()))
        {
            throw new IllegalArgumentException("Article id not found");

        }
        else
         return articleRepo.save(article);
    }

    @Override
    public void deleteArticle(Long IdArticle) {
        Article article = articleRepo.findById(IdArticle).orElse(null);
        articleRepo.delete(article);
    }

    @Override
    public List<Article> getArticles() {
        return articleRepo.findAll();
    }

    @PostConstruct
    public void initTesseract() {
        try {
            this.tesseract.setDatapath(tessdataPath);
            this.tesseract.setLanguage("ara"); // Arabic
            this.tesseract.setPageSegMode(1); // Automatic page segmentation
            this.tesseract.setOcrEngineMode(1); // Default engine
            System.out.println("Tesseract initialized successfully with path: " + tessdataPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Tesseract: " + e.getMessage(), e);
        }
    }

    @Override
    public Boolean validateArticleDate(Article article, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            System.out.println("No image file provided");
            return false;
        }

        try {
            // Extract text from image
            String extractedText = extractTextFromImage(imageFile);
            System.out.println("=== EXTRACTED TEXT ===");
            System.out.println(extractedText);
            System.out.println("======================");

            // Parse Arabic date
            Date extractedDate = parseArabicDate(extractedText);

            if (extractedDate == null) {
                System.out.println("No date found in the extracted text");
                return false;
            }

            System.out.println("Extracted date: " + extractedDate);
            System.out.println("Provided date: " + article.getDateCreation());

            // Compare dates by formatting them to ignore time components
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String extractedDateStr = dateFormat.format(extractedDate);
            String providedDateStr = dateFormat.format(article.getDateCreation());

            boolean isValid = extractedDateStr.equals(providedDateStr);
            System.out.println("Date validation result: " + isValid);
            System.out.println("Extracted: " + extractedDateStr + ", Provided: " + providedDateStr);

            return isValid;

        } catch (Exception e) {
            System.err.println("Error during date validation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String extractTextFromImage(MultipartFile imageFile) {
        try {
            byte[] imageBytes = imageFile.getBytes();
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                BufferedImage image = ImageIO.read(bis);

                if (image == null) {
                    throw new IOException("Unsupported image format");
                }

                return tesseract.doOCR(image);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from image: " + e.getMessage(), e);
        }
    }

    private Date parseArabicDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            System.out.println("Text is null or empty");
            return null;
        }

        System.out.println("Searching for date in text: " + text);

        // Enhanced patterns to handle different spacing scenarios
        Pattern[] patterns = {
                // Pattern for "13 نوفمبر 2021" (with spaces)
                Pattern.compile("(\\d{1,2})\\s+(يناير|فبراير|مارس|أبريل|مايو|يونيو|يوليو|أغسطس|سبتمبر|أكتوبر|نوفمبر|ديسمبر)\\s+(\\d{4})"),

                // Pattern for "13 نوفمبر2021" (without space after month) - FIX FOR YOUR CASE
                Pattern.compile("(\\d{1,2})\\s+(يناير|فبراير|مارس|أبريل|مايو|يونيو|يوليو|أغسطس|سبتمبر|أكتوبر|نوفمبر|ديسمبر)(\\d{4})"),

                // Pattern for dates with Arabic numerals: "١٣ نوفمبر ٢٠٢١"
                Pattern.compile("([٠-٩]{1,2})\\s+(يناير|فبراير|مارس|أبريل|مايو|يونيو|يوليو|أغسطس|سبتمبر|أكتوبر|نوفمبر|ديسمبر)\\s+([٠-٩]{4})"),

                // Pattern for "١٣نوفمبر٢٠٢١" (Arabic numerals, no spaces)
                Pattern.compile("([٠-٩]{1,2})(يناير|فبراير|مارس|أبريل|مايو|يونيو|يوليو|أغسطس|سبتمبر|أكتوبر|نوفمبر|ديسمبر)([٠-٩]{4})")
        };

        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(text);

            if (matcher.find()) {
                try {
                    System.out.println("Found date with pattern " + i + ": " + matcher.group(0));

                    String day, month, year;

                    if (i == 0) {
                        // Format: "13 نوفمبر 2021" (with spaces)
                        day = matcher.group(1);
                        month = convertArabicMonthToNumber(matcher.group(2));
                        year = matcher.group(3);
                    } else if (i == 1) {
                        // Format: "13 نوفمبر2021" (without space after month) - YOUR CASE
                        day = matcher.group(1);
                        month = convertArabicMonthToNumber(matcher.group(2));
                        year = matcher.group(3);
                    } else if (i == 2) {
                        // Format: "١٣ نوفمبر ٢٠٢١" (Arabic numerals with spaces)
                        day = convertArabicNumeralsToEnglish(matcher.group(1)); // FIXED: use convertArabicNumeralsToEnglish
                        month = convertArabicMonthToNumber(matcher.group(2));
                        year = convertArabicNumeralsToEnglish(matcher.group(3)); // FIXED: use convertArabicNumeralsToEnglish
                    } else {
                        // Format: "١٣نوفمبر٢٠٢١" (Arabic numerals, no spaces)
                        day = convertArabicNumeralsToEnglish(matcher.group(1)); // FIXED: use convertArabicNumeralsToEnglish
                        month = convertArabicMonthToNumber(matcher.group(2));
                        year = convertArabicNumeralsToEnglish(matcher.group(3)); // FIXED: use convertArabicNumeralsToEnglish
                    }

                    // Ensure two-digit day and month
                    day = day.length() == 1 ? "0" + day : day;
                    month = month.length() == 1 ? "0" + month : month;

                    String dateString = year + "-" + month + "-" + day;
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                    System.out.println("Parsed date: " + dateString);
                    return dateFormat.parse(dateString);

                } catch (Exception e) {
                    System.err.println("Error parsing date with pattern " + i + ": " + e.getMessage());
                    continue; // Try next pattern
                }
            }
        }

        System.out.println("No date pattern found in the text");
        return null;
    }

    private String convertArabicMonthToNumber(String arabicMonth) {
        switch (arabicMonth.trim()) {
            case "يناير": return "01";
            case "فبراير": return "02";
            case "مارس": return "03";
            case "أبريل": return "04";
            case "مايو": return "05";
            case "يونيو": return "06";
            case "يوليو": return "07";
            case "أغسطس": return "08";
            case "سبتمبر": return "09";
            case "أكتوبر": return "10";
            case "نوفمبر": return "11";
            case "ديسمبر": return "12";
            default: return "01";
        }
    }
    private String convertArabicNumeralsToEnglish(String arabicNumerals) {
        return arabicNumerals
                .replace('٠', '0')
                .replace('١', '1')
                .replace('٢', '2')
                .replace('٣', '3')
                .replace('٤', '4')
                .replace('٥', '5')
                .replace('٦', '6')
                .replace('٧', '7')
                .replace('٨', '8')
                .replace('٩', '9');
    }


}





