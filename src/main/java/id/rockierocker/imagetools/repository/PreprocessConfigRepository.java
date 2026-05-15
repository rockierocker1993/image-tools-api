package id.rockierocker.imagetools.repository;

import id.rockierocker.imagetools.entity.PreprocessConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreprocessConfigRepository extends JpaRepository<PreprocessConfig, Long> {

    Optional<PreprocessConfig> findFirstByConfigCode(String configCode);

}

