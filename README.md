# Check In

Ung dung web toi gian de ghi lich Check In va tinh chi phi theo ky chot rieng cua tung muc. Du lieu duoc luu tren thiet bi bang `localStorage`.

Mo `index.html` de xem truoc.

## Quy tac ma hoa va chinh sua

- Luu `index.html`, `sw.js` va tai lieu Markdown bang UTF-8.
- Khi sua noi dung tieng Viet, dung cong cu doc/ghi tep ho tro UTF-8. Tranh truyen Unicode qua pipe PowerShell/Python theo bang ma he thong.
- Sau khi sua, kiem tra nhan tieng Viet va chay `git diff --check`. Neu sua JavaScript, chay `node --check` voi phan script.
- Day la web tinh, khong co buoc build. Khi sua tai nguyen duoc cache, tang `CACHE_NAME` trong `sw.js` de trinh duyet nhan ban moi.
