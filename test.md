# LastCall Eats — Functional Test Checklist

All test accounts use password: `111111`. See `start.md` for details.

---

## User Side

### Authentication
| Feature | Action | Expected Result |
|--------|--------|----------------|
| User Registration | Enter email, password, nickname → Click *Create Account* | Registration successful, redirect to Browse page |
| Duplicate Registration | Register with an existing email | Error: email already exists |
| User Login | Login with `alice@example.com / 111111` | Login successful, redirect to Browse page |
| Invalid Password | Enter wrong password | Error: invalid credentials |

---

### Browse (Product Browsing)
| Feature | Action | Expected Result |
|--------|--------|----------------|
| Browse Listings | Enter Browse page | Display all available listings for today |
| Search | Enter “Sushi” or “Sakura” | Filtered results shown |
| Pull to Refresh | Pull down list | Data reloads |
| Add to Favorites | Tap heart icon | Turns red, added to favorites |
| Remove from Favorites | Tap again | Removed from favorites |
| View Details | Tap a listing | Navigate to listing detail page |

---

### Listing Details
| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Details | Open detail page | Show product name, merchant, original price, discount price, remaining quantity, pickup time |
| Favorite (Detail Page) | Tap heart icon | Syncs with Browse page |
| Create Order | Tap *Reserve* | Navigate to payment page |
| Duplicate Order | Tap *Reserve* again | Error: order already exists |

---

### Payment
| Feature | Action | Expected Result |
|--------|--------|----------------|
| Valid Payment | Enter `pm_card_visa` → Pay | Navigate to pickup code page |
| Invalid Payment | Enter random string | Stripe error displayed |

---

### Pickup Code
| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Pickup Code | After payment | Show 6-digit code + QR code + status = PAID |

---

### Favorites
| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Favorites | Open Favorites tab | Show saved listings with details |
| Remove Favorite | Tap heart | Removed from list |
| Refresh | Pull down | Reload list |

---

### Community
| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Feed | Open Community tab | Show all posts, avatar = first letter of username |
| Create Post | Tap + → Enter content → Post | Appears at top |
| My Posts | Switch tab | Show only user’s posts |
| Delete Post | Tap × | Removed from list |

---

### Profile
| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Profile | Open Profile tab | Show email and nickname |
| Edit Nickname | Update and save | Successfully updated |
| Logout | Tap logout | Redirect to welcome page |

---

## Merchant Side

### Authentication
| Feature | Action | Expected Result |
|--------|--------|----------------|
| Merchant Registration | Enter email, password, store name, address | Registration successful |
| Merchant Login | `bakery@example.com / 111111` | Login successful |

---

### Products — Templates
**Entry:** Products tab → Templates

| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Templates | Open tab | Show all templates |
| Create Template | Click + → Fill info → Save | Appears in list |
| Edit Template | Modify → Save | Updated |
| Delete Template | Click trash → Confirm | Removed |

---

### Products — Listings (Daily Listings)
**Entry:** Products tab → Today’s Listings

| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Listings | Open tab | Show all listings |
| Create Listing | Fill info → Create | Appears and visible to users |
| Delete Listing | Click trash | Removed from Browse page |

---

### Orders
**Entry:** Orders tab

| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Orders | Open tab | Show all orders and status |

---

### Verify Pickup Code
**Entry:** Orders tab → Verify

| Feature | Action | Expected Result |
|--------|--------|----------------|
| Enter Code | Input 6-digit code | Show user + product, success |
| Scan QR | Scan QR code | Same result |
| Duplicate Verification | Verify again | Error: already used |

---

### Profile
| Feature | Action | Expected Result |
|--------|--------|----------------|
| View Profile | Open tab | Show email, store name, address |
| Edit Profile | Update → Save | Updated |
| Logout | Tap logout | Redirect to welcome page |

---

## API Endpoints Overview

