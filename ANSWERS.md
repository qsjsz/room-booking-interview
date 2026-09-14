# ANSWERS

## 第一题：实现时间窗口规则

### 实现思路

按照题目要求的优先级顺序进行校验：

1. start 或 end 缺失 → 返回 `MISSING_BOUNDARY`
2. end 不晚于 start → 返回 `END_NOT_AFTER_START`
3. 时长小于 30 分钟或大于 120 分钟 → 返回 `DURATION_OUT_OF_RANGE`
4. 其它情况 → 返回 `VALID`

校验顺序不能改变。

例如：

- start = 10:00，end = 09:00

该输入同时满足：

- end 不晚于 start
- 时长不合法

但根据题目要求，应优先返回：

```text
END_NOT_AFTER_START
```

而不是继续判断时长。

---

### 测试覆盖情况

#### 合法边界测试

测试：

```text
30分钟
120分钟
```

预期：

```text
VALID
```

原因：

题目明确说明：

```text
30分钟和120分钟均为合法边界
```

因此边界值本身应通过校验。

---

#### 边界相邻值测试

测试：

```text
29分钟
121分钟
```

预期：

```text
DURATION_OUT_OF_RANGE
```

原因：

已经超出允许范围。

---

#### 同时违反多项规则测试

测试：

```text
start = 10:00
end = 09:00
```

同时违反：

- end 不晚于 start
- 时长不合法

预期：

```text
END_NOT_AFTER_START
```

原因：

按照题目规定的优先级顺序，应先返回 END_NOT_AFTER_START。

---

### 自选边界测试说明

测试：

```text
start = 10:00
end = 10:30
```

时长：

```text
30分钟
```

判断过程：

1. start 存在
2. end 存在
3. end 晚于 start
4. 时长等于最小合法值 30 分钟

因此预期结果：

```text
VALID
```

实际运行结果与预期一致。

---

## 第二题：修复缺失预约错误响应

### 修改前的可能原因分析

在调试前，我提出了以下几种可能原因：

#### 原因一

Repository 查询不到 Booking 时返回 null，但后续代码直接访问对象属性导致空指针异常。

验证方式：

使用 Debug 查看查询结果是否为空。

---

#### 原因二

Service 层抛出了错误异常类型，没有被全局异常处理器转换为 404。

验证方式：

观察实际抛出的异常类型。

---

#### 原因三

Controller 没有处理 Booking 不存在的情况。

验证方式：

跟踪 Controller 调用链。

---

### Debug过程

使用 IDEA GUI Debugger 单步调试。

观察到：

```text
roomId = room-101
bookingId = booking-missing
```

查询结果：

```text
Room存在
Booking不存在
```

继续执行发现：

```text
BookingNotFoundException
```

被抛出。

最终确认：

问题不是 Room 查询失败。

问题是 Booking 不存在时没有正确映射为 404 响应。

---

### 修复步骤

#### 第一步：增加回归测试

增加测试：

```text
GET /rooms/room-101/bookings/booking-missing
```

预期：

```text
404
BOOKING_NOT_FOUND
```

运行测试，确认测试失败。

---

#### 第二步：完成最小修改

只修改异常处理逻辑。

不修改其它业务逻辑。

---

#### 第三步：再次运行测试

测试通过。

返回：

```json
{
  "code": "BOOKING_NOT_FOUND"
}
```

状态码：

```text
404
```

---

### 为什么应该返回404而不是400

400 Bad Request 表示：

```text
请求格式或参数错误
```

例如：

```text
start > end
时间格式错误
缺少必填参数
```

而本题中：

```text
请求格式正确
路径正确
参数正确
```

只是目标资源不存在。

因此应返回：

```text
404 Not Found
```

而不是：

```text
400 Bad Request
```

---

### 常见4xx状态码举例

#### 400 Bad Request

请求参数非法。

例如：

```text
预约结束时间早于开始时间
```

---

#### 401 Unauthorized

未登录。

例如：

```text
访问需要认证的接口
```

---

#### 403 Forbidden

已登录但无权限。

例如：

```text
普通用户访问管理员接口
```

---

#### 404 Not Found

资源不存在。

例如：

```text
Booking不存在
Room不存在
```

---

#### 409 Conflict

资源冲突。

例如：

```text
预约时间冲突
```

---

## 第三题：修复可用性判断故障

### 修改前的可能原因分析

#### 原因一

时间重叠判断公式写错。

---

#### 原因二

比较符号错误。

例如：

```java
<
<=
>
>=
```

使用不当。

---

#### 原因三

查询到了错误的 Booking 集合。

---

#### 原因四

循环提前返回导致部分 Booking 未检查。

---

### Debug过程

使用 IDEA GUI Debugger 单步调试。

观察：

```text
roomId = room-202
start = 2030-01-15T10:15:00
end = 2030-01-15T10:45:00
```

查询到的 Booking：

```text
booking-2021
10:00 ~ 10:30
```

使用 Evaluate Expression 验证：

```java
start.isBefore(existingEnd)
```

结果：

```text
true
```

继续验证：

