package com.mm.image_aws.repo;

import com.mm.image_aws.entity.UploadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadJobRepository extends JpaRepository<UploadJob, Long> {

    /**
     * === SỬA LỖI: Bổ sung phương thức còn thiếu ===
     * Tìm tất cả các job của một người dùng, sắp xếp theo ngày tạo mới nhất.
     * Spring Data JPA sẽ tự động tạo câu lệnh query từ tên phương thức này.
     */
    List<UploadJob> findByUsernameOrderByCreatedAtDesc(String username);
}