| Module | Method | Endpoint |
|--------|--------|----------|
| Auth | POST | `/api/auth/register/user` |
| | POST | `/api/auth/register/merchant` |
| | POST | `/api/auth/login/user` |
| | POST | `/api/auth/login/merchant` |
| User | GET | `/api/user/profile` |
| | PUT | `/api/user/profile` |
| | GET | `/api/user/favorites` |
| | POST | `/api/user/favorites/{listingId}` |
| | DELETE | `/api/user/favorites/{listingId}` |
| Merchant | GET | `/api/merchant/profile` |
| | PUT | `/api/merchant/profile` |
| | GET | `/api/merchant/dashboard` |
| Products (Templates) | POST | `/api/merchant/templates` |
| | GET | `/api/merchant/templates` |
| | PUT | `/api/merchant/templates/{id}` |
| | DELETE | `/api/merchant/templates/{id}` |
| Listings | GET | `/api/products/browse` |
| | POST | `/api/merchant/listings` |
| | GET | `/api/merchant/listings` |
| | DELETE | `/api/merchant/listings/{id}` |
| Orders | POST | `/api/orders` |
| | GET | `/api/orders` |
| | GET | `/api/orders/{id}` |
| | GET | `/api/orders/{id}/pickup-code` |
| | GET | `/api/merchant/orders` |
| | PUT | `/api/merchant/orders/verify` |
| Payment | POST | `/api/payment/create` |
| | POST | `/api/payment/webhook` |
| Community | POST | `/api/posts` |
| | GET | `/api/posts` |
| | DELETE | `/api/posts/{postId}` |
| Reviews | POST | `/api/reviews` |
| | GET | `/api/reviews/order/{orderId}` |
| | GET | `/api/reviews/merchant/{merchantId}` |







---
# LastCall Eats — 功能测试清单

测试账号密码统一为 `111111`，详见 `start.md`。

---

## 用户端

### 认证
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 用户注册 | 填写邮箱、密码、昵称，点击 Create Account | 注册成功，自动跳转 Browse 页 |
| 重复邮箱注册 | 使用已注册邮箱再次注册 | 提示邮箱已存在 |
| 用户登录 | 使用 alice@example.com / 111111 登录 | 登录成功，跳转 Browse 页 |
| 错误密码登录 | 密码填写错误 | 提示凭证无效 |

### Browse（浏览商品）
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 浏览列表 | 进入 Browse 页 | 显示当日所有可用 listing |
| 搜索 | 输入 "Sushi" 或 "Sakura" | 列表过滤显示匹配结果 |
| 下拉刷新 | 下拉列表 | 重新加载数据 |
| 卡片收藏 | 点击卡片右上角心形 | 变为红心，加入收藏 |
| 取消收藏 | 再次点击红心 | 变为空心，从收藏移除 |
| 进入详情 | 点击卡片 | 跳转 listing 详情页 |

### Listing 详情
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看详情 | 进入详情页 | 显示商品名、商家名、原价、折扣价、剩余数量、取货时间 |
| 详情页收藏 | 点击右上角心形 | 状态与 Browse 页同步 |
| 创建订单 | 点击 Reserve | 跳转支付页面 |
| 重复下单 | 同一 listing 再次点击 Reserve | 提示已存在订单 |

### 支付
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 正常支付 | 输入 `pm_card_visa`，点击 Pay | 跳转取货码页面 |
| 无效支付方式 | 输入随机字符串 | 提示 Stripe 报错 |

### 取货码
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看取货码 | 支付成功后 | 显示 6 位数字码 + QR 二维码 + 订单状态 PAID |

### Favourites（收藏）
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看收藏列表 | 进入 Favourites tab | 显示已收藏的 listing，包含商品名、商家名、折扣价、取货时间 |
| 取消收藏 | 点击心形 | 从列表移除 |
| 下拉刷新 | 下拉列表 | 重新加载 |

### Community（社区）
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看 Feed | 进入 Community tab | 显示所有帖子，头像显示用户昵称首字母 |
| 发布帖子 | 点击 + 按钮，输入内容，点击 Post | 帖子出现在列表顶部 |
| 查看 My Posts | 切换到 My Posts tab | 只显示当前用户的帖子 |
| 删除帖子 | 点击自己帖子右上角 × | 帖子从列表移除 |

### Profile（个人资料）
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看资料 | 进入 Profile tab | 显示邮箱、昵称 |
| 修改昵称 | 修改昵称并保存 | 昵称更新成功 |
| 退出登录 | 点击登出 | 跳转回欢迎页 |

