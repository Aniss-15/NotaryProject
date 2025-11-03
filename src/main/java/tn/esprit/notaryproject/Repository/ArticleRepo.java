package tn.esprit.notaryproject.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.notaryproject.Entities.Article;

public interface ArticleRepo extends JpaRepository<Article,Long> {

}
