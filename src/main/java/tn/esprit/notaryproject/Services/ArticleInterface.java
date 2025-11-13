package tn.esprit.notaryproject.Services;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.notaryproject.Entities.Article;

import java.sql.Date;
import java.util.List;

public interface ArticleInterface {
    //hello1
    Article addArticle(Article article);
    Article updateArticle(Article article);
    void deleteArticle(Long IdArticle);
    List<Article> getArticles();
    Boolean validateArticleDate(Article article, MultipartFile imageFile);
    String extractTextFromImage(MultipartFile imageFile);






}
