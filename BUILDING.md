# Hướng dẫn build APK trong WSL2

Môi trường Android hiện có thể build bằng Docker trong WSL2, dùng chung image Flutter `ghcr.io/cirruslabs/flutter:3.27.1` và volume cache Gradle `chudu-ngoaingu_gradle_cache` (đã dùng chung với project `chudu-ngoaingu`).

## Build project Alarm

Chạy từ PowerShell tại thư mục gốc workspace:

```powershell
wsl -e bash -lc 'docker run --rm -v /mnt/e/xampp/ipc/check_in_repo/check-in/alarm:/app -v /mnt/e/xampp/ipc/check_in_repo/check-in/alarm/.android:/root/.android -v chudu-ngoaingu_gradle_cache:/root/.gradle -w /app ghcr.io/cirruslabs/flutter:3.27.1 bash gradlew clean assembleDebug --no-daemon'
```

Gọi `bash gradlew` trực tiếp trong image; không cần override entrypoint. `clean` giúp Gradle nhận đủ file Java mới được thêm hoặc tách riêng.

APK đầu ra:

```text
alarm/app/build/outputs/apk/debug/app-debug.apk
```

Metadata phiên bản nằm cạnh APK trong `output-metadata.json`.

## Dùng cho project Android khác

Thay đường dẫn `.../check-in/alarm` trong bind mount `-v <project>:/app` bằng đường dẫn project Android tính từ WSL (`/mnt/<ổ>/<đường/dẫn>`), đổi `-w` thành thư mục chứa `gradlew`, rồi dùng cùng image và volume cache. Nếu project cần ký APK bằng keystore riêng, mount keystore vào container đúng vị trí mà cấu hình Gradle của project yêu cầu. APK thường nằm ở `<project>/app/build/outputs/apk/`.

## Đẩy thay đổi Check In lên GitHub

Project Check In dùng nhánh `main` và remote `origin`. Chạy trong thư mục `check-in`:

```powershell
git status
git add index.html sw.js
git commit -m "Mô tả ngắn thay đổi"
git push origin main
```

Nếu sửa thêm tài nguyên khác, thêm các file đó vào lệnh `git add`. Kiểm tra `git status` sau khi push để chắc chắn working tree đã sạch.