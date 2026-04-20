package id.rockierocker.imagetools.repository;

import id.rockierocker.imagetools.model.VtraceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VtraceConfigRepository extends JpaRepository<VtraceConfig, Long> {
    Optional<VtraceConfig> findFirstByConfigCode(String configCode);
}