```java
end.isAfter(existingStart)
```

结果：

```text
true
```

说明两个时间区间确实发生重叠。

最终定位问题：

```text
重叠判断逻辑实现错误
```

导致系统错误返回：

```text
available = true
```

---

### 增加的回归测试

#### 测试一：完全重叠

预期：

```text
available = false
```

---

#### 测试二：部分重叠

预期：

```text
available = false
```

---

#### 测试三：包含关系

预期：

```text
available = false
```

---

#### 测试四：首尾相接

例如：

```text
10:30 ~ 11:00
```

预期：

```text
available = true
```

原因：

题目要求使用半开区间：

```text
[start,end)
```

首尾相接不属于重叠。

---

### 最终修复

采用标准区间重叠判断：

```java
start.isBefore(existingEnd)
        && end.isAfter(existingStart)
```

含义：

```text
新区间开始时间早于旧区间结束时间
并且
新区间结束时间晚于旧区间开始时间
```

同时满足时即发生重叠。

---

### 扩展问题：数十亿条Booking时如何优化

当前实现：

```text
查询房间全部预约记录
逐条遍历判断
```

时间复杂度：

```text
O(n)
```

当某个房间存在数十亿条预约记录时：

- 查询慢
- 内存占用大
- 响应时间长

---

### 优化方案

#### 方案一：数据库索引

建立联合索引：

```sql
(room_id,start_time,end_time)
```

减少扫描范围。

---

#### 方案二：SQL直接判断重叠

只查询可能重叠的数据：

```sql
WHERE room_id = ?
AND start_time < ?
AND end_time > ?
```

避免加载全部数据。

---

#### 方案三：时间分区

按月份或季度分区：

```text
2029Q4
2030Q1
2030Q2
```

减少查询范围。

---

#### 方案四：缓存热点数据

使用 Redis 缓存近期预约数据。

减少数据库访问次数。

---

## 第四题：实现创建预约接口

### 实现内容

实现接口：

```http
POST /rooms/{roomId}/bookings
```

请求示例：

```json
{
  "start":"2030-01-15T10:00:00",
  "end":"2030-01-15T10:30:00"
}
```

---

### 实现要求对应情况

#### 服务端生成Booking ID

由服务端自动生成：

```text
booking-3001
booking-3002
...
```

客户端无需提供 ID。

---

#### 创建成功

返回：

```text
201 Created
```

同时返回：

- 创建后的 Booking
- Location 响应头

例如：

```text
Location:
/rooms/room-101/bookings/booking-3001
```

---

#### 时间窗口非法

返回：

```text
400
INVALID_BOOKING_WINDOW
```

---

#### Room不存在

返回：

```text
404
ROOM_NOT_FOUND
```

---

#### 时间冲突

返回：

```text
409
BOOKING_CONFLICT
```

---

#### 半开区间规则

采用：

```text
[start,end)
```

例如：

```text
已有预约：
10:00~10:30

新预约：
10:30~11:00
```

结果：

```text
不冲突
```

允许创建。

---

#### 失败请求不修改数据

测试方法：

创建前：

```java
countByRoomId(roomId)
```

记录数量。

请求失败后再次统计数量。

验证：

```text
数量保持不变
```

说明失败请求没有新增或修改数据。

---

### Location响应头验证

创建成功后：

读取：

```text
Location
```

例如：

```text
/rooms/room-101/bookings/booking-3003
```

随后执行：

```http
GET /rooms/room-101/bookings/booking-3003
```

验证：

```text
200 OK
```

并成功查询到刚创建的 Booking。

证明：

```text
创建成功
Location正确
数据已成功保存
```

---

### 为什么选择POST

POST语义：

```text
在资源集合下创建新资源
```

例如：

```http
POST /rooms/room-101/bookings
```

符合：

```text
服务端生成Booking ID
```

的场景。

---

### 为什么不用PUT

PUT通常用于：

```text
客户端指定资源ID
```

例如：

```http
PUT /bookings/booking-3001
```

而本题要求：

```text
服务端生成ID
```

因此不适合使用 PUT。

---

### 为什么不用PATCH

PATCH语义：

```text
部分更新已有资源
```

例如：

```text
修改预约时间
修改预约状态
```

不适用于创建资源。

---

### POST、PUT、PATCH的幂等性比较

#### POST

通常非幂等。

连续执行两次：

```text
可能创建两个预约
```

---

#### PUT

幂等。

执行多次结果一致。

---

#### PATCH

通常非幂等。

是否幂等取决于具体实现。

---

### 其它常见HTTP方法用途

#### GET

查询资源。

例如：

```http
GET /rooms/room-101/bookings/booking-1011
```

---

#### POST

创建资源。

例如：

```http
POST /rooms/room-101/bookings
```

---

#### PUT

整体更新资源。

例如：

```http
PUT /bookings/booking-1011
```

---

#### PATCH

部分更新资源。

例如：

```http
PATCH /bookings/booking-1011
```

---

#### DELETE

删除资源。

例如：

```http
DELETE /bookings/booking-1011
```
````
