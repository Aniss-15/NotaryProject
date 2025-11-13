package tn.esprit.notaryproject.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.notaryproject.Entities.Article;
import tn.esprit.notaryproject.Services.ArticleInterface;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@NoArgsConstructor
@RequestMapping("/Articles")
public class ArticleController {

    @Autowired
    ArticleInterface articleInterface;

    @PostMapping("/AddArticles")
    public Article addArticle(@RequestBody Article article) {
        return articleInterface.addArticle(article);
    }
    @DeleteMapping("DeleteArticle/{IdArticle}")
    public ResponseEntity<String> deleteArticle(@PathVariable Long IdArticle  ) {
        articleInterface.deleteArticle(IdArticle);
        return ResponseEntity.ok("Article deleted successfully");
    }
    @GetMapping("listofArticle")
    public List<Article> listOfArticle() {
        return articleInterface.getArticles();
    }


    @Operation(
            summary = "Upload article with OCR date validation",
            description = "Upload an article with an image document and validate the creation date using OCR"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article uploaded successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file format or date validation failed", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping(value = "/ajoutavecVerification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadArticle(
            @Parameter(description = "Image file containing the document (JPEG, PNG, TIFF, BMP)", required = true)
            @RequestParam("imageFile") MultipartFile imageFile,

            @Parameter(description = "Article reference number", example = "REF-001", required = true)
            @RequestParam("reference") String reference,

            @Parameter(description = "Article description", example = "Legal contract document", required = true)
            @RequestParam("description") String description,

            @Parameter(description = "Expected creation date in YYYY-MM-DD format", example = "2021-11-13", required = true)
            @RequestParam("dateCreation") @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateCreation) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file type
            if (!isValidImageFile(imageFile)) {
                response.put("success", false);
                response.put("message", "Invalid image format. Supported formats: JPEG, PNG, TIFF, BMP");
                return ResponseEntity.badRequest().body(response);
            }

            // Create article object
            Article article = new Article();
            article.setReference(reference);
            article.setDescription(description);
            article.setDateCreation(dateCreation);
            article.setImage(imageFile.getOriginalFilename());

            // Validate date from image using OCR
            Boolean isValid = articleInterface.validateArticleDate(article, imageFile);

            if (isValid) {
                   // Save to database
                Article savedArticle = articleInterface.addArticle(article);

                response.put("success", true);
                response.put("message", "Article uploaded successfully! Date validation passed.");
                response.put("article", savedArticle);
                response.put("validatedDate", dateCreation);

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Date validation failed! The date in the document doesn't match the provided date.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing request: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(
            summary = "Check OCR service health",
            description = "Verify that the Tesseract OCR service is properly configured and running"
    )
    @ApiResponse(responseCode = "200", description = "OCR service status", content = @Content(schema = @Schema(implementation = Map.class)))
    @GetMapping("/ocr-health")
    public ResponseEntity<Map<String, Object>> ocrHealthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "OK");
            response.put("service", "Tesseract OCR");
            response.put("language", "Arabic (ara)");
            response.put("timestamp", new Date());
            response.put("message", "OCR service is operational and ready for processing");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("service", "Tesseract OCR");
            response.put("timestamp", new Date());
            response.put("message", "OCR service not available: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * Validate image file type
     */
    private boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            return false;
        }

        String contentType = file.getContentType().toLowerCase();
        return contentType.startsWith("image/") && (
                contentType.contains("jpeg") ||
                        contentType.contains("png") ||
                        contentType.contains("tiff") ||
                        contentType.contains("bmp") ||
                        contentType.contains("jpg")
        );
    }
    @PostMapping("UpdateArticle")
    public ResponseEntity<String> updateArticle(@RequestBody Article article)
    {
       Article artic = articleInterface.updateArticle(article);
        return ResponseEntity.ok("Article Updated successfully!");
    }

}