---

## 商家端

### 认证
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 商家注册 | 填写邮箱、密码、店名、地址 | 注册成功，跳转商家首页 |
| 商家登录 | 使用 bakery@example.com / 111111 | 登录成功 |

### Products — Templates（商品模板）

> 入口：Products tab → Templates

| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看模板列表 | 进入 Products tab | 显示该商家的所有模板 |
| 新建模板 | 点击 + → 填写名称、描述、原价 → Save | 模板出现在列表 |
| 编辑模板 | 点击模板编辑图标 → 修改 → Save | 模板信息更新 |
| 删除模板 | 点击垃圾桶图标 → 确认 | 模板从列表移除 |

### Products — Listings（限时上架）

> 入口：Products tab → Today's Listings

| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看上架列表 | 切换到 Today's Listings | 显示当前商家的所有 listing |
| 新建 listing | 点击 + → 选择模板、填写折扣价、数量、取货时间、日期 → Create | listing 出现在列表，用户端 Browse 可见 |
| 下架 listing | 点击垃圾桶 → 确认 | listing 从用户 Browse 页消失 |

### Orders（订单管理）

> 入口：Orders tab

| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看订单列表 | 进入 Orders tab | 显示所有用户对该商家的订单及状态 |

### 核销取货码

> 入口：Orders tab → Verify

| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 输入数字码核销 | 输入用户的 6 位取货码 → 提交 | 显示用户昵称和商品名，核销成功 |
| 扫 QR 码核销 | 扫描用户取货码页面的 QR 码 | 同上 |
| 重复核销 | 对已核销订单再次提交 | 提示已使用 |

### Profile（商家资料）
| 功能 | 操作 | 预期结果 |
|------|------|---------|
| 查看资料 | 进入 Profile tab | 显示邮箱、店名、地址 |
| 修改资料 | 修改店名或地址 → 保存 | 更新成功 |
| 退出登录 | 点击登出 | 跳转回欢迎页 |

---

## API 接口一览

| 模块 | 方法 | 路径 |
|------|------|------|
| **认证** | POST | `/api/auth/register/user` |
| | POST | `/api/auth/register/merchant` |
| | POST | `/api/auth/login/user` |
| | POST | `/api/auth/login/merchant` |
| **用户** | GET | `/api/user/profile` |
| | PUT | `/api/user/profile` |
| | GET | `/api/user/favorites` |
| | POST | `/api/user/favorites/{listingId}` |
| | DELETE | `/api/user/favorites/{listingId}` |
| | GET | `/api/user/favorites/{listingId}` |
| **商家** | GET | `/api/merchant/profile` |
| | PUT | `/api/merchant/profile` |
| | GET | `/api/merchant/dashboard` |
| | GET | `/api/merchant/{id}` |
| **商品模板** | POST | `/api/merchant/templates` |
| | GET | `/api/merchant/templates` |
| | PUT | `/api/merchant/templates/{id}` |
| | DELETE | `/api/merchant/templates/{id}` |
| **限时商品** | GET | `/api/products/browse` |
| | POST | `/api/merchant/listings` |
| | GET | `/api/merchant/listings` |
| | DELETE | `/api/merchant/listings/{id}` |
| **订单** | POST | `/api/orders` |
| | GET | `/api/orders` |
| | GET | `/api/orders/{id}` |
| | GET | `/api/orders/{id}/pickup-code` |
| | GET | `/api/merchant/orders` |
| | PUT | `/api/merchant/orders/verify` |
| **支付** | POST | `/api/payment/create` |
| | POST | `/api/payment/webhook` |
| **社区** | POST | `/api/posts` |
| | GET | `/api/posts` |
| | GET | `/api/posts/{postId}` |
| | GET | `/api/posts/user/{userId}` |
| | GET | `/api/posts/merchant/{merchantId}` |
| | DELETE | `/api/posts/{postId}` |
| **评价** | POST | `/api/reviews` |
| | GET | `/api/reviews/order/{orderId}` |
| | GET | `/api/reviews/merchant/{merchantId}` |
| | GET | `/api/reviews/template/{templateId}` |