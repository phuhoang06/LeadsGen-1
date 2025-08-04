Tổng kết công việc đã làm - Dự án LeadGen

Những gì đã làm xong

1. Tách service

User Service (chạy ở port 8081): lo về user, đăng nhập, đăng ký, xử lý số lượng lớn.

Image Service (chạy ở port 8082): upload và xử lý hình ảnh, đưa lên AWS S3.

2. Công nghệ 

Spring Boot 3.3.1 + Java 17

Oracle Database + Redis

AWS S3 + CloudFront

JWT để xác thực

3. Tính năng đã làm

User Service:

Đăng ký, đăng nhập user

Chia nhỏ dữ liệu: Thay vì xử lý một file lớn duy nhất chia nhỏ file thành các lô, mỗi lô khoảng 1000 người dùng

Xử lý song song: Sử dụng Thread Pool để chạy nhiều lô dữ liệu cùng lúc, tận dụng tối đa CPU

Batch Insert: Thay vì chạy từng lệnh INSERT, gộp cả lô dữ liệu thành một câu lệnh để giảm số lần gọi DB

Import CSV

Sinh JWT token

Image Service:

Upload ảnh từ link (Dropbox, Google Drive, direct link)

Upload lên AWS S3 + dùng CloudFront để lấy link public

Xử lý dạng job async (không chặn)

Giới hạn tần suất gọi API (1 request/giây/user)

Lấy metadata từ ảnh

Theo dõi trạng thái job

4. Các service nói chuyện với nhau

Xác thực JWT từ User Service

Dùng HTTP để gọi qua lại

Thiết kế stateless (không giữ state)

5. Database & tối ưu hiệu năng

Oracle + Hibernate tối ưu

Redis để cache và rate limiting

Batch processing cho việc insert nhiều

Tối ưu connection pooling

Tối ưu tốc độ import lượng lớn người dùng 

