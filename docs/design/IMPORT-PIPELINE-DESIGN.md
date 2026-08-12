# 导入管线设计

## 目标

把角色卡导入统一成一条稳定流程，覆盖：

- `PNG`
- `JSON`
- `ZIP`

入口保持一个按钮，后端按文件内容自动分流。

## 当前现状

- 现有批量导入入口只选择文件，不区分格式。
- 当前仓库层只真正处理 `PNG`。
- `JSON` 已经能在模型层解析，也能导出，但没有独立导入。
- `ZIP` 只在导出侧存在，导入侧还没有解包逻辑。

## 设计原则

1. 入口复用，不增加多个导入按钮。
2. 格式识别下沉到导入管线，不放在 UI 层。
3. 校验、去重、落库保持统一出口。
4. `PNG` / `JSON` / `ZIP` 最终都转成同一种内部导入对象。
5. 失败可回报，成功可统计，重复可单独计数。

## 用户入口

### 主入口

仍然保留一个按钮：

- `批量导入角色卡`

### 入口行为

用户从系统文件选择器选中文件后，允许以下输入：

- 单个或多个 `PNG`
- 单个或多个 `JSON`
- 单个或多个 `ZIP`

### UI 建议

导入对话框里只保留一个入口按钮，不拆成多个导入类型按钮。
按钮后面可以写成：

- `批量导入（PNG / JSON / ZIP）`

## 总体流程

```text
入口选择文件
  -> 文件类型识别
  -> ZIP 解包 / 普通文件直读
  -> 格式校验
  -> 规范化
  -> 去重判断
  -> 生成落库实体
  -> 写 PNG 文件 + 写 Room
  -> 汇总导入结果
```

## 详细流程

### 1. 入口选择

通过系统文件选择器获取 `Uri` 列表。

允许：

- `image/png`
- `application/json`
- `application/zip`
- `*/*` 作为兜底

### 2. 文件分流

对每个 `Uri` 先判断格式：

- `PNG`：直接读取
- `JSON`：直接读取文本
- `ZIP`：先解包，再逐个处理压缩包内条目

### 3. ZIP 处理规则

建议仅支持：

- 一级解包
- 只提取 `.png` 和 `.json`
- 忽略目录
- 忽略嵌套压缩包

原因：

- 降低压缩炸弹风险
- 降低递归和路径穿越复杂度
- 与当前“快速导入”目标一致

### 4. 格式校验

#### PNG

使用现有 PNG 校验：

- PNG 头校验
- 提取 `chara` / `ccv3` JSON
- 校验 `CharacterCardV3`

#### JSON

流程：

- 解析为 `CharacterCardV3`
- 补齐缺失名称
- 标准化后重新编码
- 生成默认封面 PNG

JSON 导入后，最终仍要落成一张本地 PNG 卡片。

#### ZIP

ZIP 内部条目按 `PNG` / `JSON` 再走一次相同校验流程。

## 去重设计

### 去重目标

避免以下重复导入：

- 完全相同的文件
- 内容相同但封装不同的文件
- JSON 格式不同但角色内容相同的文件

### 去重键

建议同时保留三类键：

1. `fileHash`
2. `semanticHash`
3. `normalizedJsonHash`

### 含义

#### `fileHash`

原始文件级哈希。

- PNG：原始 PNG bytes 的 SHA-256
- JSON：规范化后生成物的 SHA-256，或者原始 JSON 的规范化版本哈希

#### `semanticHash`

角色语义级哈希。

建议继续沿用：

- `name`
- `creator`
- `description`

拼成固定字符串后做 SHA-256。

#### `normalizedJsonHash`

JSON 专用内容哈希。

做法：

1. 解析 JSON
2. 补齐默认字段
3. 统一编码输出
4. 对标准化后的 JSON 字符串做 SHA-256

### 去重判断顺序

推荐顺序：

1. 先查 `fileHash`
2. 再查 `normalizedJsonHash`
3. 再查 `semanticHash`

### 判定结果

- 命中 `fileHash`：完全重复
- 命中 `normalizedJsonHash`：JSON 内容重复
- 命中 `semanticHash`：同角色重复

## 落库设计

### 统一内部对象

不管输入是 `PNG` / `JSON` / `ZIP`，最终都统一成：

- `CharacterCardV3`
- `rawJsonData`
- `imagePath`
- `fileHash`
- `semanticHash`
- `tagsJson`

### JSON 落库方式

JSON 导入后：

- 生成默认 PNG
- 将 JSON 重新写入 PNG chunk
- 保存到 `cards/` 目录
- 写入 Room

### PNG 落库方式

PNG 直接按现有逻辑：

- 校验
- 去重
- 写入 `cards/`
- 写入 Room

## 错误与统计

建议导入结果至少分成：

- 成功数
- 重复数
- 失败数
- 失败原因列表
- ZIP 扫描数

### 推荐结果结构

```text
BatchImportResult
  successCount
  failedCount
  duplicateCount
  scannedCount
  failedReasons
```

## 建议实现拆分

### 1. 导入入口层

负责：

- 获取文件 Uri
- 触发导入
- 展示汇总结果

### 2. 解包层

负责：

- ZIP 解包
- 条目过滤
- 安全限制

### 3. 解析层

负责：

- PNG 解析
- JSON 解析
- 默认封面生成

### 4. 去重层

负责：

- file hash
- normalized JSON hash
- semantic hash

### 5. 持久化层

负责：

- PNG 写盘
- Room 插入
- 卡片元数据保存

## 风险点

- JSON 直接导入时如果字段不完整，默认封面需要保证可用。
- ZIP 如果不限制大小，容易产生性能和安全问题。
- 去重如果只看语义字段，可能误伤同名同作者不同版本卡片。

## 推荐结论

最稳的方案是：

- 一个入口
- 三种格式统一识别
- ZIP 先解包
- PNG / JSON 最后统一成 PNG + JSON 元数据落库
- 去重同时保留 `fileHash`、`semanticHash`、`normalizedJsonHash`

