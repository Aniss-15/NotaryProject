package tn.esprit.notaryproject.Services;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.notaryproject.Utils.ChequeImagePreprocessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class OCRService {

    @Autowired
    private ChequeImagePreprocessor imagePreprocessor;

    public String extractTextFromImage(File imageFile) throws TesseractException, IOException {
        ITesseract tesseract = new Tesseract();

        // ✅ Correct path for resources/TestData
        String tessDataPath = new File("src/main/resources/TestData").getAbsolutePath();
        tesseract.setDatapath(tessDataPath);

        // ✅ Configuration optimisée pour les chèques
        configureTesseractForCheques(tesseract);

        System.out.println("=== DÉBUT TRAITEMENT OCR AVEC PRÉTRAITEMENT ===");
        System.out.println("Fichier: " + imageFile.getName());

        // Étape 1: Convertir File en byte[] pour le prétraitement
        byte[] imageData = Files.readAllBytes(imageFile.toPath());

        // Étape 2: Préprocessing de l'image
        byte[] processedImage = imagePreprocessor.preprocessChequeImage(imageData);

        // Étape 3: Sauvegarde de l'image traitée (débogage)
        File processedFile = new File("processed_" + imageFile.getName());
        Files.write(processedFile.toPath(), processedImage);
        System.out.println("Image traitée sauvegardée: " + processedFile.getAbsolutePath());

        // Étape 4: OCR sur l'image traitée
        String extractedText = tesseract.doOCR(processedFile);

        System.out.println("=== TEXTE EXTRAIT ===");
        System.out.println(extractedText);
        System.out.println("=== FIN TRAITEMENT OCR ===");

        // Nettoyer le fichier temporaire
        processedFile.delete();

        return extractedText;
    }

    // Nouvelle méthode pour traitement direct avec bytes
    public String extractTextFromImageBytes(byte[] imageData) throws TesseractException, IOException {
        ITesseract tesseract = new Tesseract();

        String tessDataPath = new File("src/main/resources/TestData").getAbsolutePath();
        tesseract.setDatapath(tessDataPath);
        configureTesseractForCheques(tesseract);

        System.out.println("=== DÉBUT TRAITEMENT OCR (bytes) ===");

        // Préprocessing de l'image
        byte[] processedImage = imagePreprocessor.preprocessChequeImage(imageData);

        // Créer fichier temporaire pour Tesseract
        File tempFile = File.createTempFile("processed_cheque_", ".png");
        Files.write(tempFile.toPath(), processedImage);

        String extractedText = tesseract.doOCR(tempFile);

        System.out.println("=== TEXTE EXTRAIT ===");
        System.out.println(extractedText);

        // Nettoyer
        tempFile.delete();

        return extractedText;
    }

    private void configureTesseractForCheques(ITesseract tesseract) {
        // Configuration optimisée pour les chèques
        tesseract.setLanguage("fra+ara+eng"); // Français + Arabe + Anglais
        tesseract.setPageSegMode(6);          // PSM_SINGLE_BLOCK - pour documents structurés
        tesseract.setOcrEngineMode(1);        // OEM_LSTM_ONLY - meilleur pour texte imprimé

        // Caractères autorisés (optimisé pour chèques)
        tesseract.setTessVariable("tessedit_char_whitelist",
                "0123456789" +
                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
                        "ÀÂÇÉÈÊËÎÏÔÙÛÜàâçéèêëîïôùûü" +
                        ".,/- €$DT ");

        tesseract.setTessVariable("user_defined_dpi", "300");
        tesseract.setTessVariable("preserve_interword_spaces", "1");
        tesseract.setTessVariable("textord_tabfind_find_tables", "1");
    }

    // Méthode originale conservée pour compatibilité (optionnelle)
    public String extractTextFromImageWithoutPreprocessing(File imageFile) throws TesseractException {
        ITesseract tesseract = new Tesseract();

        String tessDataPath = new File("src/main/resources/TestData").getAbsolutePath();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("fra+eng");

        return tesseract.doOCR(imageFile);
    }
}