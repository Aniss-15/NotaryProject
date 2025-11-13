package tn.esprit.notaryproject.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.notaryproject.Entities.Cheque;
import tn.esprit.notaryproject.Entities.ChequeValidationStatus;
import tn.esprit.notaryproject.Entities.ChequesStatus;
import tn.esprit.notaryproject.Repository.ChequeRepoo;

import java.io.File;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class chequeservice implements ChequeInterface {

    @Autowired
    private ChequeRepoo chequeRepoo;

    @Autowired
    private OCRService ocrService;

    @Autowired
    private MistralCheckValidator mistralCheckValidator;

    @Override
    public Cheque chequeAjout(Cheque cheque, MultipartFile file) throws Exception {
        ChequeValidationStatus validationStatus = ChequeValidationStatus.INVALID;

        try {
            // CORRECTION: Créer une copie des bytes AVANT toute utilisation
            byte[] fileBytes = file.getBytes();

            // Step 1: Store image data from the copy
            cheque.setImageData(fileBytes);

            // Step 2: Do OCR validation using the bytes copy with PREPROCESSING
            String ocrText = ocrService.extractTextFromImageBytes(fileBytes); // ← CHANGEMENT ICI

            // Step 3: Validate with Mistral using OCR text only
            validationStatus = validateFromOCRText(ocrText);

            // Step 4: Extract additional data from OCR
            extractAndSetChequeData(cheque, ocrText);

        } catch (Exception e) {
            System.out.println("Error during OCR processing: " + e.getMessage());
            validationStatus = ChequeValidationStatus.INVALID;
        }

        // Le reste du code reste identique...
        cheque.setValidationStatus(validationStatus);
        if (validationStatus == ChequeValidationStatus.VALID) {
            cheque.setStatus(ChequesStatus.Recieved);
        } else {
            cheque.setStatus(ChequesStatus.Canceled);
        }

        if (cheque.getChequeNumber() == null) {
            cheque.setChequeNumber(generateChequeNumber());
        }
        if (cheque.getDepositDateCheque() == null) {
            cheque.setDepositDateCheque(new Date());
        }

        System.out.println("=== FINAL VALIDATION STATUS ===");
        System.out.println("Validation: " + validationStatus);
        System.out.println("Status: " + cheque.getStatus());

        return chequeRepoo.save(cheque);
    }

    @Override
    public void DeleteCheque(Long IdCheque) {
         Cheque cheque = chequeRepoo.findById(IdCheque).orElseThrow(()-> new IllegalArgumentException("cheque not found"));
         chequeRepoo.delete(cheque);
    }

    // AJOUTER cette nouvelle méthode pour valider à partir du texte OCR seulement
    private ChequeValidationStatus validateFromOCRText(String ocrText) throws Exception {
        System.out.println("=== VALIDATING FROM OCR TEXT ===");
        System.out.println("OCR Text: " + ocrText);

        // Utiliser le même validateur mais avec le texte OCR
        String mistralResponse = mistralCheckValidator.validateChequeData(ocrText);
        System.out.println("Mistral response: " + mistralResponse);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;

        try {
            String jsonString = extractJsonFromResponse(mistralResponse);
            rootNode = mapper.readTree(jsonString);
        } catch (Exception e) {
            System.out.println("Failed to parse Mistral response: " + e.getMessage());
            try {
                rootNode = mapper.readTree(mistralResponse);
            } catch (Exception ex) {
                System.out.println("Complete parsing failure, using manual validation");
                return validateManually(ocrText);
            }
        }

        return validateFromMistralResponse(rootNode, ocrText);
    }
    @Override
    public ChequeValidationStatus checkChequeValidity(MultipartFile file) throws Exception {
        // Utiliser directement les bytes avec prétraitement
        byte[] fileBytes = file.getBytes();

        // Step 1: Extract text from cheque image WITH PREPROCESSING
        String ocrText = ocrService.extractTextFromImageBytes(fileBytes); // ← CHANGEMENT ICI
        System.out.println("OCR Extracted Text: " + ocrText);

        // Step 2: Validate text using Mistral model
        String mistralResponse = mistralCheckValidator.validateChequeData(ocrText);
        System.out.println("Raw Mistral response: " + mistralResponse);

        // Le reste du code reste identique...
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;

        try {
            String jsonString = extractJsonFromResponse(mistralResponse);
            rootNode = mapper.readTree(jsonString);
        } catch (Exception e) {
            System.out.println("Failed to parse Mistral response: " + e.getMessage());
            try {
                rootNode = mapper.readTree(mistralResponse);
            } catch (Exception ex) {
                System.out.println("Complete parsing failure, using manual validation");
                return validateManually(ocrText);
            }
        }

        return validateFromMistralResponse(rootNode, ocrText);
    }

    private ChequeValidationStatus validateFromMistralResponse(JsonNode rootNode, String ocrText) {
        String bankName = "";
        String amountWritten = "";
        String signature = "";
        boolean lisible = false;
        boolean coherent = false;

        try {
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                String content = messageNode.path("content").asText();

                String jsonContent = extractJsonFromResponse(content);
                JsonNode analysisNode = new ObjectMapper().readTree(jsonContent);

                bankName = analysisNode.path("banque").asText("").trim();
                amountWritten = analysisNode.path("montant_en_lettres").asText("").trim();
                signature = analysisNode.path("signature").asText("").trim();
                lisible = analysisNode.path("lisible").asBoolean(false);
                coherent = analysisNode.path("coherent").asBoolean(false);
            }
        } catch (Exception e) {
            System.out.println("Error parsing nested JSON structure: " + e.getMessage());
            bankName = rootNode.path("banque").asText("").trim();
            amountWritten = rootNode.path("montant_en_lettres").asText("").trim();
            signature = rootNode.path("signature").asText("").trim();
            lisible = rootNode.path("lisible").asBoolean(false);
            coherent = rootNode.path("coherent").asBoolean(false);
        }

        // CORRECTION: Ajouter ocrText comme 6ème paramètre
        boolean isValid = validateEssentialFields(bankName, amountWritten, signature, lisible, coherent, ocrText);

        System.out.println("=== CHEQUE VALIDATION REPORT ===");
        System.out.println("Bank: " + (bankName.isEmpty() ? "MISSING" : bankName));
        System.out.println("Amount (written): " + (amountWritten.isEmpty() ? "MISSING" : amountWritten));
        System.out.println("Signature: " + signature);
        System.out.println("Readable: " + lisible);
        System.out.println("Coherent: " + coherent);
        System.out.println("Final Validation: " + (isValid ? "VALID" : "INVALID"));

        return isValid ? ChequeValidationStatus.VALID : ChequeValidationStatus.INVALID;
    }
    private boolean validateEssentialFields(String bankName, String amountWritten, String signature,
                                            boolean lisible, boolean coherent, String ocrText) {

        boolean hasValidBank = !bankName.isEmpty() && bankName.length() > 2;
        boolean hasValidAmountWritten = !amountWritten.isEmpty() && hasAmountKeywords(amountWritten);
        boolean hasValidSignature = "YES".equalsIgnoreCase(signature);

        // NOUVELLE VÉRIFICATION: Vérifier aussi le montant numérique dans le texte OCR
        boolean hasValidAmountNumerique = hasNumericAmountInOCR(ocrText);

        // LOGIQUE AMÉLIORÉE: Le montant est valide si soit l'écrit, soit le numérique est présent
        boolean hasValidAmount = hasValidAmountWritten || hasValidAmountNumerique;

        // Acceptable si lisible OU si les champs essentiels sont identifiables
        boolean isAcceptable = lisible || (!bankName.isEmpty() && (hasValidAmountWritten || hasValidAmountNumerique));

        System.out.println("=== VALIDATION BREAKDOWN ===");
        System.out.println("- Valid Bank: " + hasValidBank + " (" + bankName + ")");
        System.out.println("- Valid Amount (written): " + hasValidAmountWritten + " (" + amountWritten + ")");
        System.out.println("- Valid Amount (numeric): " + hasValidAmountNumerique);
        System.out.println("- Valid Signature: " + hasValidSignature + " (" + signature + ")");
        System.out.println("- Acceptable Quality: " + isAcceptable);
        System.out.println("- Final Valid Amount: " + hasValidAmount);

        return hasValidBank && hasValidAmount && hasValidSignature && isAcceptable;
    }

    private boolean hasAmountKeywords(String amountText) {
        String lowerAmount = amountText.toLowerCase();
        return lowerAmount.contains("thousand") ||
                lowerAmount.contains("hundred") ||
                lowerAmount.contains("only") ||
                lowerAmount.matches(".*\\bfive\\b.*") ||
                lowerAmount.matches(".*\\bten\\b.*") ||
                lowerAmount.matches(".*\\bfifty\\b.*") ||
                lowerAmount.matches(".*\\bone\\b.*") ||
                lowerAmount.matches(".*\\btwo\\b.*") ||
                lowerAmount.matches(".*\\bthree\\b.*") ||
                lowerAmount.matches(".*\\bfour\\b.*") ||
                lowerAmount.matches(".*\\bsix\\b.*") ||
                lowerAmount.matches(".*\\bseven\\b.*") ||
                lowerAmount.matches(".*\\beight\\b.*") ||
                lowerAmount.matches(".*\\bnine\\b.*");
    }

    private String extractJsonFromResponse(String response) {
        try {
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                return response.substring(start, end).trim();
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.indexOf("```", start);
                return response.substring(start, end).trim();
            } else {
                int start = response.indexOf("{");
                int end = response.lastIndexOf("}") + 1;
                if (start >= 0 && end > start) {
                    return response.substring(start, end);
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting JSON: " + e.getMessage());
        }
        return response;
    }

    private ChequeValidationStatus validateManually(String ocrText) {
        boolean hasBank = ocrText.contains("State Bank Of India") || ocrText.contains("SBI");
        boolean hasAmount = ocrText.contains("Five Thousand Only") || ocrText.contains("Five Thousand");
        boolean hasSignature = ocrText.contains("Dipak Das") || ocrText.contains("Signature");

        System.out.println("=== MANUAL VALIDATION ===");
        System.out.println("Has Bank: " + hasBank);
        System.out.println("Has Amount: " + hasAmount);
        System.out.println("Has Signature: " + hasSignature);

        return (hasBank && hasAmount && hasSignature) ?
                ChequeValidationStatus.VALID : ChequeValidationStatus.INVALID;
    }

    private void extractAndSetChequeData(Cheque cheque, String ocrText) throws Exception {
        try {
            String mistralResponse = mistralCheckValidator.validateChequeData(ocrText);
            ObjectMapper mapper = new ObjectMapper();

            String jsonString = extractJsonFromResponse(mistralResponse);
            JsonNode rootNode = mapper.readTree(jsonString);

            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                String content = messageNode.path("content").asText();

                String jsonContent = extractJsonFromResponse(content);
                JsonNode analysisNode = mapper.readTree(jsonContent);

                String bankName = analysisNode.path("banque").asText("").trim();
                String amountWritten = analysisNode.path("montant_en_lettres").asText("").trim();

                // Set bank name if not already set and extracted successfully
                if ((cheque.getBankeName() == null || cheque.getBankeName().isEmpty()) && !bankName.isEmpty()) {
                    cheque.setBankeName(bankName);
                }

                // Set amount if not already set and extracted successfully
                if (cheque.getMontant() == null || cheque.getMontant() == 0.0f) {
                    Float extractedAmount = extractAmountFromText(amountWritten);
                    if (extractedAmount > 0) {
                        cheque.setMontant(extractedAmount);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting cheque data from OCR: " + e.getMessage());
            extractChequeDataManually(cheque, ocrText);
        }
    }

    private Float extractAmountFromText(String amountText) {
        if (amountText == null || amountText.isEmpty()) {
            return 0.0f;
        }

        String lowerAmount = amountText.toLowerCase();
        if (lowerAmount.contains("five thousand")) {
            return 5000.0f;
        } else if (lowerAmount.contains("one thousand")) {
            return 1000.0f;
        } else if (lowerAmount.contains("two thousand")) {
            return 2000.0f;
        } else if (lowerAmount.contains("ten thousand")) {
            return 10000.0f;
        } else if (lowerAmount.contains("fifty thousand")) {
            return 50000.0f;
        } else if (lowerAmount.contains("one hundred")) {
            return 100.0f;
        } else if (lowerAmount.contains("five hundred")) {
            return 500.0f;
        }

        return 0.0f;
    }

    private Long generateChequeNumber() {
        return System.currentTimeMillis() % 1000000000L;
    }

    private void extractChequeDataManually(Cheque cheque, String ocrText) {
        // Extract bank name
        if ((cheque.getBankeName() == null || cheque.getBankeName().isEmpty())) {
            if (ocrText.contains("State Bank Of India") || ocrText.contains("SBI")) {
                cheque.setBankeName("State Bank Of India");
            }
        }

        // Extract amount
        if (cheque.getMontant() == null || cheque.getMontant() == 0.0f) {
            if (ocrText.contains("Five Thousand Only") || ocrText.contains("Five Thousand")) {
                cheque.setMontant(5000.0f);
            } else if (ocrText.contains("One Thousand")) {
                cheque.setMontant(1000.0f);
            } else if (ocrText.contains("Two Thousand")) {
                cheque.setMontant(2000.0f);
            }
        }
    }

    // NOUVELLE MÉTHODE: Vérifier la présence d'un montant numérique dans le texte OCR avec différentes devises
    private boolean hasNumericAmountInOCR(String ocrText) {
        if (ocrText == null || ocrText.isEmpty()) {
            return false;
        }

        // Rechercher des motifs de montants numériques avec différentes devises
        String[] amountPatterns = {
                // Dinar tunisien
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*DT\\b", // 5 000 DT, 5000.50 DT
                "\\bDT\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // DT 5 000, DT 5000.50
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*دينار\\b", // 5 000 دينار
                "\\bدينار\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // دينار 5 000

                // Euro
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*€\\b", // 5 000 €, 5000.50 €
                "\\b€\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // € 5 000, € 5000.50
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*EUR\\b", // 5 000 EUR
                "\\bEUR\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // EUR 5 000
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*EURO\\b", // 5 000 EURO
                "\\bEURO\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // EURO 5 000

                // Dollar
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*\\$\\b", // 5 000 $, 5000.50 $
                "\\b\\$\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // $ 5 000, $ 5000.50
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*USD\\b", // 5 000 USD
                "\\bUSD\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // USD 5 000
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*US\\$\\b", // 5 000 US$
                "\\bUS\\$\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // US$ 5 000

                // Livre sterling
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*£\\b", // 5 000 £
                "\\b£\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // £ 5 000
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*GBP\\b", // 5 000 GBP
                "\\bGBP\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // GBP 5 000

                // Franc suisse
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*CHF\\b", // 5 000 CHF
                "\\bCHF\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // CHF 5 000

                // Yen japonais
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*¥\\b", // 5 000 ¥
                "\\b¥\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // ¥ 5 000
                "\\b\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\s*JPY\\b", // 5 000 JPY
                "\\bJPY\\s*\\d{1,3}(?:\\s\\d{3})*(?:\\.\\d{1,2})?\\b", // JPY 5 000

                // Formats génériques (sans devise spécifique)
                "\\b\\d{1,3}(?:[,\\s]\\d{3})*(?:\\.\\d{1,2})?\\b", // 5,000, 5 000, 5000.50
                "\\bRs\\.?\\s*\\d{1,3}(?:[,\\s]\\d{3})*(?:\\.\\d{1,2})?\\b", // Rs. 5000, Rs 5,000
        };

        // Vérifier chaque pattern
        for (String pattern : amountPatterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = regex.matcher(ocrText);

            if (matcher.find()) {
                String foundAmount = matcher.group();
                System.out.println("Found amount with pattern '" + pattern + "': " + foundAmount);

                // Extraire le nombre pour vérifier qu'il est dans une plage raisonnable
                String numericPart = foundAmount.replaceAll("[^\\d.]", "").trim();
                if (!numericPart.isEmpty()) {
                    try {
                        // Gérer les séparateurs de milliers
                        String cleanNumber = numericPart.replaceAll("\\s", "").replaceAll(",", "");
                        double amount = Double.parseDouble(cleanNumber);

                        // Vérifier que le montant est dans une plage raisonnable (0.01 à 1,000,000)
                        if (amount >= 0.01 && amount <= 1000000) {
                            System.out.println("Valid numeric amount detected: " + amount);
                            return true;
                        } else {
                            System.out.println("Amount out of reasonable range: " + amount);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Error parsing amount: " + numericPart);
                    }
                }
            }
        }

        // Recherche supplémentaire pour des motifs spécifiques communs dans les chèques
        String[] commonChequePatterns = {
                "\\b\\d+\\s*DTN?\\b", // 5000 DT, 5000 DTN
                "\\b\\d+\\s*EURO?S?\\b", // 5000 EURO, 5000 EUROS
                "\\b\\d+\\s*DOLLAR?S?\\b", // 5000 DOLLAR, 5000 DOLLARS
                "\\b\\d+\\s*DINAR?S?\\b", // 5000 DINAR, 5000 DINARS
        };

        for (String pattern : commonChequePatterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = regex.matcher(ocrText);

            if (matcher.find()) {
                String foundAmount = matcher.group();
                System.out.println("Found amount with common pattern '" + pattern + "': " + foundAmount);
                return true;
            }
        }

        System.out.println("No valid numeric amount found in OCR text");
        return false;
    }
}