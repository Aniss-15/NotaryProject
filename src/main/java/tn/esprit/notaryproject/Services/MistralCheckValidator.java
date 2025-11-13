package tn.esprit.notaryproject.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class MistralCheckValidator {

    @Value("${mistral.api.url}")
    private String mistralUrl;

    @Value("${mistral.api.key}")
    private String mistralApiKey;

    public String validateChequeData(String ocrText) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("model", "mistral-large-latest");
        body.put("messages", new Object[]{
                Map.of(
                        "role", "user",
                        "content", "ANALYSE CE TEXTE OCR DE CHÈQUE ET RENVOIE UNIQUEMENT UN OBJET JSON SANS AUCUN TEXTE SUPPLÉMENTAIRE.\n\n" +
                                "STRUCTURE JSON ATTENDUE :\n" +
                                "{\n" +
                                "  \"montant_en_lettres\": \"valeur ou null\",\n" +
                                "  \"banque\": \"valeur ou null\",\n" +
                                "  \"signature\": \"YES ou NO\",\n" +
                                "  \"lisible\": true/false,\n" +
                                "  \"coherent\": true/false,\n" +
                                "  \"problemes\": [\"liste des problèmes\"]\n" +
                                "}\n\n" +
                                "CRITÈRES D'ANALYSE :\n" +
                                "1. BANQUE : Identifie le nom de la banque (ex: 'State Bank of India', 'HSBC', 'Société Générale', etc.)\n" +
                                "2. MONTANT EN LETTRES : Cherche le montant écrit en toutes lettres (ex: 'Five Thousand Only', 'Deux Mille Euros', etc.)\n" +
                                "3. SIGNATURE : 'YES' si une signature ou un nom de signataire est identifiable, 'NO' sinon\n" +
                                "4. LISIBLE : true si les champs critiques (banque, montant, signature) sont identifiables malgré le bruit OCR\n" +
                                "5. COHERENT : true si les éléments identifiés sont logiques entre eux\n\n" +
                                "RÈGLES IMPORTANTES :\n" +
                                "- Le montant numérique est OPTIONNEL et ne doit pas affecter la cohérence\n" +
                                "- Sois indulgent avec les erreurs OCR mineures\n" +
                                "- Un chèque est considéré 'lisible' si on peut identifier la banque, le montant en lettres et la signature\n" +
                                "- Un chèque est 'coherent' si les éléments identifiés forment un ensemble logique\n" +
                                "- Liste uniquement les problèmes majeurs qui empêchent la validation\n\n" +
                                "TEXTE OCR DU CHÈQUE :\n" + ocrText
                )
        });
        body.put("temperature", 0.1);
        body.put("max_tokens", 1000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mistralApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mistralUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }
}
