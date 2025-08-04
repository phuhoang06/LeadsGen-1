package com.mm.image_aws.repo;

import com.mm.image_aws.entity.UploadJob;
import com.mm.image_aws.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadJobRepository extends JpaRepository<UploadJob, Long> {

    // Spring Data JPA sẽ tự động tạo câu lệnh query dựa trên tên phương thức
    // SỬA LỖI: Đổi kiểu tham số từ String sang Long để khớp với kiểu của trường 'jobId' trong entity UploadJob.
    Optional<UploadJob> findByJobId(Long jobId);



    List<UploadJob> findByUsername(String username);
}
