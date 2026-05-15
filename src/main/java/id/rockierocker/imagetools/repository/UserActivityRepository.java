package id.rockierocker.imagetools.repository;

import id.rockierocker.imagetools.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    Optional<UserActivity> findFirstByRequestId(String requestId);
}
