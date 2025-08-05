package com.mm.image_aws.repo;

import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {

    /**
     * Tìm tất cả metadata ảnh của một người dùng, sắp xếp theo ngày tạo mới nhất.
     * Spring Data JPA sẽ tự động tạo câu lệnh query từ tên phương thức này.
     * "JobUsername" tương ứng với thuộc tính "username" trong entity "Job".
     */
    List<UploadJob> findByUsernameOrderByCreatedAtDesc(String username);
    List<ImageMetadata> findByJobUsernameOrderByCreatedAtDesc(String username);

}
