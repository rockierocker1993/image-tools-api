package id.rockierocker.imagetools.repository;

import id.rockierocker.imagetools.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findByCategoryOrderBySortOrderAsc(String category);

}
