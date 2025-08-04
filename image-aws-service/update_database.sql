-- Script để cập nhật database cho image-service
-- Chạy script này để fix lỗi ORA-12899: value too large for column

-- 1. Tăng độ dài cột S_IMAGE_URL từ 2048 lên 10000
ALTER TABLE TB_UPLOAD_JOB MODIFY S_IMAGE_URL VARCHAR2(10000);

-- 2. Thêm cột N_URL_COUNT để lưu số lượng URLs
ALTER TABLE TB_UPLOAD_JOB ADD N_URL_COUNT NUMBER(10);

-- 3. Cập nhật dữ liệu hiện tại (nếu có)
-- Nếu có dữ liệu cũ, có thể cần xử lý thêm
-- UPDATE TB_UPLOAD_JOB SET N_URL_COUNT = 1 WHERE N_URL_COUNT IS NULL;

-- 4. Commit thay đổi
COMMIT;

-- 5. Kiểm tra kết quả
SELECT column_name, data_type, data_length 
FROM user_tab_columns 
WHERE table_name = 'TB_UPLOAD_JOB' 
AND column_name IN ('S_IMAGE_URL', 'N_URL_COUNT'); 