# Báo thức

Ứng dụng Android hỗ trợ nhiều báo thức độc lập. Mỗi báo thức có giờ, tên ghi chú, bật/tắt riêng, số lượt reo 1–3 và lịch lặp theo ngày/tuần/tháng/năm. Ứng dụng dùng âm báo thức mặc định của Android/Samsung, không đóng gói file âm thanh. Lịch được phục hồi khi khởi động lại hoặc đổi giờ hệ thống.

## Build APK

- Windows/Android Studio: mở thư mục này, chờ Gradle đồng bộ rồi chọn **Build > Build APK(s)**.
- Windows command line: `gradlew.bat assembleDebug`
- WSL/Linux: `./gradlew assembleDebug`

APK debug nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

Cấp quyền thông báo và báo thức chính xác khi ứng dụng yêu cầu. Trên Samsung, vào thông tin ứng dụng và đặt Pin thành **Không hạn chế** để tránh hệ thống ngủ ứng dụng. Có thể dùng nút **Thử chuông ngay** để xác nhận âm lượng báo thức và âm báo hệ thống.
