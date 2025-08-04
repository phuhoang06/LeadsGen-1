package com.mm.user.repo;

import com.mm.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // --- THAY ĐỔI: Tìm user bằng email hoặc username ---
    Optional<User> findByUsernameOrEmail(String username, String email);

    // --- THÊM MỚI: Các phương thức kiểm tra sự tồn tại ---
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByUsernameOrEmail(String username, String email);

    // --- LẤY DANH SÁCH USERNAME VÀ EMAIL ĐÃ TỒN TẠI ---
    List<User> findByUsernameIn(List<String> usernames);
    List<User> findByEmailIn(List<String> emails);

    // --- GIỮ LẠI: Vẫn cần để lấy User entity từ UserDetails ---
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);
}