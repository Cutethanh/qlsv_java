# QLSV - Ứng dụng quản lý sinh viên dùng Java Swing

Ứng dụng desktop quản lý sinh viên, điểm và tài khoản,
mọi chức năng được thiết kế theo các nguyên tắc sau:
 - băm mật khẩu
- mã hoá
dữ liệu nhạy cảm
- khoá tài khoản khi dò mật khẩu
- phân quyền
- nhật ký log và kiểm tra toàn vẹn dữ liệu.

## Cách chạy
| Cách | Thao tác |
|---|---|
| Chạy nhanh | Nháy đúp `qlsv.jar` hoặc `java -jar qlsv.jar` |
| Từ mã nguồn | Nháy đúp `chay.bat` (Win) hoặc `./chay.sh` |

Lần chạy đầu, ứng dụng tự tạo thư mục `data/` gồm khoá bí mật `secret.key` và dữ liệu mẫu.

## Tài khoản demo
| Tài khoản | Mật khẩu | Ghi chú |
|---|---|---|
| `admin` | `Admin@2026` | Quản trị viên |
| `sv001` … `sv023` | `Sinhvien@2026` | Sinh viên đã kích hoạt |
| `sv024`, `sv025` | `Sinhvien@2026` | Chờ kích hoạt: bắt buộc cập nhật thông tin + đổi mật khẩu |

Mật khẩu giống nhau nhưng mỗi tài khoản vẫn có salt riêng nên chuỗi băm khác nhau.

## Kiểm thử tự động
Chạy `kiemthu.bat` hoặc `./kiemthu.sh`. Bộ kiểm thử gồm 37 ca: lưu trữ mật khẩu,
mã hoá CCCD, khoá tài khoản, chính sách mật khẩu, phân quyền/IDOR, quên mật khẩu, kích hoạt
lần đầu, phát hiện sửa/xoá file dữ liệu và bộ lọc giải tuần tự hoá.

## Cơ chế bảo mật chính
| Mối đe doạ | Biện pháp | Lớp cài đặt |
|---|---|---|
| Lộ file dữ liệu → lộ mật khẩu | PBKDF2-HMAC-SHA256, salt 16 byte, 120 000 vòng | `PasswordHasher` |
| Lộ CCCD, số điện thoại | AES-256-GCM, IV ngẫu nhiên; che trên giao diện; xem đầy đủ phải nhập lại mật khẩu | `CryptoService`, `StudentController` |
| Dò mật khẩu (brute-force) | Khoá 15 phút sau 5 lần sai; thông báo lỗi chung; chính sách mật khẩu mạnh | `StudentController`, `InputValidator` |
| Sinh viên xem dữ liệu người khác (IDOR) | Kiểm tra quyền ở tầng controller cho từng thao tác | `StudentController` |
| Sửa / xoá file dữ liệu ngoài ứng dụng | Chữ ký HMAC-SHA256, bản sao lưu `.bak`, tự khôi phục và cảnh báo | `DataManager` |
| Tấn công giải tuần tự hoá | Kiểm chữ ký trước khi đọc + bộ lọc chỉ cho phép lớp của hệ thống | `DataManager` |
| Bỏ quên máy đang đăng nhập | Tự đăng xuất sau 5 phút không thao tác | `SessionGuard` |
| Chối bỏ / không truy vết được | Nhật ký kiểm toán có mức độ, ghi giá trị cũ → mới khi sửa điểm | `StudentController`, `LogEntry` |

