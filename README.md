# Auction App

Ứng dụng đấu giá trực tuyến viết bằng Java, JavaFX và Maven. Hệ thống được thiết kế theo mô hình client-server, hỗ trợ nhiều vai trò người dùng, quản lý phiên đấu giá, đặt giá realtime, auto-bidding, anti-sniping và lưu trữ dữ liệu bằng H2 Database.

## 1. Mô tả bài toán và phạm vi hệ thống

Auction App mô phỏng một sàn đấu giá trực tuyến cho phép seller tạo và quản lý sản phẩm, bidder tham gia đấu giá, admin theo dõi người dùng và quản lý trạng thái phiên đấu giá. Client JavaFX giao tiếp với server qua TCP socket bằng message JSON, server xử lý nghiệp vụ và phát sự kiện realtime về các client đang đăng nhập.

Phạm vi chính:

- Quản lý người dùng theo vai trò `BIDDER`, `SELLER`, `ADMIN`.
- Đăng nhập, đăng ký tài khoản bidder/seller, cập nhật thông tin cá nhân và số dư ví.
- Seller tạo, sửa, xóa phiên đấu giá khi phiên chưa có bid.
- Bidder đặt giá thủ công, cấu hình auto-bid và xem lịch sử bid.
- Server tự động cập nhật vòng đời đấu giá: `OPEN`, `RUNNING`, `FINISHED`, `PAID`, `CANCELED`.
- Xử lý ví người dùng: giữ tiền của bidder đang dẫn đầu, hoàn tiền khi bị vượt giá/hủy phiên, cộng tiền cho seller khi phiên kết thúc/thanh toán.
- Cập nhật realtime khi có tạo/sửa/xóa phiên, đặt giá, auto-bid hoặc thay đổi trạng thái.

## 2. Công nghệ sử dụng

- Java 21
- JavaFX 21.0.6
- Maven
- H2 Database 2.3.232
- Jackson Databind và Jackson Java Time
- JUnit 5
- GitHub Actions

## 3. Yêu cầu cài đặt

Cần cài đặt:

- JDK 21 trở lên
- Maven 3.9 trở lên
- Git nếu muốn clone repo từ xa

Kiểm tra môi trường:

```bash
java -version
mvn -version
```

Dữ liệu H2 được tạo trong thư mục `data/` khi server khởi động. Nếu muốn reset dữ liệu mẫu, dừng ứng dụng và xóa các file database trong `data/auction-db*`.

## 4. Cấu trúc thư mục

```text
.
+-- .github/workflows/          # CI/CD với GitHub Actions
+-- data/                       # H2 database và file server.pid khi chạy
+-- src/main/
|   +-- AuctionLauncher.java    # Entry point client; tự tìm/khởi động server nếu cần
|   +-- client/                 # JavaFX app, controller, client socket, session state
|   +-- server/                 # Server socket, service, DAO, scheduler, seed data
|   +-- shared/                 # Model, DTO, protocol, util, factory dùng chung
|   +-- resources/              # FXML, CSS, icon
+-- pom.xml                     # Maven config
+-- README.md
```

## 5. Cách chạy nhanh một máy

Lệnh này phù hợp khi demo trên một máy. Ứng dụng sẽ tìm server trên LAN; nếu không thấy server nào, nó tự khởi động embedded server cục bộ rồi mở JavaFX client.

```bash
mvn clean javafx:run
```

Tài khoản mẫu được tạo tự động nếu database chưa có dữ liệu:

| Vai trò | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `admin123` |
| Seller | `seller1` | `seller123` |
| Bidder | `bidder1` | `bid123` |
| Bidder | `bidder2` | `bid123` |

## 6. Chạy Server/Client riêng

Server lắng nghe TCP port `5555` và UDP discovery port `5556`.

### Windows PowerShell

Build project và tạo classpath:

```powershell
mvn -q -DskipTests package
mvn -q dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

Chạy server:

```powershell
$cp = "target/classes;" + (Get-Content target/classpath.txt)
java -cp $cp main.server.AuctionServerMain
```

Mở terminal khác và chạy client:

```powershell
mvn javafx:run
```

### macOS/Linux

Build project và tạo classpath:

```bash
mvn -q -DskipTests package
mvn -q dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

Chạy server:

```bash
java -cp "target/classes:$(cat target/classpath.txt)" main.server.AuctionServerMain
```

Mở terminal khác và chạy client:

```bash
mvn javafx:run
```

