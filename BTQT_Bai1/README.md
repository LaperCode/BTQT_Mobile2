# Gold Tracker (Android)

Ứng dụng theo dõi giá vàng và quy đổi sang VND.

## Tính năng chính

- Lấy giá vàng thế giới qua MetalPriceAPI.
- Quy đổi nhanh giữa các đơn vị vàng (Lượng, Chỉ, Gram, Ounce).
- Bảng giá mua/bán cho SJC, PNJ, Nhẫn 9999 và Vàng Thế Giới.
- Lưu lịch sử tra cứu vào SQLite.
- Biểu đồ biến động 7 ngày gần nhất (tự động fallback nếu API lịch sử không khả dụng).

## Ghi chú cấu hình

- Đã có sẵn API key trong `MainActivity` (bạn có thể thay thế bằng key khác).
- Tỷ giá USD/VND đang dùng mặc định là `25,000` trong `MainActivity`.

## Build & Test nhanh

- Chạy bằng Android Studio hoặc Gradle Wrapper.
- Unit test mẫu nằm ở `app/src/test/java/com/example/btqt_bai1/GoldCalculatorTest.java`.
