# Fix: Null Fields in User List Cells

## Problem
In the `UserListController`, the list cells were displaying null values for all fields except email:
- Name (fullName) → null
- Phone Number (phoneNumber) → null
- Address (address) → null
- Date of Birth (dateOfBirth) → null

Email was displaying correctly because it's returned in English by the server.

## Root Cause
**Deserialization Mismatch**:
1. The server endpoint `/api/users/all-users` returns user data as `ThongTinNguoiDung` objects
2. `ThongTinNguoiDung` uses Vietnamese field names:
   - `hoTen` (instead of `fullName`)
   - `soDienThoai` (instead of `phoneNumber`)
   - `diaChi` (instead of `address`)
   - `ngaySinh` (instead of `dateOfBirth`)
   - `maNguoiDung` (instead of `userId`)
   - `soTaiKhoanNganHang` (instead of `bankAccountNumber`)
   - `tenNganHang` (instead of `bankName`)

3. The client's `ApiClient.getAllUsers()` tries to deserialize this JSON directly into `List<User>` objects
4. Gson couldn't map the Vietnamese field names to the English property names, so these fields remained null

## Solution
Added `@SerializedName` annotations to the `User` and `Person` model classes to map Vietnamese JSON field names to English Java properties.

### Changes Made:

#### 1. `Person.java` (Shared Module)
Added `@SerializedName` annotations for:
```java
@SerializedName("maNguoiDung")
private String userId;

@SerializedName("hoTen")
private String fullName;

@SerializedName("ngaySinh")
private String dateOfBirth;
```

#### 2. `User.java` (Shared Module)
Added `@SerializedName` annotations for:
```java
@SerializedName("diaChi")
private String address;

@SerializedName("soDienThoai")
private String phoneNumber;

@SerializedName("soTaiKhoanNganHang")
private String bankAccountNumber;

@SerializedName("tenNganHang")
private String bankName;
```

## How It Works
When Gson deserializes the server's JSON response:
1. The JSON field `"hoTen"` is mapped to the Java property `fullName`
2. The JSON field `"soDienThoai"` is mapped to the Java property `phoneNumber`
3. The JSON field `"diaChi"` is mapped to the Java property `address`
4. The JSON field `"ngaySinh"` is mapped to the Java property `dateOfBirth`
5. Other fields follow the same pattern

## Result
Users in the list will now display complete information:
- ✅ Name
- ✅ Email
- ✅ Phone Number
- ✅ Address
- ✅ Date of Birth
- ✅ Ban Status

## Files Modified
1. `D:\Git\APPDAUGIAV1\shared\src\main\java\com\mycompany\models\Person.java`
2. `D:\Git\APPDAUGIAV1\shared\src\main\java\com\mycompany\models\User.java`

## Testing
The fix can be verified by:
1. Building the project: `mvn clean compile`
2. Running the server and client
3. Navigating to the User List page
4. Verifying that all user fields are populated (no null values)