Nếu server và client nằm trên các máy khác nhau trong cùng LAN, client sẽ tìm server bằng UDP broadcast. Nếu firewall chặn UDP/TCP, cần mở port `5555` và `5556`.

## 7. Lệnh build và kiểm thử

Build không chạy test:

```bash
mvn clean package -DskipTests
```

Chạy test:

```bash
mvn test
```

Chạy verify giống workflow quality:

```bash
mvn clean verify
```

Repo đã cấu hình JUnit 5 và GitHub Actions. Hai workflow hiện có:

- `.github/workflows/build.yml`: build Maven package và chạy `mvn test` trên JDK 21.
- `.github/workflows/quality.yml`: chạy `mvn clean verify` trên JDK 21.

Lưu ý: trong source hiện tại chưa thấy thư mục `src/test`, nên cần bổ sung test case nếu yêu cầu nộp bài bắt buộc có unit test thực thi.

## 8. Chức năng đã hoàn thành

- Quản lý người dùng: đăng nhập, đăng ký bidder/seller, cập nhật thông tin cá nhân, số dư ví, admin xem danh sách user.
- Quản lý sản phẩm/phiên đấu giá: seller/admin tạo, sửa, xóa phiên với các loại `ELECTRONICS`, `ART`, `VEHICLE`.
- Tham gia đấu giá: bidder đặt giá, validate giá hợp lệ, validate số dư ví, không cho seller tự bid sản phẩm của mình.
- Cập nhật realtime: server broadcast sự kiện `AUCTION_CHANGED` đến các client đang đăng nhập.
- Kết thúc phiên đấu giá: scheduler tự động chuyển trạng thái theo thời gian và xác định người thắng.
- Xử lý ví: trừ tiền người đang dẫn đầu, hoàn tiền khi bị vượt giá/hủy phiên, cộng tiền seller khi thanh toán.
- Xử lý lỗi và ngoại lệ: service/controller trả lỗi nghiệp vụ về client và hiển thị bằng alert.
- Giao diện GUI JavaFX: login, dashboard, tab bidder/seller/admin, bảng dữ liệu, form tạo/sửa phiên.
- OOP: sử dụng kế thừa cho `User` và `Item`, đóng gói model/service, đa hình theo role và loại item.
- Design patterns: Singleton (`DatabaseManager`), Factory (`ItemFactory`), Observer/Event style qua listener và broadcast realtime.
- Kiến trúc Client-Server + MVC: JavaFX controller, service, DAO, socket server/client, DTO/protocol dùng chung.
- Xử lý đồng thời: server dùng thread cho client session và lock theo từng auction để tránh race condition khi nhiều bidder đặt giá cùng lúc.

## 9. Chức năng nâng cao

- Auto-Bidding: bidder cấu hình `maxBid` và `increment`; server tự động đặt giá theo rule khi có cạnh tranh.
- Anti-sniping: nếu có bid trong 15 giây cuối, phiên được gia hạn thêm 30 giây.
- Bid History Visualization: biểu đồ line chart hiển thị biến động giá theo lịch sử bid.
- Network discovery: client tự tìm server trên LAN bằng UDP broadcast.

## 10. Thiết kế chính

### Client

- `AuctionClientApp`: khởi tạo JavaFX app.
- `AppContext`: quản lý stage, state, connection và chuyển view.
- `LoginController`, `DashboardController`, `UserInfoController`: xử lý giao diện FXML.
- `AuctionClientConnection`: quản lý socket, gửi request và nhận response/event.

### Server

- `AuctionServerMain`: entry point server riêng.
- `AuctionEmbeddedServer`: khởi tạo database, seed data, socket server, discovery responder và lifecycle scheduler.
- `AuctionSocketServer`, `ClientSession`, `SessionRegistry`: quản lý kết nối client.
- `ServerRequestController`: route message từ client đến service.
- `AuthService`, `AuctionService`, `AuctionRulesEngine`: xử lý nghiệp vụ.
- `UserDao`, `AuctionDao`, `DatabaseManager`: truy cập H2 database.

### Shared

- `model`: entity/domain model như `Auction`, `User`, `Item`, `BidTransaction`, `AutoBidConfig`.
- `dto`: request/response object giữa client và server.
- `protocol`: `ApiMessage`, `MessageType`, `MessageCategory`.
- `util`: JSON, password, money, time.

## 11. Link báo cáo và video demo

- Báo cáo PDF: cập nhật link tại đây.
- Video demo: cập nhật link tại đây.

